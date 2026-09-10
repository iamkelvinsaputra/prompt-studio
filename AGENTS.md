# Prompt Studio

Personal toy/learning project for Kotlin Multiplatform and Compose Multiplatform.

## Current targets

- Android
- macOS/Desktop
- iPhone/iPad (Apple Silicon simulator and device)
- Web (Kotlin/Wasm)

## Engineering principles

- Prefer commonMain whenever reasonable.
- Keep Gradle module count low.
- Prefer standard Kotlin/Compose libraries.
- No unnecessary frameworks.
- No dependency injection framework.
- No database.
- No backend.
- Native BYOK OpenAI/Gemini generation; no direct provider generation or API keys on Web.
- No AI inside the prompt compiler.
- Prompt compilation must be deterministic.
- Typed domain models are preferred over Map<String, Any>.
- Keep the project compiling after meaningful changes.
- Add tests primarily around domain logic and PromptCompiler.

## Product source of truth

See:

docs/character_prompt_cheatsheet.md

Use it as the source of truth for prompt vocabulary, component definitions,
prompt wording, slot limits, costume options, and pose options.

## Current scope

V2.1: complete character authoring, native BYOK generation, iOS/iPadOS and Web targets, and persistent local generation history with exact configuration snapshots.

Character JSON excludes credentials and generation artifacts. History has its own versioned metadata and separate images. Preserve compiler determinism and existing project compatibility.

See docs/v2.1-cross-platform-history.md for platform boundaries and validation.

Do not implement future features (including the V2.2 redesign or backend) unless explicitly requested.
