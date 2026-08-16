package me.mint.tntfusetimer.render;

import me.mint.tntfusetimer.config.ModConfig;
import me.mint.tntfusetimer.util.BedwarsDetector;
import me.mint.tntfusetimer.util.FuseCalibrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

/**
 * Draws the remaining fuse time above every primed TNT entity in range.
 */
public class FuseRenderer {

    /** Vanilla nametag scale (1 / 37.5). */
    private static final float NAMETAG_SCALE = 0.02666667F;

    private static final int COLOR_SAFE = 0x55FF55;
    private static final int COLOR_WARN = 0xFFAA00;
    private static final int COLOR_DANGER = 0xFF5555;
    private static final int COLOR_PLAIN = 0xFFFFFF;

    /** Exposed purely so the debug overlay can report how many timers are being drawn. */
    public static int visibleTntCount;

    /** True when the offset in use came from live calibration rather than the config value. */
    public static boolean isUsingCalibration() {
        return ModConfig.autoCalibrate
                && BedwarsDetector.isInBedwars()
                && FuseCalibrator.hasCalibration();
    }

    /**
     * The tick offset currently being applied to the raw fuse.
     *
     * <p>Calibration writes what it learns straight into {@code bedwarsTickOffset}, so
     * there is only ever one value to read regardless of where it came from.</p>
     */
    public static int resolveOffset() {
        return BedwarsDetector.isInBedwars() ? ModConfig.bedwarsTickOffset : 0;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.enabled || mc.theWorld == null || mc.thePlayer == null) {
            visibleTntCount = 0;
            return;
        }

        RenderManager renderManager = mc.getRenderManager();
        FontRenderer font = renderManager.getFontRenderer();
        if (font == null) {
            return;
        }

        float partialTicks = event.partialTicks;
        int offset = resolveOffset();
        double maxDistanceSq = (double) ModConfig.maxDistance * ModConfig.maxDistance;

        int drawn = 0;
        // RenderWorldLastEvent leaves the modelview matrix in world space with the camera
        // at the origin, so entity positions are drawn relative to the viewer.
        for (Object candidate : mc.theWorld.loadedEntityList) {
            if (!(candidate instanceof EntityTNTPrimed)) {
                continue;
            }
            EntityTNTPrimed tnt = (EntityTNTPrimed) candidate;

            double x = interpolate(tnt.lastTickPosX, tnt.posX, partialTicks) - renderManager.viewerPosX;
            double y = interpolate(tnt.lastTickPosY, tnt.posY, partialTicks) - renderManager.viewerPosY;
            double z = interpolate(tnt.lastTickPosZ, tnt.posZ, partialTicks) - renderManager.viewerPosZ;

            if (x * x + y * y + z * z > maxDistanceSq) {
                continue;
            }

            // Subtracting partialTicks makes the countdown run smoothly between ticks
            // instead of stepping in 1/20s jumps.
            float remaining = Math.max(0.0F, tnt.fuse + offset - partialTicks);
            drawn++;
            drawLabel(font, renderManager, format(remaining),
                    x, y + tnt.height + ModConfig.heightOffset, z, colorFor(remaining));
        }
        visibleTntCount = drawn;
    }

    private static double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    private static String format(float ticks) {
        float seconds = ticks / 20.0F;
        if (ModConfig.showDecimals) {
            return String.format(Locale.ROOT, "%.2f", Float.valueOf(seconds));
        }
        return String.valueOf((int) Math.ceil(seconds));
    }

    private static int colorFor(float ticks) {
        if (!ModConfig.colorByUrgency) {
            return COLOR_PLAIN;
        }
        if (ticks <= 20.0F) {
            return COLOR_DANGER;
        }
        if (ticks <= 40.0F) {
            return COLOR_WARN;
        }
        return COLOR_SAFE;
    }

    private static void drawLabel(FontRenderer font, RenderManager renderManager, String text,
                                  double x, double y, double z, int color) {
        int width = font.getStringWidth(text);
        int halfWidth = width / 2;
        float scale = NAMETAG_SCALE * ModConfig.textScale;
        boolean throughWalls = ModConfig.showThroughWalls;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);

        // Billboard the text towards the camera, matching vanilla nametag behaviour
        // (the third-person-front view has its pitch inverted).
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.options.thirdPersonView == 2
                ? -renderManager.playerViewX : renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        if (throughWalls) {
            GlStateManager.disableDepth();
        }
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (ModConfig.drawBackground) {
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer buffer = tessellator.getWorldRenderer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(-halfWidth - 1, -1.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            buffer.pos(-halfWidth - 1, 8.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            buffer.pos(halfWidth + 1, 8.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            buffer.pos(halfWidth + 1, -1.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }

        font.drawString(text, -halfWidth, 0, color, true);

        // Restore everything we touched; leaking GL state here corrupts later rendering.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        if (throughWalls) {
            GlStateManager.enableDepth();
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
