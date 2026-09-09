# Prompt Studio V2.0 — implementation and verification

V2 adds explicit, paid BYOK text-to-image generation to the existing offline editor. The character schema and deterministic PromptCompiler are unchanged. There is no backend, gallery, account system, prompt rewriting, or provider SDK.

## Workflow

Open **Prompt → Generate Image**, select a provider/model, enter a key, then press **Use key**. The input is cleared after storage. Inspect the exact prompt and effective output, then press **Generate Current**. Only Generate Current, Try Again, and Regenerate initiate network generation. Each requests one image.

You can navigate back to the editor during generation. **Cancel** cancels the coroutine and HTTP request; it cannot promise that the provider stopped processing or will refund a request. **Regenerate** uses the latest successful image's captured character, prompt, model, provider, and output settings. **Generate Current** uses the editor as it is now. Regenerate looks up the current credential for the captured provider, allowing key replacement.

## 1. Provider abstraction

`ImageGenerationProvider.generate(request, credentials)` is a suspend contract returning owned bytes, MIME type, and an optional provider request ID. Implementations have no Compose dependency. `GenerationController` detaches the entire project through its existing JSON codec, compiles that snapshot once, and submits the resulting text without semantic transformation. Request, metadata, and credentials are separate types.

## 2–3. Models and official protocols

Official documentation checked on **2026-09-09**:

| Provider | Supported model IDs | Protocol |
| --- | --- | --- |
| OpenAI | `gpt-image-2.5-sunburst` (default), `gpt-image-2` | POST `https://api.openai.com/v1/images/generations`; Bearer header; `n=1`, PNG, medium quality; decode `data[0].b64_json`; retain `x-request-id` |
| Gemini | `gemini-3.1-flash-image` | POST `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`; `x-goog-api-key` header; one candidate, IMAGE modality, 1K image configuration; decode non-thought `inlineData`; retain `responseId` |

The specification's approximate OpenAI default was superseded by current official guidance recommending GPT Image 2.5. GPT Image 2 remains explicitly supported. Both use the same V2 settings; advanced quality modes are not exposed.

Google's main guide now leads with Interactions. Its official generateContent image guide still explicitly documents this model and protocol, although the documentation labels that API family “Legacy.” V2 uses the supported, stateless generateContent contract; it does not use Imagen or introduce conversation storage.

Official references:

