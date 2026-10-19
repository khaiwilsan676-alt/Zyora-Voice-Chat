import { Analytics } from '@vercel/analytics/next'
import type { Metadata, Viewport } from 'next'
import './globals.css'
import StatusBarController from './StatusBarController'
import BackButtonController from './BackButtonController'

export const metadata: Metadata = {
  title: 'Zyora',

  description:
    'Zyora – connect with friends, join voice chat rooms, share moments, and discover new people.',

  robots: {
    index: false,
    follow: false,
  },

  icons: {
    icon: 'https://hurry-voice-chat.vercel.app/logo.png',
    apple: 'https://hurry-voice-chat.vercel.app/logo.png',
  },

  openGraph: {
    title: 'Zyora',
    description:
      'Join Zyora, connect with friends, and discover new people.',
    url: 'https://hurry-voice-chat.vercel.app/HurryOfficial',
    siteName: 'Zyora',
    images: [
      {
        url: 'https://hurry-voice-chat.vercel.app/logo.png',
        width: 550,
        height: 550,
        alt: 'Zyora',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },

  twitter: {
    card: 'summary_large_image',
    title: 'Zyora',
    description:
      'Join Zyora, connect with friends, and discover new people.',
    images: ['https://hurry-voice-chat.vercel.app/logo.png'],
  },
}

export const viewport: Viewport = {
  colorScheme: 'light',
  viewportFit: 'cover',
  themeColor: '#000000',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <head>
        <meta
          name="apple-mobile-web-app-capable"
          content="yes"
        />

        <meta
          name="apple-mobile-web-app-status-bar-style"
          content="black-translucent"
        />

        <meta
          name="apple-mobile-web-app-title"
          content="Zyora"
        />

        <meta
          name="theme-color"
          content="#000000"
        />

        <link
          rel="apple-touch-icon"
          href="https://hurry-voice-chat.vercel.app/logo.png"
        />

        <meta
          name="mobile-web-app-capable"
          content="yes"
        />

        {/* 🐛 Eruda — sirf /debug page pe load hoga */}
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function () {
                try {
                  if (window.location.pathname !== '/debug') return;
                  var s = document.createElement('script');
                  s.src = 'https://cdn.jsdelivr.net/npm/eruda';
                  s.async = true;
                  s.onload = function () {
                    if (window.eruda) {
                      window.eruda.init();
                      console.log('✅ Eruda ready');
                    }
                  };
                  document.head.appendChild(s);
                } catch (e) {}
              })();
            `,
          }}
        />
      </head>

      <script
        id="hurry-viewport-sync"
        dangerouslySetInnerHTML={{
          __html: `(function () {
            function syncViewport() {
              var vv = window.visualViewport;
              var height = vv ? vv.height : window.innerHeight;
              var width = vv ? vv.width : window.innerWidth;
              document.documentElement.style.setProperty('--app-viewport-height', height + 'px');
              document.documentElement.style.setProperty('--app-viewport-width', width + 'px');
            }
            syncViewport();
            window.addEventListener('resize', syncViewport, { passive: true });
            window.addEventListener('orientationchange', syncViewport, { passive: true });
            if (window.visualViewport) {
              window.visualViewport.addEventListener('resize', syncViewport, { passive: true });
            }
          })();`,
        }}
      />

      <body className="antialiased app-root bg-transparent">
        <StatusBarController />
        <BackButtonController />

        {children}

        {process.env.NODE_ENV === 'production' && <Analytics />}
      </body>
    </html>
  )
}
