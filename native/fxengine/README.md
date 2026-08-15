# fxengine (Rust)

Real-time DSP for FxMobile. No UI, no Android, no heap growth in `process`.

```
Kotlin Compose
    │ JNI
C++ Oboe (AAudio / OpenSL ES)   ← owns the device + callback
    │ C ABI  fx_engine_process_interleaved
Rust fxengine                   ← EQ, effects, limiter, meters
```

## Why this split

- **Oboe stays C++** because that is the supported low-latency path on OEM Android.
- **DSP is Rust** so the audio math is isolated, testable on a PC (`cargo test`), and reusable later on iOS as `libfxengine.a`.

## Build

Host tests:

```bash
cargo test --manifest-path native/fxengine/Cargo.toml
```

Android is built automatically from `app/src/main/cpp/CMakeLists.txt` (cargo cross-compile per ABI, then linked into `libfxmobile_dsp.so`).
