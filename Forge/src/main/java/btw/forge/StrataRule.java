package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.B's stone strata on 1.20.1 blocks. FC keeps a stone or ore block's strata in metadata bits 0-1
 * (FCBlockStone.GetStrata, FCBlockOre.GetStrata: hardness, explosion resistance, harvest and efficient tool levels, the
 * rough stone a conversion leaves), and 4.B wrote them only at generation: stone per column (vanilla/server
 * ChunkProviderGenerate.java:687-720: deep for y <= 24 + rand(2), mid for y <= 48 + rand(2), Overworld only) and the
 * ores per block (WorldGenMinable.java:86-104). The port runs no 4.B generation, so this class derives the strata at
 * read time from the block and its position. WorldBridge.deriveVanillaMetadata calls it on both sides, so the client's
 * dig prediction (FCMiningSpeed) reads what the server reads. Pure and stateless after its one-time id table: any
 * thread (server, render, chunk-mesh).
 */
// FCMOD-INTEGRATION(1.20.1): strata: 4.B strata at 4.B heights from the position (ruling 2026-10-21); deepslate forms are deep
public final class StrataRule {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Strata");

    static final int TOP = 0, MID = 1, DEEP = 2;
    /** The FC metadata bits that hold the strata (meta & 3). */
    static final int STRATA_BITS = 3;
    /** 1.5.2 Block.stone: FCBlockStone keeps strata in bits 0-1 though its HasStrata() is false. */
    private static final int STONE_ID = 1;
    /** 4.B's strata tops (ChunkProviderGenerate.java:696, :708; WorldGenMinable.java:91, :95). */
    private static final int DEEP_TOP = 24, MID_TOP = 48;
    /** The legacy ids some 1.20.1 block reads as and that are strata blocks, for the audit. */
    private static final List<Integer> EXPECTED_MAPPED = List.of(1, 14, 15, 16, 21, 56, 73, 129);

    /** By legacy id: true for a strata block. Null until FC is ready; built once. */
    private static volatile boolean[] strataIds;

    private StrataRule() {}

    /**
     * The 4.B strata (0 top, 1 mid, 2 deep) of {@code state} at {@code pos} when its block is a strata block, else -1.
     * Deepslate forms are always deep; outside the Overworld everything is top (4.B wrote strata in dimension 0 only).
     */
    public static int strataAt(BlockGetter level, BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (!isStrataBlock(block)) return -1;
        if (ProxyRegistry.isDeepslateForm(block)) return DEEP;
        if (!strataDimension(level)) return TOP;
        return ProxyRegistry.getBlockId(block) == STONE_ID
                ? stoneStrata(pos.getX(), pos.getY(), pos.getZ())
                : oreStrata(pos.getX(), pos.getY(), pos.getZ());
    }

    /** 4.B GenerateStrata: one deep edge and one mid edge per column. */
    static int stoneStrata(int x, int y, int z) {
        if (y <= DEEP_TOP + edgeBit(x, 0, z, 1)) return DEEP;
        return y <= MID_TOP + edgeBit(x, 0, z, 2) ? MID : TOP;
    }

    /** 4.B WorldGenMinable: mid by one roll, then deep by a second, per block. */
    static int oreStrata(int x, int y, int z) {
        if (y > MID_TOP + edgeBit(x, y, z, 3)) return TOP;
        return y <= DEEP_TOP + edgeBit(x, y, z, 4) ? DEEP : MID;
    }

    /**
     * 4.B's rand(2) as a pure function of the position: the low bit of a 64-bit mix (MurmurHash3 fmix64) of x, y, z
     * and a salt. No world seed: the client has none, and both sides must agree.
     */
    static int edgeBit(int x, int y, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ y * 0x165667B19E3779F9L
                ^ salt * 0xD6E8FEB86659FD93L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (int) (h & 1L);
    }

    /** True for a 1.20.1 block that reads as a strata block: stone (1), an FC strata ore, or a deepslate form of them. */
    public static boolean isStrataBlock(Block block) {
        if (block == null || block instanceof ProxyBlock) return false;
        boolean[] ids = strataIds();
        if (ids == null) return false;
        int id = ProxyRegistry.getBlockId(block);
        return id > 0 && id < ids.length && ids[id];
    }

