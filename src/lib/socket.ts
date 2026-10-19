import { io, Socket } from "socket.io-client";

const SOCKET_URL = "https://hurry-voice-chat-lz75.onrender.com";

export const socket: Socket = io(SOCKET_URL, {
  transports: ["websocket"],
  autoConnect: false,
  reconnection: true,
  reconnectionAttempts: Infinity,
  reconnectionDelay: 1000,
  reconnectionDelayMax: 5000,
});

export default socket;
