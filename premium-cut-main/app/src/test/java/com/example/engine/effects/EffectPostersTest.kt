package com.example.engine.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every production asset must ship its own real poster artwork: the Effects grid shows the
 * asset's actual look (Explosion -> fireball, Fire -> flames, Optical Scan -> the scan look),
 * never a clip video frame and never a shared placeholder. Pinned here because the tile
 * experience is exactly what users judge the asset library by.
 */
class EffectPostersTest {

  @Test
  fun everyProductionAssetShipsItsOwnRealPoster() {
    val catalog = ProductionEffectCatalog.effects
    assertTrue("catalog must not be empty", catalog.isNotEmpty())
    val missing = catalog.filter { EffectPosters.posterRes(it.id) == null }
    assertEquals(
      "assets without poster artwork: $missing",
      emptyList<String>(),
      missing.map { it.id }
    )
  }

  @Test
  fun posterCoverageMatchesCatalogExactly_NoStrayEntries() {
    val catalogIds = ProductionEffectCatalog.effects.map { it.id }.toSet()
    assertEquals(catalogIds, EffectPosters.coveredIds)
  }

  @Test
  fun everyPosterIsUniqueArtwork_NoSharedPlaceholder() {
    val resIds = ProductionEffectCatalog.effects.mapNotNull { EffectPosters.posterRes(it.id) }
    assertEquals(resIds.size, resIds.toSet().size)
  }

  @Test
  fun everyRenderableAssetResolvesItsPosterByShaderKey() {
    val byKey = ProductionEffectCatalog.effects.filter { !it.shaderKey.isNullOrBlank() }
    assertTrue(byKey.isNotEmpty())
    val missing = byKey.filter { EffectPosters.posterResByShaderKey(it.shaderKey!!) == null }
    assertEquals(emptyList<String>(), missing.map { it.id })
  }

  @Test
  fun coveredShaderKeysMirrorTheCatalog() {
    val keys = ProductionEffectCatalog.effects.mapNotNull { it.shaderKey }.toSet()
    assertEquals(keys, EffectPosters.coveredShaderKeys)
  }

  @Test
  fun unknownIdsAndKeysResolveToNull() {
    assertEquals(null, EffectPosters.posterRes("video.does_not_exist"))
    assertEquals(null, EffectPosters.posterResByShaderKey("vfx:trend.does_not_exist"))
    assertEquals(null, EffectPosters.posterResByShaderKey(""))
    assertNotNull(EffectPosters.posterRes("video.explosion"))
  }
}
