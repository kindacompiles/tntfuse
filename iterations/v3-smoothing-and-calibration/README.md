# v3 — Partial-tick smoothing + auto-calibration

Driven by reading [Sk1erLLC/TNTTime](https://github.com/Sk1erLLC/TNTTime).

## What Sk1er's mod actually does

The assumption was that it derived the offset automatically. It does not — it hardcodes it:

```java
// hypixel changes the fuse time in bedwars to explode around the 28th tick,
// which makes the value of the fuse starting timer presumably 52 instead of 80
// this can fluctuate between 27 and 28, but 28 seems to be more common, so we can sit with that instead
final int fuseTimer = this.playingBedwars ? tntPrimed.fuse - 28 : tntPrimed.fuse;
```

Three things came out of reading it:

1. **Independent corroboration.** A completely separate codebase landed on 27–28, matching the
   1.34s measurement from v2. Its Bed Wars check is also identical — `getObjectiveInDisplaySlot(1)`
   and `contains("BED WARS")`.
2. **It depends on Essential.** Its Hypixel check is `EssentialAPI.getMinecraftUtil().isHypixel()`.
   Load-bearing, and exactly the dependency ruled out here.
3. **It's a coremod.** Ships an `FMLLoadingPlugin` and bytecode-transforms `EntityTNTPrimed` /
   `RenderTNTPrimed`. This mod does the same job with `RenderWorldLastEvent` and no transformer.

## Change 1: partial-tick smoothing

Taken directly from their approach. Without it the countdown steps in 1/20s jumps:

```java
// before
int remaining = Math.max(0, tnt.fuse + offset);
// after
float remaining = Math.max(0.0F, tnt.fuse + offset - partialTicks);
```

`format()` and `colorFor()` moved to `float` throughout.

## Change 2: default `-27` → `-28`

Matching Sk1er's "28 seems to be more common" — a larger sample than one observation.

## Change 3: auto-calibration (new)

Since the constant provably fluctuates, measure it instead. Every detonation is a free
measurement: whatever the fuse read when the entity vanished *is* the correction.

`FuseCalibrator` tracked primed TNT per tick and, when one disappeared, recorded its last fuse
value.

```java
private static final int MIN_TRACKED_TICKS = 5;
private static final int MAX_SAMPLES = 20;
private static final int MIN_SAMPLES = 3;      // <-- becomes the v5 bug
```

Median rather than mean, so one stray sample couldn't drag the value. Samples were **in-memory
only**, and calibration ran only while Bed Wars was detected. `autoCalibrate` defaulted to off.

At this point the calibrated value competed with the config value:

```java
return isUsingCalibration() ? FuseCalibrator.getOffset() : ModConfig.bedwarsTickOffset;
```

## What went wrong

Nothing yet, but two design questions surfaced: should the manual input be disabled when auto
is on, and should the learned value survive a restart? → **v4**
