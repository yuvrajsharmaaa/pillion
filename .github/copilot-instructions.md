# Project Overview

This repository is for **Pillion**, an Android-first rider companion app.

Pillion lets a user pre-plan a trip as a sequence of *stages* (e.g., “Delhi → Dehradun ride”, “On arrival show hotels”, “Next morning show cafes”, “Ride to Mussoorie”). During the trip, the app automatically shifts what it shows based on triggers such as arrival at a destination, time of day, or manual “next stage” input. For each stage, it can search nearby places (hotels, fuel, food, etc.) using Google Maps / Places APIs and provide one-tap navigation via Google Maps.

The goal is: **a production-quality, testable Android app (Kotlin + Jetpack Compose) plus a small backend that uses Google Maps Platform for routes and places.**

Non-goals for the MVP:
- No full in-app turn-by-turn navigation—we open Google Maps for final navigation.
- No social feed, chat, or advanced ML/RL yet (we just log data to make that possible later).
- No complex booking flow—outbound deep-links only if needed.

---

## Tech Stack

**Mobile (primary):**
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: Clean Architecture + MVVM
- Async: Kotlin Coroutines + Flow
- DI: Hilt
- Networking: Retrofit + OkHttp
- JSON: Moshi or Kotlinx Serialization
- Local storage: Room
- Location: Android Fused Location Provider API
- Build: Gradle (KTS)

**Backend (secondary):**
- Language: TypeScript
- Framework: Node.js + Express (or Fastify)
- DB: PostgreSQL (via Prisma or TypeORM)
- Auth: simple token-based auth or Firebase Auth (MVP)
- External APIs: Google Maps Platform (Routes/Directions) and Places API (New)

Prefer idiomatic Kotlin and standard Android/Jetpack patterns.

---

## Core Domain Concepts

Use these names consistently in code, types, and file names:

- `Trip`
  - A user-owned entity representing a full journey (e.g., Delhi → Dehradun → Mussoorie weekend trip).
  - Fields: `id`, `userId`, `name`, `startLocation`, `endLocation`, `stages: List<TripStage>`, `createdAt`, `updatedAt`.

- `TripStage`
  - One ordered stage in a trip.
  - Fields (MVP): `id`, `tripId`, `orderIndex`, `type`, `fromLocation`, `toLocation`, `trigger`, `queryConfig`, `status`.
  - `type`: enum, e.g., `RIDE`, `ARRIVAL_PLACES`, `TIME_PLACES`, `CUSTOM`.
  - `trigger`: enum, e.g., `ON_ARRIVAL`, `AT_TIME`, `MANUAL`.
  - `queryConfig`: structured fields like `placeType` (`lodging`, `restaurant`, `gas_station`), `minRating`, `maxPriceLevel`, `openNow`.

- `PlaceSuggestion`
  - A mapped object from Google Places API for UI use.
  - Fields: `id`/`placeId`, `name`, `rating`, `userRatingsTotal`, `priceLevel`, `distanceMeters`, `lat`, `lng`, `address`.

- `TripState`
  - The runtime state machine representation on the client, tracking which stage is active and what trigger conditions are being watched (distance to destination, time, etc.).

---

## High-Level Architecture

**Mobile layers (Clean Architecture):**

- `domain/`
  - Pure Kotlin models and use cases.
  - No Android framework dependencies.
  - Examples:
    - `CreateTripUseCase`
    - `UpdateTripUseCase`
    - `GetTripsUseCase`
    - `StartTripUseCase`
    - `ObserveTripStateUseCase`
    - `SelectPlaceForStageUseCase`

- `data/`
  - Repository implementations, networking, and local storage.
  - Interfaces:
    - `TripRepository`
    - `TripStageRepository`
    - `PlaceRepository`
    - `LocationRepository`
  - Implementations use Retrofit, Room, etc.

- `ui/`
  - Jetpack Compose screens, view models, and UI state classes.
  - No direct network calls; use view models + use cases.

