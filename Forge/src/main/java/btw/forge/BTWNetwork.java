package btw.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bridges BTW's legacy packet system to Forge's SimpleChannel networking.
 *
 * BTW has one custom packet: FCPacket166StartBlockHarvest.
 * In 1.5.2 this was registered via Packet.addIdClassMapping(166, ...).
 * In Forge 1.20.1, we use SimpleChannel.
 */
public class BTWNetwork {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Network");
    private static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel CHANNEL;

    public static void register() {
        LOGGER.info("Registering BTW network channel...");

        CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BTWForgeMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        // TODO: Register BTW packet types
        // CHANNEL.registerMessage(0, StartBlockHarvestPacket.class, ...);

        LOGGER.info("BTW network channel registered.");
    }
}
