# Prompt Studio: creative workspace redesign

The studio now uses one responsive authoring workspace. Its artist-facing categories group every existing authoring module into Subject, Appearance, Pose & acting, Camera & composition, Environment, Lighting, Style, Color, Effects & atmosphere, Output, and Negative guidance. Detailed fields remain available under Refine; the older guided wizard and duplicate visual editor UI were removed.

## Interaction and visual system

The desktop workspace has a compact navigator with current-choice summaries, a spacious editor, and a live prompt inspector. Below 1180 dp the inspector becomes a separate view. Below 760 dp the navigator becomes a horizontally scrollable category strip; phone actions stay pinned below the editor. Text sizes, spacing, surfaces, borders and the warm terracotta accent are centralized in `StudioTheme`. Libre Baskerville is bundled for consistent display typography across native and Web targets; its [upstream source and OFL license](https://github.com/google/fonts/tree/main/ofl/librebaskerville) are accompanied by a packaged license in `composeResources/files/licenses`.

Spatial choices share `VisualOptionCard` and `VisualOptionGrid`, including gesture, facing, framing, composition, subject placement, camera angle, gaze, head direction, arms and prop placement. Large libraries expose search and compact cards. Six common poses lead the default gallery. Cards have textual labels, keyboard activation, focus borders and stable hover-detail space. Image decoding is asynchronous to composition with a bounded session cache. The current libraries are small; the expanded grid composes its filtered options rather than introducing a pagination framework.

The 36 starter PNGs reuse the existing renderer. Artwork is deliberately provisional. See [VISUAL_GUIDES.md](VISUAL_GUIDES.md) for asset paths, registry keys, formats, replacement instructions, fallbacks, and the distinction between replaceable option artwork and the versioned combined structural sketch.

New projects use a neutral illustration style, a clear standing pose and a valid output format. They do not assume a gender, age, clothing aesthetic or rendering technique. Existing saved projects retain their style and configuration. Simple outfit starting points use the established costume vocabulary. Every major category accepts custom wording; style editing can replace only the style section or the full prompt through the existing manual ownership controls.

## Preserved capabilities and domain changes

All character fields, slot limits, local project operations, imports/exports, autosave, variants, native BYOK generation and history remain available. Presets now support character, style and complete configurations as well as pose and composition. Applying a complete preset preserves destination ID/name. Style and character presets apply only their owned components. History, credentials and generated images remain separate from Character JSON.

Camera angles are typed additions to `CompositionConfiguration` with a nullable serialization default. Environment treatment adds explicit no-background and full-environment options. Existing option wording stays in the domain and compiler, not asset JSON or UI markup. Duplicate entries in simple prompt lists are removed deterministically; user-authored phrases otherwise retain their wording. Compiler section ordering and legacy style behavior remain intact. New values and preset types require this updated app; this is backward compatibility for existing files, not a promise that older binaries understand new options.

Category resets affect only the category's owned fields; Reset all confirms replacement of the active configuration with the neutral starting point. Scoped randomization supports pose, outfit, camera and colors, preserving custom text. Existing field locks remain under Refine and category locks under Presets & variants → Explore unlocked choices. Switching output variants now preserves the active creative category.

The native generation panel and history controller still enforce the platform capability boundary. Web offers authoring and local storage with no API-key entry or direct provider requests. No network generation occurs automatically.

## Validation — September 11, 2026

Final command passed:

```sh
./gradlew :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:lintDebug :desktopApp:assemble :shared:iosSimulatorArm64Test :shared:compileKotlinIosArm64 :shared:wasmJsBrowserTest :shared:wasmJsBrowserDistribution --console=plain
```

Final result: 161 Gradle tasks, successful in 54 seconds. Test result XML reports:

| Target | Tests | Failures / errors / skips |
| --- | ---: | ---: |
| JVM / Desktop | 138 | 0 / 0 / 0 |
| Android host | 122 | 0 / 0 / 0 |
| iOS simulator | 122 | 0 / 0 / 0 |
| Web / ChromeHeadless | 112 | 0 / 0 / 0 |

Compose UI journeys exercise 390×844 phone, 850×1000 tablet and 1440×1000 desktop layouts. They create projects, edit subjects and custom appearance, select/search poses (including missing-artwork options), change framing/position/composition, write camera instructions, select and customize styles, save complete presets, create variants, reset categories, inspect compiled prompts, copy to the actual system clipboard, and open generation. A separate keyboard test activates an artwork-free option with Tab and Enter. Existing generation/history, storage, compiler and platform tests continue to pass.

Screenshots are generated under `shared/build/reports/studio`. Desktop and phone screenshots were visually reviewed. The production Web bundle was also opened in the in-app browser to review real resource loading and browser typography. Review led to fixes for hover layout shifts, variant navigation, missing Web glyphs, and a copy snackbar that overlapped phone actions.

Android lint completed with no errors and 30 warnings about dependency/SDK versions and existing launcher/platform code. Android was compiled and linted, and Apple device code was compiled; no physical-device smoke test or live provider request was made. Provider and history regression tests use fixtures/mocks. Webpack continues to report the Compose/Skia Wasm bundle-size warnings. The app adds no new framework or Gradle module. Favorites and a separate recent-option system were omitted; saved presets already move recently used items to the front.
