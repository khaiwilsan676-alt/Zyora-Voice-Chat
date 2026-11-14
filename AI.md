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
- Added shared native Compose component `CountryFlagImage.kt`, resolving country names, ISO aliases, and flag emoji to the nine supplied PNGs in `public/` (India, Singapore, Pakistan, USA, South Africa, Turkey, UK, UAE, South Korea). The room-list API and followed-room parser now read countryName/country/countryCode/country_code/nationality/location/flag fields so Homepage room cards do not silently lose country data when backend records use alternate field names.
- Replaced visible country emoji flags with those PNG images in New User After Login country selector, Home country pills and room cards, in-room Active Users, Room Profile, Public Profile, and Search user/room results.
- Kept the existing country options, country values, filtering behavior, and surrounding layouts unchanged. Countries without one of the nine supplied images render no flag rather than an emoji.
- Images load from the repository's raw public assets through Coil's normal image caching; no country-selection or profile data format was changed.
- 2026-11-08 follow-up: country flag resolution now handles flag-emoji-only values, embedded flag emoji, country names, and ISO alpha-2/alpha-3 aliases (including codes converted to regional-indicator emoji by the existing country resolver) before loading the matching PNG from GitHub Raw URLs (`https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/`). The shared helper was cleaned up to remove malformed duplicate mapping entries; no screen layout or country-selection behavior was changed.


## Active Users sheet caching and role-tag spacing (2026-11-08)
- Active Users country values are now stored in app-private SharedPreferences with no time-based expiry. The same account's resolved country is reused after reopening the sheet and across app restarts, avoiding repeated profile lookups. Only nonblank country values are cached.
- Reduced Owner and Room Admin tag images in the Active Users sheet from 19dp × 42dp to 16dp × 36dp and tightened the gap beside the username. No other sheet layout or behavior changed.
- Native APK build and device verification remain pending.


## Room Info share lists and persistent member cache (2026-11-08)
- RoomInfo.kt: the “Share in Zyora” sheet now loads the signed-in user's Fans and Following lists, combines them into one list, excludes the current account, and deduplicates accounts that appear in both lists. Existing share-row UI and invitation action are unchanged.
- Both social lists use the existing SocialFollowApi persistent NativeIndexDatabase cache; cached rows remain available across app restarts and are used as fallback when refresh requests fail.
- Room Info Members now restores each member's saved name, avatar, and country from NativeIndexDatabase immediately, then refreshes profile-country data with the existing four-request concurrency cap and saves the refreshed result persistently. Cache keys are account-based, so the same profile can be reused across rooms.
- Status: source committed. Native APK build, live Fans/Following API response, and device/offline cache verification remain pending.


## Room mini music controller refinement (2026-11-08)
- RoomPage.kt: increased only the compact in-room music controller's volume bar height from 5dp to 8dp and changed its active fill from green to blue (#3B82F6). Existing slider interaction and volume service action are unchanged.
- Removed the hard-coded 180-second/3:00 duration fallback for room mini-player tracks. When a track lacks duration metadata, the mini-player resolves the actual duration from the track URI with MediaMetadataRetriever on Dispatchers.IO, then displays that duration; while metadata is unavailable it does not falsely display 3:00.
- No card size, position, controls, playback, queue, repeat, or other UI was intentionally changed. Native APK build and device playback verification remain pending.


## Native online-smoothness pass — 2026-11-08
- `HomeScreen.kt`: the Home room list continues to render the in-memory/Room DB snapshot immediately, while the full authoritative room refresh is now throttled to once per 60 seconds instead of every 15 seconds. Failed/offline refresh attempts also wait 60 seconds before retrying, reducing repeated background network work and avoiding replacement of a valid cached snapshot with an empty list.
- `HurryApi.kt`: room-list HTTP connect/read timeouts are now 5 seconds (previously 12 seconds), non-2xx responses are treated as failed refreshes, and the connection is always disconnected in `finally`. Parsing and room model fields remain unchanged.
- No UI, navigation, room/voice protocol, or user-facing feature behavior was intentionally changed. This is a focused reduction of Home room-list network traffic; it does not yet establish app-wide online/offline performance parity.
- Status: source committed. Native APK CI and online/slow-network/offline device checks remain pending.


## 2026-11-08 — Home refresh behavior
- Home room-list automatic full-snapshot refresh interval is five minutes (300 seconds), not 60 seconds.
- Pulling down from the top of the Home list requests an immediate refresh; the shared cache remains visible while the request runs.
- Existing UI visuals and room data behavior are preserved.


## 2026-11-08 — Room Info owner chat
- The chat icon in the Room Info white/10 owner card opens the existing native direct-chat screen for the room owner.
- Room Info closes before opening chat; owner account ID is resolved from accountId, then id, then uid.
- No owner-card layout or styling changes.


## 2026-11-08 — Room owner chat visibility
- Moved the Room Info owner-card chat destination to the topmost layer of the RoomPage overlay stack so NativeChatScreen is not covered by later room overlays.
- Preserved the existing owner identity, chat screen, and UI.


## 2026-11-08 — Room owner chat routing fix
- Room Info owner-card chat now resolves the recipient using owner account ID, then owner UID, then owner ID.
- RoomPage no longer substitutes the room ID into the owner account ID field before the chat tap handler runs.
- The chat action closes Room Info and opens the existing NativeChatScreen for the resolved owner; no UI changes.


## 2026-11-08 — Room Info owner chat navigation fix
- Room Info owner-card Chat tap now closes other room sheets and opens the direct owner chat as a full-size topmost overlay.
- Owner receiver ID resolves from account ID, Firebase UID, then owner ID; self-chat is prevented.
- No Room Info card styling or layout changes.

### Room owner profile moderation visibility
- Hide the room moderation action row when the viewed profile belongs to the room owner, including when opened by a Room Admin.
- Keep the existing moderation row behavior for other users and leave profile UI styling unchanged.

- Room Gift Picker: removed the dialog's dim-behind effect so the original room background remains visible above the sheet; reduced sheet height slightly from 50% to 47%. Gift picker contents and styling remain unchanged.
- Room Gift Picker: keep the underlying room background transparent/visible above the sheet, and slightly reduce the sheet height to 44%; gift picker content and styling remain unchanged.

## 2026-11-08 — Room Gift Picker coin value and Hot artwork
- GiftPicker.kt: fixed the gift-card coin icon to use the same resolved public-asset URL as the wallet coin icon, so the coin image loads before the value.
- Moved the coin icon/value row 2dp upward, closer to the gift name.
- Added a subtle bottom mixed-fade overlay to Hot gift artwork only; Lucky artwork and other picker controls remain unchanged.
- Native APK build and device visual verification remain pending.

## 2026-11-08 — Gift Send button brightness
- GiftPicker.kt: removed the dimming alpha from the Send button, so it stays at its normal full brightness whether or not a gift is selected.
- The existing click eligibility and send behavior are unchanged; the button remains non-actionable until a gift is selected.
- Native APK build and device verification remain pending.

