# Guided Visual Build implementation

## Delivered flow

New project → Gender → Age → Pose → Gaze → Framing → Position → Composition → Art Style → Summary → Generate.

The eight guided decisions have strong defaults, Back and Continue. Gender offers exactly Man/Woman; age offers Young Adult, Adult, Mature and Older Adult. The underlying existing enum values are retained for compatibility. Pose offers Neutral, Relaxed, Dynamic, Sitting, Confident and Crouching. Gaze offers Camera, Left, Right, Up, Down and Away. Framing offers Full Body, Three-Quarter Body, Waist Up, Bust and Close Up. Position and composition have separate visual choices.

Summary is the primary editing surface. Each row opens the corresponding shared step; Done returns directly to Summary. New/create, select, import, duplicate, variant changes and reopened storage are handled without forcing existing projects through onboarding. New projects created from the detailed editor also enter the guided flow. The wizard position is session navigation, not a persisted completion flag: reopening an unfinished project goes to Summary with every saved selection intact.

The assembled guide stays visible while options scroll. Phone layouts use horizontal cards, a persistent preview and a bottom action; desktop places the preview beside a focused choice grid. Summary shows section labels and compact derived values, prioritizes Generate, and discloses prompt text, advanced editing, variants and saved presets on demand. Generate opens the existing generation panel, retaining its provider/model/credential workflow and Generate Current action.

## Small architecture and state ownership

- `CharacterProject` remains the only authoring source of truth. No wizard copy, bitmap state or second character model was introduced.
- `visualAssembly` remains a read-only projection, extended with gender, age, gaze, composition and pose adjustments. Existing identity, pose, gaze and output fields remain canonical.
- `GuidedNavigation` contains only the current step and whether it is onboarding. `EditorUiState` owns it. `GuideStepChoices` is reused by onboarding and section editing.
- Position writes only `OutputConfiguration.figurePlacement`. Composition writes only `CompositionConfiguration.guidePreset`; it balances supporting shapes without changing subject placement.
- Selecting an age band clears the optional exact age range to prevent the old range contradicting a newly chosen band. Other authored character details remain intact.
- Existing advanced character editors, library/autosave, variant ownership, import/export, clipboard, prompt controls, saved presets, generation and history are reused. No modules, libraries, backend, provider or framework were added.

## Art Style ownership

`ArtStyleConfiguration` stores a selected preset, typed Ink/Mood/Color refinements, a manual-mode flag and an optional manual draft. Four curated presets are included: Contemporary Anime, Anime + Ink Wash, Painterly and Watercolor. Custom is reached through Advanced ART STYLE CORE. The old `style` field remains the legacy/import fallback; projects with no new selected preset compile its original text unchanged. Selecting a new preset does not overwrite that legacy value.

Structured compilation combines one rendering definition with resolved refinements in a fixed order. It replaces refinable dimensions rather than appending competing defaults. The prompt compiler consumes `effectiveArtStyleCore()` for its distinct ART STYLE CORE section; providers never rebuild style text.

Entering manual style initializes the draft from current structured compilation. Preset/refinement changes underneath manual mode preserve the draft. Leaving manual mode resumes structured compilation without parsing prose and retains the draft. Resuming an old draft is an explicit action. Entering manual mode afresh starts from current compilation again. An empty manual style omits the style section; it does not fall back silently.

Global manual prompt ownership is unchanged and has final precedence: automatic compilation uses the effective style section, while global manual mode sends the complete global draft verbatim. Toggling either mode never mutates the other scope's draft. The UI identifies global manual ownership and styles affected only in the automatic draft.

Style is shared across project variants, consistent with the existing variant architecture; pose, composition, framing and prompt ownership remain variant-specific.

## Pose and asset architecture

`GuideAssetCatalog` centralizes six normalized coherent mannequin poses. `CharacterGuideDrawing` renders curved tapered body volumes with connected joints, restrained shading and simplified head/gaze/maturity cues. These are a small vector benchmark library, not photorealistic anatomy assets. All poses use a shared scale and drawing language. Composition diagrams and framing/position thumbnails use the same renderer and current assembled state.

Adjust Pose reveals four bounded rotations: left/right shoulder and elbow, relative to the selected preset. They are persisted under `PoseConfiguration`, carry the owning base pose, feed both guide rendering and prompt compilation, and reset when selecting a new visual preset. Overrides associated with a different base pose are ignored in preview/compiler. No finger rig, facial skeleton, IK, timeline or disconnected bitmap limbs were added.

`VisualAssetCatalog` centralizes all style resource names. Four matched benchmark PNGs are bundled in Compose resources. They were created with the built-in imagegen tool and visually inspected for matching adult subject, costume, framing and palette. They are static; refinement changes do not repaint samples. Manual or unrecognized imported styles disclose that no sample is available. See [asset provenance and exact generation prompts](guided-build-assets.md).

Remaining provisional artwork: the vector mannequin and broad age cues are deliberately simplified benchmarks behind the replaceable guide catalog. The previous crude renderer is retained only for old reference replay. There are no missing style thumbnails or runtime-generated samples.

## Compiler and Phase 3 impact

Compiler changes are limited to selecting effective ART STYLE CORE, adding the composition balance wording, and emitting deterministic pose-adjustment wording. New gaze/framing enum values use the existing prompt option compilation. Existing section order and global manual selection are retained.

