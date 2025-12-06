# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device
./gradlew installDebug

# Run all unit tests
./gradlew test

# Run specific test class
./gradlew testDebugUnitTest --tests="com.uhstudio.pillreminder.ClassName"

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

# Clean build
./gradlew clean
```

## Architecture

### MVVM with Jetpack Compose
- Each screen has a dedicated ViewModel in the same package (e.g., `HomeScreen.kt` + `HomeViewModel.kt`)
- ViewModels extend `AndroidViewModel` for context access to database
- UI state flows from ViewModel to Composable via StateFlow/LiveData

### Database (Room)
- **Singleton access:** `PillReminderDatabase.getDatabase(context)`
- **Entities:** `Pill`, `PillAlarm`, `IntakeHistory` in `data/model/`
- **DAOs:** Corresponding DAOs in `data/dao/`
- **Type converters:** `LocalDateTime` and `Set<DayOfWeek>` converters in `data/converter/`
- All database operations use suspend functions or `Flow<T>`

### Navigation
- Bottom navigation with 3 tabs: Home, Alarms, Calendar
- Routes defined in `MainActivity.kt` NavHost
- Deep linking via route parameters (e.g., `pill_detail/{pillId}`)

### Alarm System
- `AlarmManagerUtil.kt` schedules exact alarms via AlarmManager
- `AlarmReceiver.kt` (BroadcastReceiver) handles alarm triggers and shows notifications
- Notification actions: "Take Pill" and "Skip Pill" update `IntakeHistory`

## Tech Stack
- Kotlin 1.9.22, Compose BOM 2024.02.00, Material3
- Room 2.6.1, Navigation Compose 2.7.7, Coil 2.5.0
- Min SDK 26, Target SDK 34, Java 17

## Key Patterns

### Coroutines
- Use `viewModelScope` for ViewModel operations
- Use `Dispatchers.IO` for database/network operations
- Room returns `Flow<T>` for reactive queries

### Resources
- String resources in Korean (`res/values/strings.xml`)
- Color palette follows Material3 theming in `ui/theme/`

## Important Files
- **Entry point:** `MainActivity.kt` - navigation setup and bottom bar
- **Database:** `data/database/PillReminderDatabase.kt` - Room database singleton
- **Alarms:** `receiver/AlarmReceiver.kt` - notification handling
- **Utilities:** `util/AlarmManagerUtil.kt` - alarm scheduling logic
