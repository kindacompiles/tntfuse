# v5 — Honest overlay + zero-sample guard

Two bugs found in testing, both caused by `forceBedwarsMode`.

## Bug 1: the overlay lied

With `forceBedwarsMode` on and no Bed Wars game present, the overlay reported:

```
Bedwars game detected: true
```

`isInBedwars()` conflates "detected" with "forced," and the overlay was reporting the former
using the latter.

**Fix** — separate the two concepts:

```java
/** Real detection, ignoring any manual override. */
public static boolean isDetected() {
    return onHypixel && bedwarsScoreboard;
}

public static boolean isInBedwars() {
    return ModConfig.forceBedwarsMode || isDetected();
}
```

The overlay now reports `isDetected()` and appends the override state:

```
Bedwars game detected: true                      (green)
Bedwars game detected: false                     (red)
Bedwars game detected: false (manually enabled)  (amber)
```

Amber is deliberate — the offset *is* being applied, but nothing was detected. Neither green
nor red tells that truthfully.

## Bug 2: calibration wiped the offset to `0`

Worse, and only possible because v4 made writes persistent.

`forceBedwarsMode` makes `isInBedwars()` true everywhere, and that was also the gate for
calibration. So in a vanilla world the calibrator sampled ordinary TNT — which burns its full
fuse and expires at **0**. Median `0`, learned offset `-0`, written to disk. A perfectly good
`-28` destroyed silently, permanently, with no warning.

**Fix** — reject the degenerate sample. A reading near zero means the client's own countdown
ran out, which is the *absence* of an early server detonation, not a measurement of one:

```java
private static final int MIN_PLAUSIBLE_FUSE = 3;
```

This was chosen over gating calibration on `isDetected()`, because that would break the
legitimate case: a Bed Wars clone server, where `forceBedwarsMode` is the right tool *and*
early detonation genuinely happens. The filter kills the bad case without killing that one.