# MY FIRST AI — THE HUMAN INSIDE THE GAME

An Android-first AI-training simulation prototype.

## Build the APK
Every push to `main` starts the **Build Android APK** GitHub Actions workflow. When it succeeds:
1. Open the repository's **Actions** tab.
2. Select the latest successful **Build Android APK** run.
3. Under **Artifacts**, download **My-First-AI-debug-apk**.
4. Extract the downloaded artifact ZIP and install `app-debug.apk` on your Android device. This ZIP is only GitHub's artifact packaging; the actual installable file inside is the APK.

You can also run the workflow manually from Actions → Build Android APK → Run workflow.

## Current prototype features
- Touch-friendly virtual world and AI character.
- Local chat simulation and teachable memory entries.
- Hunger meter that drains over ten real-world hours, including while the app is closed.
- Food buttons to restore hunger.
- Creator commands: `/obstacle 1` through `/obstacle 3`, `/wall 1`, `/cube 1`, `/ball 1`, `/tree 1`, `/remove`, and `/clear`.
- Memory, brain-activity, and creator panels.
- Local progress saved on the device.

## Important limitations
This first build is a playable 2D prototype, not the final 3D version. The chat is a local scripted simulation; no external AI model is connected. Game data is stored locally in the app's WebView storage. If hunger reaches zero, the game shows a fictional game-over/reinstall screen; it does not crash Android or delete real device files. The Restart Simulation button safely resets the game.

## Project
- Android package: `com.void.aitraining`
- Minimum Android version: Android 6.0 (API 23)
- Build: Android Gradle Plugin 8.6.1 / Gradle 8.7 / Java 17
