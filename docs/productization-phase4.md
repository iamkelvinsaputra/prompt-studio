# Phase 4: reusable local projects

## Architecture and implementation slices

Inspection found working typed character models, atomic local autosave, character library operations, deterministic prompt compilation, visual assembly projections, guide rendering, native provider adapters, and persistent history. Missing pieces were variant ownership, persisted provider/guide preferences, reusable user presets, and a project entry surface. Existing detailed editors are still used; none were removed speculatively.

The implementation proceeded through buildable slices: persisted variant models and compatibility; Projects and variant UI; reusable presets and history association; adaptive layout and verification. No modules, dependencies, databases, services, credentials formats, compiler wording, or provider wire formats were added or changed.

`CharacterLibrary` remains the local workspace index. `CharacterProject` remains the complete input to the compiler, portable character JSON, and generation snapshots. `ProjectVariants` holds only additional scene configurations and per-variant preferences. The editor resolves one complete project at the boundary; the compiler and generation providers do not need to understand workspace ownership.

## Ownership

| Scope | Canonical values |
| --- | --- |
| Project | ID/name, character identity, art style, costume, appearance, powers, props, environment, lighting, colors, texture, exclusions and priorities |
| Variant | Pose (including facing/guide prop), gaze, composition, output/ratio/framing/placement/safe areas, adjustment text, automatic/manual mode, retained manual draft |
| Variant preferences | Provider, exact model ID, visual-guide preference; no credentials |
| Workspace | Active project, active variant per project, recent project order, reusable presets |
| Session | Navigation, expanded controls, randomization locks, pending requests, platform credential session |

The primary variant's scene stays in the original `CharacterProject`; alternatives contain `VariantScene` values only. Editing shared fields from an alternative updates the single shared character, while preserving the primary scene. There is no cascading inheritance or duplicated character per variant. Creating an alternative copies the current scene/prompt/preferences once and changes its output type. Subsequent scene edits are isolated. Manual mode remains manual after copying or switching variants; aspect or visual edits never overwrite a manual draft.

Primary variants can be renamed but not removed. Alternatives can be renamed/deleted. Duplicating a project copies its variants with new project and alternative IDs. Deleting projects or variants leaves their historical generations available in All Generations. Deleting the final project creates a blank replacement, preserving the existing nonempty-library invariant and user presets.

## Persistence and migration

Autosave still uses `CharacterLibraryJson` and the existing platform storage: atomic file replacement on native platforms and local browser storage on Web. The local envelope now writes `schemaVersion: 2`, variants, preferences, recency and presets. Existing envelopes without these fields decode with defaults into one primary variant per character; no eager rewrite occurs. Old single-character autosaves also load, including non-demo IDs (the previous fallback assumed the demo identity). Unsupported/corrupt saves retain the existing safe fallback and are not overwritten merely by opening the app.

Only canonical state is saved. Compiled prompts, visual projections, rendered guides, image bytes and credentials are not added to this envelope. Actual edits update memory immediately and write through the existing atomic store. Unchanged edits and transient rendering/navigation do not write. Failed writes preserve the complete session, display an actionable error and support Retry Save.

Persistence remains synchronous and bounded by the existing 1 MB limit. A debounce/background writer was intentionally not introduced: this keeps close-after-edit durability without a new flush lifecycle. Very large libraries may warrant profiling and an ordered background writer later.

Portable **Export Character** retains version-1 compatibility and exports the resolved active variant as one character configuration. It does not export the entire workspace, other variants, saved presets, preferences or generations. Import replaces the active variant's scene/prompt and shared character fields, leaving other variant scenes intact. Full workspace backup/import is deferred; preserve the autosave file when backing up all work. Older app versions are not guaranteed to read the new workspace envelope.

## Presets

`SavedVisualPreset` contains a stable ID, name and a typed `VisualPresetValue`. Only two supported categories exist:

- Pose: exact `PoseConfiguration`, including custom mechanics and guide-facing/prop choices.
- Composition: composition instructions, framing, placement, negative space and safe-area options. Applying it preserves the destination output type/aspect ratio, pose, shared character and prompt ownership.

Save pose / Save composition requires only a name. Reusable silhouette cards apply saved values in any project/variant. Saving or using a preset moves it to the front of the workspace list. Deleting a preset does not affect projects that used it. No references from live projects to mutable presets, tagging, search or empty categories were introduced.

