# Prompt Studio

Prompt Studio is an offline character-art prompt authoring tool for Android, macOS Desktop, iPhone/iPad and Web (Kotlin/Wasm). It turns the modular character system in [the character prompt cheatsheet](docs/character_prompt_cheatsheet.md) into a typed local editor and a deterministic live prompt.

V1 supports a local character library, Quick and Advanced editing, complete character components, controlled random variations with session-only locks, portable JSON export/import, autosave, and clipboard copy. V2 adds user-triggered OpenAI/Gemini image generation using your own API keys. Editing and persistence still work offline; no Prompt Studio backend is involved.

## V2.1 cross-platform generation and history

Open **Prompt → Generate Image**, choose a provider/model, enter your API key and press **Use key**, inspect the exact prompt and effective output, then **Generate Current**. You can cancel, save the latest image, or regenerate its captured snapshot. Generation is paid through your provider account and never starts automatically.

Android offers optional Keystore-backed key storage; Desktop and Apple keys are session-only. Projects never include credentials. Successful native generations persist locally in History with exact configuration/prompt snapshots and separate image artifacts. History supports inspect, save, copy, restore, Generate Again and delete. Web provides complete local authoring and history infrastructure, but intentionally has no provider keys or direct provider generation.

See the [V2.1 implementation and validation report](docs/v2.1-cross-platform-history.md), and the earlier [V2 provider implementation](docs/v2-generation.md).

## V2 visual workflow and productization

Open a recent project or create one, choose a visual pose, and use **Generate** in the editor header. Projects support independent output variants with shared character/style/costume, persisted manual/adjustment state and provider/guide preferences. Save pose or composition cards to reuse across projects. History preserves each generation's exact prompt, variant and optional visual guide.

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

## V1 workflow

1. Select an existing character or create one in the Character Library.
2. Use **Quick** for the minimum viable fill set, or **Advanced** for every cheatsheet component.
3. Keep key structured controls locked and use **Randomize Unlocked** to explore deterministic prompt variations. Free-form writing is never randomized.
4. Read or copy the live prompt; export the active character as portable JSON when needed.

On wide desktop windows, the library, grouped component navigation, editor, and live preview appear progressively as room permits. Phone layouts keep the character selector and compact component navigation above one editor.

## Architecture

The project still has three Gradle modules: thin Android/Desktop launchers and a shared Kotlin Multiplatform module, plus a thin Xcode Apple launcher.

- `shared/.../domain` contains immutable project components, bounded collection validation, typed finite vocabulary, the local `CharacterLibrary`, and the locked art style.
- `shared/.../prompt/PromptCompiler.kt` is a pure deterministic renderer. It owns the canonical cheatsheet order and omits unfilled components.
- `shared/.../feature/editor` contains shared Compose UI, Quick/Advanced navigation, session-only locks, library state, and plain-Kotlin randomization.
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
