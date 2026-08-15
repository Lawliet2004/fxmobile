package com.fxsound.mobile

import android.app.Application
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.core.settings.SettingsRepository

class FxMobileApp : Application() {

    lateinit var presetRepository: PresetRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var audioEngineController: AudioEngineController
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        presetRepository = PresetRepository(this)
        audioEngineController = AudioEngineController(this, settingsRepository)
        val restored = settingsRepository.loadParameters() ?: presetRepository.activePreset.value.parameters
        audioEngineController.applyInitialState(restored)
    }

    override fun onTerminate() {
        super.onTerminate()
        audioEngineController.release()
    }
}
