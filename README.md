# MerkOffice

An offline-first Android document editor project.

## Current starter features
- Native Android shell with a WebView-based rich-text editor
- Create and edit documents locally
- Basic formatting: bold, italic, underline, headings, lists and alignment
- Save and reopen editor content as HTML files through Android's Storage Access Framework
- Export/print to PDF using Android's print framework
- English/Hausa language toggle placeholder

## Planned next stages
- DOCX import/export and formatting preservation
- PDF reader and document manager
- Tables and image insertion
- Hausa interface translation
- Accessibility and device testing

## Build
Open this folder in Android Studio (JDK 17; Android Gradle Plugin 8.1.4; Gradle 8.2; Android SDK 34), allow Gradle sync, then Build > Build APK(s).

This repository is a starter implementation, not a finished Microsoft Word replacement. DOCX support is not yet implemented in this first scaffold.
