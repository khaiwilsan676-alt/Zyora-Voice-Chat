'use client';

import { useCallback } from 'react';
import Fruitparty from '@/components/Fruitparty';

declare global {
  interface Window {
    NativeFruitParty?: {
      close: () => void;
      minimize: () => void;
    };
  }
}

export default function NativeFruitPartyPage() {
  const close = useCallback(() => {
    if (window.NativeFruitParty) window.NativeFruitParty.close();
    else window.history.back();
  }, []);

  const minimize = useCallback(() => {
    window.NativeFruitParty?.minimize();
  }, []);

  return (
    <main className="fixed inset-0 h-[100dvh] w-full overflow-hidden bg-black">
      <Fruitparty onClose={close} onMinimize={minimize} isMinimized={false} />
    </main>
  );
}
