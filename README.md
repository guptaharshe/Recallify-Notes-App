# Recallify 🧠

**Recallify** is a sleek, modern, and minimal note-taking application built with **Jetpack Compose**. It focuses on performance and a stable writing experience, allowing you to capture ideas with ease.

## ✨ Features

- **Modern Two-Page Layout**: A clean home screen for browsing and a dedicated, distraction-free editor.
- **Global Formatting**: Easily toggle **Bold** and *Italic* styles for your entire note.
- **Quick Text Sizing**: Adjust the editor's font size using simple **+** and **-** controls for maximum stability.
- **Focused Home Screen**: Only displays note titles and dates, keeping your workspace clutter-free.
- **Dark Mode Support**: Seamlessly switch between light and dark themes.
- **Pin Notes**: Keep your most important notes at the top.
- **Fast Search**: Find notes instantly by title or content.
- **Persistence**: Powered by **Room Database** and **DataStore**.

---

## 📸 Screenshots

| Home Screen (List) | Note Editor (Formatting) |
| :---: | :---: |
| ![Home Screen](screenshots/home_light.png) | ![Edit Screen](screenshots/edit_note.png) |

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Navigation**: Jetpack Navigation Component
- **Database**: Room Persistence Library
- **Architecture**: MVVM

---

## 📂 Project Structure

```text
Recallify/
├── app/
│   ├── src/main/java/com/recallify/app/
│   │   ├── data/                 # Data Layer (Room, Entities, Repo)
│   │   ├── ui/                   # UI Layer (Theme, Screens)
│   │   ├── viewmodel/            # ViewModels
│   │   └── MainActivity.kt       # Navigation & Screen Implementation
│   └── build.gradle.kts          
├── screenshots/                  # App screenshots
└── README.md
```

---
*Developed with ❤️ by Your Name*
