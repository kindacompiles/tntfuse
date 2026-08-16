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

    public static void sync() {
        config.addCustomCategoryComment(CATEGORY_GENERAL, "Core behaviour of the fuse timer.");
        config.addCustomCategoryComment(CATEGORY_DISPLAY, "How the timer text is drawn.");
        config.addCustomCategoryComment(CATEGORY_BEDWARS, "Hypixel Bed Wars specific adjustments.");

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
                        + "Hypixel detonates it early, leaving roughly 28 ticks still on the clock.\n"
                        + "The default of -28 cancels that out so the timer reaches 0.00 at detonation.\n"
                        + "The real value drifts between 27 and 28, so enable autoCalibrate if you want it\n"
                        + "measured rather than assumed. Negative values shorten the displayed time.");
        forceBedwarsMode = config.getBoolean("forceBedwarsMode", CATEGORY_BEDWARS, false,
                "Always apply the Bed Wars offset, even when no Bed Wars game is detected.\n"
                        + "Useful on servers running a Bed Wars clone, or to verify the offset.");
        autoCalibrate = config.getBoolean("autoCalibrate", CATEGORY_BEDWARS, false,
                "Work the offset out by watching what the fuse reads when TNT actually detonates,\n"
                        + "instead of using bedwarsTickOffset. Needs 3 detonations before it takes over,\n"
                        + "and keeps a rolling median of the last 20. Measurements are in-memory only and\n"
                        + "reset when the game closes; nothing is saved or sent anywhere.");

        if (config.hasChanged()) {
            config.save();
        }
    }
}
