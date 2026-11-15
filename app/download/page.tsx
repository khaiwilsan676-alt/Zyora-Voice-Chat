import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Download Zyora",
  description: "Download the latest Zyora Native Android APK.",
};

const APK_URL =
  "https://github.com/khaiwilsan676-alt/Zyora-Voice-Chat/releases/latest/download/app-debug.apk";

export default function DownloadPage() {
  return (
    <main className="min-h-screen bg-[#080b12] px-5 py-12 text-white">
      <div className="mx-auto flex min-h-[calc(100vh-6rem)] w-full max-w-md flex-col items-center justify-center text-center">
        <div className="mb-6 flex h-24 w-24 items-center justify-center overflow-hidden rounded-[2rem] border border-white/10 bg-white/5 shadow-2xl shadow-blue-950/50">
          <img
            src="/Zyora-Voice-Chat/logo.png"
            alt="Zyora app logo"
            className="h-full w-full object-cover"
          />
        </div>
        <p className="mb-3 text-xs font-bold uppercase tracking-[0.32em] text-blue-300">
          Official Android app
        </p>
        <h1 className="text-4xl font-black tracking-tight">Zyora</h1>
        <p className="mt-3 max-w-xs text-sm leading-6 text-slate-400">
          Voice chat, rooms and friends — all in the Zyora native Android app.
        </p>

        <div className="mt-9 w-full rounded-3xl border border-white/10 bg-white/[0.04] p-5 text-left shadow-xl">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold">Zyora Native APK</p>
              <p className="mt-1 text-xs text-slate-400">Android installer · Latest release</p>
            </div>
            <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-emerald-300">
              Official
            </span>
          </div>
          <a
            href={APK_URL}
            className="mt-6 flex min-h-14 w-full items-center justify-center rounded-2xl bg-blue-600 px-5 py-4 text-base font-bold text-white shadow-lg shadow-blue-950/40 transition hover:bg-blue-500 active:scale-[0.99]"
          >
            Download APK
          </a>
          <p className="mt-3 text-center text-[11px] leading-5 text-slate-500">
            Downloads the latest Kotlin / Jetpack Compose Android APK from the
            official Zyora GitHub release.
          </p>
        </div>

        <div className="mt-7 max-w-xs text-left text-xs leading-5 text-slate-500">
          <p className="font-semibold text-slate-300">How to install</p>
          <ol className="mt-2 list-decimal space-y-1 pl-4">
            <li>Tap Download APK and wait for the download to finish.</li>
            <li>Open the downloaded APK file.</li>
            <li>If Android asks, allow installs from this browser, then tap Install.</li>
          </ol>
        </div>
        <p className="mt-10 text-[10px] font-medium tracking-[0.2em] text-slate-600">
          ZYORA · NATIVE ANDROID
        </p>
      </div>
    </main>
  );
}