**Backend layers:**

- `src/domain/`
  - Entities and service interfaces (e.g., `TripService`, `PlaceService`).

- `src/infrastructure/`
  - DB models (Prisma/TypeORM).
  - Google Maps/Places clients (thin wrappers).
  - HTTP server setup.

- `src/api/`
  - Express route handlers.
  - DTOs and input validation.

---

## Feature Priorities (MVP)

Please help implement features in this order:

1. **Trip CRUD**
   - Create, read, update, delete trips.
   - Add/edit/delete stages within a trip.
   - Persist to local DB and sync with backend.

2. **Trip Builder UI**
   - Screen to create and edit a trip with a vertically ordered list of `TripStage` cards.
   - For each stage:
     - Choose type (`RIDE`, `ARRIVAL_PLACES`, `TIME_PLACES`).
     - Select origin/destination via search/autocomplete.
     - Configure trigger (`ON_ARRIVAL`, `AT_TIME`, `MANUAL`).
     - Configure `queryConfig` (place type, rating, price, open now).
   - Validation: ensure at least one stage and consistent ordering.

3. **Live Trip State Machine**
   - `TripStateMachine` class in the domain layer that:
     - Tracks current trip and active stage.
     - Consumes location/time events.
     - Emits `TripState` updates (e.g., active stage, next stage).
   - Pure-domain logic (no Android imports).

4. **Location & Triggers**
   - On Android, wire Fused Location Provider to feed the state machine.
   - Implement trigger logic:
     - `ON_ARRIVAL`: when within threshold distance (e.g., 1–3 km) and speed below threshold.
     - `AT_TIME`: fire based on device time.
     - `MANUAL`: always allow a user to advance.

5. **Places Search & Ranking**
   - Backend route (e.g., `GET /trips/:id/stages/:stageId/places`) that:
     - Calls Google Places API (New) given a lat/lng and `queryConfig`.
     - Filters on basic criteria.
     - Computes a simple score (rating, review count, distance).
   - Android `PlaceRepository` calls this endpoint.
   - Compose UI to display a list of `PlaceSuggestion` cards and a button to “Navigate in Google Maps” using the place’s coordinates or `placeId`.

6. **Navigation Handoff**
   - From the places list, open the Google Maps app via Intent with the selected destination.
   - For pure `RIDE` stages, allow “Open route in Google Maps” with from/to coordinates.

7. **Basic Telemetry / Logging Hooks**
   - Add domain-level events and stubs for:
     - `TripStarted`
     - `StageChanged`
     - `PlaceSelected`
   - Implement as interfaces that can later be plugged into analytics/ML pipelines.

---

## Coding Guidelines

- Prefer clear, explicit code over clever one-liners.
- Use MVVM + unidirectional data flow in UI.
- ViewModels:
  - Expose immutable `StateFlow` or `UiState` data classes.
  - Avoid business logic in Composables.
- Error handling:
  - Wrap network calls in result types (e.g., `Result<T>` or sealed classes).
  - Show user-friendly error states in UI.
- Testing:
  - For domain layer: aim for unit tests on use cases and the `TripStateMachine`.
  - Prefer writing testable pure functions for trigger logic.
- Naming:
  - Follow standard Kotlin and Android naming conventions.
  - Names should reflect domain terms above (`Trip`, `TripStage`, `TripState`, `PlaceSuggestion`).

---

## File and Package Structure (Mobile)

Suggested structure (you can refine it but stay consistent):

