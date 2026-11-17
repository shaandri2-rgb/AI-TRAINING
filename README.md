# MY FIRST AI — THE HUMAN INSIDE THE GAME

Android-first 3D AI-training simulation prototype.

## Build the APK

Every push to `main` starts the **Build Android APK** GitHub Actions workflow. When it succeeds:
1. Open the repository's **Actions** tab.
2. Select the latest successful **Build Android APK** run.
3. Under **Artifacts**, download **My-First-AI-BANC-3D**.
4. Extract the artifact ZIP and install `app-debug.apk` on Android.

The downloaded artifact ZIP is only GitHub's packaging. The installable file inside is the APK.

## BANC fruit-fly connectome integration

The CI build downloads the public BANC v888 metadata and neuron-to-neuron edge table from Harvard Dataverse, then extracts a compact **50-neuron leg sensor → interneuron → motor circuit** into the APK. The Android Java engine runs a lightweight leaky-integrate-and-fire (LIF) approximation at runtime. Its motor activity is mapped to movement and turning of the 3D humanoid avatar. The Brain tab shows the selected neuron IDs, measured connections and live spike activity.

Important scientific limits:
- BANC v888 contains a much larger adult female fruit-fly connectome; this mobile build packages only a selected 50-neuron leg-control subnetwork, not all 158,262 neurons.
- The connectivity is drawn from the published BANC v888 edge list. Membrane dynamics, signal scaling, game sensory inputs and the avatar mapping are calibrated engineering approximations, not a validated reproduction of the original Minecraft mod or a complete biological fly.
- The fruit-fly circuit controls movement/reactions only. The chat and teaching/memory feature remain a separate local scripted system. This does not create a conscious or human-level AI.
- The full source tables are downloaded during CI and are not stored in the repository. The APK contains only the compact extracted circuit.

Dataset citation: Bates, Phelps, Kim, Yang et al., “Distributed control circuits across a brain-and-nerve-cord connectome,” *Nature* (2026), BANC v888, Harvard Dataverse DOI: [10.7910/DVN/7WTH1N](https://doi.org/10.7910/DVN/7WTH1N). BANC v888 data are released under CC BY 4.0; attribution is shown in the Brain panel.

## Other prototype features

- Touch-friendly 3D WebGL world and humanoid AI character.
- Local chat simulation and teachable memory entries.
- Hunger drains over ten real-world hours, including while the app is closed.
- Food buttons restore hunger.
- Creator commands: `/obstacle 1` through `/obstacle 3`, `/wall 1`, `/cube 1`, `/ball 1`, `/tree 1`, `/remove`, and `/clear`.
- Memory, live brain-activity, and creator panels.
- Local progress saved on the device.

## Limitations

The chat is not connected to an external LLM. The BANC motor circuit is a small neural control experiment, not a full brain or a general-purpose AI. The app has not been physically tested on every Android device. If hunger reaches zero, it shows a fictional game-over/reinstall screen; it does not crash Android or delete files.

## Project

- Android package: `com.aitraining.sim`
- Minimum Android version: Android 6.0 (API 23)
- Build: Android Gradle Plugin 8.6.1 / Gradle 8.7 / Java 17
