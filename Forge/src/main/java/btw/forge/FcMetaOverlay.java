package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The FC metadata a 1.20.1 block state cannot hold, persisted per dimension (file data/btw_fc_meta.dat of the
 * level's DimensionDataStorage). In 1.5.2 every cell stored 4 metadata bits (Chunk/ExtendedBlockStorage); a
 * 1.20.1 state carries only its properties, which {@link WorldBridge#deriveVanillaMetadata} reads back as FC
 * metadata. What no property expresses lives here: the leaf decay-check flag (bit 8), the sapling growth stage
 * (bits 2-3; STAGE is 0-1), stem growth 0-14 (AGE is 0-7), the pushed-ice bit 8, the jack o'lantern
 * player-placed bit 8, the melon's stem-attachment facing, and for the post-1.5.2 plants FC grows through twins
 * (TwinRegistry) the FC sapling stage of modern saplings and azaleas and the growth levels between the visible AGE
 * steps of beetroots, torchflower and pitcher crops. It used to be an in-memory map cleared at server stop,
 * so all of that was lost on every restart (saplings restarted their growth, flagged leaves never decayed).
 *
 * <p>An entry is {@code (block, derivedAtWrite, meta)}: the block FC wrote to, what derive answered for the state
 * after the write, and FC's metadata. It is valid only while that block is still at the position AND derive still
 * answers {@code derivedAtWrite}: 1.20.1 changes blocks without going through WorldBridge (explosions, player
 * placement, pistons, fluids, bone meal advancing a stem's AGE), and the derived value must then win over a stale
 * one. WorldBridge stores an entry only when FC's metadata differs from the derived value, never for blocks whose
 * state holds all of their metadata.</p>
 *
 * <p>Concurrent: the read path drops stale entries, and FC shape/bounds code can read neighbours off-thread.</p>
 */
final class FcMetaOverlay extends SavedData {

    private static final Logger LOGGER = LogManager.getLogger("BTW-FcMetaOverlay");

    static final String STORAGE_KEY = "btw_fc_meta";

    /** Entry count above which the overlay WARNs once (expected size: a few entries per FC-managed plant/tree). */
    private static final int WARN_ENTRIES = 100_000;

    private static final int FORMAT_VERSION = 1;

    /** Save/load failure sites already logged (save runs on every autosave; it must never throw, triage RE1). */
    private static final Set<String> LOGGED_FAILURES = ConcurrentHashMap.newKeySet();

    /**
     * FC metadata written to a 1.20.1 block that has no ProxyBlock.META: valid only while {@code block} is still
     * the block at its position and WorldBridge.deriveVanillaMetadata still answers {@code derivedAtWrite}.
     */
    record MetaOverride(Block block, int derivedAtWrite, int meta) {}

    /** Packed BlockPos ({@link BlockPos#asLong()}) -> entry. */
    private final Map<Long, MetaOverride> entries = new ConcurrentHashMap<>();

    /** Dimension id, for the log lines. */
    private final String dimension;

    /** The level whose storage owns this overlay (same lifetime); null for the unpersisted fallback. */
    private final ServerLevel level;

    private volatile boolean warnedSize;

    private FcMetaOverlay(ServerLevel level, String dimension) {
        this.level = level;
        this.dimension = dimension;
    }

    /**
     * Loads (or creates) the overlay of a level from its DimensionDataStorage; DimensionDataStorage caches the
     * instance, so every WorldBridge of the level shares it and it is saved with the level. If the storage cannot
     * be used, an unpersisted overlay is returned so FC still keeps its metadata for this session.
     */
    static FcMetaOverlay attach(ServerLevel level) {
        String dimension = "?";
        try {
            dimension = level.dimension().location().toString();
            String dim = dimension;
            return level.getDataStorage().computeIfAbsent(
                    tag -> load(tag, level, dim),
                    () -> new FcMetaOverlay(level, dim),
                    STORAGE_KEY);
        } catch (Throwable t) {
            logOnce("attach " + dimension, t);
            return new FcMetaOverlay(null, dimension);
        }
    }

    /** The entry at {@code pos}, or null. The caller validates it (block + derived value). */
    MetaOverride get(BlockPos pos) {
        return entries.get(pos.asLong());
    }

    void put(BlockPos pos, Block block, int derivedAtWrite, int meta) {
        MetaOverride entry = new MetaOverride(block, derivedAtWrite, meta);
        MetaOverride previous = entries.put(pos.asLong(), entry);
        if (!entry.equals(previous)) {
            setDirty();
        }
        if (!warnedSize && entries.size() > WARN_ENTRIES) {
            warnedSize = true;
            LOGGER.warn("FC metadata overlay for {} holds {} entries (more than {}): stale entries are only dropped "
                    + "when read or saved while their chunk is loaded", dimension, entries.size(), WARN_ENTRIES);
        }
    }

    void remove(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    /** Drops {@code expected} only if it is still the entry at {@code pos} (never a newer write). */
    void remove(BlockPos pos, MetaOverride expected) {
        if (entries.remove(pos.asLong(), expected)) {
            setDirty();
        }
    }

    int size() {
        return entries.size();
    }

    // ================================================================
    // Persistence
    // ================================================================

    private static FcMetaOverlay load(CompoundTag tag, ServerLevel level, String dimension) {
        FcMetaOverlay overlay = new FcMetaOverlay(level, dimension);
        try {
            ListTag paletteTag = tag.getList("Blocks", Tag.TAG_STRING);
            Block[] palette = new Block[paletteTag.size()];
            for (int i = 0; i < palette.length; i++) {
                ResourceLocation id = ResourceLocation.tryParse(paletteTag.getString(i));
                // A block gone from the registry (mod removed) drops its entries: derive answers there now.
                palette[i] = id != null ? BuiltInRegistries.BLOCK.getOptional(id).orElse(null) : null;
            }
            long[] positions = tag.getLongArray("Pos");
            int[] blocks = tag.getIntArray("Block");
            int[] derived = tag.getIntArray("Derived");
            int[] metas = tag.getIntArray("Meta");
            int count = Math.min(Math.min(positions.length, blocks.length), Math.min(derived.length, metas.length));
            for (int i = 0; i < count; i++) {
                int index = blocks[i];
                Block block = index >= 0 && index < palette.length ? palette[index] : null;
                if (block != null) {
                    overlay.entries.put(positions[i], new MetaOverride(block, derived[i], metas[i]));
                }
            }
        } catch (Throwable t) {
            logOnce("load " + dimension, t);
        }
        LOGGER.info("FC metadata overlay for {}: {} entries loaded", dimension, overlay.entries.size());
        if (overlay.entries.size() > WARN_ENTRIES) {
            overlay.warnedSize = true;
            LOGGER.warn("FC metadata overlay for {} holds {} entries (more than {})",
                    dimension, overlay.entries.size(), WARN_ENTRIES);
        }
        return overlay;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        try {
            pruneLoaded();
            Map<Block, Integer> paletteIndex = new IdentityHashMap<>();
            ListTag paletteTag = new ListTag();
            List<Long> positions = new ArrayList<>(entries.size());
            List<Integer> blocks = new ArrayList<>(entries.size());
            List<Integer> derived = new ArrayList<>(entries.size());
            List<Integer> metas = new ArrayList<>(entries.size());
            for (Map.Entry<Long, MetaOverride> e : entries.entrySet()) {
                MetaOverride entry = e.getValue();
                Integer index = paletteIndex.get(entry.block());
                if (index == null) {
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(entry.block());
                    index = paletteTag.size();
                    paletteIndex.put(entry.block(), index);
                    paletteTag.add(StringTag.valueOf(id.toString()));
                }
                positions.add(e.getKey());
                blocks.add(index);
                derived.add(entry.derivedAtWrite());
                metas.add(entry.meta());
            }
            tag.putInt("Format", FORMAT_VERSION);
            tag.put("Blocks", paletteTag);
            tag.putLongArray("Pos", positions);
            tag.putIntArray("Block", blocks);
            tag.putIntArray("Derived", derived);
            tag.putIntArray("Meta", metas);
        } catch (Throwable t) {
            logOnce("save " + dimension, t);
        }
        return tag;
    }

    /**
     * Drops the entries of loaded chunks that are no longer valid, by the same rule as the read path
     * (WorldBridge.getBlockMetadata): blocks 1.20.1 replaced or changed while FC never read them again would
     * otherwise be saved forever. Unloaded chunks are left alone (nothing is loaded for this). Server thread only:
     * SavedData is saved from ServerLevel.save; getChunkNow answers null on any other thread. Runs only on a save of
     * a dirty overlay: SavedData.save(File) skips save(CompoundTag) unless isDirty(), so an overlay with no put or
     * remove since its last save keeps its stale entries, in memory and on disk, until the next one. That is
     * harmless (the read path validates every entry and drops a stale one, marking the overlay dirty), but the
     * cleanup and the WARN_ENTRIES bound depend on later writes.
     */
    private void pruneLoaded() {
        ServerLevel live = level;
        if (live == null) return;
        for (Map.Entry<Long, MetaOverride> e : entries.entrySet()) {
            long packed = e.getKey();
            LevelChunk chunk = live.getChunkSource().getChunkNow(
                    BlockPos.getX(packed) >> 4, BlockPos.getZ(packed) >> 4);
            if (chunk == null) continue;
            BlockPos pos = BlockPos.of(packed);
            BlockState state = chunk.getBlockState(pos);
            MetaOverride entry = e.getValue();
            // FCMOD-INTEGRATION(1.20.1): strata: a strata cell's entry outlives a strata change (WorldBridge.getBlockMetadata reads it with the position's strata)
            if (entry.block() != state.getBlock()
                    || (WorldBridge.deriveVanillaMetadata(live, state, pos) != entry.derivedAtWrite()
                        && !StrataRule.isStrataBlock(state.getBlock()))) {
                entries.remove(packed, entry);   // never a newer write
            }
        }
    }

    private static void logOnce(String what, Throwable t) {
        if (LOGGED_FAILURES.add(what)) {
            LOGGER.error("FC metadata overlay: {} failed; continuing without it (logged once)", what, t);
        }
    }
}
