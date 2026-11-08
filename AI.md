# Zyora Project Memory (AI.md)

> Living project handoff for AI-assisted development. Update this file in the same change/commit whenever a feature, bug fix, architecture decision, or build workflow changes.
> Last documented: 2026-11-06
> Repositories: `khaiwilsan676-alt/Zyora-Voice-Chat` (main app) and `khaiwilsan676-alt/Zyora-Server` (backend).

## 1. Project overview

Zyora is a voice-chat / social-room application. The main repository contains the web application and a native Android implementation. The intended Android target is Kotlin + Jetpack Compose under `android-native`; preserve existing web/TSX functionality when changing native code. The user primarily develops from an Android phone and GitHub Actions, so changes must be committed to the correct repository and build workflows should remain usable without a local desktop.

Production web URL previously supplied: https://zyora-voice-chat.vercel.app  
Backend URL previously supplied: https://zyora-server.onrender.com

## 2. Product and UI constraints

- Product name is **Zyora** (not Hurry, Hawa, or Zaho).
- Keep the existing dark / blue / grey visual language.
- For bug fixes, do not redesign UI or change unrelated layouts. Make only the requested UI change.
- Do not add guest login.
- Do not expose infrastructure/storage implementation details in user-facing privacy-policy copy.
- Existing user profile data should remain consistent across Public Profile, Me, Room Profile, and chat surfaces.
- Preserve existing features and files; avoid broad rewrites or restoring old versions over newer work.

## 3. Authentication and data architecture (current intended direction)

- Firebase Authentication is intended for Google sign-in and email/password sign-in.
- Application profile and room data are intended to live outside Firebase, using the Zyora backend and MongoDB.
- MongoDB Atlas is the currently reported persistent database; use one MongoDB database/cluster for this project unless the owner explicitly changes the decision.
- Render hosts the Node.js backend. The current reported service is `zyora-server` at the URL above.
- Redis / Socket.IO realtime infrastructure has been discussed and Redis has been configured in the project at some point; verify the live server configuration before relying on it.
- Do not put credentials, tokens, private keys, database connection strings, or personal account secrets in this file or source commits.

## 4. User profile and onboarding requirements

- First login should show onboarding only for a new account.
- Onboarding fields requested: avatar, nickname, birthday/age, country, and gender (male/female).
- Saved profile data should be the source of truth for Public Profile, Me, Room Profile, chat, and user cards.
- Public Profile edits should update the same account profile data.
- One Google account maps to one stable account ID; a different account gets a different ID, including on the same device.
- Profile avatar and room display assets should persist through the backend/database as URLs rather than storing image binaries in MongoDB where possible.

## 5. Rooms, seats, moderation, and voice

### Room behavior
- Rooms support users, seats, room presence, room chat, and voice.
- A seated user can speak/listen according to room voice rules; users not seated can listen but should not transmit.
- Seat and room presence must be cleaned up when users leave, are kicked, or disconnect.
- A user who has left a room must not remain in Active Users or other in-room UI.

### Owner moderation
- Moderation controls must not be shown to ordinary users on another user's profile.
- Room owner authorization must be checked on the server, not trusted from client UI.
- Room Profile moderation currently distinguishes whether the target is seated.
- Timed kick behavior has been implemented in recent commits: supported durations are 15 minutes, 30 minutes, 2 hours, and 24 hours; kick bans are intended to persist in MongoDB and block rejoining until expiry.
- Kick should remove the target from room presence, seat, room chat, and voice state while keeping the socket able to receive the kicked-out confirmation.

### Self mute vs owner/admin mute
- Self mute and owner/admin moderation mute are separate states.
- Self mute is controlled by the user's bottom mic control; the user can toggle their own self mute.
- Owner/admin mute is controlled by authorized room moderation and cannot be undone by the muted user. Only authorized moderation can clear it.
- Effective microphone transmission must be disabled if either self mute OR moderation mute is active.
- Do not show a mute icon/badge over the avatar for self mute.
- Current intended mute-card appearance: white card with black microphone icon (not red).
- Speaking indicators must be hidden whenever either mute state is active.
- A user selecting Leave should be removed from the seat and moved below the seats while remaining in the room.

### Voice correctness requirements
- Verify actual remote audio transmission and reception with at least two devices/accounts; a successful build alone does not prove voice works.
- Ensure mute stops the audio track/transmission immediately and unmute restarts it only when moderation mute is false and the user is otherwise permitted to speak.
- Do not assume a specific voice provider (e.g. Jitsi) without checking current implementation.
- Clean up audio tracks, listeners, room membership, and speaker/presence state on leave, kick, disconnect, and room switch.

## 6. Room invitations

