# Iteration history

Each version driven by a specific failure found in testing (this folder exists because this project was not using git until v6).

## What's in each folder

| Version | Source | Jar | Source verified against jar |
| --- | --- | --- | --- |
| v1 | yes | yes | **identical** |
| v2 | no | — | jar was never kept |
| v3 | yes | yes | **identical** |
| v4 | no | yes | — |
| v5 | yes | yes | **identical** |
| v6 | yes | yes | live source, no reconstruction |

**The jars are original build artifacts.** They were recovered and identified by fingerprinting
their bytecode, not by trusting filenames — which was worth doing, since the file named
"improved" turned out to be v3 and "improved again" was v4.

| Marker in bytecode | Identifies |
| --- | --- |
| no `FuseCalibrator` class | v1 / v2 |
| `bedwarsTickOffset` default `0` vs `-27` | v1 vs v2 |
| `FuseCalibrator` but no `setBedwarsTickOffset` | v3 |
| `setBedwarsTickOffset`, no `isDetected` | v4 |
| `isDetected`, `MAX_PLAUSIBLE_FUSE` = 79 | v5 |
| `isDetected` gating in `TNTFuseTimer` | v6 |

**The source for v1, v3 and v5 is reconstructed, not recovered** — nothing was committed to git
during development. But each reconstruction was compiled and its every class, field and method
signature diffed against the original jar, and all three came back **identical**. So they are
provably the same shape as what actually ran, even though comment text and local variable names
cannot be verified this way.

v2 differs from v1 by one integer (`bedwarsTickOffset` default `0` → `-27`). v4 and v6 have
their original jars and are documented below, but no reconstructed source — v4 is v3 plus
`setBedwarsTickOffset`, and v6 is v5 with three constants changed.

| Version | Headline | What forced it |
| --- | --- | --- |
| [v1](v1-initial/) | First working mod | — |
| [v2](v2-offset-27/) | Offset default `-27` | Timer read 1.34s at detonation in Bed Wars |
| [v3](v3-smoothing-and-calibration/) | Partial-tick smoothing, auto-calibration | Read Sk1er's TNTTime source |
| [v4](v4-calibration-writeback/) | Calibration persists to config | Manual value couldn't be disabled, so unify them |
| [v5](v5-honest-overlay-and-zero-guard/) | Honest overlay, zero-sample guard | Overlay lied under force mode; calibration wiped offset to `0` |
| [v6](v6-simplified/) | Most of v5 removed | Root cause found: calibration running outside real games |

## The through-line

Every version after v1 exists because something was **silently wrong** rather than visibly
broken. 

## Constants that never changed

Fixed in v1 and correct throughout:

- No network requests, no OneConfig, no Essential, no Mixin, no DevAuth.
- Forge's own `Configuration` + `GuiConfig` for settings.
- `EntityTNTPrimed.fuse` read directly — it's `public` in 1.8.9, so no bytecode manipulation.
- Bed Wars detection = `*.hypixel.net` **and** a sidebar objective containing `BED WARS`.
- Gradle on JDK 17, game pinned to JDK 8.