## 2026-11-08 — Gift Send button brightness
- GiftPicker.kt: keep the Send button at full alpha (1f) whether or not a gift is selected. The existing click guard remains: sending is only actionable when a gift is selected and a send is not already in progress.
- No button colors, gradient, sizing, or layout changed.
- Native APK build and device verification remain pending.


### Private chat prohibited-word filter (2026-11-09)
- Native `MessageScreen.kt` checks outgoing private-chat text before emitting `private_message`.
- Expanded the blocked Hindi/Hinglish and English profanity list, including common spelling variants. Common leetspeak substitutions are still checked only as part of matching abusive words; ordinary digits and letters (including standalone `0` and `o`) remain allowed.
- Rejected text is not emitted and remains in the draft for correction. The existing small centered Black/70 toast still displays exactly: `Can't send text-prohibited`.
- No other chat UI or message behavior is intentionally changed.
- Status: source committed; native build and runtime verification pending.


## 2026-11-10 — Room admin seat moderation
- `RoomPage.kt`: room admins now receive the same seat Lock/Unlock and occupied-seat Mute/Unmute authorization as the room owner from both the seat action sheet and seated-user profile moderation callbacks.
- Empty-seat locking continues to use the existing `room_seat_action` `lock` event; the backend already validates owner/admin permissions and broadcasts the authoritative seat snapshot. No unrelated room, voice, chat, or UI behavior was changed.
- Native APK build and multi-account runtime verification remain pending.

- Native room owner auto-seat: on the first valid seat snapshot after entering a room, the owner is placed in seat 1 only when seat 1 is empty and unlocked and the owner is not already seated. If seat 1 is occupied, the existing seat display/behavior is preserved. A missing seat-1 entry no longer consumes the one-time auto-seat attempt. No UI changes.


## 15. Chat message copy and clear feedback (2026-11-10)
- Native room chat messages and private MessageScreen chat bubbles support clipboard copy after a two-second press-and-hold; the small centered black/70 confirmation reads `Copy to clipboard`.
- Clearing room chat shows the black/70 confirmation `Public message Cleared`.
- Tapping the room chat black/50 layer closes it using the same state change as the top down-arrow control.
- No chat layout, colors, message delivery, or storage behavior is intentionally changed.

## 2026-11-10 room admin seat lock permission fix
- `backend/server.js`: room seat moderation now matches the acting socket against all available account-ID and Firebase-UID aliases.
- Admin records are normalized across string IDs and object records, retaining `accountId`, `userId`, `id`, and `uid` instead of discarding all but the first populated field.
- Fixes the server rejecting a legitimate room admin's `room_seat_action` lock/unlock request, which caused the client seat state to revert and appear to unlock repeatedly.
- No UI, seat layout, or unrelated room behavior changed.

## 2026-11-10 — Prevent wallet balance resets from profile saves
- Root cause: `PUT /api/users` spread the entire client profile payload into MongoDB `$set`, so stale profile snapshots could overwrite the server-owned `coins` / `diamonds` fields with zero or old values.
- `backend/server.js`: profile writes now strip wallet-owned fields (`coins`, `diamonds`, legacy `balance`, wallet timestamps, and daily-check-in credit keys). Wallet endpoints and atomic wallet operations remain the only balance writers; Mine → Wallet continues to use MongoDB `users.coins` as its canonical balance.
- `CoinsNativePage.kt`: every wallet page open now refreshes from the canonical server endpoint instead of allowing a 15-second local sync throttle to retain a stale zero. Failed refreshes leave the last known cached snapshot intact.
- UI unchanged. Backend deployment, native APK build, and multi-account balance persistence tests remain pending.

## 2026-11-10 — Live Render wallet reset fix (Zyora-Server)
- Correct live backend repository is `khaiwilsan676-alt/Zyora-Server` (Render service `zyora-server`, `render.yaml` confirms `npm start`, `autoDeploy: true`). Earlier wallet protection changes in this app repository's `backend/server.js` were not the deployed Render source.
- Live root cause: `Zyora-Server/server.js` `PUT /api/users` merged stale client wallet fields into a replacement MongoDB user document via `replaceOne()`, allowing profile saves to reset `coins`/wallet metadata. Profile writes now strip server-owned wallet fields before replacement.
- Migration root cause: first wallet access without `walletStorageMigratedAt` could replace an already-present MongoDB balance with a stale legacy GitHub wallet value (including zero). Migration now preserves any valid existing MongoDB coins/diamonds value and consults legacy sources only when the MongoDB field is absent/invalid.
- Wallet credit/debit, diamond exchange, and gift recipient credit/rollback now mutate the exact resolved MongoDB user document by `_id`, avoiding a broad legacy-ID `$or` query accidentally selecting a different user document.
- Live backend commit: `1ccb235f9589345e2bc26a523529dcf1a4e710da`. Render auto-deploy is configured, but deployment completion, existing-balance recovery, and live persistence testing must still be verified. No UI changes.


## Wallet canonical identity resolution follow-up (2026-11-10)
- Root cause: wallet lookup used a broad unordered MongoDB `$or` across Firebase UID, legacy IDs, and public Zyora account IDs. When duplicate/legacy profile documents existed, `findOne` could resolve the wrong document and return a zero balance even when the canonical wallet was stored on another profile.
- Actual Render backend is `khaiwilsan676-alt/Zyora-Server/server.js`. `ensureWalletUser` now resolves `auth_accounts` first, prefers the Firebase UID profile, falls back to the mapped permanent account ID, then uses legacy aliases only as a last resort. It rebinds the resolved profile to the canonical identity and all balance writes continue targeting that exact MongoDB document.
- Backend commit: `767f1a0216f9b0fa07e096eab739a2edc7ed2e21`.
- Verification: source committed; Render deployment, actual account's MongoDB document, and repeated wallet refresh on device still require live verification. Do not claim the zero-balance issue is fully verified until those checks pass.


## Daily Check-in repeated sign-in root-cause fix (2026-11-10)
- Root cause was a native/live-backend API contract mismatch. Native `HomeScreen.kt` GET parser expects `claimedToday`, but the deployed `Zyora-Server/server.js` GET route did not return it, so the UI kept treating an already-claimed day as unclaimed. Native POST sends only `userId`, while the deployed backend required `dateKey` and `currentDay`, so claims failed validation. The live route also only advanced a counter and did not credit the configured reward wallet/items.
- Replaced the actual Render backend daily-check-in routes with server-derived India date (`Asia/Kolkata`), canonical Firebase UID/permanent Zyora account identity resolution, persisted `claimedDate`/7-day progress, and a `claimedToday` GET response with `Cache-Control: no-store`.
- POST now matches the native payload (`userId` only), credits the configured 7-day coin rewards to the canonical MongoDB wallet using a deterministic idempotency marker, persists reward transaction records, and grants day 3/5/6/7 permanent inventory rewards idempotently. Retry repairs an interrupted credit without double-crediting; repeat claims on the same India calendar day do not advance the streak or pay twice.
- Actual Render backend commit: `7e32f062b0e119057053d9c112d7d4b6c5595797` in `khaiwilsan676-alt/Zyora-Server/server.js`. No UI changes.
- Verification: source committed; Render deployment and live device/MongoDB end-to-end claim still need verification. Do not claim production behavior is verified until deployed and tested.

