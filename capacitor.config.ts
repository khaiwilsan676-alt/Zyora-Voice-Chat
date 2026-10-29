import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.hawa.app',
  appName: 'Zyora',
  webDir: 'out',
  // Load the deployed Zyora app so server-side API routes and profile persistence work.
  server: {
    url: 'https://zyora-voice-chat.vercel.app',
    cleartext: false,
  },

  plugins: {
    StatusBar: {
      style: 'DARK',
      overlaysWebView: false,
      backgroundColor: '#00000000',
    },
    FirebaseAuthentication: {
      skipNativeAuth: false,
      providers: ['google.com'],
    },
  },
};

export default config;
