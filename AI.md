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
- Lucky Bag transaction history (send, claim, refund) is stored only in the Owner Panel browser IndexedDB (`ZyoraOwnerLuckyBagDB` / `transactions`). The backend must not persist Lucky Bag documents or history in MongoDB; live open-bag state exists only in server RAM and expires/refunds in memory. User coin balances still update the existing MongoDB `users.coins` field.
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
- Added an idempotent backend credit flow that increments the recipient's existing `users.coins` field directly. The idempotency key is stored on that same User document; no separate Master Wallet transaction collection/document is created. Seller Center transfer behavior is unchanged.
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
- The existing Seller transfer endpoint is reused for user lookup, legacy coin normalization, canonical MongoDB `users.coins` credit, and the same Socket.IO online / Redis offline private-message delivery. Master Wallet credits increment the existing User document directly; an idempotency key is stored on that same document. Master Wallet history is Owner Panel browser IndexedDB only, with no separate MongoDB transaction collection. Seller Center retains its existing `sellerCoinTransfers` history.
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

- Owner Panel feedback sync now merges live and reconnect snapshots by feedback ID, keeps local status values when an older snapshot arrives, and saves merged records to the existing browser IndexedDB store. User report submissions now receive a Socket.IO acknowledgement after the server accepts and broadcasts the report, matching the native submitter's acknowledgement callback. Reports continue to be stored in the existing Owner IndexedDB store and are replayed from the bounded in-memory server handoff after reconnect.


## 2026-11-14 — Owner Private Chat and report reconnect recovery
- Preserved the existing Owner Panel → Private Chat section and UI. The Owner Panel now requests an authenticated, bounded Socket.IO handoff snapshot after connect/reconnect in addition to its existing private-message history API request, and merges both sources by message ID.
- `backend/server.js`: added a bounded in-memory handoff for private-message monitor events (up to 5,000) and a history response gated by the existing `OWNER_PANEL_ACCESS_KEY` environment secret. Canonical private-message history remains in MongoDB `privateMessages`.
- User reports are now kept in a bounded in-memory reconnect handoff (up to 1,000) and replayed to the Owner Panel on `user_report_history_request`. The Owner Panel continues saving received reports to its own IndexedDB; submitting devices continue saving reports in native `index.db`.
- No Owner Panel UI redesign and no report/private-chat content is written to GitHub storage. The in-memory handoff is lost on backend restart; report durability remains the Owner browser IndexedDB and submitting device database.
- Status: **Implemented in repository**. Render deployment, web build, Owner Panel login/reconnect, report submission from a second account, and private-chat history on the GitHub Pages URL still require runtime verification.


## 2026-11-14 — Private Chat storage moved out of MongoDB
- Owner Panel → Private Chat now loads and saves its history in the Owner browser's IndexedDB database `ZyoraPrivateMessagesDB`, object store `privateMessages`.
- Removed the Owner Panel's `/api/privateMessages` MongoDB history request and removed backend MongoDB writes/reads for private message history.
- Native clients remain responsible for their own private-message history in `index.db`. The backend relays live messages and keeps only a bounded in-memory handoff for Owner Panel reconnects; it does not persist private chat content to MongoDB, Redis, or GitHub storage.
- Important: browser IndexedDB belongs to the Owner Panel browser/device. A different browser/device does not share that local database. Messages sent while the Owner Panel is offline cannot be recovered after the backend's in-memory handoff is lost; no server-side durable copy is retained.
- Status: **Committed in repository; deployment and live two-account verification pending.**


## 2026-11-14 — Lucky Bag and Master Wallet Owner Panel IndexedDB history
- Master Wallet transfer history remains saved and loaded only from Owner Panel browser IndexedDB database `ZyoraMasterWalletDB` / `transferHistory`.
- Lucky Bag send, claim, and refund events are relayed live to the Owner Panel. The panel saves these events in browser IndexedDB database `ZyoraOwnerLuckyBagDB` / `transactions` and displays them in the Master Wallet page.
- Backend retains only a bounded 2,000-event in-memory reconnect handoff for Lucky Bag transactions. It clears on server restart; no MongoDB or Redis transaction-history persistence was added. Each Android user's own Lucky Bag history remains in that device's native `index.db`.
- No unrelated UI redesign. Deployment and live runtime verification remain pending.


## 2026-11-14 — Owner Panel Private Chat live monitor fix
- Production `Zyora-Server/server.js` now forwards normal user-to-user private messages and delivered official private messages to authenticated Owner Panel sockets using the `private_message_monitor` event.
- Owner Panel authenticates its monitor socket with the existing owner secret, joins the private-chat monitor room, and receives a bounded in-memory reconnect snapshot (up to 5,000 messages).
- Owner Panel continues saving received messages in browser IndexedDB (`ZyoraPrivateMessagesDB` / `privateMessages`); each native user's durable chat history remains in their own `index.db`.
- Private chat content is not persisted in MongoDB. Monitor handoff is volatile and clears when the backend restarts; messages sent while the Owner Panel is offline may be missing after the handoff window expires.
- No Private Chat UI changes.


## 2026-11-14 — Owner Panel private chat voice-note display
- Private Chat conversation previews identify voice notes, and the message view renders received voice audio controls from the live monitor payload.
- Existing Owner browser IndexedDB storage and native `index.db` ownership remain unchanged; no MongoDB chat-history storage was added.




## 2026-11-14 — Official chat WhatsApp-style alignment and production broadcast repair
- Native MessageScreen: Zyora Team/System incoming message bubbles now sit on the same horizontal row as the official avatar; removed the timestamp rendered underneath official message bubbles. No other chat layout was changed.
- Production `Zyora-Server/server.js`: replaced the live-only `io.emit` broadcast stub (which did not acknowledge the Owner Panel send and could not reach offline users) with registered-user delivery for accounts active in the last seven days plus all currently connected identities.
- Broadcast delivery uses the normal `private_message` inbox event and the official conversation event. Online users receive it live; eligible offline users are queued in Redis for seven days. Owner Panel receives explicit delivered/live/queued/skipped counts through its existing acknowledgement callback.
- Native local IndexDB remains the recipient-side message history; unread counts and existing message notifications continue through the existing private-message inbox path. No MongoDB message-history persistence or unrelated UI changes.
- Status: source commits completed in the app and production server repositories. Render deployment, native APK build, and multi-account online/offline verification remain required; do not claim runtime success until those checks pass.


## 2026-11-15 — Zyora Team/System broadcast delivery correction
- The Owner Panel connects to `https://zyora-server.onrender.com`; the production broadcast handler is in the separate `Zyora-Server/server.js` repository.
- Root cause in the earlier broadcast implementation: recipient discovery relied on stale `lastOnlineAt/lastActiveAt` fields and custom identity properties on `io.fetchSockets()` RemoteSocket objects. That could omit registered users or fail to match connected identities.
- Production `send_official_message` now enumerates all registered `users` profiles, resolves each Firebase/user ID and permanent Zyora account ID, and checks the corresponding `user:<id>` Socket.IO identity rooms using `io.in(rooms).fetchSockets()`.
- Online recipients receive the existing private inbox and official-chat socket events. Offline recipients are queued in Redis under their canonical account ID for seven days and replayed by the existing registration path.
- Owner Panel confirmation now describes all registered Zyora users, not only users active in the previous seven days.
- Commits: production server `670fefe`; Owner Panel `f5635a8`. Render deployment and real online/offline multi-account verification remain pending.

## 2026-11-15 — Visitor count live sync and Master Wallet history storage
- Native MeScreen no longer fetches visitor history from the server. It reads and writes received visitor rows only in the signed-in device's native `index.db`.
- MeScreen now registers its Socket.IO user room and listens for `profile_visitor_updated`, persisting each visit locally and refreshing the Visitors count while Me is open.
- Owner Panel Master Wallet transfer history remains browser IndexedDB-only. The backend no longer writes `masterWalletTransactions` into MongoDB user documents and removes that legacy history field during database initialization; `masterWalletCreditIds` remains only for duplicate-credit protection.
- No UI/layout changes.


## 2026-11-15 — Invite Friends ID binding and 250,000-coin reward
- Native Invite Friends replaces the decorative friend-invite count row with a User ID input and Bind action. A target account can be bound only once; duplicate binding returns the requested “User has already bind” error.
- Each successful bind increments the inviter's invitation count and 250,000 reward balance. Claim atomically moves one 250,000 reward into the existing user's `coins` wallet balance, with the pending reward decremented in the same user-document update to prevent repeat claims.
- Invite bind records and counters are embedded in the existing `users` document; no new MongoDB collection is added. Owner Panel → Events → Bind User ID receives the inviter ID over Socket.IO and persists the list in browser IndexedDB, with a server sync request to recover records after the panel reconnects.
- Updated native `HurryApi.kt`, `InviteFriends.kt`, Owner Panel `app/owner/page.tsx`, and Zyora Server `server.js`. Build and live multi-account verification are still required.

## 2026-11-15 — Visitor profile matching, live history, and total view count
- Fixed native MeScreen visitor-event matching to accept the signed-in account ID and Firebase UID aliases, so live profile visits are not discarded when the server emits the canonical profile account ID or UID.
- My Visitors now updates an existing visitor row on repeat visits, increments that person's viewCount, and refreshes lastViewedAt instead of creating duplicate rows for every view. FollowPage applies the same local IndexDB aggregation for live events.
- The Me page Visitors total is now the sum of viewCount across both received (My Visitors) and sent (Who I have visited) histories, not the number of rows. Visitor histories remain in native index.db only; no MongoDB visitor-history collection or UI redesign was added.
- Source commits: MeScreen `fd06dc0`; Followpage `b4d0f2c`. Native build and real two-account live-view verification are still required.
- Follow-up root-cause correction: MeScreen and VisitorsPage now register their Socket.IO connection with the Firebase UID as `userId` and the permanent Zyora account ID as `accountId`. Previously both fields were populated with the account ID, so the socket could miss the Firebase-UID room targeted by the server for profile-visit events. The live Me count is recalculated from both local histories after each received visit.

