package com.example.ui.components.elements

/**
 * The original Shapes/Elements asset library - 11 categories x 20 assets = 220
 * assets - bundled directly inside the APK (no Firebase round-trip).
 *
 * Assets live under:
 *   assets/elements/thumbnails/<category>/<id>.png  (256px original render, grid + canvas)
 *
 * The list below is generated from the original library seed at generation
 * time; ids map 1:1 to the bundled file names.
 */
data class BundledElementAsset(
  val id: String,
  val name: String,
  val categoryId: String,
  val kind: String,
  val primaryColor: Long,
  val secondaryColor: Long,
  val tags: List<String>
)

object BundledElementsCatalog {
  const val THUMBNAIL_PREFIX = "elements/thumbnails/"

  val assets: List<BundledElementAsset> = listOf(
    BundledElementAsset(
      id = "shapes_solid_circle",
      name = "Solid Circle",
      categoryId = "shapes",
      kind = "circle",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("shapes", "circle", "solid circle")
    ),
    BundledElementAsset(
      id = "shapes_circle_ring",
      name = "Circle Ring",
      categoryId = "shapes",
      kind = "ring",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("shapes", "ring", "circle ring")
    ),
    BundledElementAsset(
      id = "shapes_rounded_square",
      name = "Rounded Square",
      categoryId = "shapes",
      kind = "square",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("shapes", "square", "rounded square")
    ),
    BundledElementAsset(
      id = "shapes_wide_rectangle",
      name = "Wide Rectangle",
      categoryId = "shapes",
      kind = "rect",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("shapes", "rect", "wide rectangle")
    ),
    BundledElementAsset(
      id = "shapes_triangle",
      name = "Triangle",
      categoryId = "shapes",
      kind = "triangle",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("shapes", "triangle", "triangle")
    ),
    BundledElementAsset(
      id = "shapes_diamond",
      name = "Diamond",
      categoryId = "shapes",
      kind = "diamond",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("shapes", "diamond", "diamond")
    ),
    BundledElementAsset(
      id = "shapes_pentagon",
      name = "Pentagon",
      categoryId = "shapes",
      kind = "pentagon",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("shapes", "pentagon", "pentagon")
    ),
    BundledElementAsset(
      id = "shapes_hexagon",
      name = "Hexagon",
      categoryId = "shapes",
      kind = "hexagon",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("shapes", "hexagon", "hexagon")
    ),
    BundledElementAsset(
      id = "shapes_octagon",
      name = "Octagon",
      categoryId = "shapes",
      kind = "octagon",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("shapes", "octagon", "octagon")
    ),
    BundledElementAsset(
      id = "shapes_star",
      name = "Star",
      categoryId = "shapes",
      kind = "star",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("shapes", "star", "star")
    ),
    BundledElementAsset(
      id = "shapes_heart",
      name = "Heart",
      categoryId = "shapes",
      kind = "heart",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("shapes", "heart", "heart")
    ),
    BundledElementAsset(
      id = "shapes_plus_cross",
      name = "Plus Cross",
      categoryId = "shapes",
      kind = "cross",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("shapes", "cross", "plus cross")
    ),
    BundledElementAsset(
      id = "shapes_arrow_right",
      name = "Arrow Right",
      categoryId = "shapes",
      kind = "arrow",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("shapes", "arrow", "arrow right")
    ),
    BundledElementAsset(
      id = "shapes_chevron",
      name = "Chevron",
      categoryId = "shapes",
      kind = "chevron",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("shapes", "chevron", "chevron")
    ),
    BundledElementAsset(
      id = "shapes_organic_blob",
      name = "Organic Blob",
      categoryId = "shapes",
      kind = "blob",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("shapes", "blob", "organic blob")
    ),
    BundledElementAsset(
      id = "shapes_half_circle",
      name = "Half Circle",
      categoryId = "shapes",
      kind = "half",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("shapes", "half", "half circle")
    ),
    BundledElementAsset(
      id = "shapes_water_drop",
      name = "Water Drop",
      categoryId = "shapes",
      kind = "drop",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("shapes", "drop", "water drop")
    ),
    BundledElementAsset(
      id = "shapes_crescent_moon",
      name = "Crescent Moon",
      categoryId = "shapes",
      kind = "crescent",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("shapes", "crescent", "crescent moon")
    ),
    BundledElementAsset(
      id = "shapes_pill_label",
      name = "Pill Label",
      categoryId = "shapes",
      kind = "pill",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("shapes", "pill", "pill label")
    ),
    BundledElementAsset(
      id = "shapes_badge_circle",
      name = "Badge Circle",
      categoryId = "shapes",
      kind = "seal",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("shapes", "seal", "badge circle")
    ),
    BundledElementAsset(
      id = "graphics_starburst_badge",
      name = "Starburst Badge",
      categoryId = "graphics",
      kind = "burst",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("graphics", "burst", "starburst badge")
    ),
    BundledElementAsset(
      id = "graphics_award_seal",
      name = "Award Seal",
      categoryId = "graphics",
      kind = "seal",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("graphics", "seal", "award seal")
    ),
    BundledElementAsset(
      id = "graphics_price_tag",
      name = "Price Tag",
      categoryId = "graphics",
      kind = "tag",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("graphics", "tag", "price tag")
    ),
    BundledElementAsset(
      id = "graphics_ribbon_banner",
      name = "Ribbon Banner",
      categoryId = "graphics",
      kind = "ribbon",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("graphics", "ribbon", "ribbon banner")
    ),
    BundledElementAsset(
      id = "graphics_sparkle_burst",
      name = "Sparkle Burst",
      categoryId = "graphics",
      kind = "sparkle",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("graphics", "sparkle", "sparkle burst")
    ),
    BundledElementAsset(
      id = "graphics_wave_banner",
      name = "Wave Banner",
      categoryId = "graphics",
      kind = "banner",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("graphics", "banner", "wave banner")
    ),
    BundledElementAsset(
      id = "graphics_confetti_pop",
      name = "Confetti Pop",
      categoryId = "graphics",
      kind = "confetti",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("graphics", "confetti", "confetti pop")
    ),
    BundledElementAsset(
      id = "graphics_swirl_spiral",
      name = "Swirl Spiral",
      categoryId = "graphics",
      kind = "swirl",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("graphics", "swirl", "swirl spiral")
    ),
    BundledElementAsset(
      id = "graphics_sun_rays",
      name = "Sun Rays",
      categoryId = "graphics",
      kind = "rays",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("graphics", "rays", "sun rays")
    ),
    BundledElementAsset(
      id = "graphics_quote_bubble",
      name = "Quote Bubble",
      categoryId = "graphics",
      kind = "quote",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("graphics", "quote", "quote bubble")
    ),
    BundledElementAsset(
      id = "graphics_pill_label_art",
      name = "Pill Label Art",
      categoryId = "graphics",
      kind = "label",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("graphics", "label", "pill label art")
    ),
    BundledElementAsset(
      id = "graphics_ocean_wave",
      name = "Ocean Wave",
      categoryId = "graphics",
      kind = "wave",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("graphics", "wave", "ocean wave")
    ),
    BundledElementAsset(
      id = "graphics_coral_burst",
      name = "Coral Burst",
      categoryId = "graphics",
      kind = "burst",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("graphics", "burst", "coral burst")
    ),
    BundledElementAsset(
      id = "graphics_mint_seal",
      name = "Mint Seal",
      categoryId = "graphics",
      kind = "seal",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("graphics", "seal", "mint seal")
    ),
    BundledElementAsset(
      id = "graphics_sunset_tag",
      name = "Sunset Tag",
      categoryId = "graphics",
      kind = "tag",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("graphics", "tag", "sunset tag")
    ),
    BundledElementAsset(
      id = "graphics_violet_ribbon",
      name = "Violet Ribbon",
      categoryId = "graphics",
      kind = "ribbon",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("graphics", "ribbon", "violet ribbon")
    ),
    BundledElementAsset(
      id = "graphics_golden_sparkle",
      name = "Golden Sparkle",
      categoryId = "graphics",
      kind = "sparkle",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("graphics", "sparkle", "golden sparkle")
    ),
    BundledElementAsset(
      id = "graphics_pink_banner",
      name = "Pink Banner",
      categoryId = "graphics",
      kind = "banner",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("graphics", "banner", "pink banner")
    ),
    BundledElementAsset(
      id = "graphics_teal_confetti",
      name = "Teal Confetti",
      categoryId = "graphics",
      kind = "confetti",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("graphics", "confetti", "teal confetti")
    ),
    BundledElementAsset(
      id = "graphics_neon_swirl",
      name = "Neon Swirl",
      categoryId = "graphics",
      kind = "swirl",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("graphics", "swirl", "neon swirl")
    ),
    BundledElementAsset(
      id = "frames_circle_frame",
      name = "Circle Frame",
      categoryId = "frames",
      kind = "circle",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("frames", "circle", "circle frame")
    ),
    BundledElementAsset(
      id = "frames_rounded_frame",
      name = "Rounded Frame",
      categoryId = "frames",
      kind = "rounded",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("frames", "rounded", "rounded frame")
    ),
    BundledElementAsset(
      id = "frames_double_border_frame",
      name = "Double Border Frame",
      categoryId = "frames",
      kind = "double",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("frames", "double", "double border frame")
    ),
    BundledElementAsset(
      id = "frames_polaroid_frame",
      name = "Polaroid Frame",
      categoryId = "frames",
      kind = "polaroid",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("frames", "polaroid", "polaroid frame")
    ),
    BundledElementAsset(
      id = "frames_film_strip_frame",
      name = "Film Strip Frame",
      categoryId = "frames",
      kind = "film",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("frames", "film", "film strip frame")
    ),
    BundledElementAsset(
      id = "frames_ornate_corner_frame",
      name = "Ornate Corner Frame",
      categoryId = "frames",
      kind = "ornate",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("frames", "ornate", "ornate corner frame")
    ),
    BundledElementAsset(
      id = "frames_corner_marks_frame",
      name = "Corner Marks Frame",
      categoryId = "frames",
      kind = "corner",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("frames", "corner", "corner marks frame")
    ),
    BundledElementAsset(
      id = "frames_cinematic_wide_frame",
      name = "Cinematic Wide Frame",
      categoryId = "frames",
      kind = "wide",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("frames", "wide", "cinematic wide frame")
    ),
    BundledElementAsset(
      id = "frames_portrait_frame",
      name = "Portrait Frame",
      categoryId = "frames",
      kind = "portrait",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("frames", "portrait", "portrait frame")
    ),
    BundledElementAsset(
      id = "frames_dashed_frame",
      name = "Dashed Frame",
      categoryId = "frames",
      kind = "dashed",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("frames", "dashed", "dashed frame")
    ),
    BundledElementAsset(
      id = "frames_thin_ring_frame",
      name = "Thin Ring Frame",
      categoryId = "frames",
      kind = "circle",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("frames", "circle", "thin ring frame")
    ),
    BundledElementAsset(
      id = "frames_soft_rounded_frame",
      name = "Soft Rounded Frame",
      categoryId = "frames",
      kind = "rounded",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("frames", "rounded", "soft rounded frame")
    ),
    BundledElementAsset(
      id = "frames_classic_double_frame",
      name = "Classic Double Frame",
      categoryId = "frames",
      kind = "double",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("frames", "double", "classic double frame")
    ),
    BundledElementAsset(
      id = "frames_instant_photo_frame",
      name = "Instant Photo Frame",
      categoryId = "frames",
      kind = "polaroid",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("frames", "polaroid", "instant photo frame")
    ),
    BundledElementAsset(
      id = "frames_cinema_film_frame",
      name = "Cinema Film Frame",
      categoryId = "frames",
      kind = "film",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("frames", "film", "cinema film frame")
    ),
    BundledElementAsset(
      id = "frames_royal_ornate_frame",
      name = "Royal Ornate Frame",
      categoryId = "frames",
      kind = "ornate",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("frames", "ornate", "royal ornate frame")
    ),
    BundledElementAsset(
      id = "frames_focus_corners_frame",
      name = "Focus Corners Frame",
      categoryId = "frames",
      kind = "corner",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("frames", "corner", "focus corners frame")
    ),
    BundledElementAsset(
      id = "frames_widescreen_frame",
      name = "Widescreen Frame",
      categoryId = "frames",
      kind = "wide",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("frames", "wide", "widescreen frame")
    ),
    BundledElementAsset(
      id = "frames_story_portrait_frame",
      name = "Story Portrait Frame",
      categoryId = "frames",
      kind = "portrait",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("frames", "portrait", "story portrait frame")
    ),
    BundledElementAsset(
      id = "frames_stitch_dashed_frame",
      name = "Stitch Dashed Frame",
      categoryId = "frames",
      kind = "dashed",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("frames", "dashed", "stitch dashed frame")
    ),
    BundledElementAsset(
      id = "tables_data_table_1",
      name = "Data Table 1",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("tables", "grid", "data table 1")
    ),
    BundledElementAsset(
      id = "tables_data_table_2",
      name = "Data Table 2",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("tables", "grid", "data table 2")
    ),
    BundledElementAsset(
      id = "tables_data_table_3",
      name = "Data Table 3",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("tables", "grid", "data table 3")
    ),
    BundledElementAsset(
      id = "tables_data_table_4",
      name = "Data Table 4",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("tables", "grid", "data table 4")
    ),
    BundledElementAsset(
      id = "tables_data_table_5",
      name = "Data Table 5",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("tables", "grid", "data table 5")
    ),
    BundledElementAsset(
      id = "tables_data_table_6",
      name = "Data Table 6",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("tables", "grid", "data table 6")
    ),
    BundledElementAsset(
      id = "tables_data_table_7",
      name = "Data Table 7",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("tables", "grid", "data table 7")
    ),
    BundledElementAsset(
      id = "tables_data_table_8",
      name = "Data Table 8",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("tables", "grid", "data table 8")
    ),
    BundledElementAsset(
      id = "tables_data_table_9",
      name = "Data Table 9",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("tables", "grid", "data table 9")
    ),
    BundledElementAsset(
      id = "tables_data_table_10",
      name = "Data Table 10",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("tables", "grid", "data table 10")
    ),
    BundledElementAsset(
      id = "tables_data_table_11",
      name = "Data Table 11",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("tables", "grid", "data table 11")
    ),
    BundledElementAsset(
      id = "tables_data_table_12",
      name = "Data Table 12",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("tables", "grid", "data table 12")
    ),
    BundledElementAsset(
      id = "tables_data_table_13",
      name = "Data Table 13",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("tables", "grid", "data table 13")
    ),
    BundledElementAsset(
      id = "tables_data_table_14",
      name = "Data Table 14",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("tables", "grid", "data table 14")
    ),
    BundledElementAsset(
      id = "tables_data_table_15",
      name = "Data Table 15",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("tables", "grid", "data table 15")
    ),
    BundledElementAsset(
      id = "tables_data_table_16",
      name = "Data Table 16",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("tables", "grid", "data table 16")
    ),
    BundledElementAsset(
      id = "tables_data_table_17",
      name = "Data Table 17",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("tables", "grid", "data table 17")
    ),
    BundledElementAsset(
      id = "tables_data_table_18",
      name = "Data Table 18",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("tables", "grid", "data table 18")
    ),
    BundledElementAsset(
      id = "tables_data_table_19",
      name = "Data Table 19",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("tables", "grid", "data table 19")
    ),
    BundledElementAsset(
      id = "tables_data_table_20",
      name = "Data Table 20",
      categoryId = "tables",
      kind = "grid",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("tables", "grid", "data table 20")
    ),
    BundledElementAsset(
      id = "3d_isometric_cube",
      name = "Isometric Cube",
      categoryId = "3d",
      kind = "cube",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("3d", "cube", "isometric cube")
    ),
    BundledElementAsset(
      id = "3d_glossy_sphere",
      name = "Glossy Sphere",
      categoryId = "3d",
      kind = "sphere",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("3d", "sphere", "glossy sphere")
    ),
    BundledElementAsset(
      id = "3d_pyramid",
      name = "Pyramid",
      categoryId = "3d",
      kind = "pyramid",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("3d", "pyramid", "pyramid")
    ),
    BundledElementAsset(
      id = "3d_cylinder",
      name = "Cylinder",
      categoryId = "3d",
      kind = "cylinder",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("3d", "cylinder", "cylinder")
    ),
    BundledElementAsset(
      id = "3d_cone",
      name = "Cone",
      categoryId = "3d",
      kind = "cone",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("3d", "cone", "cone")
    ),
    BundledElementAsset(
      id = "3d_torus_ring",
      name = "Torus Ring",
      categoryId = "3d",
      kind = "torus",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("3d", "torus", "torus ring")
    ),
    BundledElementAsset(
      id = "3d_glass_prism",
      name = "Glass Prism",
      categoryId = "3d",
      kind = "prism",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("3d", "prism", "glass prism")
    ),
    BundledElementAsset(
      id = "3d_sunset_cube",
      name = "Sunset Cube",
      categoryId = "3d",
      kind = "cube",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("3d", "cube", "sunset cube")
    ),
    BundledElementAsset(
      id = "3d_ocean_sphere",
      name = "Ocean Sphere",
      categoryId = "3d",
      kind = "sphere",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("3d", "sphere", "ocean sphere")
    ),
    BundledElementAsset(
      id = "3d_sand_pyramid",
      name = "Sand Pyramid",
      categoryId = "3d",
      kind = "pyramid",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("3d", "pyramid", "sand pyramid")
    ),
    BundledElementAsset(
      id = "3d_steel_cylinder",
      name = "Steel Cylinder",
      categoryId = "3d",
      kind = "cylinder",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("3d", "cylinder", "steel cylinder")
    ),
    BundledElementAsset(
      id = "3d_coral_cone",
      name = "Coral Cone",
      categoryId = "3d",
      kind = "cone",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("3d", "cone", "coral cone")
    ),
    BundledElementAsset(
      id = "3d_violet_torus",
      name = "Violet Torus",
      categoryId = "3d",
      kind = "torus",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("3d", "torus", "violet torus")
    ),
    BundledElementAsset(
      id = "3d_crystal_prism",
      name = "Crystal Prism",
      categoryId = "3d",
      kind = "prism",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("3d", "prism", "crystal prism")
    ),
    BundledElementAsset(
      id = "3d_mint_cube",
      name = "Mint Cube",
      categoryId = "3d",
      kind = "cube",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("3d", "cube", "mint cube")
    ),
    BundledElementAsset(
      id = "3d_rose_sphere",
      name = "Rose Sphere",
      categoryId = "3d",
      kind = "sphere",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("3d", "sphere", "rose sphere")
    ),
    BundledElementAsset(
      id = "3d_amber_pyramid",
      name = "Amber Pyramid",
      categoryId = "3d",
      kind = "pyramid",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("3d", "pyramid", "amber pyramid")
    ),
    BundledElementAsset(
      id = "3d_teal_cylinder",
      name = "Teal Cylinder",
      categoryId = "3d",
      kind = "cylinder",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("3d", "cylinder", "teal cylinder")
    ),
    BundledElementAsset(
      id = "3d_indigo_cone",
      name = "Indigo Cone",
      categoryId = "3d",
      kind = "cone",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("3d", "cone", "indigo cone")
    ),
    BundledElementAsset(
      id = "3d_lime_torus",
      name = "Lime Torus",
      categoryId = "3d",
      kind = "torus",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("3d", "torus", "lime torus")
    ),
    BundledElementAsset(
      id = "icons_heart_icon",
      name = "Heart Icon",
      categoryId = "icons",
      kind = "heart",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("icons", "heart", "heart icon")
    ),
    BundledElementAsset(
      id = "icons_star_icon",
      name = "Star Icon",
      categoryId = "icons",
      kind = "star",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("icons", "star", "star icon")
    ),
    BundledElementAsset(
      id = "icons_play_icon",
      name = "Play Icon",
      categoryId = "icons",
      kind = "play",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("icons", "play", "play icon")
    ),
    BundledElementAsset(
      id = "icons_microphone_icon",
      name = "Microphone Icon",
      categoryId = "icons",
      kind = "mic",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("icons", "mic", "microphone icon")
    ),
    BundledElementAsset(
      id = "icons_camera_icon",
      name = "Camera Icon",
      categoryId = "icons",
      kind = "camera",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("icons", "camera", "camera icon")
    ),
    BundledElementAsset(
      id = "icons_bell_icon",
      name = "Bell Icon",
      categoryId = "icons",
      kind = "bell",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("icons", "bell", "bell icon")
    ),
    BundledElementAsset(
      id = "icons_home_icon",
      name = "Home Icon",
      categoryId = "icons",
      kind = "home",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("icons", "home", "home icon")
    ),
    BundledElementAsset(
      id = "icons_search_icon",
      name = "Search Icon",
      categoryId = "icons",
      kind = "search",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("icons", "search", "search icon")
    ),
    BundledElementAsset(
      id = "icons_settings_gear",
      name = "Settings Gear",
      categoryId = "icons",
      kind = "gear",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("icons", "gear", "settings gear")
    ),
    BundledElementAsset(
      id = "icons_download_icon",
      name = "Download Icon",
      categoryId = "icons",
      kind = "download",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("icons", "download", "download icon")
    ),
    BundledElementAsset(
      id = "icons_share_icon",
      name = "Share Icon",
      categoryId = "icons",
      kind = "share",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("icons", "share", "share icon")
    ),
    BundledElementAsset(
      id = "icons_music_icon",
      name = "Music Icon",
      categoryId = "icons",
      kind = "music",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("icons", "music", "music icon")
    ),
    BundledElementAsset(
      id = "icons_location_pin",
      name = "Location Pin",
      categoryId = "icons",
      kind = "pin",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("icons", "pin", "location pin")
    ),
    BundledElementAsset(
      id = "icons_calendar_icon",
      name = "Calendar Icon",
      categoryId = "icons",
      kind = "calendar",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("icons", "calendar", "calendar icon")
    ),
    BundledElementAsset(
      id = "icons_clock_icon",
      name = "Clock Icon",
      categoryId = "icons",
      kind = "clock",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("icons", "clock", "clock icon")
    ),
    BundledElementAsset(
      id = "icons_chat_bubble",
      name = "Chat Bubble",
      categoryId = "icons",
      kind = "chat",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("icons", "chat", "chat bubble")
    ),
    BundledElementAsset(
      id = "icons_check_mark",
      name = "Check Mark",
      categoryId = "icons",
      kind = "check",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("icons", "check", "check mark")
    ),
    BundledElementAsset(
      id = "icons_lock_icon",
      name = "Lock Icon",
      categoryId = "icons",
      kind = "lock",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("icons", "lock", "lock icon")
    ),
    BundledElementAsset(
      id = "icons_wifi_icon",
      name = "WiFi Icon",
      categoryId = "icons",
      kind = "wifi",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("icons", "wifi", "wifi icon")
    ),
    BundledElementAsset(
      id = "icons_bolt_icon",
      name = "Bolt Icon",
      categoryId = "icons",
      kind = "bolt",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("icons", "bolt", "bolt icon")
    ),
    BundledElementAsset(
      id = "animals_happy_cat",
      name = "Happy Cat",
      categoryId = "animals",
      kind = "Cat",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("animals", "cat", "happy cat")
    ),
    BundledElementAsset(
      id = "animals_playful_dog",
      name = "Playful Dog",
      categoryId = "animals",
      kind = "Dog",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("animals", "dog", "playful dog")
    ),
    BundledElementAsset(
      id = "animals_giant_panda",
      name = "Giant Panda",
      categoryId = "animals",
      kind = "Panda",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("animals", "panda", "giant panda")
    ),
    BundledElementAsset(
      id = "animals_brave_tiger",
      name = "Brave Tiger",
      categoryId = "animals",
      kind = "Tiger",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("animals", "tiger", "brave tiger")
    ),
    BundledElementAsset(
      id = "animals_cute_bunny",
      name = "Cute Bunny",
      categoryId = "animals",
      kind = "Bunny",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("animals", "bunny", "cute bunny")
    ),
    BundledElementAsset(
      id = "animals_clever_fox",
      name = "Clever Fox",
      categoryId = "animals",
      kind = "Fox",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("animals", "fox", "clever fox")
    ),
    BundledElementAsset(
      id = "animals_gentle_elephant",
      name = "Gentle Elephant",
      categoryId = "animals",
      kind = "Elephant",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("animals", "elephant", "gentle elephant")
    ),
    BundledElementAsset(
      id = "animals_king_lion",
      name = "King Lion",
      categoryId = "animals",
      kind = "Lion",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("animals", "lion", "king lion")
    ),
    BundledElementAsset(
      id = "animals_little_pig",
      name = "Little Pig",
      categoryId = "animals",
      kind = "Pig",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("animals", "pig", "little pig")
    ),
    BundledElementAsset(
      id = "animals_happy_frog",
      name = "Happy Frog",
      categoryId = "animals",
      kind = "Frog",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("animals", "frog", "happy frog")
    ),
    BundledElementAsset(
      id = "animals_tall_giraffe",
      name = "Tall Giraffe",
      categoryId = "animals",
      kind = "Giraffe",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("animals", "giraffe", "tall giraffe")
    ),
    BundledElementAsset(
      id = "animals_striped_zebra",
      name = "Striped Zebra",
      categoryId = "animals",
      kind = "Zebra",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("animals", "zebra", "striped zebra")
    ),
    BundledElementAsset(
      id = "animals_magic_unicorn",
      name = "Magic Unicorn",
      categoryId = "animals",
      kind = "Unicorn",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("animals", "unicorn", "magic unicorn")
    ),
    BundledElementAsset(
      id = "animals_wise_owl",
      name = "Wise Owl",
      categoryId = "animals",
      kind = "Owl",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("animals", "owl", "wise owl")
    ),
    BundledElementAsset(
      id = "animals_sleepy_koala",
      name = "Sleepy Koala",
      categoryId = "animals",
      kind = "Koala",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("animals", "koala", "sleepy koala")
    ),
    BundledElementAsset(
      id = "animals_brown_bear",
      name = "Brown Bear",
      categoryId = "animals",
      kind = "Bear",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("animals", "bear", "brown bear")
    ),
    BundledElementAsset(
      id = "animals_grey_wolf",
      name = "Grey Wolf",
      categoryId = "animals",
      kind = "Wolf",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("animals", "wolf", "grey wolf")
    ),
    BundledElementAsset(
      id = "animals_white_rabbit",
      name = "White Rabbit",
      categoryId = "animals",
      kind = "Rabbit",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("animals", "rabbit", "white rabbit")
    ),
    BundledElementAsset(
      id = "animals_rhino",
      name = "Rhino",
      categoryId = "animals",
      kind = "Rhino",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("animals", "rhino", "rhino")
    ),
    BundledElementAsset(
      id = "animals_kitten",
      name = "Kitten",
      categoryId = "animals",
      kind = "Cat",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("animals", "cat", "kitten")
    ),
    BundledElementAsset(
      id = "birds_bluebird",
      name = "Bluebird",
      categoryId = "birds",
      kind = "crest",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("birds", "crest", "bluebird")
    ),
    BundledElementAsset(
      id = "birds_parrot",
      name = "Parrot",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("birds", "plain", "parrot")
    ),
    BundledElementAsset(
      id = "birds_robin",
      name = "Robin",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("birds", "plain", "robin")
    ),
    BundledElementAsset(
      id = "birds_flamingo_bird",
      name = "Flamingo Bird",
      categoryId = "birds",
      kind = "crest",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("birds", "crest", "flamingo bird")
    ),
    BundledElementAsset(
      id = "birds_owl_bird",
      name = "Owl Bird",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("birds", "plain", "owl bird")
    ),
    BundledElementAsset(
      id = "birds_penguin_bird",
      name = "Penguin Bird",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("birds", "plain", "penguin bird")
    ),
    BundledElementAsset(
      id = "birds_eagle_bird",
      name = "Eagle Bird",
      categoryId = "birds",
      kind = "crest",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("birds", "crest", "eagle bird")
    ),
    BundledElementAsset(
      id = "birds_hummingbird",
      name = "Hummingbird",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("birds", "plain", "hummingbird")
    ),
    BundledElementAsset(
      id = "birds_cardinal",
      name = "Cardinal",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("birds", "plain", "cardinal")
    ),
    BundledElementAsset(
      id = "birds_peacock_bird",
      name = "Peacock Bird",
      categoryId = "birds",
      kind = "crest",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("birds", "crest", "peacock bird")
    ),
    BundledElementAsset(
      id = "birds_toucan_bird",
      name = "Toucan Bird",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("birds", "plain", "toucan bird")
    ),
    BundledElementAsset(
      id = "birds_sparrow",
      name = "Sparrow",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("birds", "plain", "sparrow")
    ),
    BundledElementAsset(
      id = "birds_dove",
      name = "Dove",
      categoryId = "birds",
      kind = "crest",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("birds", "crest", "dove")
    ),
    BundledElementAsset(
      id = "birds_woodpecker",
      name = "Woodpecker",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("birds", "plain", "woodpecker")
    ),
    BundledElementAsset(
      id = "birds_kingfisher",
      name = "Kingfisher",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("birds", "plain", "kingfisher")
    ),
    BundledElementAsset(
      id = "birds_finch",
      name = "Finch",
      categoryId = "birds",
      kind = "crest",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("birds", "crest", "finch")
    ),
    BundledElementAsset(
      id = "birds_kite_bird",
      name = "Kite Bird",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("birds", "plain", "kite bird")
    ),
    BundledElementAsset(
      id = "birds_heron",
      name = "Heron",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("birds", "plain", "heron")
    ),
    BundledElementAsset(
      id = "birds_macaw",
      name = "Macaw",
      categoryId = "birds",
      kind = "crest",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("birds", "crest", "macaw")
    ),
    BundledElementAsset(
      id = "birds_chick",
      name = "Chick",
      categoryId = "birds",
      kind = "plain",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("birds", "plain", "chick")
    ),
    BundledElementAsset(
      id = "characters_man_with_cap",
      name = "Man With Cap",
      categoryId = "characters",
      kind = "cap",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("characters", "cap", "man with cap")
    ),
    BundledElementAsset(
      id = "characters_woman_with_bun",
      name = "Woman With Bun",
      categoryId = "characters",
      kind = "bun",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("characters", "bun", "woman with bun")
    ),
    BundledElementAsset(
      id = "characters_spiky_hair_guy",
      name = "Spiky Hair Guy",
      categoryId = "characters",
      kind = "spiky",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("characters", "spiky", "spiky hair guy")
    ),
    BundledElementAsset(
      id = "characters_curly_hair_woman",
      name = "Curly Hair Woman",
      categoryId = "characters",
      kind = "curly",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("characters", "curly", "curly hair woman")
    ),
    BundledElementAsset(
      id = "characters_long_hair_woman",
      name = "Long Hair Woman",
      categoryId = "characters",
      kind = "long",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("characters", "long", "long hair woman")
    ),
    BundledElementAsset(
      id = "characters_smiling_boy",
      name = "Smiling Boy",
      categoryId = "characters",
      kind = "spiky",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("characters", "spiky", "smiling boy")
    ),
    BundledElementAsset(
      id = "characters_girl_with_bun",
      name = "Girl With Bun",
      categoryId = "characters",
      kind = "bun",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("characters", "bun", "girl with bun")
    ),
    BundledElementAsset(
      id = "characters_cap_boy",
      name = "Cap Boy",
      categoryId = "characters",
      kind = "cap",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("characters", "cap", "cap boy")
    ),
    BundledElementAsset(
      id = "characters_curly_man",
      name = "Curly Man",
      categoryId = "characters",
      kind = "curly",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("characters", "curly", "curly man")
    ),
    BundledElementAsset(
      id = "characters_long_hair_girl",
      name = "Long Hair Girl",
      categoryId = "characters",
      kind = "long",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("characters", "long", "long hair girl")
    ),
    BundledElementAsset(
      id = "characters_happy_man",
      name = "Happy Man",
      categoryId = "characters",
      kind = "none",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("characters", "none", "happy man")
    ),
    BundledElementAsset(
      id = "characters_happy_woman",
      name = "Happy Woman",
      categoryId = "characters",
      kind = "bun",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("characters", "bun", "happy woman")
    ),
    BundledElementAsset(
      id = "characters_kid_with_cap",
      name = "Kid With Cap",
      categoryId = "characters",
      kind = "cap",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("characters", "cap", "kid with cap")
    ),
    BundledElementAsset(
      id = "characters_teen_spiky",
      name = "Teen Spiky",
      categoryId = "characters",
      kind = "spiky",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("characters", "spiky", "teen spiky")
    ),
    BundledElementAsset(
      id = "characters_woman_curly",
      name = "Woman Curly",
      categoryId = "characters",
      kind = "curly",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("characters", "curly", "woman curly")
    ),
    BundledElementAsset(
      id = "characters_girl_long_hair",
      name = "Girl Long Hair",
      categoryId = "characters",
      kind = "long",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("characters", "long", "girl long hair")
    ),
    BundledElementAsset(
      id = "characters_man_bun",
      name = "Man Bun",
      categoryId = "characters",
      kind = "bun",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("characters", "bun", "man bun")
    ),
    BundledElementAsset(
      id = "characters_boy_curly",
      name = "Boy Curly",
      categoryId = "characters",
      kind = "curly",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("characters", "curly", "boy curly")
    ),
    BundledElementAsset(
      id = "characters_woman_cap",
      name = "Woman Cap",
      categoryId = "characters",
      kind = "cap",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("characters", "cap", "woman cap")
    ),
    BundledElementAsset(
      id = "characters_elder_smile",
      name = "Elder Smile",
      categoryId = "characters",
      kind = "none",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("characters", "none", "elder smile")
    ),
    BundledElementAsset(
      id = "vehicles_sedan_car",
      name = "Sedan Car",
      categoryId = "vehicles",
      kind = "car",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("vehicles", "car", "sedan car")
    ),
    BundledElementAsset(
      id = "vehicles_sports_car",
      name = "Sports Car",
      categoryId = "vehicles",
      kind = "sport",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("vehicles", "sport", "sports car")
    ),
    BundledElementAsset(
      id = "vehicles_jeep",
      name = "Jeep",
      categoryId = "vehicles",
      kind = "jeep",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("vehicles", "jeep", "jeep")
    ),
    BundledElementAsset(
      id = "vehicles_van",
      name = "Van",
      categoryId = "vehicles",
      kind = "van",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("vehicles", "van", "van")
    ),
    BundledElementAsset(
      id = "vehicles_city_bus",
      name = "City Bus",
      categoryId = "vehicles",
      kind = "bus",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("vehicles", "bus", "city bus")
    ),
    BundledElementAsset(
      id = "vehicles_cargo_truck",
      name = "Cargo Truck",
      categoryId = "vehicles",
      kind = "truck",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("vehicles", "truck", "cargo truck")
    ),
    BundledElementAsset(
      id = "vehicles_motorbike",
      name = "Motorbike",
      categoryId = "vehicles",
      kind = "bike",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("vehicles", "bike", "motorbike")
    ),
    BundledElementAsset(
      id = "vehicles_bicycle",
      name = "Bicycle",
      categoryId = "vehicles",
      kind = "bike",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("vehicles", "bike", "bicycle")
    ),
    BundledElementAsset(
      id = "vehicles_airplane",
      name = "Airplane",
      categoryId = "vehicles",
      kind = "plane",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("vehicles", "plane", "airplane")
    ),
    BundledElementAsset(
      id = "vehicles_sail_boat",
      name = "Sail Boat",
      categoryId = "vehicles",
      kind = "boat",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("vehicles", "boat", "sail boat")
    ),
    BundledElementAsset(
      id = "vehicles_rocket",
      name = "Rocket",
      categoryId = "vehicles",
      kind = "rocket",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("vehicles", "rocket", "rocket")
    ),
    BundledElementAsset(
      id = "vehicles_helicopter",
      name = "Helicopter",
      categoryId = "vehicles",
      kind = "helicopter",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("vehicles", "helicopter", "helicopter")
    ),
    BundledElementAsset(
      id = "vehicles_train",
      name = "Train",
      categoryId = "vehicles",
      kind = "train",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("vehicles", "train", "train")
    ),
    BundledElementAsset(
      id = "vehicles_tractor",
      name = "Tractor",
      categoryId = "vehicles",
      kind = "tractor",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("vehicles", "tractor", "tractor")
    ),
    BundledElementAsset(
      id = "vehicles_hatchback_car",
      name = "Hatchback Car",
      categoryId = "vehicles",
      kind = "car",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("vehicles", "car", "hatchback car")
    ),
    BundledElementAsset(
      id = "vehicles_roadster",
      name = "Roadster",
      categoryId = "vehicles",
      kind = "sport",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("vehicles", "sport", "roadster")
    ),
    BundledElementAsset(
      id = "vehicles_offroad_jeep",
      name = "Offroad Jeep",
      categoryId = "vehicles",
      kind = "jeep",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("vehicles", "jeep", "offroad jeep")
    ),
    BundledElementAsset(
      id = "vehicles_mini_van",
      name = "Mini Van",
      categoryId = "vehicles",
      kind = "van",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("vehicles", "van", "mini van")
    ),
    BundledElementAsset(
      id = "vehicles_tour_bus",
      name = "Tour Bus",
      categoryId = "vehicles",
      kind = "bus",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("vehicles", "bus", "tour bus")
    ),
    BundledElementAsset(
      id = "vehicles_pickup_truck",
      name = "Pickup Truck",
      categoryId = "vehicles",
      kind = "truck",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("vehicles", "truck", "pickup truck")
    ),
    BundledElementAsset(
      id = "charts_bar_chart",
      name = "Bar Chart",
      categoryId = "charts",
      kind = "bar",
      primaryColor = 0xFF48CAE4L,
      secondaryColor = 0xFF0077B6L,
      tags = listOf("charts", "bar", "bar chart")
    ),
    BundledElementAsset(
      id = "charts_line_chart",
      name = "Line Chart",
      categoryId = "charts",
      kind = "line",
      primaryColor = 0xFFE0AAFFL,
      secondaryColor = 0xFF9D4EDDL,
      tags = listOf("charts", "line", "line chart")
    ),
    BundledElementAsset(
      id = "charts_area_chart",
      name = "Area Chart",
      categoryId = "charts",
      kind = "area",
      primaryColor = 0xFF90E0EFL,
      secondaryColor = 0xFF00B4D8L,
      tags = listOf("charts", "area", "area chart")
    ),
    BundledElementAsset(
      id = "charts_pie_chart",
      name = "Pie Chart",
      categoryId = "charts",
      kind = "pie",
      primaryColor = 0xFFFFAFCCL,
      secondaryColor = 0xFFFF5D8FL,
      tags = listOf("charts", "pie", "pie chart")
    ),
    BundledElementAsset(
      id = "charts_donut_chart",
      name = "Donut Chart",
      categoryId = "charts",
      kind = "donut",
      primaryColor = 0xFFA7C957L,
      secondaryColor = 0xFF6A994EL,
      tags = listOf("charts", "donut", "donut chart")
    ),
    BundledElementAsset(
      id = "charts_progress_bars",
      name = "Progress Bars",
      categoryId = "charts",
      kind = "progress",
      primaryColor = 0xFFF4A261L,
      secondaryColor = 0xFFE76F51L,
      tags = listOf("charts", "progress", "progress bars")
    ),
    BundledElementAsset(
      id = "charts_scatter_plot",
      name = "Scatter Plot",
      categoryId = "charts",
      kind = "scatter",
      primaryColor = 0xFF2EC4B6L,
      secondaryColor = 0xFF011627L,
      tags = listOf("charts", "scatter", "scatter plot")
    ),
    BundledElementAsset(
      id = "charts_growth_bars",
      name = "Growth Bars",
      categoryId = "charts",
      kind = "bar",
      primaryColor = 0xFFE63946L,
      secondaryColor = 0xFFF1FA8CL,
      tags = listOf("charts", "bar", "growth bars")
    ),
    BundledElementAsset(
      id = "charts_trend_line",
      name = "Trend Line",
      categoryId = "charts",
      kind = "line",
      primaryColor = 0xFFB388FFL,
      secondaryColor = 0xFF7C4DFFL,
      tags = listOf("charts", "line", "trend line")
    ),
    BundledElementAsset(
      id = "charts_revenue_area",
      name = "Revenue Area",
      categoryId = "charts",
      kind = "area",
      primaryColor = 0xFFFFD54FL,
      secondaryColor = 0xFFFF8F00L,
      tags = listOf("charts", "area", "revenue area")
    ),
    BundledElementAsset(
      id = "charts_market_pie",
      name = "Market Pie",
      categoryId = "charts",
      kind = "pie",
      primaryColor = 0xFF00E5FFL,
      secondaryColor = 0xFF0091FFL,
      tags = listOf("charts", "pie", "market pie")
    ),
    BundledElementAsset(
      id = "charts_goal_donut",
      name = "Goal Donut",
      categoryId = "charts",
      kind = "donut",
      primaryColor = 0xFFFF4D6DL,
      secondaryColor = 0xFFFF8FA3L,
      tags = listOf("charts", "donut", "goal donut")
    ),
    BundledElementAsset(
      id = "charts_skill_progress",
      name = "Skill Progress",
      categoryId = "charts",
      kind = "progress",
      primaryColor = 0xFFFFD166L,
      secondaryColor = 0xFFFF9100L,
      tags = listOf("charts", "progress", "skill progress")
    ),
    BundledElementAsset(
      id = "charts_data_scatter",
      name = "Data Scatter",
      categoryId = "charts",
      kind = "scatter",
      primaryColor = 0xFF06D6A0L,
      secondaryColor = 0xFF118AB2L,
      tags = listOf("charts", "scatter", "data scatter")
    ),
    BundledElementAsset(
      id = "charts_sales_bars",
      name = "Sales Bars",
      categoryId = "charts",
      kind = "bar",
      primaryColor = 0xFF9D6BFFL,
      secondaryColor = 0xFF7000FFL,
      tags = listOf("charts", "bar", "sales bars")
    ),
    BundledElementAsset(
      id = "charts_pulse_line",
      name = "Pulse Line",
      categoryId = "charts",
      kind = "line",
      primaryColor = 0xFFFF7B54L,
      secondaryColor = 0xFFFFB26BL,
      tags = listOf("charts", "line", "pulse line")
    ),
    BundledElementAsset(
      id = "charts_traffic_area",
      name = "Traffic Area",
      categoryId = "charts",
      kind = "area",
      primaryColor = 0xFF4CC9F0L,
      secondaryColor = 0xFF4361EEL,
      tags = listOf("charts", "area", "traffic area")
    ),
    BundledElementAsset(
      id = "charts_share_pie",
      name = "Share Pie",
      categoryId = "charts",
      kind = "pie",
      primaryColor = 0xFFF72585L,
      secondaryColor = 0xFFFF70A6L,
      tags = listOf("charts", "pie", "share pie")
    ),
    BundledElementAsset(
      id = "charts_score_donut",
      name = "Score Donut",
      categoryId = "charts",
      kind = "donut",
      primaryColor = 0xFF80ED99L,
      secondaryColor = 0xFF38B000L,
      tags = listOf("charts", "donut", "score donut")
    ),
    BundledElementAsset(
      id = "charts_stats_progress",
      name = "Stats Progress",
      categoryId = "charts",
      kind = "progress",
      primaryColor = 0xFFFFC300L,
      secondaryColor = 0xFFFF5733L,
      tags = listOf("charts", "progress", "stats progress")
    ),
  )

  private val byId: Map<String, BundledElementAsset> by lazy {
    assets.associateBy { it.id }
  }

  fun findById(id: String): BundledElementAsset? = byId[id]

  /** APK asset path of the original PNG thumbnail, e.g. "elements/thumbnails/shapes/shapes_star.png". */
  fun thumbnailAssetPath(id: String): String? = byId[id]?.let { "$THUMBNAIL_PREFIX${it.categoryId}/${it.id}.png" }

  /** APK asset path of the original full asset file (SVG), e.g. "elements/files/shapes/shapes_star.svg". */
}
