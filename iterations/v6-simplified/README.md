# v6 — Simplification (current)

**Current version.** This one exists as real source.

Removes some of what v5 added. The lag from a large multi-TNT explosion makes
the client's local fuse fall behind the integrated server, so removal packets arrive with time
still on the clock — readings in the 3–50 window that look plausible and mean nothing.

Every bad value this feature ever produced — the `0` wipe, the `-62`, the four that slipped
through — came from calibration running **outside a real Bed Wars game**.

## The fix

One gate, in `TNTFuseTimer.onClientTick`:

```java
// Deliberately isDetected() and not isInBedwars(): forceBedwarsMode must never feed
// the calibrator.
if (ModConfig.autoCalibrate && BedwarsDetector.isDetected()) {
    FuseCalibrator.tick();
} else {
    FuseCalibrator.clearTracking();
}
```

`isUsingCalibration()` and the overlay's calibrating indicator were switched to `isDetected()`
to match.