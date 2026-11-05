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
