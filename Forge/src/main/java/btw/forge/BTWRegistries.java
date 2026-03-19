package btw.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * BTW blocks/items are registered directly via Registry.register() in BTWRegistration,
 * NOT via DeferredRegister (which fires too early for BTW's init sequence).
 */
public class BTWRegistries {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Registries");

    public static void register(IEventBus modEventBus) {
        LOGGER.info("BTW registries: direct registration mode (no DeferredRegister).");
        // No DeferredRegister — BTW blocks/items are registered in BTWRegistration.registerAllBTWContent()
        // which runs during FMLCommonSetupEvent.enqueueWork() AFTER FC init completes.
    }
}