## Mine tab recent-room history (2026-11-10)
- `android-native/app/src/main/java/com/hurry/voicechat/MineScreen.kt`: reduced the Mine header Search/House icon spacing to 3dp and removed the Following/Recent selected underline without changing the tab labels or room-card styling.
- Added per-account persistent Recent history. A room is recorded only after the room-access API confirms entry; revisiting the same room moves it to the top and refreshes its visit time.
- Recent history expires each room two hours after its latest successful entry, keeps at most 15 unique rooms, and displays newest first in the existing two-column room-card layout. Expired entries are cleaned when the list is loaded.
- `MainActivity.kt` records successful room entry; SharedPreferences stores the recent-room list locally per account. Build/device behavior has not been run-verified in this change.


## 2026-11-10 — Mine Recent rooms IndexDB persistence
- `MineScreen.kt`: Recent room history now uses the shared native `index.db` (`profile_cache`) through `NativeIndexDatabase`, keyed separately for each signed-in account; it no longer stores this feature in SharedPreferences.
- A room is recorded only after the existing room-access check confirms entry. Re-entering the same room moves it to the top; history contains at most 15 unique rooms, newest first, with older entries dropped. No time-based expiry.
- Mine header Search/House spacing remains 3dp, and Following/Recent selected underlines remain removed. Room-card layout and styling are unchanged.
- Native build and device persistence verification remain pending.


## 2026-11-11 — Avatar frame rendering
- Native `NativeAvatarFrameOverlay` now renders equipped Avatar Frame videos with the same Store preview green-screen removal and top-fade behavior; static frame images remain fit-scaled.
- The shared overlay is used by Me, Public Profile, Room Profile, and occupied room seats. Removed the duplicate overlay call from Public Profile so a frame is not drawn twice.
- Public Profile's Frame section now loads the target user's unexpired Avatar Frame inventory and shows the actual Store image/video preview with its star row only, without an extra card.
- Store Buy and Send flows were not changed. Native build and device verification remain pending.

## 2026-11-11 — Complete Bag frame equip flow
- Native `StorePage.kt`: opening Bag now selects the Avatar Frame tab by default; switching back to Store restores Vehicle as the default tab. Existing Store card layout and Buy/Send behavior are unchanged.
- Bag Use/Remove now waits for a successful `/api/store/equip` response before changing the local equipped state. Failed requests leave the displayed equipped item unchanged, and duplicate taps are blocked while the request is active.
- Public Profile's owned-frame showcase now falls back to the exact catalog video/image when the inventory response omits or returns an empty media field, matching the Store asset for each frame.
- Existing duration countdown, expiry removal, Store purchase endpoint, and shared avatar overlay surfaces remain in place.
- Verification: source committed; native APK build and real-account Bag/profile/seat runtime checks remain pending.

## 2026-11-11 — Realtime avatar frame refresh and expiry
- Native `StorePage.kt`: a successful Avatar Frame equip/remove increments the account's shared Compose refresh version. Me and profile overlays reload the server-authoritative equipped frame without requiring the screen to be recreated.
- Native `NativeAvatarFrameOverlay` and `NativeOwnedFrameShowcase` now clear equipped overlays and owned-frame showcase entries when their server expiry timestamp is reached.
- Native `RoomPage.kt` listens for `avatar_frame_updated` and refreshes the affected user's seat overlay. The listener is removed with the room socket lifecycle.
- Backend `Zyora-Server/server.js`: after the inventory equip write succeeds, the server emits `avatar_frame_updated` to the owner's personal socket room and to active room(s) containing that user. Payload: `{ userId, itemId, equipped, updatedAt }`; no media URLs or wallet data are broadcast.
- Store Buy/Send behavior and existing UI layout are unchanged. Native build, Render deployment, and two-account runtime verification remain pending until confirmed by CI/deployment/device tests.

## 2026-11-11 — Store Theme ownership, Room Settings and expiry
- Room Settings now loads every unexpired purchased Store Theme from the account inventory, not only themes marked equipped. Purchased themes remain listed whether currently applied to a room or not.
- The Room Theme picker shows each purchased theme's live expiry countdown and hides it as soon as it expires. The existing built-in and custom theme choices are unchanged.
- RoomPage automatically switches an applied purchased theme back to Forest Night when its ownership expires or the inventory record is no longer valid.
- Actual Render backend `Zyora-Server/server.js`: inventory reads purge expired store inventory records in MongoDB and return only active items with `Cache-Control: no-store`. Room reads also validate a selected Store Theme against its active inventory record and persist the default theme when the purchase has expired.
- Store purchase and Bag countdown already use the shared inventory response; Theme purchases therefore appear in the Theme Bag tab with the same countdown as other owned items.
- Status: source committed. Native APK compilation, Render deployment, and real-account purchase/expiry/room-background verification remain pending.

## 2026-11-11 — Store theme ownership and expiry consistency
- Backend `GET /api/store/inventory/:userId` now deletes expired inventory records before returning only unexpired purchases and sends explicit no-cache headers. This prevents stale inventory snapshots from restoring expired Store themes in Bag or Room Settings.
- Native `RoomSettingPage.kt` now removes purchased themes from its live theme list at their exact expiry time while the settings screen remains open; applying a purchased theme remains a separate room-background choice and does not control whether the purchase appears in the list.
- Existing Store countdown and RoomPage active-theme expiry fallback are preserved. APK build and real-account purchase/expiry verification are still required.


## 2026-11-11 — Purchased room theme inventory refresh
- Native `RoomSettingPage.kt`: refreshes the account's server-authoritative Store inventory whenever the Room Theme picker opens, and explicitly bypasses HTTP caches. This prevents a one-time screen-entry inventory snapshot from hiding a newly purchased Theme or showing stale ownership.
- Purchased Theme entries remain independent of the equipped flag; Room Settings lists every unexpired owned Theme, and expiry filtering removes entries when their server-provided expiry is reached.
- Verification: source committed; native APK build and live purchase/apply/expiry checks remain pending.


## 2026-11-11 — Purchased Store themes in Room Settings
- Room Settings refreshes the signed-in user's Store inventory whenever the Theme picker is opened, with HTTP caching disabled, so newly purchased themes appear even when they have not been applied yet.
- Purchased theme previews now use the same public asset repository as Store catalog images; previously Room Settings and the room background resolved Store image filenames against the Zyora asset repository, causing broken/missing theme images.
- Room background inventory lookups also bypass HTTP caches and use the Store asset source for purchased theme backgrounds. Existing purchase duration (30 days), Bag countdown, and server-side expiry filtering remain authoritative.
- Verification: source changes committed; native APK build and real-account purchase/apply/expiry checks still require CI/device verification.


## 2026-11-11 — Store purchase and inventory hardening
- `Zyora-Server/server.js`: Store purchases now reserve the unique user/item inventory slot before wallet debit, preventing parallel Buy taps from charging twice; failed debits clear the reservation, and failed persistence after debit triggers a wallet refund.
- Store inventory reads exclude incomplete pending reservations and remain non-cacheable; equip requests require an active, unexpired owned item and clear competing equipped items only within the same tab.
- `StorePage.kt`: inventory refresh requests bypass HTTP caches and successful purchases replace the local owned ID rather than appending duplicate entries.
- UI unchanged. Android build and live Render deployment/runtime verification are still pending.


