package shieldimprovements;

import shieldimprovements.config.ConfigHandler;
import shieldimprovements.proxy.CommonProxy;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ShieldImprovements.MOD_ID, name = ShieldImprovements.MOD_NAME, version = ShieldImprovements.VERSION, guiFactory = ShieldImprovements.GUI_FACTORY)
public class ShieldImprovements {

    public static final Logger LOGGER = LogManager.getLogger(ShieldImprovements.MOD_NAME);
    public static final String MOD_ID = "shieldimprovements";
    public static final String MOD_NAME = "ShieldImprovements";
    public static final String VERSION = "1.0.0";
    public static final String GUI_FACTORY = "shieldimprovements.gui.GuiFactory";

    public static final String PROXY = "shieldimprovements.proxy";
    @SidedProxy(clientSide = PROXY + ".ClientProxy", serverSide = PROXY + ".CommonProxy")
    public static CommonProxy proxy;

    /**
     * <a href="https://cleanroommc.com/wiki/forge-mod-development/event#overview">
     *     Take a look at how many FMLStateEvents you can listen to via the @Mod.EventHandler annotation here
     * </a>
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigHandler.init(new Configuration(event.getSuggestedConfigurationFile()));
    }
    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event)
    {
        proxy.registerHandlers();
    }

}
