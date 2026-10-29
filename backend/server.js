require("dotenv").config();

const express = require("express");
const http = require("http");
const cors = require("cors");
const { Server } = require("socket.io");
const { GitHubClient } = require("./github-document-db");
const { redis, checkRedisConnection, setOnlineStatus, removeOnlineStatus, setSecondaryStatus, removeSecondaryStatus } = require("./redis");

const app = express();
const server = http.createServer(app);

app.disable("x-powered-by");
app.set("trust proxy", 1);

const allowedOrigins = String(process.env.HURRY_ALLOWED_ORIGINS || "*")
  .split(",")
  .map((v) => v.trim())
  .filter(Boolean);

app.use(cors({
  origin: (origin, callback) => {
    if (!origin || allowedOrigins.includes("*") || allowedOrigins.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error("CORS origin denied"));
  },
  methods: ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
  credentials: true,
}));

app.use((req, res, next) => {
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
  res.setHeader("X-Frame-Options", "SAMEORIGIN");
  next();
});

app.use(express.json({ limit: "2mb" }));
app.use(express.urlencoded({ extended: false, limit: "2mb" }));

const requestRate = new Map();
app.use((req, res, next) => {
  const now = Date.now();
  const ip = String(req.ip || req.socket.remoteAddress || "unknown");
  const current = requestRate.get(ip);
  if (!current || now - current.started >= 60000) {
    requestRate.set(ip, { started: now, count: 1 });
    return next();
  }
  current.count += 1;
  if (current.count > 240) {
    return res.status(429).json({ error: "Too many requests" });
  }
  next();
});

const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"],
  },
  maxHttpBufferSize: 1024 * 1024,
});

const databaseClient = new GitHubClient();
let db = null;

