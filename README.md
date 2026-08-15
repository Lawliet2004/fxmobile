# FxMobile — Ultra-Lightweight Mobile Audio Enhancer

[![CI Build & Test](https://github.com/Lawliet2004/fxmobile/actions/workflows/build.yml/badge.svg)](https://github.com/Lawliet2004/fxmobile/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/Lawliet2004/fxmobile?label=Release&color=blue)](https://github.com/Lawliet2004/fxmobile/releases)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen.svg)](https://developer.android.com/about/versions/oreo)
[![Footprint](https://img.shields.io/badge/Release%20APK-3.72%20MB%20(arm64)-success.svg)](install/)

**FxMobile** is a high-fidelity native Android audio enhancement application inspired by the open-source **FxSound** desktop project ([fxsound2/fxsound-app](https://github.com/fxsound2/fxsound-app)).

Android does not permit intercepting all global third-party application audio streams without root or special system capabilities. The low-latency C++ / Rust engine processes files and test signals played **inside this application**. An optional player-session mode attaches platform `AudioEffect` sessions when a compatible music player publishes an audio session ID.

Built from the ground up for extreme performance, zero-latency DSP, offline privacy, and ultra-lightweight resource utilization:
- **Rust DSP Core**: `native/fxengine` — no UI, no Android framework types, preallocated real-time processing (`panic=abort`).
- **Low-Latency I/O**: C++ Google Oboe / AAudio (OpenSL ES fallback).
- **Native Android UI**: Built with Jetpack Compose following FxSound’s iconic dark aesthetic.
- **Offline-First & Private**: Zero cloud dependencies, zero analytics/ad trackers, zero internet permissions required.
- **Ultra-Small Binary Size**: Target installed size of **3–4 MB** via R8 full-mode optimization, ABI splitting, and native symbol stripping.

---

## 📦 Releases & APK Installers

Pre-built APK installers are maintained directly in the repository under [`install/`](install/) and published to [GitHub Releases](https://github.com/Lawliet2004/fxmobile/releases).

| Version Tag | Release Status | Target Architecture | APK Installer Package | Size | Notes |
|:---|:---|:---|:---|:---|:---|
| **`v1.1.1`** | **Latest (Stable)** | Universal / Multi-ABI | [`FxSound-1.1.1-rust.apk`](install/FxSound-1.1.1-rust.apk) | ~22.8 MB | Full Rust DSP core integration, refined EQ Bezier curve, dynamic boost |
| **`v1.1.0`** | **Previous** | Universal / Multi-ABI | [`FxSound-1.1.0-rust.apk`](install/FxSound-1.1.0-rust.apk) | ~22.0 MB | Initial release with embedded Rust DSP engine |

### How to Install on Android
1. Download the latest APK installer from the table above or the [GitHub Releases](https://github.com/Lawliet2004/fxmobile/releases) tab.
2. Open the downloaded `.apk` file on your Android device (Android 8.0 Oreo / API 26 or newer).
3. If prompted, enable **"Install unknown apps"** or **"Allow from this source"** in your device settings.
4. Complete the installation and launch **FxMobile**.

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────┐
│             Android UI (Jetpack Compose)               │
│   Home  •  Parametric EQ  •  Presets  •  Diagnostics   │
└──────────────────────────┬─────────────────────────────┘
                           │ StateFlow / Control API
┌──────────────────────────▼─────────────────────────────┐
│          Audio Engine Manager (Kotlin / JNI)           │
│   Device Routing • Focus Management • Telemetry Poll   │
└──────────────────────────┬─────────────────────────────┘
                           │ JNI (Lock-Free Atomics)
┌──────────────────────────▼─────────────────────────────┐
│            Native DSP Engine Core (Rust)               │
│   DC Filter  →  Bass  →  EQ  →  Clarity  →  Dynamics   │
│   →  Ambience  →  Surround  →  Leveler  →  Limiter     │
└──────────────────────────┬─────────────────────────────┘
                           │ Real-Time Audio Callback
┌──────────────────────────▼─────────────────────────────┐
│              Google Oboe Audio Layer (AAudio)          │
│   Bluetooth (A2DP/LE) • USB-C DAC • Headphones • Phone │
└────────────────────────────────────────────────────────┘
```

---

## Core Feature Set

### 1. Master Power
- Instant ON/OFF power control with click-free 15 ms exponential crossfading to eliminate pops and clicks.

### 2. Five Core FxSound Effects (0.0 to 10.0)
1. **Clarity / Fidelity**: High-band harmonic exciter and transient enhancer (4 kHz–12 kHz) for vocal crispness without harshness.
2. **Bass Boost**: Tuned low-shelf filter + non-linear 2nd/3rd harmonic generator with DC blocker, translating rich sub-bass to mobile phone speakers and earbuds.
3. **Ambience**: Multi-tap prime-length delay reflection network with high-frequency damping for natural acoustic room presence.
4. **Surround**: Mid/Side psychoacoustic stereo widener with **100% mono compatibility guarantee** (mono downmix cancels side processing completely).
5. **Dynamic Boost**: Upward compressor reducing crest factor to increase perceived loudness and punch without digital distortion.

### 3. Parametric Equalizer
- Configurable band counts: **5, 9, 10, 15, 20, or 31 bands** (default 10-band ISO octave: 31 Hz to 16 kHz).
- Gain range: **-12 dB to +12 dB**, Q: **0.7 to 3.0**.
- Robert Bristow-Johnson (RBJ) Direct Form II Transposed biquad filters.
- Real-time interactive Bezier response curve on Compose Canvas.

### 4. Output Protection & Dynamics
- **Volume Leveler**: Slow-acting RMS envelope follower (350 ms) to smooth out track-to-track volume disparities.
- **Master Gain**: -20 dB to +20 dB with equal-power stereo balance law.
- **Safety Limiter**: Lookahead soft-knee peak limiter preventing digital clipping at $0\text{ dBFS}$ under high boost.

### 5. Preset System
- Compact versioned JSON preset schema.
- Factory presets: **Music**, **Movie**, **Gaming**, **Voice / Podcast**, **Bass Boost**, and **Flat (Neutral)**.
- User custom presets: Create, Save, Rename, Delete, and Export/Import.

---

## Performance & Binary Size Metrics

| Metric | Target | Measured Result | Status |
|---|---|---|---|
| **Base APK Size (arm64-v8a)** | < 6.0 MB (prefer 3–4) | Measure with `assembleRelease` | See [docs/PERFORMANCE.md](docs/PERFORMANCE.md) |
| **DSP Callback Duration** | << 5.33 ms | Native suite (host) | See [docs/TESTING.md](docs/TESTING.md) |
| **Idle CPU (Paused)** | Near zero | Telemetry sleeps at 2 Hz when idle | — |
| **Active DSP** | Below callback deadline | Native bench | See [docs/PERFORMANCE.md](docs/PERFORMANCE.md) |

---

## Building the Project

### Prerequisites
- **JDK 17**
- **Android SDK** (API 34, Build-Tools 34.0.0)
- **Android NDK** (`26.1.10909125` or newer)
- **CMake** (`3.22.1` or newer)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/Lawliet2004/fxmobile.git
cd fxmobile

# Rust DSP tests (host)
cargo test --manifest-path native/fxengine/Cargo.toml

# Run Android Unit Tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Production Release APK (Minified + Shrinked + LTO)
./gradlew assembleRelease
```

---

## Documentation
- [Architecture & Threading Model](docs/ARCHITECTURE.md)
- [DSP Algorithms & Mathematics](docs/DSP.md)
- [Audio Pipeline & Platform Routing](docs/AUDIO_PIPELINE.md)
- [Performance & Binary Size Optimization](docs/PERFORMANCE.md)
- [Testing & Validation Guide](docs/TESTING.md)
- [Build Instructions](docs/BUILD.md)
- [FxSound Desktop Analysis](docs/FXSOUND_ANALYSIS.md)
- [Licenses & Attribution](docs/LICENSES.md)

---

## License
FxMobile is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)** in adherence to the upstream [FxSound](https://github.com/fxsound2/fxsound-app) license.
