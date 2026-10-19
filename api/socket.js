const { createServer } = require("http");
const { Server } = require("socket.io");

const server = createServer();

const io = new Server(server, {
  path: "/api/socket",
  cors: {
    origin: "*",
    methods: ["GET", "POST"],
  },
  transports: ["websocket"],
  maxHttpBufferSize: 1024 * 1024,
});

const rooms = new Map();

function getUsers(roomId) {
  return Array.from(rooms.get(roomId)?.values() || []);
}

function socketOwnsUser(socket, userId) {
  const id = String(userId || "");
  if (!id || !socket.authenticated) return false;
  return id === String(socket.userId || "") || id === String(socket.accountId || "");
}

function socketInRoom(socket, roomId) {
  const id = String(roomId || "");
  return Boolean(id && socket.roomId === id && socket.rooms.has(`room:${id}`));
}

function emitPresence(roomId) {
  const users = getUsers(roomId);

  io.to(`room:${roomId}`).emit("room_presence", {
    roomId,
    users,
    activeUserCount: users.length,
  });
}

function removeFromRoom(socket) {
  const roomId = socket.roomId;
  const userId = socket.roomUserId;

  if (!roomId || !userId) return;

  const room = rooms.get(roomId);

  if (room) {
    room.delete(userId);

    if (room.size === 0) {
      rooms.delete(roomId);
    }
  }

  socket.to(`room:${roomId}`).emit("room_user_offline", {
    roomId,
    userId,
  });

  socket.leave(`room:${roomId}`);

  emitPresence(roomId);

  socket.roomId = null;
  socket.roomUserId = null;
}

io.on("connection", (socket) => {
  socket.on("register", (data = {}) => {
    const userId = String(
      data.userId ||
      data.uid ||
      data.id ||
      ""
    );

    const accountId = String(
      data.accountId ||
      userId
    );

    if (!userId) return;

    socket.userId = userId;
    socket.accountId = accountId;
    socket.authenticated = true;

    socket.join(`user:${userId}`);

    if (accountId !== userId) {
      socket.join(`user:${accountId}`);
    }

    socket.emit("global_room_presence", {
      rooms: Array.from(rooms.entries()).map(
        ([roomId, room]) => ({
          roomId,
          users: Array.from(room.values()),
          activeUserCount: room.size,
        })
      ),
    });
  });

  socket.on("global_room_presence_request", () => {
    if (!socket.authenticated) return;

    socket.emit("global_room_presence", {
      rooms: Array.from(rooms.entries()).map(
        ([roomId, room]) => ({
          roomId,
          users: Array.from(room.values()),
          activeUserCount: room.size,
        })
      ),
    });
  });

  socket.on("room_presence_request", ({ roomId } = {}) => {
    if (!socket.authenticated || !roomId) return;

    const id = String(roomId);
    const users = getUsers(id);

    socket.emit("room_presence", {
      roomId: id,
      users,
      activeUserCount: users.length,
    });
  });

  socket.on("room_join", (data = {}) => {
    const roomId = String(data.roomId || "");
    const requestedUserId = String(data.userId || "");

    if (!socket.authenticated || !roomId || !requestedUserId) return;
    if (!socketOwnsUser(socket, requestedUserId)) return;

    const userId = String(socket.accountId || socket.userId);

    if (
      socket.roomId &&
      socket.roomUserId &&
      (
        socket.roomId !== roomId ||
        socket.roomUserId !== userId
      )
    ) {
      removeFromRoom(socket);
    }

    if (!rooms.has(roomId)) {
      rooms.set(roomId, new Map());
    }

    const room = rooms.get(roomId);

    const user = {
      accountId: userId,
      userId: String(socket.userId),
      name: data.name || "User",
      image:
        data.dp ||
        data.image ||
        "/default-avatar.png",
      email: data.email || "",
    };

    const alreadyOnline = room.has(userId);

    room.set(userId, user);

    socket.roomId = roomId;
    socket.roomUserId = userId;

    socket.join(`room:${roomId}`);

    socket.emit("room_presence", {
      roomId,
      users: getUsers(roomId),
      activeUserCount: getUsers(roomId).length,
    });

    if (!alreadyOnline) {
      socket.to(`room:${roomId}`).emit(
        "room_user_online",
        {
          roomId,
          userId,
          user,
        }
      );
    }

    emitPresence(roomId);
  });

  socket.on("room_leave", () => {
    removeFromRoom(socket);
  });

  socket.on("room_message", (message = {}) => {
    if (!socket.authenticated || !message.roomId || !message.senderId) {
      return;
    }

    const roomId = String(message.roomId);
    if (!socketInRoom(socket, roomId)) return;
    if (!socketOwnsUser(socket, message.senderId)) return;

    io.to(`room:${roomId}`).emit(
      "room_message",
      message
    );
  });

  socket.on("private_message", (message = {}) => {
    if (
      !socket.authenticated ||
      !message.receiverId ||
      !message.senderId
    ) {
      return;
    }

    if (!socketOwnsUser(socket, message.senderId)) return;

    io.to(
      `user:${String(message.receiverId)}`
    ).emit(
      "private_message",
      message
    );
  });

  socket.on("check_presence", (targetUserId) => {
    const id = String(targetUserId || "");

    if (!socket.authenticated || !id) return;

    socket.emit("presence_status", {
      userId: id,
      online: io.sockets.adapter.rooms.has(`user:${id}`),
    });
  });

  socket.on("disconnect", () => {
    removeFromRoom(socket);
  });
});

module.exports = server;
