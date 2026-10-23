package com.example.engine.effects

import com.example.domain.model.FilterSettings
import com.example.domain.model.FilterType

/**
 * Single source of truth for the Filters panel's selectable looks and the GPU
 * look extras that ColorGradingGlEffect (preview) and GpuCompositionRenderer
 * (export) share.
 *
 * Legacy [FilterType] values remain in the enum so saved projects deserialize and
 * still render. They are not offered as new selectable presets.
 */
object FilterCatalog {

  val SELECTABLE: List<FilterType> = listOf(
    FilterType.FOUR_K,
    FilterType.BLACKLIGHT_FIX,
    FilterType.GOLDEN_AUTUMN,
    FilterType.HDR,
    FilterType.ENHANCE,
    FilterType.GLOW
  )

  fun isSelectable(type: FilterType): Boolean =
    type == FilterType.NONE || type in SELECTABLE

  fun spatialExtras(filter: FilterSettings): FilterSpatialExtras {
    if (!filter.enabled || filter.type == FilterType.NONE || filter.intensity <= 0.01f) {
      return FilterSpatialExtras.NONE
    }
    val i = filter.intensity.coerceIn(0f, 1f)
    return when (filter.type) {
      FilterType.FOUR_K -> FilterSpatialExtras(
        sharpness = 0.55f * i,
        clarity = 0.40f * i
      )
      FilterType.ENHANCE -> FilterSpatialExtras(
        sharpness = 0.38f * i,
        clarity = 0.32f * i,
        enhance = 0.45f * i
      )
      FilterType.HDR -> FilterSpatialExtras(
        hdrLook = 0.70f * i,
        clarity = 0.22f * i
      )
      FilterType.GLOW -> FilterSpatialExtras(
        glow = 0.85f * i
      )
      else -> FilterSpatialExtras.NONE
    }
  }
}

data class FilterSpatialExtras(
  val sharpness: Float = 0f,
  val clarity: Float = 0f,
  val glow: Float = 0f,
  val hdrLook: Float = 0f,
  val enhance: Float = 0f
) {
  companion object {
    val NONE = FilterSpatialExtras()
  }

  fun isActive(): Boolean =
    sharpness > 0.01f || clarity > 0.01f || glow > 0.01f || hdrLook > 0.01f || enhance > 0.01f
}
