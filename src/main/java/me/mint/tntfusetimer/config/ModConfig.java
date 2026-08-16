package me.mint.tntfusetimer.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Configuration backed by Forge's built-in {@link Configuration}.
 *
 * <p>Values are mirrored into static fields so the render path never touches disk
 * or does map lookups per frame. {@link #sync()} re-reads them after the settings
 * screen is closed.</p>
 */
public final class ModConfig {

    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_DISPLAY = "display";
    public static final String CATEGORY_BEDWARS = "bedwars";

    private static Configuration config;

    // -- general --
    public static boolean enabled = true;
    public static boolean showDebugOverlay = true;
    public static float maxDistance = 64.0F;

    // -- display --
    public static boolean showThroughWalls = true;
    public static boolean showDecimals = true;
    public static boolean colorByUrgency = true;
    public static boolean drawBackground = true;
    public static float textScale = 1.0F;
    public static float heightOffset = 0.5F;

    // -- bedwars --
    public static int bedwarsTickOffset = -28;
    public static boolean forceBedwarsMode = false;
    public static boolean autoCalibrate = false;

    private ModConfig() {
    }

    public static void init(File file) {
        if (config != null) {
            return;
        }
        config = new Configuration(file);
        config.load();
        sync();
    }

    public static Configuration getConfig() {
        return config;
    }

    /**
     * Updates the Bed Wars offset and persists it, so a value learned by calibration
     * survives a restart and there is no warm-up period on the next launch.
     *
     * <p>Only called when the learned value actually changes, so this does not
     * rewrite the config file on every detonation.</p>
     */
    public static void setBedwarsTickOffset(int value) {
        bedwarsTickOffset = value;
        if (config == null) {
            return;
        }
        config.get(CATEGORY_BEDWARS, "bedwarsTickOffset", -28).set(value);
        config.save();
    }

    public static void sync() {
        config.addCustomCategoryComment(CATEGORY_GENERAL, "Core behaviour of the fuse timer.");
        config.addCustomCategoryComment(CATEGORY_DISPLAY, "How the timer text is drawn.");
        config.addCustomCategoryComment(CATEGORY_BEDWARS,
                "Hypixel Bed Wars adjustments.\n"
                        + "Hypixel detonates Bed Wars TNT earlier than vanilla, so the client's fuse still\n"
                        + "has time left when it explodes. Recommended offset: -28.");

        enabled = config.getBoolean("enabled", CATEGORY_GENERAL, true,
                "Master toggle for the TNT fuse timer.");
        showDebugOverlay = config.getBoolean("showDebugOverlay", CATEGORY_GENERAL, true,
                "Show a small status overlay reporting whether a Hypixel Bed Wars game was detected.");
        maxDistance = config.getFloat("maxDistance", CATEGORY_GENERAL, 64.0F, 1.0F, 256.0F,
                "Only draw timers for TNT within this many blocks of the camera.");

        showThroughWalls = config.getBoolean("showThroughWalls", CATEGORY_DISPLAY, true,
                "Draw the timer even when the TNT is behind blocks.");
        showDecimals = config.getBoolean("showDecimals", CATEGORY_DISPLAY, true,
                "Show hundredths of a second (2.35) instead of whole seconds (3).");
        colorByUrgency = config.getBoolean("colorByUrgency", CATEGORY_DISPLAY, true,
                "Colour the text green/orange/red as the fuse runs down.");
        drawBackground = config.getBoolean("drawBackground", CATEGORY_DISPLAY, true,
                "Draw a translucent backing plate behind the text, like a nametag.");
        textScale = config.getFloat("textScale", CATEGORY_DISPLAY, 1.0F, 0.25F, 4.0F,
                "Size multiplier for the timer text.");
        heightOffset = config.getFloat("heightOffset", CATEGORY_DISPLAY, 0.5F, -2.0F, 4.0F,
                "Extra height above the TNT to draw the timer, in blocks.");

        bedwarsTickOffset = config.getInt("bedwarsTickOffset", CATEGORY_BEDWARS, -28, -80, 80,
                "Ticks added to the displayed fuse while in a Hypixel Bed Wars game (20 ticks = 1 second).\n"
                        + "The client spawns TNT with the vanilla 80 tick fuse and counts down locally, but\n"
                        + "Hypixel detonates it early, leaving roughly 28 ticks on the clock.\n"
                        + "Recommended value: -28 (the default). The true value drifts between 27 and 29.\n"
                        + "To measure it yourself, multiply the seconds still showing at detonation by 20\n"
                        + "and negate it. Negative values shorten the displayed time.\n"
                        + "If autoCalibrate is on, this value is maintained for you.");
        forceBedwarsMode = config.getBoolean("forceBedwarsMode", CATEGORY_BEDWARS, false,
                "Apply the Bed Wars offset even when no Bed Wars game is detected.\n"
                        + "For servers running a Bed Wars clone, or to check the offset yourself.\n"
                        + "The status overlay shows 'manually enabled' in amber while this is what is\n"
                        + "applying the offset. Auto-calibration never runs in this mode.");

        autoCalibrate = config.getBoolean("autoCalibrate", CATEGORY_BEDWARS, false,
                "Maintain bedwarsTickOffset automatically by measuring what the fuse reads when\n"
                        + "TNT actually explodes. Off by default; -28 is correct for most players.\n"
                        + "Takes the median of the last 20 detonations and needs 10 before it writes.\n"
                        + "The result is saved, so it persists across restarts and only warms up once.\n"
                        + "Runs only in a genuinely detected Bed Wars game, never in singleplayer or\n"
                        + "under forceBedwarsMode, where vanilla TNT would produce meaningless readings.");
        if (config.hasChanged()) {
            config.save();
        }
    }
}