- `app/src/main/java/com/pillion/`
  - `domain/`
    - `model/Trip.kt`, `TripStage.kt`, `TripState.kt`, `PlaceSuggestion.kt`
    - `usecase/CreateTripUseCase.kt`, etc.
    - `statemachine/TripStateMachine.kt`
  - `data/`
    - `repository/TripRepository.kt`, `PlaceRepository.kt`, etc.
    - `remote/` (Retrofit services & DTOs)
    - `local/` (Room DAOs & entities)
  - `ui/`
    - `triplist/TripListScreen.kt`, `TripListViewModel.kt`
    - `tripbuilder/TripBuilderScreen.kt`, `TripBuilderViewModel.kt`
    - `livetrip/LiveTripScreen.kt`, `LiveTripViewModel.kt`
  - `di/`
    - Hilt modules for repositories, use cases, and Retrofit clients.

Use similar structure on backend: `domain`, `infrastructure`, `api`.

---

## How to Work With This Repo (for Copilot)

When generating code:

1. **Align with domain terms & types.**
   - Always use the `Trip`, `TripStage`, `TripState`, and `PlaceSuggestion` models as the main entities.

2. **Respect the architecture.**
   - Do not put networking logic in UI code.
   - Route all external calls through repositories and use cases.

3. **Implement features incrementally.**
   - For a new feature, first create or update:
     - Domain models and use cases.
     - Repositories and DTOs.
     - ViewModels and UI composables.

4. **Generate tests where possible.**
   - When modifying domain logic, also propose Kotlin tests that verify the behavior of the trip state machine and triggers.

5. **Ask for clarification examples.**
   - If you’re unsure, write code comments like:
     - `// TODO: clarify how trigger threshold should be configured`
   - Or suggest both options and mark them clearly in comments.

---

## Example Tasks for Copilot

Use prompts like these in Copilot Chat:

- “Implement `TripStateMachine` based on the `Trip`, `TripStage`, and trigger rules described in copilot-instructions.md. It should expose a flow of `TripState` and allow feeding location/time events.”
- “Create the `TripBuilderScreen` and `TripBuilderViewModel` that let me add, edit, reorder, and delete `TripStage` items for a `Trip`.”
- “Implement a backend Express route `GET /trips/:id/stages/:stageId/places` that calls Google Places API (New) and returns a list of `PlaceSuggestion` objects.”

- “Write unit tests for the `TripStateMachine` that verify the correct stage transitions based on location and time triggers.”


# Project Overview

This repo is for **Pillion**, an Android rider companion app.

Pillion lets a user pre-plan a trip as a sequence of **stages** (e.g., “Delhi → Dehradun ride”, “On arrival show hotels”, “Next morning show cafes”, “Ride to Mussoorie”). During the trip, the app automatically switches what it shows based on triggers such as arrival at a destination, time of day, or manual “Next stage” tap.

**Important constraints for this project:**

- Android-only.
- **Kotlin + Jetpack Compose UI.**
- **NO backend, NO Firebase, NO server.**
- All data is stored **locally on the device** using Room.
- For navigation, we **open the Google Maps app via Intents**. We do not call Google Maps HTTP APIs directly in the MVP.

The goal: a **simple, offline-friendly MVP** that works on a single device, enough for a portfolio project and real rides.

---

# Tech Stack & Libraries

- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM + use cases (Clean-ish, but simple)
- Async: Kotlin Coroutines + Flow/StateFlow
- DI: Hilt (optional; if too heavy, use simple constructor injection)
- Local storage: Room
- Location: Android Fused Location Provider API
- Navigation: Jetpack Navigation for Compose
- External apps: open Google Maps via Intents (geo: URI or google.navigation).

Do **NOT** add any backend, Retrofit, Firebase, or cloud code in this repo.

---

# Core Domain Concepts

Use these names consistently in models, functions, and file names:

## Trip

Represents a full journey.

Fields (adjust as needed, but keep the idea):

- `id: Long`
- `name: String` (e.g., "Delhi → Dehradun weekend")
- `createdAt: Long`
- `updatedAt: Long`
- Optionally `startLocation` and `endLocation` (lat/lng or simple strings)

## TripStage

One ordered stage in a trip.

Fields:

