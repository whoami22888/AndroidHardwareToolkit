# Android Hardware Toolkit

v0.8.0 source bundle: Android-first hardware capability and external-adapter architecture.

## Scope

The toolkit uses Android phone hardware where available and supports external hardware through explicit provider/driver interfaces. It does not pretend unsupported radios exist in the phone.

- NFC / BLE / IR / USB Host foundations
- USB device enumeration and Android permission lifecycle
- BLE scanning and GATT-oriented foundation
- Persistent local JSON session store
- Provider registry and capability gating
- External Sub-GHz and LF RFID (125/134.2 kHz) driver interfaces

## Important hardware limitation

A normal Android phone cannot become a Sub-GHz or 125/134.2 kHz RFID radio through software alone. Those capabilities require compatible physical external hardware unless the target device exposes the required radio hardware.

The current release deliberately does not falsely identify arbitrary USB devices as radios. Concrete Sub-GHz/LF-RFID drivers must be implemented for a specific adapter using its documented USB/BLE protocol.

## Build

Open this project in Android Studio with an installed Android SDK. Then run:

```text
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Version

0.8.0

## Status

Architecture and source-level validation completed for the v0.8 bundle. Physical hardware testing and APK compilation require an Android build environment and compatible test hardware.
