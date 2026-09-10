# Contributing to FxMobile

Thanks for your interest in FxMobile! This document covers the
ground rules for contributing code, tests, and documentation.

## Code of conduct

Be respectful. Assume good faith. Focus on the technical merits of
the change. Harassment of any kind is not tolerated.

## Reporting issues

- Search the issue tracker first to avoid duplicates.
- Include the device model, Android version, and a short description
  of how to reproduce.
- For audio-quality bugs, attach a short clip and the preset you
  were using.
- For crashes, attach `adb logcat` output filtered to the app's PID.

## Development setup

1. Install Android Studio (Hedgehog or newer) with the NDK and CMake
   components (NDK 26.1.10909125 and CMake 3.22.1 are pinned in CI).
2. Install the Rust toolchain with the three Android targets:
   ```sh
   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
   ```
3. Clone the repository and open the project root in Android Studio.
   The first build will download the AGP and Kotlin plugins.
4. Use `./gradlew test` for the unit tests and
   `./gradlew :app:assembleDebug` for an installable debug APK.

## Code style

- Kotlin: official style (`kotlin.code.style=official` in
  `gradle.properties`). Run the IDE formatter before committing.
- Rust: standard `cargo fmt` and `cargo clippy --all-targets -- -D warnings`.
- Compose: prefer unidirectional data flow; UI state should be
  derived from `StateFlow`s on the `AudioEngineController` and
  `PresetRepository`, not held in `remember { mutableStateOf(...) }`
  inside leaf composables unless truly local.

## Testing

- Pure-logic unit tests live in `app/src/test/java/` and run on the
  JVM (Robolectric where Android context is required).
- Compose UI tests live in `app/src/androidTest/java/` and run on a
  device or emulator.
- Rust tests live under `native/fxengine/` and run with
  `cargo test --manifest-path native/fxengine/Cargo.toml`.

## Pull requests

1. Fork the repository and create a topic branch.
2. Make your change. Add tests for any new behaviour.
3. Run the full test suite locally:
   ```sh
   ./gradlew test
   cargo test --manifest-path native/fxengine/Cargo.toml
   ```
4. Open a pull request. The CI pipeline will run unit tests, lint,
   clippy, and build the release APK. Address any failures before
   requesting review.
5. Sign off your commits (`git commit -s`) per the Developer
   Certificate of Origin.

## Release process

Tag-driven. Pushing a tag of the form `vX.Y.Z` triggers the
`.github/workflows/release.yml` pipeline, which:

1. Builds the host CLI for Linux, Windows, and macOS.
2. Builds the Android APK and AAB.
3. Bundles the R8 mapping file as `mapping-<version>.txt`.
4. Creates a GitHub release with all of the above as assets.

## License

By contributing, you agree that your contributions will be licensed
under the same terms as the project (see `LICENSE`).
