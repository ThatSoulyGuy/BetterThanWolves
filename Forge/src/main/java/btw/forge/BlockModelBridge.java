package btw.forge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Reads FC block bounds at runtime and generates element-based model JSONs
 * for blocks that aren't full cubes. Called after FC initialization so that
 * all block bounds are populated.
 */
public class BlockModelBridge {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ModelBridge");

    // Block texture map - must match ModelGenerator's BLOCK_TEXTURE_MAP
    // Maps legacy block ID -> texture base name (lowercase)
    private static final Map<Integer, String> BLOCK_TEXTURES = new LinkedHashMap<>();

    static {
        // Copy of ModelGenerator.BLOCK_TEXTURE_MAP (lowercase values)
        BLOCK_TEXTURES.put(176, "vessel");
        BLOCK_TEXTURES.put(177, "axle");
        BLOCK_TEXTURES.put(178, "decorativeblackstone");
        BLOCK_TEXTURES.put(179, "decorativeblackstone");
        BLOCK_TEXTURES.put(180, "dung");
        BLOCK_TEXTURES.put(181, "candle");
        BLOCK_TEXTURES.put(182, "decorativesandstone");
        BLOCK_TEXTURES.put(183, "decorativesandstone");
        BLOCK_TEXTURES.put(184, "decorativewoodoak");
        BLOCK_TEXTURES.put(185, "decorativestone");
        BLOCK_TEXTURES.put(186, "decorativebrick");
        BLOCK_TEXTURES.put(187, "decorativebrick");
        BLOCK_TEXTURES.put(188, "decorativenetherbrick");
        BLOCK_TEXTURES.put(189, "decorativenetherbrick");
        BLOCK_TEXTURES.put(190, "decorativewhitestone");
        BLOCK_TEXTURES.put(191, "decorativewhitestone");
        BLOCK_TEXTURES.put(192, "decorativewhitestone");
        BLOCK_TEXTURES.put(193, "stakestring");
        BLOCK_TEXTURES.put(194, "stake");
        BLOCK_TEXTURES.put(195, "screwpump");
        BLOCK_TEXTURES.put(196, "decorativewoodspruce");
        BLOCK_TEXTURES.put(197, "decorativewoodspruce");
        BLOCK_TEXTURES.put(198, "decorativewoodbirch");
        BLOCK_TEXTURES.put(199, "decorativewoodbirch");
        BLOCK_TEXTURES.put(200, "decorativewoodjungle");
        BLOCK_TEXTURES.put(201, "decorativewoodjungle");
        BLOCK_TEXTURES.put(202, "decorativestonebrick");
        BLOCK_TEXTURES.put(203, "decorativestonebrick");
        BLOCK_TEXTURES.put(204, "farmlandfertilized");
        BLOCK_TEXTURES.put(205, "padding");
        BLOCK_TEXTURES.put(206, "slabdirt");
        BLOCK_TEXTURES.put(207, "groth");
        BLOCK_TEXTURES.put(208, "infernalenchanter");
        BLOCK_TEXTURES.put(209, "soulforgedsteel");
        BLOCK_TEXTURES.put(210, "detectorblock");
        BLOCK_TEXTURES.put(211, "leavesbloodwood");
        BLOCK_TEXTURES.put(212, "bloodwood");
        BLOCK_TEXTURES.put(213, "weeds");
        BLOCK_TEXTURES.put(214, "decorativestone");
        BLOCK_TEXTURES.put(215, "ender");
        BLOCK_TEXTURES.put(216, "slats");
        BLOCK_TEXTURES.put(217, "miningcharge");
        BLOCK_TEXTURES.put(218, "buddyblock");
        BLOCK_TEXTURES.put(219, "furnacebrick");
        BLOCK_TEXTURES.put(220, "padding");
        BLOCK_TEXTURES.put(221, "anvil");
        BLOCK_TEXTURES.put(222, "lightblock");
        BLOCK_TEXTURES.put(223, "lightblock");
        BLOCK_TEXTURES.put(224, "hibachi");
        BLOCK_TEXTURES.put(225, "hopper");
        BLOCK_TEXTURES.put(226, "saw");
        BLOCK_TEXTURES.put(227, "platform");
        BLOCK_TEXTURES.put(228, "cement");
        BLOCK_TEXTURES.put(229, "pulley");
        BLOCK_TEXTURES.put(230, "pressureplatesoulforgedsteel");
        BLOCK_TEXTURES.put(231, "decorativewoodoak");
        BLOCK_TEXTURES.put(232, "decorativestone");
        BLOCK_TEXTURES.put(233, "blockdispenser");
        BLOCK_TEXTURES.put(234, "cauldron");
        BLOCK_TEXTURES.put(235, "detectorrailwood");
        BLOCK_TEXTURES.put(236, "detectorrailsoulforgedsteel");
        BLOCK_TEXTURES.put(237, "companioncube");
        BLOCK_TEXTURES.put(238, "detectorblock");
        BLOCK_TEXTURES.put(239, "detectorblock");
        BLOCK_TEXTURES.put(240, "lens");
        BLOCK_TEXTURES.put(241, "hemp");
        BLOCK_TEXTURES.put(242, "handcrank");
        BLOCK_TEXTURES.put(243, "millstone");
        BLOCK_TEXTURES.put(244, "anchor");
        BLOCK_TEXTURES.put(245, "rope");
        BLOCK_TEXTURES.put(246, "slats");
        BLOCK_TEXTURES.put(247, "axle");
        BLOCK_TEXTURES.put(248, "redstoneclutch");
        BLOCK_TEXTURES.put(249, "turntable");
        BLOCK_TEXTURES.put(250, "bellows");
        BLOCK_TEXTURES.put(251, "firestokedstub");
        BLOCK_TEXTURES.put(252, "unfiredpottery");
        BLOCK_TEXTURES.put(253, "crucible");
        BLOCK_TEXTURES.put(254, "planter");
        BLOCK_TEXTURES.put(255, "vase");
        BLOCK_TEXTURES.put(1000, "rottenflesh");
        BLOCK_TEXTURES.put(1001, "shaft");
        BLOCK_TEXTURES.put(1002, "soulforgedormant");
        BLOCK_TEXTURES.put(1003, "stone");
        BLOCK_TEXTURES.put(1004, "rottenfleshslab");
        BLOCK_TEXTURES.put(1005, "boneslab");
        BLOCK_TEXTURES.put(1007, "decorativewoodblood");
        BLOCK_TEXTURES.put(1008, "decorativewoodblood");
        BLOCK_TEXTURES.put(1009, "planks_blood");
        BLOCK_TEXTURES.put(1010, "logchewedoak");
        BLOCK_TEXTURES.put(1011, "dirtloose");
        BLOCK_TEXTURES.put(1012, "dirtloose");
        BLOCK_TEXTURES.put(1013, "campfire");
        BLOCK_TEXTURES.put(1014, "campfire");
        BLOCK_TEXTURES.put(1015, "campfire");
        BLOCK_TEXTURES.put(1016, "campfire");
        BLOCK_TEXTURES.put(1017, "unfiredbrick");
        BLOCK_TEXTURES.put(1018, "cookedbrick");
        BLOCK_TEXTURES.put(1019, "brickloose");
        BLOCK_TEXTURES.put(1020, "brickloose");
        BLOCK_TEXTURES.put(1021, "cobblestoneloose");
        BLOCK_TEXTURES.put(1022, "cobblestoneloose");
        BLOCK_TEXTURES.put(1023, "furnacebrick");
        BLOCK_TEXTURES.put(1024, "furnacebrick");
        BLOCK_TEXTURES.put(1025, "torchfiniteidle");
        BLOCK_TEXTURES.put(1026, "torchfiniteburning");
        BLOCK_TEXTURES.put(1027, "stonerough");
        BLOCK_TEXTURES.put(1028, "stonerough");
        BLOCK_TEXTURES.put(1029, "stonerough");
        BLOCK_TEXTURES.put(1030, "workstumpoak");
        BLOCK_TEXTURES.put(1031, "basketwicker");
        BLOCK_TEXTURES.put(1032, "logchewedoak");
        BLOCK_TEXTURES.put(1033, "torchidle");
        BLOCK_TEXTURES.put(1034, "tablewoodoak");
        BLOCK_TEXTURES.put(1035, "barrel");
        BLOCK_TEXTURES.put(1037, "web");
        BLOCK_TEXTURES.put(1038, "unfiredclay");
        BLOCK_TEXTURES.put(1039, "myceliumslab");
        BLOCK_TEXTURES.put(1040, "shovel");
        BLOCK_TEXTURES.put(1041, "brickloose");
        BLOCK_TEXTURES.put(1042, "cobblestoneloose");
        BLOCK_TEXTURES.put(1043, "logsmouldering");
        BLOCK_TEXTURES.put(1044, "woodcinders");
        BLOCK_TEXTURES.put(1045, "stumpcharred");
        BLOCK_TEXTURES.put(1046, "ashgroundcover");
        BLOCK_TEXTURES.put(1047, "snowloose");
        BLOCK_TEXTURES.put(1048, "snowlooseslab");
        BLOCK_TEXTURES.put(1049, "snowsolid");
        BLOCK_TEXTURES.put(1050, "snowsolidslab");
        BLOCK_TEXTURES.put(1053, "shovel");
        BLOCK_TEXTURES.put(1054, "hamper");
        BLOCK_TEXTURES.put(1055, "creeperoysters");
        BLOCK_TEXTURES.put(1056, "creeperoysterslab");
        BLOCK_TEXTURES.put(1057, "torchnetherburning");
        BLOCK_TEXTURES.put(1058, "bucketempty");
        BLOCK_TEXTURES.put(1059, "bucket_water");
        BLOCK_TEXTURES.put(1060, "cement");
        BLOCK_TEXTURES.put(1061, "bucket_milk");
        BLOCK_TEXTURES.put(1062, "bucket_chocolate");
        BLOCK_TEXTURES.put(1063, "milk");
        BLOCK_TEXTURES.put(1064, "milkchocolate");
        BLOCK_TEXTURES.put(1065, "gearbox");
        BLOCK_TEXTURES.put(1066, "spikeiron");
        BLOCK_TEXTURES.put(1067, "lightningrod");
        BLOCK_TEXTURES.put(1068, "chunkoreiron");
        BLOCK_TEXTURES.put(1069, "chunkoregold");
        BLOCK_TEXTURES.put(1070, "stonebrickloose");
        BLOCK_TEXTURES.put(1071, "stonebrickloose");
        BLOCK_TEXTURES.put(1072, "stonebrickloose");
        BLOCK_TEXTURES.put(1073, "netherbrickloose");
        BLOCK_TEXTURES.put(1074, "netherbrickloose");
        BLOCK_TEXTURES.put(1075, "netherbrickloose");
        BLOCK_TEXTURES.put(1076, "netherrackgrothed");
        BLOCK_TEXTURES.put(1077, "lavapillow");
        BLOCK_TEXTURES.put(1078, "stub");
        BLOCK_TEXTURES.put(1079, "stub");
        BLOCK_TEXTURES.put(1080, "chunkorestorageiron");
        BLOCK_TEXTURES.put(1081, "chunkorestoragegold");
        BLOCK_TEXTURES.put(1082, "wicker");
        BLOCK_TEXTURES.put(1083, "slabwicker");
        BLOCK_TEXTURES.put(1084, "wicker");
        BLOCK_TEXTURES.put(1085, "grate");
        BLOCK_TEXTURES.put(1086, "slats");
        BLOCK_TEXTURES.put(1087, "farmlandfertilized");
        BLOCK_TEXTURES.put(1088, "farmlandfertilized");
        BLOCK_TEXTURES.put(1089, "wheatcrop");
        BLOCK_TEXTURES.put(1090, "wheatcroptop");
        BLOCK_TEXTURES.put(1091, "weeds");
        BLOCK_TEXTURES.put(1092, "planter");
    }

