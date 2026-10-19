import { Capacitor } from '@capacitor/core';
import { StatusBar, Style } from '@capacitor/status-bar';

/**
 * Initialize Capacitor plugins on app startup
 */
export async function initializeCapacitor(): Promise<void> {
  if (Capacitor.isNativePlatform()) {
    try {
      // The user wants edge-to-edge layout (icons on top of page content)
      // but without forcing an explicit transparent background color, which might be crashing.
      // Overlay the web content so pages are full-screen under the status bar
      await StatusBar.setOverlaysWebView({ overlay: true });

      // Show the status bar so time/battery icons are visible, but overlaid on web content
      await StatusBar.setStyle({ style: Style.Dark });
      await StatusBar.show();
    } catch (error) {
      console.warn('Failed to initialize Capacitor plugins:', error);
    }
  }
}
