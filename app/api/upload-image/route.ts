import { createHash } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

function getCloudinaryConfig() {
  const raw = process.env.CLOUDINARY_URL?.trim();
  if (!raw) return null;

  try {
    const parsed = new URL(raw);
    if (parsed.protocol !== "cloudinary:") return null;
    const cloudName = parsed.hostname;
    const apiKey = decodeURIComponent(parsed.username);
    const apiSecret = decodeURIComponent(parsed.password);
    if (!cloudName || !apiKey || !apiSecret) return null;
    return { cloudName, apiKey, apiSecret };
  } catch {
    return null;
  }
}

export async function POST(request: NextRequest) {
  const config = getCloudinaryConfig();
  if (!config) {
    return NextResponse.json(
      { error: "Image storage is not configured on the server." },
      { status: 503 },
    );
  }

  let form: FormData;
  try {
    form = await request.formData();
  } catch {
    return NextResponse.json({ error: "Invalid upload request." }, { status: 400 });
  }

  const file = form.get("file");
  const folderInput = String(form.get("folder") || "zyora/profiles");
  const allowedFolders = new Set(["zyora/profiles", "zyora/rooms", "zyora/albums"]);
  const folder = allowedFolders.has(folderInput) ? folderInput : "zyora/profiles";

  if (!(file instanceof File)) {
    return NextResponse.json({ error: "Choose an image to upload." }, { status: 400 });
  }
  if (!["image/jpeg", "image/png", "image/webp", "image/gif"].includes(file.type)) {
    return NextResponse.json({ error: "Only JPG, PNG, WEBP, or GIF images are allowed." }, { status: 415 });
  }
  if (file.size <= 0 || file.size > 5 * 1024 * 1024) {
    return NextResponse.json({ error: "Image must be 5 MB or smaller." }, { status: 413 });
  }

  try {
    const timestamp = Math.floor(Date.now() / 1000).toString();
    const signatureBase = `folder=${folder}&timestamp=${timestamp}${config.apiSecret}`;
    const signature = createHash("sha1").update(signatureBase).digest("hex");

    const upload = new FormData();
    upload.set("file", file);
    upload.set("folder", folder);
    upload.set("timestamp", timestamp);
    upload.set("api_key", config.apiKey);
    upload.set("signature", signature);

    const response = await fetch(
      `https://api.cloudinary.com/v1_1/${config.cloudName}/image/upload`,
      { method: "POST", body: upload, cache: "no-store" },
    );
    const result = await response.json().catch(() => ({}));

    if (!response.ok || typeof result.secure_url !== "string") {
      console.error("Cloudinary upload failed:", response.status, result.error?.message || "unknown error");
      return NextResponse.json({ error: "Image upload failed. Please try again." }, { status: 502 });
    }

    return NextResponse.json({
      success: true,
      url: result.secure_url,
      publicId: result.public_id,
      bytes: result.bytes,
      format: result.format,
    });
  } catch (error) {
    console.error("Image upload request failed:", error);
    return NextResponse.json({ error: "Image upload service is temporarily unavailable." }, { status: 502 });
  }
}