- Room seat-invite flow has been implemented in recent Android/server commits.
- Invitations should remain pending appropriately while sockets reconnect, use server-issued invite tokens, expire according to server TTL, and reset UI state when the active room changes.
- Verify send, receive, accept, reject, expiry, and actual seat assignment across two accounts.

## 7. Wallet, rewards, gifts, and storage requirements

- Wallet, gifts, transactions, daily check-in, lucky bag, and reward inventory are app features.
- Daily check-in reward must be credited to the wallet exactly once per day.
- Reward items should be placed into the correct Bag category (e.g. frames vs themes).
- Wallet balance must persist after navigating away and returning.
- Room gifts, room DP/theme, and public profile media should persist according to the existing backend contract.
- Never claim wallet or payment flows are correct without verifying the server transaction and client refresh paths.

## 8. Recent implementation history (from repository commit history)

Main repository recent commits include:
- Separate self mute from room owner mute.
- Gate local voice by both self and owner mute.
- Stop target audio immediately on owner moderation mute.
- Hide speaking state while either mute is active.
- Restrict room moderation to room owner and hide seat actions for users off mic.
- Pass selected kick duration and owner identity from room profiles.
- Listen for timed room kicks and rejoin restriction events.
- Show kicked-out confirmation card and timed room rejoin notice.
- Automatically clear the room rejoin restriction popup at expiry.
- Room seat invitation flow: server-issued invite token, pending invite handling, TTL expiry, and overlay placement.

These are repository-history entries, not proof of successful APK builds, backend deployment, or live two-device behavior.

## 9. Native Android build and development

- Native Android source is intended to be under `android-native`.
- Use the repository's existing Gradle wrapper and configured JDK/Kotlin/Compose versions; inspect current files before changing versions.
- User's primary test device is a Redmi 14C Android phone.
- The user relies on GitHub Actions for APK builds and may use Appetize for previews.
- A successful GitHub commit or web deployment is not the same as a successful native APK build.
- After Android changes, inspect the relevant GitHub Actions workflow and report the exact build result. Do not claim installation or runtime verification unless actually tested.

## 10. Engineering rules for every future change

1. Read the current implementation and recent history before editing; never patch from an old remembered snapshot.
2. Identify the root cause and update the smallest correct set of files.
3. Keep client UI state and server-authoritative state consistent. Security-sensitive actions (owner/admin moderation, wallet credits, bans) must be authorized and validated server-side.
4. Preserve unrelated UI, data models, and features.
5. Handle lifecycle cleanup: socket listeners, audio tracks, room membership, timers, and Compose effects.
6. Check reconnects, duplicate events, stale state, room switching, and multiple devices.
7. Update this `AI.md` in the same feature change with: what changed, files touched, server/client event contracts, persistence behavior, tests run, and known limitations.
8. Never mark a feature "complete" just because code was committed. Track build, deployment, and device verification separately.
9. Never put secrets or personal data in documentation.
10. Prefer a focused commit message that names the feature and root cause.

## 11. Verification status policy

Use these labels in future updates:
- **Implemented**: code change is committed.
- **Build verified**: relevant CI/build completed successfully.
- **Backend deployed**: deployment completed and health/log checks passed.
- **Device verified**: behavior tested on a real device.
- **Two-device verified**: realtime/voice/moderation behavior tested with two accounts/devices.

Current documentation does not certify that all historical features are build-, deployment-, or device-verified. Confirm each status from actual workflow/deployment/test evidence before changing it.

## 12. Security hardening (2026-11-06)

### First backend hardening commit
- Backend repository: `khaiwilsan676-alt/Zyora-Server`.
- Updated `server.js` to replace wildcard browser CORS with an explicit HTTPS origin allowlist. The production Vercel origin remains allowed; additional origins must be configured with `ZYORA_ALLOWED_ORIGINS` (comma-separated). Native clients without an Origin header remain supported. CORS is not authentication.
- Added/strengthened HTTP response headers: `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options: DENY`, `X-Permitted-Cross-Domain-Policies`, `Cross-Origin-Opener-Policy`, `Cache-Control: no-store`, and production HSTS.
- Reworked the in-process request limiter: stricter per-minute buckets for auth (20) and image upload (15), general requests (180), bounded map size, stale-entry cleanup, and `Retry-After` on 429.
- Tightened Socket.IO transport settings: no wildcard CORS, 256 KB maximum packet size, explicit ping settings, and disabled per-message compression.
- Backend code commit: `982f65eabf0104e2523357c44df257cccf2fc5b4`.
- Status: **Implemented in repository**. Render deployment, browser/native compatibility, and runtime behavior are **not yet verified**.

