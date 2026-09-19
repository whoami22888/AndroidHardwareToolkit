# Manual hardware validation

Per `CODEX_BUILD.md` milestone 6: every displayed capability must be confirmed on physical
hardware before it is reported as working. Record results here.

## Native capability matrix (fill in per device)

| Device / Android version | NFC read | NFC HCE | BLE scan | BLE connect | IR blaster (ConsumerIrManager) | USB Host / OTG |
|--------------------------|----------|---------|----------|-------------|-------------------------------|----------------|
| _(device 1)_             | ☐        | ☐       | ☐        | ☐           | ☐                             | ☐              |
| _(device 2)_             | ☐        | ☐       | ☐        | ☐           | ☐                             | ☐              |

## Procedure per capability

1. **NFC** — Enable NFC in system settings; verify the Dashboard shows the NFC provider as
   `ready=true` (not merely present). Read a known MIFARE/NTAG card and log the UID into the
   session store, then export `sessions.json` and attach it to the test report.
2. **BLE** — With location/BLUETOOTH_SCAN granted, start a scan with at least one known
   peripheral advertising nearby; confirm its address/RSSI appear, then stop the scan and
   confirm `SCAN_MODE` stops (no battery drain via `adb shell dumpsys bluetooth_manager`).
3. **Consumer IR** — On a device reporting `FEATURE_CONSUMER_IR`, transmit a known-power
   NEC/RC-5 pattern at a set-top box / IR receiver and confirm reaction. Record
   `transmit(patterns)` durations used.
4. **USB Host** — Attach a CDC-ACM or HID device through OTG; confirm the provider appears
   with correct VID/PID, grant permission, then detach it and confirm the provider
   disappears (hot-plug regression test for the detach fix).
5. **Sub-GHz / LF RFID external adapters** — Only after a concrete driver for a specific
   VID/PID is implemented: verify RX captures match a reference analyzer capture for the
   same signal, and TX reaches a reference receiver. Record frequency/bandwidth settings.

## Regression checklist before release

- `./gradlew testDebugUnitTest lintDebug assembleDebug` all pass.
- Detached USB devices disappear from the Providers tab.
- Revoking BLUETOOTH permissions while the app is backgrounded flips the BLE provider to
  `ready=false` with a permission message, and re-granting restores it after refresh.
- Airplane-mode toggles flip BLE/NFC provider readiness within one refresh cycle.