## 2026-11-11 — Lucky Bag wallet identity fix
- `RoomPage.kt` now resolves the Lucky Bag sender and claimant identity using the same authenticated `hurry_auth.uid` priority as Mine > Wallet, falling back to the room account ID only when the authenticated UID is unavailable. This prevents Lucky Bag debits/claims from targeting a different wallet identity than the Wallet page.
- `luckybag.kt` no longer reads or sends the stale local Room wallet balance; the server's canonical wallet balance is authoritative for Lucky Bag send validation and debit.
- Lucky Bag amount deduction remains server-side and atomic; successful claims credit the claimant's canonical wallet, one claim per user, and unclaimed bags are refunded after the existing five-minute expiry.
- Status: source committed. Android APK build, Render deployment, and two-account live send/claim/refund verification remain pending.


## 2026-11-12 — Native APK dependency coordinate fix
- Corrected Coil 3 GIF and OkHttp network artifact coordinates in `android-native/app/build.gradle.kts` to the `io.coil-kt.coil3` group. The previous coordinates referenced nonexistent modules and failed `:app:checkDebugAarMetadata`.
- Preserved all app UI and behavior; no screen/layout changes.
- The next `Build Zyora Native APK` run must confirm dependency resolution, APK compilation, and AAB packaging.


## 2026-11-12 — Native Kotlin compile fixes (build #2478)
- Moved coin-pack `Modifier.weight` declarations into their respective `RowScope`, preventing the Float/weight invocation compile error while preserving the same 3-column layout.
- Added the missing `kotlinx.coroutines.delay` import in `RoomPage.kt`.
- Moved theme expiry timer state and `LaunchedEffect` out of the `LazyVerticalGrid` DSL receiver and into `ThemeFullScreen` composable scope; grid still receives the filtered theme list and countdown display is unchanged.
- No UI redesign or feature changes.

## Store wallet canonical identity fix — 2026-11-12
- `backend/server.js`: `POST /api/wallet/initialize` now resolves/creates the wallet through the same deterministic `ensureWalletUser(userId)` path used by Store inventory reads and purchase debits. Removed its broad `$or` lookup, which could select a different duplicate legacy user document and make the displayed wallet balance disagree with the Store purchase debit.
- Store purchase endpoint remains server-authoritative: item ID, category, duration, and fixed price are validated; only the selected item is debited and written to inventory.
- No Store UI/layout changes. Native Build 2 remains the only native workflow present in `.github/workflows`; no Native 1 workflow file was present to remove.
- Backend deployment and live purchase verification are pending.


## 2026-11-12 — Native Store single-item purchase feedback fix
- `android-native/app/src/main/java/com/hurry/voicechat/StorePage.kt`: scope the in-progress indicator to the exact selected item ID, so unrelated Store cards never appear to be purchasing when one card is tapped.
- Apply the server-returned wallet balance on both successful and rejected purchase responses, so a stale displayed balance is corrected after an insufficient-coins response.
- The live `Zyora-Server/server.js` purchase endpoint already validates the fixed price against the selected item ID and serializes purchases per account; it does not bulk-purchase other cards. No catalog, price, or visual layout changes.
- Native source commit: `59f16cbdf215584c7e74ccf93cf6e7958714aac9`. Build and live wallet purchase verification are still pending.


## 2026-11-12 — Native Store single-item confirmation and wallet debit normalization
- `android-native/app/src/main/java/com/hurry/voicechat/StorePage.kt`: grid-card Buy now selects that exact item and opens the existing purchase sheet; the grid no longer sends a direct purchase request. The confirmation sheet is the single purchase submission path.
- `Zyora-Server/server.js`: normalize legacy MongoDB `coins` and `diamonds` values to non-negative integer BSON numbers during wallet resolution. A numeric string could display as a valid balance in the app but fail MongoDB's numeric `$gte` debit condition, falsely returning Insufficient coins.
- Commits: native `82f1c2d1e2e5b6ec29991084faf480a2641c482a`; server `340c68162b156552be71aaa32b7167be5fb1d0cd`. Render deployment and Native Build 2/live purchase verification remain pending.


## 2026-11-12 — Native Store direct purchase, Bag ownership, and shared wallet
- StorePage.kt: removed the Buy confirmation bottom sheet. Tapping Buy on a card now posts a purchase for only that card; after success that same card changes to `Owned`. Bag continues to show owned cards with `Use/Remove` actions.
- Store purchase, inventory load, and equip now use the same canonical wallet identity as CoinsNativePage (Firebase UID first, then account ID), fixing inventory/Bag lookups against a different identity.
- Store and Bag header now display the canonical wallet coin balance; balance is refreshed from the wallet endpoint while open and persisted to the same `zyora_wallet_cache` balance key, including local recharge credits.
- Latest StorePage commit: `bd7abdcb12cec755aa4dceda838a0e56d3d731c4`. Native Build 2 compilation and live purchase/Bag verification remain pending.


## Store identity alignment correction — 2026-11-12
- Store inventory records must stay keyed by the permanent Zyora account ID because profile/room frame overlays and other Bag surfaces request inventory with that ID.
- Wallet reads use Firebase UID (same priority as CoinsNativePage); the server's `ensureWalletUser` resolves either UID or account ID to the same canonical MongoDB wallet document. Store purchase/equip/inventory use the account ID for inventory ownership while the server resolves its wallet debit to that canonical wallet.
- This corrects the earlier note that Store inventory itself should be keyed by Firebase UID.

- Final cleanup commit for the direct-buy flow (removed the unused purchase-sheet state/hooks): `806d6396f928503a8c6de32eacd7e89980431ace`. Native 2 workflow watches `android-native/**` on main, so this native commit should start its build automatically.


## 2026-11-12 — Store canonical wallet debit and Bag-only actions
- Native `StorePage.kt`: Store/Bag header no longer displays a coin amount. Bag cards no longer expose Send; owned cards retain only Use/Remove. Active-item expiry countdown is shown directly beneath the star row in Bag.
- Root cause of false Insufficient balance: Store inventory/purchase identity is the permanent Zyora account ID, while Mine > Wallet reads the canonical MongoDB wallet by Firebase UID. Passing only the inventory ID made Store debit depend on the auth-account alias mapping and could resolve a different/zero wallet document. Store purchase now sends both IDs; `Zyora-Server/server.js` keeps inventory keyed by account ID but reads/debits/refunds and broadcasts wallet balance using the exact Firebase UID used by Mine > Wallet.
- Store purchase balance refresh now uses only the server-owned MongoDB balance; device-local recharge credits are not treated as spendable server coins. UI layout otherwise remains unchanged.
- Commits: app `919d7e25f781b630c1e242d6dc732f9d0671d756`; server `f38ad8f87b8529cf99f7f17a5006dd4ac3552e4a`. Render deployment, Native Build 2 compilation, and real-wallet purchase/Bag runtime verification remain pending.