### Critical security work still required (do not mark secure/complete)
- Current Socket.IO server has no Firebase-token connection middleware. Several socket handlers accept client-supplied IDs; identity spoofing remains a critical risk until clients send Firebase ID tokens and the server derives identity only from verified token claims.
- Several HTTP routes trust caller-supplied `userId` / `accountId`; wallet, store, profile, follow/block, and administrative endpoints need Firebase authentication plus per-resource ownership/role authorization.
- Owner/admin checks must use server-stored role/ownership data, never public IDs or client-provided role flags as proof.
- Wallet/gift/coin operations require server-side balance checks, atomic MongoDB transactions/updates, idempotency keys, and anti-replay validation.
- Rate limits currently run per process; add shared Redis-backed limits and authenticated-account quotas before treating them as production-grade.
- Audit MongoDB indexes/unique constraints, data access rules, upload MIME/size validation, private data exposure, Socket.IO event authorization, and secret rotation.
- Never deploy a breaking auth requirement until Web and Android clients are updated to send Firebase ID tokens and compatibility has been tested. Keep UI unchanged.

### Security verification checklist
- [ ] Render deployment succeeds and health checks pass.
- [ ] Production web and native app can still connect after CORS/Socket.IO changes.
- [ ] Unauthorized / expired Firebase tokens are rejected by HTTP and Socket.IO.
- [ ] A user cannot read or mutate another user's profile, wallet, inventory, follows, messages, or room controls.
- [ ] Non-owner cannot change room settings, mute/kick users, or administer a room.
- [ ] Forged/replayed coin transfers and duplicate reward claims are rejected.
- [ ] Rate limits work across multiple Render instances.
- [ ] Two-account / two-device regression tests pass.


## 13. Performance optimization (2026-11-06)

### Native Android targeted performance pass
- `StorePage.kt`: removed the one-second Compose state ticker that invalidated the full Store composable every second. Equipped/owned item expiry now uses an event-driven coroutine that sleeps until the nearest expiry and wakes only when needed. This reduces needless recomposition while preserving expiry behavior.
- `MainActivity.kt`: reduced the fixed launch-screen wait from 1,200 ms to 400 ms; the existing update check still runs after the launch transition and performs network work on `Dispatchers.IO`.
- No visual redesign, room/voice protocol, wallet API, or purchase behavior was intentionally changed.
- Status: **Implemented in repository**. Native APK build, startup timing on Redmi 14C, store scroll performance, and item-expiry behavior still require CI/device verification.
- Next profiling targets before broad optimization: room screen recomposition and bitmap/video decode cost, duplicate Socket.IO connections/listeners across navigation, repeated profile/inventory/wallet requests, backend cold-start/database query latency, and web bundle/render performance. Measure first; avoid speculative changes to voice and wallet correctness.


## 14. Room entry performance pass (2026-11-06)
- `RoomPage.kt`: removed a duplicate `/api/rooms?roomId=...` request on room entry. The remaining room-settings request now handles legacy DP/theme field aliases too, preserving existing settings compatibility.
- `RoomPage.kt`: avoids a second inventory fetch for the current user's own join event; the connect path already performs the local user's equipped-vehicle lookup. Other users' join events still resolve their vehicle as before.
- These changes reduce room-entry network work without changing room UI, seat rules, voice signaling, wallet operations, or other users' join toasts.
- Verification pending: native APK CI and real-device room entry / theme / DP / join-toast checks. Render free-tier cold starts and network latency remain external sources of delay and are not eliminated by client changes.

## 15. Room card scrolling performance fix (2026-11-06)
- `android-native/app/src/main/java/com/hurry/voicechat/HomeScreen.kt`: removed the per-visible-card infinite equalizer animations and repeated random live-count timer updates. Those animated state values caused continuous recompositions across visible room cards while the user scrolled.
- Preserved the existing card layout, image, displayed count, and equalizer appearance; the count and bar heights are now stable for each room card instead of continuously changing.
- Added an explicit LazyColumn `contentType` for room-card rows while retaining the stable row key.
- Status: **Implemented in repository**. Native APK CI and real-device scroll smoothness on Redmi 14C still need verification. No UI redesign or room/voice behavior change intended.

## 16. Followpage empty-state consistency (2026-11-06)
- `android-native/app/src/main/java/com/hurry/voicechat/Followpage.kt`: Fans, Friends, and Following now reuse the same empty-state image and “No data” label used by the native Mine page (`file_0000000047308211a02722299d1fda2e.png`).
- Removed the separate hand-drawn per-tab empty illustrations and “No Fans/Friends/Following yet” labels from this empty state only. Visitors behavior and the page header/background remain unchanged.
- Status: **Implemented in repository**. Native APK CI and device appearance still require verification.

