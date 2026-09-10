# Build & Compilation Guide

## 1. System Requirements
- **JDK 17** (e.g. Eclipse Temurin or OpenJDK 17)
- **Android SDK** (API level 34, Build-Tools 34.0.0)
- **Android NDK** (`26.1.10909125` or compatible)
- **CMake** (`3.22.1`)
- **Gradle 8.3+**
- **Rust stable** (for `native/fxengine` tests, Android `staticlib`, and the host CLI)

---

## 2. Command-Line Build Instructions

### Assemble Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Assemble Production Release APK (Minified + Shrinked)
```bash
./gradlew assembleRelease
```
Output: ABI-split APKs under `app/build/outputs/apk/release/` (`app-arm64-v8a-release.apk`, `app-armeabi-v7a-release.apk`, `app-x86_64-release.apk`)

### Run Unit Tests
```bash
./gradlew test
```

### Run Rust DSP Tests
```bash
cargo test --manifest-path native/fxengine/Cargo.toml
```

### Host CLI (Windows / macOS / Linux)

The Android APK does not run on desktop. Build the host-runnable CLI around `native/fxengine` (real `Engine`, not a stub):

```bash
cargo build --release --bin fxengine --manifest-path native/fxengine/Cargo.toml
```

Output: `native/fxengine/target/release/fxengine` (`.exe` on Windows). The release profile keeps `panic = abort`.

```bash
# no args / --help / -h  → usage (version, platforms, smoke flag)
./native/fxengine/target/release/fxengine --help

# --version / -V
./native/fxengine/target/release/fxengine --version

# --smoke  → Engine::new(48000, 512), bass/clarity, process_interleaved, numeric peak/rms
./native/fxengine/target/release/fxengine --smoke
```

CI builds this binary on Ubuntu, Windows, and macOS and attaches `fxengine-linux-x64`, `fxengine-windows-x64.exe`, and `fxengine-macos-x64` to GitHub Releases next to the APKs.