const crypto = require("node:crypto");
function authSession() { return crypto.randomBytes(32).toString("base64url"); }
function passwordHash(password, salt) { return crypto.scryptSync(password, salt, 64).toString("hex"); }
function normalizeEmail(value) { return String(value || "").trim().toLowerCase(); }
app.post("/api/auth/email", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "Account service is temporarily unavailable." });
    const email = normalizeEmail(req.body?.email), password = String(req.body?.password || ""), create = req.body?.create === true;
    if (!email.includes("@") || email.lastIndexOf(".") <= email.indexOf("@") + 1 || email.includes(" ")) return res.status(400).json({ error: "Please enter a valid email." });
    if (password.length < 6 || password.length > 256) return res.status(400).json({ error: "Password must be at least 6 characters." });
    const key = crypto.createHash("sha256").update(email).digest("hex"), accounts = db.collection("auth_accounts");
    let account = await accounts.findOne({ email });
    if (create) {
      if (account) return res.status(409).json({ error: "An account already exists with this email. Please login instead." });
      const salt = crypto.randomBytes(16).toString("hex");
      account = { email, uid: "email:" + key, salt, passwordHash: passwordHash(password, salt), createdAt: new Date() };
      await accounts.insertOne(account);
    } else {
      if (!account) return res.status(401).json({ error: "Invalid email or password." });
      const supplied = Buffer.from(passwordHash(password, account.salt), "hex"), expected = Buffer.from(account.passwordHash, "hex");
      if (supplied.length !== expected.length || !crypto.timingSafeEqual(supplied, expected)) return res.status(401).json({ error: "Invalid email or password." });
    }
    return res.json({ localId: account.uid, email, displayName: email.split("@")[0], photoUrl: "", idToken: authSession() });
  } catch (error) { console.error("Email authentication failed:", error); return res.status(500).json({ error: "Could not authenticate this account. Please try again." }); }
});
app.post("/api/auth/google", async (req, res) => {
  try {
    const idToken = String(req.body?.idToken || "");
    if (!idToken) return res.status(400).json({ error: "Google sign-in token is missing." });
    const verify = await fetch("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodeURIComponent(idToken));
    if (!verify.ok) return res.status(401).json({ error: "Google sign-in could not be verified." });
    const profile = await verify.json();
    if (profile.email_verified !== "true" || !profile.sub || !profile.email) return res.status(401).json({ error: "Google account verification failed." });
    const uid = "google:" + String(profile.sub);
    const account = {
      uid,
      provider: "google",
      email: normalizeEmail(profile.email),
      displayName: String(profile.name || profile.email.split("@")[0]).slice(0, 120),
      photoUrl: String(profile.picture || "").slice(0, 2048),
      emailVerified: true,
      updatedAt: new Date().toISOString(),
    };

    // Persist Google accounts in the same GitHub-backed document database used by Zyora.
    // No Firebase/Supabase account store and no GitHub token is shipped inside the APK.
    if (!db) return res.status(503).json({ error: "Zyora account storage is starting. Please try again in a few seconds." });
    await db.collection("auth_accounts").updateOne(
      { uid },
      { $set: account, $setOnInsert: { createdAt: new Date().toISOString() } },
      { upsert: true }
    );

    return res.json({ localId: uid, email: account.email, displayName: account.displayName, photoUrl: account.photoUrl, idToken: authSession() });
  } catch (error) { console.error("Google authentication failed:", error); return res.status(502).json({ error: "Google sign-in service is temporarily unavailable." }); }
});


const onlineUsers = new Map();

const memoryRoundWinners = {
  fruitparty: { round: -1, winners: [] },
  wildparty: { round: -1, winners: [] }
};

/*
 * roomId -> Map(userId -> socket count)
 */
const roomUsers = new Map();

// Deduplicate client replay of money-transfer events for a short window.
const recentTransferIds = new Map();

/*
 * roomId -> Map(seatNumber -> seat state)
 * Seat state is kept in memory because it only represents the
 * currently active room session.
 */
const roomSeats = new Map();

function getRoomSeats(roomId) {
  const room = roomSeats.get(String(roomId));
  if (!room) return [];

  return Array.from(room.values()).sort(
    (a, b) => Number(a.number) - Number(b.number)
  );
}

function emitRoomSeats(roomId) {
  const room = String(roomId);
  io.to(`room:${room}`).emit("room_seats", {
    roomId: room,
    seats: getRoomSeats(room),
  });
}

const pendingSeatDisconnects = new Map();
const lockedRooms = new Map();

function cancelPendingSeatDisconnect(roomId, userId) {
  const key = `${String(roomId)}:${String(userId)}`;
  const timer = pendingSeatDisconnects.get(key);
  if (timer) {
    clearTimeout(timer);
    pendingSeatDisconnects.delete(key);
  }
}

function clearUserSeat(roomId, userId, expectedSocketId = null) {
  // If this is a reconnecting session, never let an old disconnect clear
  // the newly occupied seat.
  if (expectedSocketId) {
    const room = roomSeats.get(String(roomId));
    if (room) {
      const id = String(userId);
      for (const seat of room.values()) {
        const seatUserId = String(seat?.user?.accountId || seat?.user?.userId || "");
        if (seatUserId === id && String(seat?.socketId || "") !== String(expectedSocketId)) {
          return false;
        }
      }
    }
  }
  const room = roomSeats.get(String(roomId));
  if (!room) return false;

  let changed = false;

  for (const [number, seat] of room.entries()) {
    if (
      seat?.isOccupied &&
      (String(seat?.user?.accountId) === String(userId) ||
       String(seat?.user?.userId) === String(userId))
    ) {
      room.set(number, {
        ...seat,
        isOccupied: false,
        user: undefined,
        isMuted: false,
        isSpeaking: false,
        gif: undefined,
      });
      changed = true;
    }
  }

  if (room.size === 0) {
    roomSeats.delete(String(roomId));
  }

  return changed;
}

function clearUserSeatGracefully(roomId, userId, delayMs = 5000, socketId = null) {
  const room = String(roomId);
  const id = String(userId);
  const key = `${room}:${id}`;

  cancelPendingSeatDisconnect(room, id);

  if (delayMs <= 0) {
    if (clearUserSeat(room, id)) {
      emitRoomSeats(room);
    }
    return;
  }

  const timer = setTimeout(() => {
    pendingSeatDisconnects.delete(key);
    if (clearUserSeat(room, id, socketId)) {
      emitRoomSeats(room);
    }
  }, delayMs);

  pendingSeatDisconnects.set(key, timer);
}


function getRoomUsers(roomId) {
  const users = roomUsers.get(String(roomId));
  if (!users) return [];

  return Array.from(users.entries()).map(([userId, data]) => ({
    accountId: String(data?.accountId || userId),
    userId: String(data?.userId || userId),
    name: data?.name || "User",
    image: data?.image || "/default-avatar.png",
    email: data?.email || "",
  }));
}

function addUserToRoom(roomId, userId, userData = {}) {
  if (!roomId || !userId) return;

  const room = String(roomId);
  const id = String(userId);

  if (!roomUsers.has(room)) {
    roomUsers.set(room, new Map());
  }

  const users = roomUsers.get(room);
  const existing = users.get(id);

  if (existing) {
    existing.count += 1;
    existing.name = userData.name || existing.name || "User";
    existing.image =
      userData.image ||
      existing.image ||
      "/default-avatar.png";
    existing.email = userData.email || existing.email || "";
    if (userData.accountId) existing.accountId = String(userData.accountId);
  } else {
    users.set(id, {
      count: 1,
      userId: id,
      accountId: String(userData.accountId || id),
      name: userData.name || "User",
      image: userData.image || "/default-avatar.png",
      email: userData.email || "",
    });
  }
}

function removeUserFromRoom(roomId, userId) {
  if (!roomId || !userId) return;

  const room = String(roomId);
  const id = String(userId);

  const users = roomUsers.get(room);
  if (!users) return;

  const existing = users.get(id);
  if (!existing) return;

  if (Number(existing.count || 0) <= 1) {
    users.delete(id);
  } else {
    existing.count -= 1;
    users.set(id, existing);
  }

  if (users.size === 0) {
    roomUsers.delete(room);
  }
}

function emitRoomPresence(roomId) {
  if (!roomId) return;

  const room = String(roomId);
  const users = getRoomUsers(room);

  io.to(`room:${room}`).emit("room_presence", {
    roomId: room,
    users,
    activeUserCount: users.length,
  });
}

function emitGlobalRoomPresence() {
  const rooms = getGlobalRoomPresence();

  io.emit("global_room_presence", {
    rooms,
  });
}

function socketOwnsIdentity(socket, id) {
  const value = String(id || "");
  return Boolean(
    value &&
    (String(socket.userId || "") === value ||
     String(socket.accountId || "") === value ||
     String(socket.roomUserId || "") === value ||
     String(socket.roomAccountId || "") === value)
  );
}

function socketIsInRoom(socket, roomId) {
  const room = String(roomId || "");
  return Boolean(
    room &&
    String(socket.roomId || "") === room &&
    socket.rooms.has(`room:${room}`)
  );
}

function markUserOnline(userId, socket, accountId = null) {
  if (!userId) return;

  const id = String(userId);
  const account = accountId ? String(accountId) : null;

  if (
    socket.userId === id &&
    socket.accountId === account &&
    socket.isPresenceRegistered
  ) {
    return;
  }

  socket.userId = id;
  socket.accountId = account;
  socket.isPresenceRegistered = true;

  socket.presenceIds = new Set([id]);

  if (account) {
    socket.presenceIds.add(account);
  }

  socket.presenceIds.forEach((presenceId) => {
    socket.join(`user:${presenceId}`);

    const count = onlineUsers.get(presenceId) || 0;
    onlineUsers.set(presenceId, count + 1);

    if (count === 0) {
      socket.broadcast.emit("user_online", presenceId);
    }

    // Sync to Redis asynchronously (fire and forget)
    setOnlineStatus(presenceId);
    setSecondaryStatus(presenceId);
  });

  socket.emit("presence_status", {
    userId: id,
    accountId: account,
    online: true,
  });
}

function markUserOffline(socket) {
  if (!socket.isPresenceRegistered) {
    return;
  }

  socket.isPresenceRegistered = false;

  const presenceIds =
    socket.presenceIds ||
    new Set(
      [
        socket.userId,
        socket.accountId,
      ].filter(Boolean)
    );

  presenceIds.forEach((presenceId) => {
    const id = String(presenceId);
    const count = onlineUsers.get(id) || 0;

    if (count <= 1) {
      onlineUsers.delete(id);
      socket.broadcast.emit("user_offline", id);

      // Remove from Redis asynchronously
      removeOnlineStatus(id);
      removeSecondaryStatus(id);
    } else {
      onlineUsers.set(id, count - 1);
    }
  });

  socket.presenceIds = new Set();
}

function getGlobalRoomPresence() {
  return Array.from(roomUsers.entries()).map(
    ([roomId, users]) => ({
      roomId: String(roomId),
      users: getRoomUsers(roomId),
      activeUserCount: getRoomUsers(roomId).length,
    })
  );
}


// ==================== USERS API ====================


app.get("/api/rooms/image", async (req, res) => {
  try {
    if (!db) return res.status(503).end();
    const roomId = String(req.query.roomId || '').trim();
    if (!roomId) return res.status(400).end();
    const room = await db.collection("rooms").findOne({
      $or: [{ accountId: roomId }, { id: roomId }, { roomId: roomId }]
    });
    if (!room) return res.status(404).end();
    const value = String(room.dp || room.image || room.roomDp || room["Room dp"] || '').trim();
    if (!value.startsWith('data:image/')) return res.redirect(value || '/default-avatar.png');
    const match = value.match(/^data:(image\/[a-zA-Z0-9.+-]+);base64,(.*)$/s);
    if (!match) return res.status(415).end();
    res.setHeader('Content-Type', match[1]);
    res.setHeader('Cache-Control', 'public, max-age=86400, immutable');
    return res.send(Buffer.from(match[2], 'base64'));
  } catch (error) {
    console.error("GET /api/rooms/image error:", error);
    return res.status(500).end();
  }
});

app.get("/api/rooms", async (req, res) => {
  try {
    const roomId = req.query.roomId;

    // Keep existing room cards usable during a temporary GitHub storage outage.
    // Redis is only a stale-read fallback here; GitHub storage remains the source of truth.
    if (!db) {
      if (redis) {
        try {
          const cachedRooms = await redis.get("rooms:list:v1");
          const parsedRooms = typeof cachedRooms === "string"
            ? JSON.parse(cachedRooms)
            : cachedRooms;
          if (Array.isArray(parsedRooms)) {
            res.set("Cache-Control", "no-store");
            if (roomId) {
              const cachedRoom = parsedRooms.find((room) =>
                [room.accountId, room.id, room.roomId].some(
                  (id) => id != null && String(id) === String(roomId)
                )
              );
              if (cachedRoom) return res.json({ room: cachedRoom, stale: true });
              return res.status(404).json({ error: "Room not found in cached room list" });
            }
            return res.json({ rooms: parsedRooms, serverTime: Date.now(), stale: true });
          }
        } catch (cacheError) {
          console.warn("Room stale-cache fallback skipped:", cacheError.message);
        }
      }
      return res.status(503).json({ error: "GitHub storage is not connected" });
    }
    const hasIncrementalSyncParam = req.query.sinceUpdatedAt !== undefined;
    const sinceUpdatedAt = Math.max(0, Number(req.query.sinceUpdatedAt) || 0);
    if (roomId) {
      const room = await db.collection("rooms").findOne({
        $or: [
          { accountId: roomId },
          { id: roomId },
          { roomId: roomId }
        ]
      });

      if (!room) {
        return res.status(404).json({ error: "Room not found" });
      }
      const safeRoom = { ...room };
      const roomImage = String(safeRoom.dp || safeRoom.image || safeRoom.roomDp || safeRoom["Room dp"] || '').trim();
      if (roomImage.startsWith('data:image/')) {
        const base = `${req.protocol}://${req.get('host')}`;
        const id = encodeURIComponent(String(safeRoom.accountId || safeRoom.id || roomId));
        safeRoom.dp = `${base}/api/rooms/image?roomId=${id}`;
        safeRoom.image = safeRoom.dp;
      }
      return res.json({ room: safeRoom });
    }

    // Redis keeps the frequently requested room-card payload warm; GitHub storage
    // remains the source of truth and Redis failures never block the API.
    const roomCacheKey = "rooms:list:v1";
    // Incremental requests bypass the full-list Redis cache.
    if (!hasIncrementalSyncParam && redis) {
      try {
        const cachedRooms = await redis.get(roomCacheKey);
        if (cachedRooms) {
          const parsed = typeof cachedRooms === "string" ? JSON.parse(cachedRooms) : cachedRooms;
          if (Array.isArray(parsed)) {
            res.set("Cache-Control", "public, max-age=15, stale-while-revalidate=30");
            return res.json({ rooms: parsed });
          }
        }
      } catch (cacheError) {
        console.warn("Room Redis cache read skipped:", cacheError.message);
      }
    }

    const roomFilter = sinceUpdatedAt > 0 ? { updatedAt: { $gt: sinceUpdatedAt } } : {};
    const rooms = await db.collection("rooms")
      .find(roomFilter)
      .sort({ updatedAt: -1, createdAt: -1 })
      .limit(100)
      .toArray();

    const base = `${req.protocol}://${req.get('host')}`;
    const safeRooms = rooms.map((room) => {
      const safeRoom = { ...room };
      const roomImage = String(safeRoom.dp || safeRoom.image || safeRoom.roomDp || safeRoom["Room dp"] || '').trim();
      if (roomImage.startsWith('data:image/')) {
        const id = encodeURIComponent(String(safeRoom.accountId || safeRoom.id || safeRoom.roomId || ''));
        safeRoom.dp = `${base}/api/rooms/image?roomId=${id}`;
        safeRoom.image = safeRoom.dp;
      }
      return safeRoom;
    });

    if (!hasIncrementalSyncParam && redis) {
      try { await redis.set(roomCacheKey, JSON.stringify(safeRooms), { ex: 21600 }); }
      catch (cacheError) { console.warn("Room Redis cache write skipped:", cacheError.message); }
    }
    res.set("Cache-Control", "public, max-age=15, stale-while-revalidate=30");
    return res.json({ rooms: safeRooms, serverTime: Date.now() });
  } catch (error) {
    console.error("GET /api/rooms error:", error);
    return res.status(500).json({ error: "Failed to fetch rooms" });
  }
});

app.put("/api/rooms", async (req, res) => {
  try {
    if (!db) {
      return res.status(503).json({ error: "GitHub storage is not connected" });
    }

    const data = req.body || {};

    // Strict room ID definition using accountId or user uid
    const accountId = String(
      data.accountId ||
      data["Room Admin"] ||
      data.roomAdmin ||
      data.id ||
      ""
    ).trim();

    if (!accountId) {
      return res.status(400).json({ error: "Missing room accountId" });
    }

    const rooms = db.collection("rooms");
    const roomFilter = {
      $or: [
        { accountId },
        { id: accountId },
        { roomId: accountId },
        { "Room Admin": accountId }
      ]
    };
    const existingRoom = await rooms.findOne(roomFilter) || {};
    const firstText = (...values) => values
      .map((value) => value == null ? "" : String(value).trim())
      .find((value) => value && value !== "null") || "";
    const incomingDp = firstText(data.dp, data.image, data.roomDp, data["Room dp"]);
    const savedDp = firstText(existingRoom.dp, existingRoom.image, existingRoom.roomDp, existingRoom["Room dp"]);
    const incomingTheme = firstText(data.theme, data.roomTheme, data["Room Theme"]);
    const incomingMessage = firstText(data.message, data.announcement);
    const hasLockState = Object.prototype.hasOwnProperty.call(data, "isLocked");
    const hasPassword = Object.prototype.hasOwnProperty.call(data, "roomPassword");
    const incomingAdmins = Array.isArray(data.admin) ? data.admin
      : Array.isArray(data.admins) ? data.admins : existingRoom.admin;

    // Room settings arrive from multiple clients with slightly different field names.
    // Merge partial updates with the saved record so a DP/theme/admin list can never
    // be reset by a client that only sends name or mic mode.
    const roomData = {
      accountId,
      id: accountId,
      roomId: accountId,
      name: firstText(data.name, data.roomName, data["Room Name"], existingRoom.name, existingRoom.roomName) || "Room",
      dp: incomingDp || savedDp || "/default-avatar.png",
      country: firstText(data.country, data.Country, existingRoom.country, existingRoom.Country) || "🇮🇳",
      message: incomingMessage || firstText(existingRoom.message, existingRoom.announcement),
      theme: incomingTheme || firstText(existingRoom.theme, existingRoom.roomTheme, existingRoom["Room Theme"]) || "forest-night",
      micMode: data.micMode !== undefined && Number.isFinite(Number(data.micMode))
        ? Number(data.micMode)
        : Number.isFinite(Number(existingRoom.micMode)) ? Number(existingRoom.micMode) : 15,
      admin: incomingAdmins || [],
      isLocked: hasLockState ? Boolean(data.isLocked) : Boolean(existingRoom.isLocked),
      roomPassword: hasPassword ? (data.roomPassword || null) : (existingRoom.roomPassword || null),
      updatedAt: Date.now()
    };

    await rooms.updateOne(
      roomFilter,
      {
        $set: roomData,
        $setOnInsert: {
          createdAt: Date.now()
        }
      },
      { upsert: true }
    );

    if (redis) {
      try { await redis.del("rooms:list:v1"); }
      catch (cacheError) { console.warn("Room Redis cache invalidation skipped:", cacheError.message); }
    }

    return res.json({
      success: true,
      room: roomData
    });
  } catch (error) {
    console.error("PUT /api/rooms error:", error);
    return res.status(500).json({ error: "Failed to save room" });
  }
});

// Persistent room follows: stored by stable user ID so follows survive reinstall.
app.get("/api/room-follows", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "GitHub storage is not connected" });
    const userId = String(req.query.userId || "").trim();
    if (!userId) return res.status(400).json({ error: "Missing userId" });
    const follows = await db.collection("roomFollows").find({ userId }).sort({ followedAt: -1 }).limit(500).toArray();
    return res.json({ follows: follows.map(({ _id, ...item }) => item) });
  } catch (error) {
    console.error("GET /api/room-follows error:", error);
    return res.status(500).json({ error: "Failed to fetch room follows" });
  }
});

app.post("/api/room-follows", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "GitHub storage is not connected" });
    const userId = String(req.body?.userId || "").trim();
    const roomId = String(req.body?.roomId || "").trim();
    if (!userId || !roomId) return res.status(400).json({ error: "Missing userId or roomId" });
    const room = await db.collection("rooms").findOne({ $or: [{ accountId: roomId }, { id: roomId }, { roomId }] });
    const follow = {
      userId, roomId,
      roomName: String(req.body?.roomName || room?.name || room?.roomName || "Room"),
      roomImage: String(req.body?.roomImage || room?.dp || room?.image || room?.roomDp || ""),
      roomCountry: String(req.body?.roomCountry || room?.country || "🇮🇳"),
      followerName: String(req.body?.followerName || "User"),
      followerImage: String(req.body?.followerImage || ""),
      followedAt: Date.now()
    };
    await db.collection("roomFollows").updateOne({ userId, roomId }, { $set: follow }, { upsert: true });
    return res.json({ success: true, follow });
  } catch (error) {
    console.error("POST /api/room-follows error:", error);
    return res.status(500).json({ error: "Failed to follow room" });
  }
});