    /**
     * Holds a single box extracted from an FCModelBlock primitive list.
     * Coordinates are in block-space (0.0 to 1.0).
     */
    public static class ModelBox {
        public final double minX, minY, minZ, maxX, maxY, maxZ;
        public ModelBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        }
    }

    /**
     * Attempt to extract exact model geometry from an FC block's FCModelBlock field(s)
     * using reflection. Returns a list of ModelBox objects representing the primitives,
     * or null if no FCModelBlock field was found.
     *
     * FCModelBlock (net.minecraft.src.btw.model.FCModelBlock) extends
     * btw.modern.FCUtilsPrimitiveGeometric and contains a private field
     * {@code m_primitiveList} (List of FCUtilsPrimitiveGeometric). Each entry
     * in the list is typically a btw.modern.AxisAlignedBB with public
     * minX/minY/minZ/maxX/maxY/maxZ double fields.
     */
    public static List<ModelBox> extractModelBoxes(btw.modern.Block block) {
        // Find the BEST FCModelBlock field on the block.
        // Priority: "m_model" > any single FCModelBlock > first element of FCModelBlock array.
        // Skip: fields containing "temp", "current", "selection", "box" (non-render models).
        Object modelObject = null;
        Object fallbackModel = null; // from array[0]

        for (Class<?> clazz = block.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    String fname = field.getName().toLowerCase();

                    // Skip obvious non-render fields
                    if (fname.contains("temp") || fname.contains("current")
                            || fname.contains("selection") || fname.contains("box")) continue;

                    field.setAccessible(true);

                    if (field.getType().isArray()) {
                        // Array of FCModelBlocks — use element [0] as fallback
                        Object arr = field.get(block);
                        if (arr != null && java.lang.reflect.Array.getLength(arr) > 0) {
                            Object elem = java.lang.reflect.Array.get(arr, 0);
                            if (elem instanceof btw.modern.FCUtilsPrimitiveGeometric) {
                                Field primListField = findFieldInHierarchy(elem.getClass(), "m_primitiveList");
                                if (primListField != null && fallbackModel == null) {
                                    fallbackModel = elem;
                                }
                            }
                        }
                        continue;
                    }

                    Object value = field.get(block);
                    if (value == null) continue;

                    if (value instanceof btw.modern.FCUtilsPrimitiveGeometric) {
                        Field primListField = findFieldInHierarchy(value.getClass(), "m_primitiveList");
                        if (primListField != null) {
                            if (fname.equals("m_model")) {
                                // Best match — use immediately
                                modelObject = value;
                                break;
                            } else if (modelObject == null) {
                                modelObject = value;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (modelObject != null && modelObject.getClass().getName().contains("m_model")) break;
        }

        // Use array fallback if no single field found
        if (modelObject == null) modelObject = fallbackModel;

        if (modelObject == null) return null;

        // Extract primitive list from FCModelBlock
        try {
            Field primListField = findFieldInHierarchy(modelObject.getClass(), "m_primitiveList");
            if (primListField == null) return null;
            primListField.setAccessible(true);
            Object listObj = primListField.get(modelObject);
            if (!(listObj instanceof List<?>)) return null;

            List<?> primitiveList = (List<?>) listObj;
            if (primitiveList.isEmpty()) return null;

            List<ModelBox> boxes = new ArrayList<>();
            for (Object primitive : primitiveList) {
                if (primitive == null) continue;
                // Each primitive could be an AxisAlignedBB directly (a box) or
                // another FCModelBlock (a nested model). We handle both cases:
                // try to extract minX..maxZ directly first, then recurse if needed.
                List<ModelBox> extracted = extractBoxesFromPrimitive(primitive);
                if (extracted != null) {
                    boxes.addAll(extracted);
                }
            }
            return boxes.isEmpty() ? null : boxes;
        } catch (Exception e) {
            LOGGER.debug("Failed to extract primitive list from model: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Recursively extracts ModelBox entries from a single FCUtilsPrimitiveGeometric.
     * If it is a plain AxisAlignedBB (has minX..maxZ), returns a singleton list.
     * If it is an FCModelBlock (has m_primitiveList), recurses into the list.
     */
    private static List<ModelBox> extractBoxesFromPrimitive(Object primitive) {
        if (primitive == null) return null;

        // First check if it has m_primitiveList (i.e., is an FCModelBlock / nested model)
        Field primListField = findFieldInHierarchy(primitive.getClass(), "m_primitiveList");
        if (primListField != null) {
            try {
                primListField.setAccessible(true);
                Object listObj = primListField.get(primitive);
                if (listObj instanceof List<?>) {
                    List<?> subList = (List<?>) listObj;
                    List<ModelBox> result = new ArrayList<>();
                    for (Object sub : subList) {
                        List<ModelBox> subBoxes = extractBoxesFromPrimitive(sub);
                        if (subBoxes != null) result.addAll(subBoxes);
                    }
                    return result.isEmpty() ? null : result;
                }
            } catch (Exception ignored) {}
        }

        // Otherwise try to read AxisAlignedBB fields: minX, minY, minZ, maxX, maxY, maxZ
        try {
            double minX = getDoubleField(primitive, "minX");
            double minY = getDoubleField(primitive, "minY");
            double minZ = getDoubleField(primitive, "minZ");
            double maxX = getDoubleField(primitive, "maxX");
            double maxY = getDoubleField(primitive, "maxY");
            double maxZ = getDoubleField(primitive, "maxZ");
            return Collections.singletonList(new ModelBox(minX, minY, minZ, maxX, maxY, maxZ));
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Search for a named field up the class hierarchy.
     */
    private static Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    /**
     * Read a public double field by name from an object, searching up the hierarchy.
     */
    private static double getDoubleField(Object obj, String fieldName) throws Exception {
        Field f = findFieldInHierarchy(obj.getClass(), fieldName);
        if (f == null) throw new NoSuchFieldException(fieldName);
        f.setAccessible(true);
        return f.getDouble(obj);
    }

    /**
     * Generate element-based block models using FC block bounds.
     * Called after FC initialization when all blocks have their bounds set.
     *
     * For blocks that have FCModelBlock fields, extracts exact multi-box geometry
     * via reflection. Falls back to single-box setBlockBounds approach otherwise.
     */
    public static void generateBlockModels(Path modelsBlockDir, Path texturesBlockDir) {
        if (!Files.exists(modelsBlockDir)) {
            LOGGER.warn("Block models directory not found: {}", modelsBlockDir);
            return;
        }

        // Scan available textures to know which side/top/end variants exist
        Map<String, List<String>> textureGroups = new HashMap<>();
        try {
            if (Files.exists(texturesBlockDir)) {
                Files.list(texturesBlockDir)
                    .filter(p -> p.toString().endsWith(".png"))
                    .forEach(p -> {
                        String name = p.getFileName().toString().replace(".png", "");
                        String baseName = getTextureBaseName(name);
                        textureGroups.computeIfAbsent(baseName, k -> new ArrayList<>()).add(name);
                    });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan textures", e);
            return;
        }

        int customModels = 0;
        int fullCubeModels = 0;
        int multiBoxModels = 0;
        int autoDiscovered = 0;

        // Build a complete block ID -> texture base map.
        // Start with hardcoded entries, then auto-discover from class names.
        Map<Integer, String> allBlockTextures = new LinkedHashMap<>(BLOCK_TEXTURES);

        // Count non-null blocks for debugging
        int nonNullBlocks = 0;
        for (int id = 175; id < btw.modern.Block.blocksList.length; id++) {
            if (btw.modern.Block.blocksList[id] != null) nonNullBlocks++;
        }
        LOGGER.info("Total non-null blocks in blocksList[175+]: {}", nonNullBlocks);
        int above900 = 0;
        for (int id = 900; id < btw.modern.Block.blocksList.length; id++) {
            if (btw.modern.Block.blocksList[id] != null) above900++;
        }
        LOGGER.info("Non-null blocks above ID 900: {}", above900);

        // Auto-discover: for each FC block, derive texture base name from class name
        for (int id = 175; id < btw.modern.Block.blocksList.length; id++) {
            if (allBlockTextures.containsKey(id)) continue;
            btw.modern.Block block = btw.modern.Block.blocksList[id];
            if (block == null) continue;

            // Get class name: "FCBlockAnvil" -> "anvil", "FCBlockDirtLoose" -> "dirtloose"
            String className = block.getClass().getSimpleName();
            String derived = className.toLowerCase();
            // Strip common prefixes
            for (String prefix : new String[]{"fcblock", "block", "fc"}) {
                if (derived.startsWith(prefix) && derived.length() > prefix.length()) {
                    derived = derived.substring(prefix.length());
                    break;
                }
            }

            // Check if any texture group matches
            if (textureGroups.containsKey(derived)) {
                allBlockTextures.put(id, derived);
                autoDiscovered++;
            } else {
                // Try with "fcblock" prefix (texture files are named fcblock*)
                String withPrefix = "fcblock" + derived;
                if (textureGroups.containsKey(withPrefix)) {
                    allBlockTextures.put(id, withPrefix);
                    autoDiscovered++;
                }
            }
        }

        LOGGER.info("Auto-discovered {} additional block texture mappings (from {} unmapped blocks, {} texture groups)",
                autoDiscovered,
                btw.modern.Block.blocksList.length - allBlockTextures.size(),
                textureGroups.size());
        // Log a few texture group keys for debugging
        if (LOGGER.isDebugEnabled()) {
            textureGroups.keySet().stream().limit(20).forEach(k ->
                    LOGGER.debug("  texture group: '{}' -> {}", k, textureGroups.get(k).size()));
        }

        for (Map.Entry<Integer, String> entry : allBlockTextures.entrySet()) {
            int blockId = entry.getKey();
            String textureBase = entry.getValue();

            // Get the FC block
            btw.modern.Block fcBlock = btw.modern.Block.blocksList[blockId];
            if (fcBlock == null) continue;

            // Check if texture group exists
            if (!textureGroups.containsKey(textureBase)) continue;

            List<String> textures = textureGroups.get(textureBase);

            // Try to extract exact multi-box geometry from FCModelBlock fields.
            // Only use for blocks that are SIMPLE shapes (single model, no arrays).
            // Blocks with context-dependent rendering (damaged logs, fences, etc.)
            // have array model fields and must fall back to setBlockBounds.
            List<ModelBox> modelBoxes = null;
            try {
                modelBoxes = extractModelBoxes(fcBlock);
            } catch (Exception e) {
                LOGGER.debug("Model extraction failed for block {}: {}", blockId, e.getMessage());
            }

            // Read block bounds as fallback
            double minX = fcBlock.minX, minY = fcBlock.minY, minZ = fcBlock.minZ;
            double maxX = fcBlock.maxX, maxY = fcBlock.maxY, maxZ = fcBlock.maxZ;
            boolean isFullCube = fcBlock.isOpaqueCube() && fcBlock.renderAsNormalBlock()
                    && minX == 0 && minY == 0 && minZ == 0
                    && maxX == 1 && maxY == 1 && maxZ == 1;

            // Also call setBlockBoundsForItemRender to get the item render bounds
            try {
                fcBlock.setBlockBoundsForItemRender();
                minX = fcBlock.minX; minY = fcBlock.minY; minZ = fcBlock.minZ;
                maxX = fcBlock.maxX; maxY = fcBlock.maxY; maxZ = fcBlock.maxZ;
                isFullCube = isFullCube
                        && minX == 0 && minY == 0 && minZ == 0
                        && maxX == 1 && maxY == 1 && maxZ == 1;
            } catch (Exception e) {
                // Some blocks might fail, use default bounds
            }

            String modelFileName = "fcblock" + textureBase + ".json";
            Path modelPath = modelsBlockDir.resolve(modelFileName);

            try {
                String json;
                if (modelBoxes != null && modelBoxes.size() > 1) {
                    // Use exact multi-box geometry from FCModelBlock
                    json = generateMultiBoxModel(textureBase, textures, modelBoxes);
                    multiBoxModels++;
                } else if (modelBoxes != null && modelBoxes.size() == 1) {
                    // Single-box model from FCModelBlock — use its exact bounds
                    ModelBox box = modelBoxes.get(0);
                    boolean singleBoxFullCube = box.minX == 0 && box.minY == 0 && box.minZ == 0
                            && box.maxX == 1 && box.maxY == 1 && box.maxZ == 1;
                    if (singleBoxFullCube && isFullCube) {
                        json = generateCubeModel(textureBase, textures);
                        fullCubeModels++;
                    } else {
                        json = generateElementModel(textureBase, textures,
                                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                        customModels++;
                    }
                } else if (isFullCube) {
                    json = generateCubeModel(textureBase, textures);
                    fullCubeModels++;
                } else {
                    json = generateElementModel(textureBase, textures, minX, minY, minZ, maxX, maxY, maxZ);
                    customModels++;
                }
                try (FileWriter writer = new FileWriter(modelPath.toFile())) {
                    writer.write(json);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to write model for block {}: {}", blockId, e.getMessage());
            }
        }

        LOGGER.info("Generated block models: {} multi-box, {} custom-shaped, {} full-cube",
                multiBoxModels, customModels, fullCubeModels);

        // Update blockstate JSONs to point to real models instead of placeholder
        Path blockstatesDir = modelsBlockDir.getParent().getParent().resolve("blockstates");
        Path texturesDirForMeta = texturesBlockDir; // save for metadata pass
        if (Files.exists(blockstatesDir)) {
            int updated = 0;
            for (Map.Entry<Integer, String> entry : allBlockTextures.entrySet()) {
                int blockId = entry.getKey();
                String textureBase = entry.getValue();
                String modelName = "fcblock" + textureBase;
                Path bsPath = blockstatesDir.resolve("block_" + blockId + ".json");

                if (Files.exists(bsPath)) {
                    try {
                        String content = new String(Files.readAllBytes(bsPath));
                        if (content.contains("btw_placeholder_cube")) {
                            // Replace placeholder with real model
                            content = content.replace("btw_placeholder_cube", modelName);
                            Files.write(bsPath, content.getBytes());
                            updated++;
                        }
                    } catch (IOException e) {
                        LOGGER.debug("Failed to update blockstate for {}: {}", blockId, e.getMessage());
                    }
                }
            }
            LOGGER.info("Updated {} blockstate JSONs from placeholder to real models", updated);
        }

        // Metadata-aware model generation pass: detect per-meta textures via registerIcons/getIcon
        generateMetadataAwareModels(modelsBlockDir, blockstatesDir, texturesDirForMeta, allBlockTextures);
    }

    /**
     * Generate a standard cube model (same as before for full-cube blocks).
     */
    private static String generateCubeModel(String baseName, List<String> textures) {
        boolean hasTop = textures.stream().anyMatch(t -> t.contains("_top"));
        boolean hasSide = textures.stream().anyMatch(t -> t.contains("_side"));
        boolean hasFront = textures.stream().anyMatch(t -> t.contains("_front"));
        boolean hasEnd = textures.stream().anyMatch(t -> t.contains("_end"));

        String baseTexture = textures.stream()
                .filter(t -> !t.contains("_"))
                .findFirst().orElse(textures.get(0));

        StringBuilder sb = new StringBuilder();
        if (hasFront && hasSide && hasTop) {
            sb.append("{\n  \"parent\": \"minecraft:block/orientable\",\n  \"textures\": {");
            sb.append("\"front\": \"betterthanwolves:block/").append(findTexture(textures, "_front")).append("\", ");
            sb.append("\"side\": \"betterthanwolves:block/").append(findTexture(textures, "_side")).append("\", ");
            sb.append("\"top\": \"betterthanwolves:block/").append(findTexture(textures, "_top")).append("\"}\n}");
        } else if (hasTop && hasSide) {
            sb.append("{\n  \"parent\": \"minecraft:block/cube_column\",\n  \"textures\": {");
            sb.append("\"end\": \"betterthanwolves:block/").append(findTexture(textures, "_top")).append("\", ");
            sb.append("\"side\": \"betterthanwolves:block/").append(findTexture(textures, "_side")).append("\"}\n}");
        } else if (hasEnd && hasSide) {
            sb.append("{\n  \"parent\": \"minecraft:block/cube_column\",\n  \"textures\": {");
            sb.append("\"end\": \"betterthanwolves:block/").append(findTexture(textures, "_end")).append("\", ");
            sb.append("\"side\": \"betterthanwolves:block/").append(findTexture(textures, "_side")).append("\"}\n}");
        } else {
            sb.append("{\n  \"parent\": \"minecraft:block/cube_all\",\n  \"textures\": {");
            sb.append("\"all\": \"betterthanwolves:block/").append(baseTexture).append("\"}\n}");
        }
        return sb.toString();
    }

    /**
     * Generate an element-based model with custom bounds.
     */
    private static String generateElementModel(String baseName, List<String> textures,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

        // Clamp bounds to 0-1 range
        minX = Math.max(0, Math.min(1, minX));
        minY = Math.max(0, Math.min(1, minY));
        minZ = Math.max(0, Math.min(1, minZ));
        maxX = Math.max(0, Math.min(1, maxX));
        maxY = Math.max(0, Math.min(1, maxY));
        maxZ = Math.max(0, Math.min(1, maxZ));

        // Scale to 0-16 range for MC model format
        double fx = minX * 16, fy = minY * 16, fz = minZ * 16;
        double tx = maxX * 16, ty = maxY * 16, tz = maxZ * 16;

        // Determine textures
        boolean hasTop = textures.stream().anyMatch(t -> t.contains("_top"));
        boolean hasSide = textures.stream().anyMatch(t -> t.contains("_side"));
        boolean hasEnd = textures.stream().anyMatch(t -> t.contains("_end"));
        boolean hasFront = textures.stream().anyMatch(t -> t.contains("_front"));
        boolean hasBottom = textures.stream().anyMatch(t -> t.contains("_bottom"));

        String baseTexture = textures.stream()
                .filter(t -> !t.contains("_"))
                .findFirst().orElse(textures.get(0));

        String sideTexture = hasSide ? findTexture(textures, "_side") : baseTexture;
        String topTexture = hasTop ? findTexture(textures, "_top") : (hasEnd ? findTexture(textures, "_end") : baseTexture);
        String bottomTexture = hasBottom ? findTexture(textures, "_bottom") : topTexture;
        String frontTexture = hasFront ? findTexture(textures, "_front") : sideTexture;

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"parent\": \"minecraft:block/block\",\n");
        sb.append("  \"textures\": {\n");
        sb.append("    \"side\": \"betterthanwolves:block/").append(sideTexture).append("\",\n");
        sb.append("    \"top\": \"betterthanwolves:block/").append(topTexture).append("\",\n");
        sb.append("    \"bottom\": \"betterthanwolves:block/").append(bottomTexture).append("\",\n");
        sb.append("    \"front\": \"betterthanwolves:block/").append(frontTexture).append("\",\n");
        sb.append("    \"particle\": \"betterthanwolves:block/").append(baseTexture).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"elements\": [\n");
        sb.append("    {\n");
        sb.append(String.format(Locale.US, "      \"from\": [%.1f, %.1f, %.1f],\n", fx, fy, fz));
        sb.append(String.format(Locale.US, "      \"to\": [%.1f, %.1f, %.1f],\n", tx, ty, tz));
        sb.append("      \"faces\": {\n");
        sb.append("        \"north\": {\"texture\": \"#front\", \"cullface\": \"north\"},\n");
        sb.append("        \"south\": {\"texture\": \"#side\", \"cullface\": \"south\"},\n");
        sb.append("        \"east\": {\"texture\": \"#side\", \"cullface\": \"east\"},\n");
        sb.append("        \"west\": {\"texture\": \"#side\", \"cullface\": \"west\"},\n");
        sb.append("        \"up\": {\"texture\": \"#top\", \"cullface\": \"up\"},\n");
        sb.append("        \"down\": {\"texture\": \"#bottom\", \"cullface\": \"down\"}\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Generate a multi-element model from exact FCModelBlock geometry.
     * Each ModelBox becomes a separate element in the JSON model.
     */
    private static String generateMultiBoxModel(String baseName, List<String> textures, List<ModelBox> boxes) {
        // Determine textures
        boolean hasTop = textures.stream().anyMatch(t -> t.contains("_top"));
        boolean hasSide = textures.stream().anyMatch(t -> t.contains("_side"));
        boolean hasEnd = textures.stream().anyMatch(t -> t.contains("_end"));
        boolean hasFront = textures.stream().anyMatch(t -> t.contains("_front"));
        boolean hasBottom = textures.stream().anyMatch(t -> t.contains("_bottom"));

        String baseTexture = textures.stream()
                .filter(t -> !t.contains("_"))
                .findFirst().orElse(textures.get(0));

        String sideTexture = hasSide ? findTexture(textures, "_side") : baseTexture;
        String topTexture = hasTop ? findTexture(textures, "_top") : (hasEnd ? findTexture(textures, "_end") : baseTexture);
        String bottomTexture = hasBottom ? findTexture(textures, "_bottom") : topTexture;
        String frontTexture = hasFront ? findTexture(textures, "_front") : sideTexture;

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"parent\": \"minecraft:block/block\",\n");
        sb.append("  \"textures\": {\n");
        sb.append("    \"side\": \"betterthanwolves:block/").append(sideTexture).append("\",\n");
        sb.append("    \"top\": \"betterthanwolves:block/").append(topTexture).append("\",\n");
        sb.append("    \"bottom\": \"betterthanwolves:block/").append(bottomTexture).append("\",\n");
        sb.append("    \"front\": \"betterthanwolves:block/").append(frontTexture).append("\",\n");
        sb.append("    \"particle\": \"betterthanwolves:block/").append(baseTexture).append("\"\n");
        sb.append("  },\n");

        sb.append("  \"elements\": [\n");

        for (int i = 0; i < boxes.size(); i++) {
            ModelBox box = boxes.get(i);

            // Clamp to 0-1 range then scale to 0-16 for MC model format
            double fx = Math.max(0, Math.min(1, box.minX)) * 16;
            double fy = Math.max(0, Math.min(1, box.minY)) * 16;
            double fz = Math.max(0, Math.min(1, box.minZ)) * 16;
            double tx = Math.max(0, Math.min(1, box.maxX)) * 16;
            double ty = Math.max(0, Math.min(1, box.maxY)) * 16;
            double tz = Math.max(0, Math.min(1, box.maxZ)) * 16;

            sb.append("    {\n");
            sb.append(String.format(Locale.US, "      \"from\": [%.1f, %.1f, %.1f],\n", fx, fy, fz));
            sb.append(String.format(Locale.US, "      \"to\": [%.1f, %.1f, %.1f],\n", tx, ty, tz));
            sb.append("      \"faces\": {\n");
            sb.append("        \"north\": {\"texture\": \"#front\"},\n");
            sb.append("        \"south\": {\"texture\": \"#side\"},\n");
            sb.append("        \"east\": {\"texture\": \"#side\"},\n");
            sb.append("        \"west\": {\"texture\": \"#side\"},\n");
            sb.append("        \"up\": {\"texture\": \"#top\"},\n");
            sb.append("        \"down\": {\"texture\": \"#bottom\"}\n");
            sb.append("      }\n");
            sb.append("    }");
            if (i < boxes.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Metadata-aware model generation pass.
     * Uses FC's registerIcons/getIcon to detect blocks that render differently
     * per metadata value, generates per-meta models, and updates blockstate JSONs.
     */
    private static void generateMetadataAwareModels(Path modelsBlockDir, Path blockstatesDir,
            Path texturesBlockDir, Map<Integer, String> allBlockTextures) {

        // Create a capturing IconRegister that produces NamedIcon objects
        btw.modern.IconRegister capturer = new btw.modern.IconRegister() {
            public btw.modern.Icon registerIcon(String name) {
                return new NamedIcon(name);
            }
            public btw.modern.Icon registerIcon(String name, btw.modern.TextureStitched tex) {
                return new NamedIcon(name);
            }
        };

        // Build a set of available texture file names (lowercase, without .png)
        Set<String> availableTextures = new HashSet<>();
        try {
            if (Files.exists(texturesBlockDir)) {
                Files.list(texturesBlockDir)
                    .filter(p -> p.toString().endsWith(".png"))
                    .forEach(p -> availableTextures.add(
                        p.getFileName().toString().replace(".png", "").toLowerCase()));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan textures for metadata pass", e);
            return;
        }

        int blocksWithMetaVariants = 0;
        int metaModelsGenerated = 0;

        for (Map.Entry<Integer, String> entry : allBlockTextures.entrySet()) {
            int blockId = entry.getKey();
            btw.modern.Block block = btw.modern.Block.blocksList[blockId];
            if (block == null) continue;

            // Call registerIcons to populate icon fields with NamedIcon objects
            try {
                block.registerIcons(capturer);
            } catch (Exception e) {
                LOGGER.debug("registerIcons failed for block {}: {}", blockId, e.getMessage());
                continue;
            }

            // For each meta 0-15, collect the 6-side texture name set
            // Side indices: 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east
            // Use a string key built from the 6 icon names to group identical configs
            Map<String, List<Integer>> textureKeyToMetas = new LinkedHashMap<>();
            Map<String, String[]> textureKeyToNames = new LinkedHashMap<>();

            for (int meta = 0; meta < 16; meta++) {
                String[] sideNames = new String[6];
                boolean allValid = true;

                for (int side = 0; side < 6; side++) {
                    try {
                        btw.modern.Icon icon = block.getIcon(side, meta);
                        if (icon instanceof NamedIcon) {
                            sideNames[side] = ((NamedIcon) icon).getIconName();
                        } else if (icon != null && icon.getIconName() != null) {
                            sideNames[side] = icon.getIconName().toLowerCase();
                        } else {
                            allValid = false;
                            break;
                        }
                    } catch (Exception e) {
                        allValid = false;
                        break;
                    }
                }

                if (!allValid) continue;

                // Build a key from all 6 side names
                StringBuilder keyBuilder = new StringBuilder();
                for (int s = 0; s < 6; s++) {
                    if (s > 0) keyBuilder.append("|");
                    keyBuilder.append(sideNames[s]);
                }
                String key = keyBuilder.toString();

                textureKeyToMetas.computeIfAbsent(key, k -> new ArrayList<>()).add(meta);
                textureKeyToNames.putIfAbsent(key, sideNames);
            }

            // If all meta values have the same texture set (or none detected), skip
            if (textureKeyToMetas.size() <= 1) continue;

            blocksWithMetaVariants++;
            LOGGER.debug("Block {} has {} texture variants across metadata values",
                    blockId, textureKeyToMetas.size());

            // Determine the base model name for this block
            String textureBase = entry.getValue();
            String baseModelName = "fcblock" + textureBase;

            // Read existing blockstate JSON
            Path bsPath = blockstatesDir.resolve("block_" + blockId + ".json");
            if (!Files.exists(bsPath)) continue;

            String bsContent;
            try {
                bsContent = new String(Files.readAllBytes(bsPath));
            } catch (IOException e) {
                LOGGER.debug("Failed to read blockstate for block {}: {}", blockId, e.getMessage());
                continue;
            }

            // Generate a model file for each unique texture variant and update blockstate
            int variantIndex = 0;
            for (Map.Entry<String, List<Integer>> variantEntry : textureKeyToMetas.entrySet()) {
                String[] sideNames = textureKeyToNames.get(variantEntry.getKey());
                List<Integer> metas = variantEntry.getValue();

                // Model name: base model for variant 0, base_meta{N} for others
                // Use the first meta value in the group as the variant suffix
                String modelName;
                if (variantIndex == 0) {
                    modelName = baseModelName;
                } else {
                    modelName = baseModelName + "_meta" + metas.get(0);
                }

                // Extract block bounds for correct geometry (slab vs full cube)
                List<ModelBox> modelBoxes = null;
                try {
                    modelBoxes = extractModelBoxes(block);
                } catch (Exception ignored) {}

                double bMinX = 0, bMinY = 0, bMinZ = 0, bMaxX = 1, bMaxY = 1, bMaxZ = 1;
                try {
                    block.setBlockBoundsForItemRender();
                    bMinX = block.minX; bMinY = block.minY; bMinZ = block.minZ;
                    bMaxX = block.maxX; bMaxY = block.maxY; bMaxZ = block.maxZ;
                } catch (Exception ignored) {}

                boolean isFullCube = (modelBoxes == null || modelBoxes.size() <= 1)
                        && bMinX == 0 && bMinY == 0 && bMinZ == 0
                        && bMaxX == 1 && bMaxY == 1 && bMaxZ == 1;

                // Generate model JSON — use bounds-aware generation for non-cube blocks
                String modelJson;
                if (isFullCube) {
                    modelJson = generateModelForSideTextures(sideNames, availableTextures);
                } else {
                    // Generate element-based model with correct bounds AND per-side textures
                    modelJson = generateBoundsAwareModelForSideTextures(
                            sideNames, availableTextures,
                            modelBoxes, bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ);
                }

                Path modelPath = modelsBlockDir.resolve(modelName + ".json");
                try (FileWriter writer = new FileWriter(modelPath.toFile())) {
                    writer.write(modelJson);
                    metaModelsGenerated++;
                } catch (IOException e) {
                    LOGGER.error("Failed to write meta model {} for block {}: {}",
                            modelName, blockId, e.getMessage());
                    continue;
                }

                // Update blockstate JSON: replace model reference for each meta in this group
                for (int meta : metas) {
                    // Replace the model for this specific meta entry
                    // Pattern: "meta=N": {"model": "betterthanwolves:block/ANYTHING"}
                    String metaPattern = "\"meta=" + meta + "\": {\"model\": \"betterthanwolves:block/";
                    int idx = bsContent.indexOf(metaPattern);
                    if (idx >= 0) {
                        int modelStart = idx + metaPattern.length();
                        int modelEnd = bsContent.indexOf("\"", modelStart);
                        if (modelEnd > modelStart) {
                            bsContent = bsContent.substring(0, modelStart) + modelName
                                    + bsContent.substring(modelEnd);
                        }
                    }
                }

                variantIndex++;
            }

            // Write updated blockstate
            try {
                Files.write(bsPath, bsContent.getBytes());
            } catch (IOException e) {
                LOGGER.error("Failed to write updated blockstate for block {}: {}", blockId, e.getMessage());
            }
        }

        LOGGER.info("Metadata-aware pass: {} blocks with per-meta variants, {} model files generated",
                blocksWithMetaVariants, metaModelsGenerated);
    }

    /**
     * Generates a cube model JSON with explicit per-side textures derived from icon names.
     * Side indices: 0=bottom(down), 1=top(up), 2=north, 3=south, 4=west, 5=east
     *
     * Icon names from FC are like "fcBlockLogChewedOak_top" which NamedIcon normalizes
     * to lowercase: "fcblocklogchewedoak_top". The texture reference becomes
     * "betterthanwolves:block/fcblocklogchewedoak_top".
     */
    /**
     * Resolves a texture name to a full resource path.
     * If the texture exists in BTW's assets, uses "betterthanwolves:block/name".
     * Otherwise, assumes it's a vanilla texture and uses "minecraft:block/name".
     */
    private static String resolveTexturePath(String name, Set<String> availableTextures) {
        if (availableTextures.contains(name)) {
            return "betterthanwolves:block/" + name;
        }
        // Try with "fcblock" prefix
        if (availableTextures.contains("fcblock" + name)) {
            return "betterthanwolves:block/fcblock" + name;
        }
        // Fall back to vanilla MC texture
        return "minecraft:block/" + name;
    }

    private static String generateModelForSideTextures(String[] sideNames, Set<String> availableTextures) {
        // Normalize all names to lowercase (NamedIcon already does this, but be safe)
        String[] names = new String[6];
        for (int i = 0; i < 6; i++) {
            names[i] = sideNames[i].toLowerCase();
        }

        // Resolve each texture name to the correct namespace
        String[] resolved = new String[6];
        for (int i = 0; i < 6; i++) {
            resolved[i] = resolveTexturePath(names[i], availableTextures);
        }

        // Check if all 6 sides use the same texture
        boolean allSame = true;
        for (int i = 1; i < 6; i++) {
            if (!resolved[i].equals(resolved[0])) {
                allSame = false;
                break;
            }
        }

        // Check if top+bottom are the same and all 4 lateral sides are the same (column pattern)
        boolean isColumn = resolved[0].equals(resolved[1])     // bottom == top
                && resolved[2].equals(resolved[3])              // north == south
                && resolved[2].equals(resolved[4])              // north == west
                && resolved[2].equals(resolved[5])              // north == east
                && !resolved[0].equals(resolved[2]);            // end != side

        StringBuilder sb = new StringBuilder();
        if (allSame) {
            sb.append("{\n  \"parent\": \"minecraft:block/cube_all\",\n  \"textures\": {");
            sb.append("\"all\": \"").append(resolved[0]).append("\"}\n}");
        } else if (isColumn) {
            sb.append("{\n  \"parent\": \"minecraft:block/cube_column\",\n  \"textures\": {");
            sb.append("\"end\": \"").append(resolved[1]).append("\", ");
            sb.append("\"side\": \"").append(resolved[2]).append("\"}\n}");
        } else {
            // Full per-face specification using cube parent
            sb.append("{\n");
            sb.append("  \"parent\": \"minecraft:block/cube\",\n");
            sb.append("  \"textures\": {\n");
            sb.append("    \"down\": \"").append(resolved[0]).append("\",\n");
            sb.append("    \"up\": \"").append(resolved[1]).append("\",\n");
            sb.append("    \"north\": \"").append(resolved[2]).append("\",\n");
            sb.append("    \"south\": \"").append(resolved[3]).append("\",\n");
            sb.append("    \"west\": \"").append(resolved[4]).append("\",\n");
            sb.append("    \"east\": \"").append(resolved[5]).append("\",\n");
            sb.append("    \"particle\": \"").append(resolved[0]).append("\"\n");
            sb.append("  }\n");
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * Generates a model with correct block geometry AND per-side textures.
     * Used when the metadata pass detects per-meta textures on a non-full-cube block
     * (like dirt slabs, which are half-height but have different textures per metadata).
     */
    private static String generateBoundsAwareModelForSideTextures(
            String[] sideNames, Set<String> availableTextures,
            List<ModelBox> modelBoxes,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

        String[] resolved = new String[6];
        for (int i = 0; i < 6; i++) {
            resolved[i] = resolveTexturePath(sideNames[i].toLowerCase(), availableTextures);
        }

        // Determine elements — use FCModelBlock boxes if available, otherwise single box from bounds
        List<double[]> boxes = new ArrayList<>();
        if (modelBoxes != null && !modelBoxes.isEmpty()) {
            for (ModelBox mb : modelBoxes) {
                boxes.add(new double[]{
                    Math.max(0, Math.min(1, mb.minX)) * 16, Math.max(0, Math.min(1, mb.minY)) * 16,
                    Math.max(0, Math.min(1, mb.minZ)) * 16, Math.max(0, Math.min(1, mb.maxX)) * 16,
                    Math.max(0, Math.min(1, mb.maxY)) * 16, Math.max(0, Math.min(1, mb.maxZ)) * 16
                });
            }
        } else {
            boxes.add(new double[]{
                Math.max(0, Math.min(1, minX)) * 16, Math.max(0, Math.min(1, minY)) * 16,
                Math.max(0, Math.min(1, minZ)) * 16, Math.max(0, Math.min(1, maxX)) * 16,
                Math.max(0, Math.min(1, maxY)) * 16, Math.max(0, Math.min(1, maxZ)) * 16
            });
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"parent\": \"minecraft:block/block\",\n");
        sb.append("  \"textures\": {\n");
        sb.append("    \"down\": \"").append(resolved[0]).append("\",\n");
        sb.append("    \"up\": \"").append(resolved[1]).append("\",\n");
        sb.append("    \"north\": \"").append(resolved[2]).append("\",\n");
        sb.append("    \"south\": \"").append(resolved[3]).append("\",\n");
        sb.append("    \"west\": \"").append(resolved[4]).append("\",\n");
        sb.append("    \"east\": \"").append(resolved[5]).append("\",\n");
        sb.append("    \"particle\": \"").append(resolved[0]).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"elements\": [\n");

        for (int i = 0; i < boxes.size(); i++) {
            double[] b = boxes.get(i);
            sb.append("    {\n");
            sb.append(String.format(Locale.US, "      \"from\": [%.1f, %.1f, %.1f],\n", b[0], b[1], b[2]));
            sb.append(String.format(Locale.US, "      \"to\": [%.1f, %.1f, %.1f],\n", b[3], b[4], b[5]));
            sb.append("      \"faces\": {\n");
            sb.append("        \"down\": {\"texture\": \"#down\"},\n");
            sb.append("        \"up\": {\"texture\": \"#up\"},\n");
            sb.append("        \"north\": {\"texture\": \"#north\"},\n");
            sb.append("        \"south\": {\"texture\": \"#south\"},\n");
            sb.append("        \"west\": {\"texture\": \"#west\"},\n");
            sb.append("        \"east\": {\"texture\": \"#east\"}\n");
            sb.append("      }\n");
            sb.append("    }");
            if (i < boxes.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private static String getTextureBaseName(String name) {
        String nameLower = name.toLowerCase();
        if (nameLower.startsWith("fcblock")) {
            name = name.substring(7);
        } else if (nameLower.startsWith("fcitem")) {
            name = name.substring(6);
        } else if (nameLower.startsWith("fc")) {
            name = name.substring(2);
        }
        name = name.toLowerCase();

        String[] suffixes = {"_side", "_top", "_bottom", "_front", "_back", "_end", "_on", "_off",
                "_lit", "_input", "_output", "_00", "_01", "_02", "_03", "_c00", "_c01",
                "_dry", "_wet", "_drying", "_open", "_closed", "_leg", "_nub", "_open_top",
                "_fast", "_slow", "_idle", "_burning", "_sputtering", "_interior",
                "_nozzle", "_band", "_wide", "_strata", "_mid", "_deep", "_grass",
                "_fertilized", "_unfired", "_cooked", "_charged", "_uncharged", "_snow",
                "_0", "_1", "_2", "_3", "_4", "_5", "_6", "_7", "_8", "_9",
                "_old", "_new", "_dirty", "_burned", "_spit", "_support", "_grown",
                "_contents", "_grate", "_ironbars", "_ladder", "_slats", "_soulsand",
                "_chiseled", "_lines", "_xp", "_item", "_overlay", "_roots", "_half"};

        boolean found;
        do {
            found = false;
            for (String suffix : suffixes) {
                if (name.endsWith(suffix)) {
                    name = name.substring(0, name.length() - suffix.length());
                    found = true;
                    break;
                }
            }
        } while (found && name.length() > 0);

        return name;
    }

    private static String findTexture(List<String> textures, String suffix) {
        return textures.stream()
                .filter(t -> t.toLowerCase().contains(suffix.toLowerCase()))
                .findFirst().orElse(textures.get(0));
    }
}
