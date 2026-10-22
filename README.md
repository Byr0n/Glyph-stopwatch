# Glyph Stopwatch

A stopwatch for the Nothing Phone (4a) Pro that shows on the Glyph Matrix.

* Place the phone face down: the stopwatch starts.
* Press volume down once: pause (one short buzz). Press once again: resume.
* Press volume down twice within about 400 ms: restart from 0:00 (two quick buzzes).
* Pick the phone up: the final time shows for 5 seconds, then the timer resets and the matrix turns off.

The 4a Pro matrix is 13 x 13 LEDs, too narrow for `mm:ss` on one line, so minutes sit above
seconds. From 100 minutes on it shows hours above minutes. While paused the digits blink dimmed.

## Build

Open the project in Android Studio (or run `./gradlew assembleDebug`). The first build downloads
Nothing's Glyph Matrix SDK into `app/libs/`, because its licence does not allow it to be
committed here.

## Setup on the phone

1. Install the app and open it.
2. Tap **Open accessibility settings** and turn on **Glyph Stopwatch volume keys**.
3. Tap **Exempt from battery optimisation** so the service stays alive.
4. Optional: tap **Show test pattern 12:34** to check the matrix works.
5. Tap **Arm timer**, then place the phone face down.

If Nothing's own **Flip to Glyph** feature is on, it may draw over the stopwatch when the phone
is face down. Turn it off in Settings > Glyph Interface if that happens.

## How it works

| Class | Job |
| --- | --- |
| `TimerService` | Foreground service holding a partial wake lock while armed. Runs the state machine and redraws the matrix each second. |
| `FaceDownDetector` + `FlipFilter` | Face down = accelerometer z below -9.0 m/s² for 500 ms with the proximity sensor near. Face up = z above -6.0 m/s² for 500 ms. |
| `VolumeKeyService` + `PressClassifier` | Accessibility service that takes over volume down only while the timer is running or paused, and splits single from double presses with a 400 ms window. |
| `GlyphRenderer` + `MatrixDigits` | Draws the digits with a 3 x 5 pixel font and sends them with `setAppMatrixFrame`. |
| `Stopwatch` | Pure state machine: IDLE, RUNNING, PAUSED. |

`Stopwatch`, `PressClassifier`, `FlipFilter` and `MatrixDigits` have no Android dependencies and
are covered by unit tests in `app/src/test`.

## Still to do

* Test on the phone with the screen off and tune the thresholds in `FlipFilter` and `PressClassifier`.
* If the accessibility service misses volume keys with the screen off, add the fallback from the
  spec: a `MediaSession` with a silent track and a `VolumeProviderCompat`, so presses arrive in
  `onAdjustVolume`.