## 17. Persistent social follow graph (2026-11-06)
- Backend `Zyora-Server/server.js`: follow routes now resolve account aliases to a canonical profile key, persist idempotent follower/target pairs in MongoDB `userFollows`, return follower/following/friend counts and profile rows, and broadcast follow updates. The GET route supports `profile`, `fans`, `following`, and mutual `friends` views.
- Native `SocialFollow.kt`: added shared API client and local cache for relationship state, counts, and social-list rows.
- Native `RoomProfile.kt` and `PublicProfile.kt`: Follow/Followed state now loads from and writes to the same persistent follow graph; fan counts refresh from the server.
- Native `Followpage.kt`: Fans, Following, and mutual Friends now load real profile rows with avatar, name, gender, country flag, and account ID (official ID image where applicable). Existing no-data artwork remains the empty state.
- Native `MeScreen.kt`: Friends, Fans, and Following counts now come from the server/cache; unresolved counts display a dash rather than a misleading hardcoded zero. A Socket.IO follow-update listener refreshes these counts while Me remains mounted.
- Verification: source changes committed. Render deployment, native compile, cache/reconnect behavior, and two-account follow/unfollow tests remain pending. Existing API trusts caller-supplied IDs; Firebase-token authorization remains a separate required security task.


## 18. Follow graph root-cause hardening (2026-11-06)
- Canonical follow identity is shared across Room Profile, Public Profile, Fans, Friends, Following, and Me counts.
- Backend GET /api/follows now resolves legacy Firebase UID/public account-number aliases in one batched users query before deduplicating incoming/outgoing edges. This prevents the same person being counted more than once when older clients saved UID edges and newer clients saved account-number edges, without issuing one database query per follow row.
- Native APK compile failure found in CoinsNativePage.kt: AgentContent onlineById had malformed mutableStateOf generic syntax. Corrected to an explicitly typed Map<String, Boolean>; no UI change.
- Backend commit: 47be4d8726de6b3fed75ccb6c4c7d7297a808758 (Zyora-Server).
- Native compile fix commit: 00fd47aa5150e33bb5aac23823e0c4cd2f37b298 (Zyora-Voice-Chat).
- Remaining verification: Render must deploy the backend commit; rerun Native APK workflow and test follow/unfollow across two accounts, including legacy UID and numeric account ID aliases. Do not claim runtime verification until these checks pass.


## 19. Instant room entry and chat profile I/O (2026-11-06)
- `MainActivity.kt`: removed the pre-entry `/api/room-kick-status` HTTP round-trip. Room UI opens immediately; the existing authoritative `room_join` handler in `Zyora-Server/server.js` checks MongoDB/in-memory kick bans before adding the user to room presence and emits `room_kick_blocked` for blocked users. This retains kick enforcement while removing a network request from the critical navigation path.
- `RoomPage.kt`: moved the per-message profile SQLite reads and writes in `NativeMessage` off the Compose main thread. Cached profile tags are applied first; remote tag refresh remains asynchronous and continues to update the same profile fields.
- No UI, seat, chat, voice, kick-ban, wallet, or room settings behavior intentionally changed.
- Verification pending: native APK CI and real-device room-entry timing / kicked-user rejection tests. Do not claim a measured latency until tested on device.


### Room socket reconnect regression fix (2026-11-06)
- `android-native/app/src/main/java/com/hurry/voicechat/RoomPage.kt`: removed asynchronously loaded display name/avatar from the `DisposableEffect` socket identity keys. Previously, when the local SQLite profile finished loading, Compose disposed the active room socket and created a second connection, delaying presence/seats/chat initialization.
- Latest profile and room display values now flow into the existing socket callbacks via `rememberUpdatedState`, so profile/settings refreshes do not reconnect the room socket or use stale join details.
- UI, voice, seat, kick, chat, and room behavior remain unchanged.
- Status: source change committed; native APK build and real-device entry timing still require verification. No measured latency improvement is claimed until tested on device.


## 20 Pre-entry room kick blocking correction (2026-11-06)
- `MainActivity.kt`: restored a server access preflight in `tryOpenRoom`. The app now calls `/api/room-kick-status` before setting `openedRoom`, so a kicked user never mounts `RoomPage` and cannot briefly see or enter the room while the Socket.IO kick event arrives.
- A blocked account receives the existing slim homepage restriction popup. The existing black/70 popup styling is retained. If the access check cannot be confirmed, the room stays closed and the existing retry message is shown.
- The server-side `room_join` kick-ban validation remains unchanged as the final enforcement layer.
- No room layout, seat, voice, chat, announcement, or moderation UI was changed in this correction.
- Status: **Implemented in repository**. Native APK CI, Render availability, and real-device kick/re-entry timing still require verification.