- `id: Long`
- `tripId: Long`
- `orderIndex: Int`
- `type: TripStageType`
- `fromLocation: LocationPoint?` (nullable if not needed)
- `toLocation: LocationPoint?`
- `trigger: StageTrigger`
- `scheduledTimeMillis: Long?` (for time-based triggers)
- `queryConfig: StageQueryConfig?`
- `status: StageStatus`

Enums:

- `TripStageType` = `RIDE`, `ARRIVAL_PLACES`, `TIME_PLACES`, `CUSTOM`
- `StageTrigger` = `ON_ARRIVAL`, `AT_TIME`, `MANUAL`
- `StageStatus` = `PENDING`, `ACTIVE`, `COMPLETED`, `SKIPPED`

## LocationPoint

Simple data class for coordinates and display text:

- `lat: Double`
- `lng: Double`
- `label: String` (human readable, e.g., "Dehradun" or "Some Cafe")

## StageQueryConfig

Configuration for what to show when we reach this stage:

- `placeType: PlaceType` (e.g., HOTEL, FOOD, FUEL, CAFE, MECHANIC, CUSTOM_QUERY)
- `minRating: Float?`
- `maxPriceLevel: Int?`
- `openNow: Boolean`
- `customQuery: String?` (e.g., "budget hotel", "veg restaurant")

## PlaceSuggestion (local only)

Represents a place the user has selected/saved.

Fields:

- `id: Long`
- `stageId: Long`
- `name: String`
- `lat: Double`
- `lng: Double`
- `address: String?`
- `notes: String?`

Initially, we won’t call Google Places HTTP API. Instead:
- We let the user search/open places using the Google Maps app UI.
- If they select a place, we allow them to save it manually into `PlaceSuggestion`.

---

# Architecture (No Backend)

## Packages (Android)

Suggested structure:

- `domain/`
  - `model/` (`Trip`, `TripStage`, `TripState`, `LocationPoint`, `StageQueryConfig`, etc.)
  - `usecase/` (simple classes with one `invoke()` function per use case)
  - `statemachine/TripStateMachine.kt`

- `data/`
  - `local/` (Room entities, DAOs, database)
  - `repository/` (implementations of repositories that talk to Room)
    - `TripRepository`
    - `TripStageRepository`
    - `PlaceSuggestionRepository`

- `ui/`
  - `triplist/` (home screen, list of trips)
  - `tripbuilder/` (create/edit trip and stages)
  - `livetrip/` (live trip screen, triggers, current stage display)
  - `common/` (shared components)

- `location/`
  - helpers for FusedLocationProvider and permission handling

- `navigation/`
  - NavHost and routes

## Repositories

Define repository interfaces in `domain` (or `data/repository/api`) and implement them using Room in `data/repository/local`.

Examples:

- `TripRepository`
  - `suspend fun createTrip(trip: Trip): Long`
  - `fun getTripsFlow(): Flow<List<Trip>>`
  - `suspend fun getTripById(id: Long): Trip?`
  - `suspend fun updateTrip(trip: Trip)`
  - `suspend fun deleteTrip(id: Long)`

- `TripStageRepository`
  - `fun getStagesForTripFlow(tripId: Long): Flow<List<TripStage>>`
  - `suspend fun upsertStage(stage: TripStage)`
  - `suspend fun deleteStage(id: Long)`

- `PlaceSuggestionRepository`
  - `fun getPlaceSuggestionsForStageFlow(stageId: Long): Flow<List<PlaceSuggestion>>`
  - `suspend fun upsertSuggestion(place: PlaceSuggestion)`
  - `suspend fun deleteSuggestion(id: Long)`

---

# Trip State Machine (On Device)

Create a pure Kotlin `TripStateMachine` in `domain/statemachine`:

Responsibilities:
- Holds current `Trip` and list of `TripStage`s.
- Knows which stage is active.
- Consumes events:
  - Location updates (lat, lng, speed)
  - Time ticks (e.g., every minute)
  - Manual events (user tapped "Next stage")

