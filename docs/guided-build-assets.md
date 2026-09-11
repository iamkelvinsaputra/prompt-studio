# Guided Build asset provenance

The four static style samples live in `shared/src/commonMain/composeResources/drawable/`:

- `style_anime.png`
- `style_anime_ink.png`
- `style_painterly.png`
- `style_watercolor.png`

Created with the built-in imagegen tool during this implementation. The anime sample is the benchmark; the other three use it as a reference/edit target, retaining subject, clothes, framing, and background arrangement. These are bundled product assets, never generated at runtime. No provider credentials were used. Original generated files remain outside the repository; complete final PNGs are copied into Compose resources.

Samples illustrate the preset's rendering language. They do not preview the user's identity, pose, refinements, or custom prose. Custom/imported styles without a matching sample explicitly show “No preview sample.” `VisualAssetCatalog` owns resource mapping, with `GuideAssetCatalog` separately owning normalized mannequin poses. Replacing artwork does not change semantic state or prompt compilation.

## Generation prompts

### Contemporary Anime

Use case: stylized-concept. Asset type: static art-style sample card for a character illustration tool. Create a beautiful polished contemporary anime illustration of one adult woman around 30, straight dark chin-length bob, simple slate-blue crew-neck jacket over ivory shirt, shown waist up, centered, facing camera, relaxed shoulders, arms lowered, composed neutral expression. Pale warm gray uncluttered studio backdrop with faint broad blue shape. Eye-level camera, entire head visible with generous margin. Square image. Style: premium contemporary anime-game character art, controlled cel shading, precise elegant ink contours, clean facial construction, balanced restrained blue and ivory palette. Believable adult proportions. No props, no words, no labels, no borders, no watermark. This is a benchmark subject; prioritize clear rendering language.

### Anime + Ink Wash

Use case: style-transfer. Edit this benchmark art-style sample. Keep exactly the same adult woman, dark bob haircut, slate-blue jacket and ivory shirt, neutral expression, centered waist-up framing and pale backdrop. Change rendering ONLY to premium contemporary anime plus Japanese sumi-e ink wash: elegant expressive black brush contours with thick-to-thin pressure, selective dry brush, translucent sky-blue watercolor pigment pooling, tactile washi grain, airy slightly melancholic mood, substantial quiet space. Keep facial clarity and believable adult anatomy. No text, border, watermark, extra subjects or props.

### Painterly

Use case: style-transfer. Edit this benchmark art-style sample. Preserve exactly the same adult woman around 30, dark bob, slate-blue jacket and ivory shirt, centered waist-up framing, neutral expression, relaxed shoulders and pale backdrop with broad blue shape. Change ONLY rendering to accomplished painterly gouache/oil editorial illustration: confident visible broad brushstrokes, sculptural color planes, softly lost and found edges, tactile opaque paint, subtle warm cool color interplay, beautifully modeled adult face. Clearly painterly, not anime and not a photo filter. No words, border, watermark or extra props.

### Watercolor

Use case: style-transfer. Edit this benchmark art-style sample. Preserve exactly the same adult woman around 30, dark bob, slate-blue jacket and ivory shirt, centered waist-up framing, neutral expression, relaxed shoulders and pale backdrop with broad blue shape. Change ONLY rendering to exquisite transparent traditional watercolor: luminous delicate layered washes, soft wet-on-wet diffusion, pigment blooms and pooling, visible cold-press paper grain, dissolving outer edges. No heavy black ink outlines. Keep face clear, believable adult proportions, pale blue/ivory palette. Obviously watercolor, not an anime filter. No words, borders, watermark or extra props.

## Guide artwork

The six guide poses are code-native vector mannequin benchmarks, using curved tapered volumes with connected joints, modeled head direction, and modest adult maturity cues. They are not ImageGen assets. Shared normalized pose coordinates, bounded shoulder/elbow rotations, framing, placement, and composition feed both preview and PNG reference export. No disjoint paper-doll image layers or runtime asset fetching are used.

V1's coarse geometry is isolated in `SilhouetteDrawing.kt` solely for saved version-1 guide replay. New previews and specifications use `CharacterGuideDrawing.kt` (version 2).
