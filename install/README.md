# FxMobile Downloads

This directory stores pre-built Android APK installer packages for **FxMobile**. Host enhancer CLIs for Windows, macOS, and Linux are built in CI and attached to [GitHub Releases](https://github.com/Lawliet2004/fxmobile/releases) next to the APKs (they are not checked into this folder).

## Android APK

### Available Installers

| File Name | Version | Release Status | Target Platforms | Direct Release Download |
|:---|:---|:---|:---|:---|
| [`FxSound-1.1.1-rust.apk`](FxSound-1.1.1-rust.apk) | **v1.1.1** | **Latest (Stable)** | `arm64-v8a`, `armeabi-v7a`, `x86_64` | [Download v1.1.1](https://github.com/Lawliet2004/fxmobile/releases/download/v1.1.1/FxSound-1.1.1-rust.apk) |
| [`FxSound-1.1.0-rust.apk`](FxSound-1.1.0-rust.apk) | **v1.1.0** | **Previous** | `arm64-v8a`, `armeabi-v7a`, `x86_64` | [Download v1.1.0](https://github.com/Lawliet2004/fxmobile/releases/download/v1.1.0/FxSound-1.1.0-rust.apk) |

### Installation Guide
1. Transfer the `.apk` file to your Android device running **Android 8.0 (API 26)** or newer.
2. Allow installation from unknown sources in device settings if prompted.
3. Install and launch the application.

All official release builds are also hosted on the [FxMobile GitHub Releases Page](https://github.com/Lawliet2004/fxmobile/releases).

---

## Host enhancer (Windows / macOS / Linux)

The Android APK (Jetpack Compose GUI + Oboe I/O) **cannot run on desktop**. The downloadable host package is a CLI around the shared Rust DSP crate `native/fxengine` — the same engine linked into the APK, not a Compose Multiplatform port.

Each GitHub Release attaches these artifacts alongside the APKs:

| Platform | Release artifact | Direct download (latest) |
|:---|:---|:---|
| Windows x64 | `fxengine-windows-x64.exe` | [fxengine-windows-x64.exe](https://github.com/Lawliet2004/fxmobile/releases/latest/download/fxengine-windows-x64.exe) |
| macOS x64 | `fxengine-macos-x64` | [fxengine-macos-x64](https://github.com/Lawliet2004/fxmobile/releases/latest/download/fxengine-macos-x64) |
| Linux x64 | `fxengine-linux-x64` | [fxengine-linux-x64](https://github.com/Lawliet2004/fxmobile/releases/latest/download/fxengine-linux-x64) |

### How to run

**Windows** (PowerShell or cmd):

```bat
fxengine-windows-x64.exe --help
fxengine-windows-x64.exe --smoke
```

**macOS / Linux:**

```bash
chmod +x fxengine-macos-x64   # or fxengine-linux-x64
./fxengine-linux-x64 --help
./fxengine-linux-x64 --smoke
```

`--help` / `-h` (or no arguments) prints usage, version, platforms, and the smoke flag. `--smoke` creates a real `Engine`, processes a short stereo sine, and prints numeric `peak` / `rms` (must not be empty). `--version` / `-V` prints `fxengine <version>`.
