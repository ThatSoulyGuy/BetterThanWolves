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

        LOGGER.info("Step 4c: Replacing ConcreteBlocks with FC vanilla replacements...");
        replaceVanillaBlocksWithFc();

        LOGGER.info("Step 4d: Replacing vanilla items with FC item replacements...");
        replaceVanillaItemsWithFc();

        LOGGER.info("Step 5: Calling FCAddOnHandler.InitializeMods()...");
        try {
            Class<?> handlerClass = Class.forName("net.minecraft.src.btw.core.FCAddOnHandler");
            Method initMods = handlerClass.getMethod("InitializeMods");
            initMods.invoke(null);
            LOGGER.info("FCAddOnHandler.InitializeMods() completed successfully.");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            LOGGER.error("FCAddOnHandler.InitializeMods() threw inside FC code:", ite.getCause());
        } catch (Throwable e) {
            LOGGER.error("Failed to call FCAddOnHandler.InitializeMods()", e);
        }

        // Sanity check — if InitializeMods succeeded, fcItemWitchWart must
        // be non-null (it's assigned in FCBetterThanWolves.InstantiateModItems
        // which is called by Initialize() which is called by InitializeMods).
        // If it's still null, Initialize() silently failed partway through.
        try {
            Class<?> btwClass = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves");
            java.lang.reflect.Field f = btwClass.getField("fcItemWitchWart");
            Object v = f.get(null);
            LOGGER.info("[POST-INIT] FCBetterThanWolves.fcItemWitchWart = {}", v);
        } catch (Throwable t) {
            LOGGER.error("[POST-INIT] Could not read fcItemWitchWart", t);
        }

        LOGGER.info("Step 5b: Loading FC language files...");
        loadLanguageFiles();

        LOGGER.info("Step 6: Block/item registration happens via RegisterEvent (see BTWForgeMod).");
        // Registration moved to BTWForgeMod.onRegister() which calls BTWRegistration.registerAllBTWContent(event)
        try {
            // Nothing to do here — registration is handled by the caller
        } catch (Exception e) {
            LOGGER.error("Failed to register BTW content with Forge", e);
        }
    }

    /**
     * Loads vanilla MC 1.5.2 and FC language files into the StringTranslate
     * system so that FC's getItemDisplayName / getUnlocalizedName produce
     * proper localized names (e.g., "Arcane Scroll of Sharpness" instead
     * of "item.fcItemScrollArcane.name").
     */
    private static void loadLanguageFiles() {
        btw.modern.StringTranslate st = btw.modern.StringTranslate.getInstance();
        int before = st.GetTranslateTable().size();

        // Load vanilla MC 1.5.2 en_US.lang (item/block names from 1.5.2)
        try (java.io.InputStream vanillaStream =
                     BTWLifecycle.class.getResourceAsStream("/lang/en_US.lang")) {
            if (vanillaStream != null) {
                st.loadLangStream(vanillaStream);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not load vanilla en_US.lang: {}", e.getMessage());
        }

        // Load FC's BTW_en_US.lang (FC-specific item/block names)
        try (java.io.InputStream btwStream =
                     BTWLifecycle.class.getResourceAsStream("/lang/BTW_en_US.lang")) {
            if (btwStream != null) {
                st.loadLangStream(btwStream);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not load BTW_en_US.lang: {}", e.getMessage());
        }

        LOGGER.info("Loaded {} translation entries.", st.GetTranslateTable().size() - before);
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
     * Attempts to instantiate FC block classes for ALL vanilla block IDs (1-158).
     * In MCP, the patched vanilla Block.java creates FC subclasses directly
     * (e.g., {@code Block.stone = new FCBlockStone(1)}). Here we replicate
     * that by reflectively constructing FC blocks after FC classes are loaded.
     *
     * The FC constructors call {@code super(id, material)} which sets
     * {@code blocksList[id] = this}, replacing our ConcreteBlock placeholders.
     * This gives us proper FC behavior (GetMovementModifier, custom drops, etc.)
     * on vanilla world blocks.
     *
     * Constructor patterns from the patched Block.java:
     *   (int)              — most common
     *   (int, boolean)     — pistons, furnaces, repeaters, comparators, redstone ore/light/torch
     *   (int, Material)    — fluids, doors, glass, end portal, end stone
     *   (int, int)         — mushroom caps
     *   (int, String)      — mushroom blocks (brown/red)
     *   (int, Block)       — walls, stems (stems handled in step 4b)
     *   (int, Block, int)  — wood/brick/sandstone stairs
     *   ()                 — FCBlockCloth (sets its own block ID 35)
     *   (int, boolean, Material, String, String) — FCBlockPane
     *   (int, String, Material)                  — FCBlockFence (nether fence)
     *
     * Blocks 104/105 (pumpkinStem/melonStem) are handled in step 4b — skipped here.
     */
    private static void replaceVanillaBlocksWithFc() {
        int replaced = 0;
        int failed = 0;
        final String PKG = "net.minecraft.src.btw.block.";

        // ---------------------------------------------------------------
        // Group 1: Simple (int) constructor — FC class sets all properties
        // ---------------------------------------------------------------
        Object[][] simpleIntBlocks = {
            // { blockID, className, fieldName }
            {  1, "FCBlockStone",               "stone"             },
            {  2, "FCBlockGrass",               "grass"             },
            {  3, "FCBlockDirt",                "dirt"              },
            {  5, "FCBlockPlanks",              "planks"            },
            {  7, "FCBlockBedrock",             "bedrock"           },
            { 12, "FCBlockSand",                "sand"              },
            { 13, "FCBlockGravel",              "gravel"            },
            { 17, "FCBlockLog",                 "wood"              },
            { 18, "FCBlockLeaves",              "leaves"            },
            { 23, "FCBlockDispenserVanilla",    "dispenser"         },
            { 24, "FCBlockSandStone",           "sandStone"         },
            { 25, "FCBlockNote",                "music"             },
            { 30, "FCBlockWeb",                 "web"               },
            { 31, "FCBlockTallGrass",           "tallGrass"         },
            { 32, "FCBlockDeadBush",            "deadBush"          },
            { 34, "FCBlockPistonExtension",     "pistonExtension"   },
            { 36, "FCBlockPistonMoving",        "pistonMoving"      },
            // 37 and 38 moved to Group 2 (need setUnlocalizedName)
            { 46, "FCBlockPowderKeg",           "tnt"               },
            { 47, "FCBlockBookshelf",           "bookShelf"         },
            { 48, "FCBlockCobblestoneMossy",    "cobblestoneMossy"  },
            { 49, "FCBlockObsidian",            "obsidian"          },
            { 50, "FCBlockTorchLegacy",         "torchWood"         },
            { 52, "FCBlockMobSpawner",          "mobSpawner"        },
            { 58, "FCBlockWorkbench",           "workbench"         },
            { 60, "FCBlockFarmlandLegacyUnfertilized", "tilledField" },
            { 64, "FCBlockDoorWood",            "doorWood"          },
            { 65, "FCBlockLegacyLadder",        "ladder"            },
            { 66, "FCBlockRailRegular",         "rail"              },
            { 67, "FCBlockStairsCobblestone",   "stairsCobblestone" },
            { 70, "FCBlockPressurePlateStone",  "pressurePlateStone"},
            { 72, "FCBlockPressurePlatePlanks", "pressurePlatePlanks"},
            { 78, "FCBlockSnowCover",           "snow"              },
            { 80, "FCBlockSnowLegacy",          "blockSnow"         },
            { 81, "FCBlockCactus",              "cactus"            },
            { 84, "FCBlockJukebox",             "jukebox"           },
            { 85, "FCBlockFenceWood",           "fence"             },
            { 86, "FCBlockPumpkinCarved",       "pumpkin"           },
            { 87, "FCBlockNetherrack",          "netherrack"        },
            { 89, "FCBlockGlowStone",           "glowStone"         },
            { 90, "FCBlockPortal",              "portal"            },
            { 91, "FCBlockJackOLantern",        "pumpkinLantern"    },
            { 96, "FCBlockTrapDoor",            "trapdoor"          },
            { 97, "FCBlockSilverfish",          "silverfish"        },
            { 98, "FCBlockStoneBrick",          "stoneBrick"        },
            { 101, "FCBlockIronBars",           "fenceIron"         },
            { 106, "FCBlockVine",               "vine"              },
            { 107, "FCBlockFenceGate",          "fenceGate"         },
            { 109, "FCBlockStairsStoneBrick",   "stairsStoneBrick"  },
            { 110, "FCBlockMycelium",           "mycelium"          },
            { 112, "FCBlockNetherBrick",        "netherBrick"       },
            { 114, "FCBlockStairsNetherBrick",  "stairsNetherBrick" },
            { 131, "FCBlockTripWireSource",     "tripWireSource"    },
            { 144, "FCBlockSkull",              "skull"             },
            { 145, "FCBlockAnvil",              "anvil"             },
            { 153, "FCBlockNetherQuartzOre",    "oreNetherQuartz"   },
            { 155, "FCBlockBlackStone",         "blockNetherQuartz" },
            { 156, "FCBlockBlackStoneStairs",   "stairsNetherQuartz"},
        };

        for (Object[] entry : simpleIntBlocks) {
            int id = (int) entry[0];
            String className = PKG + entry[1];
            String fieldName = (String) entry[2];
            try {
                Class<?> clazz = Class.forName(className);
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
                btw.modern.Block b = (btw.modern.Block) ctor.newInstance(id);
                updateBlockStaticField(fieldName, b);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create {} for ID {}: {}", className, id, e.getMessage());
                failed++;
            }
        }

        // ---------------------------------------------------------------
        // Group 2: (int) constructor + chained property setters
        // ---------------------------------------------------------------

        // 4 — FCBlockCobblestone(4).setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("stonebrick").setCreativeTab(tabBlock)
        if (createSimpleWithSetters(PKG + "FCBlockCobblestone", 4, "cobblestone",
                b -> b.setHardness(2.0F).setResistance(10.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("stonebrick"))) replaced++; else failed++;

        // 6 — FCBlockSapling(6).setHardness(0.0F).SetBuoyant().setStepSound(soundGrassFootstep).setUnlocalizedName("sapling")
        if (createSimpleWithSetters(PKG + "FCBlockSapling", 6, "sapling",
                b -> b.setHardness(0.0F).SetBuoyant()
                        .setStepSound(btw.modern.Block.soundGrassFootstep)
                        .setUnlocalizedName("sapling"))) replaced++; else failed++;

        // 14 — FCBlockOreGold(14).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("oreGold")
        if (createSimpleWithSetters(PKG + "FCBlockOreGold", 14, "oreGold",
                b -> b.setHardness(3.0F).setResistance(5.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("oreGold"))) replaced++; else failed++;

        // 15 — FCBlockOreIron(15).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("oreIron")
        if (createSimpleWithSetters(PKG + "FCBlockOreIron", 15, "oreIron",
                b -> b.setHardness(3.0F).setResistance(5.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("oreIron"))) replaced++; else failed++;

        // 16 — FCBlockOreCoal(16).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("oreCoal")
        if (createSimpleWithSetters(PKG + "FCBlockOreCoal", 16, "oreCoal",
                b -> b.setHardness(3.0F).setResistance(5.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("oreCoal"))) replaced++; else failed++;

        // 21 — FCBlockOreLapis(21).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("oreLapis")
        if (createSimpleWithSetters(PKG + "FCBlockOreLapis", 21, "oreLapis",
                b -> b.setHardness(3.0F).setResistance(5.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("oreLapis"))) replaced++; else failed++;

        // 26 — FCBlockBed(26).setHardness(0.2F).SetBuoyant().setUnlocalizedName("bed").disableStats()
        if (createSimpleWithSetters(PKG + "FCBlockBed", 26, "bed",
                b -> b.setHardness(0.2F).SetBuoyant()
                        .setUnlocalizedName("bed").disableStats())) replaced++; else failed++;

        // 28 — FCBlockDetectorRail(28).setHardness(0.7F).setStepSound(soundMetalFootstep).setUnlocalizedName("detectorRail")
        if (createSimpleWithSetters(PKG + "FCBlockDetectorRail", 28, "railDetector",
                b -> b.setHardness(0.7F)
                        .setStepSound(btw.modern.Block.soundMetalFootstep)
                        .setUnlocalizedName("detectorRail"))) replaced++; else failed++;

        // 37 — FCBlockFlowerBlossom(37).setUnlocalizedName("flower")
        if (createSimpleWithSetters(PKG + "FCBlockFlowerBlossom", 37, "plantYellow",
                b -> b.setUnlocalizedName("flower"))) replaced++; else failed++;

        // 38 — FCBlockFlowerBlossom(38).setUnlocalizedName("rose")
        if (createSimpleWithSetters(PKG + "FCBlockFlowerBlossom", 38, "plantRed",
                b -> b.setUnlocalizedName("rose"))) replaced++; else failed++;

        // 41 — FCBlockOreStorage(41).setHardness(3.0F).setResistance(10.0F).setStepSound(soundMetalFootstep).setUnlocalizedName("blockGold")
        if (createSimpleWithSetters(PKG + "FCBlockOreStorage", 41, "blockGold",
                b -> b.setHardness(3.0F).setResistance(10.0F)
                        .setStepSound(btw.modern.Block.soundMetalFootstep)
                        .setUnlocalizedName("blockGold"))) replaced++; else failed++;

        // 42 — FCBlockOreStorage(42).setHardness(5.0F).setResistance(10.0F).setStepSound(soundMetalFootstep).setUnlocalizedName("blockIron")
        if (createSimpleWithSetters(PKG + "FCBlockOreStorage", 42, "blockIron",
                b -> b.setHardness(5.0F).setResistance(10.0F)
                        .setStepSound(btw.modern.Block.soundMetalFootstep)
                        .setUnlocalizedName("blockIron"))) replaced++; else failed++;

        // 45 — FCBlockBrick(45).setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("brick")
        if (createSimpleWithSetters(PKG + "FCBlockBrick", 45, "brick",
                b -> b.setHardness(2.0F).setResistance(10.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("brick"))) replaced++; else failed++;

        // 51 — FCBlockFire(51).setHardness(0.0F).setLightValue(1.0F).setStepSound(soundWoodFootstep).setUnlocalizedName("fire").disableStats()
        if (createSimpleWithSetters(PKG + "FCBlockFire", 51, "fire",
                b -> b.setHardness(0.0F).setLightValue(1.0F)
                        .setStepSound(btw.modern.Block.soundWoodFootstep)
                        .setUnlocalizedName("fire").disableStats())) replaced++; else failed++;

        // 54 — FCBlockChest(54).setCreativeTab(null)
        if (createSimpleWithSetters(PKG + "FCBlockChest", 54, "chest",
                b -> b.setCreativeTab(null))) replaced++; else failed++;

        // 55 — FCBlockRedstoneWire(55).setHardness(0.0F).setStepSound(soundPowderFootstep).setUnlocalizedName("redstoneDust").disableStats()
        if (createSimpleWithSetters(PKG + "FCBlockRedstoneWire", 55, "redstoneWire",
                b -> b.setHardness(0.0F)
                        .setStepSound(btw.modern.Block.soundPowderFootstep)
                        .setUnlocalizedName("redstoneDust").disableStats())) replaced++; else failed++;

        // 56 — FCBlockOreDiamond(56).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("oreDiamond")
        if (createSimpleWithSetters(PKG + "FCBlockOreDiamond", 56, "oreDiamond",
                b -> b.setHardness(3.0F).setResistance(5.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("oreDiamond"))) replaced++; else failed++;

        // 57 — FCBlockOreStorage(57).setHardness(5.0F).setResistance(10.0F).setStepSound(soundMetalFootstep).setUnlocalizedName("blockDiamond")
        if (createSimpleWithSetters(PKG + "FCBlockOreStorage", 57, "blockDiamond",
                b -> b.setHardness(5.0F).setResistance(10.0F)
                        .setStepSound(btw.modern.Block.soundMetalFootstep)
                        .setUnlocalizedName("blockDiamond"))) replaced++; else failed++;

        // 59 — FCBlockWheatLegacy(59).SetBuoyant().setUnlocalizedName("crops")
        if (createSimpleWithSetters(PKG + "FCBlockWheatLegacy", 59, "crops",
                b -> b.SetBuoyant().setUnlocalizedName("crops"))) replaced++; else failed++;

        // 63 — FCBlockSign(63, true) — handled below in (int, boolean) group
        // 68 — FCBlockSignWall(68) — simple int constructor but let me add it
        // Actually 68 is FCBlockSignWall which has no field in btw.modern.Block...
        // Let's add it anyway for blocksList replacement
        if (createSimpleWithSetters(PKG + "FCBlockSignWall", 68, "signWall",
                null)) replaced++; else failed++;

        // 69 — FCBlockLever(69).setHardness(0.5F).setStepSound(soundWoodFootstep).setUnlocalizedName("lever")
        if (createSimpleWithSetters(PKG + "FCBlockLever", 69, "lever",
                b -> b.setHardness(0.5F)
                        .setStepSound(btw.modern.Block.soundWoodFootstep)
                        .setUnlocalizedName("lever"))) replaced++; else failed++;

        // 77 — FCBlockButtonStone(77).setHardness(0.5F).setStepSound(soundStoneFootstep).setUnlocalizedName("button")
        if (createSimpleWithSetters(PKG + "FCBlockButtonStone", 77, "stoneButton",
                b -> b.setHardness(0.5F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("button"))) replaced++; else failed++;

        // 82 — FCBlockClay(82).setHardness(0.6F).setUnlocalizedName("clay")
        if (createSimpleWithSetters(PKG + "FCBlockClay", 82, "blockClay",
                b -> b.setHardness(0.6F).setUnlocalizedName("clay"))) replaced++; else failed++;

        // 83 — FCBlockReed(83).setHardness(0.0F).SetBuoyant().setStepSound(soundGrassFootstep).setUnlocalizedName("reeds").disableStats()
        if (createSimpleWithSetters(PKG + "FCBlockReed", 83, "reed",
                b -> b.setHardness(0.0F).SetBuoyant()
                        .setStepSound(btw.modern.Block.soundGrassFootstep)
                        .setUnlocalizedName("reeds").disableStats())) replaced++; else failed++;

        // 88 — FCBlockSoulSand(88).setHardness(0.5F).setStepSound(soundSandFootstep).setUnlocalizedName("hellsand")
        if (createSimpleWithSetters(PKG + "FCBlockSoulSand", 88, "slowSand",
                b -> b.setHardness(0.5F)
                        .setStepSound(btw.modern.Block.soundSandFootstep)
                        .setUnlocalizedName("hellsand"))) replaced++; else failed++;

        // 92 — FCBlockCake(92).setHardness(0.5F).setStepSound(soundClothFootstep).setUnlocalizedName("cake").disableStats()
        if (createSimpleWithSetters(PKG + "FCBlockCake", 92, "cake",
                b -> b.setHardness(0.5F)
                        .setStepSound(btw.modern.Block.soundClothFootstep)
                        .setUnlocalizedName("cake").disableStats())) replaced++; else failed++;

        // 103 — FCBlockMelon(103).setHardness(1.0F).setStepSound(soundWoodFootstep).setUnlocalizedName("melon")
        if (createSimpleWithSetters(PKG + "FCBlockMelon", 103, "melon",
                b -> b.setHardness(1.0F)
                        .setStepSound(btw.modern.Block.soundWoodFootstep)
                        .setUnlocalizedName("melon"))) replaced++; else failed++;

        // 108 — FCBlockStairsBrick(108).setUnlocalizedName("stairsBrick")
        if (createSimpleWithSetters(PKG + "FCBlockStairsBrick", 108, "stairsBrick",
                b -> b.setUnlocalizedName("stairsBrick"))) replaced++; else failed++;

        // 111 — FCBlockLilyPad(111).setHardness(0.0F).setStepSound(soundGrassFootstep).setUnlocalizedName("waterlily")
        if (createSimpleWithSetters(PKG + "FCBlockLilyPad", 111, "waterlily",
                b -> b.setHardness(0.0F)
                        .setStepSound(btw.modern.Block.soundGrassFootstep)
                        .setUnlocalizedName("waterlily"))) replaced++; else failed++;

        // 115 — FCBlockNetherStalk(115).setUnlocalizedName("netherStalk")
        if (createSimpleWithSetters(PKG + "FCBlockNetherStalk", 115, "netherStalk",
                b -> b.setUnlocalizedName("netherStalk"))) replaced++; else failed++;

        // 116 — FCBlockEnchantmentTable(116).setHardness(5.0F).setResistance(2000.0F).setUnlocalizedName("enchantmentTable")
        if (createSimpleWithSetters(PKG + "FCBlockEnchantmentTable", 116, "enchantmentTable",
                b -> b.setHardness(5.0F).setResistance(2000.0F)
                        .setUnlocalizedName("enchantmentTable"))) replaced++; else failed++;

        // 117 — FCBlockBrewingStand(117).setHardness(0.5F).setLightValue(0.125F).setUnlocalizedName("brewingStand")
        if (createSimpleWithSetters(PKG + "FCBlockBrewingStand", 117, "brewingStand",
                b -> b.setHardness(0.5F).setLightValue(0.125F)
                        .setUnlocalizedName("brewingStand"))) replaced++; else failed++;

        // 118 — FCBlockVanillaCauldron(118).setHardness(2.0F).setUnlocalizedName("cauldron")
        if (createSimpleWithSetters(PKG + "FCBlockVanillaCauldron", 118, "cauldron",
                b -> b.setHardness(2.0F).setUnlocalizedName("cauldron"))) replaced++; else failed++;

        // 120 — FCBlockEndPortalFrame(120).setStepSound(soundGlassFootstep).setLightValue(0.125F).setHardness(-1.0F).setUnlocalizedName("endPortalFrame").setResistance(6000000.0F)
        if (createSimpleWithSetters(PKG + "FCBlockEndPortalFrame", 120, "endPortalFrame",
                b -> b.setStepSound(btw.modern.Block.soundGlassFootstep)
                        .setLightValue(0.125F).setHardness(-1.0F)
                        .setUnlocalizedName("endPortalFrame")
                        .setResistance(6000000.0F))) replaced++; else failed++;

        // 122 — FCBlockDragonEgg(122).setHardness(3.0F).setResistance(15.0F).setStepSound(soundStoneFootstep).setLightValue(0.125F).setUnlocalizedName("dragonEgg")
        if (createSimpleWithSetters(PKG + "FCBlockDragonEgg", 122, "dragonEgg",
                b -> b.setHardness(3.0F).setResistance(15.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setLightValue(0.125F).setUnlocalizedName("dragonEgg"))) replaced++; else failed++;

        // 127 — FCBlockCocoa(127).setHardness(0.2F).setResistance(5.0F).SetBuoyant().setStepSound(soundWoodFootstep).setUnlocalizedName("cocoa")
        if (createSimpleWithSetters(PKG + "FCBlockCocoa", 127, "cocoaPlant",
                b -> b.setHardness(0.2F).setResistance(5.0F).SetBuoyant()
                        .setStepSound(btw.modern.Block.soundWoodFootstep)
                        .setUnlocalizedName("cocoa"))) replaced++; else failed++;

        // 128 — FCBlockStairsSandStone(128).setUnlocalizedName("stairsSandStone")
        if (createSimpleWithSetters(PKG + "FCBlockStairsSandStone", 128, "stairsSandStone",
                b -> b.setUnlocalizedName("stairsSandStone"))) replaced++; else failed++;

        // 129 — FCBlockOreEmerald(129).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("oreEmerald")
        if (createSimpleWithSetters(PKG + "FCBlockOreEmerald", 129, "oreEmerald",
                b -> b.setHardness(3.0F).setResistance(5.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("oreEmerald"))) replaced++; else failed++;

        // 130 — FCBlockEnderChest(130).setHardness(22.5F).setResistance(1000.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("enderChest").setLightValue(0.5F)
        if (createSimpleWithSetters(PKG + "FCBlockEnderChest", 130, "enderChest",
                b -> b.setHardness(22.5F).setResistance(1000.0F)
                        .setStepSound(btw.modern.Block.soundStoneFootstep)
                        .setUnlocalizedName("enderChest").setLightValue(0.5F))) replaced++; else failed++;

        // 132 — FCBlockTripWire(132).setUnlocalizedName("tripWire")
        if (createSimpleWithSetters(PKG + "FCBlockTripWire", 132, "tripWire",
                b -> b.setUnlocalizedName("tripWire"))) replaced++; else failed++;

        // 133 — FCBlockOreStorage(133).setHardness(5.0F).setResistance(10.0F).setStepSound(soundMetalFootstep).setUnlocalizedName("blockEmerald")
        if (createSimpleWithSetters(PKG + "FCBlockOreStorage", 133, "blockEmerald",
                b -> b.setHardness(5.0F).setResistance(10.0F)
                        .setStepSound(btw.modern.Block.soundMetalFootstep)
                        .setUnlocalizedName("blockEmerald"))) replaced++; else failed++;

        // 138 — FCBlockBeacon(138).setUnlocalizedName("beacon").setLightValue(1.0F)
        if (createSimpleWithSetters(PKG + "FCBlockBeacon", 138, "beacon",
                b -> b.setUnlocalizedName("beacon").setLightValue(1.0F))) replaced++; else failed++;

        // 140 — FCBlockFlowerPot(140).setHardness(0.0F).setStepSound(soundPowderFootstep).setUnlocalizedName("flowerPot")
        if (createSimpleWithSetters(PKG + "FCBlockFlowerPot", 140, "flowerPot",
                b -> b.setHardness(0.0F)
                        .setStepSound(btw.modern.Block.soundPowderFootstep)
                        .setUnlocalizedName("flowerPot"))) replaced++; else failed++;

        // 141 — FCBlockCarrot(141).setUnlocalizedName("carrots")
        if (createSimpleWithSetters(PKG + "FCBlockCarrot", 141, "carrot",
                b -> b.setUnlocalizedName("carrots"))) replaced++; else failed++;

        // 142 — FCBlockPotato(142).setUnlocalizedName("potatoes")
        if (createSimpleWithSetters(PKG + "FCBlockPotato", 142, "potato",
                b -> b.setUnlocalizedName("potatoes"))) replaced++; else failed++;

        // 143 — FCBlockButtonWood(143).setHardness(0.5F).setStepSound(soundWoodFootstep).setUnlocalizedName("button")
        if (createSimpleWithSetters(PKG + "FCBlockButtonWood", 143, "woodenButton",
                b -> b.setHardness(0.5F)
                        .setStepSound(btw.modern.Block.soundWoodFootstep)
                        .setUnlocalizedName("button"))) replaced++; else failed++;

        // 154 — FCBlockVanillaHopper(154) — simple (int)
        if (createSimpleWithSetters(PKG + "FCBlockVanillaHopper", 154, "hopperBlock",
                null)) replaced++; else failed++;

        // ---------------------------------------------------------------
        // Group 3: (int, boolean) constructor
        // ---------------------------------------------------------------

        // 29 — FCBlockPistonBase(29, true).setUnlocalizedName("pistonStickyBase")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockPistonBase");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(29, true);
            b.setUnlocalizedName("pistonStickyBase");
            updateBlockStaticField("pistonStickyBase", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockPistonBase(29,true): {}", e.getMessage());
            failed++;
        }

        // 33 — FCBlockPistonBase(33, false).setUnlocalizedName("pistonBase")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockPistonBase");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(33, false);
            b.setUnlocalizedName("pistonBase");
            updateBlockStaticField("pistonBase", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockPistonBase(33,false): {}", e.getMessage());
            failed++;
        }

        // 61 — FCBlockFurnace(61, false)
        // 62 — FCBlockFurnace(62, true)
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockFurnace");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block idle = (btw.modern.Block) ctor.newInstance(61, false);
            updateBlockStaticField("furnaceIdle", idle);
            btw.modern.Block burning = (btw.modern.Block) ctor.newInstance(62, true);
            updateBlockStaticField("furnaceBurning", burning);
            replaced += 2;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockFurnace: {}", e.getMessage());
            failed += 2;
        }

        // 63 — FCBlockSign(63, true) — (int, boolean)
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockSign");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(63, true);
            updateBlockStaticField("signPost", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockSign(63,true): {}", e.getMessage());
            failed++;
        }

        // 73 — FCBlockRedstoneOre(73, false).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("oreRedstone")
        // 74 — FCBlockRedstoneOre(74, true).setLightValue(0.625F).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("oreRedstone")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockRedstoneOre");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block ore = (btw.modern.Block) ctor.newInstance(73, false);
            ore.setHardness(3.0F).setResistance(5.0F)
                    .setStepSound(btw.modern.Block.soundStoneFootstep)
                    .setUnlocalizedName("oreRedstone");
            updateBlockStaticField("oreRedstone", ore);
            btw.modern.Block glowing = (btw.modern.Block) ctor.newInstance(74, true);
            glowing.setLightValue(0.625F).setHardness(3.0F).setResistance(5.0F)
                    .setStepSound(btw.modern.Block.soundStoneFootstep)
                    .setUnlocalizedName("oreRedstone");
            updateBlockStaticField("oreRedstoneGlowing", glowing);
            replaced += 2;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockRedstoneOre: {}", e.getMessage());
            failed += 2;
        }

        // 79 — FCBlockIce(79).setHardness(0.5F).SetBuoyant().setLightOpacity(3).setStepSound(soundGlassFootstep).setUnlocalizedName("ice")
        if (createSimpleWithSetters(PKG + "FCBlockIce", 79, "ice",
                b -> b.setHardness(0.5F).SetBuoyant()
                        .setLightOpacity(3)
                        .setStepSound(btw.modern.Block.soundGlassFootstep)
                        .setUnlocalizedName("ice"))) replaced++; else failed++;

        // 93 — FCBlockRedstoneRepeater(93, false)
        // 94 — FCBlockRedstoneRepeater(94, true)
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockRedstoneRepeater");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block repeaterIdle = (btw.modern.Block) ctor.newInstance(93, false);
            updateBlockStaticField("redstoneRepeaterIdle", repeaterIdle);
            btw.modern.Block repeaterActive = (btw.modern.Block) ctor.newInstance(94, true);
            updateBlockStaticField("redstoneRepeaterActive", repeaterActive);
            replaced += 2;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockRedstoneRepeater: {}", e.getMessage());
            failed += 2;
        }

        // 123 — FCBlockRedstoneLight(123, false).setHardness(0.3F).setStepSound(soundGlassFootstep).setUnlocalizedName("redstoneLight")
        // 124 — FCBlockRedstoneLight(124, true).setHardness(0.3F).setStepSound(soundGlassFootstep).setUnlocalizedName("redstoneLight")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockRedstoneLight");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block lampIdle = (btw.modern.Block) ctor.newInstance(123, false);
            lampIdle.setHardness(0.3F)
                    .setStepSound(btw.modern.Block.soundGlassFootstep)
                    .setUnlocalizedName("redstoneLight");
            updateBlockStaticField("redstoneLampIdle", lampIdle);
            btw.modern.Block lampActive = (btw.modern.Block) ctor.newInstance(124, true);
            lampActive.setHardness(0.3F)
                    .setStepSound(btw.modern.Block.soundGlassFootstep)
                    .setUnlocalizedName("redstoneLight");
            updateBlockStaticField("redstoneLampActive", lampActive);
            replaced += 2;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockRedstoneLight: {}", e.getMessage());
            failed += 2;
        }

        // 125 — FCBlockWoodSlab(125, true)
        // 126 — FCBlockWoodSlab(126, false)
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockWoodSlab");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block doubleSlab = (btw.modern.Block) ctor.newInstance(125, true);
            updateBlockStaticField("woodDoubleSlab", doubleSlab);
            btw.modern.Block singleSlab = (btw.modern.Block) ctor.newInstance(126, false);
            updateBlockStaticField("woodSingleSlab", singleSlab);
            replaced += 2;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockWoodSlab: {}", e.getMessage());
            failed += 2;
        }

        // 149 — FCBlockComparator(149, false).setHardness(0.0F).setStepSound(soundWoodFootstep).setUnlocalizedName("comparator").disableStats()
        // 150 — FCBlockComparator(150, true).setHardness(0.0F).setLightValue(0.625F).setStepSound(soundWoodFootstep).setUnlocalizedName("comparator").disableStats()
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockComparator");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block compIdle = (btw.modern.Block) ctor.newInstance(149, false);
            compIdle.setHardness(0.0F)
                    .setStepSound(btw.modern.Block.soundWoodFootstep)
                    .setUnlocalizedName("comparator").disableStats();
            updateBlockStaticField("redstoneComparatorIdle", compIdle);
            btw.modern.Block compActive = (btw.modern.Block) ctor.newInstance(150, true);
            compActive.setHardness(0.0F).setLightValue(0.625F)
                    .setStepSound(btw.modern.Block.soundWoodFootstep)
                    .setUnlocalizedName("comparator").disableStats();
            updateBlockStaticField("redstoneComparatorActive", compActive);
            replaced += 2;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockComparator: {}", e.getMessage());
            failed += 2;
        }

        // 151 — FCBlockDaylightDetector(151).setHardness(0.2F).setStepSound(soundWoodFootstep).setUnlocalizedName("daylightDetector")
        if (createSimpleWithSetters(PKG + "FCBlockDaylightDetector", 151, "daylightSensor",
                b -> b.setHardness(0.2F)
                        .setStepSound(btw.modern.Block.soundWoodFootstep)
                        .setUnlocalizedName("daylightDetector"))) replaced++; else failed++;

        // ---------------------------------------------------------------
        // Group 4: (int, Material) constructor
        // ---------------------------------------------------------------

        // 8 — FCBlockWaterFlowing(8, Material.water).setHardness(100.0F).setLightOpacity(3).setUnlocalizedName("water").disableStats()
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockWaterFlowing");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Material.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(8, btw.modern.Material.water);
            b.setHardness(100.0F).setLightOpacity(3).setUnlocalizedName("water").disableStats();
            updateBlockStaticField("waterMoving", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockWaterFlowing: {}", e.getMessage());
            failed++;
        }

        // 9 — FCBlockWaterStationary(9, Material.water).setHardness(100.0F).setLightOpacity(3).setUnlocalizedName("water").disableStats()
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockWaterStationary");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Material.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(9, btw.modern.Material.water);
            b.setHardness(100.0F).setLightOpacity(3).setUnlocalizedName("water").disableStats();
            updateBlockStaticField("waterStill", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockWaterStationary: {}", e.getMessage());
            failed++;
        }

        // 10 — FCBlockLavaFlowing(10, Material.lava).setHardness(0.0F).setLightValue(1.0F).setUnlocalizedName("lava").disableStats()
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockLavaFlowing");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Material.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(10, btw.modern.Material.lava);
            b.setHardness(0.0F).setLightValue(1.0F).setUnlocalizedName("lava").disableStats();
            updateBlockStaticField("lavaMoving", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockLavaFlowing: {}", e.getMessage());
            failed++;
        }

        // 11 — FCBlockLavaStationary(11, Material.lava).setHardness(100.0F).setLightValue(1.0F).setUnlocalizedName("lava").disableStats()
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockLavaStationary");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Material.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(11, btw.modern.Material.lava);
            b.setHardness(100.0F).setLightValue(1.0F).setUnlocalizedName("lava").disableStats();
            updateBlockStaticField("lavaStill", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockLavaStationary: {}", e.getMessage());
            failed++;
        }

        // 20 — FCBlockGlass(20, Material.glass, false).setHardness(0.3F).setStepSound(soundGlassFootstep).setUnlocalizedName("glass")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockGlass");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Material.class, boolean.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(20, btw.modern.Material.glass, false);
            b.setHardness(0.3F).setStepSound(btw.modern.Block.soundGlassFootstep)
                    .setUnlocalizedName("glass");
            updateBlockStaticField("glass", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockGlass: {}", e.getMessage());
            failed++;
        }

        // 71 — FCBlockDoor(71, Material.iron).setHardness(5.0F).setStepSound(soundMetalFootstep).setUnlocalizedName("doorIron").disableStats()
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockDoor");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Material.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(71, btw.modern.Material.iron);
            b.setHardness(5.0F).setStepSound(btw.modern.Block.soundMetalFootstep)
                    .setUnlocalizedName("doorIron").disableStats();
            updateBlockStaticField("doorIron", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockDoor: {}", e.getMessage());
            failed++;
        }

        // 119 — FCBlockEndPortal(119, Material.portal).setHardness(-1.0F).setResistance(6000000.0F)
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockEndPortal");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Material.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(119, btw.modern.Material.portal);
            b.setHardness(-1.0F).setResistance(6000000.0F);
            updateBlockStaticField("endPortal", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockEndPortal: {}", e.getMessage());
            failed++;
        }

        // 121 — FCBlockEndStone(121, Material.rock).setHardness(3.0F).setResistance(15.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("whiteStone")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockEndStone");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Material.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(121, btw.modern.Material.rock);
            b.setHardness(3.0F).setResistance(15.0F)
                    .setStepSound(btw.modern.Block.soundStoneFootstep)
                    .setUnlocalizedName("whiteStone");
            updateBlockStaticField("whiteStone", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockEndStone: {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 5: (int, int) constructor — mushroom caps
        // ---------------------------------------------------------------

        // 99 — FCBlockMushroomCapLegacy(99, 0)
        // 100 — FCBlockMushroomCapLegacy(100, 1)
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockMushroomCapLegacy");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class);
            btw.modern.Block brown = (btw.modern.Block) ctor.newInstance(99, 0);
            updateBlockStaticField("mushroomCapBrown", brown);
            btw.modern.Block red = (btw.modern.Block) ctor.newInstance(100, 1);
            updateBlockStaticField("mushroomCapRed", red);
            replaced += 2;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockMushroomCapLegacy: {}", e.getMessage());
            failed += 2;
        }

        // ---------------------------------------------------------------
        // Group 6: (int, String) constructor — mushroom blocks
        // ---------------------------------------------------------------

        // 39 — FCBlockMushroomBrown(39, "mushroom_brown").setHardness(0.0F).SetBuoyant().setStepSound(soundGrassFootstep).setUnlocalizedName("mushroom")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockMushroomBrown");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, String.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(39, "mushroom_brown");
            b.setHardness(0.0F).SetBuoyant()
                    .setStepSound(btw.modern.Block.soundGrassFootstep)
                    .setUnlocalizedName("mushroom");
            updateBlockStaticField("mushroomBrown", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockMushroomBrown: {}", e.getMessage());
            failed++;
        }

        // 40 — FCBlockMushroom(40, "mushroom_red").setHardness(0.0F).SetBuoyant().setStepSound(soundGrassFootstep).setUnlocalizedName("mushroom")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockMushroom");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, String.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(40, "mushroom_red");
            b.setHardness(0.0F).SetBuoyant()
                    .setStepSound(btw.modern.Block.soundGrassFootstep)
                    .setUnlocalizedName("mushroom");
            updateBlockStaticField("mushroomRed", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockMushroom: {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 7: () constructor — no args
        // ---------------------------------------------------------------

        // 35 — FCBlockCloth() — sets its own block ID 35
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockCloth");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor();
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance();
            updateBlockStaticField("cloth", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockCloth: {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 8: (int, Block, int) constructor — stairs
        // ---------------------------------------------------------------

        // 53 — FCBlockStairsWood(53, planks, 0).setUnlocalizedName("stairsWood")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockStairsWood");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Block.class, int.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(53, btw.modern.Block.planks, 0);
            b.setUnlocalizedName("stairsWood");
            updateBlockStaticField("stairsWoodOak", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockStairsWood(53): {}", e.getMessage());
            failed++;
        }

        // 134 — FCBlockStairsWood(134, planks, 1).setUnlocalizedName("stairsWoodSpruce")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockStairsWood");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Block.class, int.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(134, btw.modern.Block.planks, 1);
            b.setUnlocalizedName("stairsWoodSpruce");
            updateBlockStaticField("stairsWoodSpruce", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockStairsWood(134): {}", e.getMessage());
            failed++;
        }

        // 135 — FCBlockStairsWood(135, planks, 2).setUnlocalizedName("stairsWoodBirch")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockStairsWood");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Block.class, int.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(135, btw.modern.Block.planks, 2);
            b.setUnlocalizedName("stairsWoodBirch");
            updateBlockStaticField("stairsWoodBirch", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockStairsWood(135): {}", e.getMessage());
            failed++;
        }

        // 136 — FCBlockStairsWood(136, planks, 3).setUnlocalizedName("stairsWoodJungle")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockStairsWood");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Block.class, int.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(136, btw.modern.Block.planks, 3);
            b.setUnlocalizedName("stairsWoodJungle");
            updateBlockStaticField("stairsWoodJungle", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockStairsWood(136): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 9: (int, Block) constructor — wall
        // ---------------------------------------------------------------

        // 139 — FCBlockWall(139, cobblestone).setUnlocalizedName("cobbleWall")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockWall");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.Block.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(139, btw.modern.Block.cobblestone);
            b.setUnlocalizedName("cobbleWall");
            updateBlockStaticField("cobblestoneWall", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockWall: {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 10: (int, boolean) constructor — stone/redstone slabs
        // ---------------------------------------------------------------

        // 43 — FCBlockStep(43, true).SetPicksEffectiveOn().setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("stoneSlab")
        // 44 — FCBlockStep(44, false).SetPicksEffectiveOn().setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("stoneSlab")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockStep");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, boolean.class);
            btw.modern.Block doubleSlab = (btw.modern.Block) ctor.newInstance(43, true);
            doubleSlab.SetPicksEffectiveOn().setHardness(2.0F).setResistance(10.0F)
                    .setStepSound(btw.modern.Block.soundStoneFootstep)
                    .setUnlocalizedName("stoneSlab");
            updateBlockStaticField("stoneDoubleSlab", doubleSlab);
            btw.modern.Block singleSlab = (btw.modern.Block) ctor.newInstance(44, false);
            singleSlab.SetPicksEffectiveOn().setHardness(2.0F).setResistance(10.0F)
                    .setStepSound(btw.modern.Block.soundStoneFootstep)
                    .setUnlocalizedName("stoneSlab");
            updateBlockStaticField("stoneSingleSlab", singleSlab);
            replaced += 2;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockStep: {}", e.getMessage());
            failed += 2;
        }

        // 75 — BlockRedstoneTorch(75, false).setUnlocalizedName("notGate") — vanilla class, not FC
        // 76 — BlockRedstoneTorch(76, true).setLightValue(0.5F).setUnlocalizedName("notGate")
        // Note: These use vanilla BlockRedstoneTorch, not an FC class — skip FC replacement

        // ---------------------------------------------------------------
        // Group 11: (int, String, Material) constructor — fences
        // ---------------------------------------------------------------

        // 113 — FCBlockFence(113, "netherBrick", FCBetterThanWolves.fcMaterialNetherRock)
        //        .setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep).setUnlocalizedName("netherFence")
        try {
            // Get the Material from FCBetterThanWolves.fcMaterialNetherRock
            Class<?> btwClass = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves");
            java.lang.reflect.Field materialField = btwClass.getField("fcMaterialNetherRock");
            Object netherRockMaterial = materialField.get(null);

            Class<?> clazz = Class.forName(PKG + "FCBlockFence");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, String.class, btw.modern.Material.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(113, "netherBrick", netherRockMaterial);
            b.setHardness(2.0F).setResistance(10.0F)
                    .setStepSound(btw.modern.Block.soundStoneFootstep)
                    .setUnlocalizedName("netherFence");
            updateBlockStaticField("netherFence", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockFence(113): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 12: (int, String, String, Material, boolean) — glass pane
        // ---------------------------------------------------------------

        // 102 — FCBlockPane(102, "glass", "thinglass_top", Material.glass, false)
        //        .setHardness(0.3F).SetPicksEffectiveOn().setStepSound(soundGlassFootstep).setUnlocalizedName("thinGlass")
        try {
            Class<?> clazz = Class.forName(PKG + "FCBlockPane");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(
                    int.class, String.class, String.class, btw.modern.Material.class, boolean.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(
                    102, "glass", "thinglass_top", btw.modern.Material.glass, false);
            b.setHardness(0.3F).SetPicksEffectiveOn()
                    .setStepSound(btw.modern.Block.soundGlassFootstep)
                    .setUnlocalizedName("thinGlass");
            updateBlockStaticField("thinGlass", b);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCBlockPane: {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 13: Blocks that use Block(22, Material.rock) pattern
        //           but with FC SetPicksEffectiveOn() added
        // ---------------------------------------------------------------

        // 22 — Block(22, Material.rock).SetPicksEffectiveOn() ... — this is vanilla Block, not FC
        // The patched Block.java keeps this as vanilla Block, just adding SetPicksEffectiveOn().
        // We skip this since there is no FCBlock class for it.

        // 27 — BlockRailPowered(27).SetPicksEffectiveOn() ... — vanilla class with FC method
        // The patched Block.java uses vanilla BlockRailPowered with .SetPicksEffectiveOn() added.
        // No FC class replacement needed, but we apply SetPicksEffectiveOn if the block exists.
        try {
            btw.modern.Block railPowered = btw.modern.Block.railPowered;
            if (railPowered != null) {
                railPowered.SetPicksEffectiveOn().setHardness(0.7F)
                        .setStepSound(btw.modern.Block.soundMetalFootstep)
                        .setUnlocalizedName("goldenRail");
            }
        } catch (Exception e) {
            LOGGER.debug("  Could not update railPowered properties: {}", e.getMessage());
        }

        // ---------------------------------------------------------------
        // Group 14: Stub blocks (FCBlockStub) — blocks BTW disables
        // ---------------------------------------------------------------

        // 146 — FCBlockStub(146).setUnlocalizedName("chestTrap")
        // 147 — FCBlockStub(147).setUnlocalizedName("weightedPlate_light")
        // 148 — FCBlockStub(148).setUnlocalizedName("weightedPlate_heavy")
        // 157 — FCBlockStub(157).setUnlocalizedName("activatorRail")
        // 158 — FCBlockStub(158).setUnlocalizedName("dropper")
        int[][] stubBlocks = {
            {146, 0}, {147, 0}, {148, 0}, {157, 0}, {158, 0}
        };
        String[] stubNames = {"chestTrap", "weightedPlate_light", "weightedPlate_heavy", "activatorRail", "dropper"};
        String[] stubFields = {"chestTrapped", "pressurePlateGold", "pressurePlateIron", "railActivator", "dropper"};
        for (int i = 0; i < stubBlocks.length; i++) {
            try {
                Class<?> clazz = Class.forName(PKG + "FCBlockStub");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
                btw.modern.Block b = (btw.modern.Block) ctor.newInstance(stubBlocks[i][0]);
                b.setUnlocalizedName(stubNames[i]);
                updateBlockStaticField(stubFields[i], b);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create FCBlockStub({}): {}", stubBlocks[i][0], e.getMessage());
                failed++;
            }
        }

        // ---------------------------------------------------------------
        // Group 15: Remaining blocks with special properties from patched Block.java
        // ---------------------------------------------------------------

        // 152 — BlockPoweredOre(152) — vanilla class, but patched Block.java adds setLightValue(0.75F)
        // The patched line: (new BlockPoweredOre(152)).setHardness(5.0F).setResistance(10.0F)
        //                   .setStepSound(soundMetalFootstep).setUnlocalizedName("blockRedstone").setLightValue(0.75F)
        // No FC class, but we update the existing block's light value if it exists.
        try {
            btw.modern.Block blockRedstone = btw.modern.Block.blocksList[152];
            if (blockRedstone != null) {
                blockRedstone.setLightValue(0.75F);
            }
        } catch (Exception e) {
            LOGGER.debug("  Could not update blockRedstone properties: {}", e.getMessage());
        }

        LOGGER.info("  Replaced {} vanilla blocks with FC subclasses ({} failed)", replaced, failed);
    }

    /**
     * Helper: creates an FC block with a simple (int) constructor, applies optional
     * chained setters, and updates the static field on btw.modern.Block.
     *
     * @return true if successful, false if an exception occurred
     */
    private static boolean createSimpleWithSetters(String className, int id, String fieldName,
                                                   java.util.function.Consumer<btw.modern.Block> setters) {
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
            btw.modern.Block b = (btw.modern.Block) ctor.newInstance(id);
            if (setters != null) {
                setters.accept(b);
            }
            updateBlockStaticField(fieldName, b);
            return true;
        } catch (Exception e) {
            LOGGER.debug("  Could not create {} for ID {}: {}", className, id, e.getMessage());
            return false;
        }
    }

    /**
     * Updates the corresponding static field on {@link btw.modern.Block}.
     * For example, fieldName "stone" sets {@code Block.stone = block}.
     * Silently ignores fields that don't exist on btw.modern.Block.
     */
    private static void updateBlockStaticField(String fieldName, btw.modern.Block block) {
        if (fieldName == null) return;
        try {
            java.lang.reflect.Field field = btw.modern.Block.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, block);
        } catch (NoSuchFieldException e) {
            // Field doesn't exist on btw.modern.Block — that's OK, blocksList[id] is already set
        } catch (Exception e) {
            LOGGER.debug("  Could not update Block.{}: {}", fieldName, e.getMessage());
        }
    }

    /**
     * Attempts to instantiate FC item classes for ALL vanilla item IDs that are
     * replaced in the patched Item.java. In MCP, the patched vanilla Item.java
     * creates FC subclasses directly (e.g., {@code Item.axeIron = new FCItemAxe(2, EnumToolMaterial.IRON)}).
     * Here we replicate that by reflectively constructing FC items after FC classes are loaded.
     *
     * The FC constructors call {@code super(id)} which sets {@code itemsList[256 + id] = this},
     * replacing our placeholder items.
     *
     * Constructor patterns from the patched Item.java:
     *   (int, EnumToolMaterial)          — tools: axes, pickaxes, shovels, swords, hoes
     *   (int, EnumToolMaterial, int)     — pickaxeWood (3-arg variant with maxUses)
     *   (int)                            — most simple items (flintAndSteel, bow, arrow, etc.)
     *   (int, int)                       — mushroomSoup, minecarts, armorLeather
     *   (int, int, int)                  — armorChain, armorIron, armorDiamond, armorGold
     *   (int, int, float, boolean, String)       — FCItemFood (5-arg)
     *   (int, int, float, boolean, String, bool) — FCItemFood (6-arg, wolf meat with zombies)
     *   (int, int, float, int)           — FCItemSeedFood
     *   (int, int)                       — FCItemSeeds (id, cropBlockID)
     */
    private static void replaceVanillaItemsWithFc() {
        int replaced = 0;
        int failed = 0;
        final String PKG = "net.minecraft.src.btw.item.";

        // ---------------------------------------------------------------
        // Group 1: Tools with (int, EnumToolMaterial) constructor
        // ---------------------------------------------------------------
        Object[][] toolItems = {
            // { rawID, className, material, fieldName, unlocalizedName }
            {  0, "FCItemShovel",  btw.modern.EnumToolMaterial.IRON,    "shovelIron",     "shovelIron"    },
            {  1, "FCItemPickaxe", btw.modern.EnumToolMaterial.IRON,    "pickaxeIron",    "pickaxeIron"   },
            {  2, "FCItemAxe",     btw.modern.EnumToolMaterial.IRON,    "axeIron",        "hatchetIron"   },
            { 11, "FCItemSword",   btw.modern.EnumToolMaterial.IRON,    "swordIron",      "swordIron"     },
            { 12, "FCItemSword",   btw.modern.EnumToolMaterial.WOOD,    "swordWood",      "swordWood"     },
            { 15, "FCItemAxe",     btw.modern.EnumToolMaterial.WOOD,    "axeWood",        "hatchetWood"   },
            { 16, "FCItemSword",   btw.modern.EnumToolMaterial.STONE,   "swordStone",     "swordStone"    },
            { 18, "FCItemPickaxe", btw.modern.EnumToolMaterial.STONE,   "pickaxeStone",   "pickaxeStone"  },
            { 19, "FCItemAxe",     btw.modern.EnumToolMaterial.STONE,   "axeStone",       "hatchetStone"  },
            { 20, "FCItemSword",   btw.modern.EnumToolMaterial.EMERALD, "swordDiamond",   "swordDiamond"  },
            { 21, "FCItemShovel",  btw.modern.EnumToolMaterial.EMERALD, "shovelDiamond",  "shovelDiamond" },
            { 22, "FCItemPickaxe", btw.modern.EnumToolMaterial.EMERALD, "pickaxeDiamond", "pickaxeDiamond"},
            { 23, "FCItemAxe",     btw.modern.EnumToolMaterial.EMERALD, "axeDiamond",     "hatchetDiamond"},
            { 27, "FCItemSword",   btw.modern.EnumToolMaterial.GOLD,    "swordGold",      "swordGold"     },
            { 28, "FCItemShovel",  btw.modern.EnumToolMaterial.GOLD,    "shovelGold",     "shovelGold"    },
            { 29, "FCItemPickaxe", btw.modern.EnumToolMaterial.GOLD,    "pickaxeGold",    "pickaxeGold"   },
            { 30, "FCItemAxe",     btw.modern.EnumToolMaterial.GOLD,    "axeGold",        "hatchetGold"   },
            { 34, "FCItemHoe",     btw.modern.EnumToolMaterial.WOOD,    "hoeWood",        "hoeWood"       },
            { 35, "FCItemHoe",     btw.modern.EnumToolMaterial.STONE,   "hoeStone",       "hoeStone"      },
            { 36, "FCItemHoe",     btw.modern.EnumToolMaterial.IRON,    "hoeIron",        "hoeIron"       },
            { 37, "FCItemHoe",     btw.modern.EnumToolMaterial.EMERALD, "hoeDiamond",     "hoeDiamond"    },
            { 38, "FCItemHoe",     btw.modern.EnumToolMaterial.GOLD,    "hoeGold",        "hoeGold"       },
        };

        for (Object[] entry : toolItems) {
            int id = (int) entry[0];
            String className = PKG + entry[1];
            btw.modern.EnumToolMaterial material = (btw.modern.EnumToolMaterial) entry[2];
            String fieldName = (String) entry[3];
            String unlocalizedName = (String) entry[4];
            try {
                Class<?> clazz = Class.forName(className);
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.EnumToolMaterial.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id, material);
                item.setUnlocalizedName(unlocalizedName);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create {} for ID {}: {}", className, id, e.getMessage());
                failed++;
            }
        }

        // ---------------------------------------------------------------
        // Group 1b: Special tool constructors
        // ---------------------------------------------------------------

        // shovelWood — FCItemShovel(13, WOOD).SetDamageVsEntity(2).setUnlocalizedName("shovelWood")
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemShovel");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.EnumToolMaterial.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(13, btw.modern.EnumToolMaterial.WOOD);
            item.SetDamageVsEntity(2).setUnlocalizedName("shovelWood");
            updateItemStaticField("shovelWood", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemShovel(13,WOOD): {}", e.getMessage());
            failed++;
        }

        // pickaxeWood — FCItemPickaxe(14, WOOD, 1) — 3-arg constructor with maxUses
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemPickaxe");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, btw.modern.EnumToolMaterial.class, int.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(14, btw.modern.EnumToolMaterial.WOOD, 1);
            item.setUnlocalizedName("pickaxeWood");
            updateItemStaticField("pickaxeWood", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemPickaxe(14,WOOD,1): {}", e.getMessage());
            failed++;
        }

        // shovelStone — FCItemShovelStone(17) — special subclass, just (int)
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemShovelStone");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(17);
            updateItemStaticField("shovelStone", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemShovelStone(17): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 2: Simple (int) constructor items
        // ---------------------------------------------------------------
        Object[][] simpleIntItems = {
            // { rawID, className, fieldName }
            {  3, "FCItemFlintAndSteel",     "flintAndSteel"     },
            {  5, "FCItemBow",               "bow"               },
            {  6, "FCItemArrow",             "arrow"             },
            { 24, "FCItemShaft",             "stick"             },
            { 40, "FCItemWheatLegacy",       "wheat"             },
            { 62, "FCItemFlint",             "flint"             },
            { 67, "FCItemSign",              "sign"              },
            { 68, "FCItemDoorWood",          "doorWood"          },
            { 69, "FCItemBucketEmpty",       "bucketEmpty"       },
            { 70, "FCItemBucketWater",       "bucketWater"       },
            { 71, "FCItemBucketLava",        "bucketLava"        },
            { 75, "FCItemRedstone",          "redstone"          },
            { 76, "FCItemSnowball",          "snowball"          },
            { 77, "FCItemBoat",              "boat"              },
            { 79, "FCItemBucketMilk",        "bucketMilk"        },
            { 80, "FCItemBrick",             "brick"             },
            { 81, "FCItemClay",              "clay"              },
            { 84, "FCItemBook",              "book"              },
            { 85, "FCItemSlimeball",         "slimeBall"         },
            { 88, "FCItemEgg",               "egg"               },
            { 90, "FCItemFishingRod",        "fishingRod"        },
            { 95, "FCItemDye",               "dyePowder"         },
            { 96, "FCItemBone",              "bone"              },
            {100, "FCItemRedstoneRepeater",  "redstoneRepeater"  },
            {102, "FCItemMap",               "map"               },
            {111, "FCItemRottenFlesh",       "rottenFlesh"       },
            {117, "FCItemPotion",            "potion"            },
            {129, "FCItemFireCharge",        "fireballCharge"    },
            {139, "FCItemEmptyMap",          "emptyMap"          },
            {142, "FCItemCarrotOnAStick",    "carrotOnAStick"    },
            {143, "FCItemNetherStar",        "netherStar"        },
        };

        for (Object[] entry : simpleIntItems) {
            int id = (int) entry[0];
            String className = PKG + entry[1];
            String fieldName = (String) entry[2];
            try {
                Class<?> clazz = Class.forName(className);
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create {} for ID {}: {}", className, id, e.getMessage());
                failed++;
            }
        }

        // ---------------------------------------------------------------
        // Group 3: Shears — FCItemShears(103), cast to ItemShears
        // ---------------------------------------------------------------
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemShears");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(103);
            item.setUnlocalizedName("shears");
            updateItemStaticField("shears", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemShears(103): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 4: Mushroom soup — FCItemMushroomSoup(26, 3)
        // ---------------------------------------------------------------
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemMushroomSoup");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(26, 3);
            item.setUnlocalizedName("mushroomStew");
            updateItemStaticField("bowlSoup", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemMushroomSoup(26,3): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 5: Minecarts — FCItemMinecart(id, type)
        // ---------------------------------------------------------------
        Object[][] minecartItems = {
            { 72, 0, "minecart",        "minecartEmpty"   },
            { 86, 1, "minecartChest",   "minecartCrate"   },
            { 87, 2, "minecartFurnace", "minecartPowered" },
        };
        for (Object[] entry : minecartItems) {
            int id = (int) entry[0];
            int type = (int) entry[1];
            String unlocalizedName = (String) entry[2];
            String fieldName = (String) entry[3];
            try {
                Class<?> clazz = Class.forName(PKG + "FCItemMinecart");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id, type);
                item.setUnlocalizedName(unlocalizedName);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create FCItemMinecart({},{}): {}", id, type, e.getMessage());
                failed++;
            }
        }

        // ---------------------------------------------------------------
        // Group 6: Armor — Leather: (int, int), Chain/Iron/Diamond/Gold: (int, int, int)
        // ---------------------------------------------------------------

        // Leather armor — FCItemArmorLeather(id, armorType)
        Object[][] leatherArmor = {
            { 42, 0, "helmetCloth",       "helmetLeather" },
            { 43, 1, "chestplateCloth",   "plateLeather"  },
            { 44, 2, "leggingsCloth",     "legsLeather"   },
            { 45, 3, "bootsCloth",        "bootsLeather"  },
        };
        for (Object[] entry : leatherArmor) {
            int id = (int) entry[0];
            int armorType = (int) entry[1];
            String unlocalizedName = (String) entry[2];
            String fieldName = (String) entry[3];
            try {
                Class<?> clazz = Class.forName(PKG + "FCItemArmorLeather");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id, armorType);
                item.setUnlocalizedName(unlocalizedName);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create FCItemArmorLeather({},{}): {}", id, armorType, e.getMessage());
                failed++;
            }
        }

        // Chain armor — FCItemArmorChain(id, armorType, weight)
        Object[][] chainArmor = {
            { 46, 0, 3, "helmetChain",       "helmetChain" },
            { 47, 1, 4, "chestplateChain",   "plateChain"  },
            { 48, 2, 4, "leggingsChain",     "legsChain"   },
            { 49, 3, 2, "bootsChain",        "bootsChain"  },
        };
        for (Object[] entry : chainArmor) {
            int id = (int) entry[0];
            int armorType = (int) entry[1];
            int weight = (int) entry[2];
            String unlocalizedName = (String) entry[3];
            String fieldName = (String) entry[4];
            try {
                Class<?> clazz = Class.forName(PKG + "FCItemArmorChain");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class, int.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id, armorType, weight);
                item.setUnlocalizedName(unlocalizedName);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create FCItemArmorChain({},{},{}): {}", id, armorType, weight, e.getMessage());
                failed++;
            }
        }

        // Iron armor — FCItemArmorIron(id, armorType, weight)
        Object[][] ironArmor = {
            { 50, 0, 5, "helmetIron",       "helmetIron" },
            { 51, 1, 8, "chestplateIron",   "plateIron"  },
            { 52, 2, 7, "leggingsIron",     "legsIron"   },
            { 53, 3, 4, "bootsIron",        "bootsIron"  },
        };
        for (Object[] entry : ironArmor) {
            int id = (int) entry[0];
            int armorType = (int) entry[1];
            int weight = (int) entry[2];
            String unlocalizedName = (String) entry[3];
            String fieldName = (String) entry[4];
            try {
                Class<?> clazz = Class.forName(PKG + "FCItemArmorIron");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class, int.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id, armorType, weight);
                item.setUnlocalizedName(unlocalizedName);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create FCItemArmorIron({},{},{}): {}", id, armorType, weight, e.getMessage());
                failed++;
            }
        }

        // Diamond armor — FCItemArmorDiamond(id, armorType, weight)
        Object[][] diamondArmor = {
            { 54, 0, 5, "helmetDiamond",       "helmetDiamond" },
            { 55, 1, 8, "chestplateDiamond",   "plateDiamond"  },
            { 56, 2, 7, "leggingsDiamond",     "legsDiamond"   },
            { 57, 3, 4, "bootsDiamond",        "bootsDiamond"  },
        };
        for (Object[] entry : diamondArmor) {
            int id = (int) entry[0];
            int armorType = (int) entry[1];
            int weight = (int) entry[2];
            String unlocalizedName = (String) entry[3];
            String fieldName = (String) entry[4];
            try {
                Class<?> clazz = Class.forName(PKG + "FCItemArmorDiamond");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class, int.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id, armorType, weight);
                item.setUnlocalizedName(unlocalizedName);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create FCItemArmorDiamond({},{},{}): {}", id, armorType, weight, e.getMessage());
                failed++;
            }
        }

        // Gold armor — FCItemArmorGold(id, armorType, weight)
        Object[][] goldArmor = {
            { 58, 0, 5, "helmetGold",       "helmetGold" },
            { 59, 1, 8, "chestplateGold",   "plateGold"  },
            { 60, 2, 7, "leggingsGold",     "legsGold"   },
            { 61, 3, 4, "bootsGold",        "bootsGold"  },
        };
        for (Object[] entry : goldArmor) {
            int id = (int) entry[0];
            int armorType = (int) entry[1];
            int weight = (int) entry[2];
            String unlocalizedName = (String) entry[3];
            String fieldName = (String) entry[4];
            try {
                Class<?> clazz = Class.forName(PKG + "FCItemArmorGold");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class, int.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id, armorType, weight);
                item.setUnlocalizedName(unlocalizedName);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create FCItemArmorGold({},{},{}): {}", id, armorType, weight, e.getMessage());
                failed++;
            }
        }

        // ---------------------------------------------------------------
        // Group 7: Food items — FCItemFood(id, hunger, saturation, wolfMeat, name)
        //          and FCItemFood(id, hunger, saturation, wolfMeat, name, zombiesConsume)
        // ---------------------------------------------------------------

        // porkRaw — FCItemFood(63, ..., true, "porkchopRaw", true).SetStandardFoodPoisoningEffect()
        // The hunger/saturation values are static fields on FCItemFood — we access them reflectively
        try {
            Class<?> foodClass = Class.forName(PKG + "FCItemFood");

            // porkRaw — 6-arg constructor (with zombiesConsume=true)
            {
                int hungerHealed = foodClass.getField("m_iPorkChopRawHungerHealed").getInt(null);
                float saturation = foodClass.getField("m_fPorkChopSaturationModifier").getFloat(null);
                java.lang.reflect.Constructor<?> ctor = foodClass.getConstructor(
                        int.class, int.class, float.class, boolean.class, String.class, boolean.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(63, hungerHealed, saturation, true, "porkchopRaw", true);
                // Call SetStandardFoodPoisoningEffect()
                java.lang.reflect.Method poisonMethod = foodClass.getMethod("SetStandardFoodPoisoningEffect");
                poisonMethod.invoke(item);
                updateItemStaticField("porkRaw", item);
                replaced++;
            }

            // fishRaw — FCItemFood(93, ..., false, "fishRaw").SetStandardFoodPoisoningEffect()
            {
                int hungerHealed = foodClass.getField("m_iFishRawHungerHealed").getInt(null);
                float saturation = foodClass.getField("m_fFishSaturationModifier").getFloat(null);
                java.lang.reflect.Constructor<?> ctor = foodClass.getConstructor(
                        int.class, int.class, float.class, boolean.class, String.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(93, hungerHealed, saturation, false, "fishRaw");
                java.lang.reflect.Method poisonMethod = foodClass.getMethod("SetStandardFoodPoisoningEffect");
                poisonMethod.invoke(item);
                updateItemStaticField("fishRaw", item);
                replaced++;
            }

            // beefRaw — FCItemFood(107, ..., true, "beefRaw", true).SetStandardFoodPoisoningEffect()
            {
                int hungerHealed = foodClass.getField("m_iBeefRawHungerHealed").getInt(null);
                float saturation = foodClass.getField("m_fBeefSaturationModifier").getFloat(null);
                java.lang.reflect.Constructor<?> ctor = foodClass.getConstructor(
                        int.class, int.class, float.class, boolean.class, String.class, boolean.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(107, hungerHealed, saturation, true, "beefRaw", true);
                java.lang.reflect.Method poisonMethod = foodClass.getMethod("SetStandardFoodPoisoningEffect");
                poisonMethod.invoke(item);
                updateItemStaticField("beefRaw", item);
                replaced++;
            }

            // chickenRaw — FCItemFood(109, ..., true, "chickenRaw").SetStandardFoodPoisoningEffect()
            {
                int hungerHealed = foodClass.getField("m_iChickenRawHungerHealed").getInt(null);
                float saturation = foodClass.getField("m_fChickenSaturationModifier").getFloat(null);
                java.lang.reflect.Constructor<?> ctor = foodClass.getConstructor(
                        int.class, int.class, float.class, boolean.class, String.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(109, hungerHealed, saturation, true, "chickenRaw");
                java.lang.reflect.Method poisonMethod = foodClass.getMethod("SetStandardFoodPoisoningEffect");
                poisonMethod.invoke(item);
                updateItemStaticField("chickenRaw", item);
                replaced++;
            }
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemFood items: {}", e.getMessage());
            failed += 4;
        }

        // melon — FCItemFoodHighRes(104, 2, 0F, false, "melon")
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemFoodHighRes");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(
                    int.class, int.class, float.class, boolean.class, String.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(104, 2, 0F, false, "melon");
            updateItemStaticField("melon", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemFoodHighRes(104): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 8: Seed items — FCItemSeeds(id, cropBlockID)
        //          and FCItemSeedFood(id, healAmount, saturation, cropBlockID)
        // ---------------------------------------------------------------

        // seeds — FCItemSeeds(39, Block.crops.blockID)
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemSeeds");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class);
            int cropsBlockID = btw.modern.Block.crops != null ? btw.modern.Block.crops.blockID : 59;
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(39, cropsBlockID);
            item.SetAsBasicChickenFood().setUnlocalizedName("seeds").setCreativeTab(null);
            updateItemStaticField("seeds", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemSeeds(39): {}", e.getMessage());
            failed++;
        }

        // melonSeeds — FCItemSeeds(106, Block.melonStem.blockID)
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemSeeds");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class);
            int melonStemBlockID = btw.modern.Block.melonStem != null ? btw.modern.Block.melonStem.blockID : 105;
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(106, melonStemBlockID);
            item.SetAsBasicChickenFood().setUnlocalizedName("seeds_melon");
            updateItemStaticField("melonSeeds", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemSeeds(106): {}", e.getMessage());
            failed++;
        }

        // netherStalkSeeds — FCItemSeeds(116, Block.netherStalk.blockID)
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemSeeds");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class);
            int netherStalkBlockID = btw.modern.Block.netherStalk != null ? btw.modern.Block.netherStalk.blockID : 115;
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(116, netherStalkBlockID);
            item.SetBellowsBlowDistance(1).setUnlocalizedName("netherStalkSeeds").setPotionEffect("+4");
            updateItemStaticField("netherStalkSeeds", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemSeeds(116): {}", e.getMessage());
            failed++;
        }

        // pumpkinSeeds — FCItemSeedFood(105, 1, 0F, Block.pumpkinStem.blockID)
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemSeedFood");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class, float.class, int.class);
            int pumpkinStemBlockID = btw.modern.Block.pumpkinStem != null ? btw.modern.Block.pumpkinStem.blockID : 104;
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(105, 1, 0F, pumpkinStemBlockID);
            item.SetAsBasicChickenFood().SetBellowsBlowDistance(2)
                    .SetFilterableProperties(btw.modern.Item.m_iFilterable_Fine)
                    .setUnlocalizedName("seeds_pumpkin");
            updateItemStaticField("pumpkinSeeds", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemSeedFood(105): {}", e.getMessage());
            failed++;
        }

        // carrot — FCItemSeedFood(135, 3, 0F, Block.carrot.blockID)
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemSeedFood");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class, float.class, int.class);
            int carrotBlockID = btw.modern.Block.carrot != null ? btw.modern.Block.carrot.blockID : 141;
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(135, 3, 0F, carrotBlockID);
            item.SetFilterableProperties(btw.modern.Item.m_iFilterable_Small)
                    .SetAsBasicPigFood().setUnlocalizedName("carrots");
            updateItemStaticField("carrot", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemSeedFood(135): {}", e.getMessage());
            failed++;
        }

        // potato — FCItemSeedFood(136, 3, 0F, Block.potato.blockID)
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemSeedFood");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class, int.class, float.class, int.class);
            int potatoBlockID = btw.modern.Block.potato != null ? btw.modern.Block.potato.blockID : 142;
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(136, 3, 0F, potatoBlockID);
            item.SetFilterableProperties(btw.modern.Item.m_iFilterable_Small)
                    .SetAsBasicPigFood().setUnlocalizedName("potato");
            updateItemStaticField("potato", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemSeedFood(136): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 9: Glass bottle — FCItemGlassBottle(118)
        // ---------------------------------------------------------------
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemGlassBottle");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(118);
            item.SetBuoyant().setUnlocalizedName("glassBottle");
            updateItemStaticField("glassBottle", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemGlassBottle(118): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 10: Enchanted book — FCItemEnchantedBook(147)
        // ---------------------------------------------------------------
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemEnchantedBook");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(147);
            item.setMaxStackSize(1).setUnlocalizedName("enchantedBook");
            updateItemStaticField("enchantedBook", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemEnchantedBook(147): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 11: Nether quartz — FCItemNetherQuartz(150)
        // ---------------------------------------------------------------
        try {
            Class<?> clazz = Class.forName(PKG + "FCItemNetherQuartz");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
            btw.modern.Item item = (btw.modern.Item) ctor.newInstance(150);
            item.setUnlocalizedName("netherquartz").setCreativeTab(btw.modern.CreativeTabs.tabMaterials);
            updateItemStaticField("netherQuartz", item);
            replaced++;
        } catch (Exception e) {
            LOGGER.debug("  Could not create FCItemNetherQuartz(150): {}", e.getMessage());
            failed++;
        }

        // ---------------------------------------------------------------
        // Group 12: Stub items — FCItemStub(id) — items BTW disables
        // ---------------------------------------------------------------
        Object[][] stubItems = {
            { 148, "comparator",   "comparator"   },
            { 151, "minecartTnt",  "minecartTnt"  },
            { 152, "minecartHopper","minecartHopper"},
        };
        for (Object[] entry : stubItems) {
            int id = (int) entry[0];
            String unlocalizedName = (String) entry[1];
            String fieldName = (String) entry[2];
            try {
                Class<?> clazz = Class.forName(PKG + "FCItemStub");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(int.class);
                btw.modern.Item item = (btw.modern.Item) ctor.newInstance(id);
                item.setUnlocalizedName(unlocalizedName);
                updateItemStaticField(fieldName, item);
                replaced++;
            } catch (Exception e) {
                LOGGER.debug("  Could not create FCItemStub({}): {}", id, e.getMessage());
                failed++;
            }
        }

        LOGGER.info("  Replaced {} vanilla items with FC subclasses ({} failed)", replaced, failed);
    }

    /**
     * Updates the corresponding static field on {@link btw.modern.Item}.
     * For example, fieldName "axeIron" sets {@code Item.axeIron = item}.
     * Silently ignores fields that don't exist on btw.modern.Item.
     */
    private static void updateItemStaticField(String fieldName, btw.modern.Item item) {
        if (fieldName == null) return;
        try {
            java.lang.reflect.Field field = btw.modern.Item.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, item);
        } catch (NoSuchFieldException e) {
            // Field doesn't exist on btw.modern.Item — that's OK, itemsList[id] is already set
        } catch (Exception e) {
            LOGGER.debug("  Could not update Item.{}: {}", fieldName, e.getMessage());
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