app.delete("/api/room-follows", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "GitHub storage is not connected" });
    const userId = String(req.query.userId || "").trim();
    const roomId = String(req.query.roomId || "").trim();
    if (!userId || !roomId) return res.status(400).json({ error: "Missing userId or roomId" });
    await db.collection("roomFollows").deleteOne({ userId, roomId });
    return res.json({ success: true });
  } catch (error) {
    console.error("DELETE /api/room-follows error:", error);
    return res.status(500).json({ error: "Failed to unfollow room" });
  }
});

app.get("/api/users", async (req, res) => {
  try {
    if (!db) {
      return res.status(503).json({
        error: "GitHub storage is not connected",
      });
    }

    const isOnlineCheck = req.query.online === 'true';

    const searchQuery = String(
      req.query.search ||
      req.query.q ||
      req.query.query ||
      ""
    ).trim();

    const uid = String(req.query.uid || "").trim();
    const accountId = String(req.query.accountId || "").trim();

    const users = db.collection("users");

    const normalizeUser = (user) => {
      const id = String(
        user.id || user.uid || user.appLongId || user._id || ""
      );

      let numberId = String(
        user.accountId ||
        user.accountNumber ||
        user["Account Number"] ||
        user.displayUserNumber ||
        ""
      );

      if (!numberId || numberId === id) {
        let hash = 0;
        for (let i = 0; i < id.length; i++) {
          hash = (hash << 5) - hash + id.charCodeAt(i);
          hash |= 0;
        }
        numberId = String(10000000 + (Math.abs(hash) % 90000000));
      }

      return {
        ...user,
        id,
        uid: String(user.uid || id),
        appLongId: String(user.appLongId || id),
        accountId: numberId,
        accountNumber: numberId,
        displayUserNumber: numberId,
        name: user.name || user.displayName || user.userName || "User",
        email: user.email || user.gmail || user.emailPhone || "",
        image: user.image || user.photo || user.photoURL || user.avatar || "/default-avatar.png",
        country: user.country || "🇮🇳",
      };
    };

    const q = searchQuery || accountId || uid;
    if (q) {
      // Numeric ID search takes the indexed exact-match path first.
      // The UI only accepts numeric IDs, so avoid expensive regex scans when possible.
      const numericQ = /^\d+$/.test(q) ? Number(q) : null;
      if (numericQ !== null) {
        const exactConditions = [
          { accountId: q }, { accountNumber: q }, { "Account Number": q },
          { displayUserNumber: q }, { uid: q }, { id: q }, { appLongId: q },
          { accountId: numericQ }, { accountNumber: numericQ },
          { displayUserNumber: numericQ }, { uid: q }, { id: q }
        ];
        const exactMatches = await users.find({ $or: exactConditions }).limit(20).toArray();
        if (exactMatches.length) {
          let exactUsers = exactMatches.map(normalizeUser);
          if (isOnlineCheck) {
            exactUsers = exactUsers.filter(u =>
              onlineUsers.has(String(u.id)) || onlineUsers.has(String(u.uid)) || onlineUsers.has(String(u.accountId))
            );
          }
          if (exactUsers.length) {
            res.set("Cache-Control", "private, max-age=5, stale-while-revalidate=15");
            return res.json({ users: exactUsers, user: exactUsers[0] });
          }
        }
      }

      const regex = new RegExp(q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
      const orConditions = [
        { accountId: q },
        { accountNumber: q },
        { "Account Number": q },
        { displayUserNumber: q },
        { uid: q },
        { id: q },
        { appLongId: q },
        { accountId: regex },
        { accountNumber: regex },
        { displayUserNumber: regex },
        { name: regex },
        { displayName: regex },
        { userName: regex },
        { uid: regex },
        { id: regex },
        { appLongId: regex },
      ];

      // Add numeric match if the query is a valid number
      if (numericQ !== null) {
        orConditions.push(
          { accountId: numericQ },
          { accountNumber: numericQ },
          { displayUserNumber: numericQ },
          { uid: numericQ },
          { id: numericQ }
        );
      }

      const matches = await users
        .find({
          $or: orConditions,
        })
        .limit(50)
        .toArray();

      const normalizedUsers = matches.map(normalizeUser);

      // Filter/rank to prioritize exact or startsWith matches on accountId, accountNumber, uid, or name
      const rankedUsers = normalizedUsers.filter((u) => {
        const uAcc = String(u.accountId || u.accountNumber || u.displayUserNumber || "").toLowerCase();
        const uUid = String(u.uid || u.id || u.appLongId || "").toLowerCase();
        const uName = String(u.name || "").toLowerCase();
        const qLower = q.toLowerCase();

        return (
          uAcc.includes(qLower) ||
          uUid.includes(qLower) ||
          uName.includes(qLower)
        );
      });

      const finalUsers = rankedUsers.length > 0 ? rankedUsers : normalizedUsers;

      let filteredUsers = finalUsers;
      if (isOnlineCheck) {
        filteredUsers = filteredUsers.filter(u => onlineUsers.has(String(u.id)) || onlineUsers.has(String(u.uid)) || onlineUsers.has(String(u.accountId)));
      }

      if (filteredUsers.length === 0 && (uid || q)) {
        return res.status(404).json({ error: "User not found" });
      }

      return res.json({
        users: filteredUsers,
        user: filteredUsers[0] || null,
      });
    }

    // No query = return all registered users (for Owner Panel).
    const allUsers = await users
      .find({})
      .sort({ createdAt: -1 })
      .limit(1000)
      .toArray();

    return res.json({
      users: allUsers.map(normalizeUser),
    });
  } catch (error) {
    console.error("GET /api/users error:", error);
    return res.status(500).json({
      error: "Failed to fetch user",
    });
  }
});


// ==============================================================
 // PERSISTENT FOLLOW GRAPH + REAL-TIME FOLLOW COUNTS
 // GitHub storage stores each follower/target pair; Socket.IO broadcasts changes.
 // ==============================================================
app.get("/api/follows", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });
    const targetUid = String(req.query.targetUid || "").trim();
    const followerId = String(req.query.followerId || "").trim();
    if (!targetUid) return res.status(400).json({ error: "Missing targetUid" });

    const follows = db.collection("userFollows");
    const [followers, following] = await Promise.all([
      follows.countDocuments({ targetUid }),
      followerId ? follows.findOne({ targetUid, followerId }) : Promise.resolve(null),
    ]);
    const followerRows = await follows.find({ targetUid }).sort({ createdAt: 1 }).limit(500).toArray();
    const ids = followerRows.map((row) => row.followerId).filter(Boolean);
    let users = [];
    if (ids.length) {
      users = await db.collection("users").find({
        $or: [{ uid: { $in: ids } }, { id: { $in: ids } }, { appLongId: { $in: ids } }],
      }).project({ uid: 1, id: 1, name: 1, displayName: 1, userName: 1, image: 1, photo: 1, photoURL: 1, accountId: 1 }).toArray();
    }
    const byId = new Map();
    for (const user of users) {
      const id = String(user.uid || user.id || user.appLongId || "");
      byId.set(id, {
        id,
        name: user.name || user.displayName || user.userName || "User",
        image: user.image || user.photo || user.photoURL || "/default-avatar.png",
        accountId: String(user.accountId || ""),
      });
    }
    res.json({
      success: true,
      targetUid,
      followers,
      following: Boolean(following),
      followerList: ids.map((id) => byId.get(String(id)) || { id: String(id), name: "User", image: "/default-avatar.png", accountId: "" }),
    });
  } catch (error) {
    console.error("GET /api/follows error:", error);
    res.status(500).json({ error: "Failed to load followers" });
  }
});

app.post("/api/follows", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });
    const followerId = String(req.body?.followerId || "").trim();
    const targetUid = String(req.body?.targetUid || "").trim();
    const action = String(req.body?.action || "follow").toLowerCase();
    if (!followerId || !targetUid) return res.status(400).json({ error: "Missing followerId or targetUid" });
    if (followerId === targetUid) return res.status(400).json({ error: "Cannot follow yourself" });
    if (action !== "follow" && action !== "unfollow") return res.status(400).json({ error: "Invalid action" });

    const follows = db.collection("userFollows");
    if (action === "follow") {
      await follows.updateOne(
        { followerId, targetUid },
        { $setOnInsert: { followerId, targetUid, createdAt: Date.now() }, $set: { updatedAt: Date.now() } },
        { upsert: true }
      );
    } else {
      await follows.deleteOne({ followerId, targetUid });
    }

    const followers = await follows.countDocuments({ targetUid });
    const following = action === "follow";
    const payload = { targetUid, followerId, followers, following, action, timestamp: Date.now() };
    io.emit("follow_updated", payload);
    res.json({ success: true, ...payload });
  } catch (error) {
    console.error("POST /api/follows error:", error);
    res.status(500).json({ error: "Failed to update follow" });
  }
});

app.post("/api/users/block", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });
    const { blockedBy, blockedUser } = req.body;
    if (!blockedBy || !blockedUser) return res.status(400).json({ error: "Missing ids" });

    await db.collection("blocks").updateOne(
      { blockerId: String(blockedBy), blockedId: String(blockedUser) },
      { $set: { blockerId: String(blockedBy), blockedId: String(blockedUser), timestamp: Date.now() } },
      { upsert: true }
    );
    res.json({ success: true });
  } catch (err) {
    console.error("POST /api/users/block error:", err);
    res.status(500).json({ error: "Internal error" });
  }
});

app.post("/api/users/unblock", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });
    const { blockedBy, blockedUser } = req.body;
    if (!blockedBy || !blockedUser) return res.status(400).json({ error: "Missing ids" });

    await db.collection("blocks").deleteOne({ blockerId: String(blockedBy), blockedId: String(blockedUser) });
    res.json({ success: true });
  } catch (err) {
    console.error("POST /api/users/unblock error:", err);
    res.status(500).json({ error: "Internal error" });
  }
});

