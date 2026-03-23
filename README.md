# Margin 📊

**Margin** is a premium, data-driven attendance tracker and academic task manager built for Android. This project was born out of a real-world frustration with existing, clunky attendance apps and was "vibe coded" from the ground up to provide a seamless, aesthetically pleasing experience for students.

## 🚀 The Vision
I wanted to create a tracker that didn't just feel like a spreadsheet. **Margin** was developed to tackle the real-world issue of maintaining academic attendance thresholds (the "margin") without the friction of traditional manual tracking. It balances a deep, modern "Glassmorphism" UI with robust offline-first architecture.

---

## ✨ Key Features

- **Dynamic Attendance Math**: Automatically calculates exactly how many classes you need to attend to stay above your required percentage (e.g., 75%).
- **Subject-Specific Tracking**: Support for individual subject codes and names, with support for `Present`, `Absent`, `Proxy`, and `Cancelled` statuses.
- **Smart Task Management**: Integrated Assignments, Presentations, and Practicals lists with zero-flicker UI state handling.
- **Aggressive Task Reminders**: Multiple sequential alarms via `AlarmManager` (5x on day-before, 2x on lead-up days) to ensure deadlines are never missed.
- **Smart Bunk Roasts**: Periodic background analysis via `WorkManager` that roasts you if your attendance is low or calculates your "safe bunk" budget.
- **Weekly Overview**: A timetable-driven dashboard showing your daily class counts and overall semester progress, sorted by insertion order for intuitive planning.
- **Privacy First**: Built as an offline-first Room database application. Your academic data stays on your device.

---

## 🛠️ Tech Stack

- **UI**: 100% Jetpack Compose with Material 3.
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture patterns.
- **Database**: Room Persistence Library for robust local storage.
- **Async**: Kotlin Coroutines & Flow for reactive, real-time UI updates.
- **Background Work**: WorkManager for periodic attendance analysis and "Smart Bunk" notifications.
- **Notifications**: Local Notification API & AlarmManager for high-precision, multi-alarm reminders.
- **Design**: Specialized "Neon Teal" & "Deep Nebula" dark mode theme with custom gradients.

---

## 🎨 Aesthetic
**Margin** features a premium design system including:
- **Glassmorphism**: Subtle translucency and blurred backgrounds.
- **Micro-animations**: Smooth crossfades and state transitions for a fluid feel.
- **Dynamic Colors**: Conditional UI that shifts from Teal (Safe) to Orange (Warning) to Red (Critical) based on your real-time data.

---

## 🗺️ Roadmap & Upcoming Features

- **Supabase Cloud Sync**: Effortless cloud-driven session backups and cross-device synchronization.
- **Export to PDF/Excel**: Generate local reports for academic records.
- **Community Templates**: Share and download session subject templates for specific courses.

---

## 🏗️ Getting Started

1. Clone the repository.
2. Open in **Android Studio (Hedgehog or later)**.
3. Build and run on an emulator or physical device (Android 8.0+).
4. Create your first Semester, define your subjects, and start tracking!

---

*Vibe Coded with ❤️ to solve the attendance struggle.*
