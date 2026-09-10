# Visual Assembly: first vertical slice

Historical Phase 1 record. [Phase 2](prompt-authoring-phase2.md) now compiles facing/prop placement and adds adjustment/manual ownership; the guide-only limitations and next-step notes below describe Phase 1.

Visual Build is the initial editor module and first Quick destination. The existing module navigation, Material theme, character library, autosave and detailed editors remain in place. This is a bounded addition, not the complete editor redesign.

## State and compatibility

`CharacterProject.visualAssembly` projects existing pose and output values into an immutable `VisualAssemblyState`; no remembered copy of those selections exists. Pose cards update the existing pose mechanics, preserving head, gaze and authored notes. Framing and placement update Output directly, so editing either screen, importing, randomizing, switching characters and restoring history cannot leave stale assembly selections.

Only `PoseConfiguration.guideFacing` and `guideProp` are new persisted fields. They have defaults and are appended to the constructor to preserve positional callers. Existing project/library/history JSON loads with defaults; snapshots and exports retain guide choices using the existing serialization path. Older app versions use strict JSON decoding and may reject exports containing these added fields. No schema migration or provider change is required in this app.

Pose, framing and placement already affect compiled prompts through existing fields. Facing and prop placement are explicitly **guide only**, and do not change the compiler or provider requests. The prop is a generic bar, independent of detailed prop identity. Notes and detailed pose mechanics are not visually interpreted. Unsupported/custom poses and placements remain authored values in the summary, with a disclosed neutral/centered drawing approximation rather than silently selecting a different preset.

## Rendering and interaction

Four pose cards are visible initially. Quick adjustments reveal facing, crop, generic prop placement and composition chips. The component stacks at narrow widths and places the preview beside the cards at 640 dp. The outer editor continues to supply scrolling. Cards expose selected semantics; the canvas has a structural description and the summary is readable text.

The local Compose Canvas uses simple vector shapes, one neutral palette, an output frame and thirds guides. All drawing is in `SilhouettePreview.kt`, separated from controls and domain mapping. The stateless drawing function accepts assembly state and an aspect-ratio string. No asset download, image generation, joint manipulation, export pipeline or dependency was added. Numeric output ratios determine the frame; unparseable free-form ratios use a 4:5 guide. Placeholder anatomy and orientation are intentionally coarse and can be replaced within the renderer.

## Validation

- Shared tests cover coherent pose mechanics, preservation of authored content, projection after editor changes, character switching, reset, old JSON defaults, guide round-trip, custom-value summaries and unchanged compilation for guide-only edits.
- Desktop Compose tests exercise the 360 dp editor, hidden adjustments, pose/facing/prop/crop changes and summary synchronization; a 900 dp render covers all four poses. Preview captures are written to the system temporary directory for visual inspection.
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:compileKotlinWasmJs :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug --console=plain`
- `git diff --check`

Desktop, narrow and close-crop captures were visually inspected. Android and iOS were compiled, not manually launched; Web was compiled, not browser-tested in this pass. No live provider calls were made.

## Smallest next step

Expose the existing pose notes as a collapsed “Describe adjustment” field with clear wording that text affects the prompt, not the silhouette. A separate later change can define compiler wording for guide-facing and prop placement after deciding precedence with existing authored instructions.

## Changed files

Under `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/`:

- `domain/VisualAssembly.kt`: projection, presets, summary and scoped reset.
- `domain/CharacterProject.kt`: defaulted guide fields.
- `feature/editor/VisualAssemblyEditor.kt`: adaptive cards and disclosed chips.
- `feature/editor/SilhouettePreview.kt`: stateless local vector drawing.
- `feature/editor/CharacterComponentEditors.kt`: module routing.
- `feature/editor/EditorViewModel.kt`: initial/Quick destination and reset routing.

Tests under `shared/src/`:

- `commonTest/kotlin/com/kelvinsaputra/promptstudio/VisualAssemblyTest.kt`: four domain/state compatibility tests.
- `commonTest/kotlin/com/kelvinsaputra/promptstudio/CharacterLibraryTest.kt`: updated Quick fallback expectation.
- `jvmTest/kotlin/com/kelvinsaputra/promptstudio/VisualAssemblyUiTest.kt`: narrow interaction and wide rendering tests.

This document records scope and limitations. Final result: 89 JVM and 81 Android host tests, zero failures/errors/skips; all listed build/compile tasks and whitespace checks passed.