- Emits `TripState`:
  - `activeStage: TripStage?`
  - `nextStage: TripStage?`
  - `isTripCompleted: Boolean`
  - maybe helpful UI flags like `shouldShowPlacesForStage(stageId: Long)`

Trigger logic:
- `ON_ARRIVAL`: if distance to `toLocation` < threshold (e.g., 1–3 km) and speed is low, complete current stage and move to next.
- `AT_TIME`: if current time >= `scheduledTimeMillis`, activate that stage.
- `MANUAL`: only advance when user taps "Next stage".

**Important:** This logic must be pure Kotlin (no Android imports) so it is testable.

---

# Google Maps Integration (Intents Only)

We do NOT call Google Maps HTTP APIs in this MVP.

Instead, we:

- Use Android Intents to open Google Maps app:
  - For navigation to a point:
    - `Uri.parse("google.navigation:q=${lat},${lng}")`
  - For searching a generic query near a location:
    - `Uri.parse("geo:${lat},${lng}?q=${URLEncoder.encode(query, "UTF-8")}")`

- Expose helper functions (e.g., `openGoogleMapsNavigation(context, locationPoint)`).

This keeps everything free and simple.

---

# Feature Scope for MVP

Please prioritize building features in this order:

1. **Local data layer with Room**
   - Room database, entities, and DAOs for:
     - `TripEntity`
     - `TripStageEntity`
     - `PlaceSuggestionEntity`
   - Basic repositories and use cases to create/edit/delete trips and stages.

2. **Trip List Screen**
   - Compose screen that shows all trips from Room via Flow.
   - Add FAB to create a new trip.
   - Tapping a trip opens the Trip Builder / Trip Detail screen.

3. **Trip Builder Screen**
   - Show the trip name and a list of stages.
   - Allow:
     - Adding a new stage (choose type, trigger, basic query config).
     - Editing an existing stage.
     - Deleting a stage.
     - Reordering stages (MVP: simple up/down buttons).

4. **Live Trip Screen**
   - Show current active stage and upcoming stages.
   - Button for “Start Trip”.
   - Subscribe to location updates (with runtime permissions).
   - Feed location/time events into `TripStateMachine`.
   - Always show a “Next stage” button for manual override.

5. **Maps Integration**
   - From a stage that needs navigation, show a “Open in Google Maps” button.
   - Implement helpers to:
     - Navigate from current location to stage’s `toLocation`.
     - Search in Google Maps with `StageQueryConfig` (e.g., open geo: URI with query text “budget hotel” or “restaurants”).

6. **Place Suggestions (Manual)**
   - Allow user to save a place they liked for a stage:
     - Simple form: name, lat, lng, address, notes.
   - Store in Room and show in the UI for that stage.

7. **Basic UX Polish**
   - Handle no-trips and no-stages empty states.
   - Simple error handling (e.g., snackbars/toasts).

---

# Coding Style & Patterns

- Use idiomatic Kotlin (data classes, sealed classes, extension functions).
- Use state hoisting in Compose (ViewModels hold state; Composables are dumb UI).
- Expose state from ViewModels as `StateFlow<UiState>` or similar.
- Avoid business logic in Composables—put it in use cases and the state machine.
- Write unit tests for:
  - `TripStateMachine` behavior for different sequences of location/time events.
  - Repositories (if practical).

---

# How Copilot Should Help

When I ask for implementations in this repo, assume all the above and:

1. Prefer simple, readable code over cleverness.
2. Keep everything **local-only, no backend**.
3. Respect the package structure and domain models.
4. When creating new features:
   - Define/update data models.
   - Update Room entities/DAOs.
   - Implement repositories and use cases.
   - Then wire ViewModels and Compose screens.

Example prompt I will use in Copilot Chat:

> “Implement the Room entities, DAOs, and `TripRepository` for Trip and TripStage as described in copilot-instructions.md. Use Kotlin, Room, and Flow.”
