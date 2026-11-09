const API_URL =
  process.env.NEXT_PUBLIC_BACKEND_URL ||
  "https://hurry-voice-chat-lz75.onrender.com";

export const apiUrl = (path: string) => {
  const cleanPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_URL.replace(/\/$/, "")}${cleanPath}`;
};

export default API_URL;
