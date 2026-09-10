# Phase 3: optional visual guide integration

Visual Build now supplies an optional, locally rendered structural reference to the existing generation workflow. Phase 1's silhouette geometry and Phase 2's prompt ownership remain shared. No dependencies were added.

## Ownership and rendering

`CharacterProject.visualAssembly` remains a projection of canonical character inputs. `GuideRenderSpec` captures that immutable assembly, integer dimensions and renderer version when Generate Current is pressed. Prompt resolution independently calls the existing `effectivePrompt()` selector once on the same detached character snapshot. Neither provider compiles text or reconstructs visual state.

`LocalGuideRenderer` draws into an off-screen Compose `ImageBitmap` using `CanvasDrawScope`. It calls the same `drawSilhouette` function as the editor preview. No composable, visible screen, screenshot, network or temporary file is involved. The guide fills its frame with an opaque neutral background, body silhouette, distinguishable prop, and the existing seated support mass. Preview padding and thirds lines are omitted. Shape, orientation, framing and placement use the original drawing logic.

PNG encoding is the only platform-specific boundary: Android uses Bitmap compression; Desktop, Apple and Wasm use the already-included Skia encoder. Encoded bytes have owned storage and remain in memory. Automatic sizing uses a 768-pixel long edge at the authored output ratio, rounded to integer pixels; custom ratios outside 1:32–32:1 are unavailable. Explicit render specifications are bounded to 1024 pixels per edge. Providers may map final output to their supported dimensions as before; the guide retains the authored ratio seen in Visual Build.

## Requests and provider capabilities

`ImageGenerationRequest` adds an optional persisted `guideSpec` and a transient `referenceGuide`. The controller prepares the PNG during the existing Generating lifecycle, then passes it with the already-resolved prompt to the adapter. Without a guide, the existing text request remains unchanged. Model and provider booleans determine guide capability; unsupported guided requests are rejected before rendering or network use.

Official API documentation checked on September 10, 2026:

- [OpenAI image edit API](https://developers.openai.com/api/reference/resources/images/methods/edit) supports the existing `gpt-image-2.5-sunburst` and `gpt-image-2` models. Guided requests use `/v1/images/edits` with one PNG `image[]` multipart part; text-only requests keep `/v1/images/generations`. The original prompt, output size, medium quality, PNG output and one-image count are preserved. No mask or fidelity parameter is added.
- [Gemini image editing documentation](https://ai.google.dev/gemini-api/docs/generate-content/image-generation) supports text plus image input for the existing `gemini-3.1-flash-image` model. The existing `generateContent` adapter adds one `inlineData` PNG part beside the unchanged text part. Existing model/output handling remains in place.

The integration uses the existing HTTP client and model catalog. The adapters do not add hidden prompt instructions. Reference fidelity remains provider behavior and has not been verified with live accounts in this phase.

## Small UI addition and defaults

Generation has a single accessible Use visual guide checkbox. It defaults on for a supported provider/model and a fully represented Visual Build configuration. The preference is retained in the existing session generation settings when navigating or changing characters/providers; eligibility is recomputed from current choices. Manual prompts and natural-language adjustments remain compatible with the guide; they are not interpreted into geometry.

Custom/unrepresented poses, custom placement strings, unspecified framing, and invalid ratios disable the checkbox with an explanation that generation will use text only. This includes the legacy demo's asymmetrical-standing pose until a supported visual preset is selected. It avoids submitting a neutral approximation as though it represented a custom authored pose. All existing detailed pose/text authoring remains available.

Guide preparation failure stops that attempt with a specific message. Retry retains the current guide choice. The user can turn the guide off and generate with text only; there is no silent fallback. Cancellation during preparation prevents the provider call, and duplicate Generate clicks are ignored while the existing lifecycle is busy.

## History and persistence compatibility

Character JSON contains neither guide image bytes nor render specifications. Successful generation metadata retains the small versioned render specification alongside the existing exact prompt and character snapshot. PNG bytes are transient and are not retained in saved metadata. Regenerate and historical Generate Again re-render the saved specification, preserving its original guide choice regardless of the current checkbox or character. UI text identifies whether the latest image used a guide.

Old requests/history without guide fields default to text-only. Older app versions with strict JSON decoding may reject newer metadata. The renderer version currently accepts version 1 only; future geometry changes must preserve that implementation or explicitly handle historical versions. Pixel identity is tested on one rendering runtime, not promised across Skia and Android rasterizers.

## Verification

```sh
./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:compileKotlinWasmJs :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug --console=plain
./gradlew :shared:jvmTest :shared:testAndroidHostTest --console=plain
git diff --check
```

Shared tests cover optional guide presence, exact automatic/adjusted/manual prompt preservation, unsupported providers, explicit render failure and text-only retry, cancellation and duplicate clicks during preparation, legacy request defaults, history re-rendering, and dimensions/ratio bounds. Provider mock tests verify both real request formats, PNG bytes, exact prompt/settings, and rejection of an unprepared guide. Existing no-guide provider/error tests continue to pass.

Final validation passed: 110 JVM tests and 97 Android host tests, zero failures/errors/skips. Android assembly, Wasm and iOS simulator compilation, and the diff whitespace check passed. The panel test additionally verifies that turning the guide off survives leaving and reopening the panel.

Desktop raster tests render without any UI and compare decoded pixels for determinism, opacity, dimensions, displacement and crop. A Compose interaction test uses the real PNG renderer with a fake provider to verify manual mode, default-on behavior, retry after preparation failure, toggling off, and unsupported-provider UI. Generated PNG samples are in `shared/build/reports/visual-guides/`; relaxed, side/shoulder and seated/lower samples were visually inspected.

Android is assembled and host-tested; Apple and Web implementations compile. The Android host tests use an injected renderer for orchestration, not a device bitmap runtime. No new Android/iOS device run, browser PNG runtime test or live provider request was performed.

## Files changed

Under `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/`:

- `domain/VisualAssembly.kt`: serializable assembly snapshots.
- `guide/GuideRenderer.kt`: bounded render specification, owned PNG bytes and off-screen renderer.
- `guide/SilhouetteDrawing.kt`: existing drawing moved out of the editor; guide framing option.
- `feature/editor/SilhouettePreview.kt`: thin wrapper over shared drawing.
- `generation/model/GenerationModels.kt`: guide capability, request fields and actionable errors.
- `generation/provider/ImageGenerationProvider.kt`: provider capability default.
- `feature/generation/GenerationController.kt`: preparation, snapshot and replay handling.
- `feature/generation/GenerationPanel.kt`: checkbox, capability explanation and retry consistency.

Platform encoders: `guide/GuideEncoding.kt` in `androidMain`, `jvmMain`, `iosMain` and `wasmJsMain`. Existing native adapters: `nativeGenerationMain/.../generation/provider/HttpImageProviders.kt`.

Tests: `commonTest/.../VisualGuideGenerationTest.kt`, `jvmTest/.../GuideRendererTest.kt`, `jvmTest/.../GenerationDesktopTest.kt`, and `nativeGenerationTest/.../GenerationProviderTest.kt`. This document records scope and validation.

## Remaining limits and next step

The guide still uses coarse placeholder anatomy and a generic prop bar. It cannot express detailed costume, emotion, arbitrary notes or unsupported poses. Web retains its existing prohibition on direct provider generation. There is no user-facing guide export, image upload/editor, strength control, new generation state machine or Phase 4 feature.

The smallest next step is a native smoke test with an already-configured provider account: generate the same supported composition with the guide on and off, then try a manual prompt with the guide on. Assess provider interpretation before adding controls or changing the placeholder artwork.
