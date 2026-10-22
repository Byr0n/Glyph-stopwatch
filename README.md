# Glyph Stopwatch

A stopwatch for the Nothing Phone (4a) Pro that shows on the Glyph Matrix.

* Place the phone face down: the stopwatch starts from 00.
* Lift it: the stopwatch stops and resets. Place it back down: it starts again from 00.
* Shake it while face down: turn off (one long buzz). The matrix goes dark and stays dark
  until the phone is next lifted and put down. App mode disarms.

The display shows the minutes as two centred digits, 00 to 99. To their right a bar fills
from the bottom over each minute, one pixel about every 6.7 seconds with the next pixel fading
in, so it moves every second; when it reaches the top the minutes tick over and it starts
again.

The 4a Pro matrix is a 13 x 13 grid with 137 LEDs in a circle (the corners have none). The
3 x 7 digits use rows 3 to 9 and columns 2 to 8, and the 9 pixel bar column 11, rows 2 to 10,
all inside the circle.

## Build

Open the project in Android Studio (or run `./gradlew assembleDebug`). The first build downloads
Nothing's Glyph Matrix SDK into `app/libs/`, because its licence does not allow it to be
committed here.

## Two ways to run it

**Glyph Toy (recommended on the Phone (4a) Pro).** The 4a Pro only runs always on toys, which
the system shows through Flip to Glyph. The system can also show the toy at other times, so
while it is shown the toy checks the accelerometer itself: the stopwatch starts only once the
phone has been face down for 500 ms. No need to arm anything.

A jiggle of the phone on the table counts as a shake; the detector ignores movement once
the phone starts to turn over, so picking it up is never taken as one.

**App mode.** Arm the timer in the app. It watches the accelerometer itself and draws with the
app's own Glyph Matrix access. Use this if you would rather keep Flip to Glyph off.

Both share one stopwatch, so if both are on they never run two timers. While the toy is
showing it draws the time, because the system puts toys above apps on the matrix.

## Battery

With the timer off, the app only listens to the proximity sensor, which on most phones wakes
the processor by itself when something covers it. So the processor sleeps until the phone is
placed face down; then the accelerometer checks for up to 2 seconds. While the timer is on,
the accelerometer runs at 25 Hz, watching for a lift or a shake, and a wake lock keeps the
processor awake for the per second redraw. Phones whose proximity and accelerometer cannot wake the processor fall back to a
slow accelerometer under a wake lock.

## Setup on the phone

1. Install the app and open it.
2. Tap **Exempt from battery optimisation** so the timer keeps running.
3. Optional: tap **Show test pattern** to check the matrix works.
4. For the toy: tap **Add to Glyph Toys** and add **Stopwatch**. Then go to Settings > Glyph
   Interface > Flip to Glyph > Always-on Glyph Toy and pick it.
5. For app mode instead: tap **Arm timer**. If Flip to Glyph is on with another toy, that toy
   will draw over the stopwatch, so pick Stopwatch there or turn Flip to Glyph off.
6. Place the phone face down.

## How it works

| Class | Job |
| --- | --- |
| `StopwatchController` | The rules: face down starts, lift resets, a shake turns off. Pure Kotlin. |
| `TimerSession` | Runs the sensors in stages (see Battery), applies the rules, and redraws each second on the toy's matrix when it is showing, otherwise on the app's. |
| `StopwatchToy` | Glyph Toy service. Hands its matrix to the session while the system shows it. On phones with a Glyph Button, a long press pauses, and the paused time flashes. |
| `TimerService` | App mode foreground service that keeps the session watching while armed. |
| `MotionSensors` + `FlipFilter` | Face down = accelerometer z below -9.0 m/s² for 500 ms with the proximity sensor near. Face up = z above -6.0 m/s² for 500 ms. |
| `ShakeFilter` | A low pass filter removes gravity. A shake is 3 samples within 500 ms above 4 m/s² of movement while the phone lies face down. Placing the phone down is ignored for 1 s. |
| `GlyphRenderer` + `MatrixDigits` | Draws the minutes with a 3 x 7 pixel font and the minute bar, and sends the frame to the toy's or the app's matrix. |
| `Stopwatch` | Pure state machine: IDLE, RUNNING, PAUSED. |

`Stopwatch`, `StopwatchController`, `ShakeFilter`, `FlipFilter` and `MatrixDigits` have no Android dependencies and
are covered by unit tests in `app/src/test`.

## Still to do

* Test on the phone with the screen off and tune the thresholds in `FlipFilter` and `ShakeFilter`.
* Check how often the system lets an always on toy redraw. The toy draws every second, but if
  the phone only refreshes it on the once a minute AOD event, the seconds will not show.
