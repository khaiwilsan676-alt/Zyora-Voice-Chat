const { io } = require("socket.io-client");
const socket = io("http://localhost:10000");

socket.on("connect", () => {
  console.log("Connected to WebSocket");
  socket.disconnect();
  process.exit(0);
});
socket.on("connect_error", (err) => {
  console.error("Connection error:", err);
  process.exit(1);
});