## 2026-11-15 Daisy manual-reply delivery root-cause fix
- Root cause: Owner Panel sent manual Daisy replies through `owner_support_reply`, and the backend attempted live delivery only through Socket.IO `user:<identity>` rooms. A connected/reconnected support client could have its identity registered while its room subscription was temporarily absent, causing the Owner Panel's local AI-chat snapshot to update while the actual user never received the `private_message`.
- `backend/server.js`: manual support reply delivery now resolves recipients using both the existing identity rooms and a live connected-socket identity match across Firebase UID / Zyora account ID aliases. It de-duplicates matching sockets and emits one `private_message` to each resolved connection. Redis remains the offline queue fallback; no chat history is persisted to MongoDB.
- `app/owner/page.tsx`: both Enter and Send now wait for the backend acknowledgement. The draft is cleared only after `ok: true`; failed delivery remains in the input and is logged instead of silently appearing sent.
- Daisy STOP remains an automation-only switch; it does not block owner replies. Native UI, message layout, and local `index.db` chat persistence are unchanged.
- Status: **Implemented in repository**. Backend deployment, Owner Panel deployment, native APK build, and two-account device delivery still require verification; do not claim runtime delivery is verified until tested.


## 2026-11-15 — My Visitors identity-alias delivery correction
- Root cause follow-up: the production visitor relay previously emitted only to the canonical account ID and Firebase UID/legacy ID. Accounts registered through an account-number or legacy app-long-ID alias could therefore miss the live visit event even though the profile visit POST succeeded.
- `Zyora-Server/server.js`: the profile-visit relay now includes every known profile identity alias (`accountId`, `accountNumber`, `uid`, `id`, `appLongId`, and the requested profile ID`) and emits the same live event to each corresponding `user:<identity>` room. No visitor history is persisted on the server or in MongoDB.
- Native `MeScreen.kt` and `Followpage.kt` now accept events matching any supplied profile alias before writing the received visitor row to that profile owner's local native `index.db`. Existing row aggregation, repeat-view count, view timestamp, and UI are retained.
- Source commits: production server `ab02d83`; native MeScreen `347d62e`; native Followpage `80f0c46`. Render deployment, native APK build, and real two-account live-view verification are still required.


## Room vehicle entry playback guard (2026-11-15)
- `RoomPage.kt`: a room-scoped atomic playback claim allows the equipped vehicle video to start only once per room entry, even if the Socket.IO entry event is duplicated or the retained-socket inventory fallback races with it.
- The existing Store-matched video presentation, fades, green-screen handling, and one-shot completion behavior are unchanged.
- Commit: `0b1d1ac241fa1d9f7fea83c7f9b0f4f4e3e5ac62`.

## 15. Kept-room silent re-entry fix (2026-11-15)
- `RoomPage.kt`: when a kept room's socket reconnects after transport loss, treat it as a silent session restore and do not replay the equipped vehicle preview. When the retained socket is already connected, refresh only room presence and seat snapshots.
- `Zyora-Server/server.js`: honor the client's `suppressEntry` flag for silent kept-room restores, preventing duplicate `Enter the Room` messages and vehicle-entry broadcasts while allowing new chat messages and live room state to continue normally.

- Room music controller: the top Music pill remains visible whenever a room track is active and tapping it restores the controller; the compact controller is independent of the pill, uses a pure black card background, and can be minimized by dragging the card downward. The pill disappears only when the controller's Power action stops playback. Existing playback controls and card dimensions remain unchanged.

## 2026-11-15 — Native room public-message permission fix

- Root cause: `RoomPage.kt` initialized `publicMsgOff` to `true`, so a newly entered room temporarily treated public chat as closed for every non-owner/non-admin until a `room_settings_updated` event arrived. This could leave normal users unable to send even when public messages were enabled.
- Fix: initialize the transient client gate to `false` (public chat open); an explicit room settings update with `publicMsgOff=true` still closes it, and owner/admin bypass behavior remains unchanged.
- Scope: native room chat permission initialization only; no UI/layout changes and no message transport changes.
- Verification: source committed; Android build and live multi-user room send test still need to run.

## 2026-11-15 — Public chat default clarified and persisted setting loaded

- Requirement clarified: public room messages must be OFF by default.
- Corrected the earlier transient-default change: `publicMsgOff` initializes to `true` (OFF).
- Root cause addressed properly: the native room's existing `/api/rooms?roomId=...` initial settings fetch now reads persisted `publicMsgOff` (defaulting to `true` only when the field is absent) and applies it to the UI state. Thus rooms already configured ON remain ON for normal users after entry, while rooms with no saved setting default OFF.
- Existing owner/admin bypass, settings toggle, message transport, and UI remain unchanged.
- Verification: source committed; Android build and live room test remain pending.


## 2026-11-15 — Room public messages ON by default
- Native RoomPage now initializes public messaging as ON (`publicMsgOff=false`) and treats a missing saved setting as ON.
- A persisted owner setting of OFF (`publicMsgOff=true`) still blocks normal users; turning it back ON allows them to send again. Owner/admin permission behavior, message transport, and UI are unchanged.
- Source commit: d228c6ad149e805b5e15a39a891c599f2b92b18f. Android build and live multi-user room test remain pending.


## 2026-11-16 — Daisy AI image and video attachments
- Native Daisy AI composer now has the same image attachment icon pattern as Message chat and opens the system picker for images/videos. Attachments are uploaded to the existing MongoDB media endpoint under a dedicated `daisy-support-media` folder, limited to 5 MB and supported JPEG/PNG/WebP/GIF/MP4/WebM/QuickTime types.
- Daisy chat snapshots carry the media URL and media type through the existing `ai_support_message` Socket.IO event. The Owner Panel AI Chats conversation renders image previews and playable video controls for those messages.
- Existing text messages, Daisy automation, native `index.db` history, Owner Panel IndexedDB history, and existing chat layout remain unchanged. Build, Render deployment, and real-device/Owner Panel verification are still required.


## 2026-11-16 — Room public messaging root-cause fix (ON by default)
- Requirement: room public messages remain ON by default and stay ON unless the room owner explicitly switches them OFF.
- Root cause: Zyora Server treated a missing MongoDB `publicMsgOff` field as OFF by using `!== false` in the room-join cache and `room_message` permission check. Legacy rooms without this field were therefore blocked for ordinary members even though the native client defaulted to ON.
- Fix: server now considers public messaging OFF only when the persisted value is explicitly `true`. Missing/legacy values resolve to ON on room join, settings broadcasts, and every message send; an explicit owner OFF remains enforced. Owner/admin bypass is unchanged.
- Native RoomPage retains `publicMsgOff=false` as its default; only the persisted explicit OFF setting closes the composer gate. Removed a stale commented-out OFF-default line; no UI/layout or message transport changes.
- Commits: server `57952561fe7c968d13560f11aafc52289521b1c8`; native `50b0aa81aa6d3ca944b8eeecb5ccb18fcd349468`.
- Verification: source updates committed; Render deployment and live multi-user room messaging test remain required.

## 2026-11-16 — Me page instant local artwork
- Bundled the Me page menu icons, official-ID artwork, Coins/Diamonds tiles, SVIP banner, Invite badge, and Level badge into Android drawable resources instead of fetching them from GitHub Raw at render time.
- These decorative assets now render directly from the APK on first open and remain available offline; the live profile avatar and profile data retain their existing cache/network behavior.
- No layout, sizing, colors, navigation, or profile behavior changed. Native build and real-device timing verification remain pending.


## 2026-11-16 — Room gender persistence, MongoDB visitor history, and stable social counts
- `RoomInfo.kt`: room member gender resolution now falls back to the last valid cached gender when incoming profile/member data is blank; blank refreshes no longer overwrite saved gender, preventing the gender tag from repeatedly disappearing.
- `Zyora-Server/server.js`: profile visits are now persisted inside each existing `users` document in `visitors` (received / My Visitors) and `visitedProfiles` (sent / Who I have visited). Repeat visits update the same person's `viewCount` and `lastViewedAt`; each list is capped at 20 distinct users. No new collection is created.
- `GET /api/profile-visitors?userId=...&type=received|sent` now returns the corresponding embedded user-document history. `Followpage.kt` loads server history first and retains native `index.db` as the offline fallback; the existing live Socket.IO visitor update remains active.
- `MeScreen.kt`: Fans, Friends, and Following counts initialize from the saved social-count snapshot before the background refresh, avoiding the temporary dash/blank state during navigation.
- Source commits: server `e0bced5`; native FollowPage `7f3ad29`, RoomInfo `2f2d481`, MeScreen `04ecb2e`. Render deployment, native APK build, and two-account visitor persistence verification remain required.

- Follow-up: `MeScreen.kt` now refreshes both received and sent visitor histories from the users-document API before calculating the Visitors stat, and mirrors successful responses into local `index.db`; offline fallback remains intact. Commit: `64fe424`.
- Visitor API identity lookup also matches numeric MongoDB accountId/accountNumber/id values when clients send the same account as a string. Zyora Server follow-up commit: `6100330`.

## 2026-11-16 — Stable Lucky Gift notification and combo timer
- `RoomPage.kt`: replaced five independent delayed Handler callbacks with one room-scoped Compose countdown keyed by a generation token, so a newer Lucky Gift or combo tap cancels the previous timer cleanly.
- The sender's Combo button now stays mounted after tapping and its five-second window restarts while the previous Lucky Gift is replayed. The echoed combo replay also renews the same timer; it no longer gets excluded from the sender's combo-window handling.
- Preserved the existing notification artwork, placement, sizing, styling, and its 5-second transient display; no unrelated room UI/layout changes.
- Native build and multi-device live-room verification remain to be run.

## 2026-11-16 — Lucky Gift notification width
- `GiftPicker.kt`: increased the Lucky Gift notification row maximum width from 300dp to 340dp and sender/recipient text column maximum width from 110dp to 150dp so the notification can display wider content.
- Kept its start padding, alignment, vertical position, height, animation, colors, and all other layout properties unchanged.

## 2026-11-16 — Restore previous room music after app restart
- Persist the signed-in user's last selected room track (ID, title, URI, duration) locally when selected/updated and restore it into the room mini-controller after reopening the app.
- Restoring the track does not auto-start audio and does not restore/rejoin the previous room or seat. The user must enter a room normally; the saved song is available in the controller.
- Pressing the controller's Power/Stop button explicitly clears the saved track. Existing playback, playlist, and controller UI behavior remain unchanged.
- Native build and force-stop/relaunch device verification remain pending.

## 2026-11-16 — Gift Picker Send button width
- `GiftPicker.kt`: set only the blue Send button to an exact width of 70dp; removed its previous horizontal padding so the fixed width is respected.
- The multiplier/arrow section, button height, colors, shape, click behavior, and surrounding layout remain unchanged.
- Native build and device visual verification remain pending.

## 2026-11-16 — Lucky Gift picker banner
- `GiftPicker.kt`: show `public/IMG_20261116_162726.png` only while the Lucky tab is selected, positioned outside and directly above the gift sheet with a 3dp overlap; no card container or extra spacing.

## 2026-11-16 — Image-only native launch screen
- Replaced the native app's Compose launch screen with only `public/file_00000000b46081fd889ad8fece9e9e00.png`, displayed full-screen.
- Removed the previous launch logo and `Zyora` title from the launch UI; the old splash artwork is no longer packaged as the launch drawable.
- Updated the Android window launch background to use the same requested image so it appears immediately when the app icon is tapped.
- No other app UI or navigation behavior was changed.

## 2026-11-16 — Family Profile, invitations, and gift-based rankings
- Added native `familyprofile.kt` and routed the Family card in `PublicProfile.kt` into the dedicated Family Profile screen. The page uses the requested family cover image with a dimmed 10% bottom fade into the grey page, square 12dp family DP, Family ID/member count, white Announcement card, and member ranking list.
- Owner-only pencil opens the full-screen edit form (family DP/name/announcement only; no Required Level or create-cost button). Owner-only Add Members opens a 70%-height Invite Member sheet with server-backed ID/name search and invite action. Non-owners see Apply.
- Family invitations are delivered as `family_invite` private-chat messages, including family DP/name/ID, the exact invitation text, and an Accept action. Accept persists membership in MongoDB and syncs the joining user's local public-profile family fields. Private chat history remains in the existing local IndexedDB flow; Redis is used only for offline delivery.
- Zyora Server now stores families in MongoDB `families`, enforces a 50-member maximum, exposes family load/update/search/invite/accept and global leaderboard endpoints, and updates each member's `coinsSent` plus family `totalCoinsSent` only after a successful `coin_transfer`.
- `Family.kt` now renders the actual top 1–3 family DP/frame/target and only renders ranks 4–50 when corresponding families exist. Member ranking sorts by each member's sent-gift total; member coin amounts are masked as requested.
- Existing profile-based families are migrated into the new collection when their Family Profile is first opened. Source commits: native Family Profile/navigation/chat/ranking updates through `82c61f1`; server family API/ranking updates through `23bc796`.
- GitHub Actions native APK build is queued/running for the latest native commit. APK success, Render deployment, and two-account invitation/acceptance plus gift-ranking verification are still pending; do not mark this feature device-verified until those checks pass.

## 2026-11-16 — Family completion follow-up
- Fixed the Family Profile page's missing Compose context used by Apply and family-DP upload.
- Owner pencil now opens the same `FamilyCreate` screen used for initial creation. Edit mode changes the heading/button to Edit my family / Save Changes and hides Required Level and the 30,000,000-coin creation control.
- The edit form saves the family name, hosted DP, and announcement through the owner-authenticated MongoDB update endpoint; successful changes update local profile preferences and reload the page.
- The backend family creation path already synchronizes `familyCode` and `familyAnnouncement` onto the owner's public-profile document; joined members receive the same fields on acceptance.
- Latest APK workflow run: #323, associated with commit `ad258d3`; build result is pending at the time of this note.


## 2026-11-17 — Family Admin tools, member visibility and Zyora System requests
- Family Admins now see the Add Members action and can search/invite users; owner and admins can review and approve family applications.
- The Apply action is hidden when the viewer is already listed as a member of that family. Apply and Invite actions use matching compact blue button styling.
- Family invite and apply notifications are delivered in the dedicated Zyora System conversation, not a user-to-user personal chat. Both involved accounts receive targeted system updates; approved applicants also receive a Zyora System confirmation. Offline delivery uses the existing Redis pending-message queue.
- Only the family owner and Admins receive member coin contribution values. Other viewers receive member records without the coinsSent field; the profile continues to show masked values.


## 2026-11-17 — Joined members' Family cards
- Public Profile resolves a user's Family from their stored familyCode or familyId, then loads the authoritative family record. Joined members now display the same existing Family card as the creator: family DP, family name, creator's account ID as Family ID, and live member count.
- The Family home bottom card now resolves the signed-in user's current family from their profile and the Family API, instead of relying only on the local creator-only familyCreated flag. Joined members see the same Family bottom card and tapping its DP opens that family's profile.
- Existing card artwork, spacing, and layout are preserved.
- Native APK build and device verification are pending.


## 2026-11-17 — Public Profile Family tag and centered family banner name
- `PublicProfile.kt`: the family banner now uses `public/IMG_20261117_130644.png`; the current family name is overlaid at the exact center of the image, with the Family ID and member count beneath it. Existing family DP/frame placement and card dimensions are retained.
- Added a compact Family tag beside the profile display name for users who belong to a family, beside the family name on the Public Profile Family card, and alongside the existing Lv.1 badge row.
- The tag visibility is driven by the existing resolved family membership state, so joined members and family creators both receive it. No backend or membership permissions were changed.
- Status: **Implemented in repository**. Native APK build and visual positioning on device remain unverified.

## 2026-11-17 — Family tag image and Public Profile family card visibility
- Replaced the plain blue text Family badges with the requested Family tag image asset `public/IMG_20261117_130644.png` in the Lv.1 row and beside the family name in the Public Profile Family card.
- Kept the user-name row free of a Family badge; the Lv.1 row is the profile-level tag placement.
- Fixed the local-profile early-return path so a joined/created family saved in local family preferences can still hydrate and display the Family card when both remote and cached profile records are unavailable.
- Family banner continues to use `public/IMG_20261117_130644.png`, with the family name and details overlaid at the center.
- Build and device rendering still require verification.

## 2026-11-17 — Family System invitations and applications
- Family owner invitation sends the invited user the existing Family invitation card and sends the owner a Zyora System message: “Invite Send to (User name) from (Family name)”.
- Family applications send the applicant a Zyora System message: “Apply Sended to (Family name)”. The owner receives an applicant card with avatar, name, account ID, the requested join text, and an Approve action that uses the existing family-application approval API.
- Family Profile member rows no longer display boolean “True”/“False” values beside account IDs when those values appear in the optional tag field.


## 2026-11-17 — Public Profile Family image loading fix
- `PublicProfile.kt`: load `IMG_20261117_130644.png` for both the Lv.1-row Family tag and the Family banner/tag from the repository's raw GitHub public asset URL, matching the working asset-loading pattern used by the Family screens instead of relying on the Vercel deployment.
- Family tag and Family banner visibility now also recognize the resolved family code and legacy local `familyId`/`familyCreated` values, so an existing member is not hidden solely because the transient `hasOwnFamily` state has not hydrated yet. Other users' profiles do not inherit the viewer's local membership flags.
- The requested Family banner image remains fitted without changing the existing card size or layout. APK build and device image-rendering verification remain pending.


## 2026-11-17 — Joined Family navigation and member counts
- The Family tab now resolves the signed-in user's family from the server profile, with local family preferences as fallback, so both creators and joined members receive the same Family card.
- Tapping the Family DP in the Family tab opens that family's existing Family Profile screen; Back returns to the Family tab.
- Family leaderboard cards and the user's Family card now display the actual member list count out of 50 instead of a hardcoded 0/50.
- No unrelated Family UI layout, artwork, colors, or spacing was changed.

## 2026-11-17 — Room contribution cup sheet
- Added `Roomcup.kt` with a 70vh black bottom sheet, Contribution / Online User tabs, Daily / Weekly selector, GMT+05:30 24-hour clock, and a vertical Top 1–20 contribution list (no podium layout).
- Rank 1–3 use the supplied rank images; ranks 4–20 use white number text. Empty state reuses the Mine empty-state image and `No data` label. Bottom strip shows the current user's avatar, name, Lv.1 badge, coins, and rank.
- Room cup icon opens the sheet; Online User tab hands off to the existing ActiveUsers sheet. The top active-user pill continues to open that same ActiveUsers sheet.
- Zyora-Server records successful room gift amounts in `room_gift_contributions` and exposes `/api/rooms/:roomId/contributions` for rolling 24-hour / 7-day totals.
- Status: source changes committed; Android build and live Render deployment still require verification.

## 2026-11-17 — Global Rich, Charm and Room leaderboards
- Native `Leaderboard.kt` now loads real rankings from Zyora-Server for Daily, Weekly and Monthly periods and refreshes every 15 seconds.
- Rich ranks users by total sent gift coins across all rooms; Charm ranks users by received gift coins across all rooms.
- Room ranks rooms by total sent gift coins and displays the room DP/name; no fabricated users or placeholder rows are rendered.
- Top three user positions use the existing rank frames and real profile avatar/details; ranks 4–50 render only returned accounts.
- Zyora-Server records recipient-side gift totals in addition to sender-side totals and exposes `GET /api/leaderboard`. Legacy sender records remain included in Rich and Room totals.
- Countdown uses each period's next boundary in India Standard Time.
- Note: Charm totals begin accumulating from the server deployment onward because historical contribution records only stored senders, not recipients. No build or live Render deployment verification has been performed in this change.


## 2026-11-18 — Room Active Users moved into Room Cup Online User tab
- The room's top-right active-user count/icon now opens the existing Room Cup sheet directly on its Online User tab; the Cup icon still opens the same sheet on Contribution.
- The Online User tab renders the room's live active-user list, with owner/admin tags, country flag, level badge, avatar, and profile-open behavior.
- Removed the separate normal Active Users sheet entry point and overlay. The separate Invite User sheet remains unchanged.
- No other room UI styling or layout was intentionally changed. Native APK build and device verification remain pending.


## 2026-11-18 — Official IDs excluded from normal registration slots
- Native login now bypasses device-registration locks for provisioned official/support Firebase UIDs; those identities no longer consume Google or Email/Password registration slots or get saved as a normal registered Google account.
- Backend device-registration responses ignore legacy lock records belonging to official IDs, and device-google-lock explicitly exempts official IDs/Uids.
- Normal accounts retain two independent device slots: one permanent Google identity and one permanent Email/Password identity. Each provider can reuse only its own existing UID/ID; the other provider remains a separate second account.
- Login/Register UI styling and layout were not changed.

## 2026-11-18 — Room display name refresh from canonical profile
- Root cause: `RoomPage.kt` selected the first nonblank name from the local `index.db` snapshot. A stale placeholder value (`User`) therefore masked the official account's updated MongoDB profile name. Room join sent that stale value to the room presence snapshot.
- Fix: `RoomPage.kt` now renders the local cached profile immediately, then asynchronously reconciles it with the canonical `/api/users` profile via `ProfilePersistence.load`. A valid remote name/avatar is merged into the local profile cache and Compose state. Placeholder `User` values are ignored when selecting a real profile name.
- The existing room socket identity/effect keys are unchanged: profile refresh does not reconnect the room socket or interrupt seats/voice/chat. No UI/layout/color changes.
- Performance: local cached data remains the instant first render; one background profile refresh updates stale data without blocking room entry. APK build, Render availability, and 2-account live room verification remain pending.

## 2026-11-18 — Homepage room-card scrolling performance
- Cached each room card's normalized image model by room image URL to avoid repeating URL normalization during scroll-driven recompositions.
- Changed long room-name marquee from an infinite animation to one pass, removing continuous per-card animation work while preserving the existing card layout and text treatment.
- Kept the existing two-column room grid, card dimensions, images, frames, header, banner, category cards, country pills, and bottom navigation unchanged.

- Correction (2026-11-18): restored the original infinite room-name marquee animation; continuous marquee is intentional and must not be removed as part of scrolling optimization.

## 2026-11-18 — Room music exit and controller restore fix
- Explicit room Exit now sends STOP to the room music service immediately, so music cannot continue after leaving the room.
- Exit preserves the last selected track in local preferences and shared music state; it does not auto-resume playback. On the next room entry, the top Music pill and mini-controller can restore that track.
- Reopening Music from the four-grid/controller path restores the previous track directly into the mini-controller instead of forcing the music library open. The library remains available when no previous track exists.
- The Music pill continues to be driven by the selected track and tapping it reveals the controller; stopping with the controller Power action still clears the saved track.


## Room seat disconnect and moderator leave correction (2026-11-18)
- Server repo: `khaiwilsan676-alt/Zyora-Server`, `server.js`.
- Root cause: the disconnect handler intentionally preserved occupied microphone seats even after the user's final live room socket disconnected. It now releases the seat only after a successful cross-process check confirms no other live session remains; seat state, mute, speaking, GIF, and socket ownership are cleared and `room_seats` is broadcast. Socket-ID protection prevents an older disconnect from clearing a seat already rebound to a reconnecting socket.
- Root cause: the `leave` handler cleared both the moderator's own occupied seat and the selected target seat because it treated requester ownership as sufficient even when `targetUserId` identified another user. It now clears only the selected target seat for verified room owners/admins; normal self-leave clears only the authenticated user's seat.
- Removing another user's seat no longer calls `stopRoomVoice()` for the moderator, so their own microphone session stays active.
- No UI changes.
- Server commit: `f87b3c88c844beaa2b542384258f62f74245b0de`.
- Verification: fetched the committed server file and confirmed the new disconnect, target-only leave, and moderator voice-preservation logic. Live multi-device/app-force-close testing remains pending.


## 2026-11-18 — Per-user room playback effect controls
- Native Fourgride.kt now reports Entry Effect and Gift Effect toggles to the owning RoomPage; the existing visual controls are unchanged.
- Native RoomPage.kt stores Entry Effect and Gift Effect locally per signed-in account, across rooms on that device. Entry Effect OFF suppresses the room-entry vehicle event and equipped-vehicle fallback, including Store-owned Vehicle MP4 playback in the room. Gift Effect OFF suppresses incoming gift-video playback for that account only.
- The Speaker toggle is persisted per account and gates only incoming room voice packets on that device. It does not mute the user's microphone, suppress room music packets, alter other users' audio, or change server room voice state. Turning it back ON resumes incoming voice playback.
- No room UI/layout, Store preview, gift sending, seat, or server behavior was intentionally changed.
- Verification status: source-level changes only; native APK build and multi-device playback tests remain pending.


## 2026-11-18 — Per-user Four Grid effects and Speaker playback
- Native `RoomPage.kt`: Entry Effect and Gift Effect preferences are strictly local to the signed-in account. Removed the legacy room-ID preference fallback so one room member cannot inherit another member's prior setting. Turning either effect OFF immediately dismisses that user's currently visible corresponding video; vehicle and gift event listeners/fallbacks continue to gate new playback using the same account-local state.
- Native `JitsiRoomAudio.kt`: Speaker OFF filters only incoming `room_voice_chunk` packets for that device. Pending voice frames are discarded and an in-flight voice frame stops at its next packet boundary. Incoming `room_music_chunk` playback remains enabled; mic publishing, room socket, seat state, and other users are unaffected.
- Four Grid layout and controls are unchanged. No server broadcast or shared room preference was added.
- Verification: source-level changes committed; APK build and two-device runtime verification remain pending.


## 2026-11-18 — Centralized seat emoji media sizing
- `Emojipicker.kt`: selected stickers carry picker-defined seat scale. Added one shared `SeatEmojiOverlay` renderer that owns MP4-vs-image selection and sizing for every seat.
- `RoomPage.kt`: all seat reactions render through the shared picker renderer. The selected sticker scale is included in the existing room-seat emoji event and retained in seat state.
- `backend/server.js`: seat emoji state stores a bounded scale and broadcasts it with the existing seat snapshot, so every participant sees the same picker-defined sizing. Other seat gift/image overlays keep the default scale.
- No seat layout, picker layout, voice, or other room controls changed.
- Verification: source updates committed; native APK build and multi-device runtime verification remain pending.


## 2026-11-18 — Room music exit cleanup and explicit controller visibility
- Native `RoomPage.kt`: the saved previous track is restored as metadata only; the music pill and controller card remain hidden on every room visit until the user opens Four-grid and taps Music.
- Tapping Four-grid → Music restores the previous track into the controller without autoplay. The pill and card remain available together after that deliberate action.
- Real room disposal/exit sends `RoomMusicService.ACTION_STOP`, clears active playback state, and preserves the last selected track metadata. Intentionally keeping the room miniaturized does not trigger this room-exit cleanup; swiping Zyora away from Recents is handled by `RoomMusicService.onTaskRemoved`.
- No room layout, colors, card dimensions, or unrelated controls were changed.


## 2026-11-18 — Family frame video playback and Store-matched vehicle fades
- Native `Family.kt`: Family reward video previews and the tapped reward modal now use the shared `StoreVideoPreview` renderer instead of the separate ExoPlayer/TextureView path for these previews. This aligns playback with the Store decoder/surface lifecycle and reuses Store green-screen removal for Family frame videos.
- Family Vehicle video previews and their modal use the Store alpha-mask fade settings: 10% from the top and 15% from the bottom. Vehicle audio remains enabled; looping remains enabled.
- Family card/modal layout and the Store UI were not changed.
- Source commit: `3cfdec7458c501e071b0327783829c30785267b6`. Native APK build and on-device playback verification are pending.

- Follow-up source commit `615a50dc5714873f547a7fde9c2bf1928a0c0174` preserves the existing audio behavior: Family Vehicle previews remain audible and Avatar Frame previews remain muted.



## 2026-11-18 — Room top Cup pill daily total
- Replaced the hardcoded zero in the room's top-left Cup pill with the sum of all users' daily contribution values from the same room contributions endpoint used by the Cup leaderboard.
- The pill refreshes every 10 seconds and uses the existing K/M/B formatter. When the server daily period resets and the daily leaderboard is empty, the displayed total becomes 0.
- No room layout, seat, or other UI styling was changed.

## 2026-11-18 — Official room name still showing User (server root cause)
- Confirmed root cause in backend/server.js: /api/users normalization used user.name || user.displayName ... . A truthy legacy name value of User therefore shadowed the newly saved displayName / nickname values. The native client received the placeholder again, so a client-only cache refresh could not fix it.
- Server fix: /api/users now chooses the first meaningful non-placeholder name across name, displayName, userName, nickname, and supported nested profile fields.
- Room fix: Socket.IO room_join resolves the canonical MongoDB profile by app account ID / UID and uses the saved name/avatar for room presence, online broadcasts, and entry messages instead of trusting stale client payloads.
- Native RoomPage.kt retains instant local rendering, then reconciles the canonical profile in the background. Existing room UI, seats, voice, chat, and layout are untouched.
- Commits: 8859415 (API normalization + Mongo lookup), eeeb66b (room presence/entry name), ffe2c49 (canonical avatar use), 5bc83ef (native background profile refresh).
- Deployment and build verification are still required; test account 500005 after Render deploy and native APK rebuild.
- Performance follow-up: the room_join profile reconciliation uses one MongoDB $or lookup (instead of sequential per-identifier queries) to avoid multiple round trips on room entry. Commit: 8443bfc.

## 2026-11-18 — Room avatar voice and music waves
- Native `RoomPage.kt`: replaced the single white speaking outline with three solid, concentric white circles at 20% opacity. Each starts small, expands beyond the avatar, fades out, and repeats with staggered timing behind the avatar.
- Waves activate for remote microphone speakers from the existing room speaking state, for the local user's detected microphone speech, and for music playback.
- Native `JitsiRoomAudio.kt`: observes existing `room_music_chunk` packets and tracks the publishing account ID while packets arrive; a short inactivity timeout clears that publisher's avatar wave. The local sender remains handled by the existing local music-playing state.
- No seat positions, avatar dimensions, seat layout, music playback, voice routing, or other UI styling was changed.
- Commits: `e09762e` (remote music activity tracking), `dad02d4` (three-circle avatar wave), followed by main-thread callback safety update. APK build and multi-device runtime verification remain pending.


## 2026-11-18 — Room avatar voice/music wave tuning
- Native `RoomPage.kt`: reduced the visual wave scale from 1.6x to 1.45x and the maximum circle radius from 0.46 to 0.42, keeping the avatar and seat dimensions unchanged.
- Increased each wave's white opacity from 20% to 30% and slowed the repeating three-circle cycle from 1,500 ms to 1,900 ms. The three staggered circles and existing voice/music activation sources remain unchanged.
- No other room UI, seat, avatar, voice-routing, or music behavior was changed. Native APK build and device verification remain pending.
## 2026-11-18 — Leaderboard period countdown correction
- Daily, Weekly and Monthly leaderboard totals remain independently filtered by the server using India Standard Time boundaries: Daily resets at midnight, Weekly runs Monday through Sunday, and Monthly ends at the next month's first midnight (after the calendar month's last day).
- Native `Leaderboard.kt` now labels the active reset countdown as Daily, Weekly or Monthly. Daily shows hours/minutes/seconds; Weekly and Monthly also show remaining days so longer periods do not wrap every 24 hours.
- Updated the Rules copy to state the exact IST reset schedule. No ranking UI or aggregation logic changed.

## 2026-11-18 — Daisy support image attachment UI
- HurrySupport.kt: moved the Daisy image attachment icon to a neutral grey tint (no blue icon tint).
- Sent image attachments use a neutral light-grey bubble background and render as a compact 190dp square thumbnail (approximately 5cm).
- Tapping an image opens a full-size preview with a close (X) control in the top-left corner.
- Video attachment rendering and the rest of the Daisy support chat UI remain unchanged.

## 2026-11-18 — Family Rules fullscreen
- Updated the Family Rules dialog to render edge-to-edge by disabling dialog window system fitting while retaining its existing full-size layout, status-bar spacing, colours, typography, and content.
- No other Family UI or layout was changed.

## 2026-11-18 — Room avatar voice/music wave approved animation
- Updated the existing three staggered avatar waves in native `RoomPage.kt` to a 1.5x visual scale and a 1,900 ms repeating cycle.
- Kept the existing 30% white circle opacity, radius, three-circle phase offsets, and voice/music activation logic unchanged. The stagger means each next circle begins before the preceding circle completes its fade.
- Avatar dimensions, seat layout, colors outside the wave, voice routing, and music behavior were not changed.
- Source committed; APK build and on-device animation verification remain pending.

## 2026-11-18 — Room avatar wave opacity
- Increased only the existing voice/music wave circle opacity from white/30 to white/50 in native `RoomPage.kt`.
- Kept the 1.5x scale, 1,900 ms cycle, staggered overlap, circle radius, avatar, seats, and all other UI/behavior unchanged.
- Source committed; APK build and device verification remain pending.


## 2026-11-19 — Room music fidelity and avatar-wave gating
- Native `JitsiRoomAudio.kt`: kept microphone capture, voice packet format, voice output track, voice permissions, mute behavior, and room voice events unchanged. The dedicated `room_music_chunk` path now carries 48 kHz stereo 16-bit PCM (20 ms packets) instead of collapsing music into 16 kHz mono speech-quality PCM. Receivers use a separate 48 kHz stereo media AudioTrack; mono source tracks are duplicated to stereo and stereo sources retain left/right separation.
- Native `RoomPage.kt`: avatar waves now require an occupied, unmuted, non-self-muted seat. Remote microphone waves use the live speaking event state; remote music waves use the music-packet activity timeout; local mic/music indicators remain tied to the local activity states. Empty seats and muted seats cannot animate.
- No microphone/voice routing, capture, voice chunk protocol, seat layout, avatar dimensions, or visual styling was intentionally changed.
- Verification: source changes committed; Android APK build, server relay compatibility, and two-device audio quality/wave behavior must still be verified before calling this device-tested.
 
## 23 Purchased chat bubble in native room chat (2026-11-19)
- Store/Bag Chat Bubble items continue to use the existing purchase and server-backed Use/equip flow.
- Added native equipped-bubble lookup for each room-chat sender using that sender's server inventory, with the existing local inventory cache as the immediate/offline fallback.
- Replaced the normal black message-card fill with the sender's active purchased Chat Bubble image; message text and mention handling remain overlaid on top of the image. Image messages and all other room UI remain unchanged.
- Verification: source committed; native APK build and multi-account runtime checks remain pending.


## 2026-11-19 — Registration false-positive follow-up
- Root cause: Email/Password sign-up queried the device registration slot before authenticating the submitted Firebase identity. This could show the prior account's "Register this ID" restriction before the app knew whether the incoming identity was official.
- Removed that premature device-slot precheck. Firebase authentication now resolves the identity first, then the provider-specific device lock runs.
- Official identity detection now checks the server-resolved Zyora accountId as well as known legacy Firebase UIDs, rather than relying on Firebase UID alone.
- No login/register UI changes.

## Room speaker mute/unmute and music-wave fix
- Native `RoomPage.kt`: music activity keeps the seat wave visible independently of the seat's voice mute flags; microphone voice waves remain gated by mute state.
- Native `JitsiRoomAudio.kt`: speaker mute clears queued incoming voice and flushes the partially buffered voice AudioTrack once on the mute transition, preventing stale pre-mute samples from overlapping playback after unmute.
- Increased incoming voice/music jitter queues from 6 to 20 packets to absorb short network/device scheduling delays. Mic capture, voice publishing, server authorization, music encoding, and UI layout are unchanged.

## 2026-11-19 — Prevent stale seat restoration after app restart
- `backend/server.js`: a genuinely new room socket session now clears any leftover occupied seat for that account before joining and broadcasting the room snapshot.
- Removed the join-time logic that rebound an old occupied seat to the new socket, which caused the room owner/avatar to reappear seated after force-closing and reopening the app.
- The existing Keep/minimize flow is unchanged because it retains the same connected socket; duplicate joins on that same socket also preserve the active seat.
- No UI changes.


## 2026-11-19 — Simultaneous user-to-user room voice mixing
- Native `JitsiRoomAudio.kt`: replaced the shared voice FIFO playback path with independent bounded 20 ms PCM queues keyed by the authenticated sender ID; each playback tick sums samples from concurrent speakers into one mono frame with clipping protection. This prevents concurrent users' frames from being played sequentially as overlapping/chopped audio. Speaker OFF, disconnect, and room cleanup clear the per-speaker queues. Music playback remains on its separate queue.
- Server relay already stamps each `room_voice_chunk` with the authenticated `userId` and excludes the publishing socket; no server fanout change was needed.
- Verification: source committed; native APK build and two-device simultaneous-unmute test still required.

## 2026-11-19 — Message inbox profile avatar refresh
- `MessageScreen.kt`: existing conversation previews no longer overwrite a valid cached peer avatar when an incoming message omits `senderPhoto` or sends the default placeholder.
- The Message inbox asynchronously refreshes cached peer name/photo metadata from the existing `/api/users` endpoint on a background dispatcher, then updates the visible row and IndexDB cache without blocking first render.
- No Message page UI, layout, navigation, chat delivery, unread-count, or room-presence behavior was intentionally changed.
- GitHub commits: `789bc65e` (initial inbox avatar refresh), `56753eed` (corrected preview initialization order).
- Native Gradle build and device verification are still required; this change has not been represented as build-tested.

## 2026-11-19 — Room Cup totals and weekly contribution aggregation
- `Zyora-Server/server.js`: room contribution endpoint no longer truncates its date-window records at 10,000 rows; weekly and daily per-user totals aggregate all matching sent records.
- Endpoint now returns `totalRoomCoins`, calculated from every sent contribution record in the requested room and period, independently of the Top 20 leaderboard.
- `Roomcup.kt`: the room's top cup counter reads `totalRoomCoins` rather than summing only the returned Top 20 leaderboard entries.
- No UI layout, colors, room gifting, wallet debit, or ranking presentation was intentionally changed.
- Commits: server `49ee99f`; native `4957bf9`.
- Native build and live MongoDB data verification remain pending.

### Seat menu Invite excludes seated users
- The seat-menu Invite User sheet now excludes every account currently occupying a room seat, using the live `seats` snapshot as the source of truth.
- The filter is recomputed when seat occupancy changes, so users become inviteable again after leaving a seat. Existing Active Users and Room Profile invitation behavior is unchanged.


## 2026-11-19 — Keep room silent reconnect preserves occupied seat
- `backend/server.js`: the `room_join` path no longer clears the account's occupied seat when the native client marks a reconnect as `suppressEntry` (the retained Keep-session recovery path). The server still sends authoritative room presence and seat snapshots after the reconnect, but suppresses the duplicate `room_user_online` broadcast so other room members do not see a second entry event.
- Normal room entry (`suppressEntry` false) continues clearing stale seats, preventing a genuinely new session from inheriting an old seat.
- No UI, voice relay, music playback, or normal room-entry behavior was intentionally changed.
- Verification: backend syntax/runtime and a real two-device Keep → temporary disconnect → restore test are still required.


## 2026-11-19 — Room voice reliability fixes
- `JitsiRoomAudio.kt`: removed stale references to the retired shared voice queue from playback failure, speaker-mute, and disposal paths; these references could prevent native compilation.
- Voice playback now resolves each remote publisher by the available sender/account identity, requires the protocol's exact 640-byte 20 ms PCM frame, and uses bounded per-speaker queues.
- Concurrent speaker samples are accumulated before one final soft limiter; this avoids order-dependent clipping from saturating after every speaker.
- Voice permission-denied retries are capped at three instead of repeatedly requesting permission every 500 ms when a user is not seated or is muted.
- `backend/server.js`: microphone relay rejects malformed frame sizes and room leave revokes the socket's active voice publisher state and sends a final non-speaking update.
- Music relay, music decoder, music controls, and room UI were not intentionally changed.
- Commits: native playback/mix `c3552e5`; server frame validation `87bf52a`; bounded permission retries `18b3c9f`; server room-leave cleanup `e121ce1`.
- Native Gradle build and multi-device voice testing remain required; these changes are code-reviewed but not yet device-verified.

- `backend/server.js`: seat leave, self-mute, owner/admin mute, and seat lock now immediately revoke an active mic publisher and broadcast a non-speaking state; the server no longer waits for another PCM packet to notice the seat permission changed.
- Commit: server seat-permission revocation `f18fd1e`.


## 2026-11-19 — Native performance: stable inbox presence socket and memoized invite list
- `MessageScreen.kt`: the room-presence Socket.IO `DisposableEffect` is now keyed only by the signed-in account ID, not the mutable chat preview list. Previously, updates to `isInRoom`/`roomId` changed the effect key, disposed the socket, reconnected it, and requested every peer's room status again. The socket now stays connected while presence updates and chat row ordering change.
- `RoomPage.kt`: `RoomInviteSheet` now remembers its de-duplicated, owner-first sorted invite list until room users/current user/owner actually change, avoiding repeated full-list filtering and sorting during unrelated room recompositions.
- No UI, voice, music, chat delivery, room-seat, or navigation behavior was intentionally changed.
- Verification: native Gradle build, CI, and Redmi 14C runtime profiling/regression tests are still required; smoothness improvement is based on removing the identified repeated socket lifecycle and list work, not yet device-measured.

## Room Info member loading performance fix (2026-11-19)
- `android-native/app/src/main/java/com/hurry/voicechat/RoomInfo.kt`: Room Info Members now renders the incoming room/socket member rows immediately, merging persistent cached names, avatars, country, and gender without waiting for profile requests.
- Profile refresh runs in the background with up to six concurrent requests and publishes each member as soon as its refresh completes, instead of waiting for every member before updating the list.
- Room-profile country lookup is skipped when the row/cache already has a country. Existing row-first name/avatar precedence, cache persistence, member roles, profile navigation, and Room Info UI are preserved.
- Build/device performance verification is still required.

### Simultaneous two-speaker voice distortion follow-up
- `JitsiRoomAudio.kt`: changed the concurrent-speaker mixer from nonlinear peak limiting to averaging the PCM samples of speakers present in each output frame. The previous limiter could audibly crunch/flatten peaks when two users spoke together.
- Added a short 8 ms packet collection window before each voice mix to reduce alternating/missing contributions when Socket.IO delivers two publishers' 20 ms frames a few milliseconds apart; per-speaker queues are bounded to four frames to limit stale backlog.
- Music path and UI remain unchanged.
- Commit: `c0ba781`.
- Requires native APK build and a real two-device simultaneous-speech test; not yet verified on hardware.


## 2026-11-20 — Kept-room overlay resumes the retained session
- Native `MainActivity.kt`: fixed the kept-room overlay's reopen action to match the Home kept-room action. It now restores `openedRoom` directly, clears the mini-room state and stale saved mini-room metadata, and does not call `tryOpenRoom` for an already-authorized retained session.
- This avoids the overlay-specific fresh access/join flow that could remount the room as a new entry instead of resuming the retained Socket.IO session. Existing room UI and normal room-entry checks are unchanged.
- Commit: `9a84c62b5e117af1601e428b95fee41cdd971d94`.
- Native build and two-device Keep → reopen voice/chat/seat regression testing remain pending.


## 2026-11-20 — Preserve room while opening profile and overlay pages
- Native `MainActivity.kt`: keep the active `RoomPage` composed as the base layer while public profiles, room support, search, chat, and other root pages are shown above it. Returning from a profile therefore reveals the same in-memory room state instead of disposing and re-entering the room.
- Native `RoomPage.kt`: disable the room BackHandler while another root page covers it, and prevent the room image-preview Dialog from appearing above a covering page.
- Opening a public profile or room support no longer marks the room as explicitly kept/minimized; only the actual Keep action does that, so later room exit still performs normal cleanup.
- Existing room UI is unchanged. Native APK compilation and device navigation/overlay regression tests are still required; this commit is not yet hardware-verified.
- Commits: `4780519`, `d047b99`.


## 2026-11-20 — Complete retained room session and overlay navigation
- Native `MainActivity.kt`: separated the active `roomSession` from the currently visible `openedRoom`. Keep now hides the RoomPage visual host while leaving the same RoomPage composition alive, preserving its in-memory seats, voice publishers/subscribers, Socket.IO listeners, room chat, and music state. Reopening the kept-room bubble restores visibility of the same session instead of mounting a fresh room.
- Native `RoomPage.kt`: removed the old socket-only Keep disposal path. The room socket/listeners and voice state are now released only when the room is genuinely exited or switched; the normal dispose path emits `room_leave`, disconnects, and closes the socket. Opening a public profile or room support no longer sets the Keep flag.
- Navigation: room content stays composed underneath profile/support/search/leaderboard/invite/chat overlays; overlays are rendered above the room and take Back priority. The room BackHandler is disabled while covered. The room image-preview Dialog is dismissed and blocked while another page is active.
- Kept-room Exit now clears the retained `roomSession`, triggering the real room cleanup path rather than only removing the bubble.
- Commits: `8043a36`, `adecf33`, `3a5a97f`, `e36cac5`.
- Native APK build and device regression testing are pending; do not treat this as hardware-verified until the workflow and Keep/profile/overlay tests pass.


## 2026-11-20 — Retained room lifecycle and page overlay completion
- Root cause: the previous Keep callback set `openedRoom = null`, which removed `RoomPage` from the Compose tree. The retained-socket helper only kept the raw Socket.IO connection; `onDispose` still cancelled room work and removed every room event listener, so voice/chat/seat updates were not truly kept alive.
- `MainActivity.kt`: introduced a separate `roomSession` state. `openedRoom` now controls visibility/navigation only; `roomSession` owns the active RoomPage composition. Keep hides the host at 1dp/alpha 0 while retaining the exact RoomPage instance, and opening the kept bubble restores its full-size host. Room switching uses a stable room-ID key; actual Exit, kick, or room switch clears the session.
- `RoomPage.kt`: removed the obsolete socket-only retained-session helper. The same Socket.IO socket, voice state, room listeners, seat state, and chat state remain alive while the RoomPage is hidden. The normal dispose path now runs only on actual exit/switch and performs the full leave/listener/socket/music cleanup.
- Overlay layering: all root pages (profile, family, support, search, leaderboard, invite, chat) render above the active room; Back priority returns to the underlying room. Room BackHandler is disabled while covered. The room chat image-preview Dialog receives the covered state through both chat layouts and closes while another page is open.
- Build blockers discovered in the first CI attempt were fixed: thread `isCovered` through `RoomChatMessages` into `NativeMessage`, correct the invalid `InviteFriends.kt` padding overload, and use `Arrangement.Center` rather than `Alignment.CenterVertically` in `SearchPage.kt`.
- Commits: `8043a36`, `adecf33`, `3a5a97f`, `e36cac5`, `71ae7fe`, `e749e17`, `46472c3`, `5f6fc73`, `22d4639`, `119e948`, `840b364`, `5e07e57`, `cdc747b`.
- APK build and real-device Keep → reopen, profile → Back, overlay layering, voice/chat continuity, and Exit regression tests must pass before calling this hardware-verified.


## 2026-11-20 — Owner Assets Panel
- Added a separate `components/owner/OwnerAssetsPanel.tsx` and mounted it inside the existing Gift Catalog section without replacing the existing gift catalogue.
- The panel supports Chat bubble, Theme, Frame, and Vehicle asset types; the selected type's image-only picker appears immediately below the type selector, followed by asset name, days, preview, and Send.
- Sent asset history is rendered as one table with user avatar/name, Zyora ID, asset type, and days.
- `Zyora-Server/server.js`: added owner-key-protected grant/history endpoints. Grants write to the existing `storeInventory` collection with the selected expiry and do not debit the recipient's wallet; history is stored separately in `ownerAssetGrants`.
- Source committed. Next verification required: web production build, Render deployment, owner grant to a test account, and confirmation that the item appears in native Bag and expires on schedule.


## 2026-11-20 — Room Settings performance pass
- Native `RoomSettingPage.kt`: replaced multi-step mutable member-list mutations with immutable list state and single batched assignments to reduce avoidable Compose recompositions during room-member refresh and admin search.
- Admin-sheet visible-member merging is memoized against its actual inputs instead of rebuilding/deduplicating the full list on every unrelated recomposition.
- Store-theme inventory refresh now replaces the list in one state update; theme item composition no longer creates repeated list snapshots.
- `ThemeFullScreen` no longer runs a permanent one-second clock/recomposition loop when the displayed themes have no expiry. The countdown ticker runs only while expiring themes are present and is bounded by the next expiry.
- Existing network refresh-on-theme-open, expiry filtering/countdowns, member/admin behavior, settings save flow, and UI are retained.
- Commits: `54fb18a`, `0a14efc`. APK compilation and on-device smoothness/regression checks remain required; not yet hardware-verified.

### Store owned-state and responsiveness fix (2026-11-20)
- `android-native/app/src/main/java/com/hurry/voicechat/StorePage.kt`: key owned, equipped, and expiry Compose state to the inventory account ID (the same ID used for the inventory cache/API), not the separate wallet UID. This prevents stale ownership state when the wallet UID remains stable but the inventory account ID changes.
- Items explicitly marked `ownedByDefault` render as `Owned` immediately from the first frame.
- Removed the transient `...` purchase label; unowned cards continue to show `Buy`, while owned cards show `Owned` without waiting for a network request.
- Existing cached-first inventory rendering, purchase endpoint, wallet synchronization, equip/remove actions, and expiry handling are preserved.

### Store instant-action follow-up (2026-11-20)
- Store purchase now optimistically adds the item to the visible Owned state immediately after tapping Buy; the item is removed from that state if the server/payment request fails. The server remains authoritative for ownership and wallet debit.
- Bag Use/Remove updates the equipped state and local inventory cache immediately, while the equip request runs in the background. If the server rejects the change, the previous equipped item state is restored.
- Existing server purchase validation, wallet debit, inventory persistence, and avatar-frame realtime refresh remain in place.

## 2026-11-20 — Leaderboard low-memory crash fix
- `android-native/app/src/main/java/com/hurry/voicechat/Leaderboard.kt`: chroma frame PNGs are bounds-checked and decoded with power-of-two sampling capped at 1024px per dimension before entering the existing 16 MiB LRU cache. This prevents full-resolution frame bitmaps from causing memory spikes and app termination on low-memory Android devices; leaderboard UI and ranking logic are unchanged.

## 2026-11-20 — Leaderboard image-loading crash follow-up
- Replaced the custom `BitmapFactory`/`ImageBitmap` frame loader and manual bitmap LRU cache in `android-native/.../Leaderboard.kt` with Coil `AsyncImage`. Coil now sizes and downsamples podium frame images to their measured Compose bounds and manages decoding/caching, avoiding full-resolution bitmap allocation and manual cache retention. Leaderboard UI and ranking behavior remain unchanged.


## 2026-11-20 — Level profile identity and duplicate seat snapshot guard
- Native `Level.kt`: load the canonical signed-in profile through the existing ProfilePersistence source and render its public-profile name/avatar in the Level header; keep valid local profile values as immediate fallback and avoid displaying a Guest placeholder for a signed-in account.
- Native `RoomPage.kt`: normalize each authoritative seat snapshot so one account can occupy/render only one seat during a rapid seat transfer; preserve the last seat occurrence from the server payload and keep existing voice/speaking state reconciliation.
- No visual redesign or unrelated room behavior changes. Native APK build and live multi-user seat-transfer verification remain pending.


## 2026-11-20 — Room entry critical-path optimization
- Native `RoomPage.kt`: the Socket.IO connect callback now emits the authenticated `room_join` event immediately after the WebSocket handshake, before performing optional local profile-cache reads/writes.
- Root cause: synchronous `NativeIndexDatabase` profile cache work ran inside the connect callback ahead of `room_join`, delaying the server's room membership, presence, and seat snapshot flow on slower devices/storage.
- Profile cache enrichment is now dispatched to the existing room-scoped IO coroutine scope; it remains account/profile-key scoped and is cancelled on real room exit. Room authorization, server-side join validation, seat/voice/chat behavior, and UI are unchanged.
- Commit created for this optimization; native APK CI and real-device cold/warm room-entry timing plus voice/seat regression tests are still required. No measured speedup or bug-free claim is made until those checks pass.


## 2026-11-20 — Bottom navigation and private chat responsiveness
- Native `HomeScreen.kt`: bottom navigation ignores taps on the already-selected tab, avoiding redundant tab-state and unread-state work. Existing Home/Message/Me layout and destinations are unchanged.
- Native `MessageScreen.kt`: plain-text messages now appear immediately via optimistic local echo with a unique message ID, local timestamp, and conversation preview; the existing Socket.IO `private_message` event remains the delivery path. The server echo/ack is deduplicated by that message ID.
- Chat history snapshots are serialized from Compose state and written to the local IndexDB store on `Dispatchers.IO`, keeping disk writes off the UI thread.
- If the server rejects a message because of block state, rollback now removes the message, ID, and timestamp at the same index so subsequent message metadata stays aligned.
- Native APK CI and real-device tab-switch/message-delivery regression tests remain required; performance is not certified until those pass.

## 24 Level own-profile identity and instant seat transfer UI (2026-11-20)
- Root cause: `Level.kt` read name, UID, and photo from `hurry_user`, while login stores the signed-in identity and canonical profile fields in `hurry_auth`. The Level header therefore fell back to Guest/blank avatar. It now reads `hurry_auth`, passes the signed-in account ID/UID to `ProfilePersistence.load`, and uses the saved profile name/image.
- Root cause: when taking another seat, `RoomPage.kt` optimistically occupied the destination but left the previous seat occupied until the server snapshot arrived. It now clears the same account's previous local seat immediately before painting the destination. Existing seat locks and authoritative server updates are preserved.
- Native APK CI and live multi-account seat-transfer verification remain required.


## 2026-11-20 — Private chat opening latency: remove synchronous history reads
- Root cause: `NativeChatScreen` initialized message bodies, timestamps, and IDs with three separate synchronous `NativeIndexDatabase.loadChat` calls during Compose composition. A large local conversation could block the main thread before the chat became responsive. Conversation-preview writes also ran synchronously from send/receive callbacks.
- `MessageScreen.kt`: load the persisted chat once on `Dispatchers.IO`, restore the three aligned message/timestamp/ID lists together, and preserve any messages sent while the background read is in flight. The existing official-chat SharedPreferences fallback remains supported.
- Conversation-preview persistence now runs on the screen's lifecycle-bound IO coroutine scope. Existing optimistic local echo, Socket.IO `private_message` delivery, message IDs/deduplication, and local IndexDB-only chat-history persistence are unchanged. No MongoDB message-history storage or UI redesign was added.
- Bottom navigation already avoids reselect work and retains per-tab saveable state; this change does not alter Home/Message/Me destinations or visuals. Redis-backed delivery/offline queue is already part of the Zyora server architecture; no duplicate Redis layer was added here.
- Status: **Implemented in repository**. Native APK CI, chat history restore/send/receive regression tests, and Redmi 14C navigation/message latency profiling remain required. No measured speedup or device verification is claimed.
## 2026-11-20 — Seat transfer duplicate-avatar root-cause correction
- `RoomPage.kt`: regular-user seat moves now clear every local seat occupied by any verified current-user identity (`accountId`, UID, and current-user ID), not only one account-ID string, before setting the destination seat. The owner/admin `onTake` path applies the same immediate one-seat transition while preserving seat locks.
- `Zyora-Server/server.js`: `room_seat_action: take` now validates the destination before clearing the previous seat, then clears any old seat matching the authenticated socket's verified identity set and sets the destination before emitting one authoritative `room_seats` snapshot. A failed/locked destination no longer removes the user's current seat.
- Existing room UI, seat layout, voice/mute state handling, and Socket.IO/Redis adapter are unchanged. Native build and multi-device live seat-transfer tests are still required; no device verification is claimed.


## 2026-11-20 — Native Home / Message / Me responsiveness
- MeScreen profile and visitor-history cache reads no longer run synchronously during Compose composition; the local database hydration runs on Dispatchers.IO.
- MessageScreen conversation-cache hydration now runs on Dispatchers.IO while the built-in Zyora Team/System rows render immediately.
- Inbox preview persistence and profile-refresh conversation reads/writes are moved off the UI thread to avoid blocking scrolling and tab interactions.
- No visual/layout changes; existing chat, visitor, follow, and profile behavior is retained.
- Native APK workflow/build verification is pending.


## 2026-11-20 — Gift Sharing / Lucky Gift responsiveness
- `GiftPicker.kt`: fixed the Lucky reward threshold ordering so the 2× outcome is reachable; previously the earlier 5× condition shadowed it.
- Gift recipient IDs are now de-duplicated and resolved through one-pass room-user/seat lookup maps, avoiding repeated nested list searches during picker recompositions while always reflecting in-place seat updates.
- The in-flight gift send now snapshots the selected gift, tab, multiplier, recipient IDs, and cost before launching its coroutine so UI recompositions cannot mix transaction data mid-send.
- `RoomPage.kt`: removed the unnecessary `seats.toList()` copy passed into GiftPicker.
- Lucky gifts now render an immediate local sender-side notice/seat effect before the Socket.IO emit. The transfer ID is marked as seen first, so the later server echo does not create a duplicate animation. Other room members still receive the authoritative server event.
- Gift picker layout, colors, gift catalogue, prices, multipliers, and server-side transaction delivery are unchanged.
- Native APK CI and Redmi 14C send/receive, combo, wallet, and multi-recipient regression tests are required; no device smoothness claim is made until verified.

## 2026-11-20 — Persistent native root tabs for faster switching
- `MainActivity.kt`: Home, Message, and Me are now hosted in separate stable `SaveableStateProvider` slots and remain composed while another root tab is selected. Previously, the single `when(tab)` branch disposed the outgoing screen, causing tab-local state and screen effects to be recreated on return.
- Inactive root tabs are constrained to a transparent 1dp host; the selected tab retains the full content area. Existing bottom navigation, screen content, and visual styling are unchanged.
- This keeps screen state and existing socket/listener lifetimes warm across tab switches. It also means inactive screens remain composed, so background work and memory usage must be checked on-device.
- APK compilation and Redmi 14C responsiveness testing are pending; no measured smoothness claim is made.


## 2026-11-20 — Owner gift catalog: Level, Family and Daily Check-in rewards
- Extended `components/owner/OwnerAssetsPanel.tsx` with separate selectable catalog groups for all native Level Entry, Level Frame and Level Medal images, the Family TOP1/TOP2/TOP3/TOP4-10 reward images, and Daily Check-in reward images (including Day 3 ×2, Day 5 ×1, Day 6 ×2 and Day 7 ×3 items).
- New reward thumbnails and videos resolve from the Zyora repository's `public/` assets; existing Chat bubble, Theme, Frame and Vehicle catalog entries keep their original asset source and behavior.
- The existing owner-key-protected grant endpoint and expiry-days input are reused; no new wallet debit or purchase behavior was added.
- Verification still required: web production build, owner grant to a test account, and confirmation that each special reward category is represented correctly in native Bag/equipment. This catalog change alone does not prove native equip support for Medal, Family Tag or Daily Check-in inventory categories.

- Follow-up server compatibility: `Zyora-Server/server.js` now accepts the new owner grant ID families (`le`, `lf`, `lm`, `fr`, `dc`) with a strict tab allowlist while retaining owner-key authentication, account validation, and expiry validation.


## Private chat gift sending
- Native private chat input adds the gift entry point and opens the shared Room GiftPicker.
- Chat GiftPicker is fixed to the active chat partner: only that avatar is shown; MIC and HOUSE recipient controls are hidden.
- Chat gift payments use the canonical wallet debit endpoint and existing private-message Socket.IO delivery. Room GiftPicker behavior is unchanged.

- Native `JitsiRoomAudio.kt`: stabilized concurrent 2+ speaker playback by adding a short voice jitter-buffer prefill, increasing bounded per-speaker frame queues, and using power-normalized mixing instead of averaging every active speaker down. Switched mic capture to Android's `VOICE_COMMUNICATION` source so supported devices can apply communication echo/noise processing. Self-packet filtering and server-side sender-socket exclusion remain in place. **Needs native APK build plus real 2-, 3-, and 5-speaker device testing; this source change alone does not prove echo/crackle is fully resolved.**
## 2026-11-20 — Owner-granted medals and Family Event badges
- Owner Panel medal grants continue to use the existing Store inventory record, preserving the exact selected medal image/video and expiry for the recipient; the server now emits `store_inventory_updated` after a successful grant.
- Added a native owned-medal showcase that reads the recipient's no-cache Store inventory, filters active `Medal` entries, displays the medal image with star and remaining-time metadata, and removes expired medals from the visible list.
- Public Profile Medal now uses the same owned-medal showcase instead of always showing the empty state. Medal page Event tab and Wear Badge sheet also show the recipient's active medals, including Family TOP medal rewards (their existing inventory tab is `Medal`).
- Existing Medal page styling, Public Profile layout, owner panel catalog, and other inventory categories remain unchanged. Native APK build and real-device verification are still required.

## 2026-11-20 — Owner Assets panel layout
- Removed the gift grid, gift tabs, and gift search from the Owner page's Gift Catalog destination; it now contains only the Assets panel.
- Reworked Assets Type into a compact square dropdown that opens a small selection menu.
- Asset choices are compact square image tiles; the selected asset name and thumbnail are shown in a left-aligned row, followed by the Title Assets preview and Days field.
- Kept asset grant requests, asset catalog data, history, and backend endpoints unchanged.
- Owner Gift Catalog asset grants now send the recipient a private Zyora System inbox message after the inventory grant is saved: `You receive: [asset type]`, `Day's: [days]`, `From. Zyora Official Team`. Online delivery uses Socket.IO; offline delivery is queued in Redis for up to 7 days. Backend commit: `0f9e752be32073197c486708c57a7b2af320b740`.


## Temporary Special ID profile sync fix (2026-11-20)
- PublicProfile.kt now copies `vipId`, `vipIdDays`, `vipIdAssignedAt`, `vipIdExpiresAt`, `vipIdBadge`, and `vipIdBadgeType` from the latest remote profile response into the local profile cache, even when a cached profile already exists.
- RoomProfile.kt now refreshes the same temporary-ID fields from the remote profile instead of leaving an existing cache without the assigned ID.
- The original App ID remains the fallback when the temporary ID is expired; no original account identifier is overwritten.
- Native APK build and live server assignment were not verified as part of this source change.


## Owner Special ID + Asset notification root-cause correction (2026-11-21)
- `Zyora-Server/server.js`: Special ID recipient lookup now checks both string and numeric MongoDB account-ID values, including legacy display/account-number fields, matching the numeric compatibility already used by asset grants. It also resolves Firebase UID aliases and sends the official DM through the shared `sendFamilySystemMessage` delivery path (live UID/App-ID rooms or Redis offline queue).
- Owner asset grants now use the same shared official DM delivery helper, numeric account lookup includes all supported account-number fields, and message text uses actual line breaks. Both endpoints return `notification: delivered|queued|unavailable` rather than silently implying that a message was sent.
- Owner Special IDs and Gift Catalog panels now show whether the system message was delivered, queued, or unavailable.
- Original App ID is not overwritten. Native APK and deployed Render behavior still require build/live verification.


## 2026-11-21 — Variable-length VIP ID and public search
- Owner Special ID assignment accepts digit-only VIP IDs without the old 4–20 digit length restriction; the original permanent App ID remains unchanged.
- Native User search now matches an active VIP ID as an additional alias (expiry-aware), while still returning the account's original profile identity. Search cache version was bumped so old results cannot hide new VIP-ID matches.
- VIP ID is not used after its expiry; normal App ID lookup remains intact.


## 2026-11-21 — Room voice silent playback root-cause fix
- Root cause: Zyora-Server/server.js mixes remote microphone PCM per recipient and emits room_voice_chunk with only { roomId }, because the mixed packet has no single speaker identity. Native JitsiRoomAudio.kt required a sender/account ID before enqueuing every voice packet, so it silently discarded every server-mixed packet and nobody could hear remote voices.
- Native JitsiRoomAudio.kt: accept the server's recipient-specific mixed voice stream under a stable room-mix queue key when no publisher ID is present. Continue using real publisher IDs when supplied and preserve self-packet filtering for individually identified packets. No UI, capture format, permission, mute, or server behavior changes.
- Commit: 9b7141b7607ddeb93ff9ae7b8f50ca576974344d.
- Verification: confirmed the deployed-server source packet shape and committed the native listener fix. APK build and live multi-device voice test remain required; not yet hardware-verified.


## 2026-11-21 — Room voice per-speaker relay fix
- Follow-up root-cause fix after music was audible but remote microphones were not: the server's recipient-side mixer was combining microphone frames into one anonymous packet. The native client is designed to queue/mix independent publisher streams, so the server now relays each 20 ms PCM frame separately to every other room socket and includes the publisher account ID (`userId`). The sender is excluded server-side to prevent hearing their own microphone.
- Changed `Zyora-Server/server.js` in commit `a524150c709bdc6e2f604558223b8ff3ac8738d5`.
- Native playback already supports per-publisher queues and local PCM mixing; no UI or native capture behavior changed. Requires the Render backend to deploy the server commit and a live two-device test. Not yet hardware-verified.

## 2026-11-21 — Lucky Bag sender open, expiry timer, and compact room shortcuts
- The Lucky Bag sender's own in-room floating bag remains tappable and uses the same claim/open flow as every other room member; the backend claim route does not exclude the sender.
- The send response's authoritative `expiresAt` is passed through `LuckyBag.kt` and `Fourgride.kt` into `RoomPage.kt`. The small countdown beneath the floating Lucky Bag shows remaining time from the server expiry and hides the expired icon.
- Reduced the right-corner room shortcut artwork sizes and tightened the vertical gaps without changing the column anchor or bottom offset.
- Native code commits: `81f5c9252195e7cd9d699cbfe787709c147794b0`, `14277d7fe28743a4ee49751dbcd56fcf5388a675`, `26f94edfb5303a7255e706279491288767f1c512`, `737a0e37f422e7f7de950e7a9fd08fcde04c673f`.
- Verification: source changes committed; APK build and live-device UI behavior still need verification.


## 2026-11-21 — Lucky Bag expiry and sender refund
- `backend/server.js`: when the five-minute Lucky Bag timer expires, the bag is removed from active room listings. If no claim was made, the full sent amount is returned to the sender's canonical wallet using an idempotency key, and the refreshed wallet balance is broadcast immediately.
- If at least one claim was reserved, the bag expires without refunding already-claimed coins; pending claim confirmations can still complete. Transient refund/storage failures are retried without double-crediting.
- No UI layout or positioning changes.
