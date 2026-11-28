# Zyora iOS — strict 1:1 Android parity tracker

**Product requirement: Zyora on iPhone must be the same Zyora app as Android.** Android Native Compose is the source of truth. Do not redesign, simplify, omit, replace with web views, or create demo-only screens.

## Strict parity acceptance criteria

- Reproduce every Android screen and feature on iOS, including less frequently used pages, admin/owner tools, dialogs, sheets, menus, overlays, empty/loading/error states and edge cases.
- Match the Android UI 1:1: same assets, image crops, colors, typography, text, icon artwork, dimensions, spacing, alignments, card shapes, gradients, animations, timing, transitions, gestures and navigation. Use custom SwiftUI drawing/components where native iOS defaults differ. Only adapt safe-area/device-size layout where required; do not redesign.
- Match behavior 1:1: same validation, permissions, account rules, state transitions, socket events, API payloads, refresh behavior, persistence and error handling.
- Both platforms must use the same Firebase project, backend, user identities, room state, messages, wallet balance/history, game rounds/results and moderation state. Server remains authoritative for shared data and transactions.
- No placeholder screens, fake data, inert controls, local-only wallet/game logic, duplicate user identities, or iOS-only feature substitutes.
- Android is read-only for this port: modify only `ios-native/**` and iOS-specific workflow/configuration files. Never alter Android source, Android UI, or existing backend behavior to make the port easier.
- A feature is not complete until its Android counterpart has been inspected, the iOS implementation is connected to real services, and parity has been tested. Hardware-dependent features (voice, Bluetooth, notifications, camera, media) require physical-iPhone testing.

## Android source inventory

Port the complete native Android source, not only the initial demo:

- App shell/navigation: `MainActivity.kt`, `HurryTheme.kt`, `HurryIcons.kt`, `HurryModels.kt`, `HurrySupport.kt`, `NativeTranslations.kt`.
- Authentication/onboarding: `NativeLoginScreen.kt`, `LoginPage.kt`, `Newuserafterlogin.kt`, `ProfilePersistence.kt`.
- Home/discovery: `HomeScreen.kt`, `Discover.kt`, `SearchPage.kt`, `InviteFriends.kt`, `Leaderboard.kt`, `Notification.kt`.
- Rooms/voice: `RoomPage.kt`, `RoomInfo.kt`, `RoomSettingPage.kt`, `RoomProfile.kt`, `RoomSupport.kt`, `SeatActions.kt`, `SeatItem.kt`, `ActiveUsers.kt`, `RoomMusicService.kt`, `JitsiRoomAudio.kt`, `MusicController.kt`, `Roomcup.kt`, `Roomtask.kt`.
- Messaging: `MessageScreen.kt`, `ChatScreen.kt`, `VoiceMessageStore.kt`.
- Profiles/social: `MeScreen.kt`, `MineScreen.kt`, `PublicProfile.kt`, `Followpage.kt`, `SocialFollow.kt`, `followlist.kt`, `Family.kt`, `familyprofile.kt`, `Level.kt`, `Medal.kt`, `Svip.kt`, `PK.kt`.
- Wallet/gifts/games: `CoinsNativePage.kt`, `NativeWalletPending.kt`, `GiftPicker.kt`, `StorePage.kt`, `Wildparty.kt`, `DragonBattle.kt`, `Lucky777.kt`, `LuckyAnimation.kt`, `BagPage.kt`, `luckybag.kt`, `LuckyBagNotificationSlider.kt`, `DailyCheckInModal.kt`, `cupicon.kt`, `EntryEffect.kt`, `ExitMenu.kt`, `Emojipicker.kt`, `Fourgride.kt`, `ChromaKeyVideoView.kt`, `GreenColorRemovalImage.kt`, `WhiteColorRemovalShader.kt`.
- Settings/support/moderation: `SettingsPage.kt`, `LanguagePage.kt`, `HelpFeedbackPage.kt`, `Reports.kt`, `UserReport.kt`, `ownerban.kt`, `SellerCenter.kt`.
- Data/network: `HurryApi.kt`, `NativeIndexDatabase.kt`, `ZyoraApplication.kt`.

## Implementation phases

### 0. Build and Firebase foundation
- [x] Register Firebase Apple app with bundle ID `com.zyora.voicechat`.
- [x] Add `GoogleService-Info.plist` and Firebase Core initialization.
- [ ] Verify iOS simulator build.
- [ ] Implement and test real Firebase email/password and Google sign-in, URL callback handling and existing-account identity rules.

### 1. Shared app foundation
- [ ] Exact app shell, navigation and Android-matched design system.
- [ ] Asset catalogue, image/video/audio loading and caching.
- [ ] API models/client, auth token handling, Socket.IO events/reconnect and lifecycle.
- [ ] Loading/offline/empty/error/permission states.

### 2. Core application
- [ ] Login, registration, onboarding and session restore.
- [ ] Home, discovery, banners, search, room cards, refresh and pagination.
- [ ] Rooms, room info/settings, seats, locks, active users, follow and moderation.
- [ ] Live voice, microphone, speaker, Bluetooth, mute and room music.
- [ ] Messaging, realtime chat, images, voice notes and unread counts.
- [ ] Me/public/room profiles, family, follow/fans/friends, level, medals, SVIP and leaderboard.

### 3. Economy and games
- [ ] Wallet and transaction history.
- [ ] Gift picker, gift animations, store and owned items.
- [ ] Lucky Bag, notifications and daily check-in.
- [ ] Wild Party, Dragon Battle and Lucky 777.
- [ ] Verify shared server-authoritative balances and results against Android.

### 4. Remaining screens and release
- [ ] Settings, language, feedback, reports, moderation and seller/owner tools.
- [ ] Push notifications, deep links, privacy prompts, accessibility and crash handling.
- [ ] Screen-by-screen visual and behavioral comparison across iPhone sizes.
- [ ] Physical-device voice/Bluetooth/notification tests.
- [ ] Signed archive and IPA distribution.

## Build and signing facts

The existing workflow builds an iOS Simulator app and uploads a ZIP; this is not an installable iPhone IPA. A device IPA requires Apple signing assets (Team ID, distribution certificate and provisioning profile) stored as protected GitHub Actions secrets. App Store ID can remain blank until an App Store listing exists.

## Completion gate

Never claim “100% complete” based on a successful compile alone. Complete only after every Android source feature above has a real iOS counterpart, backend/account parity passes, screen-by-screen UI and interaction comparisons pass, and hardware features pass on a physical iPhone. Report any remaining gaps explicitly.
