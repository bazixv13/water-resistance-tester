# Water Resistance Tester — Material 3 Expressive remake

A ground-up remake of [raywdude/water-resistance-tester](https://github.com/raywdude/water-resistance-tester)
in Kotlin + Jetpack Compose, redesigned around
[Material 3 Expressive](https://m3.material.io/blog/building-with-m3-expressive).

The original project publishes only a README and a screenshot, so this app was rebuilt from the
behaviour it describes: use the phone's barometer to check that the water-resistance seals still
hold air, without putting the phone anywhere near water.

## How the test works

1. **Calibrate** — sample the barometer for 3.5 s, taking the average and standard deviation.
2. **Press** — the user holds two fingers on the on-screen pad for 6 s. Pressing a sealed display
   compresses the air inside, so the barometer rises. The timer only advances while both fingers
   stay down, so lifting a finger pauses the test.
3. **Judge** — a peak rise of **+1.5 hPa** or more counts as "seals appear intact". If the baseline
   was noisy (std. dev. over 0.6 hPa), the result carries a caution.

That is the same methodology Samsung service technicians use, as reported in the original README.
It is an indicator, never a warranty — seals fail after drops, repairs and ageing.

## Material 3 Expressive design

* `MaterialExpressiveTheme` with the expressive motion scheme (`MotionScheme.expressive()`); every
  transition, gauge and morph is driven by the expressive spatial/effects springs.
* `LargeFlexibleTopAppBar` with a subtitle that collapses into a small app bar on scroll.
* `ButtonGroup` for the result actions, alongside an overflow action.
* `CircularWavyProgressIndicator` ringing the morphing hero for baseline progress, and a large
  filled start button.
* `MaterialShapes` + `Morph` shape morphing: the result hero morphs from a circle into a
  `Sunny` burst on a pass or a `SoftBoom` on a failure; step badges use `Cookie9Sided`.
* Expressive `Shapes` (extra-large rounded containers), tonal surfaces, bold display type and a
  water-inspired teal palette with semantic success/caution accents, in light and dark themes.
* `ModalBottomSheet` help/FAQ, keep-screen-on while a test is running, and phase changes that
  cross-fade and scale instead of swapping.
* Haptics with weight: toggle feedback on each pad, a segment tick per second of the countdown,
  a confirm when the pass mark is crossed, and a composed double-click or double-thud pattern for
  the verdict (with `VIBRATE`; falls back to a waveform on older devices).
* A bottom opacity fade on the result screen when there is more content to scroll.

## Screens

| Phase | What it shows |
| --- | --- |
| Ready | Device/OS info, how-it-works steps, dry-phone warning, start button, support link |
| Calibrating | Live average, std. deviation and current pressure plus a determinate loading indicator |
| Pressing | Two-finger press pad, morphing gauge, live trace with the pass threshold, countdown |
| Result | Morphing hero, verdict, peak rise and baseline stats, retry/share actions |
| Unsupported | Shown on phones without a barometer |
| Help | `ModalBottomSheet` FAQ (also reachable from the app bar) |

## Build

Everything needed to build — JDK 21, the Android SDK (platform 37, build-tools 37), Gradle and all
caches — is contained in `.toolchain/` and `.gradle-home/` inside this folder.

```bash
./build.sh                     # debug APK -> app/build/outputs/apk/debug/app-debug.apk
./build.sh :app:installDebug   # install on a connected device
```

`./build.sh` exports `JAVA_HOME`, `ANDROID_HOME` and `GRADLE_USER_HOME` to the project-local
paths. `local.properties` points at the bundled SDK.

## Stack

Kotlin 2.3.21 · AGP 9.5.0 · Gradle 9.8.2 · compileSdk 37 · minSdk 26 ·
Compose 1.12.0 · Material3 **1.5.0-beta02** (the version that ships the Expressive components).
Current build: `2.8-expressive-polish` (versionCode 10).

## Notes and limitations

* Material3 1.5 is a beta, so the Expressive API can shift between releases, and it requires
  compileSdk 37 / AGP 9.1+.
* Verified by compiling the debug APK; there is no emulator or physical device in this
  environment, so the barometer flow and haptics have not been exercised at runtime.
* The app needs a device with a barometer; emulators usually do not have one.
* Original app and methodology: Raymond Wang. This remake keeps the attribution and support links.
