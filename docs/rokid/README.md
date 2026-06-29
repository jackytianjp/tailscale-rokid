# Tailscale For Rokid

This directory contains a custom Android shell for running Tailscale on Rokid AI glasses.

## Scope

- Target OS: YodaOS-Sprite class devices that expose an Android-compatible runtime.
- Target CPU: ARM64 only in the first delivery.
- Mesh mode: userspace networking by default to reduce platform-specific VPN integration risk.
- XR adaptation: floating HUD on Sprite-class devices, richer spatial UI reserved for Master-capable devices.

## Implemented Pieces

- `android/rokid/` standalone Android application skeleton.
- Foreground service wrapper for `tailscaled` lifecycle.
- Voice command parsing for start, sleep, resume, status, and node summary.
- Power-state receiver that lowers heartbeat cadence on screen-off and restores it on wake.
- Overlay HUD controller for glanceable status display.
- Power and policy defaults in `rokid-default-policy.json`.
- ARM64 build script for bundling `cmd/tailscaled` into app assets.
- APK install guide for Rokid glasses.

## Integration Notes

1. Run `android/rokid/scripts/build-tailscaled-android-arm64.ps1` to place `tailscaled` under app assets.
2. Open `android/rokid/` in Android Studio.
3. Connect a Rokid device with ADB enabled.
4. Install the APK and grant overlay, microphone, location, notification, and battery optimization exemptions.
5. Replace the placeholder userspace-only launch path with a real `VpnService` integration if full-device traffic capture is required.

See `docs/rokid/INSTALL_APK.md` for the step-by-step installation flow.
See `docs/rokid/GET_APK.md` if you only need an APK for Rokid Store upload.

## Performance Targets

- Keep `tailscaled` in userspace mode to avoid expensive route churn on mobile.
- Persist state under app-private storage only.
- Use screen state as a low-power signal to switch to a sparse keepalive profile.
- Prefer a low-verbosity daemon config to reduce background I/O.

## Gaps To Close Before Production

- Add Rokid private SDK hooks for gaze focus and native gesture channels.
- Replace the placeholder daemon snapshot values with live LocalAPI reads.
- Add a `VpnService` bridge if store policy or product requirements demand system-wide traffic tunneling.
- Validate on-device memory, CPU, standby drain, and weak-network recovery with telemetry.