app.put("/api/users", async (req, res) => {
  try {
    if (!db) {
      return res.status(503).json({
        error: "GitHub storage is not connected",
      });
    }

    const data = req.body || {};
    const requestedRemoveFields = Array.isArray(data.removeFields)
      ? data.removeFields.map((field) => String(field || "").trim()).filter(Boolean)
      : [];
    const removableProfileFields = new Set([
      "image", "photo", "photoURL", "avatar", "picture",
      "coverPhotos", "coverPhoto", "albumImages", "bio", "age", "gender"
    ]);
    const removeFields = [...new Set(requestedRemoveFields)].filter((field) => removableProfileFields.has(field));
    const profileData = { ...data };
    delete profileData.removeFields;
    removeFields.forEach((field) => delete profileData[field]);

    const uid = String(
      data.uid ||
      data.id ||
      data.appLongId ||
      ""
    ).trim();

    if (!uid) {
      return res.status(400).json({
        error: "Missing user uid",
      });
    }

    const accountId = String(
      data.accountId ||
      data.accountNumber ||
      data["Account Number"] ||
      data.displayUserNumber ||
      ""
    ).trim();

    const users = db.collection("users");

    // Do not let profile edits or stale clients erase a previously saved avatar
    // by sending an empty/default image. Explicit removeFields remains supported.
    const avatarFields = ["photo", "image", "photoURL", "avatar", "picture"];
    const explicitlyRemovingAvatar = removeFields.some((field) => avatarFields.includes(field));
    const existingUser = await users.findOne({
      $or: [{ uid }, { id: uid }, { appLongId: uid }]
    });
    if (existingUser && !explicitlyRemovingAvatar) {
      const isUsableAvatar = (value) => {
        const candidate = String(value || "").trim();
        return candidate && candidate !== "/default-avatar.png" && candidate !== "default-avatar.png";
      };
      const existingAvatar = avatarFields.map((field) => existingUser[field]).find(isUsableAvatar);
      const incomingAvatar = avatarFields.map((field) => profileData[field]).find(isUsableAvatar);
      if (existingAvatar && !incomingAvatar) {
        for (const field of avatarFields) {
          if (!isUsableAvatar(profileData[field])) profileData[field] = existingAvatar;
        }
      }
    }

    const userData = {
      ...profileData,
      id: String(data.id || uid),
      uid,
      appLongId: String(data.appLongId || uid),
      accountId,
      accountNumber: accountId,
      displayUserNumber: accountId,
      updatedAt: Date.now(),
      lastIp: req.headers['x-forwarded-for'] || req.socket.remoteAddress || '',
    };
    if (data.deviceId) {
      userData.lastDeviceId = data.deviceId;
    }

    const updateDocument = {
      $set: userData,
      $setOnInsert: { createdAt: Date.now() },
    };
    if (removeFields.length) {
      updateDocument.$unset = Object.fromEntries(removeFields.map((field) => [field, ""]));
    }

    await users.updateOne(
      { $or: [{ uid }, { id: uid }, { appLongId: uid }] },
      updateDocument,
      { upsert: true }
    );

    return res.json({
      success: true,
      user: userData,
    });
  } catch (error) {
    console.error("PUT /api/users error:", error);
    return res.status(500).json({
      error: "Failed to save user",
    });
  }
});

app.get("/api/privateMessages", (_req, res) => {
  // Private chat history is not stored on the server; Owner panel uses its browser-local IndexedDB
  // plus the Socket.IO private_message_monitor event for live messages.
  return res.json({ messages: [] });
});


/* One permanent wallet per user: users.coins is the canonical coin balance.
   Legacy luckyBagWallets is read only for one-time migration; new writes go to users. */
const luckyBagTimers = new Map();
const walletUserQuery = (userId) => ({ $or: [
  { uid: String(userId) }, { id: String(userId) }, { appLongId: String(userId) },
  { accountId: String(userId) }, { accountNumber: String(userId) },
  { displayUserNumber: String(userId) }
] });
async function ensureWalletUser(userId) {
  if (!db) throw new Error("GitHub storage is not connected");
  const users = db.collection("users");
  let user = await users.findOne(walletUserQuery(userId));
  if (!user) {
    const legacy = await db.collection("luckyBagWallets").findOne({ userId: String(userId) });
    const initialCoins = Number(legacy?.balance ?? 0);
    await users.updateOne({ uid: String(userId) }, { $setOnInsert: {
      uid: String(userId), id: String(userId), appLongId: String(userId),
      coins: initialCoins, diamonds: 0, createdAt: Date.now()
    } }, { upsert: true });
    user = await users.findOne(walletUserQuery(userId));
  }
  if (user && !Number.isFinite(Number(user.coins))) {
    const legacy = await db.collection("luckyBagWallets").findOne({ userId: String(userId) });
    const initialCoins = Math.max(Number(user.balance || 0), Number(legacy?.balance || 0));
    await users.updateOne({ _id: user._id }, { $set: { coins: Math.max(0, initialCoins) } });
    user.coins = Math.max(0, initialCoins);
  } else if (user && user.coins == null) {
    const legacy = await db.collection("luckyBagWallets").findOne({ userId: String(userId) });
    const initialCoins = Number(user.balance ?? legacy?.balance ?? 0);
    await users.updateOne({ _id: user._id }, { $set: { coins: Math.max(0, initialCoins) } });
    user.coins = Math.max(0, initialCoins);
  }
  return user;
}
async function walletBalance(userId) {
  const user = await ensureWalletUser(userId);
  return Math.max(0, Number(user?.coins ?? 0));
}
async function creditWallet(userId, amount) {
  const value = Number(amount);
  if (!Number.isFinite(value) || value <= 0) throw new Error("Invalid wallet credit");
  await ensureWalletUser(userId);
  const result = await db.collection("users").updateOne(walletUserQuery(userId), {
    $inc: { coins: value }, $set: { walletUpdatedAt: Date.now() }
  });
  if (!result.modifiedCount) throw new Error("Wallet user not found");
}
async function debitWallet(userId, amount) {
  const value = Number(amount);
  if (!Number.isFinite(value) || value <= 0) return false;
  await ensureWalletUser(userId);
  const result = await db.collection("users").updateOne({
    ...walletUserQuery(userId), coins: { $gte: value }
  }, { $inc: { coins: -value }, $set: { walletUpdatedAt: Date.now() } });
  return Boolean(result.modifiedCount);
}
const luckyBags = () => {
  if (!db) throw new Error("GitHub storage is not connected");
  return db.collection("luckyBags");
};
const luckyBagTransactions = () => {
  if (!db) throw new Error("GitHub storage is not connected");
  return db.collection("luckyBagTransactions");
};
async function creditLuckyBagWallet(userId, amount) {
  await creditWallet(userId, amount);
}
async function refundLuckyBag(bagId) {
  const bag = await luckyBags().findOne({ bagId: String(bagId), status: "open", claimedCount: 0 });
  if (!bag) return false;
  const result = await luckyBags().updateOne(
    { bagId: String(bagId), status: "open", claimedCount: 0 },
    { $set: { status: "refunded", refundedAt: Date.now() } }
  );
  if (!result.modifiedCount) return false;
  await creditLuckyBagWallet(bag.senderId, bag.amount);
  await luckyBagTransactions().insertOne({
    userId: bag.senderId, type: "lucky_bag_refund", amount: bag.amount,
    bagId: bag.bagId, createdAt: Date.now()
  });
  io.emit("lucky_bag_refunded", { bagId: bag.bagId, senderId: bag.senderId, amount: bag.amount });
  return true;
}
function scheduleLuckyBagRefund(bag) {
  const wait = Math.max(0, Number(bag.expiresAt) - Date.now());
  const timer = setTimeout(() => {
    refundLuckyBag(bag.bagId).catch((err) => console.error("Lucky Bag refund error:", err.message))
      .finally(() => luckyBagTimers.delete(String(bag.bagId)));
  }, wait);
  if (typeof timer.unref === "function") timer.unref();
  luckyBagTimers.set(String(bag.bagId), timer);
}
app.post("/api/wallet/initialize", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "GitHub storage is not connected" });
    const userId = String(req.body?.userId || "").trim();
    const localBalance = Math.max(0, Math.floor(Number(req.body?.localBalance) || 0));
    if (!userId) return res.status(400).json({ error: "userId required" });
    const users = db.collection("users");
    let user = await users.findOne(walletUserQuery(userId));
    if (!user) {
      const legacy = await db.collection("luckyBagWallets").findOne({ userId });
      const initialCoins = Math.max(localBalance, Number(legacy?.balance || 0));
      await users.updateOne({ uid: userId }, { $setOnInsert: {
        uid: userId, id: userId, appLongId: userId, coins: initialCoins,
        diamonds: 0, createdAt: Date.now(), walletInitializedAt: Date.now()
      } }, { upsert: true });
    } else if (user.coins == null) {
      const legacy = await db.collection("luckyBagWallets").findOne({ userId });
      const initialCoins = Math.max(localBalance, Number(user.balance || 0), Number(legacy?.balance || 0));
      await users.updateOne({ _id: user._id }, { $set: { coins: initialCoins, walletInitializedAt: Date.now() } });
    }
    return res.json({ success: true, balance: await walletBalance(userId) });
  } catch (error) {
    console.error("POST /api/wallet/initialize error:", error.message);
    return res.status(503).json({ error: "Wallet initialization failed" });
  }
});

app.get("/api/store/inventory/:userId", async (req, res) => {
  try {
    const userId = String(req.params.userId || "").trim();
    if (!userId) return res.status(400).json({ error: "userId required" });
    const items = await db.collection("storeInventory").find({ userId }).toArray();
    res.json({ items, balance: await walletBalance(userId) });
  } catch (error) {
    console.error("GET /api/store/inventory error:", error.message);
    res.status(503).json({ error: "Store inventory unavailable" });
  }
});
app.post("/api/store/purchase", async (req, res) => {
  try {
    const userId = String(req.body?.userId || "").trim();
    const itemId = String(req.body?.itemId || "").trim();
    const name = String(req.body?.name || "").slice(0, 100);
    const tab = String(req.body?.tab || "").trim();
    const image = String(req.body?.image || "").slice(0, 300);
    const video = String(req.body?.video || "").slice(0, 300);
    const duration = String(req.body?.duration || "").trim().toUpperCase();
    const price = Number(req.body?.price);
    const prices = {v1:1000000,v10:1200000,v2:600000,v3:549999,v4:500000,v5:259000,v6:890999,v7:425000,v8:525000,v9:359999,a1:250000,a2:500000,a3:750000,a4:700000,a5:249000,a6:300000,a7:199000,a8:500000,t1:2700000,t2:2400000,t3:2700000,t4:2400000,t5:2700000,t6:2400000,t7:899999,t8:700000,t9:1000000,c1:500000,c2:250000,c3:300000,c4:246000,c5:159000,c6:200000,i1:10000000};
    const expectedTab = itemId.startsWith("v") ? "Vehicle" : itemId.startsWith("a") ? "Avatar Frame" : itemId.startsWith("t") ? "Theme" : itemId.startsWith("c") ? "Chat Bubble" : itemId === "i1" ? "ID" : "";
    const expectedDuration = itemId.startsWith("v") ? "5D" : itemId.startsWith("a") || itemId.startsWith("c") && itemId !== "c2" || itemId === "i1" ? "3D" : itemId === "c2" ? "2D" : itemId.startsWith("t") ? "30D" : "";
    if (!userId || !prices[itemId] || prices[itemId] !== price || tab !== expectedTab || duration !== expectedDuration || !name || !image) return res.status(400).json({ error: "Invalid store item" });
    const now = Date.now();
    const inventory = db.collection("storeInventory");
    const existing = await inventory.findOne({ userId, itemId, expiresAt: { $gt: now } });
    if (existing) return res.status(409).json({ error: "Item already owned", balance: await walletBalance(userId), item: existing });
    const debit = await debitWallet(userId, price);
    if (!debit) return res.status(409).json({ error: "Insufficient coins", balance: await walletBalance(userId) });
    const item = { userId, itemId, name, tab, image, video, duration, price, purchasedAt: now, expiresAt: now + Number(duration.slice(0, -1)) * 86400000, equipped: false };
    try {
      await inventory.updateOne({ userId, itemId }, { $set: item }, { upsert: true });
      await db.collection("walletTransactions").insertOne({ userId, type: "store_purchase", amount: -price, itemId, createdAt: now });
    } catch (error) {
      await creditLuckyBagWallet(userId, price);
      throw error;
    }
    res.json({ success: true, item, balance: await walletBalance(userId) });
  } catch (error) {
    console.error("POST /api/store/purchase error:", error.message);
    res.status(503).json({ error: "Store purchase failed" });
  }
});
app.post("/api/store/equip", async (req, res) => {
  try {
    const userId = String(req.body?.userId || "").trim();
    const itemId = String(req.body?.itemId || "").trim();
    const equipped = req.body?.equipped === true;
    if (!userId || !itemId) return res.status(400).json({ error: "userId and itemId required" });
    const inventory = db.collection("storeInventory");
    const item = await inventory.findOne({ userId, itemId, expiresAt: { $gt: Date.now() } });
    if (!item) return res.status(404).json({ error: "Owned item not found or expired" });
    await inventory.updateMany({ userId, tab: item.tab }, { $set: { equipped: false } });
    await inventory.updateOne({ userId, itemId }, { $set: { equipped } });
    res.json({ success: true, equipped });
  } catch (error) {
    console.error("POST /api/store/equip error:", error.message);
    res.status(503).json({ error: "Could not update equipped item" });
  }
});

