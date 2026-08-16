package me.mint.tntfusetimer.render;

import me.mint.tntfusetimer.config.ModConfig;
import me.mint.tntfusetimer.util.BedwarsDetector;
import me.mint.tntfusetimer.util.FuseCalibrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Small status readout so it is obvious whether Bed Wars detection is actually firing.
 *
 * <p>This is drawn in-game rather than inside the config screen on purpose: the config
 * screen is normally opened from the main menu or pause screen, where there is no
 * scoreboard to read, so it would report "false" even when detection works fine.</p>
 */
public class DebugOverlay {

    private static final int LINE_HEIGHT = 10;
    private static final int COLOR_TITLE = 0xFFFF55;
    private static final int COLOR_TRUE = 0x55FF55;
    private static final int COLOR_FALSE = 0xFF5555;
    private static final int COLOR_FORCED = 0xFFAA00;
    private static final int COLOR_LABEL = 0xAAAAAA;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (!ModConfig.showDebugOverlay) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        // Stay out of the way of the F3 screen.
        if (mc.gameSettings.showDebugInfo) {
            return;
        }

        FontRenderer font = mc.fontRendererObj;
        int x = 4;
        int y = 4;

        font.drawStringWithShadow("TNT Fuse Timer", x, y, COLOR_TITLE);
        y += LINE_HEIGHT;

        y = drawFlag(font, "Hypixel", BedwarsDetector.isOnHypixel(), x, y);

        // Report what was genuinely detected, never what the override forced, otherwise
        // forceBedwarsMode makes this line claim a game was found when none was.
        boolean detected = BedwarsDetector.isDetected();
        boolean forced = !detected && BedwarsDetector.isInBedwars();
        font.drawStringWithShadow(
                "Bedwars game detected: " + detected + (forced ? " (manually enabled)" : ""),
                x, y, forced ? COLOR_FORCED : (detected ? COLOR_TRUE : COLOR_FALSE));
        y += LINE_HEIGHT;

        int offset = FuseRenderer.resolveOffset();
        String source;
        if (FuseRenderer.isUsingCalibration()) {
            source = " (auto, n=" + FuseCalibrator.getSampleCount() + ")";
        } else if (ModConfig.autoCalibrate && BedwarsDetector.isInBedwars()) {
            source = " (manual, calibrating " + FuseCalibrator.getSampleCount() + "/3)";
        } else {
            source = " (manual)";
        }
        font.drawStringWithShadow("Offset applied: " + (offset >= 0 ? "+" : "") + offset + " ticks" + source,
                x, y, COLOR_LABEL);
        y += LINE_HEIGHT;

        font.drawStringWithShadow("Primed TNT: " + FuseRenderer.visibleTntCount, x, y, COLOR_LABEL);
    }

    private static int drawFlag(FontRenderer font, String label, boolean value, int x, int y) {
        String text = label + ": " + value;
        font.drawStringWithShadow(text, x, y, value ? COLOR_TRUE : COLOR_FALSE);
        return y + LINE_HEIGHT;
    }
}
