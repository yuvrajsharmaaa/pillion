# Pillion

Pillion is an Android rider companion app built with Kotlin and Jetpack Compose.

This project is local-only for MVP:
- No backend
- No Firebase
- No Retrofit or cloud APIs
- Room for persistence
- Google Maps handoff through Intents

## What is implemented

- Clean-ish layered structure:
  - domain: models, repository contracts, use cases, pure `TripStateMachine`
  - data: Room entities, DAOs, mappers, local repository implementations
  - ui: trip list, trip builder, and live trip screens with view models
  - navigation: Compose NavHost and route helpers
  - location: permission helper and fused location flow helper

- Core features:
  - Trip CRUD (create, edit, delete)
  - Stage add/edit/delete/reorder in Trip Builder
  - Runtime stage advancement in Live Trip (manual, time tick, arrival simulation)
  - Google Maps navigation/search handoff intents
  - Manual place suggestions per active stage (saved locally)
  - Domain telemetry hooks (`TripStarted`, `StageChanged`, `PlaceSelected`)

- Tests:
  - Unit tests for `TripStateMachine`

## Open and run

1. Open this folder in Android Studio.
2. Let Gradle sync and download dependencies.
3. Run the `app` configuration on an emulator or Android device.

## Google Maps API key setup

This project uses secure manifest placeholder injection for the Maps key.

- `AndroidManifest.xml` uses `android:value="${MAPS_API_KEY}"`
- `app/build.gradle.kts` reads `MAPS_API_KEY` from either:
  - Gradle property: `MAPS_API_KEY=...`
  - Environment variable: `MAPS_API_KEY`

Recommended local setup (not committed):

1. Add to `~/.gradle/gradle.properties`:

```properties
MAPS_API_KEY=YOUR_DEBUG_OR_LOCAL_KEY
```

Alternative local setup in shell:

```powershell
$env:MAPS_API_KEY="YOUR_DEBUG_OR_LOCAL_KEY"
```

Recommended CI/release setup:

1. Store release key in CI secret variable `MAPS_API_KEY`.
2. Inject it only at build time for release workflows.
3. Do not hardcode the key in source files.

## Build from terminal

Gradle wrapper files are already present in this repository. Run:

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug
```
