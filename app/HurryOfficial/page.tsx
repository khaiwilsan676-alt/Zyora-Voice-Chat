"use client";

import { useState } from "react";
import Image from "next/image";
import { Menu } from "lucide-react";

export default function HurryOfficial() {
  // Menu open/close ke liye state
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-black text-white">
      {/* Smooth scroll ke liye global CSS */}
      <style jsx global>{`
        html {
          scroll-behavior: smooth;
        }
        body {
          margin: 0;
          background: #000;
          font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        }
      `}</style>

      {/* Navbar */}
      <header className="sticky top-0 z-50 flex items-center justify-between bg-black/10 backdrop-blur-md px-5 py-4 shadow-sm border-b border-white/5">
        <div className="flex items-center gap-3">
          <Image
            src="/logo.png"
            alt="Zyora Logo"
            width={42}
            height={42}
            className="h-9 w-9 object-contain rounded-xl"
          />
          <span className="text-2xl font-bold tracking-wide text-white">Zyora</span>
        </div>

        {/* Menu Button aur Dropdown */}
        <div className="relative">
          <button
            aria-label="Menu"
            className="text-white transition hover:scale-110"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            <Menu size={29} />
          </button>

          {/* Dropdown Box */}
          {isMenuOpen && (
            <div className="absolute right-0 mt-4 flex w-40 flex-col overflow-hidden rounded-2xl border border-gray-800 bg-[#0a0a0a] shadow-[0_0_20px_rgba(0,0,0,0.8)]">
              <a
                href="#home"
                onClick={() => setIsMenuOpen(false)}
                className="px-5 py-3 text-sm font-medium text-gray-300 transition hover:bg-[#141414] hover:text-blue-600"
              >
                Home
              </a>
              <div className="h-[1px] w-full bg-gray-800/50"></div>
              <a
                href="#feature"
                onClick={() => setIsMenuOpen(false)}
                className="px-5 py-3 text-sm font-medium text-gray-300 transition hover:bg-[#141414] hover:text-blue-600"
              >
                Feature
              </a>
              <div className="h-[1px] w-full bg-gray-800/50"></div>
              <a
                href="#about"
                onClick={() => setIsMenuOpen(false)}
                className="px-5 py-3 text-sm font-medium text-gray-300 transition hover:bg-[#141414] hover:text-blue-600"
              >
                About
              </a>
            </div>
          )}
        </div>
      </header>

      {/* Main Content */}
      <main className="px-4 py-12 md:px-8">
        
        {/* 1. Hero Section Card -> ID: home */}
        <section id="home" className="mx-auto mb-10 max-w-5xl overflow-hidden relative rounded-[32px] bg-[#0a0a0a] p-8 md:p-16 shadow-2xl scroll-mt-24">
          <div className="absolute -top-32 left-1/2 h-64 w-[150%] -translate-x-1/2 rounded-[100%] bg-blue-600/30 blur-[75px] pointer-events-none"></div>
          
          <div className="relative z-10 flex flex-col-reverse md:flex-row items-center justify-between gap-10 text-center md:text-left">
            <div className="md:w-1/2 flex flex-col items-center md:items-start">
              <div className="flex items-center gap-5 mb-4">
                <Image
                  src="/logo.png"
                  alt="Zyora App"
                  width={80}
                  height={80}
                  className="h-20 w-20 object-contain rounded-3xl shadow-[0_0_40px_rgba(37,99,235,0.4)]"
                />
                <h1 className="text-5xl font-bold leading-tight text-white md:text-6xl">
                  Zyora
                </h1>
              </div>
              
              <h2 className="text-2xl font-light text-gray-300 md:text-4xl">
                Party Rooms Chat
              </h2>
            </div>

            {/* Hero Image ka size increase kar diya */}
            <div className="md:w-1/2 flex justify-center md:justify-end">
              <Image 
                src="/file_00000000d60081fa99e9262352f29b19.png" 
                alt="Zyora App Screen" 
                width={380} 
                height={760} 
                className="object-contain rounded-2xl shadow-xl w-72 md:w-80 lg:w-[380px]"
              />
            </div>
          </div>
        </section>

        {/* 2. Invite Your Friend Card */}
        <section className="mx-auto mb-10 max-w-5xl overflow-hidden relative rounded-[32px] bg-[#0a0a0a] p-8 md:p-12 shadow-2xl scroll-mt-24">
          <div className="absolute -top-32 left-1/2 h-64 w-[150%] -translate-x-1/2 rounded-[100%] bg-blue-600/20 blur-[75px] pointer-events-none"></div>
          
          <div className="relative z-10 flex flex-col-reverse md:flex-row items-center justify-between gap-10 text-center md:text-left">
            <div className="md:w-3/5">
              <h2 className="mb-4 text-3xl font-bold text-white md:text-5xl leading-tight">
                Invite Your Friend <br /> And find your Honour
              </h2>
            </div>
            <div className="md:w-2/5 flex justify-center md:justify-end">
              <Image 
                src="/file_000000009bc481fa8cddb12eb18774a1.png" 
                alt="Invite Friend Screen" 
                width={240} 
                height={480} 
                className="object-contain rounded-2xl shadow-xl"
              />
            </div>
          </div>
        </section>

        {/* 3. Honour Events Card */}
        <section className="mx-auto mb-10 max-w-5xl overflow-hidden relative rounded-[32px] bg-[#0a0a0a] p-8 md:p-12 shadow-2xl scroll-mt-24">
          <div className="absolute -top-32 left-1/2 h-64 w-[150%] -translate-x-1/2 rounded-[100%] bg-blue-600/20 blur-[75px] pointer-events-none"></div>
          
          <div className="relative z-10 flex flex-col-reverse md:flex-row items-center justify-between gap-10 text-center md:text-left">
            <div className="md:w-3/5">
              <h2 className="mb-4 text-4xl font-bold text-white md:text-5xl">
                Honour Events
              </h2>
            </div>
            <div className="md:w-2/5 flex justify-center md:justify-end">
              <Image 
                src="/IMG_20260913_205804.png" 
                alt="Honour Events Screen" 
                width={240} 
                height={480} 
                className="object-contain rounded-2xl shadow-xl"
              />
            </div>
          </div>
        </section>

        {/* 4. Find Your Crowd Card */}
        <section className="mx-auto mb-10 max-w-5xl overflow-hidden relative rounded-[32px] bg-[#0a0a0a] p-8 md:p-12 shadow-2xl scroll-mt-24">
          <div className="absolute -top-32 left-1/2 h-64 w-[150%] -translate-x-1/2 rounded-[100%] bg-blue-600/20 blur-[75px] pointer-events-none"></div>
          
          <div className="relative z-10 flex flex-col-reverse md:flex-row items-center justify-between gap-10 text-center md:text-left">
            <div className="md:w-3/5">
              <h2 className="mb-4 text-3xl font-bold text-white">Find Your Crowd</h2>
              <p className="mb-6 text-lg font-light leading-relaxed text-gray-300">
                Whether you're discussing a punchline or sharing your own
                stories, Zyora helps you find your crowd and express yourself
                freely.
              </p>
              <p className="text-lg font-medium text-white">
                Join Zyora and make your day!
              </p>
            </div>
            <div className="md:w-2/5 flex justify-center md:justify-end">
              <Image 
                src="/file_00000000ab208211968341bc1564060f.png" 
                alt="Find Your Crowd Screen" 
                width={240} 
                height={480} 
                className="object-contain rounded-2xl shadow-xl"
              />
            </div>
          </div>
        </section>

        {/* 5. Key Features Card -> ID: feature */}
        <section id="feature" className="mx-auto mb-10 max-w-5xl overflow-hidden relative rounded-[32px] bg-[#0a0a0a] p-8 md:p-12 shadow-2xl scroll-mt-24">
          <div className="absolute -top-32 left-1/2 h-64 w-[150%] -translate-x-1/2 rounded-[100%] bg-blue-600/20 blur-[75px] pointer-events-none"></div>
          
          <div className="relative z-10">
            <SectionTitle title="Key Features" />

            <div className="grid grid-cols-1 gap-12 md:grid-cols-3">
              <FeatureCard
                image="/IMG_20260913_205718.png"
                title="Chat"
                text="Engage in lively video chats about the latest shows or dive into 1-on-1 private conversations."
              />

              <FeatureCard
                image="/IMG_20260913_205700.png"
                title="Share"
                text="Share your moments, from backstage snippets to stand-up clips with like-minded enthusiasts."
              />

              <FeatureCard
                image="/IMG_20260913_205733.png"
                title="Connect"
                text="Connect with creative souls in a vibrant stage designed for theater and comedy lovers."
              />
            </div>
          </div>
        </section>

        {/* 6. Community Card -> ID: about */}
        <section id="about" className="mx-auto mb-16 max-w-5xl overflow-hidden relative rounded-[32px] bg-[#0a0a0a] p-8 md:p-12 shadow-2xl scroll-mt-24">
          <div className="absolute -top-32 left-1/2 h-64 w-[150%] -translate-x-1/2 rounded-[100%] bg-blue-600/20 blur-[75px] pointer-events-none"></div>
          
          <div className="relative z-10">
            <SectionTitle title="Join Our Community" />

            <p className="mb-6 text-center text-lg font-light leading-relaxed text-gray-300 md:px-10">
              Zyora is more than an app—it's a home for creative expression.
              Whether you're a theater enthusiast, comedy lover, or just
              someone who appreciates authentic connections, you'll find your
              place here.
            </p>

            <p className="mb-16 text-center text-lg font-light leading-relaxed text-gray-300 md:px-10">
              Share your passion, discover new talents, and build meaningful
              relationships in a space designed for creativity.
            </p>

            <div className="grid grid-cols-1 gap-x-8 gap-y-12 sm:grid-cols-2 md:grid-cols-4">
              <CommunityItem
                image="/IMG_20260913_205638.png"
                title="Theater Lovers"
                text="Connect with fellow theater enthusiasts"
              />

              <CommunityItem
                image="/IMG_20260913_205620.png"
                title="Comedy Fans"
                text="Share laughs and discover new comedians"
              />

              <CommunityItem
                image="/IMG_20260913_205607.png"
                title="Performers"
                text="Showcase your talent and get feedback"
              />

              <CommunityItem
                image="/IMG_20260913_205553.png"
                title="Community"
                text="Build meaningful connections"
              />
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="bg-black px-6 py-10 text-center text-sm text-gray-500">
        <p className="mb-4 transition hover:text-white">
          Support: HurrySup@outlook.com
        </p>
        <p>© 2026 Zyora App. All rights reserved.</p>
      </footer>
    </div>
  );
}

