# Security policy

## Supported versions

FxMobile is distributed as a single Android application. Security
fixes are applied to the latest tagged release. Older tags are not
back-patched unless the maintainers explicitly say otherwise.

| Version | Supported          |
| ------- | ------------------ |
| latest  | :white_check_mark: |
| older   | :x:                |

## Reporting a vulnerability

**Do not open a public issue for security bugs.** Email the
maintainers at the address listed in the latest GitHub release
notes (or, for private disclosure, open a
[GitHub Security Advisory](https://github.com/<owner>/<repo>/security/advisories/new)).

Please include:

- A clear description of the issue and the attack scenario.
- The device model, Android version, and FxMobile version.
- Reproduction steps and a sample input file if applicable.
- A rough estimate of impact (data leak, RCE, DoS, ...).

You can expect an acknowledgement within 72 hours and a fix or
mitigation plan within 30 days, depending on severity.

## Hardening notes

FxMobile is built with the following security defaults:

- `android:allowBackup="false"`: app data is not included in cloud
  or device-to-device backups.
- `android:usesCleartextTraffic="false"` and a default-deny
  `network_security_config.xml`: cleartext HTTP is refused
  everywhere.
- `android:dataExtractionRules` and `android:fullBackupContent`:
  the `fxsound_presets` and `fxsound_settings` SharedPreferences
  (which hold user presets and the last media URI) are excluded
  from both cloud backup and device transfer.
- `minSdk = 26` (Android 8.0) so the cryptographic and sandbox
  guarantees of modern Android are always in effect.
- ProGuard/R8 rules keep JNI bindings, R$* ids, Oboe entry points,
  and kotlinx.coroutines internals from being silently renamed.
- The release build type enables R8 full mode and resource
  shrinking, with `lint { abortOnError = true; warningsAsErrors = true }`.

If you find a way to bypass any of these defaults, please report it
privately as described above.
