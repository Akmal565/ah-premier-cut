package com.example.engine.effects

import com.example.R

/**
 * Real artwork for every production asset tile. Each entry is an actual picture of the effect
 * itself — the Explosion tile shows a fireball, Fire shows flames, Optical Scan shows the scan
 * look, Wink shows a wink — bundled under `res/drawable-nodpi`. This replaces the old
 * clip-frame thumbnails: a tile must communicate the asset's own look, never the video it
 * happens to sit under.
 *
 * The poster is the BASE the live shader swatch animates on ([EffectLiveSwatch] path), so a
 * tile is both immediately recognizable (poster, frame zero) and genuinely animated (the real
 * effect shader runs over it at panel refresh, GPU with CPU-raster fallback).
 *
 * Compile-time safety: every mapping references `R.drawable` directly, so a missing or
 * misnamed bundled poster fails the build. [EffectPostersTest] additionally pins full catalog
 * coverage (37/37) and one-to-one artwork uniqueness.
 */
object EffectPosters {

  private val byId: Map<String, Int> = mapOf(
    // Video Effects (9)
    "video.explosion" to R.drawable.fx_poster_video_explosion,
    "video.ink_spill" to R.drawable.fx_poster_video_ink_spill,
    "video.rotating_earth" to R.drawable.fx_poster_video_earth,
    "video.magical_tome" to R.drawable.fx_poster_video_magical_tome,
    "video.motion_blur" to R.drawable.fx_poster_video_motion_blur,
    "video.cut_shift" to R.drawable.fx_poster_video_cut_shift,
    "video.fire" to R.drawable.fx_poster_video_fire,
    "video.mini_zoom" to R.drawable.fx_poster_video_mini_zoom,
    "video.white_flash" to R.drawable.fx_poster_video_white_flash,
    // Body Effects (9)
    "body.optical_scan" to R.drawable.fx_poster_body_optical_scan,
    "body.phantom_clone" to R.drawable.fx_poster_body_phantom_clone,
    "body.light_trails" to R.drawable.fx_poster_body_light_trails,
    "body.pixel_mask" to R.drawable.fx_poster_body_pixel_mask,
    "body.action_paint" to R.drawable.fx_poster_body_action_paint,
    "body.face_scan" to R.drawable.fx_poster_body_face_scan,
    "body.light_dissolve" to R.drawable.fx_poster_body_light_dissolve,
    "body.luminescence" to R.drawable.fx_poster_body_luminescence,
    "body.hand_drawn" to R.drawable.fx_poster_body_hand_drawn,
    // Face Effects (5)
    "face.funny" to R.drawable.fx_poster_face_funny,
    "face.alien_eyes" to R.drawable.fx_poster_face_alien_eyes,
    "face.small_face" to R.drawable.fx_poster_face_small_face,
    "face.melting" to R.drawable.fx_poster_face_melting,
    "face.chubby" to R.drawable.fx_poster_face_chubby,
    // Photo Effects (10)
    "photo.expand_x" to R.drawable.fx_poster_photo_expand_x,
    "photo.orbit360" to R.drawable.fx_poster_photo_orbit360,
    "photo.perm_curls" to R.drawable.fx_poster_photo_perm_curls,
    "photo.wink" to R.drawable.fx_poster_photo_wink,
    "photo.layer" to R.drawable.fx_poster_photo_layer,
    "photo.doll" to R.drawable.fx_poster_photo_doll,
    "photo.zoom3d" to R.drawable.fx_poster_photo_zoom3d,
    "photo.ai_filter" to R.drawable.fx_poster_photo_ai_filter,
    "photo.sad" to R.drawable.fx_poster_photo_sad,
    "photo.cartoon3d" to R.drawable.fx_poster_photo_cartoon3d,
    // AI Effects (4)
    "ai.shadow_clone" to R.drawable.fx_poster_ai_shadow_clone,
    "ai.golden_statue" to R.drawable.fx_poster_ai_golden_statue,
    "ai.cyber_neon" to R.drawable.fx_poster_ai_cyber_neon,
    "ai.cartoon_character" to R.drawable.fx_poster_ai_cartoon_character,
  )

  private val byShader: Map<String, Int> by lazy {
    ProductionEffectCatalog.effects.mapNotNull { effect ->
      val key = effect.shaderKey ?: return@mapNotNull null
      byId[effect.id]?.let { key to it }
    }.toMap()
  }

  /** Poster drawable for a catalog asset id, e.g. `"video.explosion"`. */
  fun posterRes(effectId: String): Int? = byId[effectId]

  /** Poster drawable for an asset shader key, e.g. `"vfx:trend.explosion"` or `"face:funny"`. */
  fun posterResByShaderKey(shaderKey: String): Int? = byShader[shaderKey]

  /** Catalog asset ids that ship real poster artwork. */
  val coveredIds: Set<String> get() = byId.keys

  /** Shader keys that resolve to poster artwork (derived from the production catalog). */
  val coveredShaderKeys: Set<String> get() = byShader.keys
}
