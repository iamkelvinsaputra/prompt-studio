# Prompt Studio

Prompt Studio is an offline character-art prompt authoring tool for Android, macOS Desktop, iPhone/iPad and Web (Kotlin/Wasm). It turns the modular character system in [the character prompt cheatsheet](docs/character_prompt_cheatsheet.md) into a typed local editor and a deterministic live prompt.

The studio combines artist-facing categories, replaceable silhouette libraries, custom descriptions, a structured live prompt inspector, reusable presets, and independent output variants. Native apps support user-triggered OpenAI/Gemini generation with your own API keys. Editing and persistence work locally; no Prompt Studio backend is involved.

See the [studio redesign and validation report](docs/studio-redesign.md) and [visual artwork replacement guide](docs/VISUAL_GUIDES.md).

## V2.1 cross-platform generation and history

Open **Generate**, choose a provider/model, enter your API key and press **Use key**, inspect the exact prompt and effective output, then **Generate Current**. You can cancel, save the latest image, or regenerate its captured snapshot. Generation is paid through your provider account and never starts automatically.

Android offers optional Keystore-backed key storage; Desktop and Apple keys are session-only. Projects never include credentials. Successful native generations persist locally in History with exact configuration/prompt snapshots and separate image artifacts. History supports inspect, save, copy, restore, Generate Again and delete. Web provides complete local authoring and history infrastructure, but intentionally has no provider keys or direct provider generation.

See the [V2.1 implementation and validation report](docs/v2.1-cross-platform-history.md), and the earlier [V2 provider implementation](docs/v2-generation.md).

## V2 visual workflow and productization

Open a recent project or create one, choose a visual pose, and use **Generate** in the editor header. Projects support independent output variants with shared character/style/costume, persisted manual/adjustment state and provider/guide preferences. Open **Presets & variants** to save a pose, composition, character, style, or complete configuration for reuse across projects. History preserves each generation's exact prompt, variant and optional visual guide.

See the [Phase 4 architecture, migration and validation report](docs/productization-phase4.md). Portable Character JSON still exports the active variant only; local autosave preserves the complete project library and presets.

## Run

Use JDK 17+ and an Android SDK with API 36 installed.

```sh
# Launch macOS/Desktop
./gradlew :desktopApp:run

# Build an Android debug APK
./gradlew :androidApp:assembleDebug

# Run Android/Desktop shared tests
./gradlew :shared:jvmTest :shared:testAndroidHostTest

# Run Web locally
./gradlew :shared:wasmJsBrowserDevelopmentRun

# Apple: open iosApp/PromptStudio.xcodeproj and run the PromptStudio scheme
# Requires full Xcode and an installed iOS simulator runtime.

# Build everything
./gradlew build
```

The Android APK is `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Studio workflow

1. Open a study or create a project with a usable neutral configuration.
2. Move through Subject, Appearance, Pose, Camera, Environment, Lighting, Style, Color, Effects, Output, and Negative guidance.
3. Choose visual cards or descriptive chips, add custom wording, and open **Refine** for the full character controls.
4. Inspect the live building blocks or compiled prompt. Copy it, export the active configuration, or generate in a native app.
5. Use **Presets & variants** to save reusable choices, create output variations, and explore with locks.

Desktop provides a category navigator, creative editor, and prompt inspector. Tablet retains the navigator and opens the inspector separately. Phones use a horizontally scrollable category strip, a sequential editor, and persistent Inspect / Copy / Generate actions.

## Architecture

The project still has three Gradle modules: thin Android/Desktop launchers and a shared Kotlin Multiplatform module, plus a thin Xcode Apple launcher.

- `shared/.../domain` contains immutable project components, bounded collection validation, typed finite vocabulary, the local `CharacterLibrary`, and modular style configurations.
- `shared/.../prompt/PromptCompiler.kt` is a pure deterministic renderer. It owns the canonical cheatsheet order and omits unfilled components.
- `shared/.../feature/editor` contains detailed authoring controls, session-only locks, library state, and plain-Kotlin randomization.
- `shared/.../feature/studio` contains the shared theme, creative categories, option cards, asset loading, and prompt inspector.
- `shared/.../guide/VisualGuideRegistry.kt` connects typed options to the file-driven visual library. Artwork metadata lives in `composeResources/files/visual-guides/registry.json`.
- `shared/.../persistence` keeps single-character `ProjectJson` portable for import/export. Autosave uses `CharacterLibraryJson`; an existing V0 single-project autosave is read as a one-character library on first launch.
- `shared/.../platform` contains small storage, file, clipboard, image and generation capability boundaries.

The project schema intentionally remains version `1`: V1 fields have serialization defaults, so a valid V0 export (style, subject, costume, pose, output) opens without a generic migration system. New V1 exports include character ID/name and all component data; locks, selected module, and compiled prompts are never exported.

## Key limits

- Personality: 7 traits
- Accessories: 3 items
- Shape language: 6 principles
- Avoid: 15 active exclusions
- Priority stack: 5 items
- Costume customization: 2 markers

These are authoring limits from the cheatsheet, not image-generation constraints.
