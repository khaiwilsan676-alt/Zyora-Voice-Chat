import { io, Socket } from "socket.io-client";

// Keep the Owner Panel on the same Socket.IO backend used by the native app
// and the REST API. The previous Hurry host split feedback events across servers.
const SOCKET_URL = "https://zyora-server.onrender.com";

export const socket: Socket = io(SOCKET_URL, {
  transports: ["websocket"],
  autoConnect: false,
  reconnection: true,
  reconnectionAttempts: Infinity,
  reconnectionDelay: 1000,
  reconnectionDelayMax: 5000,
});

export default socket;
