# Lumi Dex Track for Android

This wrapper bundles the existing web interface, data and sprites, and adds read-only save monitoring. No web server or internet connection is needed at runtime. Package ID: `com.lumidex.track` (installs alongside upstream).

## Build

Install JDK 17 and Android SDK Platform 35 / Build Tools 35.0.0 (Android Studio's SDK Manager can install these). Set `ANDROID_HOME` to the SDK location, or put `sdk.dir=/absolute/path/to/sdk` in `android/local.properties`. Set `JAVA_HOME` to JDK 17.

From this directory:

```sh
./gradlew assembleDebug lintDebug
```

Every push and pull request also runs this verification in GitHub Actions. The workflow validates JavaScript and JSON, runs the save parser tests, runs Android lint, builds the debug APK, and stores the APK as a 30-day workflow artifact. Tags matching `v*` additionally publish that APK to a GitHub Release. It is a reproducible debug/sideload build; release signing is intentionally not configured until a signing key is added to repository secrets.

Windows: use `gradlew.bat`. Gradle 8.11.1 and Android Gradle Plugin 8.9.2 are pinned. The first build requires internet access. The build copies the web assets automatically; do not edit generated assets.

Install `app/build/outputs/apk/debug/app-debug.apk`. For distribution, use Android Studio's Generate Signed Bundle / APK action and retain your own signing key for future updates. Never commit signing keys. The upstream signing key is not required for this separate app.

## Connect Eden

1. Save in-game in Luminescent Platinum.
2. In Lumi Dex Track, open Settings → Eden save folder → Choose folder.
3. Open **Eden** in the Android system picker's sidebar (its document provider), then browse to Luminescent Platinum's game folder, identified by title ID `0100000011D90000`:

   ```text
   Android/data/dev.eden.eden_emulator/files/nand/user/save/
   └── 0000000000000000/
       └── <your profile ID>/
           └── 0100000011D90000/
   ```

   Select the folder directly containing **SaveData.bin**, not the whole NAND, an exported ZIP or a save-state folder.
4. Grant folder access. The app reads the file every five seconds, including while Eden is foregrounded, using an ongoing notification. No write permission is requested and no save file is modified.
5. Settings shows status and the last successful read. Pause stops the service; Start resumes it. Last verified catches stay locked while paused or if reading fails. Restart monitoring after force-stop/reboot; Android may also stop services under resource pressure.

If Eden is absent from the picker, availability depends on the installed Eden build. Use Eden's custom save location if your build supports it, selecting the same accessible live save folder here. An exported copy updates only when you replace that copy. This app does not bypass Android private-storage restrictions.

The service uses Android's `specialUse` foreground-service type for user-enabled companion monitoring. Play Store publication would need a separate policy/declaration review; this project currently targets sideloaded APKs.

## Caught state and current support

Manual marks and save-derived marks are separate. A caught species from the save is checked and locked in Dex, details and route encounter rows; route locks indicate global ownership, not the place of capture. Clearing manual progress never clears save-derived status. A successful read replaces the last snapshot, so loading another/older save follows that save. Invalid or partial reads retain the previous valid snapshot, including when choosing a different folder until that folder validates.

The parser validates known BDSP save lengths, MD5 and Pokédex state values before accepting data. It reads historical species caught flags (so traded/released Pokémon remain caught). Luminescent revision 1 (`0xFFFF0134`) uses packed nibble states and supports expanded species. It also validates encrypted Pokémon records in the party, boxes, and daycare, extracting each record's species/form pair and mapping it to the app's exact form IDs, including Burmy cloaks and regional forms. The species-level flag remains caught after a Pokémon is released; form locks reflect forms currently present in the save.

The supplied sample decodes to 72 of the app's species. Its stored MD5 does not match vanilla BDSP's checksum algorithm. For this specific Lumi revision, checksum mismatch is reported in Settings rather than rejected. Two identical full-file reads across separate polls plus header, length and state validation reduce the risk of reading an in-progress write; they do **not** prove integrity of a consistently corrupted file. Vanilla BDSP saves still require a valid MD5. Other Lumi revisions fail with an unsupported-format message. Live device testing with Eden remains necessary.

Format references (used to understand file layout; implementation written for this project):
- https://github.com/WolfpackMC/PKLumiHex/blob/master/PKHeX.Core/Saves/SAV8BSLuminescent.cs
- https://github.com/WolfpackMC/PKLumiHex/blob/master/PKHeX.Core/Saves/Substructures/Gen8/LUMI/Zukan8bLumi.cs
- https://github.com/kwsch/PKHeX/blob/master/PKHeX.Core/Saves/Util/SaveUtil.cs
- Eden provider: https://github.com/eden-emulator/mirror/blob/master/src/android/app/src/main/AndroidManifest.xml

## Tests

From the repository root:

```sh
node --test tests/save-sync.test.js
javac -d /tmp/lumi-parser-tests android/app/src/main/java/com/lumidex/track/SaveParser.java tests/SaveParserTest.java
java -cp /tmp/lumi-parser-tests SaveParserTest
```

Synthetic fixtures cover both layouts, seen versus caught, first/last species, supported lengths, revision mismatch, corruption and invalid state rejection. Pass a path to SaveData.bin as an optional argument to SaveParserTest to exercise the supplied sample regression. Device acceptance checks still needed: select Eden folder, save after catching, observe locks, clear manual marks, pause/restart, restart app, revoke folder access, and change profiles. Test on the target device before relying on background monitoring.
