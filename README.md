# Water Tracker

A simple, ad-free Android app for tracking how much water you drink each day.

It has no ads, no account and no internet access. Your data stays on your phone and is backed up by Android's automatic Google Drive backup (assuming enabled).

## Features

- **Ounces or milliliters:** pick a unit in Settings. It defaults to oz in the US and ml elsewhere.
- One-tap logging (8, 12, 16 or 20 oz, or 250, 350, 500 or 750 ml), plus a custom amount
- Daily goal (64 oz or 2000 ml by default, change it in Settings) with a progress ring
- Undo from the snackbar, or remove any entry from today's list
- **Goal celebration:** when a drink takes you over your goal, the ring bounces, droplets splash out and the phone gives a short buzz
- Today's total resets automatically at local midnight, and daylight-saving days are handled correctly
- **History tab:** a 7-day or 30-day bar chart with a goal line, your daily average, how many days you met your goal, and a total for each day
- **Home-screen widget:** a 4×1 bar with today's progress and two quick-add buttons, with amounts you set in Settings (8 and 16 oz to start)
- **CSV export and import:** save all your drinks to a file, or merge them back in (for example on a new phone). Drinks already in the app are skipped.
- **Pace reminders:** a notification when you fall behind (see [How reminders work](#how-reminders-work))
- Light and dark themes

### Roadmap

- [x] History: last 7 and 30 days
- [x] Pace reminders during waking hours
- [x] Goal-reached celebration
- [ ] Log a drink at a different time, for drinks you forgot to log when you had them
- [x] Home-screen widget with today's progress and quick-add buttons
- [ ] Custom quick-add amounts, to match your own glasses and bottles
- [x] Ounces or milliliters
- [x] CSV export and import
- [ ] Write drinks to [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) and see whether Garmin Connect picks them up (on hold)

## How reminders work

Turn on **Pace reminders** in Settings (the gear icon on Today) and set your wake-up time and bedtime.

- **Pace:** your target climbs in a straight line from 0 oz at wake-up to your full goal **an hour before bedtime**. With 7 AM–10 PM and an 80 oz goal, you should be at 40 oz by 2 PM.
- **Checks** happen every 2 hours after wake-up, and none at or after bedtime. For 7 AM–10 PM that's 9, 11, 1, 3, 5, 7 and 9.
- **You get a reminder** only if you're behind pace by **at least 10% of your goal** (8 oz for an 80 oz goal). A new reminder replaces an unread one.
- **Reminders stop for the day** once you reach your goal.

Behind the scenes there's one `AlarmManager` alarm at a time, set for the next check. When it fires, `ReminderReceiver` checks your pace, notifies you if needed, and sets the next alarm. The alarm is also re-set after a reboot, an app update, a clock or time-zone change, and every time the app starts.

**Limitations**

- Bedtime must be before midnight and at least 3 hours after wake-up.
- Alarms are inexact (a 10-minute window) and don't need the "Alarms & reminders" permission. If the phone is in deep Doze (idle, screen off, not moving), Android can hold a reminder until its next maintenance window.

## The widget

Long-press your home screen, tap **Widgets**, and find **Water Tracker**. Change the button amounts in **Settings → Widget**.

The widget updates as soon as you log a drink from the widget or the app. It resets for a new day at the next system refresh, which happens every 30 minutes, or sooner if you open the app.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose, Material 3 |
| Storage | Room (drink log), DataStore (settings) |
| Widget | Jetpack Glance |
| Async | Coroutines and Flow |
| Build | Gradle 9.7 (Kotlin DSL, version catalog), Android Gradle Plugin 9.4 |
| Min / target SDK | 26 (Android 8.0) / 37 |

## Architecture

The app has a single screen and a small, layered structure. It doesn't use a DI framework; `WaterTrackerApp` creates the shared objects lazily.

```
app/src/main/java/dev/ericferguson/watertracker/
├── MainActivity.kt          Hosts the Compose UI
├── WaterTrackerApp.kt       Application class: repositories, notification channel, reschedule on start
├── data/
│   ├── Drink.kt             Room entity: id, amountMl, timestampMillis
│   ├── VolumeUnit.kt        oz/ml: conversion, formatting, quick-add amounts and input ranges
│   ├── DrinkDao.kt          Queries; observeBetween() returns a Flow that re-emits on every change
│   ├── WaterDatabase.kt     Room database, including the v1→v2 oz-to-ml migration
│   ├── DrinkRepository.kt   Converts "a date" into a millisecond range and calls the DAO
│   ├── DayRange.kt          Start and end of a local calendar day (handles DST)
│   ├── DailyTotal.kt        Groups drinks into per-day totals, filling in 0 for empty days
│   ├── DrinkCsv.kt          CSV format for export/import, plus duplicate detection
│   ├── DrinkBackup.kt       Reads and writes CSV files chosen with the system file picker
│   └── SettingsRepository.kt  Unit, goal, reminder and widget settings, stored in DataStore
├── reminders/
│   ├── Pace.kt              Target-by-now math and the 10%-behind rule
│   ├── ReminderSchedule.kt  Check times and the next check after a given time
│   ├── ReminderManager.kt   Sets and cancels the alarm; posts the notification
│   └── ReminderReceiver.kt  Handles the alarm, boot, app update and time changes
├── widget/
│   ├── WaterWidget.kt       Glance widget: total, progress bar, two buttons; tap the text to open the app
│   ├── AddDrinkAction.kt    Logs a drink when a widget button is tapped
│   └── WidgetUpdater.kt     Refreshes the widget when drinks or settings change
└── ui/
    ├── AppRoot.kt           Bottom navigation between the Today and History tabs
    ├── TodayViewModel.kt    Combines today's drinks with the goal into a single StateFlow<TodayUiState>
    ├── TodayScreen.kt       Stateful TodayScreen that wraps the stateless TodayContent (which has a preview)
    ├── HistoryViewModel.kt  Daily totals for the selected period, plus the average and goal-met stats
    ├── HistoryScreen.kt     Period toggle, stat cards, Canvas bar chart and day list
    ├── SettingsViewModel.kt Saves settings, then reschedules the alarm
    ├── SettingsScreen.kt    Units, goal, reminders, wake/bed times, widget buttons, export/import
    ├── AmountDialog.kt      Number-of-ounces dialog shared by Today and Settings
    ├── GoalCelebration.kt   Ring bounce and droplet splash when you reach your goal
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

## Installing on your phone

Debug builds install as **Water Tracker (debug)** with the app id `dev.ericferguson.watertracker.debug`, so a debug build can sit next to the release build you use every day. Each has its own data.

### 1. Create a signing key (one time)

```bash
mkdir -p ~/.android-keys
keytool -genkeypair -v -keystore ~/.android-keys/water-tracker.jks -alias water-tracker -keyalg RSA -keysize 4096 -validity 36500
```

`keytool` asks for a password and a name (the other questions can be left blank). **Back up the `.jks` file and its password**, for example in a password manager. Every future update must be signed with this key. If you lose it, you'll have to uninstall the app to install a new version.

### 2. Tell Gradle about the key

Create `keystore.properties` in the project root. It's in `.gitignore`, so it's never committed:

```properties
storeFile=/Users/YOUR_USER/.android-keys/water-tracker.jks
storePassword=YOUR_PASSWORD
keyAlias=water-tracker
keyPassword=YOUR_PASSWORD
```

(`keytool` uses the store password for the key too, so both passwords are the same.)

### 3. Build the release APK

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.

### 4. Install over Wi-Fi (Android 11+)

1. On the phone, turn on Developer options: **Settings → About phone**, then tap **Build number** 7 times.
2. Connect the phone and the Mac to the same Wi-Fi network.
3. Go to **Settings → System → Developer options → Wireless debugging**, turn it on, and tap **Pair device with pairing code**.
4. On the Mac, pair using the IP address, port and code shown on the phone. Then connect using the IP and port on the main Wireless debugging screen, which is a *different* port from the pairing one:

   ```bash
   adb pair 192.168.1.23:37011
   adb connect 192.168.1.23:41235
   ```

5. Install:

   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

Android Studio can also do the pairing: **Device Manager → Pair Devices Using Wi-Fi** shows a QR code to scan from the phone's Wireless debugging screen.

**Without developer mode:** copy the APK to the phone (Google Drive, email it to yourself, and so on), open it, and allow that app to "install unknown apps" when asked.

### Testing a release build first

Release builds are shrunk by R8, which can remove code that libraries only reach by reflection. Debug builds aren't shrunk, so they won't show these problems. Keep rules for the cases found so far are in `app/proguard-rules.pro`. To try a shrunk build on an emulator without clashing with other installs:

```bash
./gradlew assembleRelease -PreleaseTest
adb install -r app/build/outputs/apk/release/app-release.apk
```

It installs as `dev.ericferguson.watertracker.releasetest`. Rebuild without `-PreleaseTest` before installing on your phone.

### Updating

Increase `versionCode` (and `versionName`) in `app/build.gradle.kts`, rebuild, and run `adb install -r` again. Your data is kept.

## Units and your data

Amounts are always **stored in whole milliliters**. Ounces are only a display unit, so switching back and forth never loses precision. 8 oz is stored as 237 ml and still shows as 8 oz. Whether you've met your goal is judged in the unit you see: five 16 oz drinks add up to 2365 ml against 2366 ml for an 80 oz goal, but both show as 80 oz, so it counts as met. Switching units rounds your goal to a tidy number (whole ounces, or the nearest 50 ml) and resets the widget buttons to that unit's defaults.

**Export** (Settings → Data) writes one row per drink:

```
timestamp,amount_ml,amount_oz
2026-09-27T08:15:00-05:00,237,8.0
```

The timestamp is your local time with its UTC offset. **Import** reads `timestamp` and `amount_ml`, ignores `amount_oz`, and skips any drink already in the app (same time and amount), so importing a file twice is safe. Files are opened through the system file picker, so the app needs no storage permission.

## Tests

Unit tests are in `app/src/test`:

- `DayRangeTest` checks day boundaries, including the 23-hour and 25-hour days when daylight saving time starts and ends.
- `TodayUiStateTest` checks the totals and progress calculation (including going over the goal and a goal of zero), and when a drink counts as reaching the goal.
- `DailyTotalTest` checks grouping drinks by local day, including empty days and time zones.
- `VolumeUnitTest` checks oz/ml conversion (every whole oz survives the trip to ml and back), rounding when switching units, and the default unit per country.
- `DrinkCsvTest` checks the CSV format, an exact export/import round trip, bad rows, Windows line endings, quotes, and skipping duplicates.
- `HistoryUiStateTest` checks the average (which leaves out today and days with nothing logged) and the goal-met count.
- `PaceTest` checks the pace target, the 10% threshold, and staying quiet outside waking hours or after reaching the goal.
- `ReminderScheduleTest` checks the check times, the next check across midnight, and waking-hours validation.

### Testing reminders

In **debug builds only**, the reminder receiver is exported (see `app/src/debug/AndroidManifest.xml`), so you can run a pace check right away instead of waiting for the alarm:

```bash
adb shell am broadcast -n dev.ericferguson.watertracker.debug/dev.ericferguson.watertracker.reminders.ReminderReceiver -a dev.ericferguson.watertracker.action.CHECK_PACE
```

To see the scheduled alarm:

```bash
adb shell dumpsys alarm | grep -A2 CHECK_PACE
```

### Known limitation

The app stores only your current goal, so history compares every past day against today's goal. If you change your goal, past days are re-scored against the new one.

## Database schema

Room exports its schema to `app/schemas/`. Commit these files: when the `Drink` table changes, bump the database version and add a migration so existing data isn't lost. Version 2 renamed `amountOz` to `amountMl` and converted the values (`WaterDatabase.OzToMl`).
