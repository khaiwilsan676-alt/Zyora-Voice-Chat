import { createHmac, timingSafeEqual } from "node:crypto";
import { NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(request: Request) {
  const secret = process.env.GITHUB_SESSION_SECRET || process.env.GITHUB_CLIENT_SECRET;
  const token = request.headers.get("cookie")?.match(/(?:^|;\s*)zyora_github_session=([^;]+)/)?.[1];
  if (!secret || !token) {
    return NextResponse.json({ authenticated: false }, { status: 401 });
  }

  try {
    const decoded = decodeURIComponent(token);
    const separator = decoded.lastIndexOf(".");
    if (separator < 1) throw new Error("Invalid session");
    const payload = decoded.slice(0, separator);
    const suppliedSignature = Buffer.from(decoded.slice(separator + 1));
    const expectedSignature = Buffer.from(createHmac("sha256", secret).update(payload).digest("base64url"));
    if (suppliedSignature.length !== expectedSignature.length || !timingSafeEqual(suppliedSignature, expectedSignature)) {
      throw new Error("Invalid signature");
    }

    const user = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    if (!user?.uid || !user?.githubId || !user?.exp || user.exp <= Math.floor(Date.now() / 1000)) {
      throw new Error("Expired session");
    }

    return NextResponse.json({
      authenticated: true,
      user: {
        id: user.uid,
        uid: user.uid,
        email: user.email || "",
        name: user.name || "User",
        image: user.image || "",
        photo: user.image || "",
        provider: "github",
      },
    }, { headers: { "Cache-Control": "no-store" } });
  } catch {
    const response = NextResponse.json({ authenticated: false }, { status: 401 });
    response.cookies.delete("zyora_github_session");
    return response;
  }
}
