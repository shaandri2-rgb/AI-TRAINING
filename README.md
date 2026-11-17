# MY FIRST AI — THE HUMAN INSIDE THE GAME

Android-first 3D AI-training game prototype.

## Android build

Every push to `main` starts **Build Android APK**. When the GitHub Actions run succeeds:
1. Open the repository's **Actions** tab.
2. Open the newest successful **Build Android APK** run.
3. Download the artifact named **My-First-AI-FlyWire-FAFB-v783**.
4. Extract the artifact ZIP and install `app-debug.apk`.

The artifact ZIP is only GitHub's packaging; the APK inside is the installable Android app.

## Whole-brain connectome

The build downloads the published FlyWire FAFB v783 model files:
- `Completeness_783.csv`
- `Connectivity_783.parquet`

The packer preserves every neuron and every directed connection in the Shiu et al. v783 computational model, using a compact compressed-sparse-row (CSR) binary asset. The Android app loads that network and runs a lightweight leaky-integrate-and-fire (LIF) approximation.

**This is a whole-connectome-based mobile simulation, not a perfect reproduction of the original Brian2 model.** The original model uses Brian2, its published parameters and its own runtime. This Android implementation uses a simplified fixed-step Java runner to fit a phone.

### How the brain affects the game

- Touch, gait and nearby obstacles provide experimental stimulation signals to 24 distributed network input ports.
- Network activity is measured and passed into an experimental game-director mapping.
- The director can create an obstacle, tree or ball in the 3D world.
- The AI avatar also receives an experimental movement signal.

Important scientific limit: those 24 input ports are distributed by index, not selected from a verified sensory-neuron annotation list. The director and avatar mappings are game-engine rules, not proof that the fly brain naturally chooses those objects or has human-like thoughts. This is a connectome simulation, not a conscious fly or human-level AI.

## Data and citations

- FlyWire FAFB v783 connectome: Dorkenwald et al., “Neuronal wiring diagram of an adult brain,” *Nature* 634, 124–138 (2024), [DOI 10.1038/s41586-024-07558-y](https://doi.org/10.1038/s41586-024-07558-y).
- Whole-brain annotations: Schlegel et al., *Nature* 634, 139–152 (2024), [DOI 10.1038/s41586-024-07686-5](https://doi.org/10.1038/s41586-024-07686-5).
- LIF model: Shiu et al., “A Drosophila computational brain model reveals sensorimotor processing,” *Nature* 634, 210–219 (2024), [DOI 10.1038/s41586-024-07763-9](https://doi.org/10.1038/s41586-024-07763-9).
- Model implementation: [philshiu/Drosophila_brain_model](https://github.com/philshiu/Drosophila_brain_model).

The FlyWire data have their own terms and attribution requirements. Check the current [FlyWire data terms](https://flywire.ai/) before redistribution or commercial use. The build downloads source data at build time; the original source files are not committed to this repository.

## Other game features

- Local chat teaching and memory entries (scripted; no external LLM is connected).
- Hunger drains over ten real-world hours, including while the app is closed.
- Food buttons restore hunger.
- Memory, brain activity and creator panels.
- Local progress saved on device.
- At zero hunger, a fictional in-game game-over screen appears; Android does not crash and files are not deleted.

## Technical

- Package: `com.aitraining.sim`
- Minimum Android: 6.0 (API 23)
- Java 17, Gradle 8.7
