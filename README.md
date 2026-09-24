# Water Tracker

A simple, ad-free Android app for tracking how much water you drink each day.

It has no ads, no account and no internet access. Your data stays on your phone and is backed up by Android's automatic Google Drive backup.

## Features

- One-tap logging of 8, 12, 16 or 20 oz, plus a custom amount
- Daily goal in ounces (64 oz by default) with a progress ring
- Undo from the snackbar, or remove any entry from today's list
- Today's total resets automatically at local midnight, and daylight-saving days are handled correctly
a- **History tab:** a 7-day or 30-day bar chart with a goal line, your daily average, how many days you met your goal, and a total for each day
- Light and dark themes

### Roadmap

- [x] History: last 7 and 30 days
- [ ] Reminder notifications at a set interval, only during waking hours
- [ ] Write drinks to [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) (and see whether Garmin Connect picks them up)
- [ ] CSV export
- [ ] Home-screen widget

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose, Material 3 |
| Storage | Room (drink log), DataStore (settings) |
| Async | Coroutines and Flow |
| Build | Gradle 9.7 (Kotlin DSL, version catalog), Android Gradle Plugin 9.4 |
| Min / target SDK | 26 (Android 8.0) / 37 |

## Architecture

The app has a single screen and a small, layered structure. It doesn't use a DI framework; `WaterTrackerApp` creates the shared objects lazily.

```
app/src/main/java/dev/ericferguson/watertracker/
├── MainActivity.kt          Hosts the Compose UI
├── WaterTrackerApp.kt       Application class that holds the repositories
├── data/
│   ├── Drink.kt             Room entity: id, amountOz, timestampMillis
│   ├── DrinkDao.kt          Queries; observeBetween() returns a Flow that re-emits on every change
│   ├── WaterDatabase.kt     Room database
│   ├── DrinkRepository.kt   Converts "a date" into a millisecond range and calls the DAO
│   ├── DayRange.kt          Start and end of a local calendar day (handles DST)
│   ├── DailyTotal.kt        Groups drinks into per-day totals, filling in 0 for empty days
│   └── SettingsRepository.kt  Daily goal, stored in DataStore
└── ui/
    ├── AppRoot.kt           Bottom navigation between the Today and History tabs
    ├── TodayViewModel.kt    Combines today's drinks with the goal into a single StateFlow<TodayUiState>
    ├── TodayScreen.kt       Stateful TodayScreen that wraps the stateless TodayContent (which has a preview)
    ├── HistoryViewModel.kt  Daily totals for the selected period, plus the average and goal-met stats
    ├── HistoryScreen.kt     Period toggle, stat cards, Canvas bar chart and day list
    └── theme/Theme.kt       Water-blue Material 3 color scheme
```

**Data flow:** Room → `Flow<List<Drink>>` → `TodayViewModel` (combined with the goal) → `StateFlow<TodayUiState>` → Compose. The UI never queries the database directly. It redraws whenever the database changes.

## Getting started

### Requirements

- [Android Studio](https://developer.android.com/studio) (a recent stable version)
- An Android phone with USB debugging enabled, or an emulator

### Run it

1. In Android Studio, choose **File → Open** and select this folder. Wait for Gradle sync to finish.
2. Choose a device from the toolbar:
   - **Emulator:** open **Device Manager**, click **+**, and create a Pixel device with the latest system image.
   - **Phone:** enable **Developer options → USB debugging** and connect it with a cable.
3. Click **Run ▶**.

### From the command line

```bash
./gradlew assembleDebug          # build app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # run unit tests
./gradlew installDebug           # install on a connected device
```

The Gradle wrapper needs a JDK 17 or newer. Android Studio's bundled JDK works:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Tests

Unit tests are in `app/src/test`:

- `DayRangeTest` checks day boundaries, including the 23-hour and 25-hour days when daylight saving time starts and ends.
- `TodayUiStateTest` checks the totals and progress calculation, including going over the goal and a goal of zero.
- `DailyTotalTest` checks grouping drinks by local day, including empty days and time zones.
- `HistoryUiStateTest` checks the average (which leaves out today and days with nothing logged) and the goal-met count.

### Known limitation

The app stores only your current goal, so history compares every past day against today's goal. If you change your goal, past days are re-scored against the new one.

## Database schema

Room exports its schema to `app/schemas/`. Commit these files: when the `Drink` table changes, bump the database version and add a migration so existing data isn't lost.
