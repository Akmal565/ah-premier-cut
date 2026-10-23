# Shapes Panel — Bundled Asset Library

Bottom-navigation **Shapes/Elements** panel shows the **original asset
library bundled directly inside the APK** (no Firebase): **11 categories ×
20 original assets = 220 assets**, all drawn procedurally by the original
generator (no third-party artwork). The assets were previously staged in the
deleted `firebase-assets/` upload folder; they now ship with the app.

## Data model (unchanged ids/structure)

```
ElementCategory enum  — shapes|graphics|frames|tables|3d|icons|animals|birds|characters|vehicles|charts
BundledElementsCatalog.assets — 220 entries (id, name, categoryId, kind, colors, tags)
```

## Files

| Path | What |
| --- | --- |
| `app/src/main/assets/elements/files/{category}/{id}.svg` | 220 original full assets (512×512 transparent SVG masters) |
| `app/src/main/assets/elements/thumbnails/{category}/{id}.png` | 220 original PNG renders (256px, transparent) |
| `app/.../ui/components/elements/BundledElementsCatalog.kt` | Generated single source of truth (220 entries + path helpers) |
| `app/.../ui/components/elements/ElementsCatalog.kt` | Maps the bundled assets to the panel's `ElementItem` grid (search / category filter / find by id) |
| `app/.../ui/components/elements/ElementAssetCache.kt` | Small LruCache decoding the bundled PNGs for the canvas renderer (init from `StudioApplication`) |
| `app/.../ui/components/elements/ElementRenderer.kt` | Draws the ORIGINAL asset bitmap first; procedural drawing is only a legacy fallback for unknown ids |
| `app/src/test/java/.../BundledElementsCatalogTest.kt` | Pins 11×20=220, unique ids, path conventions, real colors/names |

## Display pipeline (original assets everywhere)

- **Panel grid**: every tile loads its original PNG thumbnail straight from the
  APK via Coil (`file:///android_asset/elements/thumbnails/...`) — same
  category-wise organization the firebase upload folder had.
- **Video canvas (preview + export + GPU sprites)**: inserted element clips
  resolve through `ElementAssetCache` and draw the same original PNG; the
  procedural `ElementRenderer` branches remain only as a legacy fallback.
- **Insertion** still goes through the existing `TimelineEngine.addElementClip`
  (real editable clip at the playhead, `renderData` = bundled SVG path) —
  timeline, undo/redo, save, playback and export paths are unchanged.

Tables & Charts carry the same editable-model metadata in
`defaultProperties` (`kind`: grid / chart types) and render as their original
asset image like every other category.

## Firebase cleanup (this change)

Removed end to end — the upload never went live:

- `firebase-assets/` staging folder (seed + 440 upload files)
- `tools/assets/` upload tooling (upload_firebase.js, generator, validator)
- `.github/workflows/upload-elements-assets.yml` upload workflow
- `FirebaseElementsRepository.kt` (Firestore/Storage fetch used only by the panel)
- `assets/elements/catalog.json` seed mirror (replaced by `BundledElementsCatalog.kt`)
- `categories` / `assets` / `elements/**` blocks in `firestore.rules` / `storage.rules`

The rest of Firebase (auth, captions, text templates, effects) is untouched.