    /** {@code meta} with the strata bits of a strata block taken from {@code derived}: FC writes never move them. */
    public static int withPositionBits(Block block, int meta, int derived) {
        return isStrataBlock(block) ? positionBits(meta, derived) : meta;
    }

    /** {@code meta}'s FC bits above the strata, with the strata bits of {@code derived} (the caller knows it is a strata block). */
    static int positionBits(int meta, int derived) {
        return (meta & ~STRATA_BITS) | (derived & STRATA_BITS);
    }

    private static boolean[] strataIds() {
        boolean[] ids = strataIds;
        if (ids != null) return ids;
        if (!BTWLifecycle.isFcReady()) return null;   // never cached before FC init
        synchronized (StrataRule.class) {
            if (strataIds == null) strataIds = buildIds();
            return strataIds;
        }
    }

    private static boolean[] buildIds() {
        btw.modern.Block[] list = btw.modern.Block.blocksList;
        boolean[] ids = new boolean[list.length];
        for (int id = 1; id < list.length; id++) {
            btw.modern.Block fc = list[id];
            if (fc == null) continue;
            try {
                ids[id] = id == STONE_ID
                        || (fc.HasStrata() && fc.GetMetadataConversionForStrataLevel(MID, 0) == MID
                            && fc.GetMetadataConversionForStrataLevel(DEEP, 0) == DEEP);
            } catch (Throwable t) {
                ids[id] = false;   // a block whose strata hooks throw has no strata
            }
        }
        return ids;
    }

    /**
     * 4.B wrote strata in the Overworld only. A getter that is not a Level (FCBakedModel's RenderChunkRegion snapshot on
     * the chunk-mesh threads) has no dimension here and takes the Overworld rule: in the Nether or the End such a read
     * of a stone or ore cell answers its height's strata where the server answers top. No FC render code reads a
     * stone or ore neighbour's strata today; a client feature that does (strata textures) must pass the client level.
     */
    static boolean strataDimension(BlockGetter level) {
        if (level instanceof Level l) return l.dimension() == Level.OVERWORLD;
        if (level instanceof WorldGenLevel w) return w.getLevel().dimension() == Level.OVERWORLD;
        return true;
    }

    // ---------------------------------------------------------------- self-audit

    /** The nine deepslate forms and the FC id each reads as (ProxyRegistry.initVanillaBlockMap). */
    private static Object[][] deepslateForms() {
        return new Object[][]{
                {Blocks.DEEPSLATE, 1}, {Blocks.COBBLED_DEEPSLATE, 4}, {Blocks.DEEPSLATE_GOLD_ORE, 14},
                {Blocks.DEEPSLATE_IRON_ORE, 15}, {Blocks.DEEPSLATE_COAL_ORE, 16}, {Blocks.DEEPSLATE_LAPIS_ORE, 21},
                {Blocks.DEEPSLATE_DIAMOND_ORE, 56}, {Blocks.DEEPSLATE_REDSTONE_ORE, 73},
                {Blocks.DEEPSLATE_EMERALD_ORE, 129}};
    }

    /** The canonical 1.20.1 block of each strata id and of cobblestone: FC's writes of these ids place them. */
    private static Object[][] canonical() {
        return new Object[][]{
                {1, Blocks.STONE}, {4, Blocks.COBBLESTONE}, {14, Blocks.GOLD_ORE}, {15, Blocks.IRON_ORE},
                {16, Blocks.COAL_ORE}, {21, Blocks.LAPIS_ORE}, {56, Blocks.DIAMOND_ORE}, {73, Blocks.REDSTONE_ORE},
                {129, Blocks.EMERALD_ORE}};
    }

