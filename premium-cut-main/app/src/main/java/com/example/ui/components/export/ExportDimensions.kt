package com.example.ui.components.export

import com.example.domain.model.AspectRatio
import com.example.domain.model.Resolution

/**
 * Export pixel dimensions for a resolution/aspect pair. Kept as its own lightweight helper
 * after the duplicate export-configuration dialog was removed (the Export screen's single
 * inline configuration card is the one panel now); the export dimension math and its test
 * contract (ExportDimensionResolverTest / ExportSystemTest) are unchanged.
 */
internal fun calculateExportDimensions(res: Resolution, aspect: AspectRatio): Pair<Int, Int> =
  com.example.engine.export.ExportDimensionResolver.resolve(res, aspect)
