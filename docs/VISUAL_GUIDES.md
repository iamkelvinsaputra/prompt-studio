# Visual guide artwork

The studio's option cards load artwork from:

`shared/src/commonMain/composeResources/files/visual-guides/`

`registry.json` is the only artwork manifest. Each key is a stable `category/option-id`, for example `pose/contrapposto`. The typed option catalog in `guide/VisualGuideRegistry.kt` derives its IDs, labels and prompt values from the existing domain enums. The compiler consumes those same enums. Artwork metadata never supplies or overrides compiler wording.

## Replace artwork

1. Replace the image at the path named by the registry entry.
2. If the filename or format changes, update `image` (and `thumbnail`, if present) in that entry.
3. Rebuild and relaunch the native app, or rebuild the Web bundle and reload its page. Compose packages these resources at build time; a browser refresh alone cannot update an already-built bundle.

No UI component needs changing. Paths are relative to the `visual-guides` folder, not URLs. Keep assets local.

```json
"pose/contrapposto": {
  "image": "pose/contrapposto.png",
  "thumbnail": "pose/contrapposto-small.webp",
  "label": "Relaxed",
  "description": "A relaxed standing gesture with weight on one leg.",
  "tags": ["standing", "weight shift"],
  "aliases": ["casual", "contrapposto"],
  "order": 1
}
```

Only `image` is needed to attach artwork. Every field is optional. Labels default to the typed option label. `order` controls browsing order within a category; search matches labels, descriptions, prompt vocabulary, aliases and tags. Six common poses are shown first, with the current selection included even when it is outside that set. Browse all opens search and a compact density toggle.

## Conventions and formats

Use lowercase kebab-case IDs and filenames. Current folders include `pose`, `body-orientation`, `framing`, `placement`, `composition`, `gaze`, and `prop-placement`. The catalog also exposes `camera-angle`, `head-direction`, and `arm-position`; their artwork can be added whenever it is ready.

Use a consistent 4:5 canvas, ideally 320×400 or 640×800 pixels. Keep a generous margin around the complete gesture. Transparent PNG is suitable; an opaque warm neutral background also works. Cards fit the complete image without cropping. Keep thumbnails small (ideally under 100 KB).

PNG and WebP use Compose's bitmap decoder across targets. SVG uses Compose's SVG decoder on Desktop, iOS and Web. Android's resource decoder does not support SVG: supply a PNG/WebP thumbnail for Android-compatible SVG entries, or it will display the text fallback. SVGs should be self-contained, with a viewBox and no linked fonts or external images. No image-loading dependency or network service is required.

## Add or remove

For an existing option, add its key to the registry and point it to the file. For a new *semantic option*, first add the typed domain value and its prompt wording, then include it in the relevant catalog. Enum-backed catalogs automatically include new entries. Add compiler/domain tests for any new semantics. This is separate from replacing artwork, which never requires Kotlin changes.

To remove artwork, delete the entry or set `image` to null. The option remains selectable with its text label. To remove the option itself, consider saved JSON compatibility before deleting a domain enum value; ordinarily retain the value for imports.

## Fallbacks and accessibility

Missing metadata, missing files, corrupt images and unsupported formats produce a neutral “Text guide” tile. The option's actual label and selection state remain visible and accessible. No broken-image icon is shown. A textual description appears below the selected group. Hover and keyboard focus reveal brief details without changing card height. Cards support Tab, Enter and Space through Compose's semantic controls and have a strong focus/selection border. No meaning depends on artwork or color alone.

The original 36 bundled PNGs are temporary exports of the existing structural renderer, not final illustrations. Unsupported poses, head and hand choices intentionally use text fallbacks rather than misleading stock silhouettes. The optional export utility `VisualGuideAssetsTest.exportTemporaryArtwork` writes a fresh starter collection to `shared/build/reports/studio-artwork`; it never overwrites the curated source assets.

## Two distinct uses of silhouettes

The **option library** is completely file-driven and replaceable through the registry. The **combined structural sketch** is still rendered from the selected pose, framing and placement. It is labeled approximate. That procedural renderer is also versioned in existing generation history, so replacing option artwork does not silently change historical guide reconstruction or provider reference images. A card's artwork is a visual explanation, not generated output.

## Guided creation additions

`lighting-direction/` contains ten PNG diagrams: front, front-left, front-right, left, right, back, back-left, back-right, top and below. Each shows a constant figure, an external light and its direction, and a lit region (or rim for backlight). `environment/` contains nine scene-category diagrams. `camera-angle/` includes five camera-position/roll diagrams, replacing the older text fallbacks. Both categories use the same registry and card decoder as pose/framing; there is no diagram drawing code in UI components.

`tools/build_direction_guides.py` is an optional offline authoring utility using Pillow to recreate these starter PNGs and their registry entries. It is not part of the runtime or Gradle build. Run it only when deliberately regenerating the starter collection; replace individual PNGs/registry metadata directly for custom artwork. All added assets use PNG for Android, iOS, Desktop and Wasm compatibility. Artwork remains independent of compiler wording.
