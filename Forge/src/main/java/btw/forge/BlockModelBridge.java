package btw.forge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
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
     * Generate element-based block models using FC block bounds.
     * Called after FC initialization when all blocks have their bounds set.
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

        for (Map.Entry<Integer, String> entry : BLOCK_TEXTURES.entrySet()) {
            int blockId = entry.getKey();
            String textureBase = entry.getValue();

            // Get the FC block
            btw.modern.Block fcBlock = btw.modern.Block.blocksList[blockId];
            if (fcBlock == null) continue;

            // Check if texture group exists
            if (!textureGroups.containsKey(textureBase)) continue;

            List<String> textures = textureGroups.get(textureBase);

            // Read block bounds
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
                if (isFullCube) {
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

        LOGGER.info("Generated block models: {} custom-shaped, {} full-cube", customModels, fullCubeModels);
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
