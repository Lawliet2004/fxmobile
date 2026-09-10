package com.fxsound.mobile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import com.fxsound.mobile.ui.navigation.FxAppScaffold
import com.fxsound.mobile.ui.theme.FxMobileTheme
import com.fxsound.mobile.audio.AudioEngineController

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val app = application as FxMobileApp
        val audioEngine = app.audioEngineController
        val presetRepo = app.presetRepository
        audioEngine.onNeedNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleIntent(intent, audioEngine)

        setContent {
            FxMobileTheme {
                FxAppScaffold(
                    audioController = audioEngine,
                    presetRepository = presetRepo
                )
                LaunchedEffect(Unit) {
                    reportFullyDrawn()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val audioEngine = (application as FxMobileApp).audioEngineController
        handleIntent(intent, audioEngine)
    }

    private fun handleIntent(intent: Intent?, audioEngine: AudioEngineController) {
        val data: Uri? = intent?.data
        if (data != null) {
            audioEngine.openAudioFile(data)
        }
    }

    override fun onStart() {
        super.onStart()
        (application as FxMobileApp).audioEngineController.setForegroundVisible(true)
    }

    override fun onStop() {
        (application as FxMobileApp).audioEngineController.setForegroundVisible(false)
        super.onStop()
    }
}
