# Zyora iOS

Native SwiftUI iOS target kept separate from the existing Android application.

## Project

- XcodeGen project: `project.yml`
- SwiftUI entry point: `Sources/ZyoraApp.swift`
- Shared API transport and room response models: `Sources/ZyoraAPI.swift`
- Bundle identifier: `com.zyora.voicechat`
- Minimum iOS: 16.0
- Firebase project: the same Zyora Firebase project used by Android
- Backend: `https://zyora-server.onrender.com`

## Build

The GitHub Actions workflow `.github/workflows/ios-build.yml` runs on macOS, resolves Firebase Auth, Google Sign-In and Socket.IO packages, builds the iOS Simulator app and uploads `Zyora-iOS-Simulator.zip`.

A simulator build is for CI/testing and cannot be installed on a physical iPhone. The workflow also creates a signed App Store Connect IPA when all four Apple signing secrets are configured in the repository:

- `IOS_P12_BASE64`: base64-encoded Apple Distribution .p12 certificate.
- `IOS_P12_PASSWORD`: password for that .p12 file.
- `IOS_PROVISIONING_PROFILE_BASE64`: base64-encoded App Store provisioning profile for `com.zyora.voicechat`.
- `IOS_TEAM_ID`: Apple Developer Team ID.

The resulting `Zyora-iOS-IPA` artifact is suitable for App Store Connect/TestFlight upload. Signing secrets must only be stored in GitHub Actions Secrets, never committed to the repository.

## Port status

This is an in-progress native iOS port, not yet a feature-complete Android equivalent. The Android implementation remains the source of truth. Authentication/device-account parity, the full screen inventory, live voice/audio routing, room/chat Socket.IO event parity, gifts, wallet, games, notifications and physical-iPhone validation remain release blockers. Do not replace missing features with demo data or claim parity based only on a successful build.
