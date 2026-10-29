'use client';

import { useRef, useState } from "react";
import { UploadCloud, File, CheckCircle2, AlertCircle, Loader2 } from "lucide-react";

export default function OwnerGitHubUpload() {
  const fileInput = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [path, setPath] = useState("public/uploads/");
  const [secret, setSecret] = useState("");
  const [message, setMessage] = useState("Owner Panel: upload file");
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<{ ok: boolean; text: string; url?: string } | null>(null);

  const upload = async () => {
    if (!file) { setResult({ ok: false, text: "Pehle file select karo." }); return; }
    if (!secret) { setResult({ ok: false, text: "Upload access key enter karo." }); return; }
    const targetPath = path.trim().replace(/\/+$/, "") + "/" + file.name;
    if (!targetPath.startsWith("public/") && !targetPath.startsWith("android-native/")) {
      setResult({ ok: false, text: "Path public/ ya android-native/ ke andar hona chahiye." }); return;
    }
    if (file.size > 5 * 1024 * 1024) { setResult({ ok: false, text: "Maximum file size 5 MB hai." }); return; }
    setBusy(true); setResult(null);
    try {
      const contentBase64 = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onerror = () => reject(new Error("File read nahi ho payi."));
        reader.onload = () => {
          const value = String(reader.result || "");
          resolve(value.slice(value.indexOf(",") + 1));
        };
        reader.readAsDataURL(file);
      });
      const response = await fetch("/api/owner/github-upload", {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-zyora-upload-secret": secret },
        body: JSON.stringify({ path: targetPath, fileName: file.name, contentBase64, message }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "Upload failed.");
      setResult({ ok: true, text: `GitHub commit successful: ${data.path}`, url: data.commitUrl || undefined });
      setFile(null);
      if (fileInput.current) fileInput.current.value = "";
    } catch (error) {
      setResult({ ok: false, text: error instanceof Error ? error.message : "Upload failed." });
    } finally { setBusy(false); }
  };

  return (
    <section className="p-4 md:p-6">
      <div className="max-w-3xl rounded-2xl border border-slate-200 bg-white p-5 md:p-7 shadow-sm">
        <div className="flex items-center gap-3 mb-2">
          <div className="rounded-xl bg-indigo-50 p-3 text-indigo-600"><UploadCloud className="h-6 w-6" /></div>
          <div><h2 className="text-xl font-extrabold text-slate-800">GitHub File Upload</h2><p className="text-sm text-slate-500">Upload files from Owner Panel and commit to Zyora repository.</p></div>
        </div>
        <p className="text-sm text-slate-600 mt-4 mb-5">Files are committed directly to the configured GitHub branch. Website deployment or APK build runs only when its workflow path filters match.</p>
        <div className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-bold text-slate-700">Upload access key</label>
            <input type="password" autoComplete="current-password" value={secret} onChange={e => setSecret(e.target.value)} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-indigo-500" placeholder="Vercel ZYORA_UPLOAD_SECRET value" />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-bold text-slate-700">Repository folder</label>
            <input value={path} onChange={e => setPath(e.target.value)} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-indigo-500" placeholder="public/uploads or android-native/app/src/main" />
            <p className="mt-1 text-xs text-slate-500">Allowed folders: public/ and android-native/. File name is added automatically.</p>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-bold text-slate-700">Commit message</label>
            <input value={message} onChange={e => setMessage(e.target.value)} maxLength={120} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-indigo-500" />
          </div>
          <button type="button" onClick={() => fileInput.current?.click()} className="flex w-full items-center gap-3 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-4 text-left hover:border-indigo-400">
            <File className="h-5 w-5 text-slate-500" /><span className="min-w-0 flex-1"><span className="block text-sm font-bold text-slate-800">{file ? file.name : "Choose file"}</span><span className="block text-xs text-slate-500">{file ? `${(file.size / 1024).toFixed(1)} KB · selected` : "Images, assets, source files · up to 5 MB"}</span></span>
          </button>
          <input ref={fileInput} type="file" className="hidden" onChange={e => { setFile(e.target.files?.[0] || null); setResult(null); }} />
          <button type="button" disabled={busy || !file || !secret} onClick={upload} className="flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-3 text-sm font-extrabold text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50">
            {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <UploadCloud className="h-4 w-4" />}{busy ? "Committing to GitHub…" : "Upload & Commit"}
          </button>
          {result && <div className={`flex items-start gap-2 rounded-xl p-3 text-sm font-semibold ${result.ok ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>{result.ok ? <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" /> : <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />}<div>{result.text}{result.url && <a href={result.url} target="_blank" rel="noreferrer" className="mt-1 block underline">Open GitHub commit</a>}</div></div>}
        </div>
      </div>
    </section>
  );
}
