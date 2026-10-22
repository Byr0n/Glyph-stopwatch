# Glyph Stopwatch

A stopwatch for the Nothing Phone (4a) Pro that shows on the Glyph Matrix.

* Place the phone face down: the stopwatch starts.
* Press volume down once: pause (one short buzz). Press once again: resume.
* Press volume down twice within about 400 ms: restart from 0:00 (two quick buzzes).
* Pick the phone up: the final time shows for 5 seconds, then the timer resets and the matrix turns off.

The 4a Pro matrix is a 13 x 13 grid with 137 LEDs in a circle (the corners have none), too
narrow for `mm:ss` on one line, so minutes sit above seconds. The digits use rows 1 to 11 and
columns 3 to 9, which are all inside the circle. From 100 minutes on it shows hours above minutes. While paused the digits blink dimmed.

## Build

Open the project in Android Studio (or run `./gradlew assembleDebug`). The first build downloads
Nothing's Glyph Matrix SDK into `app/libs/`, because its licence does not allow it to be
committed here.

## Two ways to run it

**Glyph Toy (recommended on the Phone (4a) Pro).** The 4a Pro only runs always on toys, which
the system shows through Flip to Glyph when the phone goes face down. The system showing the
toy starts the stopwatch, and picking the phone up stops it. No need to arm anything.

**App mode.** Arm the timer in the app. It watches the accelerometer itself and draws with the
app's own Glyph Matrix access. Use this if you would rather keep Flip to Glyph off.

Both share one stopwatch, so if both are on they never run two timers. While the toy is
showing it draws the time, because the system puts toys above apps on the matrix.

## Setup on the phone

1. Install the app and open it.
2. Tap **Open accessibility settings** and turn on **Glyph Stopwatch volume keys**. Volume
   down works the same in both modes.
3. Tap **Exempt from battery optimisation** so the timer keeps running.
4. Optional: tap **Show test pattern 12:34** to check the matrix works.
5. For the toy: tap **Add to Glyph Toys** and add **Stopwatch**. Then go to Settings > Glyph
   Interface > Flip to Glyph > Always-on Glyph Toy and pick it.
6. For app mode instead: tap **Arm timer**. If Flip to Glyph is on with another toy, that toy
   will draw over the stopwatch, so pick Stopwatch there or turn Flip to Glyph off.
7. Place the phone face down.

## How it works

| Class | Job |
| --- | --- |
| `TimerSession` | The one shared stopwatch. Redraws each second on the toy's matrix when it is showing, otherwise on the app's. |
| `StopwatchToy` | Glyph Toy service. Starts the stopwatch when the system shows it, stops it when hidden. A Glyph Button long press pauses on phones that have one. |
| `TimerService` | App mode foreground service holding a partial wake lock while armed, so the face down sensors keep working. |
| `FaceDownDetector` + `FlipFilter` | Face down = accelerometer z below -9.0 m/s² for 500 ms with the proximity sensor near. Face up = z above -6.0 m/s² for 500 ms. |
| `VolumeKeyService` + `PressClassifier` | Accessibility service that takes over volume down only while the timer is running or paused, and splits single from double presses with a 400 ms window. |
| `GlyphRenderer` + `MatrixDigits` | Draws the digits with a 3 x 5 pixel font and sends them with `setAppMatrixFrame`. |
| `Stopwatch` | Pure state machine: IDLE, RUNNING, PAUSED. |

`Stopwatch`, `PressClassifier`, `FlipFilter` and `MatrixDigits` have no Android dependencies and
are covered by unit tests in `app/src/test`.

## Still to do

* Test on the phone with the screen off and tune the thresholds in `FlipFilter` and `PressClassifier`.
* Check how often the system lets an always on toy redraw. The toy draws every second, but if
  the phone only refreshes it on the once a minute AOD event, the seconds will not show.
* If the accessibility service misses volume keys with the screen off, add the fallback from the
  spec: a `MediaSession` with a silent track and a `VolumeProviderCompat`, so presses arrive in
  `onAdjustVolume`.
