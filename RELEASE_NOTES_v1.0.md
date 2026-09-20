# Lumi Dex Track v1.0

The first Android release of Lumi Dex Track.

## What’s included

- Native Android APK wrapping the existing Lumi Dex interface.
- Eden save-folder monitoring for Luminescent Platinum.
- Read-only polling of `SaveData.bin` every five seconds.
- Two matching reads before a save update is accepted.
- Automatic caught detection from the save, including exact Pokémon forms where available.
- Save-derived caught marks are locked and cannot be cleared manually.
- Route catches stay consistent across every route where that Pokémon appears.
- “Show only uncaught” route filter.
- Compact mobile layout and an Android settings screen for selecting the Eden save folder.
- GitHub Actions workflow for validation, Android SDK caching, APK builds, and tagged releases.

## Installation

Download the APK attached to this release and install it on an Android device. In the app, open **Settings → Eden save folder → Choose folder** and select the folder containing `SaveData.bin` for title ID `0100000011D90000`.

The app reads the save file without modifying it. See [`android/README.md`](android/README.md) for the folder layout, supported save formats, and testing notes.

## Attribution

This project is based on [Lumi Dex](https://github.com/soonerbutte-rockybeers/lumi-dex) by soonerbutte-rockybeers. The Android packaging, Eden integration, save parsing, route synchronization, and other changes in this fork are maintained separately.