app.get("/api/lucky-bag/wallet/:userId", async (req, res) => {
  try {
    const userId = String(req.params.userId || "").trim();
    if (!userId) return res.status(400).json({ error: "userId required" });
    // Return the Mine > Wallet page's canonical GitHub storage balance.
    const balance = await walletBalance(userId);
    res.json({ userId, balance });
  } catch (err) {
    res.status(503).json({ error: "Lucky Bag wallet unavailable" });
  }
});
app.post("/api/lucky-bag/send", async (req, res) => {
  try {
    const senderId = String(req.body?.senderId || "").trim();
    const senderName = String(req.body?.senderName || "User").trim().slice(0, 80);
    const senderImage = String(req.body?.senderImage || "").slice(0, 1000);
    const roomId = String(req.body?.roomId || "").trim();
    const roomName = String(req.body?.roomName || "Room").trim().slice(0, 100);
    const amount = Number(req.body?.amount);
    const recipients = Number(req.body?.recipients);
    const bagId = String(req.body?.bagId || require("crypto").randomUUID());
    if (!senderId || !roomId || !Number.isSafeInteger(amount) || amount <= 0 ||
        ![10000,100000,200000,500000,1000000,1500000,2000000].includes(amount) ||
        ![5,10,30,50].includes(recipients)) {
      return res.status(400).json({ error: "Invalid Lucky Bag details" });
    }
    // Debit the same GitHub storage users.coins balance used by Mine > Wallet.
    const debit = await debitWallet(senderId, amount);
    if (!debit) return res.status(409).json({ error: "Insufficient coins", balance: await walletBalance(senderId) });
    const now = Date.now();
    const bag = { bagId, senderId, senderName, senderImage, roomId, roomName, amount, recipients, claimedCount: 0, claimedTotal: 0, status: "open", createdAt: now, expiresAt: now + 5 * 60 * 1000 };
    try {
      await luckyBags().insertOne(bag);
      await luckyBagTransactions().insertOne({ userId: senderId, type: "lucky_bag_send", amount: -amount, bagId, createdAt: now });
    } catch (err) {
      await creditLuckyBagWallet(senderId, amount);
      throw err;
    }
    io.emit("lucky_bag_banner", { bagId, senderId, senderName, senderImage, roomId, roomName, amount, recipients, expiresAt: bag.expiresAt });
    scheduleLuckyBagRefund(bag);
    res.json({ success: true, bagId, balance: await walletBalance(senderId), expiresAt: bag.expiresAt });
  } catch (err) {
    console.error("POST /api/lucky-bag/send error:", err.message);
    res.status(503).json({ error: "Could not send Lucky Bag" });
  }
});
app.post("/api/lucky-bag/claim", async (req, res) => {
  try {
    const bagId = String(req.body?.bagId || "").trim();
    const userId = String(req.body?.userId || "").trim();
    if (!bagId || !userId) return res.status(400).json({ error: "bagId and userId required" });
    const bag = await luckyBags().findOne({ bagId });
    if (!bag) return res.status(404).json({ error: "Lucky Bag not found" });
    if (bag.senderId === userId) return res.status(403).json({ error: "Sender cannot claim own Lucky Bag" });
    if (bag.status !== "open" || Date.now() >= bag.expiresAt) {
      if (bag.status === "open" && bag.claimedCount === 0) await refundLuckyBag(bagId);
      return res.status(410).json({ error: "Lucky Bag expired or closed" });
    }
    const claimKey = { bagId, userId };
    const existing = await luckyBags().findOne({ bagId, claims: { $elemMatch: { userId } } }, { projection: { claims: 1 } });
    if (existing) return res.status(409).json({ error: "Already claimed this Lucky Bag" });
    const current = await luckyBags().findOne({ bagId, status: "open" });
    if (!current || current.claimedCount >= current.recipients) return res.status(409).json({ error: "Lucky Bag is empty" });
    const slotsLeft = current.recipients - current.claimedCount;
    const coinsLeft = current.amount - current.claimedTotal;
    const share = slotsLeft <= 1 ? coinsLeft : Math.max(1, Math.floor(Math.random() * Math.max(2, Math.floor(coinsLeft / slotsLeft) * 2)));
    const amount = Math.min(coinsLeft, share);
    const claimUpdate = await luckyBags().updateOne(
      { bagId, status: "open", claimedCount: current.claimedCount, claimedTotal: current.claimedTotal, claims: { $not: { $elemMatch: { userId } } } },
      { $inc: { claimedCount: 1, claimedTotal: amount }, $push: { claims: { userId, amount, claimedAt: Date.now() } } }
    );
    if (!claimUpdate.modifiedCount) return res.status(409).json({ error: "Another claim was processed; try again" });
    await creditLuckyBagWallet(userId, amount);
    await luckyBagTransactions().insertOne({ userId, type: "lucky_bag_claim", amount, bagId, createdAt: Date.now() });
    const after = await luckyBags().findOne({ bagId });
    if (after && after.claimedCount >= after.recipients) {
      await luckyBags().updateOne({ bagId, status: "open" }, { $set: { status: "claimed", completedAt: Date.now() } });
      const timer = luckyBagTimers.get(bagId);
      if (timer) clearTimeout(timer);
      luckyBagTimers.delete(bagId);
    }
    io.emit("lucky_bag_claimed", { bagId, userId, amount, claimedCount: after?.claimedCount || 1, recipients: current.recipients });
    const balance = await walletBalance(userId);
    res.json({ success: true, amount, balance, claimedCount: after?.claimedCount || 1, recipients: current.recipients });
  } catch (err) {
    console.error("POST /api/lucky-bag/claim error:", err.message);
    res.status(503).json({ error: "Could not claim Lucky Bag" });
  }
});
app.get("/api/lucky-bag/room/:roomId", async (req, res) => {
  try {
    const roomId = String(req.params.roomId || "");
    const bag = await luckyBags().findOne(
      { roomId, status: "open", expiresAt: { $gt: Date.now() } },
      { sort: { createdAt: -1 } }
    );
    if (!bag) return res.json({ bag: null });
    res.json({ bag: { bagId: bag.bagId, senderId: bag.senderId, senderName: bag.senderName, senderImage: bag.senderImage, roomId: bag.roomId, roomName: bag.roomName, amount: bag.amount, recipients: bag.recipients, claimedCount: bag.claimedCount, expiresAt: bag.expiresAt } });
  } catch (err) {
    res.status(503).json({ error: "Lucky Bag unavailable" });
  }
});
app.get("/api/lucky-bag/:bagId", async (req, res) => {
  try {
    const bag = await luckyBags().findOne({ bagId: String(req.params.bagId) });
    if (!bag) return res.status(404).json({ error: "Lucky Bag not found" });
    res.json({ bagId: bag.bagId, senderId: bag.senderId, senderName: bag.senderName, senderImage: bag.senderImage, roomId: bag.roomId, roomName: bag.roomName, amount: bag.amount, recipients: bag.recipients, claimedCount: bag.claimedCount, claimedTotal: bag.claimedTotal, status: bag.status, expiresAt: bag.expiresAt, claims: bag.claims || [] });
  } catch (err) {
    res.status(503).json({ error: "Lucky Bag unavailable" });
  }
});

app.get("/health/redis", async (_req, res) => {
  const health = await checkRedisConnection();
  return res.status(health.connected ? 200 : 503).json({
    service: "redis",
    configured: health.configured,
    connected: health.connected,
  });
});

app.get("/", (_req, res) => {
  res.json({
    status: "ok",
    service: "Hurry Backend",
    socket: "ready",
    database: db
      ? "GitHub storage-connected"
      : "GitHub storage-connecting",
    onlineUsers: onlineUsers.size,
    activeRooms: roomUsers.size,
  });
});

