# Guided character creation

Implemented from `prompt-studio-astra-ux-refinement.md` (September 2026). This refinement supersedes the older dashboard-first journey while retaining the warm surfaces, editorial typography, Studio, local storage and native generation boundaries.

## Journey

New project creates a valid portrait study and opens Character immediately. Ten major steps cover Character, Appearance, Pose, Camera, Environment, Lighting, Style, Color, Effects and Review. One fixed Continue button advances; Back preserves choices. Review offers direct section edits with Return to Review, Generate and a secondary Open Studio action. The current step is saved in `creationStep`; null denotes Studio, including legacy files. Projects can be left and resumed without losing progress. No credentials or generation controls appear during the nine authoring steps.

Studio uses the same controls and compiler. Presets and variants are contextual. Save preset opens one menu for character, pose, camera, style or complete presets. Project menu and Projects have distinct labels. The inspector has Structure and Prompt modes; Copy prompt belongs to Prompt. The large approximate sketch no longer occupies the inspector.

## Character and scene ownership

`IdentityConfiguration.characterName` is the character's semantic handle, distinct from the project title. Older projects fall back to their project name. `CharacterProfile` adds optional skin, eyes, jaw, nose, hair texture and distinguishing features while retaining the existing typed body/face/hair fields as their sole source of truth. The canonical compiler emits those traits in a stable order, independently of expression, costume, camera, scene, style and palette. New profiles use the CHARACTER IDENTITY section; legacy authored sections remain readable. Names alone do not guarantee image consistency.

Trait locks are serialized with the profile. The library applies them to edits, randomization and preset application; unlock a trait explicitly before changing it. Confirmed history restore deliberately restores the historical profile and its lock state, even over current locks. Variants share the canonical character and independently retain appearance, expression, pose, camera, environment, lighting, style, palettes, effects, output, exclusions and prompt drafts. Historical variant fields that did not exist are nullable and continue to inherit the corresponding legacy shared data. New variants capture all these fields explicitly. No seed/reference-image/LoRA functionality is simulated.

The two selectable character-template branches are Male and Female. Legacy enum values remain decodable, but are absent from controls and randomization. Six age presentations include Teen and Late teen. Outfit templates are distinct per branch across 17 collections (34 complete starting outfits); templates do not constrain the free builder. Garment vocabulary extends the existing cheatsheet with overshirts, cardigans, athletic jackets, tees, blouses, tanks, straight trousers, cargos and training wear.

## Art direction

Environment offers nine illustrated categories, contextual locations and optional time, weather, season, depth, density, detail, foreground, architecture and mood. Switching categories clears incompatible structured details while retaining custom notes. Effects use nine families with up to six items, subtle default intensity and optional direction/custom text; None clears structured and legacy effects.

Lighting offers ten presets and ten replaceable direction diagrams. Presets populate direction, quality, temperature and contrast. Changing a property marks it custom. Color offers semantic text, twelve palettes, five optional color roles, validated HEX entry and live RGB sliders. Saturation, contrast, temperature and brightness are optional 0–100 controls. Swatch roles and effect items compile in deterministic order. Palette and style state are independent. Twenty-three curated style families retain advanced rendering and manual section editing.

## Compatibility

The portable version-1 project format and version-2 library envelope remain additive. Missing fields use defaults without discarding authored legacy data; unsupported enum values and malformed palette ranges remain validation errors. JSON still excludes credentials, provider results and image artifacts. Generation history continues to store exact press-time configuration and prompt. Restoring into another project keeps its container ID/title and explicitly restores the historical character name. Historical Generate Again still uses the saved prompt, not recompilation.

All authoring, controls and deterministic compilation remain in commonMain. There are no new Gradle modules, frameworks, network services or runtime dependencies. Web still has no provider implementations or credential entry. Native generation uses the existing BYOK flow.

## Validation

`CreativeDirectionTest` covers canonical identity stability, locks/unlocking, variant isolation, HEX validation, palette ordering and style independence, exact JSON round trips, guided defaults and template coverage. `ProductUiTest` traverses all ten steps at 390×844, 850×1000 and 1440×1000, edits from Review, checks invalid HEX, opens Studio, inspects prompts, saves a preset, creates a variant and opens generation. It captures each major screen under `shared/build/reports/studio` for visual inspection. Existing compiler, persistence, storage, provider and history suites remain in place.

Live provider image quality is not part of these automated journeys; generation/controller and provider tests use fixtures and mocks. Real image consistency depends on the external model.

Final verification on 2026-09-11 passed with zero failures, errors or skips: JVM 145 tests, Android host 129, ChromeHeadless/Wasm 119, iOS simulator 129 (522 total). Android debug assembly, Desktop assembly, production Web distribution, iOS simulator debug framework linking and iOS ARM64 compilation passed. `git diff --check` passed. All 24 new lighting, environment and camera PNGs decoded successfully. The Web build retains the existing Compose/Skia bundle-size warnings. No live provider request or physical-device installation was performed.

```sh
./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:wasmJsBrowserTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :desktopApp:assemble :shared:wasmJsBrowserDistribution :shared:linkDebugFrameworkIosSimulatorArm64 :shared:compileKotlinIosArm64 --continue --console=plain
```
