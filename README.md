# Margin 📊

**Margin** is an ultra-lightweight, high-performance academic attendance manager and student routine optimizer for Android. Built with 100% Jetpack Compose and modern Kotlin coroutines, Margin calculates precise attendance thresholds, safe bunks, and recovery targets with zero lag, zero bloat, and an offline-first architecture.

---

## ⚡ Performance & Resource Footprint

- **App Binary Size**: ~16.4 MB (Debug APK uncompressed) / ~6–8 MB (Release with R8 minification).
- **Zero Background Resource Consumption**: No persistent background services, no wakelocks, and zero background CPU usage.
- **Battery-Friendly Alarms**: Deadline reminders scheduled via Android's native `AlarmManager` with 30-bit collision-free request codes.
- **Daily Roast Analysis**: Runs via Android `WorkManager` for < 100ms once every 24 hours.
- **Memory & Flow Safety**: All reactive database streams use `SharingStarted.WhileSubscribed(5000)`, stopping observation 5 seconds after leaving the UI.
- **Runtime Data Storage**: < 200 KB for an entire semester of timetables, attendance records, notes, and task deliverables.

---

## ✨ Key Features

### 1. Deterministic Attendance Mathematics
- Exact integer threshold calculations in [`AttendanceMath.kt`](file:///c:/Codes/Attendance%20Manager/app/src/main/java/com/Margin/app/domain/AttendanceMath.kt):
  $$\text{Safe Bunks} = \max\left(0, \left\lfloor\frac{4A - 3T}{3}\right\rfloor\right)$$
  $$\text{Classes Needed to Recover} = \max(0, 3T - 4A)$$
- **Zero Floating-Point Drift**: Eliminates IEEE 754 precision issues when tracking the strict 75% attendance margin.
- **Complete Semester Log**: Accurate tracking of all conducted classes ($T$), attended classes ($A$), bunks, absences, and manually logged events/medical leaves (`EXCUSED`) so students retain absolute transparency over their records.

### 2. Squad Sync (1-Tap Peer-to-Peer Timetable Sharing)
- **100% Serverless & File-Free**: Share whole timetables and subject rosters directly with classmates without creating accounts, relying on servers, or juggling JSON file pickers.
- **Base64 Squad Codes (`MARGIN:...`)**: Formatted for seamless sharing via WhatsApp, Telegram, Discord, or SMS, with built-in word-wrapping resilience, messaging app chat-prefix trimming, and automatic slot deduplication.
- **Direct Integration**: Export and import codes directly in the **Insights** tab, **Landing** onboarding, or the **Semester Creation** dialog.

### 3. Full 7-Day Timetable Support
- Complete routine management covering Monday through Sunday (Days 1–7).
- Slot-level attendance binding: Prevents multi-period daily classes from colliding or overwriting each other.
- Time parsing (`AttendanceMath.parseTimeToMinutes`) guarantees strict chronological sorting of classes regardless of 12h/24h time format strings.

### 4. Attendance Time Machine & Interactive Calendar
- **Date Travel**: Seamlessly browse past and future days with timezone-safe UTC $\leftrightarrow$ Local midnight normalization.
- **Native Compose Calendar Sheet**: Zero-dependency interactive calendar featuring:
  - Multi-status heatmap indicators: 🟢 Attended, 🔴 Bunk, 🟣 Excused / Event, 🟡 Duty, ⚪ Day Off.
  - Selected-day breakdown card showing logged class counts, pills, and custom notes.
  - 1-tap jump into that date's ledger.
- **Duty & Extra Attendance**: Dedicated tracker for official college duties (Sports, Cultural, Fest, NCC/NSS), integrated directly into the custom interactive calendar.

### 5. Academic Task Manager & Deadline Reminders
- Track deliverables categorized by type: `Assignment`, `Presentation`, `Practical`, `Exam`, `Quiz`, `Project`, and `Other`.
- Relative due date badges ("Due today", "Due tomorrow", "Overdue by 2 days") with timezone-resilient day rounding.
- High-priority alarms scheduled at 8:00 AM on the day before and the morning of deadlines.
- **Reboot Persistence**: `BootReceiver` (`android:exported="true"`) restores pending alarms automatically after device reboots.

### 6. Insights & Analytics Dashboard
- Toggle between **Entire Semester** aggregate and **Particular Month** analysis.
- Risk-sorted subjects: Automatically highlights subjects in attendance deficit first.
- Real-time heaviest workload identification dynamically bound to active pending deliverables.
- "Wall of Shame" identifying the most bunked subjects.

### 7. 100% Offline & Private
- Local Room SQLite database (v6) stored strictly in device internal storage.
- **Zero Internet Permissions**: `android.permission.INTERNET` is not requested.
- **Zero Telemetry / Trackers**: No Google Analytics, Firebase, or external ads.

### 8. Two-Tier Offline Data Backup & Recovery
- **Tier 1 (Automated OS Backup)**: Integrated with Android Auto Backup (`allowBackup="true"`) to automatically snapshot the SQLite database and preferences to private Google Drive device backup when connected to Wi-Fi and charging.
- **Tier 2 (Manual 1-Tap Offline Snapshot)**: Export a verified, non-destructive `.marginbackup` container to any folder of choice (Downloads, SD Card, USB, or personal storage) via Android's native Storage Access Framework.
- **100% Non-Destructive**: Creating a backup flushes WAL (`PRAGMA wal_checkpoint(FULL)`) and purely performs a read-only stream of `margin.db`; live attendance logs and settings are **never wiped or modified**.
- **Works Without Google Services**: If Google Auto Backup is disabled in phone settings or running on de-Googled devices, Tier 2 remains 100% functional, allowing instant phone-to-phone migration via file transfer.
- **Integrity Verification**: Backups inspect container metadata and verify SQLite magic header bytes (`"SQLite format 3"`) before touching the live database. On confirmation, it prunes stale `-wal`/`-shm` cache and cleanly reboots the app process.

---

## 📱 Navigation & App Structure

Margin features a clean 4-tab bottom navigation scaffold with edge-to-edge Compose rendering:

```
Margin/
├── LandingScreen            # Onboarding, fresh setup, Squad Sync import, past semester reactivator
└── MainScaffold
    ├── TrackScreen (Tab 1)     # Daily ledger, Time Machine, class swaps, bulk actions (Day Off, Bunked, Absent)
    ├── TimetableScreen (Tab 2) # 7-day routine, room numbers, class editor, Squad Sync sharing
    ├── TaskListScreen (Tab 3)  # Deliverables & deadlines, category filters, reminder toggles
    └── InsightsScreen (Tab 4)  # Semester vs monthly analytics, margin buffers, recovery calculators
```

---

## 🛠️ Architecture & Tech Stack

```
com.Margin.app/
├── MainActivity.kt                  # Edge-to-edge ComponentActivity container
├── MarginApplication.kt             # Application singleton, notification channels, WorkManager setup
├── navigation/
│   └── AppNavigation.kt             # MainScaffold, bottom navigation bar, cold-start loading guard
├── domain/
│   └── AttendanceMath.kt            # Pure functions for attendance formulas, time & date parsing
├── data/local/
│   ├── AppDatabase.kt               # Room database (v6) with auto-reconciling MIGRATION_1_6
│   ├── AttendanceRepository.kt      # Unified repository with slot deduplication & swap protection
│   ├── UserPreferencesRepository.kt # DataStore preferences for active session state
│   └── entity/
│       └── Entities.kt              # Room entities (Session, Subject, Timetable, Record, Task)
├── ui/
│   ├── components/
│   │   ├── MarginTopBar.kt          # Unified header with semester switcher
│   │   └── Sheets.kt                # InteractiveCalendarSheet, ExtraDutySheet, SwapClassSheet, SquadSyncSheet, BackupRestoreSheet
│   ├── screens/
│   │   ├── TrackScreen.kt           # Attendance ledger & Time Machine date controls
│   │   ├── TimetableScreen.kt       # 7-day routine viewer & editor
│   │   ├── TaskListScreen.kt        # Deliverables & deadline management
│   │   ├── InsightsScreen.kt        # Semester aggregate & monthly breakdown
│   │   └── LandingScreen.kt         # Onboarding & archived semester manager
│   ├── theme/                       # Modern dark-mode palette (NeonTeal, CardDark, SlatePill)
│   └── viewmodel/
│       ├── AttendanceViewModel.kt   # Central reactive state engine & timetable flows
│       └── TaskViewModel.kt         # Deliverable state & reminder coordination
├── reminders/
│   └── TaskReminderManager.kt       # Exact AlarmManager scheduler (30-bit collision-free codes)
├── receivers/
│   ├── BootReceiver.kt              # Post-reboot broadcast receiver for alarm restoration
│   └── TaskAlarmReceiver.kt         # BroadcastReceiver for firing high-priority deadline notifications
├── workers/
│   └── SmartBunkWorker.kt           # Periodic WorkManager daily bunk-budget analyzer
└── utils/
    ├── BackupManager.kt             # Two-Tier offline backup & restore engine (.marginbackup)
    ├── TimetableExportManager.kt    # Base64 Squad Sync encoding & decoding engine
    └── ViewModelExt.kt              # CompositionLocal ViewModel injection helpers
```

- **UI Framework**: 100% Jetpack Compose with Material 3.
- **Database**: Room 2.6+ with SQLite foreign keys, cascade deletion, and `ORDER BY rowid DESC`.
- **Concurrency**: Kotlin Coroutines with `StateFlow` and `combine` pipelines.
- **Background Tasks**: Android `WorkManager` (24-hour periodic worker).
- **Alarms**: Android `AlarmManager` with exact while-idle delivery.

---

## 🔄 What Changed Since Commit `b64d2c6`

- **Pure Squad Sync (Removed JSON File Import/Export)**: Replaced file import/export with instant, serverless Base64 Squad Sync codes across the Insights screen, Landing screen, and Semester Creation dialog.
- **Two-Tier Offline Backup System**: Implemented automated Android OS Auto-Backup and a 1-tap manual `.marginbackup` snapshot engine with non-destructive WAL checkpointing, SQLite header verification, and safe reboot restoration.
- **Database Evolution (Room v6)**: Auto-reconciling migration preserving all existing tables and indexes while migrating schema to v6.
- **Eradication of Phantom Duplicates**: Implemented slot-to-date binding (`getRecordsBySlotAndDate`, `deleteRecordsBySlotAndDate`) and in-memory deduplication across all ViewModels and workers.
- **Interactive Calendar Sheet**: Replaced generic system DatePicker dialogs with the custom, attendance-indicator-dotted `InteractiveCalendarSheet` across both Time Machine and Duty Logging.
- **Weekend / Sunday Schedule Support**: Added Sunday (Day 7) support across timetable scheduling and cyclic day chevrons.
- **Cold-Start Polish**: Added `isInitialLoading` state guard eliminating the momentary flash of `LandingScreen` on application cold start.
- **Reboot Alarm Reliability**: Made `BootReceiver` exported (`android:exported="true"`), ensuring task deadlines are re-scheduled after device restarts.
- **Collisions Eliminated in Alarms**: Expanded `AlarmManager` request codes to 30-bit unique IDs, eliminating hash collision bugs.
- **Archived Term Reactivation**: Added "Your Semesters" manager on the landing screen, allowing seamless switching and reactivation of past terms.

---

## 🏗️ Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/attendance-manager.git
   ```
2. Open the project in **Android Studio (Ladybug / Hedgehog or newer)**.
3. Verify that **JDK 17 or JDK 21** is selected under **Project Structure > SDKs**.
4. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
5. Deploy to any physical Android device or emulator running **Android 8.0 (API 26) or higher**.
