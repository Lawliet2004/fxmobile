package com.fxsound.mobile.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.fxsound.mobile.core.model.AudioTelemetry
import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.EqBandTables
import com.fxsound.mobile.core.model.Preset
import com.fxsound.mobile.core.model.ProcessingMode
import com.fxsound.mobile.core.runtime.DevicePerformance
import com.fxsound.mobile.core.runtime.KeepAlivePolicy
import com.fxsound.mobile.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class AudioEngineController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var telemetryJob: Job? = null
    private var persistJob: Job? = null

    // Use AtomicLong so we can atomically take ownership of the native handle in release()
    // and during the telemetry snapshot. Plain @Volatile would allow a race where another
    // thread reads the old (non-zero) pointer just as release() nulls it out.
    private val enginePtr = AtomicLong(0L)
    private val streamPtr = AtomicLong(0L)
    @Volatile private var dspBackend: String = "Rust DSP"
    @Volatile private var nativeStarted: Boolean = false
    @Volatile private var released: Boolean = false
    private var initJob: Job? = null
    @Volatile private var keepAliveActive: Boolean = false
    @Volatile private var keepAliveTitle: String? = null
    var onNeedNotificationPermission: (() -> Unit)? = null

    /**
     * Remembered "was playing" state for restore-after-noisy. Set to the current
     * `isPlaying` flag in the `onBecomingNoisy` callback; cleared when we resume.
     */
    @Volatile private var playingBeforeNoisy: Boolean = false

    private val _parameters = MutableStateFlow(DspParameters())
    val parameters: StateFlow<DspParameters> = _parameters.asStateFlow()

    private val _telemetry = MutableStateFlow(AudioTelemetry())
    val telemetry: StateFlow<AudioTelemetry> = _telemetry.asStateFlow()

    private val meterBuffer = FloatArray(5)
    private val telemetryBuffer = FloatArray(6)

    private val sessionEffects = SessionEffectController()
    private var decoder: AudioFileDecoder? = null
    @Volatile private var currentMediaUri: Uri? = null
    @Volatile private var settingsVisible: Boolean = false
    @Volatile private var foregroundVisible: Boolean = true
    @Volatile private var metersVisible: Boolean = false

    private val focusManager = AudioFocusManager(
        context = context,
        onDuck = { ducked ->
            withEngine { engine ->
                val duckGain = if (ducked) -12.0f else _parameters.value.masterGainDb
                NativeDspBridge.setMasterGainDb(engine, duckGain)
            }
        },
        onLoss = { stopPlayback() },
        onGain = {
            withEngine { NativeDspBridge.setMasterGainDb(it, _parameters.value.masterGainDb) }
        },
        onTransientLoss = { shouldResume ->
            // Remember the pre-pause playing state so we can auto-resume when focus
            // comes back (if the output route is still available).
            playingBeforeNoisy = _telemetry.value.isPlaying
            pausePlayback()
            // Expose a hook for the caller to wire resume-on-gain if desired. The
            // current onGain callback unconditionally restores the master gain, so
            // we don't auto-call shouldResume() here — the resume is performed by
            // the device-restore path or by explicit user action.
            @Suppress("UNUSED_EXPRESSION") shouldResume
        }
    )

    private val deviceReceiver = AudioDeviceReceiver(
        context = context,
        onDeviceChanged = { deviceName, isBluetooth ->
            _telemetry.value = _telemetry.value.copy(
                activeOutputDevice = deviceName,
                isBluetooth = isBluetooth
            )
            // If a wired output reappears (or a non-bluetooth device takes over)
            // and we had paused on BECOMING_NOISY, try to resume.
            if (!isBluetooth &&
                (deviceName.startsWith("Wired") || deviceName.startsWith("USB")) &&
                playingBeforeNoisy
            ) {
                restoreIfNeeded()
            }
        },
        onBecomingNoisy = {
            // Only mark if we were actually playing - prevents stale state from a
            // pause that already happened for another reason.
            if (_telemetry.value.isPlaying) {
                playingBeforeNoisy = true
            }
            pausePlayback()
        }
    )

    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
            if (sessionId == AudioEffect.ERROR) return
            when (intent.action) {
                SessionEffectController.ACTION_OPEN -> {
                    if (_telemetry.value.processingMode == ProcessingMode.PLAYER_SESSION) {
                        val ok = sessionEffects.attach(sessionId)
                        if (ok) {
                            sessionEffects.apply(_parameters.value)
                            _telemetry.value = _telemetry.value.copy(
                                sessionAttached = true,
                                sessionLabel = "Session $sessionId",
                                lastError = null
                            )
                            syncKeepAlive()
                        } else {
                            _telemetry.value = _telemetry.value.copy(
                                lastError = "Processing is unavailable on this device."
                            )
                        }
                    }
                }
                SessionEffectController.ACTION_CLOSE -> {
                    if (sessionEffects.sessionId == sessionId) {
                        sessionEffects.detach()
                        _telemetry.value = _telemetry.value.copy(
                            sessionAttached = false,
                            sessionLabel = ""
                        )
                        syncKeepAlive()
                    }
                }
            }
        }
    }

    init {
        deviceReceiver.register()
        registerSessionReceiver()
        setProcessingMode(settingsRepository.loadProcessingMode())
        restoreLastMedia()
        startTelemetryPolling()
    }

    fun ensureNativeStarted() {
        if (released) return
        if (nativeStarted) return
        synchronized(this) {
            if (released) return
            if (nativeStarted || initJob != null) return
            initJob = scope.launch {
                initializeNativeEngine()
                synchronized(this@AudioEngineController) {
                    if (!released && enginePtr.get() != 0L) {
                        nativeStarted = true
                    } else {
                        initJob = null
                    }
                }
            }
        }
    }

    private fun registerSessionReceiver() {
        val filter = IntentFilter().apply {
            addAction(SessionEffectController.ACTION_OPEN)
            addAction(SessionEffectController.ACTION_CLOSE)
        }
        ContextCompat.registerReceiver(context, sessionReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    private fun initializeNativeEngine() {
        try {
            if (!NativeDspBridge.isAvailable) {
                setError("Audio processing could not be started.")
                return
            }
            dspBackend = try {
                NativeDspBridge.getDspBackend()
            } catch (t: Throwable) {
                Log.w(TAG, "getDspBackend failed, using default", t)
                "Rust DSP"
            }
            val newEngine = NativeDspBridge.createEngine(48000.0f, 2048)
            if (released) {
                if (newEngine != 0L) NativeDspBridge.destroyEngine(newEngine)
                return
            }
            if (enginePtr.compareAndSet(0L, newEngine)) {
                if (newEngine != 0L) {
                    // Do not create a stream against an engine release() already
                    // destroyed (or is about to destroy). If we still own the
                    // pointer, take it back and free it ourselves.
                    if (released) {
                        val owned = enginePtr.getAndSet(0L)
                        if (owned != 0L) NativeDspBridge.destroyEngine(owned)
                        return
                    }
                    val newStream = NativeDspBridge.createAudioStream(newEngine)
                    if (released) {
                        if (newStream != 0L) NativeDspBridge.destroyAudioStream(newStream)
                        val owned = enginePtr.getAndSet(0L)
                        if (owned != 0L) NativeDspBridge.destroyEngine(owned)
                        return
                    }
                    if (streamPtr.compareAndSet(0L, newStream)) {
                        if (released) {
                            if (newStream != 0L) NativeDspBridge.destroyAudioStream(newStream)
                            streamPtr.set(0L)
                            return
                        }
                        if (newStream != 0L && DevicePerformance.preferLowPowerIo(context)) {
                            NativeDspBridge.setAudioStreamLowPowerIo(newStream, true)
                        }
                        applyAllParameters(_parameters.value)
                    } else if (newStream != 0L) {
                        NativeDspBridge.destroyAudioStream(newStream)
                    }
                } else {
                    setError("Audio processing could not be started.")
                }
            } else if (newEngine != 0L) {
                NativeDspBridge.destroyEngine(newEngine)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Native engine initialization failed", t)
            setError("Audio processing could not be started.")
        }
    }

    fun applyInitialState(parameters: DspParameters) {
        val ten = parameters.toTenBand()
        _parameters.value = ten
        if (enginePtr.get() != 0L) {
            applyAllParameters(ten)
        }
        syncKeepAlive()
        ensureNativeStarted()
    }

    fun setSettingsVisible(visible: Boolean) {
        settingsVisible = visible
    }

    fun setForegroundVisible(visible: Boolean) {
        foregroundVisible = visible
    }

    fun setMetersVisible(visible: Boolean) {
        metersVisible = visible
    }

    fun setProcessingMode(mode: ProcessingMode) {
        settingsRepository.saveProcessingMode(mode)
        _telemetry.value = _telemetry.value.copy(processingMode = mode)
        if (mode == ProcessingMode.INTERNAL_ENGINE) {
            sessionEffects.detach()
            _telemetry.value = _telemetry.value.copy(sessionAttached = false, sessionLabel = "")
        }
        syncKeepAlive()
    }

    private fun syncKeepAlive() {
        val tel = _telemetry.value
        val keep = KeepAlivePolicy.shouldKeepAlive(
            powerEnabled = _parameters.value.powerEnabled,
            isPlaying = tel.isPlaying,
            mode = tel.processingMode,
            sessionAttached = tel.sessionAttached
        )
        if (keep) {
            val title = when {
                tel.isPlaying && tel.sourceType == 4 -> tel.mediaTitle
                tel.processingMode == ProcessingMode.PLAYER_SESSION -> "Player session"
                else -> "Enhancement on"
            }
            if (keepAliveActive && keepAliveTitle == title) return
            try {
                onNeedNotificationPermission?.invoke()
            } catch (t: Throwable) {
                Log.w(TAG, "notification permission request failed", t)
            }
            try {
                AudioPlaybackService.start(context, title)
                keepAliveActive = true
                keepAliveTitle = title
            } catch (t: Throwable) {
                Log.w(TAG, "keep-alive service start failed", t)
                keepAliveActive = false
                keepAliveTitle = null
            }
        } else if (keepAliveActive) {
            try {
                AudioPlaybackService.stop(context)
            } catch (t: Throwable) {
                Log.w(TAG, "keep-alive service stop failed", t)
            }
            keepAliveActive = false
            keepAliveTitle = null
        }
    }

    private fun startTelemetryPolling() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            while (isActive) {
                if (released) break

                // Take a consistent atomic snapshot of the native pointers for this
                // iteration so we don't race with release().
                val engine = enginePtr.get()
                val stream = streamPtr.get()

                val playing = _telemetry.value.isPlaying
                val needMeters = settingsVisible || (playing && metersVisible)
                val uiAwake = foregroundVisible || settingsVisible
                if (engine != 0L && uiAwake && needMeters && NativeDspBridge.isAvailable) {
                    try {
                        NativeDspBridge.getMeterData(engine, meterBuffer)
                    } catch (t: Throwable) {
                        Log.w(TAG, "getMeterData failed", t)
                        delay(2000L)
                        continue
                    }
                    var sr = 48000
                    var ch = 2
                    var buf = 256
                    var burst = 128
                    var cpu = 0.0f
                    var isAAudio = true

                    if (stream != 0L && !released) {
                        try {
                            NativeDspBridge.getAudioStreamTelemetry(stream, telemetryBuffer)
                            sr = telemetryBuffer[0].toInt()
                            ch = telemetryBuffer[1].toInt()
                            buf = telemetryBuffer[2].toInt()
                            burst = telemetryBuffer[3].toInt()
                            cpu = telemetryBuffer[4]
                            isAAudio = telemetryBuffer[5] > 0.5f
                        } catch (t: Throwable) {
                            Log.w(TAG, "getAudioStreamTelemetry failed", t)
                        }
                    }

                    val latencyMs = if (sr > 0) (buf.toFloat() / sr.toFloat()) * 1000.0f else 5.33f
                    _telemetry.value = _telemetry.value.copy(
                        sampleRate = sr,
                        channelCount = ch,
                        bufferSizeFrames = buf,
                        burstFrames = burst,
                        estimatedLatencyMs = latencyMs,
                        dspCpuLoadPercent = cpu,
                        audioBackend = (if (isAAudio) "AAudio" else "OpenSL ES") + " + " + dspBackend,
                        peakLeft = meterBuffer[0],
                        peakRight = meterBuffer[1],
                        rmsLeft = meterBuffer[2],
                        rmsRight = meterBuffer[3],
                        isClipping = meterBuffer[4] > 0.5f
                    )
                }
                delay(
                    when {
                        !uiAwake -> 2000L
                        needMeters && playing -> 200L
                        else -> 2000L
                    }
                )
                if (released) break
            }
        }
    }

    fun startPlayback(sourceType: Int = 0, frequency: Float = 440.0f) {
        if (released) return
        if (!frequency.isFinite()) {
            Log.w(TAG, "ignored non-finite frequency")
            return
        }
        ensureNativeStarted()
        val stream = streamPtr.get()
        if (stream == 0L) {
            val job = initJob
            if (job != null && !job.isCompleted) {
                scope.launch {
                    job.join()
                    if (released) return@launch
                    if (streamPtr.get() != 0L) startPlayback(sourceType, frequency)
                    else setError("No compatible audio output is available.")
                }
                return
            }
            setError("No compatible audio output is available.")
            return
        }
        stopDecoder()
        NativeDspBridge.resetPcmRing(stream)
        if (!focusManager.requestAudioFocus()) {
            setError("Another application controls this audio route.")
            return
        }
        NativeDspBridge.setAudioStreamSource(stream, sourceType, frequency)
        val success = NativeDspBridge.startAudioStream(stream)
        if (success) {
            AudioPlaybackService.start(context, if (sourceType == 4) _telemetry.value.mediaTitle else "Test generator")
            _telemetry.value = _telemetry.value.copy(
                isPlaying = true,
                sourceType = sourceType,
                lastError = null
            )
            syncKeepAlive()
        } else {
            focusManager.abandonAudioFocus()
            setError("Audio processing could not be started.")
        }
    }

    fun openAudioFile(uri: Uri) {
        if (released) return
        ensureNativeStarted()
        val stream = streamPtr.get()
        val engine = enginePtr.get()
        if (stream == 0L || engine == 0L) {
            val job = initJob
            if (job != null && !job.isCompleted) {
                scope.launch {
                    job.join()
                    if (released) return@launch
                    if (streamPtr.get() != 0L && enginePtr.get() != 0L) openAudioFile(uri)
                    else setError("Audio processing could not be started.")
                }
                return
            }
            setError("Audio processing could not be started.")
            return
        }
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        settingsRepository.saveLastMediaUri(uri.toString())
        currentMediaUri = uri
        stopDecoder()
        NativeDspBridge.resetPcmRing(stream)
        if (!focusManager.requestAudioFocus()) {
            setError("Another application controls this audio route.")
            return
        }
        NativeDspBridge.setAudioStreamSource(stream, 4, 440.0f)
        val started = NativeDspBridge.startAudioStream(stream)
        if (!started) {
            focusManager.abandonAudioFocus()
            setError("No compatible audio output is available.")
            return
        }
        // Sample-rate-aware decoder: prefer the Oboe-actual rate reported in telemetry.
        // If telemetry hasn't refreshed yet (stream was just started) fall back to 48000.
        // TODO: once Oboe reports its actual rate synchronously, drop the fallback.
        val sr = _telemetry.value.sampleRate.takeIf { it > 0 } ?: 48000
        decoder = AudioFileDecoder(
            context = context,
            writeFrames = { data, offset, frames ->
                val s = streamPtr.get()
                if (s == 0L) 0 else NativeDspBridge.writePcmFrames(s, data, offset, frames)
            },
            availableWrite = {
                val s = streamPtr.get()
                if (s == 0L) 0 else NativeDspBridge.pcmAvailableWrite(s)
            },
            onProgress = { pos, dur ->
                _telemetry.value = _telemetry.value.copy(
                    mediaPositionMs = pos,
                    mediaDurationMs = dur
                )
            },
            onEnded = {
                scope.launch(Dispatchers.Main.immediate) { stopPlayback() }
            },
            onError = { msg ->
                _telemetry.value = _telemetry.value.copy(lastError = msg, isPlaying = false)
                AudioPlaybackService.stop(context)
            }
        )
        decoder?.openAndPlay(uri, sr)
        val title = decoder?.title ?: "Audio"
        AudioPlaybackService.start(context, title)
        _telemetry.value = _telemetry.value.copy(
            isPlaying = true,
            sourceType = 4,
            mediaTitle = title,
            lastError = null
        )
        syncKeepAlive()
    }

    fun toggleMediaPlayback() {
        if (currentMediaUri == null) return
        if (_telemetry.value.isPlaying && _telemetry.value.sourceType == 4) {
            pausePlayback()
        } else {
            resumeMedia()
        }
    }

    private fun resumeMedia() {
        val uri = currentMediaUri ?: return
        if (decoder != null && streamPtr.get() != 0L) {
            if (!focusManager.requestAudioFocus()) {
                setError("Another application controls this audio route.")
                return
            }
            val stream = streamPtr.get()
            if (stream == 0L) return
            NativeDspBridge.setAudioStreamSource(stream, 4, 440.0f)
            val started = NativeDspBridge.startAudioStream(stream)
            if (started) {
                decoder?.resume()
                _telemetry.value = _telemetry.value.copy(isPlaying = true, lastError = null)
                syncKeepAlive()
            } else {
                setError("Audio processing could not be started.")
            }
        } else {
            openAudioFile(uri)
        }
    }

    fun seekMedia(positionMs: Long) {
        decoder?.seekMs(positionMs)
        val stream = streamPtr.get()
        if (stream != 0L) NativeDspBridge.resetPcmRing(stream)
        _telemetry.value = _telemetry.value.copy(mediaPositionMs = positionMs)
    }

    fun stopPlayback() {
        stopDecoder()
        val stream = streamPtr.get()
        if (stream != 0L) {
            NativeDspBridge.stopAudioStream(stream)
            NativeDspBridge.resetPcmRing(stream)
        }
        focusManager.abandonAudioFocus()
        _telemetry.value = _telemetry.value.copy(isPlaying = false)
        syncKeepAlive()
    }

    fun pausePlayback() {
        decoder?.pause()
        val stream = streamPtr.get()
        if (stream != 0L) {
            NativeDspBridge.pauseAudioStream(stream)
        }
        _telemetry.value = _telemetry.value.copy(isPlaying = false)
        syncKeepAlive()
    }

    fun setSourceType(sourceType: Int, frequency: Float = 440.0f) {
        if (sourceType == 4) return
        if (!frequency.isFinite()) {
            Log.w(TAG, "ignored non-finite frequency")
            return
        }
        stopDecoder()
        val stream = streamPtr.get()
        if (stream != 0L) {
            NativeDspBridge.resetPcmRing(stream)
            NativeDspBridge.setAudioStreamSource(stream, sourceType, frequency)
        }
        _telemetry.value = _telemetry.value.copy(sourceType = sourceType)
    }

    private fun stopDecoder() {
        decoder?.stop()
        decoder = null
    }

    private fun restoreLastMedia() {
        // Intentionally does NOT start playback - it just remembers the URI so the
        // user can resume via toggleMediaPlayback / openAudioFile.
        val saved = settingsRepository.loadLastMediaUri() ?: return
        val uri = runCatching { Uri.parse(saved) }.getOrNull() ?: return
        currentMediaUri = uri
        val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio"
        _telemetry.value = _telemetry.value.copy(mediaTitle = title, sourceType = 4)
    }

    fun applyPreset(preset: Preset) {
        val params = preset.parameters.toTenBand()
        _parameters.value = params
        applyAllParameters(params)
        persistParams(immediate = true)
    }

    private fun applyAllParameters(params: DspParameters) {
        // Safe to call before the native engine is up: it just updates _parameters
        // state. JNI calls are skipped when enginePtr == 0L.
        withEngine { engine ->
            NativeDspBridge.setPowerEnabled(engine, params.powerEnabled)
            NativeDspBridge.setBass(engine, params.bass)
            NativeDspBridge.setClarity(engine, params.clarity)
            NativeDspBridge.setAmbience(engine, params.ambience)
            NativeDspBridge.setSurround(engine, params.surround)
            NativeDspBridge.setDynamicBoost(engine, params.dynamicBoost)
            NativeDspBridge.setVolumeLeveling(engine, params.volumeLeveling)
            NativeDspBridge.setMasterGainDb(engine, params.masterGainDb)
            NativeDspBridge.setBalance(engine, params.balance)
            NativeDspBridge.setEqEnabled(engine, params.eqEnabled)
            NativeDspBridge.setEqBandCount(engine, params.eqBandCount)
            val count = params.eqBandCount
            for (i in 0 until count) {
                if (i < params.eqFrequencies.size) {
                    NativeDspBridge.setEqBandFrequency(engine, i, params.eqFrequencies[i])
                }
                if (i < params.eqQFactors.size) {
                    NativeDspBridge.setEqBandQ(engine, i, params.eqQFactors[i])
                }
                if (i < params.eqGains.size) {
                    NativeDspBridge.setEqBandGain(engine, i, params.eqGains[i])
                }
            }
        }
        if (_telemetry.value.processingMode == ProcessingMode.PLAYER_SESSION && sessionEffects.isAttached) {
            sessionEffects.apply(params)
        }
    }

    private fun persistParams(immediate: Boolean = false) {
        persistJob?.cancel()
        if (immediate) {
            settingsRepository.saveParameters(_parameters.value)
            return
        }
        persistJob = scope.launch {
            delay(800)
            settingsRepository.saveParameters(_parameters.value)
        }
    }

    private fun setFloat(
        value: Float,
        range: ClosedFloatingPointRange<Float>,
        copy: (DspParameters, Float) -> DspParameters,
        native: (Long, Float) -> Unit,
        session: Boolean = false
    ) {
        if (!value.isFinite()) {
            Log.w(TAG, "ignored non-finite DSP value")
            return
        }
        val clamped = value.coerceIn(range.start, range.endInclusive)
        _parameters.value = copy(_parameters.value, clamped)
        persistParams()
        withEngine { native(it, clamped) }
        if (session && sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
    }

    fun setPowerEnabled(enabled: Boolean) {
        if (released) return
        _parameters.value = _parameters.value.copy(powerEnabled = enabled)
        persistParams()
        withEngine { NativeDspBridge.setPowerEnabled(it, enabled) }
        if (sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
        syncKeepAlive()
    }

    private inline fun withEngine(block: (Long) -> Unit) {
        if (released) return
        if (!NativeDspBridge.isAvailable) return
        val engine = enginePtr.get()
        if (engine == 0L) return
        try {
            block(engine)
        } catch (t: Throwable) {
            Log.w(TAG, "native engine call failed", t)
        }
    }

    fun setBass(amount: Float) = setFloat(amount, 0f..10f, { p, v -> p.copy(bass = v) }, NativeDspBridge::setBass, session = true)
    fun setClarity(amount: Float) = setFloat(amount, 0f..10f, { p, v -> p.copy(clarity = v) }, NativeDspBridge::setClarity)
    fun setAmbience(amount: Float) = setFloat(amount, 0f..10f, { p, v -> p.copy(ambience = v) }, NativeDspBridge::setAmbience)
    fun setSurround(amount: Float) = setFloat(amount, 0f..10f, { p, v -> p.copy(surround = v) }, NativeDspBridge::setSurround)
    fun setDynamicBoost(amount: Float) = setFloat(amount, 0f..10f, { p, v -> p.copy(dynamicBoost = v) }, NativeDspBridge::setDynamicBoost, session = true)
    fun setVolumeLeveling(amount: Float) = setFloat(amount, 0f..4f, { p, v -> p.copy(volumeLeveling = v) }, NativeDspBridge::setVolumeLeveling)
    fun setMasterGainDb(gainDb: Float) = setFloat(gainDb, -20f..20f, { p, v -> p.copy(masterGainDb = v) }, NativeDspBridge::setMasterGainDb, session = true)
    fun setBalance(balance: Float) = setFloat(balance, -20f..20f, { p, v -> p.copy(balance = v) }, NativeDspBridge::setBalance)

    fun setEqEnabled(enabled: Boolean) {
        if (released) return
        _parameters.value = _parameters.value.copy(eqEnabled = enabled)
        persistParams()
        withEngine { NativeDspBridge.setEqEnabled(it, enabled) }
        if (sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
    }

    fun setEqBandGain(bandIndex: Int, gainDb: Float) {
        if (!gainDb.isFinite()) {
            Log.w(TAG, "ignored non-finite EQ gain")
            return
        }
        val currentGains = _parameters.value.eqGains.toMutableList()
        if (bandIndex in currentGains.indices) {
            val clamped = gainDb.coerceIn(-12.0f, 12.0f)
            currentGains[bandIndex] = clamped
            _parameters.value = _parameters.value.copy(eqGains = currentGains)
            persistParams()
            withEngine { NativeDspBridge.setEqBandGain(it, bandIndex, clamped) }
            if (sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
        }
    }

    fun setEqBandFrequency(bandIndex: Int, freq: Float) {
        if (!freq.isFinite()) {
            Log.w(TAG, "ignored non-finite EQ frequency")
            return
        }
        val freqs = _parameters.value.eqFrequencies.toMutableList()
        if (bandIndex in freqs.indices) {
            val clamped = freq.coerceIn(20.0f, 20000.0f)
            freqs[bandIndex] = clamped
            _parameters.value = _parameters.value.copy(eqFrequencies = freqs)
            persistParams()
            withEngine { NativeDspBridge.setEqBandFrequency(it, bandIndex, clamped) }
            if (sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
        }
    }

    fun setEqBandQ(bandIndex: Int, q: Float) {
        if (!q.isFinite()) {
            Log.w(TAG, "ignored non-finite EQ Q")
            return
        }
        val qs = _parameters.value.eqQFactors.toMutableList()
        if (bandIndex in qs.indices) {
            val clamped = q.coerceIn(0.7f, 3.0f)
            qs[bandIndex] = clamped
            _parameters.value = _parameters.value.copy(eqQFactors = qs)
            persistParams()
            withEngine { NativeDspBridge.setEqBandQ(it, bandIndex, clamped) }
        }
    }

    fun resetEqBand(bandIndex: Int) {
        setEqBandGain(bandIndex, 0.0f)
        val defaults = EqBandTables.TEN
        val defaultQ = EqBandTables.defaultQ()
        if (bandIndex in defaults.indices) {
            setEqBandFrequency(bandIndex, defaults[bandIndex])
        }
        setEqBandQ(bandIndex, defaultQ)
    }

    fun resetAllEqBands() {
        val freqs = EqBandTables.TEN
        val qs = EqBandTables.qFactors()
        _parameters.value = _parameters.value.copy(
            eqBandCount = EqBandTables.COUNT,
            eqGains = List(EqBandTables.COUNT) { 0.0f },
            eqFrequencies = freqs,
            eqQFactors = qs
        )
        persistParams()
        withEngine { engine ->
            NativeDspBridge.resetAllEqBands(engine)
            freqs.forEachIndexed { i, f -> NativeDspBridge.setEqBandFrequency(engine, i, f) }
            qs.forEachIndexed { i, q -> NativeDspBridge.setEqBandQ(engine, i, q) }
        }
    }

    fun resetToFlat() {
        applyPreset(Preset("flat_tmp", "Flat", true, DspParameters.FLAT))
    }

    fun getMagnitudeResponse(frequencies: FloatArray, outDbs: FloatArray) {
        if (frequencies.isEmpty() || frequencies.size != outDbs.size) return
        withEngine { NativeDspBridge.getMagnitudeResponse(it, frequencies, outDbs, frequencies.size) }
    }

    fun computeMagnitudeResponse(frequencies: FloatArray): List<Float> {
        val out = FloatArray(frequencies.size)
        getMagnitudeResponse(frequencies, out)
        return out.toList()
    }

    fun clearError() {
        _telemetry.value = _telemetry.value.copy(lastError = null)
    }

    private fun setError(message: String) {
        Log.w(TAG, "Engine error: $message")
        _telemetry.value = _telemetry.value.copy(lastError = message, isPlaying = false)
    }

    /**
     * If we previously paused on `BECOMING_NOISY` and a wired output is now connected,
     * resume the current media source. No-op if we weren't paused-for-noisy or if the
     * new device is still the built-in speaker / bluetooth.
     */
    private fun restoreIfNeeded() {
        if (!playingBeforeNoisy) return
        if (!deviceReceiver.hasWiredOutput()) return
        playingBeforeNoisy = false
        resumeMedia()
    }

    fun release() {
        // 1) Flip the released flag FIRST so concurrent async branches and the
        //    telemetry poll bail out before touching native handles.
        released = true

        // 2) Persist parameters synchronously so we don't lose writes if the
        //    process is killed shortly after release.
        persistParams(immediate = true)
        runCatching { settingsRepository.saveParameters(_parameters.value) }

        // 3) Stop coroutines and decoders.
        telemetryJob?.cancel()
        persistJob?.cancel()
        stopPlayback()
        sessionEffects.detach()

        // 4) Unregister receivers.
        try { context.unregisterReceiver(sessionReceiver) } catch (_: Exception) {}
        deviceReceiver.unregister()

        // 5) Atomically take ownership of the native handles and destroy them.
        //    compareAndSet(0,0) on already-zero pointers is a no-op; getAndSet
        //    ensures no other thread can race in a stale non-zero value.
        val stream = streamPtr.getAndSet(0L)
        if (stream != 0L) {
            try { NativeDspBridge.destroyAudioStream(stream) } catch (e: Exception) {
                Log.w(TAG, "destroyAudioStream failed", e)
            }
        }
        val engine = enginePtr.getAndSet(0L)
        if (engine != 0L) {
            try { NativeDspBridge.destroyEngine(engine) } catch (e: Exception) {
                Log.w(TAG, "destroyEngine failed", e)
            }
        }

        keepAliveActive = false
        keepAliveTitle = null

        // 6) Finally cancel the scope so no further coroutines launch.
        scope.cancel()
    }

    companion object {
        private const val TAG = "FxEngine"
    }
}
