package shieldimprovements.proxy;

import shieldimprovements.event.EventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CommonProxy {

    public boolean isClient()
    {
        return false;
    }

    public void registerHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

}
