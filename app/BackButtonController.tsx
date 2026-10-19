'use client'

import { useEffect } from 'react'
import { App } from '@capacitor/app'

export default function BackButtonController() {
  useEffect(() => {
    let listenerHandle: any = null;

    const setupBackButton = async () => {
      try {
        listenerHandle = await App.addListener('backButton', () => {
          const customEvent = new CustomEvent('hardwareBackPress', {
            cancelable: true
          });

          window.dispatchEvent(customEvent);

          if (!customEvent.defaultPrevented) {
            App.minimizeApp().catch((err: any) => console.warn('Minimize app failed', err));
          }
        });
      } catch (error) {
        console.warn('App plugin not available', error);
      }
    };

    setupBackButton();

    return () => {
      if (listenerHandle) {
        listenerHandle.remove().catch(() => {});
      }
    };
  }, []);

  return null;
}
