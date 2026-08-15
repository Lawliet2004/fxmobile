# Architecture & Threading Model

## 1. Architectural Philosophy
FxMobile adheres to strict architectural boundaries separating real-time audio computation from visual rendering and operating system lifecycles.

```
┌──────────────────────────────────────────────────────────┐
│                   UI Thread (Jetpack Compose)            │
│   • Renders 60/120 FPS UI                                │
│   • Dispatches state updates via Kotlin StateFlow        │
│   • Throttled telemetry collection (25–30 Hz)            │
└────────────────────────────┬─────────────────────────────┘
                             │ Kotlin Coroutine Flows
┌────────────────────────────▼─────────────────────────────┐
│                 Control / Service Thread                 │
│   • AudioFocusManager (Ducking & Interruptions)          │
│   • AudioDeviceReceiver (Bluetooth / USB Route changes)  │
│   • PresetRepository (JSON storage & schema)             │
└────────────────────────────┬─────────────────────────────┘
                             │ JNI Boundary (Atomics)
┌────────────────────────────▼─────────────────────────────┐
│     Real-Time Audio Thread                                │
│   C++ Oboe callback (AAudio / OpenSL ES)                  │
│     → Rust fxengine process (no UI, no alloc)             │
│   • Pre-allocated scratch / delay lines                   │
│   • Panic=abort, no heap growth on the callback           │
└──────────────────────────────────────────────────────────┘
```

---

## 2. Why Rust DSP + C++ Oboe

Android’s reliable low-latency path is still **Oboe (C++)**. A full-Rust audio I/O stack would re-fight OEM AAudio/OpenSL quirks.

The **enhancement math** does not need Android headers, so it lives in `native/fxengine`:

```
Kotlin UI  →  JNI  →  C++ Oboe I/O  →  Rust C ABI (fx_engine_process_*)
```

Kotlin, file decode, presets, and Mode B session effects are unchanged. Only the sample processor behind `DspEngine` moved.

iOS can later link the same `libfxengine.a` from Swift/ObjC++.

---

## 3. JNI Boundary & Memory Strategy
1. Rust allocates delay lines and scratch once in `Engine::new` / `prepare`.
2. Parameter writes from Kotlin stay primitive JNI calls into the Rust engine.
3. Meters are read at ~30 Hz on a non-audio thread.
