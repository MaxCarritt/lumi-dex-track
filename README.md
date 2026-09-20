# Lumi Dex Track

Lumi Dex Track is an offline Pokédex and progress tracker for Pokémon Luminescent Platinum. It is a small web app that can also be built as an Android APK.

This project is based on [Lumi Dex](https://github.com/soonerbutte-rockybeers/lumi-dex) by soonerbutte-rockybeers. This fork adds Android packaging, Eden save synchronization, exact form detection, and reproducible build tooling.

The Android version adds an Eden save-folder connection. It reads the selected save file, marks Pokémon found in the save as caught, and locks those marks so they cannot be removed in the app. It reads the save without modifying it.

## Use the app

- **Android APK:** download the APK from the [latest release](https://github.com/MaxCarritt/lumi-dex-track/releases/latest) and install it as a sideloaded app. Android setup and Eden instructions are in [`android/README.md`](android/README.md).
- **Web app:** open the [web version](https://maxcarritt.github.io/lumi-dex-track/) in Chrome and choose “Add to Home screen.”

The web app stores manual progress in browser storage. Save-file syncing is available in the Android build.

## What it includes

- Route tracker covering 80 locations, encounters, trainers and items.
- Pokédex entries for 513 species plus alternate forms, with locations, stats, abilities, moves and evolution chains.
- Boss checklist covering the story and post-game fights.
- Team builder with learnsets, matchup grids, recommendations and team grading.
- Move database covering all 826 moves.
- Offline bundled data and sprites, with an option to cache sprites in the web version.

## Eden save sync

In the Android app, open Settings → Eden save folder → Choose folder. For Luminescent Platinum, find title ID `0100000011D90000` under Eden's save directory and select the folder that directly contains `SaveData.bin`:

```text
Android/data/dev.eden.eden_emulator/files/nand/user/save/
└── 0000000000000000/
    └── <your profile ID>/
        └── 0100000011D90000/
```

The app checks the save every five seconds while monitoring is enabled. It requires two identical reads before accepting a change, which avoids most partial-save reads. Species-level Pokédex flags preserve historical catches. Valid Pokémon records also provide exact forms, including regional forms and Burmy cloaks.

The current parser supports Luminescent revision 1 saves and standard BDSP save revisions documented in [`android/README.md`](android/README.md). The supplied sample save decodes to 72 caught species. Luminescent’s save checksum differs from standard BDSP, so the Android monitor validates complete repeated reads and individual Pokémon record checksums instead of rejecting the save on the vanilla checksum alone.

## Build the Android APK

The Android project is in [`android/`](android/). It bundles the existing app, data and sprites into a native Android shell and adds the save-folder monitor.

Requirements are JDK 17 and Android SDK Platform 35 / Build Tools 35.0.0. From the `android` directory:

```sh
./gradlew assembleDebug lintDebug
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`. Full setup, testing and signing notes are in [`android/README.md`](android/README.md).

GitHub Actions runs the JavaScript, parser and data checks, Android lint, and APK build on pull requests and pushes to `main`. A `v*` tag also publishes the debug APK to a GitHub Release. The workflow is in [`.github/workflows/android.yml`](.github/workflows/android.yml).

## Data

The app data is based on Luminescent 2.2F data from Team Luminescent’s site. Pokémon sprites and game names are the property of their respective rights holders.
