import { randomBytes } from "node:crypto";
import { NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(request: Request) {
  const clientId = process.env.GITHUB_CLIENT_ID;
  if (!clientId) {
    return NextResponse.redirect(new URL("/?github=error", request.url));
  }

  const state = randomBytes(32).toString("hex");
  const callbackUrl = new URL("/api/auth/github/callback", request.url);
  const authorizeUrl = new URL("https://github.com/login/oauth/authorize");
  authorizeUrl.searchParams.set("client_id", clientId);
  authorizeUrl.searchParams.set("redirect_uri", callbackUrl.toString());
  authorizeUrl.searchParams.set("scope", "read:user user:email");
  authorizeUrl.searchParams.set("state", state);

  const response = NextResponse.redirect(authorizeUrl);
  response.cookies.set("zyora_github_oauth_state", state, {
    httpOnly: true,
    secure: new URL(request.url).protocol === "https:",
    sameSite: "lax",
    path: "/",
    maxAge: 600,
  });
  return response;
}
