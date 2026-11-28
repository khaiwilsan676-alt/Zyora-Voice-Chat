# Zyora iOS — Android parity tracker

Android Native Compose is the source of truth. The iOS app must reproduce the actual Zyora experience, not the earlier placeholder demonstration.

## Safety rules

- Work only in `ios-native/**` and iOS-specific workflow/configuration files. Never change Android source, Android UI, wallet rules, room rules, or backend behavior for the port.
- Match Android screens, assets, spacing, typography, colors, navigation, animations, gestures, sheets, dialogs, loading/empty/error states and behavior as closely as iOS conventions allow.
- Reuse the same backend endpoints, Socket.IO event names, Firebase project, user IDs and server-authoritative data. Never create iOS-only balances, game outcomes, users or room membership.
- Preserve existing Android behavior. Do not call a screen complete while it contains placeholder data or inert buttons.
- Keep secrets out of source control. The Firebase client plist is app configuration, not a substitute for server credentials; signing certificates and private keys must remain protected.

## Android source inventory to port

The Android Native app currently has these feature modules. Each must be mapped to a corresponding native SwiftUI implementation and checked against the Android implementation before being marked complete:

- App shell and navigation: `MainActivity.kt`, `HurryTheme.kt`, `HurryIcons.kt`, `HurryModels.kt`, `HurrySupport.kt`, `NativeTranslations.kt`.
- Authentication/onboarding: `NativeLoginScreen.kt`, `LoginPage.kt`, `Newuserafterlogin.kt`, `ProfilePersistence.kt`.
- Home/discovery: `HomeScreen.kt`, `Discover.kt`, `SearchPage.kt`, `InviteFriends.kt`, `Leaderboard.kt`, `Notification.kt`.
- Rooms/voice: `RoomPage.kt`, `RoomInfo.kt`, `RoomSettingPage.kt`, `RoomProfile.kt`, `RoomSupport.kt`, `SeatActions.kt`, `SeatItem.kt`, `ActiveUsers.kt`, `RoomMusicService.kt`, `JitsiRoomAudio.kt`, `MusicController.kt`, `Roomcup.kt`, `Roomtask.kt`.
- Messaging: `MessageScreen.kt`, `ChatScreen.kt`, `VoiceMessageStore.kt`.
- Profiles/social: `MeScreen.kt`, `MineScreen.kt`, `PublicProfile.kt`, `Followpage.kt`, `SocialFollow.kt`, `followlist.kt`, `Family.kt`, `familyprofile.kt`, `Level.kt`, `Medal.kt`, `Svip.kt`, `PK.kt`.
- Wallet/gifts/games: `CoinsNativePage.kt`, `NativeWalletPending.kt`, `GiftPicker.kt`, `StorePage.kt`, `Wildparty.kt`, `DragonBattle.kt`, `Lucky777.kt`, `LuckyAnimation.kt`, `BagPage.kt`, `luckybag.kt`, `LuckyBagNotificationSlider.kt`, `DailyCheckInModal.kt`, `cupicon.kt`, `EntryEffect.kt`, `ExitMenu.kt`, `Emojipicker.kt`, `Fourgride.kt`, `ChromaKeyVideoView.kt`, `GreenColorRemovalImage.kt`, `WhiteColorRemovalShader.kt`.
- Settings/support/moderation: `SettingsPage.kt`, `LanguagePage.kt`, `HelpFeedbackPage.kt`, `Reports.kt`, `UserReport.kt`, `ownerban.kt`, `SellerCenter.kt`.
- Data/network: `HurryApi.kt`, `NativeIndexDatabase.kt`, `ZyoraApplication.kt`.

This inventory is a porting checklist, not a claim that these modules have already been implemented on iOS.

## Phased implementation

### Phase 0 — iOS build and Firebase foundation
- [x] Register Firebase Apple app with bundle ID `com.zyora.voicechat`.
- [x] Add `GoogleService-Info.plist` to the iOS target resources.
- [x] Add Firebase iOS SDK package dependency and initialize Firebase at app launch.
- [ ] Verify the macOS GitHub Actions simulator build succeeds.
- [ ] Add Firebase Auth and Google Sign-In dependencies, URL callback handling and real email/password + Google flows.
- [ ] Confirm Firebase sign-in resolves to the same existing Zyora backend account; preserve account de-duplication rules.

### Phase 1 — shared foundation
- [ ] Replace demo navigation with the actual Android tab/navigation structure.
- [ ] Establish reusable SwiftUI design tokens and reusable components from Android measurements.
- [ ] Port image/video/audio asset catalog and caching behavior.
- [ ] Port API client, request/response models, auth token handling, Socket.IO client, reconnect and lifecycle handling.
- [ ] Add robust loading, offline, empty, permission-denied and error states.

### Phase 2 — core social app
- [ ] Login, registration, profile setup and session restore.
- [ ] Home, discovery, search, room cards, refresh and pagination.
- [ ] Room entry, room information/settings, seats, seat locks, active users, follow and moderation.
- [ ] Live voice, microphone/speaker routing, Bluetooth, mute and room music.
- [ ] Messages, realtime chat, image messages, voice messages and unread state.
- [ ] Me/public/room profiles, family, follow/fans/friends, level, medals, SVIP and leaderboard.

### Phase 3 — economy and games
- [ ] Wallet and transaction history.
- [ ] Gift picker, gift animations, store and owned items.
- [ ] Lucky Bag and notifications.
- [ ] Wild Party, Dragon Battle, Lucky 777 and daily check-in.
- [ ] Verify every balance mutation and game result remains server-authoritative and consistent with Android.

### Phase 4 — remaining screens and release
- [ ] Settings, language, feedback, reports, moderation and seller/owner-facing tools.
- [ ] Push notifications, deep links, privacy permissions, accessibility and crash reporting.
- [ ] Visual/behavioral parity review against Android on multiple iPhone sizes.
- [ ] Physical-device voice/Bluetooth/notification regression.
- [ ] Signed archive and IPA distribution after Apple Developer signing assets are configured.

## Build and release facts

- GitHub Actions currently builds an iOS Simulator app and uploads a ZIP. A simulator artifact is not installable on a physical iPhone.
- A distributable IPA requires Apple signing assets (Team ID, distribution certificate and provisioning profile); configure them as protected GitHub Actions secrets.
- Do not add an App Store ID until the app has an App Store listing. Team ID can remain unset for unsigned simulator builds.

## Definition of done

A feature is complete only when it uses real Zyora services and the same account/backend data as Android; matches Android's UI and interaction behavior; passes success, loading, empty, offline, denied-permission and error cases; and has been tested on a physical iPhone where hardware behavior is involved.
