package com.example.ui.components.elements

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.StudioApplication

/**
 * Decodes and caches the bundled original PNG thumbnails of the Shapes/Elements
 * library so both the panel grid and the video-canvas renderer draw the real
 * asset (never a procedural stand-in) with zero Firebase dependency.
 *
 * Thumbnails are 256px originals (~256KB decoded each); the cache holds a small
 * working set and re-decodes on eviction. Init happens in [com.example.StudioApplication];
 * before init (or on JVM unit tests) lookups return null and callers fall back.
 */
object ElementAssetCache {
  private const val MAX_CACHE_KB = 8 * 1024 // 8 MB working set

  @Volatile private var appContext: Context? = null

  private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_KB) {
    override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
  }

  /** Idempotent; called once from StudioApplication.onCreate. */
  fun init(context: Context) {
    appContext = context.applicationContext
  }

  private fun contextOrNull(): Context? {
    appContext?.let { return it }
    return runCatching { StudioApplication.instance.applicationContext }.getOrNull()
  }

  /**
   * The original bundled thumbnail bitmap for a library asset id, e.g.
   * "shapes_star" -> assets/elements/thumbnails/shapes/shapes_star.png.
   * Null when the id is unknown, assets are unavailable, or decode fails.
   */
  fun bitmapFor(elementId: String): Bitmap? {
    val ctx = contextOrNull() ?: return null
    val path = BundledElementsCatalog.thumbnailAssetPath(elementId) ?: return null
    cache.get(path)?.let { return it }
    return runCatching {
      val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
      var bmp: Bitmap? = null
      ctx.assets.open(path).use { input ->
        bmp = BitmapFactory.decodeStream(input, null, opts)
      }
      bmp?.also { cache.put(path, it) }
    }.getOrNull()
  }
}
