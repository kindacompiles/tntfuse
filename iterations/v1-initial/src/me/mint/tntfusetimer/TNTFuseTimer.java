package me.mint.tntfusetimer;

import me.mint.tntfusetimer.config.ModConfig;
import me.mint.tntfusetimer.render.DebugOverlay;
import me.mint.tntfusetimer.render.FuseRenderer;
import me.mint.tntfusetimer.util.BedwarsDetector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Client-side fuse timer for primed TNT.
 *
 * <p>This mod makes no network requests of any kind and pulls in no third-party
 * libraries; configuration and the settings screen are both provided by Forge.</p>
 */
@Mod(
        modid = TNTFuseTimer.MODID,
        name = TNTFuseTimer.NAME,
        version = TNTFuseTimer.VERSION,
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]",
        guiFactory = "me.mint.tntfusetimer.config.ConfigGuiFactory"
)
public class TNTFuseTimer {

    // Replaced at build time by blossom from the values in `gradle.properties`.
    public static final String MODID = "@ID@";
    public static final String NAME = "@NAME@";
    public static final String VERSION = "@VER@";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());

        // RenderWorldLastEvent and RenderGameOverlayEvent are posted on the Forge bus...
        MinecraftForge.EVENT_BUS.register(new FuseRenderer());
        MinecraftForge.EVENT_BUS.register(new DebugOverlay());

        // ...but in 1.8.9 TickEvent and ConfigChangedEvent are posted on the FML bus,
        // so this handler has to be registered separately. Registering it on the Forge
        // bus instead is the classic reason these two events silently never fire.
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BedwarsDetector.tick();
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (MODID.equals(event.modID)) {
            ModConfig.sync();
        }
    }
}
