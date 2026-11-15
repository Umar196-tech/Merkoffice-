# MerkOffice Pro Android project

This is the Android Studio project supplied in the user's `MerkOffice-Android-source.zip`.

## Offline audit
- The Android manifest declares CAMERA and legacy WRITE_EXTERNAL_STORAGE (maxSdk 28).
- The manifest does **not** declare android.permission.INTERNET.
- The app loads its bundled local page from Android assets: `file:///android_asset/www/index.html`.
- The HTML and Android bridge are bundled in `app/src/main/assets/www/`; the bridge comments and implementation describe local file export, local storage, and no network requests.
- Document data is stored locally by the app (IndexedDB / local app storage). No server or account is required for the core local workflow.
- Camera permission is requested only when the scanner feature is used.

## Build requirements
- Android Studio
- JDK 17
- Android Gradle Plugin 8.1.4
- Gradle wrapper 8.2
- Android SDK 34

Open this directory in Android Studio and build with **Build > Build APK(s)**.

## Important feature notes
The supplied project is the Android wrapper and bundled web app. Features visible in the UI should be tested on a real Android device before treating them as production-ready. The source package includes the complete web UI, native bridge, and Java Android host. The supplied APK is a prebuilt 1.0.0 artifact; it is not a substitute for retaining the source project.

## Permissions
The app needs camera access for scanning. File selection and export use Android's document/file mechanisms. The source manifest intentionally has no INTERNET permission, so online services and cloud sync are not part of this build.
