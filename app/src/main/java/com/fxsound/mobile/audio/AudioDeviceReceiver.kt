package com.fxsound.mobile.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat

class AudioDeviceReceiver(
    private val context: Context,
    private val onDeviceChanged: (String, Boolean) -> Unit,
    private val onBecomingNoisy: () -> Unit = {}
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isRegistered = false

    /**
     * The last reported output device. Public so [AudioEngineController] can check whether a
     * wired device is currently connected when it wants to restore playback after a
     * `BECOMING_NOISY` pause.
     */
    @Volatile var activeOutputDevice: String = "Built-in Speaker"
        private set

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                onBecomingNoisy()
            }
            updateCurrentDevice()
        }
    }

    private val deviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                updateCurrentDevice()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                updateCurrentDevice()
            }
        }
    } else null

    fun register() {
        if (isRegistered) return

        // System-side broadcasts from the audio framework (headset plug and the
        // protected "becoming noisy" signal) are delivered even to non-exported
        // receivers, but we keep the noisy one exported to match historical
        // behaviour and to be explicit about it being a system-only stream.
        val systemFilter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            systemFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Bluetooth profile connection events come from another UID. Modern
        // Android still allows them to be delivered to a non-exported receiver
        // because they are protected broadcasts, and we want the tightest
        // possible surface for arbitrary in-process broadcasts.
        val bluetoothFilter = IntentFilter().apply {
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        }
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            bluetoothFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
        }

        isRegistered = true
        updateCurrentDevice()
    }

    fun unregister() {
        if (!isRegistered) return

        // The same receiver instance is registered twice (once for the system
        // stream, once for the BT profile stream), so unregister it twice to
        // fully detach.
        try { context.unregisterReceiver(broadcastReceiver) } catch (_: Exception) {}
        try { context.unregisterReceiver(broadcastReceiver) } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        }

        isRegistered = false
    }

    /**
     * Returns true if a wired audio output (3.5mm headphones or USB DAC) is currently connected.
     * Used to decide whether playback should be auto-resumed after a transient `BECOMING_NOISY`
     * pause.
     */
    fun hasWiredOutput(): Boolean {
        val name = activeOutputDevice
        return name.startsWith("Wired") || name.startsWith("USB")
    }

    fun updateCurrentDevice() {
        var deviceName = "Built-in Speaker"
        var isBluetooth = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            dev.productName.toString()
                        } else "Bluetooth Audio"
                        deviceName = "Bluetooth: $name"
                        isBluetooth = true
                        break
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        deviceName = "Wired Headphones (3.5mm)"
                    }
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        deviceName = "USB Audio DAC"
                    }
                }
            }
        }

        activeOutputDevice = deviceName
        onDeviceChanged(deviceName, isBluetooth)
    }
}