    /**
     * BridgeSelfAudit.runCommon (FMLCommonSetupEvent, FC ready): the strata ids, the deepslate forms and their item
     * identity, the canonical blocks, the height rule, the counts-as items and the position-aware blast resistance
     * hook. One INFO line, a WARN per mismatch.
     */
    static void audit() {
        int warnings = 0;
        boolean[] ids = strataIds();
        if (ids == null) {
            LOGGER.warn("[SELF-AUDIT/strata] FC is not ready: no strata ids (StrataRule reads every stone and ore as top)");
            return;
        }
        List<Integer> mapped = new ArrayList<>();
        for (int id = 1; id < ids.length; id++) {
            if (ids[id] && ProxyRegistry.isLegacyIdMapped(id) && ProxyRegistry.getProxy(id) == null) mapped.add(id);
        }
        if (!mapped.equals(EXPECTED_MAPPED)) {
            LOGGER.warn("[SELF-AUDIT/strata] mapped strata ids {} (expected {})", mapped, EXPECTED_MAPPED);
            warnings++;
        }
        btw.modern.Block stone = btw.modern.Block.blocksList[STONE_ID];
        if (stone == null || !stone.getClass().getSimpleName().equals("FCBlockStone")) {
            LOGGER.warn("[SELF-AUDIT/strata] blocksList[1] is {} (expected FCBlockStone)",
                    stone == null ? "empty" : stone.getClass().getName());
            warnings++;
        }
        int deep = 0;
        for (Object[] row : deepslateForms()) {
            Block block = (Block) row[0];
            int id = (Integer) row[1];
            boolean strata = id != 4;
            if (ProxyRegistry.getBlockId(block) != id || !ProxyRegistry.isDeepslateForm(block)
                    || isStrataBlock(block) != strata || ProxyRegistry.getBlockFormItemId(block) != 0) {
                LOGGER.warn("[SELF-AUDIT/strata] {} reads as {} (deepslate form {}, strata block {}, item id {});"
                                + " expected {}, a deepslate form, strata block {}, item id 0", block,
                        ProxyRegistry.getBlockId(block), ProxyRegistry.isDeepslateForm(block), isStrataBlock(block),
                        ProxyRegistry.getBlockFormItemId(block), id, strata);
                warnings++;
            } else if (strata) {
                deep++;
            }
        }
        for (Object[] row : canonical()) {
            int id = (Integer) row[0];
            if (ProxyRegistry.getModernBlock(id) != row[1]) {
                LOGGER.warn("[SELF-AUDIT/strata] FC id {} places {} (expected {})", id, ProxyRegistry.getModernBlock(id),
                        row[1]);
                warnings++;
            }
        }
        int[][] heights = {{24, DEEP}, {26, MID}, {48, MID}, {50, TOP}, {-64, DEEP}};
        for (int[] h : heights) {
            if (stoneStrata(0, h[0], 0) != h[1] || oreStrata(0, h[0], 0) != h[1]) {
                LOGGER.warn("[SELF-AUDIT/strata] y={} reads stone {} / ore {} (expected {})", h[0],
                        stoneStrata(0, h[0], 0), oreStrata(0, h[0], 0), h[1]);
                warnings++;
            }
        }
        int ironChunk = btw.forge.compat.FcRefs.legacyItemId("fcItemChunkIronOre");
        int goldChunk = btw.forge.compat.FcRefs.legacyItemId("fcItemChunkGoldOre");
        int[] rawIron = LegacyVariants.toLegacy(Items.RAW_IRON);
        int[] rawGold = LegacyVariants.toLegacy(Items.RAW_GOLD);
        if (rawIron == null || rawIron[0] != ironChunk || rawGold == null || rawGold[0] != goldChunk) {
            LOGGER.warn("[SELF-AUDIT/strata] raw iron / raw gold read as {} / {} (expected the chunks {} / {})",
                    rawIron == null ? "nothing" : rawIron[0], rawGold == null ? "nothing" : rawGold[0], ironChunk,
                    goldChunk);
            warnings++;
        }
        boolean blastHook = false;
        for (java.lang.reflect.Method m : ExplosionDamageCalculator.class.getDeclaredMethods()) {
            if (m.getName().endsWith("btw$fcPositionalResistance")) blastHook = true;
        }
        if (!blastHook) {
            LOGGER.warn("[SELF-AUDIT/strata] ExplosionDamageCalculatorMixin is not applied: 1.20.1 explosions read"
                    + " every stone and ore as top strata");
            warnings++;
        }
        LOGGER.info("[SELF-AUDIT/strata] mapped strata ids {}; {} deepslate forms ({} deep, cobbled deepslate -> 4);"
                        + " raw_iron -> {}, raw_gold -> {}; blast resistance by position {}; {} warnings (0 expected)",
                mapped, deepslateForms().length, deep, rawIron == null ? "nothing" : rawIron[0],
                rawGold == null ? "nothing" : rawGold[0], blastHook ? "on" : "OFF", warnings);
    }
}