## 21 Room kick chat system-row event delivery (2026-11-06)
- Root cause: native `RoomPage.kt` already listened for `room_kick_announcement` and rendered `kickNotices` through the existing `RoomChatMessages` system-row layout, but the server's successful `room_seat_action` kick handler never emitted that event.
- Server repo fix: after the kick ban is persisted and target sockets are confirmed, broadcast `room_kick_announcement` to the room with target avatar/name, kicker avatar/name, owner/admin role, and timestamp. Existing client listener now receives it and inserts the already-implemented system row.
- Kept the existing chat row UI, spacing, position, typography, and popup unchanged. No unrelated room UI behavior changed.
- Status: **Server event implemented and committed**. Render deployment and live room verification remain pending.


## 22 Kick notice chat placement and kicker avatar (2026-11-06)
- `RoomPage.kt`: moved the existing kick-notice LazyColumn items from before announcements/normal messages to after the normal chat-message items, so kick notices render at the bottom of the chat like the latest chat row.
- Moved the kicker avatar to the start of the kicker-name row, immediately before the kicker's name. Removed its previous placement after the word "by" to avoid showing the same avatar twice.
- Preserved the existing kick notice card styling, target avatar/name, System row, actor role text, normal message rendering, and clear-chat/announcement logic. No other chat UI was changed.
- Status: **Implemented in repository**. Native APK CI and live room/chat ordering verification remain pending.


## 23 Native wallet cached rendering (2026-11-06)
- `CoinsNativePage.kt`: cache the last known coins/diamonds snapshot in account-keyed SharedPreferences and initialize the displayed balance from that snapshot immediately, avoiding a zero-to-server-balance flash on page entry.
- Room database is used as a one-time fallback only when no cached snapshot exists. The canonical server wallet refresh remains asynchronous; successful refreshes update Room, the snapshot cache, and only changed balance fields.
- Rapid reopen operations within 15 seconds reuse the existing snapshot without issuing another wallet GET. Both diamond-exchange success paths also update the cached snapshot immediately so the next wallet entry cannot show a stale pre-exchange balance. No wallet UI layout, payment flow, or balance source-of-truth was changed.
- Status: source committed; native APK CI and real-device repeated-open/cache behavior remain to be verified.


## 18. Follow inbox notification and latency fix (2026-11-06)
- Native `SocialFollow.kt`: follow mutation now uses the authoritative POST response instead of performing a second GET; local relationship cache is updated from that response.
- Native `RoomProfile.kt`: follow state/count update optimistically and roll back if the server mutation fails. Public Profile retains its existing optimistic UI and rollback.
- `Zyora-Server/server.js`: a newly-created follow edge sends exactly one normal private inbox message, `I followed you`, to the followed user's Message page; repeat follow requests do not duplicate it. Online delivery uses the existing private-message socket event and offline delivery uses the existing Redis pending-message queue.
- No UI redesign or layout changes.
- Room Profile Invite: tapping Invite on a non-seated user's room profile now sends that exact user the existing Socket.IO seat invitation for the first available unlocked seat, using the same `room_seat_invite_send` flow as the seat menu. No UI changes.
- Seat Lock: when the room owner locks an occupied seat, the server clears the occupant and live mic/emoji state before broadcasting the locked seat; unlocking preserves the empty seat. No UI changes.

## 24 Room info sharing and room chat image support (2026-11-07)
- RoomInfo.kt: the Room Info three-dot menu offers Report and room Follow/Followed actions, retaining the Public Profile danger-sheet layout. Room follow mutations use the room-specific /api/room-follows endpoint and room ID, not the owner's user-follow relationship.
- Room Info Share in Zyora loads the room owner's Me → Fans list through SocialFollowApi.list(..., "fans") and sends the selected fan a private-message room invitation containing that room's ID, name, and DP. The existing white 12dp invitation card opens the exact shared room; special IDs keep their ID image.
- Fans-list API compatibility hardening: `SocialFollowApi.list(..., "fans")` now calls the canonical `/api/follows?targetUid=...` form and normalizes the returned `followerList` into the existing local cache. The production `Zyora-Server/server.js` route currently accepts both `targetUid` and `userId`, and returns both `rows` and `followerList`; therefore this is a compatibility/normalization improvement, not proof that the former query alone caused an empty sheet. Live owner-account data still needs verification.
- ProfilePersistence.kt: added the shared uploadChatImage uploader used by direct and room chats. It validates the 5 MB limit and uses dedicated media folders, avoiding accidental writes to the user-DP folder.
- RoomPage.kt: the room chat image icon opens the system image picker, uploads to room-chat-image, broadcasts the image URL over the existing room Socket.IO message event, and renders a tappable image with a full-screen preview and top-left close control.
- Direct-chat image messages retain their existing image picker, image preview, and close control. Existing chat and room layouts were left unchanged.
- Status: source changes committed. Native APK build and live multi-device image/room-invitation verification remain pending.


