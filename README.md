# Glyph Stopwatch

A stopwatch for the Nothing Phone (4a) Pro that shows on the Glyph Matrix.

* Place the phone face down: the stopwatch starts.
* Lift it: pause (one short buzz). Place it back down: it carries on from there (one short buzz).
  A pause in the hand ends on its own after 10 minutes.
* Shake it once: reset to 0:00 (two quick buzzes). Face down it keeps counting; in the hand it
  waits at 0:00 until placed down. A single shake takes effect after about 1.5 seconds, the
  time allowed for a second shake.
* Shake it twice (shake, stop, shake): turn off (one long buzz). The timer stops and the
  matrix goes dark. App mode disarms; the toy starts again the next time the phone is placed
  face down.

The 4a Pro matrix is a 13 x 13 grid with 137 LEDs in a circle (the corners have none), too
narrow for `mm:ss` on one line, so minutes sit above seconds. The digits use rows 1 to 11 and
columns 3 to 9, which are all inside the circle. From 100 minutes on it shows hours above minutes. While paused the digits flash.

## Build

Open the project in Android Studio (or run `./gradlew assembleDebug`). The first build downloads
Nothing's Glyph Matrix SDK into `app/libs/`, because its licence does not allow it to be
committed here.

## Two ways to run it

**Glyph Toy (recommended on the Phone (4a) Pro).** The 4a Pro only runs always on toys, which
the system shows through Flip to Glyph. The system can also show the toy at other times, so
while it is shown the toy checks the accelerometer itself: the stopwatch starts only once the
phone has been face down for 500 ms. No need to arm anything.

On the table, a jiggle counts as a shake; the detector ignores movement once the phone starts
to turn over, so picking it up is never taken as one. In the hand it takes a firm shake, well
above the movement of simply holding the phone. A lift hides the toy, but the app keeps
watching the motion sensor while the timer is paused.

**App mode.** Arm the timer in the app. It watches the accelerometer itself and draws with the
app's own Glyph Matrix access. Use this if you would rather keep Flip to Glyph off.

Both share one stopwatch, so if both are on they never run two timers. While the toy is
showing it draws the time, because the system puts toys above apps on the matrix.

## Battery

With the timer off, the app only listens to the proximity sensor, which on most phones wakes
the processor by itself when something covers it. So the processor sleeps until the phone is
placed face down; then the accelerometer checks for up to 2 seconds. While the timer is on,
the accelerometer runs at 25 Hz and a wake lock keeps the processor awake for the per second
redraw. Phones whose proximity and accelerometer cannot wake the processor fall back to a
slow accelerometer under a wake lock.

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
| `StopwatchController` | The rules: face down, lift, one shake and two shakes. Pure Kotlin. |
| `ShakeCounter` | Tells one shake from two with a 1.5 second window. Pure Kotlin. |
| `TimerSession` | Runs the sensors in stages (see Battery), applies the rules, and redraws each second on the toy's matrix when it is showing, otherwise on the app's. |
| `StopwatchToy` | Glyph Toy service. Hands its matrix to the session while the system shows it. A Glyph Button long press pauses on phones that have one. |
| `TimerService` | App mode foreground service that keeps the session watching while armed. |
| `MotionSensors` + `FlipFilter` | Face down = accelerometer z below -9.0 m/s² for 500 ms with the proximity sensor near. Face up = z above -6.0 m/s² for 500 ms. |
| `ShakeFilter` | A low pass filter removes gravity. A shake is 3 samples within 500 ms above 4 m/s² of movement face down on the table, or above 10 m/s² in the hand. The next shake counts only after 0.3 s of calm, so one long shake is one shake. Placing the phone down is ignored for 1 s, and the first 0.3 s after a lift. |
| `GlyphRenderer` + `MatrixDigits` | Draws the digits with a 3 x 5 pixel font and sends them with `setAppMatrixFrame`. |
| `Stopwatch` | Pure state machine: IDLE, RUNNING, PAUSED. |

`Stopwatch`, `StopwatchController`, `ShakeCounter`, `ShakeFilter`, `FlipFilter` and `MatrixDigits` have no Android dependencies and
are covered by unit tests in `app/src/test`.

## Still to do

* Test on the phone with the screen off and tune the thresholds in `FlipFilter` and `ShakeFilter`.
* Check how often the system lets an always on toy redraw. The toy draws every second, but if
  the phone only refreshes it on the once a minute AOD event, the seconds will not show.
