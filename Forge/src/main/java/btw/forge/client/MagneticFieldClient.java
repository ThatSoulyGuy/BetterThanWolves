package btw.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/** The client half of magnetic_compass: the local player's compass follows the server's magnetic target. */
// FCMOD-INTEGRATION(1.20.1): magnetic_compass: the local player's compass points at 4.B's strongest magnetic point; lodestone compasses, item frames and other players keep 1.20.1's target
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = btw.forge.BTWForgeMod.MOD_ID, value = Dist.CLIENT)
public final class MagneticFieldClient {
    private MagneticFieldClient() {}

    private static volatile boolean synced;
    private static volatile GlobalPos target;

    /** MagneticTargetSync's handler (render thread, enqueueWork). */
    public static void accept(@Nullable GlobalPos t) {
        target = t;
        synced = true;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        synced = false;
        target = null;
    }

    /** The compass target: a lodestone compass keeps its lodestone; the local player's compass follows the synced
     *  magnetic point (null = 4.B's spin); item frames, other players and the first 40 ticks keep 1.20.1's spawn target. */
    @Nullable
    static GlobalPos targetFor(ClientLevel level, ItemStack stack, Entity holder) {
        if (CompassItem.isLodestoneCompass(stack)) return CompassItem.getLodestonePosition(stack.getOrCreateTag());
        if (synced && holder == Minecraft.getInstance().player) return target;
        return CompassItem.getSpawnPosition(level);
    }

    /** Replaces 1.20.1's compass angle (ItemProperties.java:167-169); FMLClientSetupEvent.enqueueWork. */
    public static void registerCompass() {
        ItemProperties.register(Items.COMPASS, new ResourceLocation("angle"),
                new CompassItemPropertyFunction(MagneticFieldClient::targetFor));
    }
}
