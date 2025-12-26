# Repository Guidelines

## Project Structure & Module Organization
- `app/` is the main Android app module (Compose UI, receivers, services, data/domain layers).
- `smtp/` is a library module for email sending and HTML templating.
- Source: `app/src/main/java/pe/brice/smsreplay/` and `smtp/src/main/java/pe/brice/smtp/`.
- Tests: unit tests in `app/src/test/`, instrumented tests in `app/src/androidTest/`.
- Room schemas live in `app/schemas/` and are updated on schema changes.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew assembleRelease` builds a release APK (R8 disabled by default).
- `./gradlew test` runs JVM unit tests.
- `./gradlew connectedAndroidTest` runs instrumented tests on a device/emulator.
- `./gradlew clean` wipes build outputs before a fresh build.

## Coding Style & Naming Conventions
- Kotlin with Jetpack Compose and Koin; follow standard Kotlin style (4-space indentation).
- Packages follow Clean Architecture: `data/`, `domain/`, `presentation/`.
- Use `*Repository` interfaces in `domain/repository` and `*RepositoryImpl` in `data/repository`.
- Use `*UseCase` for domain actions and `*ViewModel` for UI logic.

## Testing Guidelines
- Unit tests use JUnit in `app/src/test/` (e.g., `ExampleUnitTest.kt`).
- Instrumented tests use AndroidX JUnit/Espresso in `app/src/androidTest/`.
- Name test classes after the unit under test (e.g., `SmsQueueManagerTest`).

## Commit & Pull Request Guidelines
- Commits follow Conventional Commits (e.g., `feat:`, `fix:`) based on recent history.
- PRs should describe the change, include test commands run, and note any UI changes.
- If behavior changes, add or update tests and mention affected modules (`app`, `smtp`).

## Security & Configuration Notes
- SMTP credentials are stored via EncryptedSharedPreferences; never log passwords.
- Permissions and foreground service behavior are defined in `app/src/main/AndroidManifest.xml`.
- Keep `gradle/libs.versions.toml` updated when adding dependencies.
