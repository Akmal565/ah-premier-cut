package com.example.ui.components.elements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the bundled Shapes/Elements library contract: the original asset library
 * (formerly the Firebase upload set) ships inside the APK - 11 categories x 20
 * assets = 220 unique ids, every asset resolving to its bundled PNG thumbnail
 * and SVG master file under assets/elements/{thumbnails,files}/<category>/.
 *
 * Pure-Kotlin scope by design (no Android/compose classes): the enum- and
 * panel-level wiring is verified by the Task 29 static verification script and
 * by CI's compile + lint stages.
 */
class BundledElementsCatalogTest {

  private val expectedCategories = setOf(
    "shapes", "graphics", "frames", "tables", "3d", "icons",
    "animals", "birds", "characters", "vehicles", "charts"
  )

  @Test
  fun libraryShipsExactly220AssetsAcross11Categories() {
    assertEquals(220, BundledElementsCatalog.assets.size)
    val byCategory = BundledElementsCatalog.assets.groupBy { it.categoryId }
    assertEquals(expectedCategories, byCategory.keys)
    byCategory.forEach { (category, list) ->
      assertEquals("category $category must carry 20 assets", 20, list.size)
    }
  }

  @Test
  fun assetIdsAreUnique() {
    val ids = BundledElementsCatalog.assets.map { it.id }
    assertEquals(220, ids.distinct().size)
  }

  @Test
  fun thumbnailPathsFollowTheBundledCategoryConvention() {
    BundledElementsCatalog.assets.forEach { asset ->
      assertEquals(
        "elements/thumbnails/${asset.categoryId}/${asset.id}.png",
        BundledElementsCatalog.thumbnailAssetPath(asset.id)
      )
    }
  }

  @Test
  fun namesKindsAndColorsAreRealData() {
    BundledElementsCatalog.assets.forEach { asset ->
      assertTrue("blank name for ${asset.id}", asset.name.isNotBlank())
      assertTrue("blank kind for ${asset.id}", asset.kind.isNotBlank())
      assertTrue("missing tags for ${asset.id}", asset.tags.isNotEmpty())
      assertTrue("bad primary color for ${asset.id}", asset.primaryColor != 0L)
      assertTrue("bad secondary color for ${asset.id}", asset.secondaryColor != 0L)
    }
  }

  @Test
  fun unknownIdReturnsNull() {
    assertNull(BundledElementsCatalog.findById("shape_circle_solid"))
    assertNull(BundledElementsCatalog.thumbnailAssetPath("does_not_exist"))
  }
}
