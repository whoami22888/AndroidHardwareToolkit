# Codex build directive

Continue development from the current `main` branch. Treat the existing implementation as the baseline and preserve working behavior.

## Primary objective
Build Android Hardware Toolkit as a real phone-first hardware toolkit. Use every hardware capability that the Android handset actually exposes through supported Android APIs. External USB/BLE hardware must extend capabilities the handset physically lacks, not replace native capabilities.

## Rules
- Detect native hardware at runtime; never assume a capability exists.
- Prefer Android public APIs/HAL-backed interfaces for native NFC, BLE, Consumer IR, USB Host and other exposed hardware.
- Do not emulate missing RF hardware in software.
- Do not claim Sub-GHz or 125/134.2-kHz LF RFID works natively unless the handset actually exposes suitable physical RF hardware.
- Unknown external USB devices remain generic until a concrete, documented driver identifies them.
- Concrete external drivers must use documented protocols and real device responses; no fabricated telemetry.
- Keep operations scoped to owned/authorized equipment and interoperability testing.
- Preserve the provider abstraction so native and external providers can coexist.
- Add tests for provider discovery, capability gating, disconnect/error handling, and driver selection.
- Keep the application installable on minSdk 26 and compatible with compileSdk/targetSdk 36.
- Run unit tests and a release/debug build in CI. Fix all compile/test failures before considering a milestone complete.

## Next implementation milestones
1. Expand `NativeHardwareDetector` and provider capabilities for hardware genuinely exposed by Android.
2. Give native providers concrete operation services where Android permits them, beginning with NFC, BLE and Consumer IR.
3. Add lifecycle-safe permission/state handling and refresh native provider readiness when hardware state changes.
4. Keep Sub-GHz and LF RFID as first-class modules with `PHONE_NATIVE` support available only when a real compatible native controller is detected, otherwise use external adapter providers.
5. Add documented adapter drivers only after exact hardware model/VID/PID and protocol are known.
6. Add instrumentation/manual-test documentation for physical Android hardware validation.
7. Maintain CI as a required quality gate and upload a debug APK artifact for successful builds.

## Definition of done
No placeholder claims of hardware support, no fake scan/capture data, no silent capability fallbacks. Every displayed capability must map to a detected provider and an implemented operation path or clearly state that a driver/API/permission is still required.
