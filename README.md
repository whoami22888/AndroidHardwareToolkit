# Android Hardware Toolkit

v0.9.0 source bundle: Android-first hardware capability and external-adapter architecture.

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

0.9.0

## Status

The application has a real native/external capability architecture, lifecycle-safe BLE permission handling, live BLE scanning UI, USB enumeration/permission handling, provider capability gating, and persistent session storage. Sub-GHz and LF RFID remain driver-dependent: no unsupported radio is simulated. Physical hardware validation remains required for hardware-specific operation.
