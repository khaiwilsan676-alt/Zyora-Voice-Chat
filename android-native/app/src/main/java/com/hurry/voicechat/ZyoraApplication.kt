package com.hurry.voicechat

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toPath

/**
 * Shared image loader for the native app.
 * Reuses room avatars, banners and room backgrounds between screens and room re-entry,
 * reducing repeated network downloads, decode work and scrolling stutter.
 */
class ZyoraApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("zyora_image_cache").absolutePath.toPath())
                    .maxSizeBytes(96L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
