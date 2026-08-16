# v2 — Offset default `-27`

A one-line change driven by a measurement.

## The measurement

TNT was detonating while the timer still read **~1.34 seconds**:

```
1.34s x 20 = ~27 ticks
```

## Why that number exists

The client spawns `EntityTNTPrimed` with the vanilla 80-tick fuse and counts down locally.
Hypixel detonates Bed Wars TNT on its own schedule — roughly 53 ticks — and simply removes the
entity. So the client still has `80 - 53 = 27` ticks on its clock when the server blows it up.

The discrepancy is constant, which is why a constant offset is the right correction.

## Change

```java
// before
public static int bedwarsTickOffset = 0;
// after
public static int bedwarsTickOffset = -27;
```

## The gotcha that made this nearly pointless

Forge writes defaults into `config/tntfusetimer.cfg` on first run and **never rewrites an
existing key**. Anyone who had already launched v1 still had `bedwarsTickOffset=0` on disk, so
changing the default in code did nothing for them. The value had to be edited in the GUI.

This is a recurring trap with Forge's `Configuration` and it comes up again in v4.

## What went wrong

Nothing — but it raised the question of whether Sk1er's TNTTime derived this automatically,
which led to reading its source. → **v3**
