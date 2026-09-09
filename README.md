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

- **Style:** the built-in Sumi-e Sky Blue RPG core is locked. Its full text is collapsed behind **View Style Prompt**. The temporary demo subject stays fixed.
- **Costume:** select one preset per slot and up to two personal customization markers. For custom garments, select **Not set** for the relevant slot and describe them in custom notes.
- **Pose:** select one value per category, with free text for leg action, motion direction, and custom notes.
- **Output:** wallpaper, square, and portrait presets derive their aspect ratios. Custom accepts a ratio or descriptive string such as `3:2` or `widescreen`. Blank ratios disable Copy Prompt until filled.
- **Prompt:** selectable live text, available as a dedicated page and alongside the editor in wide windows. **Copy Prompt** copies the complete prompt from any component.

Edits automatically save locally and restore on launch. Locks and selected sections remain session-only. Use **Project → Export Project** for a portable JSON backup and **Project → Import Project** to replace the current project. There is no image generation, backend, or network integration.

## Code boundaries

The existing three Gradle modules remain: two thin launchers and `shared`.

- `shared/src/commonMain/.../domain`: immutable configurations, typed preset enums, locked style, slot limits, and output defaults.
- `shared/src/commonMain/.../prompt`: deterministic section compiler; no Compose dependencies. Empty values are omitted, customization follows catalog order, and the six V0 sections compile in a fixed order: style, output, subject, costume, pose, composition.
- `shared/src/commonMain/.../feature/editor`: ViewModel with immutable `StateFlow`, lifecycle-aware collection in `App`, adaptive editors, and preview derived from the project. No independently mutable prompt state.
- `shared/src/commonMain/.../platform`: one `expect` function constructing a text `ClipEntry`. Android uses `ClipData`, Desktop uses AWT `StringSelection`; the actual clipboard write uses Compose `LocalClipboard` in shared UI. Native project storage and document pickers are the other small platform boundary.
- `shared/src/commonTest`: compiler, validation, and editor state tests.

Vocabulary and style wording come from [the character prompt cheatsheet](docs/character_prompt_cheatsheet.md). The V0 component set follows the implementation request; the other character modules are intentionally absent.

## Editor layout and state

`App` owns the shared `EditorViewModel` and collects its `StateFlow` with lifecycle awareness. Section editors receive immutable configurations and focused module callbacks. The prompt is synchronously derived from the project, never stored as separate mutable state.

`EditorScreen` uses available content width: below 840 dp, horizontally scrolling section chips sit above a single editor; at 840 dp, a sidebar appears; at 1240 dp, the live preview appears alongside the editor. Each editor remembers its scroll position when switching sections. The sidebar also scrolls in short windows.

All editors, preview, state, and clipboard feedback live in `commonMain`. Only constructing the clipboard payload needs Android `ClipData` in `androidMain` and AWT `StringSelection` in `jvmMain` (this repository's Desktop source set). Compose's shared `LocalClipboard` performs the write.

## Explore costume and pose

Each module has **Randomize**, **Reset**, **Lock All**, and **Unlock All**. Lock buttons beside finite-choice fields protect those values from randomization; manual edits remain available. Authored fields are always preserved during randomization, so they need no lock toggle. Customization locks apply to the whole marker selection, including an empty selection.

**Reset** restores the entire selected module to its demo configuration, including notes and locked values. It preserves that module's lock choices and all other modules. This behavior is stated beside the controls.

`EditorRandomizer` is plain Kotlin editor logic, accepts `kotlin.random.Random`, and chooses only existing vocabulary. Inject `Random(seed)` for repeatable tests. Single-choice fields select a different value when alternatives exist; customization selects a different set of zero, one, or two markers. Pose weight suggestions avoid obvious seated/standing mismatches, but locks and authored notes take precedence, so review their compatibility when exploring.

Locks are typed sets in `EditorUiState`, outside `CharacterProject`. They never enter compiled prompts and last only for the session. The compiler remains unchanged and deterministic.

## Local saving and portable JSON (V0)

The version-1 JSON file directly represents `CharacterProject`:

```json
{
  "version": 1,
  "style": { "id": "sumi-e-sky-blue-rpg", "name": "Sumi-e Sky Blue RPG", "prompt": "..." },
  "subject": "...",
  "costume": { "...": "..." },
  "pose": { "...": "..." },
  "output": { "...": "..." }
}
```

This schematic example abbreviates configuration contents. Actual exports include all default values, authored text, enum selections, and nulls; they contain no locks, navigation, or compiled prompt. `ProjectJson` uses kotlinx.serialization with explicit defaults and pretty printing. Only version 1 is supported; missing sections, wrong types, unknown enum values, and invalid customization counts are rejected. Files are limited to 1 MB.

Autosave happens synchronously at the ViewModel's project-edit boundary, only when the project changes. This deliberately favors simple, immediate durability for a small local file, including closing right after an edit, over a debounce or background-save lifecycle. Native dialogs read/write external files on an I/O dispatcher. There are no writes on recomposition, section changes, or lock changes. If projects become large, autosave scheduling can be revisited.

- **Android:** `noBackupFilesDir/current-project.json`, atomically replaced with `AtomicFile`. This is app-private and excluded from automatic OS backup. OpenDocument/CreateDocument use the system picker without broad storage permissions.
- **macOS/Desktop:** `~/Library/Application Support/Prompt Studio/current-project.json`. Writes use a temporary file in the same directory, sync its contents, then atomically replace the target. Import/export uses native AWT `FileDialog`.
- **Shared:** serialization, validation, startup fallback, import state replacement, and save/error decisions live in commonMain. The compiler is unchanged.

A missing save starts with the demo. An unreadable, corrupt, or unsupported save shows a warning and leaves the original file untouched until an explicit edit, valid import, or retry. Failed imports leave the active project and local save untouched. Successful imports preserve current session locks and immediately autosave. Save failure keeps in-memory edits and displays **Retry Save**; export can still provide a backup. Cancelling a picker changes nothing.

Study `ProjectJson.kt`, `ProjectStorage.kt`, `EditorViewModel.kt`, the platform `ProjectFiles` implementations, and `ProjectJsonTest`/`ProjectPersistenceTest`/`DesktopProjectStorageTest` for the complete flow.
