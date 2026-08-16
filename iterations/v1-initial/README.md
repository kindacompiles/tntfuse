# v1 — Initial implementation

The first working mod, built after confirming the Polyfrost template compiled and launched.

## Features

- Fuse timer drawn above primed TNT via `RenderWorldLastEvent`.
- Bed Wars detection (`*.hypixel.net` + `BED WARS` sidebar), re-checked once per second.
- Forge `Configuration` with a `GuiConfig` screen in the mod list.
- Status overlay: Hypixel, Bed Wars detected, offset applied, primed TNT count.
- `bedwarsTickOffset` default **`0`**.

## Build changes that made this possible

The template did **not** work out of the box. Two things had to be fixed first:

**OneConfig stripped.** The template's jar manifest set
`TweakClass: cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker` and shaded
`oneconfig-wrapper-launchwrapper`. Launching the unmodified template proved this downloads at
startup:

```
[OneConfigLoader] Downloading new version of OneConfig... (17480.328KB)
```

Incompatible with the no-network requirement, so the tweaker, the dependency, Mixin, and
DevAuth were all removed.

**Java 8 pinned for the client.** `runClient` died instantly:

```
java.lang.ClassCastException: class jdk.internal.loader.ClassLoaders$AppClassLoader
cannot be cast to class java.net.URLClassLoader
```

LaunchWrapper assumes a Java 8 classloader, but Loom inherited Gradle's JDK 17. Fixed with:

```kotlin
val java8Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}
tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(java8Launcher)
}
```

## The one compile error

`GlStateManager.glNormal3f` does not exist in 1.8.9 — it lives on `GL11`. Everything else
compiled first time, which incidentally validated `event.modID`, `event.partialTicks`,
`tnt.fuse`, the `RenderManager` fields, and the `IModGuiFactory`/`IConfigElement` signatures.

## Offset handling

```java
int offset = BedwarsDetector.isInBedwars() ? ModConfig.bedwarsTickOffset : 0;
int remaining = Math.max(0, tnt.fuse + offset);
```

Integer arithmetic, no partial-tick interpolation — the countdown stepped in 1/20s jumps.

## What went wrong

Detection worked perfectly. The timer was correct outside Bed Wars and wrong inside it,
because `bedwarsTickOffset` defaulted to `0` and nothing computed it. The mod was applying an
offset of zero and reporting exactly that. → **v2**
