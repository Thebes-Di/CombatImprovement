package shieldimprovements.gui;

import shieldimprovements.config.ConfigHandler;
import shieldimprovements.ShieldImprovements;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

public class ShieldImprovementsGuiConfig extends GuiConfig {
    public ShieldImprovementsGuiConfig(GuiScreen screen) {
        super(screen,getConfigElements(), ShieldImprovements.MOD_ID, false, false, ShieldImprovements.MOD_ID + "Config");

    }

    private static List<IConfigElement> getConfigElements()
    {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();

        for(int i = 0; i < ConfigHandler.categories.length; i++)
        {
            elements.add(new ConfigElement(ConfigHandler.config.getCategory(ConfigHandler.categories[i])));
        }
        return elements;
    }
}
