import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Zyora",
  description: "Every time with You",
};

const APK_URL =
  "https://github.com/khaiwilsan676-alt/Zyora-Voice-Chat/releases/latest/download/app-debug.apk";

const BASE = "/Zyora-Voice-Chat";

export default function DownloadPage() {
  return (
    <main
      className="relative min-h-[100svh] w-full overflow-hidden bg-cover bg-center bg-no-repeat text-white"
      style={{ backgroundImage: `url("${BASE}/IMG_20261115_170818.png")` }}
    >
      <div className="absolute inset-0 bg-black/15" />

      <div className="relative mx-auto flex min-h-[100svh] w-full flex-col items-center px-0 pb-28 pt-9 text-center">
        <img
          src={`${BASE}/logo.png`}
          alt="Zyora"
          className="h-[88px] w-[88px] rounded-[10px] object-cover shadow-2xl"
        />

        <h1 className="-mt-1 text-[32px] font-extrabold tracking-wide drop-shadow-lg">
          Zyora
        </h1>
        <p className="-mt-1 text-[20px] font-medium tracking-wide drop-shadow-md">
          Every time with You
        </p>

        <div className="mt-8 flex w-full snap-x snap-mandatory gap-4 overflow-x-auto px-5 pb-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          <img
            src={`${BASE}/IMG_20261115_171617.png`}
            alt=""
            className="h-[100vw] w-[62vw] max-w-none shrink-0 snap-center rounded-md border-2 border-[#102f70] bg-[#07152e] object-contain"
          />
          <img
            src={`${BASE}/IMG_20261115_171557~2.jpg`}
            alt=""
            className="h-[100vw] w-[65vw] max-w-none shrink-0 snap-center rounded-md border-2 border-[#102f70] bg-[#07152e] object-contain"
          />
          <img
            src={`${BASE}/IMG_20261115_171647.png`}
            alt=""
            className="h-[100vw] w-[70vw] max-w-none shrink-0 snap-center rounded-md border-2 border-[#102f70] bg-[#07152e] object-contain"
          />
        </div>
      </div>

      <a
        href={APK_URL}
        className="fixed bottom-5 left-5 right-5 z-10 mx-auto flex min-h-[58px] max-w-[520px] items-center justify-center gap-3 rounded-[18px] bg-white px-6 text-[17px] font-bold text-[#171717] shadow-2xl active:scale-[0.98]"
      >
        <svg viewBox="0 0 48 48" aria-hidden="true" className="h-6 w-6">
          <path fill="#4285F4" d="M43.6 24.5c0-1.4-.1-2.5-.4-3.7H24v7h11a9.5 9.5 0 0 1-4.1 6.2v5.1h6.6c3.9-3.6 6.1-8.7 6.1-14.6Z" />
          <path fill="#34A853" d="M24 44c5.5 0 10.1-1.8 13.5-4.9l-6.6-5.1c-1.8 1.2-4.1 2-6.9 2-5.3 0-9.8-3.6-11.4-8.4H5.8v5.3A20 20 0 0 0 24 44Z" />
          <path fill="#FBBC05" d="M12.6 27.6a12 12 0 0 1 0-7.2v-5.3H5.8a20 20 0 0 0 0 17.8l6.8-5.3Z" />
          <path fill="#EA4335" d="M24 11.9c3 0 5.7 1 7.8 3.1l5.8-5.8C34.1 5.8 29.5 4 24 4A20 20 0 0 0 5.8 15.1l6.8 5.3C14.2 15.5 18.7 11.9 24 11.9Z" />
        </svg>
        <span>Download</span>
      </a>
    </main>
  );
}