## 25 Room invitation delivery and room info refinements (2026-11-07)
- Native direct-chat inbox sync and the background message listener now preserve the full `ROOM_INVITE::` payload in local IndexDB instead of reducing an incoming room invitation to the plain text label. The payload retains room ID, room name, room DP, and owner ID, so the receiving user's Chat page restores the tappable room invitation card and opens that exact room.
- Room Info Share in Zyora now loads the signed-in user's Fans list (excluding the current account), rather than the room owner's Fans list. The room invitation still carries the selected room's ID/name/DP.
- Room Info sheet height increased from 50% to 60% of screen height; the three-dot danger menu is hidden from the room owner.
- Room chat shared-image preview thumbnail maximum size reduced from 230dp to 140dp; tapping still opens the full-screen image preview.
- Verification: source changes committed; native APK CI and two-account online/offline invitation delivery, room join, fan-list, and device-layout checks remain pending.


## Room owner tools and shared block enforcement (2026-11-07)
- Fourgride owner-only controls: Clear Chat, Public Message toggle, Music, and PK Battle are visible only to the room owner. Non-owner grid cells reflow without reserved empty slots. The sheet uses an opaque near-black background.
- Public Message starts closed for each room. The room owner can open/close it; the setting is broadcast to current members and sent to new joiners. The server rejects room messages from non-owner/non-admin members while closed, with the existing small closed-message notice on the client.
- Entry Effect and Gift Effect are initialized and persisted OFF when Fourgride opens.
- User blocks are shared across Public Profile, Room Profile, and ChatPage through the server blocks collection. Private-message delivery is rejected in either direction when either participant has blocked the other. Room join is rejected when the room owner and joining account have a block relationship.
- Block verification, native build, server deployment, and multi-account runtime tests remain pending; do not treat source commits alone as verified behavior.

### Native Room Info sheet layout update
- Reduced the Room Info sheet height from 60% to 55% of the available screen height.
- Removed the centered “Room Information” heading. Room DP now overlaps the sheet's top edge at the horizontal center; room name and ID remain visible above the tabs on both tabs.
- Moved the existing three-dot action (for non-owners) and Share control together to the top-left.
- Reordered the Room info tab content: tabs, then a white/10 Room owner card (owner avatar/name, “Room owner” subtitle, and the existing chat icon asset), then Announcement.
- Kept Members list behavior and existing actions unchanged. Removed the green “Copied!” label from the room ID row while retaining the copy action.

### Seat menu Invite → Active User picker
- The seat action menu's Invite option now opens the existing Active Users sheet layout, titled "Invite User".
- It lists the same currently active room users with their avatar, name, country flag and Lv.1 display; each row has a blue Invite pill on the right.
- Selecting Invite sends the existing `room_seat_invite_send` event to that selected active user and targets the first available unlocked seat.
- Room Profile's existing Invite action and implementation were not changed.


## 21. Fast homepage room-card entry (2026-11-07)
- `MainActivity.kt`: ordinary room cards already marked unlocked now open immediately without waiting for separate Render `/api/room-access` and `/api/room-kick-status` requests. This removes two HTTP round trips from the normal room-entry critical path and prevents transient preflight failures from showing “Please try again” for unlocked rooms.
- Locked rooms still use the existing password preflight and grant flow. The server-side Socket.IO `room_join` handler remains authoritative for kick bans, owner blocks, and the current lock state before admitting the socket to room presence.
- No room UI, seat, voice, chat, or moderation behavior was intentionally changed. Real-device verification and native APK CI remain pending.


## 22. Immediate block handling for chat and room entry (2026-11-07)
- `MessageScreen.kt`: the chat screen now waits for the initial block-status lookup before allowing text send. If either account has blocked the other, it shows the existing compact black popup immediately and does not add an optimistic outgoing text bubble. Accepted outgoing text is rendered only after the server emits `private_message_sent`; rejected messages therefore never flash in chat.
- `Zyora-Server/server.js`: `POST /api/room-access` now checks owner/user block relationships and active room kick bans before granting entry, alongside the existing lock/password validation.
- `MainActivity.kt`: every room entry is gated by that single access response before `RoomPage` is mounted. Blocked users receive the owner-block popup and kicked users receive the existing timed restriction notice, so neither briefly sees room content first.
- UI layout, room seats, voice, and unrelated chat behavior were not intentionally changed. APK/real-device verification remains pending.


## Chat / profile danger-sheet styling alignment (2026-11-07)
- Aligned the three-dot action sheets used by Public Profile, Room Profile, and Room Info: the action card is pure black, while the existing Cancel button remains blue.
- Made the sheet sit slightly lower by reducing its bottom spacing, widened the sheet content slightly, brought the black card closer to Cancel, and reduced both card and Cancel corner radii slightly.
- Report/Block/Unblock/Room Follow actions and their behavior remain unchanged. Native APK build and on-device visual verification remain pending.


