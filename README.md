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

## Note about Gradle wrapper

This environment did not have a `gradle` command available, so wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper`) were not generated here.

If you want command-line builds from this repo, generate the wrapper once on your machine:

```powershell
gradle wrapper
```

Then run:

```powershell
.\gradlew.bat test
```
