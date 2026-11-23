# Zyora iOS

Native SwiftUI iOS app scaffold, kept separate from the existing Android app.

## Project

- XcodeGen project definition: `project.yml`
- SwiftUI source: `Sources/ZyoraApp.swift`
- Bundle identifier: `com.zyora.voicechat`
- Minimum iOS: 16.0

## Build on GitHub Actions

The workflow at `.github/workflows/ios-build.yml` runs on macOS, generates the Xcode project with XcodeGen, builds an iOS Simulator app and uploads it as an artifact.

The simulator build is for CI/testing and is not installable on a physical iPhone. A distributable, signed IPA requires Apple Developer signing assets (Team ID, signing certificate and provisioning profile) and app-specific Firebase configuration.

## Remaining integration

This is the initial native UI foundation, not a completed port of every Android feature. Before release, integrate the existing Zyora API endpoints, Firebase iOS Auth/Google Sign-In configuration, Socket.IO room/chat events, push notifications, microphone/audio routing, and verify all account and room flows on physical iPhones. Do not put private signing keys or service credentials in the repository.
