const GOOGLE_SCRIPT_URL =
  process.env.NEXT_PUBLIC_GOOGLE_SCRIPT_URL || "";

async function request(
  method: "GET" | "POST",
  action: string,
  data: Record<string, any> = {}
) {
  if (!GOOGLE_SCRIPT_URL) {
    throw new Error("NEXT_PUBLIC_GOOGLE_SCRIPT_URL is missing");
  }

  let response: Response;

  if (method === "GET") {
    const url = new URL(GOOGLE_SCRIPT_URL);
    url.searchParams.set("action", action);

    Object.entries(data).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.set(key, String(value));
      }
    });

    response = await fetch(url.toString(), {
      method: "GET",
      cache: "no-store",
    });
  } else {
    response = await fetch(GOOGLE_SCRIPT_URL, {
      method: "POST",
      headers: {
        "Content-Type": "text/plain;charset=utf-8",
      },
      body: JSON.stringify({
        action,
        ...data,
      }),
      cache: "no-store",
    });
  }

  if (!response.ok) {
    throw new Error(`Google Sheet API error: ${response.status}`);
  }

  return response.json();
}

// ===============================
// USERS
// ===============================

export function getUsers() {
  return request("GET", "users");
}

export function saveUser(data: Record<string, any>) {
  return request("POST", "saveUser", data);
}

// ===============================
// ROOMS
// ===============================

export function getRooms() {
  return request("GET", "rooms");
}

export function createRoom(data: Record<string, any>) {
  return request("POST", "createRoom", data);
}

// ===============================
// ROOM MEMBERS
// ===============================

export function getRoomMembers() {
  return request("GET", "roomMembers");
}

export function joinRoom(data: Record<string, any>) {
  return request("POST", "joinRoom", data);
}

// ===============================
// PRIVATE MESSAGES
// ===============================

export function getMessages(userA: string, userB: string) {
  return request("GET", "messages", {
    userA,
    userB,
  });
}

export function sendMessage(data: Record<string, any>) {
  return request("POST", "sendMessage", data);
}

// ===============================
// OWNER PANEL
// ===============================

export function getOwnerCredentials() {
  return request("GET", "ownerCredentials");
}

export function saveOwnerCredentials(data: Record<string, any>) {
  return request("POST", "saveOwnerCredentials", data);
}

export function getSession(id: string) {
  return request("GET", "session", { id });
}

export function updateSession(
  id: string,
  data: Record<string, any>
) {
  return request("POST", "updateSession", {
    id,
    ...data,
  });
}

// ===============================
// FEEDBACK
// ===============================

export function getFeedbacks() {
  return request("GET", "feedbacks");
}

export function deleteFeedback(id: string) {
  return request("POST", "deleteFeedback", {
    id,
  });
}

// ===============================
// AI CHATS
// ===============================

export function getAiChats() {
  return request("GET", "aiChats");
}

export function saveAiChat(data: Record<string, any>) {
  return request("POST", "saveAiChat", data);
}

export function updateAiChat(id: string, data: Record<string, any>) {
  return request("POST", "updateAiChat", {
    id,
    ...data,
  });
}

export function deleteAiChat(id: string) {
  return request("POST", "deleteAiChat", { id });
}

export function saveRoom(data: Record<string, any>) {
  return request("POST", "saveRoom", data);
}

export function saveAdminSession(id: string, data: Record<string, any>) {
  return request("POST", "updateSession", {
    id,
    ...data,
  });
}
