[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
# TNT Fuse Timer

A Minecraft **1.8.9 Forge** mod that draws the remaining fuse time above every
primed TNT entity, with a configurable tick offset applied automatically when it detects a
Hypixel Bed Wars game.

## Design constraints

- **No network requests of any kind.** The mod never opens a socket. Bed Wars detection uses
  only data the client already has: the address you connected to and the scoreboard the
  server already sent.
- **No third-party runtime dependencies.** No Essential, no OneConfig, no Mixin. Config
  storage and the settings screen both come from Forge itself
  (`net.minecraftforge.common.config.Configuration` and `fml.client.config.GuiConfig`).
- **No unofficial authentication.** DevAuth was removed from the build.

## Configuration

Settings live in `config/tntfusetimer.cfg` and are editable in-game via
**Mods → TNT Fuse Timer → Config**. Changes apply immediately on closing the screen.

### general
| Option | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Master on/off toggle. |
| `showDebugOverlay` | `true` | Show the live status readout (see below). |
| `maxDistance` | `64.0` | Only draw timers for TNT within this many blocks. |

### display
| Option | Default | Description |
| --- | --- | --- |
| `showThroughWalls` | `true` | Draw the timer even when the TNT is behind blocks. |
| `showDecimals` | `true` | `2.35` instead of `3`. |
| `colorByUrgency` | `true` | Green > 2s, orange > 1s, red ≤ 1s. |
| `drawBackground` | `true` | Translucent backing plate, like a nametag. |
| `textScale` | `1.0` | Size multiplier. |
| `heightOffset` | `0.5` | Extra height above the TNT, in blocks. |

### bedwars
| Option | Default | Description |
| --- | --- | --- |
| `bedwarsTickOffset` | `-28` | Ticks added to the displayed fuse during Bed Wars (20 ticks = 1s). |
| `forceBedwarsMode` | `false` | Always apply the offset, even outside a detected game. |
| `autoCalibrate` | `false` | Derive the offset from observed detonations instead of the value above. |

## Status overlay

While in a world, a readout is drawn in the top-left (hidden while F3 is open):

```
TNT Fuse Timer
Hypixel: true
Bedwars game detected: true
Offset applied: +5 ticks
Primed TNT: 3
```

`Bedwars game detected` is green for `true` and red for `false`, so you can confirm detection
is firing at the moment it matters. This is deliberately an in-game overlay rather than a line
in the config screen: that screen is usually opened from the main menu or pause screen, where
there is no scoreboard to read, so it would report `false` even when detection works.

## How Bed Wars detection works

Two independent conditions must both hold:

1. **Server address** — `Minecraft.getCurrentServerData().serverIP` resolves to `hypixel.net`
   or a `*.hypixel.net` subdomain. Lookalike domains such as `nothypixel.net` do not match.
2. **Sidebar scoreboard** — the objective in display slot 1 has a title containing `BED WARS`
   once formatting codes are stripped.

The result is cached and re-evaluated once per second rather than every frame.

`forceBedwarsMode` bypasses both checks, which is useful on Bed Wars clones or to verify your
offset without being in a real game.

## Why an offset is needed at all

The mod reads `EntityTNTPrimed.fuse` directly from the client's entity. The client spawns TNT
with the vanilla 80 tick fuse and counts it down locally, but Hypixel detonates Bed Wars TNT on
its own schedule — roughly 53 ticks — so the client still has about 27 ticks on the clock when
the server blows it up. That is why an uncorrected timer reads ~1.35s at the moment of
detonation.

`bedwarsTickOffset` defaults to `-28` to cancel this out, so the timer reaches `0.00` exactly
when the TNT explodes.

If it ever drifts (Hypixel changes the fuse, or a different mode uses a different one), measure
it again: note the seconds still showing at detonation, multiply by 20, and negate. Showing
`1.40` at detonation means `-28`.

### Auto-calibration

The true value is not actually constant — it drifts between 27 and 28 ticks. Setting
`autoCalibrate = true` measures it instead of assuming it.

Every detonation is a free measurement: whatever the fuse still read at the instant the entity
vanished *is* the correction. The mod keeps a rolling **median** of the last 20 detonations
(median, not mean, so a single stray reading cannot drag the value), and once it has 10 samples
it writes the result **back into `bedwarsTickOffset`**.

That write-back is the whole design:

- There is only ever one offset value, so `bedwarsTickOffset` is always the current best guess
  whether you set it or the mod did.
- It persists. The learned value is on disk, so the next launch starts correct and the
  warm-up only ever happens once.
- The manual value stays useful as the seed used while those first samples are gathered, which
  is why it isn't disabled when calibration is on.

The config file is only rewritten when the learned value actually changes, not on every
detonation. Sampling runs only while a Bed Wars game is detected.

The status overlay shows which mode is active:

```
Offset applied: -28 ticks (auto, n=12)
Offset applied: -28 ticks (manual, calibrating 1/3)
Offset applied: -28 ticks (manual)
```

**Enabling this will overwrite an offset you set by hand.** That is the point of enabling it,
but worth knowing.

#### The one rule that keeps the data clean

**Calibration runs only in a genuinely detected Bed Wars game** — never in singleplayer, and
never under `forceBedwarsMode`.

This single restriction is what makes it work, and it was learned the hard way. Earlier
versions sampled whenever the offset was being *applied*, which included `forceBedwarsMode` in
a vanilla world. That produced two separate corruptions:

- **Wiped to `0`** — vanilla TNT burns its full fuse and expires at `0`, which looks like
  "no offset needed."
- **Dragged to `-62`** — spamming TNT in creative and lighting it. The lag from a large
  multi-TNT explosion makes the client's local fuse fall behind the server, so removal packets
  arrive with time still on the clock. Those readings look perfectly plausible and are
  meaningless.

Both were fixed by gating on real detection rather than by filtering the readings afterwards.
An earlier attempt at filtering (plausibility windows, isolation rules, sanity bands) was
removed — it was five patches for a single root cause, and one of them was actively wrong: it
discarded readings of `0`, which would prevent ever learning that a mode genuinely uses the
vanilla fuse.

What remains is deliberately simple: median of the last 20 detonations, 10 required before
anything is written.

Note that Forge writes the default into `config/tntfusetimer.cfg` on first run, so **changing
the default in code does not update an existing config file** — edit it in the GUI or the file.

## Building

Requires **JDK 17** to run Gradle and **JDK 8** for the game. Both must be installed.

```bash
./gradlew build
```

Output jar:

```
versions/1.8.9-forge/build/libs/TNTFuseTimer-1.8.9-forge-1.0.0.jar
```

Use the jar **without** the `-dev` classifier — that one is unremapped and will not work in a
normal installation.

To launch a dev client:

```bash
./gradlew :1.8.9-forge:runClient
```

## Notes on the template

Based on [Polyfrost/OneConfigExampleMod](https://github.com/Polyfrost/OneConfigExampleMod)
(`legacy-forge` toolchain, Polyfrost Loom). Changes from upstream:

- Removed the OneConfig dependency, its shaded launchwrapper, and the
  `cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker` manifest entry. That loader
  downloads ~17MB at startup, which conflicts with the no-network requirement.
- Removed Mixin entirely. `EntityTNTPrimed.fuse` is `public` in 1.8.9, so no bytecode
  manipulation is needed.
- Removed DevAuth.
- **Pinned `JavaExec` tasks to a Java 8 toolchain.** Without this `runClient` fails instantly
  with `ClassCastException: AppClassLoader cannot be cast to URLClassLoader`, because
  LaunchWrapper assumes a Java 8 classloader. Gradle and Loom still run on 17.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
