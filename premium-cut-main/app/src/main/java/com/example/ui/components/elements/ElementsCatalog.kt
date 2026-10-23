package com.example.ui.components.elements

/**
 * The Shapes/Elements panel catalog - backed by the 220 original bundled
 * library assets (11 categories x 20) shipped in the APK under
 * assets/elements/thumbnails/<category>/.
 *
 * Every item carries its original PNG thumbnail (`bundledThumbnailAsset`), so
 * the grid and the video canvas both render the real asset instead of a
 * procedural stand-in.
 */
object ElementsCatalog {

  val allElements: List<ElementItem> by lazy {
    BundledElementsCatalog.assets.map { asset ->
      val category = ElementCategory.values().first { it.id == asset.categoryId }
      ElementItem(
        id = asset.id,
        title = asset.name,
        subtitle = "${category.displayName} • Original asset",
        category = category,
        tags = asset.tags,
        iconSymbol = categoryGlyph(category),
        vectorType = asset.kind,
        primaryColor = asset.primaryColor,
        secondaryColor = asset.secondaryColor,
        bundledThumbnailAsset = BundledElementsCatalog.thumbnailAssetPath(asset.id)
      )
    }
  }

  fun getByCategory(category: ElementCategory): List<ElementItem> {
    return allElements.filter { it.category == category }
  }

  fun search(query: String): List<ElementItem> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return allElements
    return allElements.filter { item ->
      item.title.lowercase().contains(q) ||
      item.subtitle.lowercase().contains(q) ||
      item.category.displayName.lowercase().contains(q) ||
      item.tags.any { it.lowercase().contains(q) }
    }
  }

  fun findById(id: String): ElementItem? {
    return allElements.find { it.id == id }
  }

  /** Neutral per-category glyph, only used as the last-resort fallback label. */
  private fun categoryGlyph(category: ElementCategory): String = when (category) {
    ElementCategory.SHAPES -> "⬡"
    ElementCategory.GRAPHICS -> "✦"
    ElementCategory.FRAMES -> "▣"
    ElementCategory.TABLES -> "▦"
    ElementCategory.THREE_D -> "◈"
    ElementCategory.ICONS -> "★"
    ElementCategory.ANIMALS -> "🐾"
    ElementCategory.BIRDS -> "🐦"
    ElementCategory.CHARACTERS -> "👤"
    ElementCategory.VEHICLES -> "🚗"
    ElementCategory.CHARTS -> "📊"
  }
}
