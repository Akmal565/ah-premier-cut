package com.example.ui.components.effects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.engine.effects.EffectPosters

/**
 * Decodes the bundled real poster artwork once per asset at swatch resolution and keeps it in a
 * small LruCache. Posters are the base the live shader swatch animates, decoded at ~2x the
 * 96px swatch raster so the shader has detail to work with: a few ms per asset, lazily on
 * first composition of the tile, never re-decoded while the panel is open.
 */
object EffectPosterCache {

  private const val TARGET_PX = 192

  /** Count-based cache: 24 posters at 256px ARGB ~= 6 MB worst case, far above panel need. */
  private val cache = object : LruCache<String, Bitmap>(24) {
    override fun sizeOf(key: String, value: Bitmap): Int = 1
  }

  /** Poster bitmap for a catalog asset id (Effects tools grid path), or null when none ships. */
  fun forEffectId(context: Context, effectId: String): Bitmap? {
    val res = EffectPosters.posterRes(effectId) ?: return null
    return forRes(context, "id:$effectId", res)
  }

  /** Poster bitmap for an asset shader key (Effect Stack panel path), or null when none ships. */
  fun forShaderKey(context: Context, shaderKey: String): Bitmap? {
    val res = EffectPosters.posterResByShaderKey(shaderKey) ?: return null
    return forRes(context, "key:$shaderKey", res)
  }

  private fun forRes(context: Context, cacheKey: String, resId: Int): Bitmap? =
    cache.get(cacheKey) ?: runCatching {
      decode(context, resId)?.also { cache.put(cacheKey, it) }
    }.getOrNull()

  private fun decode(context: Context, resId: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= TARGET_PX) sample *= 2
    return BitmapFactory.decodeResource(
      context.resources, resId,
      BitmapFactory.Options().apply { inSampleSize = sample }
    )
  }
}
