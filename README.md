# Prompt Studio

An offline character-art prompt builder for Android and macOS Desktop, built with Kotlin Multiplatform and shared Compose UI. Select a locked style, edit costume and pose, choose output composition, then copy the live compiled prompt into your preferred image tool.

## Run

Use JDK 17+ and an Android SDK with API 36 installed. The existing Gradle toolchain and dependency versions are retained.

```sh
# Launch the macOS/Desktop app
./gradlew :desktopApp:run

# Build Android; run androidApp from Android Studio on a device/emulator
./gradlew :androidApp:assembleDebug

# All builds and checks
./gradlew build

# Shared domain/compiler/state tests on both targets
./gradlew :shared:jvmTest :shared:testAndroidHostTest
```

The Android APK is `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Use

- **Style:** the built-in Sumi-e Sky Blue RPG core is locked. The temporary subject is editable here.
- **Costume:** select one preset per slot and up to two personal customization markers. For custom garments, select **Not set** for the relevant slot and describe them in custom notes.
- **Pose:** select one value per category, with free text for leg action, motion direction, and custom notes.
- **Output:** wallpaper, square, and portrait presets derive their aspect ratios. Custom accepts positive whole-number ratios such as `3:2`. Invalid ratios remain editable and disable Copy Prompt until corrected.
- **Prompt:** selectable live text, available as a dedicated page and alongside the editor in wide windows. **Copy Prompt** copies the complete prompt from any component.

Edits survive Android activity configuration changes through the shared ViewModel. Closing/restarting the app or Android process death resets the project. Local saving, serialization/export, and randomization are deferred. There is no image generation, backend, or network integration.

## Code boundaries

The existing three Gradle modules remain: two thin launchers and `shared`.

- `shared/src/commonMain/.../domain`: immutable configurations, typed preset enums, locked style, slot limits, and aspect-ratio validation.
- `shared/src/commonMain/.../prompt`: deterministic section compiler; no Compose dependencies. Empty values are omitted, customization follows catalog order, and a small fixed priority stack follows the design guide.
- `shared/src/commonMain/.../feature/editor`: ViewModel with immutable `StateFlow`, lifecycle-aware collection in `App`, adaptive editors, and preview derived from the project. No independently mutable prompt state.
- `shared/src/commonMain/.../platform`: one `expect` function constructing a text `ClipEntry`. Android uses `ClipData`, Desktop uses AWT `StringSelection`; the actual clipboard write uses Compose `LocalClipboard` in shared UI. These constructors are the only added platform boundary.
- `shared/src/commonTest`: compiler, validation, and editor state tests.

Vocabulary and style wording come from [the character prompt cheatsheet](docs/character_prompt_cheatsheet.md). The V0 component set follows the implementation request; the other character modules are intentionally absent.
