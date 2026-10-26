# Osho Audio

An Android app for listening to Osho's audio discourses — 295 series,
5,179 talks in Hindi (160 series / 2,283 talks) and English (135 series /
2,896 talks), organised by language and by the person spoken about.

Audio is **streamed on tap, not bundled** (the full collection is ~181 GB).
It comes from the Internet Archive item `osho-audio-discourses-collection`.
Per-talk download saves audio into the app's own folder for offline listening.

## Features

- Browse by language, by person, or A–Z; search across series
- Background playback with a foreground service
- Per-talk download, with Wi-Fi-only and "Download all" settings
- Sleep timer (Off → 15 → 30 → 60 minutes)
- Bluetooth and lock-screen controls via MediaSession (play/pause, next,
  previous, remove-earbud auto-pause)
- Audio-focus handling: pauses for reels, videos, and voice-note recording
  and resumes only if it was focus-paused — manual pauses are never
  auto-resumed
- Player screen with series/person typography, like toggle (local only),
  download button, Downloaded/Streaming source indicator, repeat-one
- Colour-glow halo and theme support

## Project layout

| Path | What it is |
| --- | --- |
| `app/src/main/` | The Android app (Kotlin, Jetpack Compose) |
| `app/src/main/assets/catalog.json` | Talk index used by the app |
| `catalog.json` | Same talk index, project-root copy |
| `item_metadata.json` | Archive.org item metadata the catalog was built from |
| `build_catalog.py` | Script that builds `catalog.json` from the metadata |
| `telegram_osho.json`, `telegram_measure.json` | Coverage measurements comparing Telegram audio sources with the archive |
| `gradle/` | Version catalog + Gradle wrapper properties |

## Build

Open the project in Android Studio, or build with Gradle. Two files are
intentionally **not** in this repo:

- `gradle/wrapper/gradle-wrapper.jar` — binary; use a local Gradle install
  or fetch the wrapper jar for the version in
  `gradle/wrapper/gradle-wrapper.properties`.
- `local.properties` — machine-specific Android SDK path; create your own
  with `sdk.dir=/path/to/android-sdk`.

The Telegram measurement scripts are also left out: they need private
credentials to run and are not part of the app.
