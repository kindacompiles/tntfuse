# v4 — Calibration writes back to config

A design change that resolved two questions at once.

## The questions

1. Should `autoCalibrate` disable the manual `bedwarsTickOffset` input?
2. Should a learned value persist across restarts?

## Why the input can't just be disabled

Manual is the **seed**. Calibration needs samples before it can produce anything, and during
that warm-up something has to supply the offset. Disabling the field leaves nothing to fall
back on.

(Mechanically it's also awkward — Forge's `GuiConfig` builds entries from the config file and
has no reactive "disable this entry when that one is true". It would need a custom
`IConfigElement`.)

## The change

Have calibration write its result **into `bedwarsTickOffset`** rather than hold a competing
value:

```java
private static void writeBackIfChanged() {
    if (!hasCalibration()) return;
    int learned = getLearnedOffset();
    if (learned != ModConfig.bedwarsTickOffset) {
        ModConfig.setBedwarsTickOffset(learned);
    }
}
```

```java
public static void setBedwarsTickOffset(int value) {
    bedwarsTickOffset = value;
    if (config == null) return;
    config.get(CATEGORY_BEDWARS, "bedwarsTickOffset", -28).set(value);
    config.save();
}
```

This collapsed the render path to a single source of truth:

```java
public static int resolveOffset() {
    return BedwarsDetector.isInBedwars() ? ModConfig.bedwarsTickOffset : 0;
}
```

## What it bought

- One value, wherever it came from.
- Persistence for free — it's already in `tntfusetimer.cfg`, so the 3-sample warm-up happens
  once, ever.
- The manual field stays meaningful as the seed, so nothing needs disabling.
- Written only when the median actually moves, so no config thrashing.

## What went wrong

Persistence turned a transient bad reading into a **permanent** one. Combined with
`forceBedwarsMode`, calibration could now silently overwrite a good value on disk. → **v5**
