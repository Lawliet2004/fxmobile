# Changelog

All notable changes to FxMobile are documented in this file. The format
is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Dates are in ISO-8601 (YYYY-MM-DD). Versions are tags of the form `vX.Y.Z`.

## [Unreleased]

### Added
- Audio-layer unit tests covering `setFloat` clamping, NaN/Inf rejection,
  preset apply, and `setPowerEnabled` state updates.
- `FakeNativeDspBridge` test double recording all native calls.
- Compose UI smoke test for the Home screen under `androidTest`.
- `PresetRepository` migration test (v1 single-preset JSON → v2 list).
- `PresetRepository` import-suffix test for name collisions.
- `SessionEffectMapping` property-based clamping tests.
- `EqBandTables` property-based `remapGains` tests with random band counts.
- `AudioFocusManager` test covering focus request, abandon, and
  duck / loss / gain callbacks.
- `PresetPack` round-trip tests for v2 array form and v1 fallback.
- CI: cache Gradle (setup-java), cache cargo (Swatinem/rust-cache),
  add dedicated `lint` job running `cargo clippy -- -D warnings`,
  cancel-in-progress concurrency for the build and release workflows.
- Release workflow: build AAB via `:app:bundleRelease`, upload the
  R8 mapping file as `mapping-<version>.txt`, upload the AAB artifact.
- Gradle: version catalog (`gradle/libs.versions.toml`).
- Gradle: `buildConfig = true` and `signingConfigs.release` reading from
  `signing.properties` (falling back to debug signing with a warning).
- Gradle: `bundle { abi { enableSplit = true } }` and a release-only
  `lint { abortOnError = true; warningsAsErrors = true }` block.
- ProGuard keep rules for `NativeDspBridge`, `external fun` JNI methods,
  `R$*`, Oboe, kotlinx.coroutines volatile fields, with
  `keepattributes SourceFile,LineNumberTable`.
- Security: `allowBackup="false"`, `usesCleartextTraffic="false"`,
  `networkSecurityConfig`, `fullBackupContent` and `dataExtractionRules`
  that exclude `fxsound_presets` and `fxsound_settings` from cloud
  backup and device transfer.
- Dependabot weekly updates for `gradle`, `cargo`, and `github-actions`,
  grouped by patch/minor with a cap of 10 open PRs.

### Changed
- `gradle.properties`: bumped heap to 4g, added `org.gradle.caching=true`
  and `kotlin.code.style=official`.
- `app/build.gradle.kts`: migrated to version catalog and added
  Robolectric + Compose test dependencies.

## History (from `git log --oneline`)

```
d446213 docs: add installer documentation and release asset download links
a4d7e10 feat: initial commit for FxMobile native audio enhancer
```

## [0.1.0] - initial public commit

First public release of FxMobile. See the commit `a4d7e10` for the
initial feature set (Rust DSP engine, Oboe/AAudio backend, Compose
UI, built-in and user presets, parametric 10-band EQ, session-effect
mode, foreground service for media playback).
