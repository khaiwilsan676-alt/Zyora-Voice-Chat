import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

export async function POST(request: NextRequest) {
  try {
    let userId = "";
    let folder = "user-dp";
    let imageData = "";

    if ((request.headers.get("content-type") || "").includes("application/json")) {
      const body = await request.json();
      userId = String(body.userId || "").trim();
      folder = String(body.folder || "user-dp");
      imageData = String(body.imageData || "");
    } else {
      const form = await request.formData();
      userId = String(form.get("userId") || "").trim();
      folder = String(form.get("folder") || "user-dp");
      const file = form.get("file");
      if (!(file instanceof File)) {
        return NextResponse.json({ error: "Choose an image to upload." }, { status: 400 });
      }
      if (file.size <= 0 || file.size > MAX_IMAGE_BYTES) {
        return NextResponse.json({ error: "Profile image must be 5 MB or smaller." }, { status: 413 });
      }
      const allowedTypes = new Set(["image/jpeg", "image/png", "image/webp", "image/gif"]);
      if (!allowedTypes.has(file.type)) {
        return NextResponse.json({ error: "Only JPG, PNG, WEBP, or GIF images are allowed." }, { status: 415 });
      }
      imageData = "data:" + file.type + ";base64," + Buffer.from(await file.arrayBuffer()).toString("base64");
    }

    const upstream = await fetch("https://zyora-server.onrender.com/api/upload-image", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId, folder, imageData }),
      cache: "no-store",
    });
    const result = await upstream.json().catch(() => ({ error: "Image upload service returned an invalid response." }));
    return NextResponse.json(result, { status: upstream.status });
  } catch (error) {
    console.error("Profile image proxy failed:", error);
    return NextResponse.json({ error: "Profile image upload is temporarily unavailable." }, { status: 502 });
  }
}
