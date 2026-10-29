import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const REPO = "khaiwilsan676-alt/Zyora-Voice-Chat";
const MAX_BYTES = 5 * 1024 * 1024;

export async function POST(request: NextRequest) {
  try {
    const expectedSecret = process.env.ZYORA_UPLOAD_SECRET;
    const githubToken = process.env.GITHUB_TOKEN;
    if (!expectedSecret || !githubToken) {
      return NextResponse.json({ error: "Upload service is not configured. Add server-side environment variables in Vercel." }, { status: 503 });
    }

    const suppliedSecret = request.headers.get("x-zyora-upload-secret") || "";
    if (suppliedSecret.length !== expectedSecret.length) {
      return NextResponse.json({ error: "Access denied." }, { status: 401 });
    }
    let matches = true;
    for (let i = 0; i < expectedSecret.length; i++) {
      matches = matches && suppliedSecret.charCodeAt(i) === expectedSecret.charCodeAt(i);
    }
    if (!matches) return NextResponse.json({ error: "Access denied." }, { status: 401 });

    const body = await request.json();
    const path = typeof body.path === "string" ? body.path.trim() : "";
    const fileName = typeof body.fileName === "string" ? body.fileName.trim() : "";
    const contentBase64 = typeof body.contentBase64 === "string" ? body.contentBase64 : "";
    const message = typeof body.message === "string" ? body.message.trim().slice(0, 120) : "";

    if (!path || path.startsWith("/") || path.includes("..") || path.includes("\\") || path.includes("\0") || path.split("/").some((part: string) => !part || part.startsWith("."))) {
      return NextResponse.json({ error: "Invalid repository path." }, { status: 400 });
    }
    if (!path.startsWith("public/") && !path.startsWith("android-native/")) {
      return NextResponse.json({ error: "Uploads are allowed only inside public/ or android-native/." }, { status: 400 });
    }
    if (/(^|\/)(\.env[^/]*|secrets?[^/]*|credentials?[^/]*|.*\.keystore|.*\.jks|.*\.pem|.*\.p12|.*\.pfx)$/i.test(path) ||
        path.startsWith("android-native/.") || path.startsWith("public/.") ||
        path.startsWith("public/api/")) {
      return NextResponse.json({ error: "This file path is blocked for security." }, { status: 400 });
    }
    if (!fileName || fileName.length > 180 || /[\\/\0]/.test(fileName) || !contentBase64) {
      return NextResponse.json({ error: "Choose a file and provide a valid repository path." }, { status: 400 });
    }

    const decoded = Buffer.from(contentBase64, "base64");
    if (!decoded.length || decoded.length > MAX_BYTES) {
      return NextResponse.json({ error: "File must be between 1 byte and 5 MB." }, { status: 413 });
    }
    if (decoded.toString("base64") !== contentBase64.replace(/\s/g, "")) {
      return NextResponse.json({ error: "Invalid file data." }, { status: 400 });
    }

    const apiUrl = `https://api.github.com/repos/${REPO}/contents/${path.split("/").map(encodeURIComponent).join("/")}`;
    const existing = await fetch(apiUrl, {
      headers: { Authorization: `Bearer ${githubToken}`, Accept: "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28" },
      cache: "no-store",
    });
    let sha: string | undefined;
    if (existing.ok) {
      const current = await existing.json();
      sha = typeof current.sha === "string" ? current.sha : undefined;
    } else if (existing.status !== 404) {
      return NextResponse.json({ error: "GitHub could not check this path. Verify token permissions." }, { status: 502 });
    }

    const commitResponse = await fetch(apiUrl, {
      method: "PUT",
      headers: { Authorization: `Bearer ${githubToken}`, Accept: "application/vnd.github+json", "Content-Type": "application/json", "X-GitHub-Api-Version": "2022-11-28" },
      body: JSON.stringify({
        message: message || `Owner Panel: upload ${fileName}`,
        content: contentBase64.replace(/\s/g, ""),
        ...(sha ? { sha } : {}),
        branch: process.env.GITHUB_UPLOAD_BRANCH || "main",
      }),
      cache: "no-store",
    });
    const result = await commitResponse.json().catch(() => ({}));
    if (!commitResponse.ok) {
      return NextResponse.json({ error: commitResponse.status === 409 || commitResponse.status === 422 ? "GitHub rejected this commit (path conflict or branch protection)." : "GitHub commit failed. Check the token's Contents: Read and write permission." }, { status: 502 });
    }
    return NextResponse.json({
      success: true,
      path,
      commitUrl: result?.commit?.html_url || null,
      commitSha: result?.commit?.sha || null,
      message: "File committed to GitHub. Configured deployment/build workflows will run if their path filters match.",
    });
  } catch {
    return NextResponse.json({ error: "Upload failed. Check the file and try again." }, { status: 400 });
  }
}