/* ---------------- Components ---------------- */

function SectionTitle({ title }: { title: string }) {
  return (
    <div className="mb-12 text-center">
      <h2 className="mb-3 text-3xl font-bold text-white">{title}</h2>
      <div className="mx-auto h-1 w-16 rounded-full bg-blue-600 shadow-[0_0_10px_rgba(37,99,235,0.6)]" />
    </div>
  );
}

// Yahan se Background Card (bg-#141414, padding etc) hata diya aur Image chhoto kardi (80px)
function FeatureCard({
  image,
  title,
  text,
}: {
  image: string;
  title: string;
  text: string;
}) {
  return (
    <div className="text-center flex flex-col items-center transition hover:scale-105">
      <div className="mb-5 flex justify-center">
        <Image 
          src={image} 
          alt={title} 
          width={80} 
          height={80} 
          className="object-contain drop-shadow-[0_0_15px_rgba(37,99,235,0.3)]"
        />
      </div>
      <h3 className="mb-3 text-2xl font-bold text-white">{title}</h3>
      <p className="text-sm font-light text-gray-400 px-4">{text}</p>
    </div>
  );
}

function CommunityItem({
  image,
  title,
  text,
}: {
  image: string;
  title: string;
  text: string;
}) {
  return (
    <div className="text-center transition hover:scale-105 flex flex-col items-center">
      <div className="mb-4 flex justify-center">
        <Image 
          src={image} 
          alt={title} 
          width={90} 
          height={90} 
          className="object-contain drop-shadow-[0_0_15px_rgba(37,99,235,0.4)]"
        />
      </div>
      <h4 className="mb-2 text-xl font-bold text-white">{title}</h4>
      <p className="text-sm text-gray-400">{text}</p>
    </div>
  );
}