io.on("connection", (socket) => {
  console.log("User connected:", socket.id);

  socket.on("register", (payload) => {
    const userId =
      typeof payload === "object"
        ? payload.userId || payload.uid || payload.id
        : payload;

    const accountId =
      typeof payload === "object"
        ? payload.accountId
        : null;

    if (!userId) return;

    markUserOnline(
      String(userId),
      socket,
      accountId ? String(accountId) : null
    );

    socket.emit("global_room_presence", {
      rooms: getGlobalRoomPresence(),
    });
  });

  const handleGameWinner = async (gameName, payload = {}) => {
    const round = Number(payload.round || 0);
    if (!round) return;

    const win = Number(payload.win || 0);
    if (!Number.isFinite(win) || win <= 0) return;

    const name = String(
      payload.name ||
      payload.userName ||
      payload.displayName ||
      payload.username ||
      "User"
    ).trim() || "User";

    const avatar = String(
      payload.avatar ||
      payload.photo ||
      payload.image ||
      payload.photoUrl ||
      payload.photoURL ||
      payload.profilePhoto ||
      "/default-avatar.png"
    ).trim() || "/default-avatar.png";

    const currentData = memoryRoundWinners[gameName];
    if (currentData.round !== round) {
      currentData.round = round;
      currentData.winners = [];
    }

    const existing = currentData.winners.find(
      (winner) => winner.name === name && winner.avatar === avatar
    );
    if (existing) {
      existing.win = Math.max(Number(existing.win) || 0, win);
    } else {
      currentData.winners.push({ name, win, avatar });
    }

    currentData.winners.sort((a, b) => b.win - a.win);
    currentData.winners = currentData.winners.slice(0, 3);

    io.emit(`${gameName}_top_winners`, currentData.winners);
  };

  socket.on("fruitparty_winner_update", (payload) => {
    handleGameWinner("fruitparty", payload);
  });

  socket.on("wildparty_winner_update", (payload) => {
    handleGameWinner("wildparty", payload);
  });

  socket.on("presence_online", (userId) => {
    if (!userId) return;

    markUserOnline(String(userId), socket);
  });

  socket.on("check_presence", (userId) => {
    if (!userId) return;

    const id = String(userId);

    socket.emit("presence_status", {
      userId: id,
      online: onlineUsers.has(id),
    });
  });

  socket.on(
    "room_presence_request",
    ({ roomId } = {}) => {
      if (!roomId) return;

      const room = String(roomId);
      const users = getRoomUsers(room);

      socket.emit("room_presence", {
        roomId: room,
        users,
        activeUserCount: users.length,
      });
    }
  );

  socket.on(
    "global_room_presence_request",
    async () => {
      const baseRooms = getGlobalRoomPresence();
      let finalRooms = baseRooms;

      if (db && baseRooms.length > 0) {
        try {
          const roomIds = baseRooms.map((r) => r.roomId);
          const roomDataList = await db.collection("rooms").find({
            $or: [
              { accountId: { $in: roomIds } },
              { id: { $in: roomIds } },
              { roomId: { $in: roomIds } }
            ]
          }).toArray();

          const roomMap = new Map();
          for (const rd of roomDataList) {
            if (rd.accountId) roomMap.set(String(rd.accountId), rd);
            if (rd.id) roomMap.set(String(rd.id), rd);
            if (rd.roomId) roomMap.set(String(rd.roomId), rd);
          }

          finalRooms = baseRooms.map((r) => {
            const match = roomMap.get(r.roomId);
            if (match) {
              return {
                ...r,
                roomName: match.name || match.roomName || match["Room Name"] || "Room",
                roomDp: match.image || match.dp || match.roomDp || match["Room dp"] || "/default-avatar.png",
              };
            }
            return r;
          });
        } catch (err) {
          console.error("Error fetching room data in bulk for global presence:", err);
        }
      }

      socket.emit("global_room_presence", {
        rooms: finalRooms,
      });
    }
  );

  socket.on(
    "room_join",
    async ({ roomId, userId, accountId, name, dp, email, roomOwnerId } = {}) => {
      if (!roomId || (!userId && !accountId)) return;

      const room = String(roomId);
      const accId = accountId ? String(accountId) : String(userId);
      const requestedOwnerId = String(roomOwnerId || "");

      // Do not allow a client to impersonate a different account identity.
      if (socket.userId || socket.accountId || socket.roomUserId || socket.roomAccountId) {
        if (!socketOwnsIdentity(socket, accId) && !socketOwnsIdentity(socket, userId)) return;
      }

      cancelPendingSeatDisconnect(room, accId);
      if (userId) cancelPendingSeatDisconnect(room, String(userId));

      // Prevent duplicate joins from increasing the live count.
      if (
        socket.roomId === room &&
        socket.roomAccountId === accId
      ) {
        const users = getRoomUsers(room);

        socket.emit("room_presence", {
          roomId: room,
          users,
          activeUserCount: users.length,
        });

        socket.emit("room_seats", {
          roomId: room,
          seats: getRoomSeats(room),
        });
      } else {
        if (socket.roomId) {
          const oldRoom = String(socket.roomId);
          const oldUser =
            socket.roomAccountId ||
            socket.accountId ||
            socket.roomUserId ||
            socket.userId;

          if (oldRoom !== room) {
            socket.leave(`room:${oldRoom}`);

            clearUserSeatGracefully(oldRoom, oldUser, 0);

            removeUserFromRoom(
              oldRoom,
              oldUser
            );

            socket
              .to(`room:${oldRoom}`)
              .emit("room_user_offline", {
                roomId: oldRoom,
                userId: String(oldUser || ""),
              });

            emitRoomPresence(oldRoom);
          }
        }

        socket.join(`room:${room}`);

        socket.roomId = room;
        if (userId) socket.roomUserId = String(userId);
        socket.accountId = accId;
        socket.roomAccountId = accId;

        addUserToRoom(room, accId, {
          name: name || "User",
          image:
            dp ||
            "/default-avatar.png",
          email: email || "",
          accountId: accId,
        });

        const users = getRoomUsers(room);

        socket.emit("room_presence", {
          roomId: room,
          users,
          activeUserCount: users.length,
        });

        socket.emit("room_seats", {
          roomId: room,
          seats: getRoomSeats(room),
        });

        socket
          .to(`room:${room}`)
          .emit("room_user_online", {
            roomId: room,
            userId: accId,
            user: {
              accountId: accId,
              userId: userId ? String(userId) : accId,
              name: name || "User",
              image:
                dp ||
                "/default-avatar.png",
              email: email || "",
            },
          });

        emitRoomPresence(room);
        emitGlobalRoomPresence();
      }

      // ASYNC LOCK CHECKS AFTER SYNCHRONOUS JOIN
      const cachedLock = lockedRooms.get(room);

      if (cachedLock?.locked && String(cachedLock.ownerId || "") !== accId) {
        socket.emit("room_locked", { roomId: room, ownerId: String(cachedLock.ownerId || "") });

        // Revert join side effects
        socket.leave(`room:${room}`);
        clearUserSeatGracefully(room, accId, 0);
        removeUserFromRoom(room, accId);
        socket.to(`room:${room}`).emit("room_user_offline", { roomId: room, userId: accId });
        emitRoomPresence(room);
        emitGlobalRoomPresence();
        socket.roomId = null;
        socket.roomAccountId = null;
        socket.roomUserId = null;
        return;
      }

      if (db) {
        try {
          const roomDoc = await db.collection("rooms").findOne({
            $or: [{ accountId: room }, { roomId: room }, { id: room }]
          });
          if (roomDoc?.isLocked && String(roomDoc.accountId || roomDoc.roomId || roomDoc.id || requestedOwnerId) !== accId) {
            const owner = String(roomDoc.accountId || roomDoc.roomId || roomDoc.id || requestedOwnerId || "");
            lockedRooms.set(room, { locked: true, ownerId: owner, updatedAt: Date.now() });
            socket.emit("room_locked", { roomId: room, ownerId: owner });

            // Revert join side effects
            socket.leave(`room:${room}`);
            clearUserSeatGracefully(room, accId, 0);
            removeUserFromRoom(room, accId);
            socket.to(`room:${room}`).emit("room_user_offline", { roomId: room, userId: accId });
            emitRoomPresence(room);
            emitGlobalRoomPresence();
            socket.roomId = null;
            socket.roomAccountId = null;
            socket.roomUserId = null;
            return;
          }
        } catch (error) {
          console.error("Room lock check failed:", error.message);
        }
      }
    }
  );

  socket.on("coin_transfer", async ({ roomId, senderId, recipientIds, amount, giftName, giftImage, giftType, transferId, luckyGift, senderName, senderImage, recipientName, recipientImage, multiplier } = {}) => {
    const room = String(roomId || "");
    const sender = String(senderId || "");
    const value = Number(amount);
    const eventId = String(transferId || "").trim();
    if (eventId) {
      const seenAt = recentTransferIds.get(eventId);
      if (seenAt && Date.now() - seenAt < 30000) return;
      recentTransferIds.set(eventId, Date.now());
      if (recentTransferIds.size > 5000) {
        for (const [id, at] of recentTransferIds) {
          if (Date.now() - at >= 30000) recentTransferIds.delete(id);
        }
      }
    }

    const recipients = Array.isArray(recipientIds)
      ? [...new Set(recipientIds.map((id) => String(id)).filter(Boolean))]
      : [];

    if (!room || !sender || !Number.isFinite(value) || value <= 0 || value > 1000000000) {
      return;
    }

    // Sender must actually be inside this room and own the sending identity.
    // Accept any identity registered on the socket (accountId/userId/room identities)
    // so the wallet/room account identifier cannot block a valid gift transfer.
    if (
      String(socket.roomId || "") !== room ||
      !socketOwnsIdentity(socket, sender)
    ) {
      return;
    }

    const roomMap = roomUsers.get(room);
    if (!roomMap || recipients.length === 0) return;

    // Only users currently present in this room can receive the diamonds.
    // GiftPicker sends accountId, while roomUsers is keyed by userId.
    // Accept either identity and always emit the canonical accountId so the
    // selected user (including the sender/self) receives the diamonds.
    const validRecipients = recipients
      .map((id) => {
        const direct = roomMap.get(id);
        if (direct) return String(direct.accountId || direct.userId || id);

        for (const [userId, user] of roomMap.entries()) {
          if (
            String(userId) === id ||
            String(user?.userId || "") === id ||
            String(user?.accountId || "") === id
          ) {
            return String(user?.accountId || user?.userId || userId);
          }
        }
        return null;
      })
      .filter(Boolean)
      .filter((id, index, arr) => arr.indexOf(id) === index);

    if (validRecipients.length === 0) return;

    const isLucky = luckyGift === true || String(giftType || "") === "Lucky";
    const diamondAmount =
      Number.isFinite(value) && value > 0
        ? Math.floor(value)
        : 0;

    if (diamondAmount <= 0) return;

    let senderUser = null;
    for (const [userId, user] of roomMap.entries()) {
      if (
        String(userId) === sender ||
        String(user?.userId || "") === sender ||
        String(user?.accountId || "") === sender
      ) {
        senderUser = user;
        break;
      }
    }

    let firstRecipient = null;
    const firstRecipientId = validRecipients[0];
    for (const [userId, user] of roomMap.entries()) {
      if (
        String(userId) === String(firstRecipientId) ||
        String(user?.userId || "") === String(firstRecipientId) ||
        String(user?.accountId || "") === String(firstRecipientId)
      ) {
        firstRecipient = user;
        break;
      }
    }

    // Lucky return is a transparent random 0–50% return of the sent coin value.
    // The exact percentage is included in the room event so every client sees
    // the same result; it is not hidden from the user.
    const luckyReturnPercent = isLucky
      ? Math.floor(Math.random() * 51)
      : 0;
    const luckyReturnAmount = isLucky
      ? Math.floor(value * (luckyReturnPercent / 100))
      : 0;

    const transferPayload = {
      roomId: room,
      senderId: sender,
      recipientIds: validRecipients,
      amount: value,
      diamondAmount,
      giftName: String(giftName || "Gift"),
      giftImage: String(giftImage || ""),
      giftType: String(giftType || ""),
      luckyGift: isLucky,
      senderName: String(senderName || senderUser?.name || "User"),
      senderImage: String(senderImage || senderUser?.image || "/default-avatar.png"),
      recipientName: String(recipientName || firstRecipient?.name || "User"),
      recipientImage: String(recipientImage || firstRecipient?.image || "/default-avatar.png"),
      multiplier: Math.max(1, Number(multiplier) || 1),
      luckyReturnPercent,
      luckyReturnAmount,
      transferId: eventId || undefined,
      timestamp: Date.now(),
    };

    // Persist the gift against the same GitHub storage wallet used by Mine > Wallet.
    // Never broadcast a successful gift if the sender could not pay.
    try {
      if (!db) throw new Error("GitHub storage wallet unavailable");
      const paid = await debitWallet(sender, Math.floor(value));
      if (!paid) {
        socket.emit("coin_transfer_failed", { transferId: eventId || undefined, reason: "insufficient_coins" });
        return;
      }
      const diamondCredit = Math.max(0, Math.floor(diamondAmount));
      for (const recipientId of validRecipients) {
        await ensureWalletUser(recipientId);
        await db.collection("users").updateOne(walletUserQuery(recipientId), {
          $inc: { diamonds: diamondCredit },
          $set: { walletUpdatedAt: Date.now() }
        });
      }
      if (luckyReturnAmount > 0) await creditWallet(sender, luckyReturnAmount);
      io.to("room:" + room).emit("coin_transfer_received", transferPayload);
      socket.emit("wallet_balance_updated", {
        userId: sender, balance: await walletBalance(sender), transferId: eventId || undefined
      });
    } catch (error) {
      console.error("coin_transfer wallet persistence failed:", error.message);
      socket.emit("coin_transfer_failed", { transferId: eventId || undefined, reason: "wallet_unavailable" });
    }
  });

  socket.on(
    "room_leave",
    ({ roomId, userId, accountId } = {}) => {
      const room = roomId
        ? String(roomId)
        : socket.roomId;

      if (!room) return;

      // A client cannot remove another user's room presence.
      const requestedId = String(accountId || userId || "").trim();
      if (requestedId && !socketOwnsIdentity(socket, requestedId)) return;

      const id = String(
        accountId ||
        userId ||
        socket.roomAccountId ||
        socket.accountId ||
        socket.roomUserId ||
        socket.userId ||
        ""
      );

      socket.leave(`room:${room}`);

      clearUserSeatGracefully(room, id, 0);

      removeUserFromRoom(room, id);

      socket
        .to(`room:${room}`)
        .emit("room_user_offline", {
          roomId: room,
          userId: id,
        });

      emitRoomPresence(room);

      if (socket.roomId === room) {
        socket.roomId = null;
        socket.roomUserId = null;
        socket.roomAccountId = null;
      }

      emitGlobalRoomPresence();
    }
  );

  socket.on("room_seats_request", ({ roomId } = {}) => {
    if (!roomId) return;

    const room = String(roomId);

    // Only return seats for a room this socket has actually joined.
    if (String(socket.roomId || "") !== room) {
      return;
    }

    socket.emit("room_seats", {
      roomId: room,
      seats: getRoomSeats(room),
    });
  });

  socket.on("room_seat_action", (data = {}) => {
    const roomId = data.roomId ? String(data.roomId) : "";
    const userId = data.userId ? String(data.userId) : "";
    const action = data.action;
    const seatNumber = Number(data.seatNumber);

    if (
      !roomId ||
      !userId ||
      !Number.isFinite(seatNumber)
    ) {
      return;
    }

    // Only a user who is actually joined to this room can control a seat.
    const isRoomUser =
      String(socket.roomAccountId || "") === userId ||
      String(socket.accountId || "") === userId ||
      String(socket.roomUserId || "") === userId ||
      String(socket.userId || "") === userId;

    if (!isValidRoomActionSocket(socket, roomId, userId) || !isRoomUser) {
      return;
    }

    if (!roomSeats.has(roomId)) {
      roomSeats.set(roomId, new Map());
    }

    const seats = roomSeats.get(roomId);

    const current = seats.get(seatNumber) || {
      number: seatNumber,
      isOccupied: false,
      isLocked: false,
      isMuted: false,
      isSpeaking: false,
    };

    if (action === "take") {
      // One user can occupy only one seat.
      for (const [number, seat] of seats.entries()) {
        if (
          seat?.isOccupied &&
          String(seat?.user?.accountId) === userId &&
          Number(number) !== seatNumber
        ) {
          seats.set(number, {
            ...seat,
            isOccupied: false,
            user: undefined,
            isMuted: false,
            isSpeaking: false,
            gif: undefined,
          });
        }
      }

      if (
        current.isLocked &&
        !current.isOccupied
      ) {
        emitRoomSeats(roomId);
        return;
      }

      if (
        current.isOccupied &&
        String(current.user?.accountId) !== userId
      ) {
        emitRoomSeats(roomId);
        return;
      }

      seats.set(seatNumber, {
        ...current,
        number: seatNumber,
        isOccupied: true,
        user: {
          name: data.user?.name || "User",
          image:
            data.user?.image ||
            "/default-avatar.png",
          accountId: userId,
        },
        isMuted: false,
        isSpeaking: false,
        gif: undefined,
        socketId: String(socket.id),
      });
    }

    if (action === "leave") {
      // Only the socket currently occupying this seat may release it.
      if (
        current.isOccupied &&
        (!current.socketId || String(current.socketId) === String(socket.id)) &&
        String(current.user?.accountId || "") === userId
      ) {
        seats.set(seatNumber, {
          ...current,
          isOccupied: false,
          user: undefined,
          isMuted: false,
          isSpeaking: false,
          gif: undefined,
        });
      }
    }

    if (action === "mute") {
      if (current.isOccupied) {
        seats.set(seatNumber, {
          ...current,
          isMuted: Boolean(data.isMuted),
        });
      }
    }

    if (action === "lock") {
      seats.set(seatNumber, {
        ...current,
        isLocked: Boolean(data.isLocked),
      });
    }

    if (action === "emoji") {
      if (current.isOccupied) {
        seats.set(seatNumber, {
          ...current,
          gif: {
            src: data.src,
            timestamp: Number(
              data.timestamp || Date.now()
            ),
          },
        });

        // Clear the emoji automatically after 5 seconds to sync across all clients
        setTimeout(() => {
          const currentSeats = roomSeats.get(roomId);
          if (currentSeats) {
            const seat = currentSeats.get(seatNumber);
            if (seat && seat.gif) {
              currentSeats.set(seatNumber, {
                ...seat,
                gif: undefined,
              });
              emitRoomSeats(roomId);
            }
          }
        }, 5000);
      }
    }

    // Lucky Gift fly is a transient visual event, so relay it to the other room clients.
    // The sender also plays it locally from GiftPicker.tsx.
    if (action === "lucky_image") {
      // Broadcast to every room member, including the sender. The client-side
      // animation is mounted at room level, so GiftPicker does not need a
      // global listener or a local duplicate animation.
      io.to(`room:${roomId}`).emit("room_seat_action", data);
    }

    // IMPORTANT:
    // Broadcast the complete seat state to EVERYONE in the room.
    emitRoomSeats(roomId);
  });

  socket.on("room_settings_update", (data = {}) => {
    if (!data || !data.roomId) return;
    const roomId = String(data.roomId);
    if (!socketIsInRoom(socket, roomId)) return;
    if (data.userId && !socketOwnsIdentity(socket, data.userId)) return;

    const locked = Boolean(data.isLocked);
    const ownerId = String(data.roomOwnerId || data.userId || socket.roomAccountId || socket.accountId || "");
    lockedRooms.set(roomId, { locked, ownerId, updatedAt: Date.now() });

    io.to(`room:${roomId}`).emit("room_settings_updated", data);

    if (locked) {
      for (const client of io.sockets.sockets.values()) {
        if (String(client.roomId || "") !== roomId) continue;
        const clientId = String(client.roomAccountId || client.accountId || client.roomUserId || client.userId || "");
        if (clientId === ownerId) continue;
        client.emit("room_locked", { roomId, ownerId });
        client.leave(`room:${roomId}`);
        client.roomId = null;
        removeUserFromRoom(roomId, clientId);
      }
      emitRoomPresence(roomId);
      emitGlobalRoomPresence();
    }
  });
  socket.on("gift_video_play", (data = {}) => {
    if (!data?.roomId || !data?.video) return;
    const roomId = String(data.roomId);
    if (!socketIsInRoom(socket, roomId)) return;
    if (data.senderId && !socketOwnsIdentity(socket, data.senderId)) return;
    io.to(`room:${roomId}`).emit("gift_video_play", {
      roomId, senderId: String(data.senderId || ""), giftName: data.giftName || "",
      video: String(data.video), videoStyle: data.videoStyle || "fade", timestamp: Number(data.timestamp || Date.now())
    });
  });

  socket.on("room_message", (message) => {
    if (!message?.roomId || !message?.senderId) return;
    const roomId = String(message.roomId);
    if (!socketIsInRoom(socket, roomId)) return;
    if (!socketOwnsIdentity(socket, message.senderId)) return;
    io.to(`room:${roomId}`).emit("room_message", message);
  });

  socket.on("room_clear_chat", (data = {}) => {
    const roomId = data?.roomId ? String(data.roomId) : "";
    if (!roomId || !socketIsInRoom(socket, roomId)) return;
    if (data.userId && !socketOwnsIdentity(socket, data.userId)) return;
    io.to(`room:${roomId}`).emit("room_chat_cleared", {
      roomId,
      timestamp: Date.now(),
    });
  });

  socket.on("private_message", async (message) => {
    if (!message?.senderId || !message?.receiverId) return;
    if (!socketOwnsIdentity(socket, message.senderId)) return;

    const receiverId = String(message.receiverId);

    // Check if the receiver has blocked the sender
    if (db) {
      const blockRecord = await db.collection("blocks").findOne({
        blockerId: receiverId,
        blockedId: String(message.senderId)
      });
      if (blockRecord) {
        // Drop the message silently if blocked
        return;
      }
    }

    const receiverAccountId = message.receiverAccountId ? String(message.receiverAccountId) : null;

    const normalizedMessage = {
      id: String(message.id || `${message.senderId}_${Date.now()}`),
      senderId: String(message.senderId),
      receiverId,
      receiverAccountId,
      senderName: message.senderName || message.otherUserName || "User",
      senderPhoto: message.senderPhoto || message.otherUserPhoto || "/default-avatar.png",
      receiverName: message.receiverName || "User",
      receiverPhoto: message.receiverPhoto || "/default-avatar.png",
      text: String(message.text || ""),
      type: message.type || "message",
      imageUrl: message.imageUrl || undefined,
      roomData: message.roomData || undefined,
      replyTo: message.replyTo || null,
      timestamp: Number(message.timestamp || Date.now()),
    };

    // Relay only; message history is persisted locally on each native client in index.db.
    // Do not save private chat contents in GitHub storage.
    io.to(`user:${receiverId}`).emit(
      "private_message",
      normalizedMessage
    );
    // Live-only event updates the Owner panel private-message monitor.
    io.emit("private_message_monitor", normalizedMessage);

    if (receiverAccountId && receiverAccountId !== receiverId) {
      io.to(`user:${receiverAccountId}`).emit(
        "private_message",
        normalizedMessage
      );
    }
  });

  socket.on("private_message_history_request", (data = {}) => {
    // No GitHub storage history: clients use their local index.db cache.
    socket.emit("private_message_history", {
      chatId: data.chatId || "",
      messages: [],
    });
  });

  socket.on("private_message_clear", (data = {}) => {
    const userId = String(data.userId || "");
    const otherUserId = String(data.otherUserId || "");
    if (!userId || !otherUserId || !socketOwnsIdentity(socket, userId)) return;

    // Local-only chat clearing: notify both clients; never touch GitHub storage.
    io.to(`user:${userId}`).emit("private_message_cleared", { chatId: data.chatId || "" });
    io.to(`user:${otherUserId}`).emit("private_message_cleared", { chatId: data.chatId || "" });
  });

  socket.on("private_message_delete", (data = {}) => {
    const userId = String(data.userId || "");
    const otherUserId = String(data.otherUserId || "");
    const messageId = String(data.messageId || "");
    if (!userId || !otherUserId || !messageId || !socketOwnsIdentity(socket, userId)) return;

    const payload = { chatId: data.chatId || "", messageId };
    io.to(`user:${userId}`).emit("private_message_deleted", payload);
    io.to(`user:${otherUserId}`).emit("private_message_deleted", payload);
  });

  socket.on("private_message_delete_many", (data = {}) => {
    const userId = String(data.userId || "");
    const otherUserId = String(data.otherUserId || "");
    const messageIds = Array.isArray(data.messageIds) ? data.messageIds.map(String).filter(Boolean) : [];
    if (!userId || !otherUserId || !messageIds.length || !socketOwnsIdentity(socket, userId)) return;

    const payload = { chatId: data.chatId || "", messageIds };
    io.to(`user:${userId}`).emit("private_messages_deleted", payload);
    io.to(`user:${otherUserId}`).emit("private_messages_deleted", payload);
  });

  socket.on("ai_support_message", async (data = {}) => {
    const userId = String(data.userId || "");
    if (!userId) return;

    const payload = {
      userId,
      userName: data.userName || "User",
      userEmail: data.userEmail || "",
      userPhoto: data.userPhoto || data.userImage || "",
      userAccountId: data.userAccountId || "",
      messages: Array.isArray(data.messages) ? data.messages : [],
      lastMessage: data.lastMessage || data.message || null,
      timestamp: Number(data.timestamp || Date.now()),
    };

    // Real-time only: chat history belongs to the clients' local index.db / Owner IndexedDB.
    // Do not persist Daisy support messages in GitHub storage.
    io.emit("ai_support_message", payload);
  });

  socket.on("ai_support_history_request", async () => {
    // No server-side chat history; the Owner panel restores its own browser IndexedDB cache.
    socket.emit("ai_support_history_response", { chats: [] });
  });


  socket.on("user_report", async (data = {}) => {
    const reportId = String(data.id || `report_${Date.now()}`);

    const payload = {
      id: reportId,
      senderId: String(data.senderId || ""),
      senderName: String(data.senderName || "User"),
      senderPhoto: String(data.senderPhoto || ""),
      reportedId: String(data.reportedId || ""),
      reportedName: String(data.reportedName || "User"),
      reportedPhoto: String(data.reportedPhoto || ""),
      category: String(data.category || ""),
      description: String(data.description || ""),
      proofImage: data.proofImage ? String(data.proofImage) : null,
      timestamp: Number(data.timestamp || Date.now()),
    };

    // Live-only relay. Reports are kept in the submitting device's local database
    // and the Owner panel's browser IndexedDB; never write report content to GitHub storage.
    io.emit("user_report", payload);
  });

  socket.on("user_report_history_request", () => {
    // No server-side report history and no GitHub storage read.
    socket.emit("user_report_history_response", { reports: [] });
  });

  socket.on("user_feedback", async (data = {}) => {
    const feedbackId = String(data.id || `fb_${Date.now()}`);

    const payload = {
      id: feedbackId,
      userId: String(data.userId || ""),
      userName: String(data.userName || "User"),
      userAccountId: String(data.userAccountId || ""),
      userPhoto: String(data.userPhoto || ""),
      type: String(data.type || ""),
      typeLabel: String(data.typeLabel || "Feedback"),
      description: String(data.description || ""),
      contactInfo: String(data.contactInfo || ""),
      createdAt: data.createdAt || new Date().toISOString(),
      timestamp: Number(data.timestamp || Date.now()),
      status: String(data.status || "pending")
    };

    // Live-only relay. Feedback is stored locally by clients/Owner IndexedDB;
    // never write feedback content to GitHub storage.
    io.emit("user_feedback", payload);
  });

  socket.on("user_feedback_history_request", () => {
    // No server-side feedback history and no GitHub storage read.
    socket.emit("user_feedback_history_response", { feedbacks: [] });
  });

  // Official messages are never persisted on the server. Socket.IO only relays them live;
  // each native client stores received messages in its own local index.db.
  socket.on("send_official_message", (data = {}) => {
    const rawSenderId = String(data.senderId || "");
    if (rawSenderId !== "hurry_team_official" && rawSenderId !== "hurry_system_official") {
      console.warn("Unauthorized or invalid official sender ID:", rawSenderId);
      return;
    }

    const isTeam = rawSenderId === "hurry_team_official";
    const payload = {
      id: String(data.id || `official_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`),
      senderId: rawSenderId,
      senderName: isTeam ? "Zyora Team" : "Zyora System",
      senderPhoto: isTeam ? "/logo.png" : "/file_00000000a66881f8aa9e15d2fe2b9a0c.png",
      text: String(data.text || ""),
      type: data.type || (data.imageUrl ? "image" : "message"),
      imageUrl: data.imageUrl || undefined,
      timestamp: Number(data.timestamp || Date.now()),
      isOfficialBroadcast: true,
    };

    // Live-only relay; deliberately no GitHub storage/Redis write.
    io.emit("official_broadcast_message", payload);
  });

  socket.on("official_message_history_request", () => {
    // History is device-local (index.db), so the server does not return database history.
    socket.emit("official_message_history_response", { messages: [] });
  });

  socket.on("disconnect", () => {
    const room = socket.roomId;
    const userId =
      socket.roomUserId ||
      socket.userId;

    if (socket.roomId && userId) {
      const room = String(socket.roomId);

      // Grace period of 5s before clearing seat on disconnect
      clearUserSeatGracefully(room, String(userId), 5000, socket.id);
    }

    if (room && userId) {
      removeUserFromRoom(
        String(room),
        String(userId)
      );

      socket
        .to(`room:${room}`)
        .emit("room_user_offline", {
          roomId: String(room),
          userId: String(userId),
        });

      emitRoomPresence(room);
    }

    markUserOffline(socket);

    emitGlobalRoomPresence();

    console.log(
      "User disconnected:",
      socket.id
    );
  });
});