## 2026-11-12 — Store equip toggle drives existing room-entry vehicle video
- Bag equip action is a toggle: after a successful Use request the Store inventory marks that item equipped and the Bag button reads Remove; after Remove it is unequipped and the button reads Use. Only one item per Store tab remains equipped through the existing `/api/store/equip` endpoint.
- RoomPage already listens for `room_vehicle_entry` and renders the supplied video inside the existing room-entry card using `StoreVideoPreview`; no new Room UI or separate vehicle control was added.
- Root cause for vehicle video not appearing: the server never emitted the `room_vehicle_entry` event when a user entered a room. Zyora-Server now looks up that user's active, unexpired, equipped Vehicle in `storeInventory` and emits its exact stored video/item ID alongside the existing entry card. The Store inventory remains the only source of selection/control; the video plays for other room members when the equipped user enters.
- Server commit: `4e50fbed070b70459b05a90287548800068d880e`. Render deployment and in-room runtime verification remain pending.

## 2026-11-13 — Vehicle Bag preview and room-entry-only playback
- Native `StorePage.kt`: Vehicle cards in Bag now render the exact same catalog image/preview as their Store cards; Bag no longer starts the vehicle MP4. Store card appearance and Store preview behavior are unchanged.
- Vehicle MP4 playback is shown in the Room's main content when an equipped, unexpired vehicle owner enters; it plays once and hides on completion. The entry toast remains avatar/name only. Store inventory equip/Remove remains the only control; no separate Room video settings or player were added.
- Verification: source commit only; Native Build 2 and device room-entry playback checks remain pending.


## 2026-11-12 — Gender tags show icon only
- Native `PublicProfile.kt` and `RoomProfile.kt`: removed the numeric age text (and its spacing) from the compact Gender tag. The tag now renders only the existing male/female gender icon. Age data, profile age editing/display outside the Gender tag, and all other UI remain unchanged.
- Commits: `2c9eed8c52860cea5e935430184e78b7cbd30213` and `53074336e74e6ad31dcf7ac0c707252e42e51a33`. Native Build 2 compilation and device verification pending.


## 2026-11-13 — Move vehicle playback from entry toast into Room
- Native `RoomPage.kt`: removed vehicle video from the entry toast. The equipped vehicle video now plays once in the Room main area after the `room_vehicle_entry` event and hides when playback completes.
- Native `StorePage.kt`: the shared `StoreVideoPreview` supports optional looping/completion; Store previews retain their existing looping default. Store remains the only place to select, preview, Use, or Remove a vehicle.
- No separate Room video controls or settings were added. Native Build 2 and device playback verification remain pending.

## 2026-11-13 — Avatar frame preview and profile visibility
- Native Public Profile's Frame showcase now always displays the still catalogue image from the Store item; it no longer plays the frame MP4 inside the showcase. Equipped frame videos continue playing as transparent overlays on avatars.
- Equipped avatar-frame video playback is cached in the app cache directory and reuses the local file on later displays. The overlay is enlarged slightly and shifted upward by 4dp.
- Native avatar-frame inventory lookup now tries the account ID plus available UID/ID aliases with no-cache requests, fixing equipped-frame visibility when profile surfaces use different account identifiers. Applied to Me, Public Profile, and Room Profile; occupied room seats inherit the shared enlarged/cached video overlay.
- Store purchase, equip, expiry, and other profile/room UI behavior remain unchanged. Native build and device verification pending.

## 2026-11-13 — Room entry Vehicle video visibility
- Root cause: `RoomPage.kt` created the room-entry vehicle player before the full-screen room-theme `AsyncImage`, so the later theme layer covered the video. The player now uses a small positive z-index so the entry video renders above the theme/background.
- The existing server-side inventory lookup remains authoritative: only an unexpired, equipped Vehicle with a non-empty video is broadcast on `room_vehicle_entry`. If no Vehicle is equipped (including after removing the last equipped Vehicle), the backend sends no playable vehicle and the client does not invent a fallback.
- Store Preview, Bag ownership/equip logic, room layout, and other UI are unchanged. Native APK build and real-device room-entry/removal verification remain pending.

## 2026-11-13 — Larger avatar frames and profile inventory rows
- Shared native `NativeAvatarFrameOverlay` is enlarged to 1.28x for both video and still-image frames. This applies consistently to Me, Public Profile, Room Profile, and occupied room-seat avatars through the shared overlay.
- Public Profile Frame inventory now displays owned frame still images in a three-column grid, with additional frames wrapping to following rows; video playback remains limited to equipped avatar overlays.
- Public Profile Vehicle section now reads the user's unexpired owned Vehicle inventory and displays each owned vehicle's catalogue image in the same three-column row layout. The existing default Vehicle artwork remains when the user owns none.
- No Store purchase/equip logic, avatar dimensions, profile layout colors, or room/seat sizing were changed. Native APK build and on-device visual verification are still pending.


## 2026-11-13 — Four Grid defaults and equipped vehicle identity fix
- Native `Fourgride.kt`: Entry Effect and Gift Effect now default ON per room, retain the room owner's/admin's ON/OFF choice across reopening, and are only adjustable by room owner/admin.
- Native `RoomPage.kt`: passes the active room ID into Four Grid so effect preferences do not leak between rooms.
- `backend/server.js`: room-entry vehicle lookup now checks both the app account ID and Firebase UID used by Store Bag inventory. This fixes the identity mismatch that could prevent an equipped vehicle video from being included in the `room_vehicle_entry` event.
- Vehicle playback remains tied to the existing room-entry event and `StoreVideoPreview`; Store/Bag remains the only place to select, use, or remove a vehicle. No room layout redesign or separate vehicle controls were added.
- Verification: source changes committed; Native APK CI, Render deployment, and real-device multi-account vehicle playback tests are still pending.


## 2026-11-13 — Vehicle video now belongs to the Enter the Room banner
- Root cause: the room page played the vehicle in a separate center overlay instead of the `NativeEntryToast`; the toast model already had vehicle fields but the event handler never populated or rendered them. This detached the video from the Enter the Room card and made it appear absent/misaligned.
- `RoomPage.kt`: correlate `room_vehicle_entry` by account ID, buffer the video if it arrives before the toast, update the matching toast, and render `StoreVideoPreview` directly inside that same entry banner. The banner remains until playback completes; ordinary entries without a vehicle still dismiss automatically.
- Store/Bag remains the only place to equip or remove vehicles. No separate room vehicle controls were added.
- Verification: source committed; APK compilation, Render deployment of the earlier backend identity fallback, and real-device playback remain unverified.


## 2026-11-13 — Room Vehicle playback matches Store preview exactly
- Native `RoomPage.kt`: the in-room equipped Vehicle player now mirrors the Store full-screen Vehicle preview: full available width, the same per-item top/bottom fade values, the same bottom-versus-center alignment, and green-screen removal only for Vehicle `v1`.
- Playback starts automatically and is explicitly non-looping. On the first playback completion, the room preview URL and item ID are cleared so the video disappears; no replay or loop is started.
- The Enter the Room toast remains separate and contains no video. Store/Bag remains the only place to select, equip, and remove Vehicles.
- Source committed; Native APK build and real-device visual/playback verification remain pending.


