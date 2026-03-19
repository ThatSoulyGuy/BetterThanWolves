package btw.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("betterthanwolves")
public class BTWForgeMod {

    public static final String MOD_ID = "betterthanwolves";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public BTWForgeMod() {
        LOGGER.info("Better Than Wolves Forge module loading...");

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegister);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onAttributeCreation);
        MinecraftForge.EVENT_BUS.register(this);

        // Register network channel early so both sides agree on the protocol
        // before any packets are sent. Must happen during mod construction
        // (before FMLCommonSetupEvent) so the channel exists when the server
        // starts sending penalty sync packets.
        BTWNetwork.register();
    }

    /**
     * RegisterEvent fires while registries are still open.
     * We initialize BTW here AND register ProxyBlocks/Items.
     *
     * This fires once per registry type. We initialize FC during the BLOCKS
     * phase, then register proxy items during the ITEMS phase.
     */
    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.BLOCKS)) {
            LOGGER.info("Better Than Wolves: Initializing legacy systems (during RegisterEvent/BLOCKS)...");
            BTWLifecycle.initialize();
            LOGGER.info("Better Than Wolves: Registering proxy blocks...");
            BTWRegistration.registerAllBTWContent(event);
            LOGGER.info("Better Than Wolves: Block registration complete.");
        } else if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS)) {
            LOGGER.info("Better Than Wolves: Registering proxy items...");
            BTWRegistration.registerAllBTWContent(event);
            LOGGER.info("Better Than Wolves: Item registration complete.");
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // BTW init already done during RegisterEvent
        LOGGER.info("Better Than Wolves: FMLCommonSetupEvent (post-init).");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Better Than Wolves: Server starting...");
        BTWLifecycle.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Better Than Wolves: Server stopping...");
    }

    /**
     * Registers entity attributes for all BTW living entity types.
     * Forge requires this for any LivingEntity subclass.
     */
    private void onAttributeCreation(EntityAttributeCreationEvent event) {
        LOGGER.info("Better Than Wolves: Registering entity attributes...");
        try {
            BTWEntityRegistration.registerAttributes(event);
        } catch (Exception e) {
            LOGGER.error("Failed to register BTW entity attributes", e);
        }
    }
}
