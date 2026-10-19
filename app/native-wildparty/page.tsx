'use client';

import { useCallback } from 'react';
import Wildparty from '@/components/Wildparty';

declare global {
  interface Window {
    NativeWildParty?: {
      close: () => void;
      minimize: () => void;
    };
  }
}

export default function NativeWildPartyPage() {
  const close = useCallback(() => {
    if (window.NativeWildParty) {
      window.NativeWildParty.close();
    } else {
      window.history.back();
    }
  }, []);

  const minimize = useCallback(() => {
    window.NativeWildParty?.minimize();
  }, []);

  return (
    <main className="fixed inset-0 h-[100dvh] w-full overflow-hidden bg-black">
      <Wildparty onClose={close} onMinimize={minimize} />
    </main>
  );
}
