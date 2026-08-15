package com.fxsound.mobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.fxsound.mobile.ui.navigation.FxAppScaffold
import com.fxsound.mobile.ui.theme.FxMobileTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as FxMobileApp
        val audioEngine = app.audioEngineController
        val presetRepo = app.presetRepository
        audioEngine.onNeedNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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

    override fun onStart() {
        super.onStart()
        (application as FxMobileApp).audioEngineController.setForegroundVisible(true)
    }

    override fun onStop() {
        (application as FxMobileApp).audioEngineController.setForegroundVisible(false)
        super.onStop()
    }
}
