# Glyph Stopwatch

A stopwatch for the Nothing Phone (4a) Pro that shows on the Glyph Matrix.

* Place the phone face down: the stopwatch starts.
* Shake the phone while it is still face down: pause (one short buzz).
* Shake again while paused: restart from 0:00 and keep counting (two quick buzzes).
* Lift the phone and shake it within 3 seconds: hold (one buzz). The time freezes at the moment
  you lifted it; place the phone back down and it carries on from there (one buzz). A hold ends
  on its own after 10 minutes.
* Lift the phone without shaking it: after 3 seconds the timer stops at the time you lifted it,
  resets, and the matrix turns off. Put it back down within the 3 seconds and it just carries on.

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
phone has been face down for 500 ms. No need to arm anything.

On the table, a jiggle counts as a shake; the detector ignores movement once the phone starts
to turn over, so picking it up is never taken as one. In the hand it takes a firm shake, well
above the movement of simply holding the phone. A lift hides the toy, but the app keeps
watching the motion sensor until the hold or stop is decided.

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
| `StopwatchController` | The rules: face down, shakes, the 3 second wait after a lift, and holds. Pure Kotlin. |
| `TimerSession` | Owns the motion sensors and a wake lock while the toy shows, app mode is armed or the timer is active. Applies the rules and redraws each second on the toy's matrix when it is showing, otherwise on the app's. |
| `StopwatchToy` | Glyph Toy service. Hands its matrix to the session while the system shows it. A Glyph Button long press pauses on phones that have one. |
| `TimerService` | App mode foreground service that keeps the session watching while armed. |
| `MotionSensors` + `FlipFilter` | Face down = accelerometer z below -9.0 m/s² for 500 ms with the proximity sensor near. Face up = z above -6.0 m/s² for 500 ms. |
| `ShakeFilter` | A low pass filter removes gravity. A shake is 3 samples within 500 ms above 4 m/s² of movement face down on the table, or above 10 m/s² in the hand, then 1 s of cooldown. Placing the phone down is ignored for 1 s, and the first 0.3 s after a lift. |
| `GlyphRenderer` + `MatrixDigits` | Draws the digits with a 3 x 5 pixel font and sends them with `setAppMatrixFrame`. |
| `Stopwatch` | Pure state machine: IDLE, RUNNING, PAUSED. |

`Stopwatch`, `StopwatchController`, `ShakeFilter`, `FlipFilter` and `MatrixDigits` have no Android dependencies and
are covered by unit tests in `app/src/test`.

## Still to do

* Test on the phone with the screen off and tune the thresholds in `FlipFilter` and `ShakeFilter`.
* Check how often the system lets an always on toy redraw. The toy draws every second, but if
  the phone only refreshes it on the once a minute AOD event, the seconds will not show.
