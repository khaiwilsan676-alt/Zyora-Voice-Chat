# Zyora iOS Android-Parity Implementation Plan

This file tracks the native iOS port. Android remains the behavior and visual reference; do not modify Android UI, wallet rules, room rules, or backend behavior as part of the iOS port.

## Non-negotiable parity rules

- Rebuild screens in SwiftUI to match the current Android Compose UI: spacing, dimensions, colors, typography, images, icons, sheets, dialogs, gestures, loading/empty/error states, and navigation.
- Reuse the existing Zyora backend contracts and account identity. Do not create a second user identity or a separate iOS-only data model.
- Preserve server-authoritative wallet, game, moderation, room membership, and account rules. Never implement wallet/game balance changes as client-only logic.
- Keep Android and iOS changes isolated. Do not rename or restructure Android modules for the iOS port.
- Do not mark a feature complete until it has been connected to real services and tested on a physical iPhone.

## Porting workstreams

1. Foundation — app navigation, shared design tokens, asset catalog, image loading/cache, API configuration, error handling, lifecycle and deep links.
2. Authentication and onboarding — Firebase email/password and Google sign-in, existing-account behavior, ban/unban handling, permissions and profile setup.
3. Home and discovery — banners, room cards, search, online/presence indicators, refresh, pagination and scroll performance.
4. Rooms and live voice — room/profile/info/settings, seats and locks, owner/admin controls, active users, follow/join/leave, Socket.IO presence/events, microphone permissions, audio session routing, mute, speaker/listener behavior and music streaming.
5. Messaging — conversation list, chat, image sending, voice notes, unread counts, delivery/realtime events and the same persistence contract as Android.
6. Profiles and social — Me/public/room profiles, follow/fans/friends, visitors, tags, country, avatar and room presence.
7. Wallet and games — wallet, transactions, rewards, daily check-in, game screens and server-validated balance updates.
8. Owner/admin tools — reports, user management, App Staff, moderation, game management and shared owner data.
9. Release readiness — accessibility, privacy strings, notification setup, crash handling, physical-device regression, signing, archive and IPA distribution.

## Current verified status

- ios-native currently contains a small SwiftUI demonstration scaffold only.
- The current iOS workflow builds an iOS Simulator app and uploads a ZIP artifact. That artifact is not an installable iPhone IPA.
- The current sign-in button is intentionally not connected to Firebase. Rooms, messages and profile data are placeholder states.
- Therefore the iOS app is not feature-parity complete and must not be described as ready for iPhone users.

## Required release configuration

A signed device IPA requires Apple code-signing assets (Apple Developer Team, distribution certificate and provisioning profile). Firebase iOS authentication also requires the matching iOS Firebase app configuration and Google Sign-In URL scheme. Keep signing credentials and private configuration in protected GitHub Actions secrets; never commit private keys or provisioning credentials.

## Completion gate

For each workstream, compare the iOS screen against the current Android screen and test the same account and backend data. Verify success, loading, empty, offline, permission-denied and error cases. Run the iOS Simulator build after changes; perform physical-iPhone tests before calling microphone, Bluetooth/audio routing, push notifications, Google sign-in or IPA distribution complete.
