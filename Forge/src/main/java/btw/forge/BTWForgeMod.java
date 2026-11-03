package btw.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
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

        // Register Global Loot Modifier codecs (used by #004 loot overrides
        // and any future loot-rebalance integrations). Must bind during
        // mod construction so Forge's loot-modifier loader sees the codec
        // before datapack scan runs.
        btw.forge.data.BTWLootModifiers.LOOT_MODIFIERS.register(modEventBus);

        // One SoundEvent per 1.5.2 sound name (assets/betterthanwolves/sounds.json), so FC's
        // legacy sound names play their 1.5.2 files (LegacySounds, audit S4). DeferredRegister
        // entries must exist before RegisterEvent fires, i.e. during mod construction.
        LegacySounds.register(modEventBus);

        // FCMOD-INTEGRATION(1.20.1): fc_particles: the BTW particle types (FC's white smoke, white cloud, small flame
        // and cinders, 1.5.2's snow-shovel puff); DeferredRegister entries must exist before RegisterEvent fires.
        FcParticleTypes.register(modEventBus);

        // FCMOD-INTEGRATION(1.20.1): fc_beacon: FC's beacon potions as MobEffects
        BTWMobEffects.register(modEventBus);

        // Register network channel early so both sides agree on the protocol
        // before any packets are sent. Must happen during mod construction
        // (before FMLCommonSetupEvent) so the channel exists when the server
        // starts sending penalty sync packets.
        BTWNetwork.register();

        // FCMOD-INTEGRATION(1.20.1): mod-compat: one guarded loader for optional mods; a missing or broken mod only switches its own integration off
        btw.forge.compat.BTWCompat.init(modEventBus);

        // FCMOD-INTEGRATION(1.20.1): fc_log_rules: 1.20.1 logs FC has no identity for follow FCBlockLog's rule (class F: the bark and stripped forms FC reads as log 17 or 165; class S: stems and modded logs). The FC machine hook is registered after BTWCompat.init, so an integration's own hook (Dynamic Trees #027) is asked first.
        ModernLogHooks.register();
        MinecraftForge.EVENT_BUS.register(ModernLogRules.class);

        // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: FC's saw cuts the newer woods' stairs and slabs and the bamboo blocks; after the log rule so Dynamic Trees and the log rule are asked first
        ModernWoodSawHooks.register();
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
        // Startup self-audit: catch registry/physics regressions at launch (renderer coverage
        // runs client-side after the render map is built; block speed-factor sanity here).
        event.enqueueWork(BridgeSelfAudit::runCommon);
        // Every literal 1.5.2 sound name FC plays must resolve to a legacy sound event.
        event.enqueueWork(SoundMapping::verifyCoverage);
        // FCMOD-INTEGRATION(1.20.1): stays-removed: the removed-id table must cover every FC stub/inert id and
        // resolve every BTW successor; FC init (RegisterEvent/BLOCKS) and proxy registration are done by now.
        event.enqueueWork(btw.forge.removed.RemovedContent::audit);
        // FCMOD-INTEGRATION(1.20.1): modern_dyes: the 1.20.1 dyes' FC colour twins, before RecipeBridge and JEI read
        // FC's recipe lists (INTEGRATIONS #038)
        event.enqueueWork(ModernSpellingRecipes::apply);
        // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: FC's sub-block recipes for wood types 5-8, after the modern-dye
        // twins so no shipped fc_recipe_<n> id moves, before RecipeBridge (ServerStarting) and JEI read FC's list
        event.enqueueWork(ModernWoodSubBlocks::addFcRecipes);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Better Than Wolves: Server starting...");
        BTWLifecycle.onServerStarting(event.getServer());
        BridgeSelfAudit.runServerStart();

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
     * Fires after stopServer() has saved and closed every level (STOPPING is too early -- the
     * final save comes after it). Releases the static legacy server and the bridge caches so
     * the next world never sees this one (triage RE2/RE3).
     */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        LOGGER.info("Better Than Wolves: Server stopped, releasing legacy server state...");
        BTWLifecycle.onServerStopped(event.getServer());
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
