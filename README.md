# Glyph Stopwatch

A stopwatch for the Nothing Phone (4a) Pro that shows on the Glyph Matrix.

* Place the phone face down: the stopwatch starts.
* Shake the phone while it is still face down: pause (one short buzz).
* Shake again while paused: restart from 0:00 and keep counting (two quick buzzes).
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
the system shows through Flip to Glyph. The system can also show the toy at other times, so
while it is shown the toy checks the accelerometer itself: the stopwatch starts only once the
phone has been face down for 500 ms, and stops when it is picked up. No need to arm anything.

Shake the phone while it stays face down, for example by jiggling it on the table. Picking it
up to shake it would stop the timer, and the shake detector ignores the phone once it turns
over, so a pick up never counts as a shake.

**App mode.** Arm the timer in the app. It watches the accelerometer itself and draws with the
app's own Glyph Matrix access. Use this if you would rather keep Flip to Glyph off.

Both share one stopwatch, so if both are on they never run two timers. While the toy is
showing it draws the time, because the system puts toys above apps on the matrix.

## Setup on the phone

1. Install the app and open it.
2. Tap **Exempt from battery optimisation** so the timer keeps running.
3. Optional: tap **Show test pattern 12:34** to check the matrix works.
4. For the toy: tap **Add to Glyph Toys** and add **Stopwatch**. Then go to Settings > Glyph
   Interface > Flip to Glyph > Always-on Glyph Toy and pick it.
5. For app mode instead: tap **Arm timer**. If Flip to Glyph is on with another toy, that toy
   will draw over the stopwatch, so pick Stopwatch there or turn Flip to Glyph off.
6. Place the phone face down.

## How it works

| Class | Job |
| --- | --- |
| `TimerSession` | The one shared stopwatch. Redraws each second on the toy's matrix when it is showing, otherwise on the app's. |
| `StopwatchToy` | Glyph Toy service. While the system shows it, it holds a wake lock and runs `FaceDownDetector`, starting the stopwatch on face down and stopping it on face up or when hidden. A Glyph Button long press pauses on phones that have one. |
| `TimerService` | App mode foreground service holding a partial wake lock while armed, so the face down sensors keep working. |
| `FaceDownDetector` + `FlipFilter` | Face down = accelerometer z below -9.0 m/s² for 500 ms with the proximity sensor near. Face up = z above -6.0 m/s² for 500 ms. |
| `ShakeDetector` + `ShakeFilter` | While the timer is active, a low pass filter removes gravity from the accelerometer. A shake is 3 samples above 4 m/s² of movement within 500 ms while the phone is still face down, then 1 s of cooldown. Placing the phone down is ignored for the first second. |
| `GlyphRenderer` + `MatrixDigits` | Draws the digits with a 3 x 5 pixel font and sends them with `setAppMatrixFrame`. |
| `Stopwatch` | Pure state machine: IDLE, RUNNING, PAUSED. |

`Stopwatch`, `ShakeFilter`, `FlipFilter` and `MatrixDigits` have no Android dependencies and
are covered by unit tests in `app/src/test`.

## Still to do

* Test on the phone with the screen off and tune the thresholds in `FlipFilter` and `ShakeFilter`.
* Check how often the system lets an always on toy redraw. The toy draws every second, but if
  the phone only refreshes it on the once a minute AOD event, the seconds will not show.
