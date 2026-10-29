import { createHmac, timingSafeEqual } from "node:crypto";
import { NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type GitHubUser = {
  id: number;
  login: string;
  name: string | null;
  avatar_url: string;
  email: string | null;
};

function sign(value: string, secret: string) {
  return createHmac("sha256", secret).update(value).digest("base64url");
}

function safeEqual(a: string, b: string) {
  const left = Buffer.from(a);
  const right = Buffer.from(b);
  return left.length === right.length && timingSafeEqual(left, right);
}

function fail(request: Request) {
  const response = NextResponse.redirect(new URL("/?github=error", request.url));
  response.cookies.delete("zyora_github_oauth_state");
  return response;
}

export async function GET(request: Request) {
  const url = new URL(request.url);
  const code = url.searchParams.get("code");
  const returnedState = url.searchParams.get("state");
  const stateCookie = request.headers.get("cookie")?.match(/(?:^|;\s*)zyora_github_oauth_state=([^;]+)/)?.[1];
  const clientId = process.env.GITHUB_CLIENT_ID;
  const clientSecret = process.env.GITHUB_CLIENT_SECRET;
  const sessionSecret = process.env.GITHUB_SESSION_SECRET || clientSecret;

  if (!code || !returnedState || !stateCookie || !safeEqual(returnedState, decodeURIComponent(stateCookie)) || !clientId || !clientSecret || !sessionSecret) {
    return fail(request);
  }

  try {
    const tokenResponse = await fetch("https://github.com/login/oauth/access_token", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        "User-Agent": "Zyora",
      },
      body: JSON.stringify({
        client_id: clientId,
        client_secret: clientSecret,
        code,
        redirect_uri: new URL("/api/auth/github/callback", request.url).toString(),
      }),
      cache: "no-store",
    });
    if (!tokenResponse.ok) return fail(request);
    const tokenData = await tokenResponse.json();
    if (!tokenData.access_token) return fail(request);

    const headers = {
      Authorization: `Bearer ${tokenData.access_token}`,
      Accept: "application/vnd.github+json",
      "X-GitHub-Api-Version": "2022-11-28",
      "User-Agent": "Zyora",
    };
    const [userResponse, emailsResponse] = await Promise.all([
      fetch("https://api.github.com/user", { headers, cache: "no-store" }),
      fetch("https://api.github.com/user/emails", { headers, cache: "no-store" }),
    ]);
    if (!userResponse.ok) return fail(request);
    const user = (await userResponse.json()) as GitHubUser;
    const emails = emailsResponse.ok ? await emailsResponse.json() : [];
    const primaryEmail = Array.isArray(emails)
      ? emails.find((item: any) => item?.primary && item?.verified)?.email
      : undefined;
    const email = primaryEmail || user.email || "";
    const uid = `github:${user.id}`;
    const sessionPayload = Buffer.from(JSON.stringify({
      uid,
      githubId: String(user.id),
      login: user.login,
      name: user.name || user.login || "User",
      email,
      image: user.avatar_url || "",
      exp: Math.floor(Date.now() / 1000) + 60 * 60 * 24 * 7,
    })).toString("base64url");
    const sessionValue = `${sessionPayload}.${sign(sessionPayload, sessionSecret)}`;

    const response = NextResponse.redirect(new URL("/?github=success", request.url));
    response.cookies.set("zyora_github_session", sessionValue, {
      httpOnly: true,
      secure: url.protocol === "https:",
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60 * 24 * 7,
    });
    response.cookies.delete("zyora_github_oauth_state");
    return response;
  } catch {
    return fail(request);
  }
}
