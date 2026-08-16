package me.mint.tntfusetimer.util;

import me.mint.tntfusetimer.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.util.Locale;

/**
 * Decides whether the player is currently in a Hypixel Bed Wars game.
 *
 * <p>Detection is purely local: the server address the client already connected to,
 * plus the sidebar scoreboard the server already sent us. Nothing is requested over
 * the network.</p>
 */
public final class BedwarsDetector {

    /** Scoreboard display slot 1 is the sidebar. */
    private static final int DISPLAY_SLOT_SIDEBAR = 1;

    /** Re-evaluate about once a second instead of every frame. */
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private static int ticksSinceRefresh = REFRESH_INTERVAL_TICKS;
    private static boolean onHypixel;
    private static boolean bedwarsScoreboard;

    private BedwarsDetector() {
    }

    /** True if connected to a hypixel.net address. */
    public static boolean isOnHypixel() {
        return onHypixel;
    }

    /** True if the sidebar currently looks like a Bed Wars game. */
    public static boolean isBedwarsScoreboard() {
        return bedwarsScoreboard;
    }

    /** True if the Bed Wars tick offset should currently be applied. */
    public static boolean isInBedwars() {
        return ModConfig.forceBedwarsMode || (onHypixel && bedwarsScoreboard);
    }

    public static void tick() {
        if (++ticksSinceRefresh < REFRESH_INTERVAL_TICKS) {
            return;
        }
        ticksSinceRefresh = 0;

        Minecraft mc = Minecraft.getMinecraft();
        onHypixel = detectHypixel(mc);
        // Only trust the scoreboard once we know we're on Hypixel, so a custom server
        // with a "BED WARS" sidebar can't silently switch the offset on.
        bedwarsScoreboard = onHypixel && detectBedwarsScoreboard(mc);
    }

    private static boolean detectHypixel(Minecraft mc) {
        if (mc.isSingleplayer()) {
            return false;
        }
        ServerData data = mc.getCurrentServerData();
        if (data == null || data.serverIP == null) {
            return false;
        }
        String host = data.serverIP.toLowerCase(Locale.ROOT).trim();
        int portSeparator = host.indexOf(':');
        if (portSeparator != -1) {
            host = host.substring(0, portSeparator);
        }
        // Matches hypixel.net and subdomains such as mc.hypixel.net, but not
        // lookalikes like nothypixel.net.
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }

    private static boolean detectBedwarsScoreboard(Minecraft mc) {
        if (mc.theWorld == null) {
            return false;
        }
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return false;
        }
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(DISPLAY_SLOT_SIDEBAR);
        if (objective == null) {
            return false;
        }
        String title = EnumChatFormatting.getTextWithoutFormattingCodes(objective.getDisplayName());
        if (title == null) {
            return false;
        }
        // Hypixel's Bed Wars sidebar title reads "BED WARS", sometimes with an
        // event suffix appended during special modes.
        return title.toUpperCase(Locale.ROOT).contains("BED WARS");
    }
}
