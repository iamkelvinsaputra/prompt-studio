# Prompt Studio

Personal toy/learning project for Kotlin Multiplatform and Compose Multiplatform.

## Current targets

- Android
- macOS/Desktop

## Engineering principles

- Prefer commonMain whenever reasonable.
- Keep Gradle module count low.
- Prefer standard Kotlin/Compose libraries.
- No unnecessary frameworks.
- No dependency injection framework for V0.
- No database for V0.
- No backend.
- No image-generation API yet.
- No AI inside the prompt compiler.
- Prompt compilation must be deterministic.
- Typed domain models are preferred over Map<String, Any>.
- Keep the project compiling after meaningful changes.
- Add tests primarily around domain logic and PromptCompiler.

## Product source of truth

See:

docs/character-prompt-cheatsheet.md

Use it as the source of truth for prompt vocabulary, component definitions,
prompt wording, slot limits, costume options, and pose options.

## Current scope

V0 vertical slice:

Style
+ Costume
+ Pose
+ Output
  → Deterministic Prompt Compiler
  → Live Prompt Preview
  → Copy Prompt

Do not implement future features unless explicitly requested.