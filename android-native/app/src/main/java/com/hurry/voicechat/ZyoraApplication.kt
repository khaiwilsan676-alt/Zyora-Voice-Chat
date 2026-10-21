package com.hurry.voicechat

import android.app.Application
import coil3.ImageLoader
import coil3.ImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache

/**
 * Shared image loader for the native app.
 * Reuses room avatars, banners and room backgrounds between screens and room re-entry,
 * reducing repeated network downloads, decode work and scrolling stutter.
 */
class ZyoraApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this@ZyoraApplication, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("zyora_image_cache"))
                    .maxSizeBytes(96L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
