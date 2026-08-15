package com.fxsound.mobile.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.net.Uri
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioEngineController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var telemetryJob: Job? = null
    private var persistJob: Job? = null

    @Volatile private var enginePtr: Long = 0
    @Volatile private var streamPtr: Long = 0
    @Volatile private var dspBackend: String = "Rust DSP"
    @Volatile private var nativeStarted: Boolean = false
    private var initJob: Job? = null
    var onNeedNotificationPermission: (() -> Unit)? = null

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
            if (enginePtr != 0L) {
                val duckGain = if (ducked) -12.0f else _parameters.value.masterGainDb
                NativeDspBridge.setMasterGainDb(enginePtr, duckGain)
            }
        },
        onLoss = { stopPlayback() },
        onGain = {
            if (enginePtr != 0L) {
                NativeDspBridge.setMasterGainDb(enginePtr, _parameters.value.masterGainDb)
            }
        }
    )

    private val deviceReceiver = AudioDeviceReceiver(
        context = context,
        onDeviceChanged = { deviceName, isBluetooth ->
            _telemetry.value = _telemetry.value.copy(
                activeOutputDevice = deviceName,
                isBluetooth = isBluetooth
            )
        },
        onBecomingNoisy = { pausePlayback() }
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
        if (nativeStarted) return
        synchronized(this) {
            if (nativeStarted || initJob != null) return
            initJob = scope.launch {
                initializeNativeEngine()
            }
            nativeStarted = true
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
            dspBackend = try {
                NativeDspBridge.getDspBackend()
            } catch (_: Exception) {
                "Rust DSP"
            }
            enginePtr = NativeDspBridge.createEngine(48000.0f, 2048)
            if (enginePtr != 0L) {
                streamPtr = NativeDspBridge.createAudioStream(enginePtr)
                if (streamPtr != 0L && DevicePerformance.preferLowPowerIo(context)) {
                    NativeDspBridge.setAudioStreamLowPowerIo(streamPtr, true)
                }
                applyAllParameters(_parameters.value)
            } else {
                setError("Audio processing could not be started.")
            }
        } catch (_: Exception) {
            setError("Audio processing could not be started.")
        }
    }

    fun applyInitialState(parameters: DspParameters) {
        val ten = parameters.toTenBand()
        _parameters.value = ten
        if (enginePtr != 0L) {
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
                _telemetry.value.isPlaying && _telemetry.value.sourceType == 4 ->
                    _telemetry.value.mediaTitle
                _telemetry.value.processingMode == ProcessingMode.PLAYER_SESSION ->
                    "Player session"
                else -> "Enhancement on"
            }
            onNeedNotificationPermission?.invoke()
            AudioPlaybackService.start(context, title)
        } else {
            AudioPlaybackService.stop(context)
        }
    }

    private fun startTelemetryPolling() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            while (isActive) {
                val playing = _telemetry.value.isPlaying
                val needMeters = settingsVisible || (playing && metersVisible)
                val uiAwake = foregroundVisible || settingsVisible
                if (enginePtr != 0L && uiAwake && needMeters) {
                    NativeDspBridge.getMeterData(enginePtr, meterBuffer)
                    var sr = 48000
                    var ch = 2
                    var buf = 256
                    var burst = 128
                    var cpu = 0.0f
                    var isAAudio = true

                    if (streamPtr != 0L) {
                        NativeDspBridge.getAudioStreamTelemetry(streamPtr, telemetryBuffer)
                        sr = telemetryBuffer[0].toInt()
                        ch = telemetryBuffer[1].toInt()
                        buf = telemetryBuffer[2].toInt()
                        burst = telemetryBuffer[3].toInt()
                        cpu = telemetryBuffer[4]
                        isAAudio = telemetryBuffer[5] > 0.5f
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
            }
        }
    }

    fun startPlayback(sourceType: Int = 0, frequency: Float = 440.0f) {
        ensureNativeStarted()
        if (streamPtr == 0L) {
            val job = initJob
            if (job != null && !job.isCompleted) {
                scope.launch {
                    job.join()
                    if (streamPtr != 0L) startPlayback(sourceType, frequency)
                    else setError("No compatible audio output is available.")
                }
                return
            }
            setError("No compatible audio output is available.")
            return
        }
        stopDecoder()
        NativeDspBridge.resetPcmRing(streamPtr)
        if (!focusManager.requestAudioFocus()) {
            setError("Another application controls this audio route.")
            return
        }
        NativeDspBridge.setAudioStreamSource(streamPtr, sourceType, frequency)
        val success = NativeDspBridge.startAudioStream(streamPtr)
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
        ensureNativeStarted()
        if (streamPtr == 0L || enginePtr == 0L) {
            val job = initJob
            if (job != null && !job.isCompleted) {
                scope.launch {
                    job.join()
                    if (streamPtr != 0L && enginePtr != 0L) openAudioFile(uri)
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
        NativeDspBridge.resetPcmRing(streamPtr)
        if (!focusManager.requestAudioFocus()) {
            setError("Another application controls this audio route.")
            return
        }
        NativeDspBridge.setAudioStreamSource(streamPtr, 4, 440.0f)
        val started = NativeDspBridge.startAudioStream(streamPtr)
        if (!started) {
            focusManager.abandonAudioFocus()
            setError("No compatible audio output is available.")
            return
        }
        val sr = _telemetry.value.sampleRate.takeIf { it > 0 } ?: 48000
        decoder = AudioFileDecoder(
            context = context,
            writeFrames = { data, offset, frames ->
                if (streamPtr == 0L) 0 else NativeDspBridge.writePcmFrames(streamPtr, data, offset, frames)
            },
            availableWrite = {
                if (streamPtr == 0L) 0 else NativeDspBridge.pcmAvailableWrite(streamPtr)
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
        if (decoder != null && streamPtr != 0L) {
            if (!focusManager.requestAudioFocus()) {
                setError("Another application controls this audio route.")
                return
            }
            NativeDspBridge.setAudioStreamSource(streamPtr, 4, 440.0f)
            val started = NativeDspBridge.startAudioStream(streamPtr)
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
        if (streamPtr != 0L) NativeDspBridge.resetPcmRing(streamPtr)
        _telemetry.value = _telemetry.value.copy(mediaPositionMs = positionMs)
    }

    fun stopPlayback() {
        stopDecoder()
        if (streamPtr != 0L) {
            NativeDspBridge.stopAudioStream(streamPtr)
            NativeDspBridge.resetPcmRing(streamPtr)
        }
        focusManager.abandonAudioFocus()
        _telemetry.value = _telemetry.value.copy(isPlaying = false)
        syncKeepAlive()
    }

    fun pausePlayback() {
        decoder?.pause()
        if (streamPtr != 0L) {
            NativeDspBridge.pauseAudioStream(streamPtr)
        }
        _telemetry.value = _telemetry.value.copy(isPlaying = false)
        syncKeepAlive()
    }

    fun setSourceType(sourceType: Int, frequency: Float = 440.0f) {
        if (sourceType == 4) return
        stopDecoder()
        if (streamPtr != 0L) {
            NativeDspBridge.resetPcmRing(streamPtr)
            NativeDspBridge.setAudioStreamSource(streamPtr, sourceType, frequency)
        }
        _telemetry.value = _telemetry.value.copy(sourceType = sourceType)
    }

    private fun stopDecoder() {
        decoder?.stop()
        decoder = null
    }

    private fun restoreLastMedia() {
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
        if (enginePtr == 0L) return
        NativeDspBridge.setPowerEnabled(enginePtr, params.powerEnabled)
        NativeDspBridge.setBass(enginePtr, params.bass)
        NativeDspBridge.setClarity(enginePtr, params.clarity)
        NativeDspBridge.setAmbience(enginePtr, params.ambience)
        NativeDspBridge.setSurround(enginePtr, params.surround)
        NativeDspBridge.setDynamicBoost(enginePtr, params.dynamicBoost)
        NativeDspBridge.setVolumeLeveling(enginePtr, params.volumeLeveling)
        NativeDspBridge.setMasterGainDb(enginePtr, params.masterGainDb)
        NativeDspBridge.setBalance(enginePtr, params.balance)
        NativeDspBridge.setEqEnabled(enginePtr, params.eqEnabled)
        NativeDspBridge.setEqBandCount(enginePtr, params.eqBandCount)
        val count = params.eqBandCount
        for (i in 0 until count) {
            if (i < params.eqFrequencies.size) {
                NativeDspBridge.setEqBandFrequency(enginePtr, i, params.eqFrequencies[i])
            }
            if (i < params.eqQFactors.size) {
                NativeDspBridge.setEqBandQ(enginePtr, i, params.eqQFactors[i])
            }
            if (i < params.eqGains.size) {
                NativeDspBridge.setEqBandGain(enginePtr, i, params.eqGains[i])
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
        val clamped = value.coerceIn(range.start, range.endInclusive)
        _parameters.value = copy(_parameters.value, clamped)
        persistParams()
        if (enginePtr != 0L) native(enginePtr, clamped)
        if (session && sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
    }

    fun setPowerEnabled(enabled: Boolean) {
        _parameters.value = _parameters.value.copy(powerEnabled = enabled)
        persistParams()
        if (enginePtr != 0L) NativeDspBridge.setPowerEnabled(enginePtr, enabled)
        if (sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
        syncKeepAlive()
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
        _parameters.value = _parameters.value.copy(eqEnabled = enabled)
        persistParams()
        if (enginePtr != 0L) NativeDspBridge.setEqEnabled(enginePtr, enabled)
        if (sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
    }

    fun setEqBandGain(bandIndex: Int, gainDb: Float) {
        val currentGains = _parameters.value.eqGains.toMutableList()
        if (bandIndex in currentGains.indices) {
            val clamped = gainDb.coerceIn(-12.0f, 12.0f)
            currentGains[bandIndex] = clamped
            _parameters.value = _parameters.value.copy(eqGains = currentGains)
            persistParams()
            if (enginePtr != 0L) NativeDspBridge.setEqBandGain(enginePtr, bandIndex, clamped)
            if (sessionEffects.isAttached) sessionEffects.apply(_parameters.value)
        }
    }

    fun setEqBandFrequency(bandIndex: Int, freq: Float) {
        val freqs = _parameters.value.eqFrequencies.toMutableList()
        if (bandIndex in freqs.indices) {
            val clamped = freq.coerceIn(20.0f, 20000.0f)
            freqs[bandIndex] = clamped
            _parameters.value = _parameters.value.copy(eqFrequencies = freqs)
            persistParams()
            if (enginePtr != 0L) NativeDspBridge.setEqBandFrequency(enginePtr, bandIndex, clamped)
        }
    }

    fun setEqBandQ(bandIndex: Int, q: Float) {
        val qs = _parameters.value.eqQFactors.toMutableList()
        if (bandIndex in qs.indices) {
            val clamped = q.coerceIn(0.7f, 3.0f)
            qs[bandIndex] = clamped
            _parameters.value = _parameters.value.copy(eqQFactors = qs)
            persistParams()
            if (enginePtr != 0L) NativeDspBridge.setEqBandQ(enginePtr, bandIndex, clamped)
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
        if (enginePtr != 0L) {
            NativeDspBridge.resetAllEqBands(enginePtr)
            freqs.forEachIndexed { i, f -> NativeDspBridge.setEqBandFrequency(enginePtr, i, f) }
            qs.forEachIndexed { i, q -> NativeDspBridge.setEqBandQ(enginePtr, i, q) }
        }
    }

    fun resetToFlat() {
        applyPreset(Preset("flat_tmp", "Flat", true, DspParameters.FLAT))
    }

    fun getMagnitudeResponse(frequencies: FloatArray, outDbs: FloatArray) {
        if (enginePtr != 0L && frequencies.isNotEmpty() && frequencies.size == outDbs.size) {
            NativeDspBridge.getMagnitudeResponse(enginePtr, frequencies, outDbs, frequencies.size)
        }
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
        _telemetry.value = _telemetry.value.copy(lastError = message, isPlaying = false)
    }

    fun release() {
        persistParams(immediate = true)
        telemetryJob?.cancel()
        persistJob?.cancel()
        stopPlayback()
        sessionEffects.detach()
        try { context.unregisterReceiver(sessionReceiver) } catch (_: Exception) {}
        deviceReceiver.unregister()
        if (streamPtr != 0L) {
            NativeDspBridge.destroyAudioStream(streamPtr)
            streamPtr = 0
        }
        if (enginePtr != 0L) {
            NativeDspBridge.destroyEngine(enginePtr)
            enginePtr = 0
        }
    }
}
