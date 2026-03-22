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

        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onAttributeCreation);
        MinecraftForge.EVENT_BUS.register(this);

        // Register creative tab
        BTWCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);

        // Register menu types for FC container GUIs (hopper, soulforge, etc.)
        BTWMenuTypes.MENU_TYPES.register(modEventBus);

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

            // Generate proper block models from FC block bounds (before model loading)
            try {
                java.nio.file.Path resourcesRoot = findResourcesRoot();
                if (resourcesRoot != null) {
                    java.nio.file.Path modelsBlock = resourcesRoot.resolve("assets/betterthanwolves/models/block");
                    java.nio.file.Path texturesBlock = resourcesRoot.resolve("assets/betterthanwolves/textures/block");
                    BlockModelBridge.generateBlockModels(modelsBlock, texturesBlock);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to generate block models from FC bounds", e);
            }

            LOGGER.info("Better Than Wolves: Registering proxy blocks...");
            BTWRegistration.registerAllBTWContent(event);
            LOGGER.info("Better Than Wolves: Block registration complete.");
        } else if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS)) {
            LOGGER.info("Better Than Wolves: Registering proxy items...");
            BTWRegistration.registerAllBTWContent(event);
            LOGGER.info("Better Than Wolves: Item registration complete.");
        } else if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES)) {
            LOGGER.info("Better Than Wolves: Registering entity types...");
            BTWRegistration.registerAllBTWContent(event);
            LOGGER.info("Better Than Wolves: Entity type registration complete.");
        } else if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.BLOCK_ENTITY_TYPES)) {
            LOGGER.info("Better Than Wolves: Registering block entity types...");
            BTWRegistration.registerAllBTWContent(event);
            LOGGER.info("Better Than Wolves: Block entity type registration complete.");
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

        // Inject FC recipes into the MC recipe manager.
        // FC recipes take precedence — they're added after vanilla recipes load.
        LOGGER.info("Better Than Wolves: Injecting FC recipes...");
        RecipeBridge.injectRecipes(event.getServer());
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

    /**
     * Finds the resources root directory (build/resources/main) by scanning
     * the classpath for our pack.mcmeta file.
     */
    private java.nio.file.Path findResourcesRoot() {
        try {
            // Look for our pack.mcmeta on the classpath
            java.net.URL url = getClass().getClassLoader().getResource("pack.mcmeta");
            if (url != null && "file".equals(url.getProtocol())) {
                java.nio.file.Path packPath = java.nio.file.Paths.get(url.toURI());
                return packPath.getParent(); // The directory containing pack.mcmeta
            }
            // Fallback: try known dev paths
            java.nio.file.Path devPath = java.nio.file.Paths.get("../../src/main/resources");
            if (java.nio.file.Files.exists(devPath.resolve("pack.mcmeta"))) {
                return devPath;
            }
            java.nio.file.Path buildPath = java.nio.file.Paths.get("../../build/resources/main");
            if (java.nio.file.Files.exists(buildPath.resolve("pack.mcmeta"))) {
                return buildPath;
            }
        } catch (Exception e) {
            LOGGER.warn("Could not find resources root: {}", e.getMessage());
        }
        return null;
    }
}
