# Android Hardware Toolkit

v0.9.0 source bundle: Android-first hardware capability and external-adapter architecture.

## Scope

The toolkit uses Android phone hardware where available and supports external hardware through explicit provider/driver interfaces. It does not pretend unsupported radios exist in the phone.

- NFC / BLE / IR / USB Host foundations
- USB device enumeration, permission lifecycle, and hot-plug (attach/detach) tracking
- BLE scanning and GATT-oriented foundation
- Persistent local JSON session store
- Provider registry and capability gating
- External Sub-GHz and LF RFID (125/134.2 kHz) driver interfaces

## Important hardware limitation

A normal Android phone cannot become a Sub-GHz or 125/134.2 kHz RFID radio through software alone. Those capabilities require compatible physical external hardware unless the target device exposes the required radio hardware.

The current release deliberately does not falsely identify arbitrary USB devices as radios. Concrete Sub-GHz/LF-RFID drivers must be implemented for a specific adapter using its documented USB/BLE protocol.

## Build

Open this project in Android Studio with an installed Android SDK, or run:

```text
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Run the unit-test and lint gates locally:

```text
./gradlew testDebugUnitTest lintDebug
```

## Verified toolchain

- Gradle wrapper 9.7.1 (AGP 9.1.1 requires Gradle ≥ 9.3.1)
- Android Gradle Plugin 9.1.1 with **built-in Kotlin** (`android.builtInKotlin=true`); the standalone
  `org.jetbrains.kotlin.android` plugin is intentionally absent because it conflicts with AGP 9
- `org.jetbrains.kotlin.plugin.compose` 2.2.10 (required whenever Compose is enabled on Kotlin 2.x)
- compileSdk 37 (Compose UI 1.12.x requires compiling against API 37), targetSdk 37, minSdk 26
- Tests: `testDebugUnitTest` (15 tests), lint: `lintDebug` with `warningsAsErrors=true`

## CI

GitHub Actions (`.github/workflows/android.yml`) runs unit tests, lint, and debug/release assembly
on every push/PR to `main` and uploads the debug APK as an artifact.

## Version

0.9.0

## Status

The application has a real native/external capability architecture, lifecycle-safe BLE permission handling, live BLE scanning UI, USB enumeration/permission/hot-plug handling, provider capability gating, and persistent session storage. Sub-GHz and LF RFID remain driver-dependent: no unsupported radio is simulated. Physical hardware validation remains required for hardware-specific operation — see `docs/MANUAL_TESTING.md`.