## Remove background mobile-message notifications (2026-11-07)
- Removed the persistent background Socket.IO message-listener foreground service and boot receiver that displayed the ongoing “Zyora is running” notification and generated Android system message notifications while the app was backgrounded.
- Removed the unused local Android message-notification helper, the associated manifest components/permissions, and the POST_NOTIFICATIONS runtime permission request.
- Kept the existing in-app chat banner/unread indicators and the separate Room Music foreground service/notification required for active media playback. No screen layout or unrelated feature behavior was intentionally changed.
- Native build and device behavior verification pending.


## Native responsiveness and notification cleanup follow-up (2026-11-07)
- Home room-list refresh now respects the process-wide 15-second successful-refresh window when Home is recreated during tab navigation. This avoids immediately repeating the same full room-list download and Room DB replacement while keeping the cached snapshot visible and retaining periodic full refreshes for deleted-room correctness.
- Confirmed the background message listener, boot receiver, Android message-notification helper, runtime notification permission request, and their manifest registrations are absent from the current source tree.
- The Room Music foreground playback notification and in-app chat banners/unread badges remain intentionally separate.
- Build verification for this follow-up is pending.

## 21 Room kick acknowledgement order correction (2026-11-07)
- A kick received while inside a room now immediately returns the app to the Home tab and shows the existing kick-details acknowledgement card there.
- Tapping **Got it** dismisses that card and then shows the existing compact **You can't join until...** restriction popup using the same kick expiry timestamp.
- The pre-entry access-check path also routes a kicked user to Home before showing the acknowledgement card.
- Kick enforcement, expiry, owner details, popup styling, and room access rules are unchanged; only the presentation/navigation order was corrected.
- Status: **Implemented in repository**. APK CI and real-device behavior still require verification.

## Room Info Room DP square shape restore (2026-11-07)
- Restored the Room Info room display picture to a true square crop with square corners.
- Kept its existing 80dp × 80dp dimensions and -40dp vertical offset, so it remains half outside and half inside the top edge of the sheet.
- No other Room Info positions, dimensions, controls, or UI styling were changed.
- Status: committed; APK build/device verification pending.


## Room Info DP corners and public-message realtime root fix (2026-11-07)
- RoomInfo.kt: restored 12dp rounded corners on the existing 80dp Room DP while preserving its centered -40dp overlap, size, border, and all other sheet UI.
- Zyora-Server/server.js: fixed the public-message owner authorization root cause. The settings handler previously compared the owner's socket identity directly with the room ID; those identifiers are not guaranteed to match, so the server silently discarded the owner's toggle. Room join now records server-verified owner identity aliases and only those verified identities can change room settings.
- Public Message ON/OFF is now persisted in MongoDB and refreshed into the server cache on every room join. Each room-message send checks the persisted shared setting, preventing another server process or stale in-memory cache from continuing to reject messages after the owner opens Public Message.
- While the setting is OFF, only the verified room owner and verified room admins can send; when ON, all joined room members can send. The existing Socket.IO room broadcast and message UI are retained.
- Status: source commits completed. Render deployment, native APK CI, and two-account real-time verification are still required; do not claim runtime success until those checks pass.

## 16. Room lifecycle background-work cleanup (2026-11-07)
- `android-native/app/src/main/java/com/hurry/voicechat/RoomPage.kt`: room-only inventory and room-follow network fetches now run in a `SupervisorJob`-backed IO scope owned by the room `DisposableEffect`.
- On room disposal, the scope is cancelled and queued main-thread callbacks are removed before socket listeners are released. This prevents pending room fetches and delayed UI callbacks from continuing to mutate state after leaving or switching rooms.
- Socket retention, room membership, seat persistence, event payloads, and visible UI are unchanged.


## 2026-11-07 — Room Admin management
- Redesigned native Room Admins and Room Members bottom sheets with black surfaces, 12dp top corners, 45%/50% heights, 5-admin cap, add/remove controls, and persistent room admin list updates through `/api/rooms`.
- Updated seat invite buttons to use `public/IMG_20261107_153857.png`; admin badge uses `public/IMG_20261107_154649.png`.
- Admins inherit room moderation/seat controls while the Room Settings icon remains owner-only. Admin saves no longer dismiss Room Settings.
- Build verification remains pending; no successful Actions run was returned for the commits in this change.


### Room admin badge and moderation visibility (2026-11-07)
- Native Room Info includes saved room admins in the Members list and shows the dedicated room-admin badge beside their names.
- Native Active Users and room chat use the dedicated admin badge asset; the owner badge remains unchanged.
- Room admins receive the same Fourgride tools visibility and Room Profile moderation controls as the owner, while the Room Settings entry remains owner-only.
- Room admin IDs are preserved when a room API response omits the admin field; both legacy `admin` and alternate `admins` arrays are accepted. The Room members Add Admin image is slightly larger (30dp to 36dp).


