# Phase 2: visual choices to prompt authoring

This extends the existing Visual Build module, CharacterProject, editor update/save path and deterministic compiler. No parallel document architecture, provider feature or Phase 3 work was introduced.

## Ownership

- `CharacterProject` remains the canonical character. `visualAssembly` is a read-only projection of pose and output, not a second mutable configuration.
- `PromptAuthoring.adjustmentText` is independent authored language. The compiler appends a labeled section only when the text is nonblank, with an explicit instruction to prioritize the adjustment in conflicts. Leading/trailing whitespace is trimmed for compilation; internal text is preserved. No interpretation or reverse parsing occurs.
- `PromptCompiler.compile(project)` always means automatic compilation, regardless of manual mode. It returns ordered `PromptSection` values with derived `text`, preserving existing cheatsheet ordering.
- `PromptAuthoring.manualDraft` and `mode` explicitly own manual output. `effectivePrompt()` is the single output selector used by the editor viewer, Copy and the already-existing generation preview/controller.
- An empty manual draft remains empty and disables Copy/generation. It cannot silently fall back to automatic text.

## Transitions

Fresh entry into Manual Prompt copies the current automatic compilation, including any adjustment. Entering again while already manual does nothing. Manual typing changes only the draft. Visual edits, detailed editors, adjustment edits, randomization and module resets cannot overwrite it.

Returning to automatic derives output from current structured choices plus adjustment and retains the manual draft. A fresh Manual Prompt action starts from current compilation; a separate Resume saved manual draft action restores the retained draft. Mode and draft follow the selected character, including autosave/restart and existing JSON import/export.

## Compiler mapping and precedence

Pose, framing and figure placement feed the existing POSE and COMPOSITION sections through `visualAssembly`. Facing and primary prop placement now compile too; Phase 1's `guideFacing` and `guideProp` field names are retained for JSON compatibility.

Facing describes body orientation independently of dedicated head/gaze controls and torso mechanics. A held-down or shoulder prop suppresses the stored arm-action bullet in automatic output, while preserving that stored choice. Selecting No prop restores the arm-action bullet and means no prop is held in the pose; it does not erase the separate authored prop identity/story block. The generic preview bar does not invent a weapon type. Authored negative-space instructions remain authoritative; selecting Lower does not silently rewrite them.

Natural-language notes may still contradict structured choices. Phase 2 preserves authored text and provides explicit precedence for Describe adjustment, rather than trying to interpret arbitrary text. Manual output replaces the entire automatic prompt.

## UI

Visual Build stays visually dominant, with its automatic side preview hidden until the user opens the prompt destination. Describe adjustment discloses a small field with Done, Clear and an active indicator. Advanced discloses Manual Prompt, whose active state is visible globally; the generated viewer stays read-only and identifies automatic versus manual output. The existing Quick/Advanced navigation filter is now labeled Quick/All sections to distinguish section visibility from manual editing. No new navigation architecture or design system is involved.

## Persistence and compatibility

Only the new authored inputs and explicit mode/draft are persisted. Automatic compiled sections/text are not serialized. Old project, library and history configurations without `promptAuthoring` default to automatic with an empty adjustment and no manual draft. Invalid manual state without a draft is rejected. Existing size limits/error handling apply, including larger manual drafts.

Older app versions use strict JSON decoding and may reject exports containing the new fields. Existing historical captured prompt strings remain untouched; history storage/schema was not rewritten. The generation boundary merely uses selected output when an existing Generate Current action is invoked. No live calls were made.

## Validation

Commands:

```sh
./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:compileKotlinWasmJs :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug --console=plain
git diff --check
```

Focused shared tests cover deterministic visual mappings, per-property section changes, arm precedence, adjustment/clear, all automatic/manual transitions, no reverse parsing, empty manual drafts, invalid manual state, autosave/restart, character switching, reset/randomization preservation and legacy defaults. One narrow Compose interaction test exercises adjustment, manual entry/editing, visual changes while manual, return to automatic and clearing. The existing fake-provider suite checks exact manual request text and empty-draft rejection; it does not call a real provider.

Final result: **97 JVM tests and 88 Android host tests passed**, with zero failures/errors/skips. The narrow manual-editing capture was visually inspected and the diff whitespace check passed.

Android is assembled and host-tested; iOS simulator and Wasm source sets are compiled. No new device/simulator or browser runtime smoke test was performed in this phase.

## Changed files for Phase 2

Paths below are relative to `shared/src/`.

- `commonMain/kotlin/com/kelvinsaputra/promptstudio/domain/PromptAuthoring.kt`: serialized authored inputs and invariant.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/domain/CharacterProject.kt`: defaulted authoring field; guide documentation.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/prompt/PromptAuthoring.kt`: output selector and pure transitions.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/prompt/PromptCompiler.kt`: sections, visual wording, precedence and adjustment.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/editor/PromptAuthoringControls.kt`: progressive text controls.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/editor/VisualAssemblyEditor.kt`: integration and updated labels.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/editor/EditorScreen.kt`: output viewer/Copy, active indicator and navigation label.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/editor/EditorViewModel.kt`: derived effective output.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/generation/GenerationController.kt`: existing action uses selected output and rejects blank text.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/generation/GenerationPanel.kt`: existing exact-prompt preview uses the same selector.
- `commonMain/kotlin/com/kelvinsaputra/promptstudio/persistence/ProjectJson.kt`: format comment updated.
- `commonTest/kotlin/com/kelvinsaputra/promptstudio/PromptAuthoringTest.kt`: ownership/compiler/persistence tests.
- `commonTest/kotlin/com/kelvinsaputra/promptstudio/PromptCompilerTest.kt`: expected visual wording.
- `commonTest/kotlin/com/kelvinsaputra/promptstudio/VisualAssemblyTest.kt`: guide choices now affect compilation.
- `commonTest/kotlin/com/kelvinsaputra/promptstudio/GenerationStateTest.kt`: existing generation boundary regression.
- `jvmTest/kotlin/com/kelvinsaputra/promptstudio/VisualAssemblyUiTest.kt`: updated labels and complete authoring interaction.

Documentation: this file and the historical Phase 1 document link. The pre-existing uncommitted Phase 1 changes were preserved.

## Smallest next step

Smoke-test this exact automatic → adjustment → manual → automatic sequence on an iPhone and in the browser, including restart and a character switch, before extending features. The placeholder silhouette still does not render free text or detailed anatomy; no parsing, AI rewriting or image-reference submission has been added.