## 2026-11-13 — Native APK compile root-cause fix
- `RoomPage.kt`: closed the Enter-the-Room toast `RoomPageBlock` before starting the separate Vehicle preview block. The missing closing brace had accidentally nested subsequent room overlays and helper composables inside the toast block/RoomPage scope, causing a cascade of unresolved composables, invalid local declarations, and misleading NativeSeat argument errors.
- No UI behavior or layout changes; this is a Kotlin scope/brace correction only.
- Verification: source committed; the Native 2 GitHub Actions build must complete to confirm APK compilation.


## Room Vehicle Playback Fallback (2026-11-13)
- Fixed equipped Vehicle playback when opening Room with an already-connected/reused room socket. That path refreshes presence/seats without emitting a fresh `room_join`, so the server's `room_vehicle_entry` broadcast is not guaranteed to run.
- RoomPage now resolves the signed-in user's equipped, unexpired Vehicle directly from `/api/store/inventory/{userId}` after a fresh join and when reusing an already-connected socket, checking UID/account identity variants and using the same public video asset base as Store.
- The fallback only fills an empty Room preview, so it does not replace a vehicle event already received from another entrant. Playback remains on the Room screen, outside the entry card, with the existing Store-matched layout and one-shot behavior.


## 2026-11-13 — Vehicle one-shot playback and persistent local video cache
- Native `StorePage.kt`: Vehicle preview playback is non-looping, so a completed Store vehicle clip does not restart every 5–10 seconds. The Store preview downloads the MP4 once into the app's persistent `filesDir` cache (SHA-256 URL key) and reuses the same file for later previews.
- Native `RoomPage.kt`: room-entry vehicle playback uses that same persistent cached MP4 and remains one-shot; the existing `room_vehicle_entry` Socket.IO event and server-authoritative equipped/unexpired vehicle lookup remain unchanged.
- Native Android does not provide browser IndexedDB; the equivalent device-persistent cache is the app-private files directory. Each device downloads the shared public MP4 once and then reuses its local copy; the video bytes are not transferred between users' devices.
- Store inventory remains server-authoritative. The existing inventory GET deletes expired rows and returns only active, unexpired purchases; the Bag expiry timer removes expired cards locally so Store changes from Owned back to Buy after expiry/refresh.
- Verification: source commits `182d0dab`, `f618237a`, and `f7c3615f`; Native Build 2 and real-device first-download/cached-playback timing still need verification.


## 2026-11-13 — Lucky Bag notification artwork overlay
- `android-native/app/src/main/java/com/hurry/voicechat/LuckyBagNotificationSlider.kt`: removed the dark translucent row scrim so sender, amount, room name, countdown, and GO content overlay directly on the notification artwork.
- Switched the banner artwork to `ContentScale.Fit` so the source image is not cropped; added only a subtle warm edge blend. Removed the countdown circle border.
- Restyled GO as a smaller brown/gold raised button and shifted it slightly right.
- Verification: source committed on a focused branch. Native APK build and device appearance remain unverified. The room-side Lucky Bag reveal/claim/Confirm flow and authoritative wallet credit timing were not changed in this commit and still require a separate end-to-end fix/test.


## 2026-11-13 — Lucky Bag Confirm-gated wallet credit and room placement
- The received Lucky Bag artwork now occupies its own right-aligned row immediately before the room side-banner column.
- Tapping Open reserves a per-user reward and opens the reward card; the claim endpoint does not change wallet coins.
- Coins are credited only by the explicit Confirm action through `POST /api/lucky-bag/confirm`.
- Server credit uses an idempotency key scoped to bag + user, so retrying Confirm cannot credit the same reward twice. Wallet balance updates are broadcast after confirmation.
- The confirmation card cannot be dismissed by tapping the backdrop; failed confirmation keeps the card visible and shows an error for retry.
- Verification still required: backend deployment, native APK build, and multi-user/retry testing against MongoDB.

## 2026-11-13 — Vehicle preview cache and playback lifecycle root-cause fix
- Every Store Vehicle preview path now uses the same persistent app-private video cache, including the Store grid, item detail, and room-entry preview. A device downloads each shared MP4 once; subsequent previews read the local file.
- Cache downloads write to unique temporary files and publish only after a non-empty download completes. This prevents simultaneous grid/detail/room previews from corrupting a shared temporary file.
- Vehicle previews are one-shot rather than looping; non-vehicle frame previews retain their existing looping behavior.
- MediaPlayer and its output Surface now share one idempotent cleanup path. Surface destruction and Compose view release can no longer release the same player twice or leave the output Surface unreleased.
- The room vehicle preview continues to use the same cached file and existing Socket.IO room-entry event; video bytes are not sent through Socket.IO.
- Verification status: source changes committed. A successful Native Build 2 APK run and on-device playback test are still required before calling this fully verified.


## 2026-11-13 — Public Profile owned Vehicle and Frame details
- Public Profile Vehicle and Frame sections now use the user's owned, unexpired Store inventory directly, showing catalogue images without wrapping cards or card backgrounds.
- Both sections use three images per row, with remaining items continuing on the next row. Store star artwork appears below each image and the same day/hour expiry countdown style used in the Store Bag appears below the stars.
- Vehicle showcase now includes the Store star row; both showcases update countdowns every second and remove items at expiry. Store purchase/equip behavior and unrelated profile UI remain unchanged.
- Verification: GitHub source commits only; Native Build 2 and device visual verification remain pending.


## 2026-11-13 — Restore original empty Vehicle/Frame cards
- Public Profile owned Vehicle and Frame showcases now render the original white/10 translucent EmptySectionCard with their original artwork/text whenever inventory is empty or unavailable.
- Removed the newly introduced full-width default Store artwork fallbacks. Owned inventory image grid, stars, countdowns, and three-column layout remain unchanged.
- Verification: source commit only; Native Build 2 and device verification pending.


## Own public profile navigation and fans count (2026-11-13)
- MeScreen.kt: the signed-in user's already-loaded Fans count is passed into the own Public Profile model so the profile can display the same count as the Me page.
- The standalone top-right chevron shortcut was removed. Opening the signed-in user's Public Profile from Me is now done by tapping the avatar; other menu and stats navigation remain unchanged.

## 2026-11-13 — Persistent Store inventory and avatar asset cache
- `StorePage.kt`: cache unexpired owned inventory per account in app-private SharedPreferences so Store Bag ownership, equipped state, and expiry hydrate immediately before the server refresh.
- Successful Buy and Equip/Remove operations update the local inventory cache immediately; cache refresh merges still-valid entries so an older in-flight inventory response cannot hide a just-purchased item.
- `NativeOwnedFrameShowcase`, `NativeOwnedVehicleShowcase`, and `NativeAvatarFrameOverlay` now render cached owned/equipped items first and refresh from the server without clearing visible content while loading. Expired items remain filtered by their server expiry timestamp.
- No Store/profile/room/seat UI layout, colors, dimensions, or purchase/equip rules changed. Native build and device verification remain pending.