- [OpenAI Images API request and response reference](https://developers.openai.com/api/reference/resources/images/methods/generate)
- [OpenAI image-generation guide](https://developers.openai.com/api/docs/guides/image-generation)
- [GPT Image 2.5 Sunburst](https://developers.openai.com/api/docs/models/gpt-image-2.5-sunburst)
- [GPT Image 2](https://developers.openai.com/api/docs/models/gpt-image-2)
- [OpenAI errors](https://developers.openai.com/api/docs/guides/error-codes)
- [Gemini generateContent native image generation](https://ai.google.dev/gemini-api/docs/generate-content/image-generation)
- [Gemini generateContent reference](https://ai.google.dev/api/generate-content)
- [Gemini troubleshooting](https://ai.google.dev/gemini-api/docs/troubleshooting)
- [Gemini current image-generation guide](https://ai.google.dev/gemini-api/docs/image-generation)

## 4. Network architecture

Ktor Client 3.4.0 and kotlinx.serialization JSON are shared in commonMain. Android uses OkHttp, with connection retries and redirects explicitly disabled. Desktop uses CIO with one connection attempt. There is no HTTP retry plugin, logging plugin, telemetry, provider SDK, new Gradle module, or startup network request. Central timeouts are 300 seconds for request/socket and 30 seconds for connection. The packaged Desktop runtime includes EC cryptography for HTTPS.

## 5–6. Credentials

**Android:** session-only by default; optional “Remember securely on this device” uses a generated Android Keystore AES key with standard AES/GCM/NoPadding. Keystore keys are not exported. Ciphertext and random IV are saved atomically under `noBackupFilesDir`, separate from projects. No hardcoded wrapping key or custom cryptographic algorithm is used. Storage failures are reported without exception text; the user can choose session-only storage. Removal deletes the provider's encrypted blob and memory entry.

**Desktop/macOS:** session-only. A dependable Keychain integration would require a native bridge/dependency; command-line secret arguments and ordinary JSON persistence were rejected. The UI explicitly states the limitation. Keys are released when the application composition ends. No credentials are saved through `rememberSaveable`.

`ProviderCredentials` is not serializable and its string representation is redacted. Saved keys are never repopulated into the text field. Replace by entering a new key, or remove explicitly. Standalone key validation is omitted, so entering a key makes no paid call.

References: [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [Apple Keychain Services](https://developer.apple.com/documentation/security/keychain-services).

## 7. State and snapshots

`GenerationState` has Idle, Generating (captured metadata), Success, Error, and Cancelled. `GenerationUiState` separately retains only the latest successful image. Failed or cancelled attempts preserve that image. Generating is set synchronously before launching, preventing duplicate taps. Cancellation propagates; cancelled jobs cannot overwrite newer results. The controller lives at app scope, so changing editor modules does not cancel generation.

## 8. Aspect ratios

| Authored ratio | OpenAI fixed output | Gemini setting |
| --- | --- | --- |
| 1:1 | 1024×1024 | 1:1, 1K |
| 9:16 | 864×1536 | 9:16, 1K |
| 16:9 | 1536×864 | 16:9, 1K |
| 4:5 | 1024×1280 | 4:5, 1K |
| 3:2 / 2:3 | 1536×1024 / 1024×1536 | matching ratio, 1K |

For other numeric ratios, OpenAI mapping searches dimensions in multiples of 16, between 655,360 and 3,686,400 pixels, with edges no larger than 2560 and ratio between 1:3 and 3:1. It prioritizes the smallest ratio error, then proximity to one megapixel. Exact equivalents are used when possible within this conservative size budget; experimental 4K sizes are deliberately excluded. Ratios beyond provider bounds are clamped and visibly reported.

Gemini supports the fourteen documented ratios in the central catalog, at 1K. Other numeric ratios map by smallest logarithmic ratio distance. Authored text like “widescreen” remains valid offline prompt content, but generation requires a numeric width:height ratio. The UI shows authored and effective settings before submission, and decoded image dimensions afterward.

## 9. Errors and cancellation

Missing credentials, authentication/permission denial, quota/rate limit, invalid request, safety rejection, network/DNS failure, timeout, malformed image response, server failure, and unknown failure have safe application messages. Raw provider error messages are deliberately not displayed because they can echo sensitive input. OpenAI safety error codes and Gemini prompt/candidate safety results are recognized. Base64, expected image count, MIME type, and image signatures are checked. Cancellation is never wrapped as a provider failure. Retry is always explicit and may incur another charge.

## 10. Image memory and storage

One successful image byte array and its metadata are retained in memory. The UI decodes near the platform boundary on a background dispatcher and preserves aspect ratio. Decoded pixels are released when that preview leaves composition; replacing the successful image releases the prior session result. Base64 and raw HTTP response bodies are not kept in UI state, project JSON, or a database. Saving uses original bytes and detected MIME type, without recompression.

Desktop uses a native FileDialog and `CREATE_NEW`, refusing existing paths even after a dialog overwrite choice. Android uses ACTION_CREATE_DOCUMENT with the response MIME type and a captured image reference while the picker is open. Filenames are sanitized. Android system document creation avoids silently replacing same-name documents. Current images and provider selection are not persisted across restart; there is no history.

## 11–15. Verification

All tests are credential-free. New common tests use Ktor MockEngine and fake providers for exact request/auth construction, every catalog model, decoding, invalid Base64/signatures, provider errors, safety, rate limits, authentication, DNS/network/timeout failures, cancellation, no retries, snapshot isolation, duplicate protection, regeneration, current generation, compiler identity, and credential exclusion/removal. Desktop Compose tests exercise masked key entry/storage, missing credentials, loading/cancel, error/retry, image display, metadata controls, and Save Image availability. A Desktop file test decodes a real PNG, verifies byte-identical output, and verifies overwrite refusal.

Commands executed:

| Command | Result |
| --- | --- |
| `./gradlew :shared:jvmTest :androidApp:assembleDebug :desktopApp:assemble --console=plain` | Passed; initial integrated V1 regression and target compilation check |
| `./gradlew :shared:jvmTest :shared:testAndroidHostTest --console=plain` | Passed; shared provider/state/compiler/persistence tests on both targets |
| `./gradlew :shared:jvmTest --console=plain` | Passed including new Desktop Compose and image-file tests |
| `./gradlew build --console=plain` | Passed; full build, tests and Android lint |
| `./gradlew :desktopApp:createDistributable --console=plain` | Passed; macOS application packaged and opened |
| `./gradlew :desktopApp:run --console=plain` | Desktop app launched; process stopped after smoke testing |
| `./gradlew build :desktopApp:createDistributable --console=plain` | Final source: BUILD SUCCESSFUL, 162 tasks (34 executed, 128 up-to-date), 8 seconds |
| `git diff --check` | Passed |

Final XML test totals: **67 Desktop/JVM tests and 64 Android host tests**, zero failures, errors, or skips. Shared common tests execute on both targets; these are not 131 distinct tests. V2 adds 13 common tests and 2 Desktop tests. Android debug/release compilation and Desktop compilation/package creation are included in the final full build. Project schema remains version 1; application versions are Android 2.0 (code 2) and Desktop 2.0.0.

**Live OpenAI generation: not performed. Live Gemini generation: not performed.** No provider keys were used. Mock fixtures prove request/response handling and UI behavior, not account/model access, real latency, billing, current provider availability, safety decisions, or generated visual quality.

The packaged Desktop application was opened and inspected manually: existing library loading, character switching, prompt preview, provider selection, effective output, missing-key errors, and navigation back to the editor worked. Desktop text-input automation could not reliably operate the native Java field; key-entry and paid-request states were therefore verified through Compose UI tests. The original active character was restored. The native image save picker and Android Keystore/document-provider behavior have not been exercised on a physical device. Android compilation and host tests pass; this is not a claim of Android device testing.

## 16. Limitations and provider-neutral compromises

- Gemini uses ratio plus a resolution tier; OpenAI uses explicit pixel dimensions. `EffectiveOutput.size` honestly carries the chosen provider setting rather than pretending both expose identical dimensions.
- OpenAI uses fixed PNG/medium quality. Gemini controls output encoding and returns its actual MIME type; no fabricated common quality, seed, sampler, or CFG setting is exposed.
- Gemini's model can return text, thought content, no image, or multiple image parts. V2 filters thought parts and accepts exactly one final image; other outputs become a useful failure, not a gallery.
- Neither protocol supplies a reproducibility seed for this workflow. Snapshot regeneration preserves input, not pixel identity.
- Local cancellation cannot guarantee provider-side cancellation or prevent charges.
- Desktop credentials, latest images, and provider preferences are session-only. Android secure persistence remains an optional OS-backed path requiring device validation.
- A malformed encoded image that passes its signature check can still fail platform decoding; the UI reports preview unavailability and allows saving original bytes.
- API aliases can change behavior. The central model catalog and linked protocol references are the update points.

## 17. Ten files to study, in order

Paths below are relative to the repository root.

1. `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/generation/GenerationController.kt` — snapshot, concurrency, state, regenerate.
2. `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/generation/model/GenerationModels.kt` — catalog, capabilities, metadata, errors.
3. `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/generation/provider/ImageGenerationProvider.kt` — provider contract.
4. `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/generation/provider/HttpImageProviders.kt` — both protocols, decoding, error mapping.
5. `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/feature/generation/GenerationPanel.kt` — shared UI and exact prompt inspection.
6. `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/credentials/CredentialStore.kt` — secret boundary and session storage.
7. `shared/src/androidMain/kotlin/com/kelvinsaputra/promptstudio/credentials/CredentialStore.android.kt` — Keystore-backed persistence.
8. `shared/src/commonMain/kotlin/com/kelvinsaputra/promptstudio/App.kt` — app-scoped ownership and integration.
9. `shared/src/commonTest/kotlin/com/kelvinsaputra/promptstudio/GenerationProviderTest.kt` — wire-contract regression tests.
10. `shared/src/commonTest/kotlin/com/kelvinsaputra/promptstudio/GenerationStateTest.kt` — lifecycle and compiler integrity tests.

Platform image-save implementations and `GenerationDesktopTest.kt` are useful next reads. `PromptCompiler.kt`, character models, library behavior, and ordinary project persistence were preserved.