// ==============================================================
// BANS ENDPOINTS
// ==============================================================

app.get("/api/bans", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });
    const bans = await db.collection("bans").find().toArray();
    res.json({ bans });
  } catch (err) {
    console.error("GET /api/bans error:", err);
    res.status(500).json({ error: "Internal error" });
  }
});

app.post("/api/bans", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });

    // Check if requester is actually an official/admin (Using a simple check matching the owner panel login logic)
    const requesterId = req.headers['x-requester-id'];
    const OFFICIAL_IDS = ['500001', '500002', '500003', '500004', '500005', '700001', '700002', '700003'];
    // In this app, special accounts and owner login use specific IDs.
    // Ideally we'd verify a Firebase token here, but given the existing codebase's reliance on client-side ID checks (like in LoginPage),
    // we'll enforce that the requester is in the OFFICIAL_IDS list or is the Hurry Owner.
    if (!requesterId || (!OFFICIAL_IDS.includes(requesterId) && requesterId !== '100002' && requesterId !== '100003')) {
       return res.status(403).json({ error: "Forbidden: Not an admin" });
    }

    const data = req.body;
    if (!data.accountId) return res.status(400).json({ error: "Missing accountId" });

    await db.collection("bans").updateOne(
      { accountId: data.accountId },
      { $set: data },
      { upsert: true }
    );
    io.emit('banned_logout', {
      accountId: data.accountId,
      userId: data.userId,
      type: data.type,
      unbanTime: data.unbanTime
    });
    res.json({ success: true });
  } catch (err) {
    console.error("POST /api/bans error:", err);
    res.status(500).json({ error: "Internal error" });
  }
});

