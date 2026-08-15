# Audio Pipeline & Platform Routing

## 1. Real-Time Native Audio I/O (Google Oboe)
FxMobile uses Google's **Oboe** library to access the lowest possible latency audio paths on Android:
- **AAudio Engine** on Android 8.0+ (API 26+): High-performance shared/exclusive memory-mapped audio ring buffers directly interfaced with the Linux kernel ALSA/hardware endpoints.
- **OpenSL ES Fallback** on legacy hardware or devices where AAudio is disabled by OEM policy.
- Format: `AudioFormat::Float` (32-bit floating point PCM) to eliminate redundant integer-to-float conversions.

---

## 2. Audio Routing Lifecycle & Hardware Endpoints

```
[ Output Device Detection ]
         │
         ├── Bluetooth (SBC / AAC / aptX / LDAC / LE Audio)
         ├── Wired Headphones (3.5 mm Jack)
         ├── USB-C Digital Audio Interface / External DAC
         └── Internal Loudspeaker
```

- **AudioDeviceReceiver**: Listens for device change broadcasts and registers `AudioDeviceCallback`.
- **Automatic Stream Reconnection**: When an audio route disconnects (e.g. user unplugs headphones or powers off Bluetooth earbuds), `onErrorAfterClose` triggers an immediate clean restart without crashing or dropping DSP state.

---

## 3. Android Platform Operating Modes & Limitations

> [!IMPORTANT]
> **Android Platform Reality**
> Android sandbox security prevents ordinary applications from intercepting global audio from third-party apps without system root or custom OEM firmware.

FxMobile addresses this honestly through two operating modes:

1. **Mode A — Internal engine (primary)**  
   SAF-picked local audio files and the Settings test generator are decoded (or synthesized) and processed by the Rust `fxengine` through Oboe. This is the only path with the full effect chain.

2. **Mode B — Player session (limited)**  
   When a player broadcasts `OPEN_AUDIO_EFFECT_CONTROL_SESSION`, FxMobile attaches `DynamicsProcessing` (API 28+) or `Equalizer` + `BassBoost`, plus `LoudnessEnhancer` for dynamic/master gain. EQ, bass, dynamic boost, and master gain are mapped. Ambience, surround, clarity harmonics, and the custom limiter do **not** run on other apps. YouTube / Chrome typically never publish a session.

There is no Mode C / system-wide enhancement. Root and undocumented OEM hooks are out of scope.

### Honest Home status

| State | Banner |
|---|---|
| Internal engine, idle | Enhancement applies to audio you open here. |
| Internal engine, playing a file | Processing this file. |
| Player session, attached | Limited EQ on [session]. |
| Player session, waiting | Waiting for a player that publishes a session. YouTube usually will not. |

Factory presets match desktop FxSound `.fac` values (Volume Boost is Dynamic Boost 8.1, not master gain). They only change sound on the path above.

A foreground service stays up in player-session mode whenever power is on, even before a session attaches, so the OPEN broadcast can be received.