## 2026-11-07 — Emoji image cache and network-stall guard
- `android-native/app/src/main/java/com/hurry/voicechat/Emojipicker.kt`: replaced the process-wide unbounded, unsynchronized emoji bitmap map with a thread-safe 12 MiB LRU cache. Reopening the picker can no longer retain every previously decoded sticker bitmap indefinitely.
- Added connection and read timeouts to sticker image downloads so a stalled image host cannot leave the picker coroutine waiting without a bound. Bitmap state is published after the IO decode completes; existing sticker URLs, grid, click behavior, and static-first-frame GIF behavior remain unchanged.
- Status: implemented in repository. Native APK CI and repeated-open / low-memory device checks remain pending; this is a focused confirmed-risk fix, not certification that every app bug or hang has been eliminated.


## 2026-11-07 — Leaderboard image cache memory bound
- `android-native/app/src/main/java/com/hurry/voicechat/Leaderboard.kt`: replaced the process-lifetime unbounded concurrent image map with a thread-safe 16 MiB LRU cache sized from decoded image dimensions. Repeated leaderboard visits no longer retain every distinct podium/avatar bitmap indefinitely.
- Added connection/read timeouts to leaderboard image downloads; existing image URLs, rendering, tabs, and screen UI remain unchanged.
- Status: source committed; APK compile and on-device memory/scroll verification pending.


## 17. Active member/profile loading performance fix (2026-11-07)
- `ActiveUsers.kt`: country/profile resolution for visible active users now runs concurrently with a four-request cap instead of serially. Duplicate account IDs are removed before requests.
- `RoomInfo.kt`: member country/profile resolution uses the same bounded concurrency, avoiding a long sequential wait when opening Members in a room with many followers/admins.
- `GreenColorRemovalImage.kt`: image downloads now have explicit 7-second connect and 9-second read timeouts and always disconnect, preventing a stalled image host from holding a background decode indefinitely.
- No composable layout, colors, navigation, room permissions, voice/chat behavior, or user-facing feature was intentionally changed.
- Status: **Implemented in repository**. Native APK CI and real-device sheet/image regression checks are pending; do not treat these changes as device-verified.


## 18. Image-network hang protection (2026-11-07)
- `InviteFriends.kt`, `StorePage.kt`, and `RoomPage.kt`: replaced unbounded `URL.openStream()` image downloads with HTTP connections using 7-second connect and 9-second read timeouts, and guaranteed disconnect in `finally`.
- Image processing remains on `Dispatchers.IO`; image transformations and visual output are unchanged.
- No layout, feature, interaction, navigation, room, chat, voice, or wallet behaviour was intentionally changed.
- Status: committed to the default branch; native APK build and device regression verification still pending.

## Native performance hardening — 2026-11-07
- Room audio teardown now detaches Socket.IO listeners, stops audio producers, and updates speaking state immediately, then joins worker threads and releases audio resources on a dedicated daemon cleanup thread. This avoids blocking Compose navigation/room exit on the main thread while preserving the existing room voice protocol, mute flow, media routing, and UI.
- Fixed the Kotlin coroutine `async` receiver usage in Active Users and Room Info so the native APK build does not fail on the deprecated scope-less async call.


## 2026-11-07 — Room chat overlay and entry banner refinement
- Native RoomPage moves the dedicated room-chat message list into the upper room area while chat is open, over a black/30 dim layer; the normal room message list is hidden during chat mode and restored when chat closes.
- The existing `room_chat_view` Socket.IO room subscription and server-side split remain the delivery rule: users currently on the dedicated chat page receive `room_chat_message` together, while users not on that page continue receiving the existing `room_message` presentation.
- Chat list continues auto-scrolling to the latest message when new messages arrive.
- The room-entry banner no longer shows the owner/admin tag image beside the entrant; “Enter the room” is bold.
- No room voice, seat, permission, or public-message behavior was intentionally changed. Native APK build and two-device chat-mode verification remain pending.
## 18. Country flag PNG rollout (2026-11-08)
- Added shared native Compose component `CountryFlagImage.kt`, resolving country names and ISO aliases to the nine supplied PNGs in `public/` (India, Singapore, Pakistan, USA, South Africa, Turkey, UK, UAE, South Korea).
- Replaced visible country emoji flags with those PNG images in New User After Login country selector, Home country pills and room cards, in-room Active Users, Room Profile, Public Profile, and Search user/room results.
- Kept the existing country options, country values, filtering behavior, and surrounding layouts unchanged. Countries without one of the nine supplied images render no flag rather than an emoji.
- Images load from the repository's raw public assets through Coil's normal image caching; no country-selection or profile data format was changed.