app.post("/api/bans/unban", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });

    // Check if requester is actually an official/admin (Using a simple check matching the owner panel login logic)
    const requesterId = req.headers['x-requester-id'];
    const OFFICIAL_IDS = ['500001', '500002', '500003', '500004', '500005', '700001', '700002', '700003'];
    // In this app, special accounts and owner login use specific IDs.
    // Ideally we'd verify a Firebase token here, but given the existing codebase's reliance on client-side ID checks (like in LoginPage),
    // we'll enforce that the requester is in the OFFICIAL_IDS list or is the Hurry Owner.
    if (!requesterId || (!OFFICIAL_IDS.includes(requesterId) && requesterId !== '100002' && requesterId !== '100003')) {
       return res.status(403).json({ error: "Forbidden: Not an admin" });
    }

    const { accountId, reason } = req.body;
    if (!accountId) return res.status(400).json({ error: "Missing accountId" });

    await db.collection("bans").deleteOne({ accountId: String(accountId) });
    res.json({ success: true });
  } catch (err) {
    console.error("POST /api/bans/unban error:", err);
    res.status(500).json({ error: "Internal error" });
  }
});

app.post("/api/check-ban", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });
    const { accountId, deviceId, ipAddress } = req.body;

    const query = { $or: [] };
    if (accountId) query.$or.push({ accountId: String(accountId) });
    if (deviceId && deviceId !== '') query.$or.push({ deviceId: String(deviceId), timeOption: 'Device Ban' });

    if (query.$or.length === 0) {
      return res.json({ banned: false });
    }

    const activeBans = await db.collection("bans").find(query).toArray();
    if (activeBans.length === 0) return res.json({ banned: false });

    const now = Date.now();
    for (const ban of activeBans) {
      if (ban.unbanTime === -1 || ban.unbanTime > now) {
        return res.json({
          banned: true,
          banData: ban
        });
      }
    }

    res.json({ banned: false });
  } catch (err) {
    console.error("POST /api/check-ban error:", err);
    res.status(500).json({ error: "Internal error" });
  }
});

// ==============================================================
 // DAILY CHECK-IN PERSISTENCE
 // Browser/WebView storage remains the fast local cache, while this
 // record survives APK uninstall/reinstall for the same user ID.
 // ==============================================================
app.get("/api/daily-checkin", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });
    const userId = String(req.query.userId || "").trim();
    if (!userId) return res.status(400).json({ error: "Missing userId" });

    const record = await db.collection("dailyCheckIns").findOne({ userId });
    res.json({
      success: true,
      currentDay: Number(record?.currentDay || 1),
      claimedDate: String(record?.claimedDate || ""),
    });
  } catch (err) {
    console.error("GET /api/daily-checkin error:", err);
    res.status(500).json({ error: "Internal error" });
  }
});

app.post("/api/daily-checkin", async (req, res) => {
  try {
    if (!db) return res.status(503).json({ error: "DB not connected" });

    const userId = String(req.body?.userId || "").trim();
    const dateKey = String(req.body?.dateKey || "").trim();
    const requestedDay = Number(req.body?.currentDay || 1);

    if (!userId || !dateKey || !Number.isInteger(requestedDay) || requestedDay < 1 || requestedDay > 7) {
      return res.status(400).json({ error: "Invalid check-in data" });
    }

    const existing = await db.collection("dailyCheckIns").findOne({ userId });

    if (existing?.claimedDate === dateKey) {
      return res.json({
        success: true,
        alreadyClaimed: true,
        currentDay: Number(existing.currentDay || requestedDay),
        claimedDate: dateKey,
      });
    }

    const nextDay = requestedDay < 7 ? requestedDay + 1 : 1;
    await db.collection("dailyCheckIns").updateOne(
      { userId },
      {
        $set: {
          userId,
          currentDay: nextDay,
          claimedDate: dateKey,
          updatedAt: Date.now(),
        },
      },
      { upsert: true }
    );

    res.json({
      success: true,
      alreadyClaimed: false,
      currentDay: nextDay,
      claimedDate: dateKey,
    });
  } catch (err) {
    console.error("POST /api/daily-checkin error:", err);
    res.status(500).json({ error: "Internal error" });
  }
});


const PORT = process.env.PORT || 10000;

app.get("/health", (_req, res) => {
  res.status(200).json({
    status: "ok",
    service: "Zyora Backend",
    socket: "ready",
    database: db ? "github-database-connected" : "github-database-unavailable",
  });
});

let githubConnectInProgress = false;
async function connectGitHubDatabase() {
  if (githubConnectInProgress || db) return;
  githubConnectInProgress = true;
  try {
    await databaseClient.connect();
    db = databaseClient.db();
    console.log("GitHub document database ready");
    const pendingBags = await db.collection("luckyBags").find({ status: "open" }).toArray();
    for (const bag of pendingBags) {
      if (Number(bag.expiresAt) <= Date.now() && Number(bag.claimedCount || 0) === 0) {
        await refundLuckyBag(bag.bagId);
      } else if (Number(bag.expiresAt) > Date.now()) {
        scheduleLuckyBagRefund(bag);
      }
    }
  } catch (error) {
    console.error("GitHub database connection/restore failed:", error.message);
  } finally {
    githubConnectInProgress = false;
  }
}
function startGitHubRetryLoop() {
  void connectGitHubDatabase();
  const retryTimer = setInterval(() => { if (!db) void connectGitHubDatabase(); }, 5000);
  if (typeof retryTimer.unref === "function") retryTimer.unref();
}
server.listen(PORT, "0.0.0.0", () => {
  console.log(`Zyora Backend listening on port ${PORT}`);
  startGitHubRetryLoop();
});
