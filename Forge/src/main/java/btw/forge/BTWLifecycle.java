package btw.forge;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Manages the initialization order for BTW's legacy systems.
 *
 * BTW was designed for MC 1.5.2 where initialization happened in a specific order
 * during class loading. In Forge 1.20.1, we trigger the same sequence during
 * FMLCommonSetupEvent.enqueueWork().
 *
 * The FC code is on the runtime classpath unremapped, so it references
 * btw.api.* types directly. These resolve to the Api stubs at runtime,
 * with real implementations provided by Modern-Common (btw.modern.* extends btw.api.*)
 * and our Forge wrapper classes.
 *
 * ForgeGradle uses a module-based classloader (ModuleClassLoader) that only sees
 * classes declared in the mod's module layer. The FC JAR is on the system classpath
 * but not in the module layer, so we create a bridge classloader that:
 * - Loads FC classes from the JAR(s) in build/remapped/
 * - Delegates to the mod classloader for btw.api.*, btw.modern.*, and MC/Forge types
 */
public class BTWLifecycle {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Lifecycle");
    private static boolean initialized = false;
    private static ClassLoader fcClassLoader;

    /**
     * Master initialization -- called from FMLCommonSetupEvent.enqueueWork().
     * Must run on the main thread (enqueueWork ensures this).
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;

        LOGGER.info("Step 1: Initializing ID mapping service...");
        btw.modern.IDMappingService.initialize();

        LOGGER.info("Step 2: Initializing vanilla block references...");
        btw.modern.Block.initializeVanillaBlocks();

        LOGGER.info("Step 2b: Initializing vanilla item references...");
        btw.modern.Item.initializeVanillaItems();

        LOGGER.info("Step 2c: Initializing vanilla potion references...");
        btw.modern.Potion.initializeVanillaPotions();

        LOGGER.info("Step 2d: Initializing vanilla enchantment references...");
        btw.modern.Enchantment.initializeVanillaEnchantments();

        LOGGER.info("Step 2e: Initializing vanilla biome references...");
        btw.modern.BiomeGenBase.initializeVanillaBiomes();

        LOGGER.info("Step 3: Loading FC classes (same classloader — no bridge needed)...");

        LOGGER.info("Step 4: Loading FCBetterThanWolves (triggers self-registration)...");
        try {
            // FC classes are in the mod's module layer (via the fc source set),
            // so they share the same classloader as btw.modern classes.
            Class<?> btwClass = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves");
            LOGGER.info("FCBetterThanWolves class loaded: {}", btwClass.getName());
        } catch (ClassNotFoundException e) {
            LOGGER.error("FCBetterThanWolves not found! Run :Forge:extractFcClasses first.", e);
            return;
        } catch (Throwable e) {
            LOGGER.error("Failed to load FCBetterThanWolves class", e);
            return;
        }

        LOGGER.info("Step 4b: Pre-creating FC vanilla replacement blocks...");
        try {
            // FC code expects certain vanilla blocks to be FC subclasses
            // (e.g., Block.pumpkinStem must be FCBlockStem).
            // Load these FC classes to trigger their constructors, which
            // populate Block.blocksList[] with the correct FC types.
            // FCBlockStem(int blockID, Block fruitBlock) — needs a dummy fruit block
            Class<?> stemClass = Class.forName("net.minecraft.src.btw.block.FCBlockStem");
            java.lang.reflect.Constructor<?> ctor = stemClass.getConstructor(int.class, btw.modern.Block.class);
            // Use pumpkin/melon as fruit blocks (already initialized)
            btw.modern.Block.pumpkinStem = (btw.modern.Block) ctor.newInstance(104, btw.modern.Block.pumpkin);
            btw.modern.Block.melonStem = (btw.modern.Block) ctor.newInstance(105, btw.modern.Block.melon);
            LOGGER.info("Pre-created FCBlockStem for pumpkinStem(104) and melonStem(105)");
        } catch (Exception e) {
            LOGGER.warn("Could not pre-create FC stem blocks: {}", e.getMessage());
        }

        LOGGER.info("Step 5: Calling FCAddOnHandler.InitializeMods()...");
        try {
            Class<?> handlerClass = Class.forName("net.minecraft.src.btw.core.FCAddOnHandler");
            Method initMods = handlerClass.getMethod("InitializeMods");
            initMods.invoke(null);
            LOGGER.info("FCAddOnHandler.InitializeMods() completed successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to call FCAddOnHandler.InitializeMods()", e);
        }

        LOGGER.info("Step 6: Block/item registration happens via RegisterEvent (see BTWForgeMod).");
        // Registration moved to BTWForgeMod.onRegister() which calls BTWRegistration.registerAllBTWContent(event)
        try {
            // Nothing to do here — registration is handled by the caller
        } catch (Exception e) {
            LOGGER.error("Failed to register BTW content with Forge", e);
        }
    }

    /**
     * Creates a classloader that can find FC classes from the remapped JAR(s)
     * and delegates to the mod's classloader for btw.api.*, btw.modern.*, and
     * MC/Forge types.
     *
     * This bridge is needed because ForgeGradle's ModuleClassLoader only sees
     * classes declared in the mod's module layer (Forge, Modern-Common, Api source sets).
     * The FC JAR is on the system classpath but not in the module layer.
     */
    private static ClassLoader createFcClassLoader() {
        try {
            // Find the FC JAR(s) in the remapped directory.
            // At build time, remapFcCode places fc-classes-*.jar here.
            // At runtime, we locate it relative to this class's source location.
            File remappedDir = findRemappedDir();
            if (remappedDir == null || !remappedDir.isDirectory()) {
                LOGGER.error("Could not find remapped FC directory. Looked for build/remapped/");
                return null;
            }

            File[] jars = remappedDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars == null || jars.length == 0) {
                LOGGER.error("No JAR files found in {}", remappedDir.getAbsolutePath());
                return null;
            }

            URL[] urls = new URL[jars.length];
            for (int i = 0; i < jars.length; i++) {
                urls[i] = jars[i].toURI().toURL();
                LOGGER.info("  FC classpath entry: {}", jars[i].getName());
            }

            // Parent = the mod classloader (can resolve btw.api.*, btw.modern.*, MC types)
            ClassLoader modClassLoader = BTWLifecycle.class.getClassLoader();
            return new URLClassLoader(urls, modClassLoader);
        } catch (Exception e) {
            LOGGER.error("Failed to create FC classloader", e);
            return null;
        }
    }

    /**
     * Locates the build/remapped directory that contains the FC JAR.
     *
     * ForgeGradle's module classloader uses 'union:' URIs for code sources,
     * so we cannot rely on ProtectionDomain.getCodeSource(). Instead, we scan
     * the system classpath for the FC JAR path, or navigate from the working
     * directory (which ForgeGradle sets to Forge/run/server).
     */
    private static File findRemappedDir() {
        try {
            // Strategy 1: Scan the java.class.path for entries containing "remapped"
            String cp = System.getProperty("java.class.path", "");
            for (String entry : cp.split(File.pathSeparator)) {
                if (entry.contains("remapped") && entry.endsWith(".jar")) {
                    File jarFile = new File(entry);
                    if (jarFile.exists()) {
                        File dir = jarFile.getParentFile();
                        LOGGER.info("Found remapped dir from classpath: {}",
                            dir.getAbsolutePath());
                        return dir;
                    }
                }
            }

            // Strategy 2: Navigate from working directory.
            // ForgeGradle sets workingDirectory to Forge/run/server (or Forge/run/client).
            // The build dir is at ../../build/remapped.
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                File fromWorkDir = new File(userDir, "../../build/remapped");
                if (fromWorkDir.isDirectory()) {
                    LOGGER.info("Found remapped dir (from workdir): {}",
                        fromWorkDir.getCanonicalPath());
                    return fromWorkDir.getCanonicalFile();
                }
            }

            // Strategy 3: Try to extract path from the code source URL even
            // if it uses a non-standard scheme (e.g., union:/)
            try {
                URL classUrl = BTWLifecycle.class.getProtectionDomain()
                        .getCodeSource().getLocation();
                String path = classUrl.getPath();
                // Strip any query/fragment and jar-internal path markers
                if (path.contains("!")) path = path.substring(0, path.indexOf('!'));
                File classDir = new File(path);
                File candidate = classDir;
                for (int i = 0; i < 6; i++) {
                    File remapped = new File(candidate, "remapped");
                    if (remapped.isDirectory()) {
                        LOGGER.info("Found remapped dir (from code source): {}",
                            remapped.getAbsolutePath());
                        return remapped;
                    }
                    candidate = candidate.getParentFile();
                    if (candidate == null) break;
                }
            } catch (Exception ignored) {
                // Code source parsing may fail for non-file URIs; that's OK.
            }

            LOGGER.warn("Could not locate remapped dir. "
                + "Working dir: {}, classpath entries with 'remapped': none found", userDir);
            return null;
        } catch (Exception e) {
            LOGGER.error("Error locating remapped directory", e);
            return null;
        }
    }

    /**
     * Called when the server starts -- wire up the MinecraftServer instance
     * so FC code can access it via MinecraftServer.getServer().
     */
    public static void onServerStarting(MinecraftServer server) {
        LOGGER.info("Wiring MinecraftServer for BTW legacy code...");

        // FC code calls btw.modern.MinecraftServer.getServer() to get the server instance.
        // We create a ForgeMinecraftServerWrapper (which IS-A btw.modern.MinecraftServer)
        // and register it via setInstance().
        try {
            ForgeMinecraftServerWrapper wrapper = new ForgeMinecraftServerWrapper(server);
            btw.modern.MinecraftServer.setInstance(wrapper);
            LOGGER.info("MinecraftServer instance wired successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to wire MinecraftServer instance", e);
        }

        // Wire up worlds
        wireWorlds(server);
    }

    /**
     * Populate the worldServers array on the btw.modern.MinecraftServer so that
     * FC code can access worlds via MinecraftServer.getServer().worldServers[].
     */
    private static void wireWorlds(MinecraftServer server) {
        LOGGER.info("Wiring world instances for BTW legacy code...");
        try {
            btw.modern.MinecraftServer btwServer = btw.modern.MinecraftServer.getServer();

            if (btwServer == null) {
                LOGGER.warn("btw.modern.MinecraftServer.getServer() returned null, cannot wire worlds.");
                return;
            }

            // Collect all ServerLevels from the real MC server
            java.util.List<net.minecraft.server.level.ServerLevel> levelList = new java.util.ArrayList<>();
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                levelList.add(level);
            }

            // Create ForgeWorldServerWrapper instances (extend btw.modern.WorldServer)
            // that delegate to the real MC ServerLevel for world operations.
            btw.modern.WorldServer[] worldServers = new btw.modern.WorldServer[levelList.size()];
            for (int i = 0; i < levelList.size(); i++) {
                worldServers[i] = new ForgeWorldServerWrapper(levelList.get(i));
                LOGGER.info("  Wrapped world {} -> index {}",
                    levelList.get(i).dimension().location(), i);
            }

            // Set the worldServers array on the btw server instance
            btwServer.worldServers = worldServers;

            LOGGER.info("World wiring complete: {} worlds registered.", levelList.size());
        } catch (Exception e) {
            LOGGER.error("Failed to wire world instances", e);
        }
    }
}