## 2026-11-14 — Owner Seller Center user activation
- Added a separate `SellerCenterUsers.tsx` screen under Owner Panel → User Center → Seller Center. It searches users through the existing `/api/users?q=` MongoDB search path and shows the matched user's avatar, name, and real account ID in a table.
- Added an owner action endpoint that updates the existing profile `coinSellerTag` (and its legacy aliases) on the matched MongoDB user record. Public profiles already consume this profile flag to display the Coin Seller badge.
- Removed the Seller Center shortcut from the Me page menu; unrelated Me page items and layouts remain unchanged.

## 2026-11-14 Owner User Reports
- Rebuilt `components/Reports.tsx` as the Owner Reports table with search, All/Open/Ignored/Handled filtering, and 20-row Previous/Next pagination.
- Each row shows S.No, Reporter ID, Target user ID, report reason, status, creation time, and a blue Open action. Open displays a centered Report card with Reporter ID, Target, Status, and Description.
- Reports and status changes are stored in the owner's browser IndexedDB (`ZyoraReportsDB` / `userReports`); new reports arrive through the existing live `user_report` Socket.IO event. No report-history backend or MongoDB persistence was added.

## 2026-11-14 Feedback, AI Chat, and User Reports
- Me > Help & Feedback now saves each submitted feedback payload in the app's existing native `index.db` and relays it through the existing `user_feedback` Socket.IO event. The Owner Panel receives the live event and stores its own IndexedDB copy.
- Daisy AI support chat already persists its message history in native `index.db` and relays updates through `ai_support_message`; Owner Panel caches received chats in its browser IndexedDB.
- User reports now save locally in native `index.db` and relay through `user_report`; the Owner Reports page stores received reports in its browser IndexedDB.
- Manage Users no longer inserts a synthetic current-session user when that account is not present in the real users API response. Real API users remain unchanged.
- No UI changes.

## 2026-11-14 — Feedback, AI Chat, and Reports storage rule
- Feedback, AI support chat, and user reports are stored in the submitting user's native local `index.db`; do not persist these records in MongoDB or Redis.
- Socket.IO is used only to deliver new submissions to the Owner Panel in real time. The Owner Panel may keep its own browser IndexedDB cache for entries it receives.
- These flows are live-only: if the Owner Panel is disconnected when an event is sent, the server does not retain a backlog for later replay.
- Keep the existing UI unchanged.
## 2026-11-14 Daisy AI Support Live Delivery Fix
- Native Daisy support chat now queues the latest `ai_support_message` snapshot until its Socket.IO connection is established, preventing messages sent during initial connection from being silently dropped.
- Chat history remains in the user's native `index.db` and Owner Panel browser IndexedDB; no MongoDB persistence was added.


## 2026-11-14 — Seller Center Me menu placement
- Moved the activated Seller Center entry into the main Me menu directly below Bag.
- Restored the original Seller Center menu artwork asset `IMG_20260915_225536.png`; removed the temporary custom-drawn icon and the misplaced bottom-list entry.
- Seller Center remains visible only when the signed-in user's seller activation profile flag is enabled.


## 2026-11-14 — Help & Feedback submission reliability
- Native Help & Feedback keeps each submission in the user's local `index.db` and sends the same record over the existing Socket.IO `user_feedback` event. Contact information is genuinely optional, matching its UI label; leaving it blank no longer blocks submission.
- The server relays the feedback live to the Owner Panel, includes the optional evidence URI in the payload, and acknowledges successful relay so the native sender can close its one-shot socket cleanly. No feedback body is written to MongoDB, Redis, or server-side history.
- Owner Panel receives the event and persists it in its browser IndexedDB cache; its existing feedback table and UI are unchanged.

## 2026-11-14 — Daisy AI chat owner live-sync reliability
- Daisy conversations continue to be saved locally in the native `index.db` `chat_cache` store under the per-user support-chat key.
- The backend now keeps only a bounded, in-memory handoff of recent AI chat snapshots so the Owner Panel can recover messages after a Socket.IO reconnect; no AI chat content is written to MongoDB, Redis, or GitHub storage.
- Owner Panel requests the handoff on initial connection and every subsequent Socket.IO reconnect, merges received chats into its existing browser IndexedDB cache, and updates the existing AI Chat section live.

## 2026-11-14 — Zyora System targeted private messages
- Owner Panel → Official Notifications now searches the canonical /api/users?q= endpoint by real Zyora account ID and requires selecting one matching account before sending.
- Zyora Team / Zyora System messages use a dedicated Socket.IO send_official_private_message event and are delivered only to the selected user's registered Socket.IO identity rooms as the normal private_message event.
- Official private messages are not broadcast and are not written to MongoDB or Redis; the native private-message inbox receives and stores them in the user's local index.db using its existing private-chat sync path.
- Existing Official Notifications sender selector, message composer, image attachment, and surrounding Owner Panel layout are retained. Offline recipients are reported as not sent rather than falsely showing success.


## 2026-11-14 — Seller Center successful recharge notification
- After a Seller Center coin transfer has successfully debited the seller and credited the recipient, the backend sends the recipient a private `Zyora System` message through the existing `private_message` delivery event.
- Notification text includes `🎉 Successful Offline Recharge`, the exact transferred coin amount, and the seller's name. Online recipients receive it immediately; offline recipients use the existing Redis pending private-message queue.
- The native recipient inbox stores the message through its existing local `index.db` private-message path. No MongoDB private-message history write, UI redesign, or change to the wallet transfer/debit/credit rules was added. Duplicate completed transfer requests do not send a second notification.

## 2026-11-14 — Gate Me Seller Center menu by owner activation
- Native `MeScreen.kt`: the Seller Center menu entry is appended immediately after Bag only when the freshly loaded account profile has the owner-controlled seller activation flag (`coinSellerTag`, with legacy aliases retained).
- When the Owner Panel turns Seller Center off, the next Me profile refresh hides the entry; when turned on, it appears below Bag. No unrelated Me layout or icon asset is changed.
- Status: implemented in source; native build and device behavior still require verification.


## 2026-11-14 — Owner Master Wallet coin credit
- Enabled the previously disabled Owner Panel Master Wallet Transfer Coins action.
- Added an idempotent backend credit flow that updates only the recipient's canonical `users.coins` balance and records transfer status in `masterWalletTransfers`; it does not debit a seller wallet or alter Seller Center transfer behavior.
- On successful credit, sends the recipient a private Zyora System message with the credited amount and “From Zyora official Team”. Online delivery uses Socket.IO; offline delivery uses the existing pending private-message queue when Redis is available.
- Owner Panel transfer history remains in its existing browser IndexedDB store. Official Notifications now times out visibly if the Socket.IO acknowledgement never arrives rather than remaining stuck in Sending.
- Verification: source changes committed; backend deployment and live account-to-account testing remain required.


