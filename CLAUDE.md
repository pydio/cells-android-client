# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is the Android application module (`com.pydio.android.Client`) for the Pydio Cells client. It is consumed as a Git submodule by `cells-android-app` (the parent repo). Build from the parent repo root — do not run Gradle commands from inside this directory.

## Architecture

**MVVM + Service layer** using Jetpack Compose for UI and Koin for dependency injection.

```conf
CellsApp.kt              ← Application class: initialises SDK logger, ClientData, Koin, WorkManager
MainActivity.kt          ← Single activity; hosts the nav graph; handles OAuth deep-link + share intents
MigrateActivity.kt       ← Legacy migration flow (separate activity)

ui/                      ← All UI code (Compose)
  core/
    AbstractCellsVM.kt   ← Base for all ViewModels — connectionState, errorMessage, layout prefs
    nav/CellsNavigation.kt ← Top-level nav destinations and NavHostController wrappers
  browse/                ← File browser (main feature)
  login/                 ← Auth flow (URL entry → OAuth/P8 credentials → token)
  share/                 ← Share-to-app intent handling
  system/                ← Settings, jobs, logs, housekeeping, splash/landing
  account/, search/,
  migration/, theme/

services/                ← Business logic layer; all are Koin singletons
di/Modules.kt            ← Complete Koin graph (5 modules, see below)
db/                      ← Room databases (4 total)
transfer/                ← S3 upload/download, Glide image loading integration
utils/                   ← Shared utilities
```

## Dependency Injection (Koin)

All DI is in `di/Modules.kt`, exported as `allModules = appModule + dbModule + daoModule + serviceModule + viewModelModule`.

- **`appModule`** — `DataStore<Preferences>` + `PreferencesService`
- **`dbModule`** — `RuntimeDB`, `AuthDB`, `AccountDB` singletons (named `"runtimedb"`, `"authdb"`, `"accountdb"`)
- **`daoModule`** — all DAOs from the three fixed databases
- **`serviceModule`** — named Dispatcher qualifiers (`DiNames.uiDispatcher/ioDispatcher/cpuDispatcher`), named Stores (`DiNames.tokenStore`, `passwordStore`, `serverStore`, `transportStore`), all service singletons, `OfflineSyncWorker` factory
- **`viewModelModule`** — 26 ViewModels via `viewModelOf(::...)`

`DiNames` companion object centralises all Koin qualifier strings.

**`TreeNodeDB` is not in the Koin graph.** It is managed via a `ConcurrentHashMap<accountId, TreeNodeDB>` pool inside `TreeNodeRepository` — one Room database per logged-in account, accessed directly.

In Compose, inject ViewModels with `by viewModel<SomethingVM>()`. In non-Compose code, use `KoinComponent` field injection.

## Navigation

Four route namespaces, each with a sealed `Destinations` object and a `NavGraph` composable:

| Namespace | Prefix | Key destinations |
| --- |--- | --- |
| `CellsDestinations` | (root) | `home`, `accounts`, `search/{queryContext}/{stateID}`, `download/{stateID}` |
| `BrowseDestinations` | `browse/` | `open/{stateID}`, `carousel/{stateID}`, `bookmarks/{stateID}`, `offline-roots/{stateID}`, `transfers/{stateID}` |
| `LoginDestinations` | `login/` | `ask-url`, `skip-verify/{stateID}`, `p8-credentials/...`, `launch-auth-processing/...`, `process-auth-callback/{stateID}` |
| `ShareDestinations` | `share/` | `choose-account`, `open/{stateID}`, `in-progress/{stateID}/{uid}` |
| `SystemDestinations` | varies | `about`, `settings`, `logs`, `jobs`, `share/clear-cache/{stateID}` |

All nav graphs are composed inside `WithNavGraph.kt`. Navigation actions are wrapped in `*NavigationActions` / `*Helper` classes rather than called directly on the controller.

## Room Databases