Preview and new off-screen PNG export call the same version-2 renderer. `GuideRenderSpec.from` snapshots the expanded canonical projection and emits version 2. Missing/old specification versions still default to version 1, which calls the preserved original drawing function. Existing history can therefore replay its old guide geometry. Tests verify that new identity/gaze/composition/rig fields do not affect v1 pixels.

The generation controller still captures a detached project and resolves its effective prompt at request time. Guide PNGs stay transient; history retains its versioned spec and exact character/style/prompt snapshot. Native adapters and their reference-image formats were untouched. Web retains its prohibition on direct provider generation and API keys.

## Old UI disposition

The locked Style screen is replaced with the shared editable style controls. The app's Visual Build route uses the focused guided/Summary shell instead of the module rail, randomization and default raw-prompt controls. Existing detailed dropdown-based character editors remain behind “All character details.” The old `VisualAssemblyEditor` is deprecated and retained for Phase 1 compatibility tests and its legacy component entry point; the app's `EditorScreen` intercepts Visual Build before that entry point. Its old pose/placement UI is not the default route.

## Validation

Final passing command:

```sh
./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:compileKotlinWasmJs :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug --console=plain
```

- JVM: **131 tests**, zero failures/errors/skips.
- Android host: **115 tests**, zero failures/errors/skips.
- Android debug assembly, Wasm compilation, iOS simulator compilation: passed.
- `git diff --check`: passed.
- Earlier incremental shared JVM compilations and targeted JVM UI/renderer runs were also used.

Focused coverage includes canonical selections, navigation persistence, direct-edit return, existing project entry routes, separate position/composition ownership, bounded pose overrides, deterministic style compilation, refinement replacement, manual style initialization/preservation/resumption, global manual precedence, summary labels, JSON and variant round-trip, canonical export, effective provider requests and v1 replay.

Desktop Compose interaction journeys run at **360 × 780** and **1280 × 900**. They exercise all eight steps, pose adjustment, direct edits, refinements, manual style entry/exit, saved presets, variant switching, generation handoff and reopening at Summary. Raster tests inspect deterministic opacity, framing, placement, semantic differences and every pose. Captures are in `shared/build/reports/guided-build/` and `shared/build/reports/visual-guides/`; phone/desktop layouts, style cards and pose samples were visually inspected.

Initial failures were resolved: a Compose receiver qualification error; old UI tests expecting the removed default panel; a pixel detector assuming the v1 palette; and a test needing to reveal the horizontal preset carousel through the surrounding vertical scroll panel. Visual review also found and fixed cropped style-card heads and Full Body feet clipping at Lower Center.

Known deprecation warnings refer to the intentionally retained legacy panel. Android/iOS device UI, browser PNG runtime, physical-device accessibility and live provider fidelity were not newly exercised. No live paid generation request was made.

## Changed files

All Kotlin paths below are under `shared/src/`.

New common code:

- `commonMain/kotlin/com/kelvinsaputra/promptstudio/domain/GuidedBuild.kt`
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/domain/ArtStyleConfiguration.kt`
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/editor/GuidedVisualBuild.kt`
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/editor/VisualAssetCatalog.kt`
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/guide/GuideAssetCatalog.kt`
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/guide/CharacterGuideDrawing.kt`

Updated common code in the same package tree:

- `App.kt`
- `domain/CharacterProject.kt`, `domain/Options.kt`, `domain/VisualAssembly.kt`
- `feature/editor/EditorViewModel.kt`, `EditorScreen.kt`, `CharacterComponentEditors.kt`, `StyleEditor.kt`, `VisualAssemblyEditor.kt`
- `prompt/PromptCompiler.kt`
- `guide/GuideRenderer.kt`, `guide/SilhouetteDrawing.kt`

Tests and assets:

- New `commonTest/kotlin/com/kelvinsaputra/promptstudio/GuidedBuildTest.kt`.
- Updated `commonTest/kotlin/com/kelvinsaputra/promptstudio/VisualGuideGenerationTest.kt`.
- Updated `jvmTest/kotlin/com/kelvinsaputra/promptstudio/ProductUiTest.kt` and `GuideRendererTest.kt`.
- Four `commonMain/composeResources/drawable/style_*.png` files.
- `docs/guided-visual-build.md` and `docs/guided-build-assets.md`.

## UX compromises and technical risks

The guide is approximate and does not visualize costume, detailed facial identity, free-form notes, arbitrary advanced mechanics or exact age. Adult maturity cues are subtle. Pose adjustment is deliberately limited to four arm rotations. Style samples illustrate rendering, not the user's current character or refinements. Phone option galleries and Summary require scrolling; optional controls remain below the visual choices. Generate retains the existing generation panel rather than immediately making a provider request.

The four original PNGs add roughly 9 MB of bundled assets; asset optimization can be a separate follow-up. Older app versions with strict JSON decoding may reject the newly added fields and enum values. Renderer v1 is retained for historical compatibility; future geometry changes must preserve v2 too or explicitly version it. Cross-platform raster pixel identity and provider interpretation are not guaranteed. Header/file/detailed editor functionality remains intentionally close to the existing product.

Smallest recommended validation: on one native device, create a project, accept defaults through Summary, edit Pose then Done, edit/refine Art Style then Done, and generate once with an already-configured provider and the guide enabled. Confirm actual provider interpretation and keyboard/touch comfort before commissioning a larger artwork library.