## 2026-11-14 — Owner Manage Users canonical list and cache
- Owner Manage Users membership is sourced only from the MongoDB-backed `/api/users` list; the signed-in Owner Panel session is never inserted as a synthetic user.
- Search now filters the already-loaded canonical list instead of merging search responses into state, preventing deleted MongoDB accounts from being resurrected by stale search results.
- Added a short-lived 15-second in-memory snapshot to avoid repeated list requests in the same tab. The existing `owner_users_changed` Socket.IO event invalidates the snapshot and reloads the canonical list.
- No Manage Users UI changes. Verification: source committed; production API/event behavior and browser testing remain pending.


## 2026-11-14 — Report submission local DB and live delivery correction
- All four native Report entry points (Room Info Sheet, Room Profile, Public Profile, and Message Page chat) use the shared `UserReport` flow.
- Added a dedicated `user_reports` table to the submitting device's native `index.db` with a version-7 migration; report submissions no longer masquerade as chat-cache records.
- The native sender keeps its local copy and emits `user_report` over Socket.IO. The backend only relays the event live and acknowledges delivery; it does not persist report content in MongoDB, Redis, or server history.
- Owner Panel Reports continues to receive `user_report` live and cache the received report/status in its own browser IndexedDB. No Report UI redesign.


## 2026-11-14 — Master Wallet Seller transfer engine reuse
- Owner Panel Master Wallet now calls the established `/api/seller/transfer` endpoint with `masterWallet: true`, avoiding a separate un-deployed route that could return an HTML 404 page.
- The existing Seller transfer engine is reused for user lookup, legacy coin normalization, idempotency, transfer records, canonical MongoDB `users.coins` credit, and the same Socket.IO online / Redis offline private-message delivery.
- Master Wallet mode does not debit a seller or owner user balance; it credits the selected recipient and sends the exact Zyora System notice: `You receive / Coins: [amount] / From Zyora official Team`.
- The Owner Panel now detects HTML/non-JSON server responses and shows a readable deployment error instead of throwing an opaque JSON parse exception.


## 2026-11-14 — Seller Center + Master Wallet System notification live-server root cause
- Root cause: the deployed Render service uses the separate `khaiwilsan676-alt/Zyora-Server` repository, while the previously documented transfer notification implementation existed only in the app repository's backend copy. The live server's `/api/seller/transfer` route did not support `masterWallet: true` and did not send either transfer notification.
- Fixed the actual Render backend route in `Zyora-Server/server.js`: Seller Center keeps its seller debit/recipient credit behavior and sends `🎉 Successful Offline Recharge / Coins: [amount] / From Seller: [seller name]`; Master Wallet credits the selected recipient without debiting a seller and sends `You receive / Coins: [amount] / From Zyora official Team`.
- Both notices use the existing `private_message` event consumed by the native Message page and are saved by the recipient in local `index.db`; online delivery targets registered UID/account-ID rooms, offline delivery queues under all known recipient aliases in Redis for seven days, and duplicate completed transfer retries do not send a second notice.
- Wallet transfer amounts, debit/credit rules, existing chat UI, and unrelated screens were not changed. Source commits were pushed to the live server repository; Render deployment and real-account online/offline tests are still required.


## 2026-11-14 — Zyora Team/System private message delivery and chat presentation
- Added the missing `send_official_private_message` Socket.IO handler to Zyora-Server. Targeted official messages now deliver through the normal `private_message` inbox event, also notify the dedicated official conversation, and queue for offline users in Redis when available; the send callback reports success/failure.
- Native Message page now keeps official message timestamps in local chat history, shows the Zyora Team/System avatar above each official message, and displays the message time below it.
- Official Team/System inbox rows now load and refresh their last-message preview and time, and official avatar URLs use the Zyora public assets. No visual redesign or MongoDB chat-history persistence added.


## Official broadcast active-user delivery (2026-11-14)
- Owner Panel broadcast now targets only registered MongoDB users whose `lastOnlineAt` or `lastActiveAt` timestamp is within the previous 7 days; users inactive for more than 7 days are excluded.
- Socket.IO registration records `lastOnlineAt` and `lastActiveAt` on the matching registered user record.
- Active users currently connected receive the broadcast immediately through the existing private-message inbox event; active users currently offline are queued in Redis for delivery on their next registration. UID/account-ID aliases are deduplicated.
- Owner Panel confirmation now describes the 7-day audience and reports live versus queued counts. Server acknowledgement returns explicit delivery/queue/skipped counts and catches handler failures rather than leaving the UI to time out.
- Legacy users without a recorded activity timestamp become eligible after their next Socket.IO registration. Render deployment and multi-account delivery verification are still required; source commits alone do not prove runtime delivery.


## Official broadcast delivery follow-up (2026-11-14)
- Zyora Team and Zyora System broadcasts use the same private-message inbox event so native clients persist them in local IndexDB.
- Broadcast recipients include MongoDB users active within the last seven days and all currently connected socket identities, even when an activity timestamp is missing or stale.
- Live delivery targets matched socket instances directly rather than relying on Socket.IO room membership; offline eligible users remain queued in Redis for up to seven days.
- Runtime delivery still requires Render deployment and multi-account verification.

## 2026-11-14 — Production user report delivery root-cause fix
- Root cause: the Android app emitted `user_report` to the Render production host `zyora-server.onrender.com`, but the deployed source repository `Zyora-Server/server.js` had no `user_report` Socket.IO listener. The similarly named relay existed only in the app repository's unused backend copy, so the production server silently ignored submissions.
- Added the missing handler to the actual Render backend repository. It validates and bounds the report payload, broadcasts `user_report` to the Owner Panel through Socket.IO, and acknowledges success/failure to the Android sender.
- Report content remains saved in the submitting device's native `index.db` and the Owner Panel's browser IndexedDB. The production relay does not write report content to MongoDB, Redis, or server history. Existing report UI and entry points are unchanged.
- Production deployment and an end-to-end submit-to-Owner-Panel test are still required; source commit alone does not prove the Render service has redeployed.


## 2026-11-14 — GitHub Pages Owner Panel AI chat and report recovery
- Production `Zyora-Server/server.js` now keeps a bounded in-memory handoff of the latest AI support snapshot per user (maximum 200 users) and recent reports (maximum 500). The Owner Panel requests both snapshots after connect/reconnect and merges them into its existing browser IndexedDB stores.
- AI chat and report content remain durable only in the submitting user's local `index.db` and the Owner Panel browser IndexedDB; the backend handoff is volatile and clears on server restart. No MongoDB or Redis chat/report persistence was added.
- Owner Panel GitHub Pages deployment is triggered by the `app/**` change. Render deployment and a real-device AI chat/report test are still required before marking runtime delivery verified.

- Native Daisy support now republishes the restored local conversation snapshot when the support screen opens and Socket.IO connects, so the bounded server handoff can be repopulated without requiring the user to send a new message. Existing users need to open Daisy once on an updated app for their local history to be handed off.

- Owner Panel AI Chat handlers now merge snapshots by user ID, preserve existing moderation/status fields, reject malformed live snapshots, and ignore older out-of-order updates so a delayed history response cannot overwrite a newer conversation. Server acknowledges accepted AI chat snapshots; the existing local index.db + Owner browser IndexedDB storage model is unchanged.
