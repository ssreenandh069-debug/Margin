# Margin 📊

**Margin** is an ultra-lightweight, high-performance attendance tracker and academic task manager for Android. Built with 100% Jetpack Compose and modern Kotlin coroutines, Margin calculates precise attendance margins, safe bunks, and recovery targets with zero lag and an offline-first architecture.

---

## ⚡ Performance & Footprint

- **App Binary Size**: ~16.4 MB (Debug APK uncompressed) / ~6–8 MB (Release with R8 minification).
- **Runtime Data Storage**: < 150 KB for a full semester of attendance and task records.
- **Memory Footprint**: Highly efficient StateFlow architecture with minimal recompositions and zero background battery drain.
- **Codebase Simplicity**: Clean, modular structure consolidated into just 23 purposeful Kotlin source files.

---

## ✨ Key Features

- **Exact Attendance Mathematics**:
  - Deterministic integer calculations in `AttendanceMath.kt`:
    $$\text{Safe Bunks} = \max\left(0, \left\lfloor\frac{4A - 3N}{3}\right\rfloor\right)$$
    $$\text{Recovery Classes Needed} = \max(0, 3N - 4A)$$
  - Eliminates floating-point rounding errors when tracking the 75% threshold.
- **Full 7-Day Timetable Support (Monday – Sunday)**:
  - Complete support for weekend classes (including Saturday and Sunday schedules).
  - Slot-level attendance tracking that prevents multi-period daily classes from overwriting each other.
- **Squad Sync (1-Tap Timetable Sharing & Join)**:
  - Shareable Base64 Squad Codes (`MARGIN:...`) that can be sent over WhatsApp, SMS, or Discord.
  - 1-tap copy and paste with zero manual JSON file pickers or permissions needed.
  - Fallback support for standard JSON import/export.
- **Chronological Class Order Guarantee**:
  - Deterministic time parsing via `AttendanceMath.parseTimeToMinutes` converts 12h/24h time strings (e.g. "09:00 AM", "1:30 PM") into minutes from midnight.
  - Ensures exported, imported, and rendered timetable slots always appear in exact chronological sequence.
- **Lenient Tracking (Teacher Swaps & Campus Events)**:
  - **Substitute Teacher Swap**: 1-tap swap allows attributing attendance to the substitute subject when professors swap periods, complete with custom reason notes.
  - **Seminar & Event Logging**: Easily mark days/periods excused for seminars, workshops, or symposiums with custom event notes without penalizing safe bunk margins.
- **1-Tap Attendance Steppers (`+` & `-` Buttons)**:
  - Directly accessible from the **Remind** tab ("All Subjects" view) and Semester Overview sheet.
  - 1-tap `+ Present`, `- Bunk`, and `Undo` buttons for effortless daily adjustments.
  - Live "Wall of Shame" (most bunked subject) and "Heavy Load" (most deliverables) indicators.
- **Attendance Time Machine**:
  - Date navigator with timezone-safe UTC $\leftrightarrow$ Local midnight conversion.
  - Quick action tracking: `Present`, `Bunk`, `Cancelled / Off`, `Proxy`, and `Duty Attendance`.
  - Margin indicators: Instant feedback on how many classes can be safely skipped or how many consecutive classes are needed to recover.
- **Academic Tasks & Deliverables**:
  - Filterable by status (`All`, `Pending`, `Completed`) and categories (`Assignments`, `Presentations`, `Practicals`, `Study Goals`, `Other`).
  - Search by task title, subject name, or subject code.
  - Exact AlarmManager reminders scheduled before due dates with automatic reboot recovery.
- **100% Offline & Private**:
  - Pure local Room SQLite database (v5) with automated schema migrations. No external tracking, no cloud telemetry, no accounts required.

---

## 🛠️ Architecture & Tech Stack

```
com.Margin.app/
├── MainActivity.kt                  # Edge-to-edge Compose container
├── MarginApplication.kt             # Application init & notification channels
├── navigation/
│   └── AppNavigation.kt             # 3-tab scaffold (Track, Timetable, Remind)
├── domain/
│   └── AttendanceMath.kt            # Pure attendance, time & date arithmetic
├── data/local/
│   ├── AppDatabase.kt               # Room database (v5) & consolidated DAOs
│   ├── AttendanceRepository.kt      # Unified data repository (steppers & swaps)
│   ├── UserPreferencesRepository.kt # DataStore settings
│   └── entity/
│       └── Entities.kt              # Room entities & enums (with note support)
├── ui/
│   ├── components/
│   │   └── Sheets.kt                # Modal sheets: SquadSync, SwapClass, LogSeminar
│   ├── screens/
│   │   ├── TrackScreen.kt           # Primary dashboard with swap & seminar actions
│   │   ├── TimetableScreen.kt       # 7-day schedule & Squad Sync launcher
│   │   └── TaskListScreen.kt        # Remind tab: tasks + subject stepper controls
│   ├── theme/                       # Colors, Typography, & M3 Theme
│   └── viewmodel/
│       ├── AttendanceViewModel.kt   # Central state management
│       └── TaskViewModel.kt         # Task & reminder management
├── reminders/
│   └── TaskReminderManager.kt       # Exact AlarmManager scheduler
├── receivers/
│   ├── BootReceiver.kt              # Post-reboot alarm rescheduling
│   └── TaskAlarmReceiver.kt         # Deadline notifications
├── workers/
│   └── SmartBunkWorker.kt           # Periodic WorkManager margin checks
└── utils/
    ├── TimetableExportManager.kt    # Squad Code & JSON export/import
    └── ViewModelExt.kt              # ViewModel factory helpers
```

- **UI**: 100% Jetpack Compose with Material 3.
- **Database**: Android Room 2.6+ with SQLite foreign keys and cascade deletion.
- **Concurrency**: Kotlin Coroutines & reactive `StateFlow`.
- **Background Processing**: WorkManager (`SmartBunkWorker`).
- **Scheduling**: Android `AlarmManager` with direct boot and reboot re-arming.

---

## 🔒 Privacy, Security & Data Ownership

- **Zero Network Permissions**: `android.permission.INTERNET` is neither requested nor used in `AndroidManifest.xml`. The app is incapable of making network requests or transmitting telemetry.
- **Zero Third-Party SDKs**: No Google Analytics, no Firebase, no crash loggers, and no advertising frameworks.
- **100% Offline-First**: All timetable, attendance, and task data is stored in your private local SQLite database within your device's sandboxed internal storage.
- **No Account Required**: No logins, email addresses, phone numbers, or passwords. Your data belongs exclusively to you.

---

## 🏗️ Getting Started

1. Clone the repository.
2. Open in **Android Studio (Ladybug / Hedgehog or newer)**.
3. Ensure JDK 17 or JDK 21 is configured (e.g. Android Studio JBR).
4. Run `./gradlew assembleDebug` to build the APK.
5. Deploy to any physical Android device or emulator running **Android 8.0 (API 26) or higher**.
