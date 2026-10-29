import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const MIME_TO_EXT: Record<string, string> = {
  "image/jpeg": "jpg",
  "image/png": "png",
  "image/webp": "webp",
  "image/gif": "gif",
};

export async function POST(request: NextRequest) {
  const token = process.env.GITHUB_TOKEN || process.env.GITHUB_UPLOAD_TOKEN;
  const repository = process.env.GITHUB_UPLOAD_REPOSITORY || "khaiwilsan676-alt/Zyora-Voice-Chat";
  const branch = process.env.GITHUB_UPLOAD_BRANCH || "main";
  if (!token) return NextResponse.json({ error: "GitHub image storage is not configured. Set GITHUB_TOKEN on the server." }, { status: 503 });

  let userId = "";
  let folderInput = "user-dp";
  let mimeType = "";
  let bytes: Buffer;
  try {
    if ((request.headers.get("content-type") || "").includes("application/json")) {
      const body = await request.json();
      userId = String(body.userId || "").trim();
      folderInput = String(body.folder || "user-dp");
      const dataUri = String(body.imageData || "");
      const match = dataUri.match(/^data:(image\/(?:jpeg|png|webp|gif));base64,([A-Za-z0-9+/=]+)$/);
      if (!match) return NextResponse.json({ error: "A valid image data URI is required." }, { status: 400 });
      mimeType = match[1];
      bytes = Buffer.from(match[2], "base64");
    } else {
      const form = await request.formData();
      userId = String(form.get("userId") || "").trim();
      folderInput = String(form.get("folder") || "user-dp");
      const file = form.get("file");
      if (!(file instanceof File)) return NextResponse.json({ error: "Choose an image to upload." }, { status: 400 });
      mimeType = file.type;
      bytes = Buffer.from(await file.arrayBuffer());
    }
  } catch {
    return NextResponse.json({ error: "Invalid upload request." }, { status: 400 });
  }

  const allowedFolders = new Set(["user-dp", "user-albums", "user-covers"]);
  const folder = allowedFolders.has(folderInput) ? folderInput : "user-dp";
  if (!userId || !/^[a-zA-Z0-9_-]{1,100}$/.test(userId)) return NextResponse.json({ error: "A valid userId is required." }, { status: 400 });
  const extension = MIME_TO_EXT[mimeType];
  if (!extension) return NextResponse.json({ error: "Only JPG, PNG, WEBP, or GIF images are allowed." }, { status: 415 });
  if (bytes.length <= 0 || bytes.length > MAX_IMAGE_BYTES) return NextResponse.json({ error: "Profile image must be 5 MB or smaller." }, { status: 413 });

  const path = `public/${folder}/${userId}/${folder === "user-dp" ? "dp" : folder === "user-albums" ? "album" : "cover"}-${Date.now()}.${extension}`;
  const apiUrl = `https://api.github.com/repos/${repository}/contents/${path.split("/").map(encodeURIComponent).join("/")}`;
  try {
    const response = await fetch(apiUrl, {
      method: "PUT",
      headers: {
        Accept: "application/vnd.github+json",
        Authorization: `Bearer ${token}`,
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json",
        "User-Agent": "Zyora-Image-Upload",
      },
      body: JSON.stringify({ message: `Upload ${folder} image for user ${userId}`, content: bytes.toString("base64"), branch }),
      cache: "no-store",
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) {
      console.error("GitHub image upload failed:", response.status, result.message || "unknown error");
      return NextResponse.json({ error: "GitHub image upload failed. Check repository access and token permissions." }, { status: response.status === 401 || response.status === 403 ? 502 : response.status });
    }
    const url = `https://raw.githubusercontent.com/${repository}/${branch}/${path}`;
    return NextResponse.json({ success: true, url, path, size: bytes.length, sha: result.content?.sha });
  } catch (error) {
    console.error("GitHub image upload request failed:", error);
    return NextResponse.json({ error: "GitHub image storage is temporarily unavailable." }, { status: 502 });
  }
}
