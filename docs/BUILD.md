# Build & Compilation Guide

## 1. System Requirements
- **JDK 17** (e.g. Eclipse Temurin or OpenJDK 17)
- **Android SDK** (API level 34, Build-Tools 34.0.0)
- **Android NDK** (`26.1.10909125` or compatible)
- **CMake** (`3.22.1`)
- **Gradle 8.3+**

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