| Database | Version | Notable detail |
| --- | --- | --- |
| `AccountDB` | 2 | `AutoMigration(1→2)`; entities: `RAccount`, `RSession`, `RWorkspace`; view: `RSessionView` |
| `AuthDB` | 1 | Entities: `RToken`, `RLegacyCredentials`, `ROAuthState` |
| `RuntimeDB` | 2 | Manual `MIGRATION_1_2`; `fallbackToDestructiveMigrationOnDowngrade`; entities: `RJob`, `RLog` |
| `TreeNodeDB` | 5 | Per-account pool (see above); `MIGRATION_4_5` adds `external_id` to transfers; destructive fallback for v1-3 |

Schema JSON files are exported to `cells-client/schemas/`. `CellsConverters.kt` provides TypeConverters and must be kept by ProGuard.

## ViewModels

All ViewModels extend `AbstractCellsVM : ViewModel(), KoinComponent`, which provides:

- `connectionState: StateFlow<ConnectionState>` — combines loading state + `ConnectionService.sessionStateFlow`
- `errorMessage` — relayed from `ErrorService`
- Layout/sort preferences from `PreferencesService`
- `getNode()` with remote fallback, `mustConfirmDL()` for metered-network thresholds, `viewFile()` via FileProvider

## Key Services

| Service | Responsibility |
| --- | --- |
| `NodeService` | CRUD on remote nodes, listings, moves, bookmarks |
| `AccountService` | Account lifecycle (add, remove, switch) |
| `AuthService` | OAuth / P8 token acquisition and refresh |
| `TransferService` | Coordinates uploads/downloads via S3 or P8 |
| `S3TransferService` | AWS S3 SDK integration |
| `OfflineService` | Offline root management, sync scheduling |
| `ConnectionService` | Session state, account switching, connection health |
| `NetworkService` | Network type / availability monitoring |
| `TreeNodeRepository` | Owns the `TreeNodeDB` pool; primary data source for node queries |
| `JobService` | Persists background job records to `RuntimeDB` |
| `WorkerService` | WorkManager setup (Koin factory, `OfflineSyncWorker`) |

## Application Entry Points

- **Normal launch:** `MainActivity` → `LAUNCHER` intent filter
- **OAuth callback:** deep link `cellsauth://callback` → `MainActivity` → `LoginDestinations.ProcessAuthCallback`
- **Share intent:** `ACTION_SEND` / `ACTION_SEND_MULTIPLE` → `MainActivity` → `ShareDestinations` nav graph
- **File sharing out:** `FileProvider` authority `com.pydio.android.Client.fileprovider`; WorkManager's default initializer is removed from manifest (Koin factory is used instead)

## Testing

**Unit tests** (`src/test/`):

- `integration/` — live-server tests requiring `src/test/resources/accounts/*.properties` files (`serverURL`, `username`, `pwd`, `defaultWorkspace`, `skipVerify`, `skipServer`)
- `dummy/` — minimal Koin startup smoke tests

**Instrumented tests** (`src/androidTest/`):

- `CheckModuleTest.kt` — verifies the full Koin graph starts on a real device
- `db/AccountDBTest.kt` — in-memory Room tests for `AccountDB`/`RuntimeDB`
- `legacy/` — v2 account migration tests

Run a specific instrumented test class:

```sh
./gradlew :cells-client:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pydio.android.cells.CheckModuleTest
```

## Utilities Worth Knowing

- **`BackOffTicker`** — exponential backoff with a hardcoded delay table; used in connection polling. Thread-safe via `synchronized`.
- **`NodeUtils.isPreViewable()`** — determines carousel eligibility by mime + extension fallback.
- **`NodeUtils.areNodeContentEquals()`** — fine-grained remote-vs-local change detection (eTag, modtime, metaHash).
- **`NavigationUtils.externallyView()`** — opens a cached file with the system chooser via FileProvider; handles legacy double-quoted mime types.
- **`DateUtils`** — timestamps are **epoch seconds** (`currentTimeMillis() / 1000L`), not milliseconds.
- **`Utils.parseOrder()`** — decodes `"column||direction"` sort preference strings.