## History and generation

New `GenerationMetadata.variant` stores the captured variant ID/name alongside the existing complete character snapshot, exact effective prompt, model/output, timestamp and optional guide spec. Legacy records without a variant remain readable. History retains separate metadata and image artifacts; guide PNG bytes remain transient. Edits, renames and deletion never rewrite prior records. Generate Again continues to replay the saved request and guide spec; it does not consult current editor state.

History defaults to the current project, offers All Generations, and displays variant and guide usage. Restore Configuration explains that it replaces shared character fields and the current variant's scene/prompt; it preserves other variant scenes and current provider preferences. Generation preferences restore per variant. A retired saved model requires explicit selection rather than silently generating with a replacement. Guide preparation now has its own honest loading label. Latest results are shown only within their project, with variant attribution.

## Product UX and accessibility

The app opens on a responsive recent-project grid with silhouette cards, create/open/rename/duplicate/delete. Creation takes a name and one output format; new work starts with a usable neutral visual composition. Opening returns to Visual Build. Desktop keeps variant tabs; narrow windows use an active-variant selector so the selected label stays visible. Both use the same state and editor.

Generate is directly accessible from the editor header. The existing prompt inspection route also remains available. Randomization and locks now sit under All sections, reducing default clutter. Already configured keys no longer expose an empty replacement form on each visit. Presets, adjustment and advanced manual ownership remain below the primary visual workflow.

Material buttons/chips/cards provide keyboard activation, focus behavior and touch targets. Preset cards include text and explicit apply/remove names; thumbnails have descriptive semantics. Project actions and variant deletion use confirmations where destructive. Screens at 360 and 1280 pixels were rendered and inspected. No global keyboard shortcuts or separate mobile product implementation were added.

## Assets and performance

Existing `VisualPose`/`VisualAssemblyState` identifiers remain independent of representation. Both previews and PNG guide generation use the centralized `guide/SilhouetteDrawing.kt` geometry in consistent 200 × 300 body coordinates. Professional assets can replace rendering in this boundary without changing project or prompt logic. Preserve the version-1 guide renderer for historical replay, or version new rendering with explicit support for old specs.

Silhouettes remain rough placeholders, including neutral approximations for custom unsupported choices. No external asset lookup or missing-file path was introduced. History continues lazy visible-row image decoding. Prompt text is remembered by project in the editor; no derived prompt cache is persisted. No speculative caching or asset framework was added.

## Files and validation

New production files: `domain/ProjectVariants.kt`, `domain/VisualPresets.kt`, `feature/editor/ProjectsScreen.kt`, `feature/editor/PresetLibrary.kt`.

Integration changes: `App.kt`, `domain/CharacterLibrary.kt`, `feature/editor/EditorViewModel.kt`, `feature/editor/EditorScreen.kt`, `feature/generation/GenerationPanel.kt`, `feature/generation/GenerationController.kt`, `generation/model/GenerationModels.kt`, `history/HistoryPanel.kt`.

Tests: `ProductizationTest.kt`, `ProductUiTest.kt`, and additional variant replay assertions in `VisualGuideGenerationTest.kt`. Coverage includes restart restoration of visual/manual/adjustment/provider/guide state, shared-field propagation, isolated scenes, duplication identities, preset application and round trips, legacy loading, failed-save recovery, unchanged historical inputs, and full create → visual choice → save preset → variant switch → apply preset → Generate navigation at narrow/wide sizes.

Validation command:

```sh
./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:compileKotlinWasmJs :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug
```

117 JVM tests and 102 Android host tests pass, along with Wasm/iOS simulator compilation and the Android debug build. UI captures are written to `/tmp/product-editor-{360,1280}.png` and `/tmp/product-projects-{360,1280}.png`. Tests initially exposed incorrect test viewport bounds; the journey suite now uses the public desktop test API's explicit window dimensions. Nested preset-row tests explicitly scroll the outer editor before interacting with the row.

Remaining manual validation: cold-close/reopen on Android, iPhone/iPad and browser storage; large font sizes and screen readers; one paid OpenAI/Gemini request with guide on/off; historical replay after switching projects; native disk-full and browser quota recovery. Provider calls in automated tests are mocked; no live paid requests were made. iOS/Wasm were compile-checked, not device/browser-runtime tested in this phase.
