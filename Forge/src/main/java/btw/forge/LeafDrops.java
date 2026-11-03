package btw.forge;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.loot.LootTableIdCondition;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * BTW's leaf rule on every loot path (INTEGRATIONS #035): a {@code #minecraft:leaves} block drops what FCBlockLeaves
 * drops (Server FCBlockLeaves.java:62-85), whichever path rolls its loot table. Shears and silk touch give the leaves;
 * anything else gives the species' sapling at 1 in 20 (1 in 40 for jungle), never sticks or apples, with no Fortune
 * bonus and no explosion penalty (FC reads neither).
 *
 * <ul>
 *   <li><b>Dual-sourced.</b> FC's own paths (FC harvest through ServerPlayerGameModeMixin, FCBlockLeaves.updateTick
 *       decay, FC machines, the block dispenser) never roll a table: they run FCBlockLeaves on the 1.5.2 drop members
 *       of Modern-Common BlockLeaves. This rule covers the paths that roll a table: 1.20.1 player breaks
 *       (playerDestroy) and decay of leaves with no FC identity, 1.20.1 explosions of leaves FC has no blast rule
 *       for (FC's own leaves, legacy 18, follow FCBlockLeaves on every blast: BlastRules, fc_blast_rules; FC's mining
 *       charge reaches FCBlockLeaves through Block.DropItemsOnDestroyedByExplosion), stand-in machine drops
 *       (ModernBlockStandIn.dropMcLoot) and foreign
 *       {@code destroyBlock(pos, true)}. getDrops is not bridged for vanilla-mapped FC blocks (BlockBehaviorMixin), and
 *       no FC path rolls a table, so nothing drops twice.</li>
 *   <li><b>Who decides the sapling.</b> A registered {@link Hook} first (Dynamic Trees, #027: the species' seed, DT's
 *       seasonal chance as a veto). Otherwise, only for the block's own table: FC identity (legacy id 18, oak, spruce,
 *       birch and jungle) takes FC's idDropped / damageDropped through the item bridge; any other leaves block takes
 *       the same-namespace member of {@code #minecraft:saplings} named {@code <stem>_sapling}, else {@code <stem>},
 *       else the only {@code <stem>_*} (acacia, dark oak and cherry saplings, azalea, flowering azalea, the mangrove
 *       propagule). A block it cannot resolve keeps its own loot, with one INFO line.</li>
 *   <li><b>Strip.</b> Saplings, the resolved sapling's item, {@code #forge:rods/wooden}, the hook's byproducts and, for
 *       minecraft-namespace leaves only, apples (oak and dark oak; FCBlockLeaves.java:65 "get rid of apple drop on
 *       oak"). Everything else in the loot stays: modded fruit, other mods' GLM additions.</li>
 * </ul>
 *
 * <p>Order with other GLMs does not matter for this rule's own output: it only removes leaf byproducts and adds one
 * sapling. It never returns early on empty loot (most leaf rolls yield nothing and FC still rolls), and skips
 * {@code LootTable.EMPTY} rolls, whose queried id is {@code forge:unknown_loot_table} (LootTable.java:37,
 * LootContext.java:107-108; DT's LeavesProperties.NULL during a fell). Every failure logs one WARN and keeps the
 * table's loot.</p>
 */
// FCMOD-INTEGRATION(1.20.1): leaves_drop_like_fc: one rule for every leaves block on every loot path, FCBlockLeaves's sapling roll; integrations register hooks with Minecraft types only
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class LeafDrops {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Leaves");

    /** FCBlockLeaves's sapling odds (Server FCBlockLeaves.java:71-76): 1 in 20, 1 in 40 for wood species 3. */
    private static final int FC_ODDS = 20;
    private static final int FC_JUNGLE_ODDS = 40;
    /** FC wood species / type 3, jungle. */
    private static final int FC_JUNGLE = 3;
    /** 1.5.2 Block.leaves, which FCBlockLeaves replaces in blocksList. */
    private static final int LEGACY_LEAVES_ID = 18;
    private static final String LEAVES_SUFFIX = "_leaves";

    /**
     * How a leaves roll is rewritten: {@code sapling} (one, at FC's odds; EMPTY rolls nothing), the FC wood species
     * that picks the odds ({@code 3} = jungle, -1 for none), {@code allows} (a veto tested after FC's roll succeeds)
     * and extra byproduct items to strip.
     */
    public record Resolution(ItemStack sapling, int fcSpecies, Predicate<LootContext> allows, Set<Item> byproducts) {}

    /** Leave the table's loot as it is (compared by identity). */
    public static final Resolution KEEP = new Resolution(ItemStack.EMPTY, -1, ctx -> false, Set.of());

    private static final Predicate<LootContext> ALWAYS = ctx -> true;

    /** Decides a leaves roll for the states it owns; return {@link #KEEP} to leave that roll alone. */
    @FunctionalInterface
    public interface Hook {
        Resolution resolve(BlockState leaves, LootContext ctx);
    }

    private record Entry(Predicate<BlockState> owns, Hook hook) {}

    private static final CopyOnWriteArrayList<Entry> HOOKS = new CopyOnWriteArrayList<>();
    /** Static resolutions by block; cleared on every server tag load. */
    private static final Map<Block, Resolution> STATIC = new ConcurrentHashMap<>();
    /** FC's idDropped takes a Random; FCBlockLeaves's (BlockLeaves.idDropped) never reads it. */
    private static final Random FC_RANDOM = new Random();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private LeafDrops() {}

    /**
     * Registers {@code hook} for the leaves states {@code owns} accepts; the first registered match wins, for any
     * table rolled with that state. Safe during mod construction: it touches no registry.
     */
    public static void register(Predicate<BlockState> owns, Hook hook) {
        HOOKS.add(new Entry(Objects.requireNonNull(owns, "owns"), Objects.requireNonNull(hook, "hook")));
        LOGGER.info("[LEAVES] leaf drop hook registered: {}", hook);
    }

    /** The GLM body (FCLeavesLootModifier): returns {@code loot} unchanged unless this is a leaves roll it decides. */
    public static ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        BlockState state = null;
        try {
            state = ctx.getParamOrNull(LootContextParams.BLOCK_STATE);
            if (state == null || !state.is(BlockTags.LEAVES)) return loot;
            ResourceLocation queried = ctx.getQueriedLootTableId();
            if (LootTableIdCondition.UNKNOWN_LOOT_TABLE.equals(queried)) return loot;

            Resolution r = resolve(state, queried, ctx);
            if (r == KEEP) return loot;

            Block block = state.getBlock();
            Item own = block.asItem();
            for (ItemStack s : loot) {
                // shears or silk touch: 4.B and 1.20.1 agree (1.5.2 BlockLeaves.harvestBlock :250-256, Block.harvestBlock)
                if (s.is(ItemTags.LEAVES) || (own != Items.AIR && s.is(own))) return loot;
            }

            boolean stripApples = "minecraft".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
            Item sapling = r.sapling().isEmpty() ? null : r.sapling().getItem();
            ObjectArrayList<ItemStack> out = new ObjectArrayList<>(loot.size() + 1);
            for (ItemStack s : loot) {
                boolean byproduct = s.is(ItemTags.SAPLINGS) || (sapling != null && s.is(sapling))
                        || s.is(Tags.Items.RODS_WOODEN) || r.byproducts().contains(s.getItem())
                        || (stripApples && s.is(Items.APPLE));
                if (!byproduct) out.add(s);
            }
            int odds = r.fcSpecies() == FC_JUNGLE ? FC_JUNGLE_ODDS : FC_ODDS;
            if (sapling != null && ctx.getRandom().nextInt(odds) == 0 && r.allows().test(ctx)) {
                out.add(r.sapling().copyWithCount(1));
            }
            return out;
        } catch (RuntimeException | LinkageError t) {
            if (WARNED.compareAndSet(false, true)) {
                LOGGER.warn("[LEAVES] the leaf drop rule failed for {}; the table's loot is kept (logged once)",
                        state, t);
            }
            return loot;
        }
    }

    private static Resolution resolve(BlockState state, ResourceLocation queried, LootContext ctx) {
        for (Entry e : HOOKS) {
            if (e.owns().test(state)) {
                Resolution r = e.hook().resolve(state, ctx);
                return r != null ? r : KEEP;
            }
        }
        Block block = state.getBlock();
        // another mod's table rolled with a leaves state is not leaf loot
        if (!queried.equals(block.getLootTable())) return KEEP;
        return staticResolution(block);
    }

    private static Resolution staticResolution(Block block) {
        Resolution cached = STATIC.get(block);
        if (cached != null) return cached;
        int legacyId = ProxyRegistry.getBlockId(block);
        Resolution r;
        if (legacyId == LEGACY_LEAVES_ID) {
            if (!BTWLifecycle.isFcReady()) return KEEP; // never cached: FC's drop is known only once FC is up
            r = fcResolution(block);
        } else if (legacyId != 0) {
            // another FC identity owns its drops through FC; its table paths are the general getDrops bridge's
            LOGGER.info("[LEAVES] {}: FC block {} owns its drops; its own leaf loot is kept",
                    BuiltInRegistries.BLOCK.getKey(block), legacyId);
            r = KEEP;
        } else {
            r = nameResolution(block);
        }
        STATIC.put(block, r);
        return r;
    }

    /**
     * FC identity: FCBlockLeaves's own sapling for this species (idDropped / damageDropped, Modern-Common BlockLeaves),
     * converted as FC's drops are (ItemStackHelper.toMcStack). A result outside {@code #minecraft:saplings} is never
     * minted: one WARN, and the table's loot stays.
     */
    private static Resolution fcResolution(Block block) {
        int meta = Math.max(ProxyRegistry.getLegacyVariantMeta(block), 0);
        btw.modern.Block fc = btw.modern.Block.blocksList[LEGACY_LEAVES_ID];
        ItemStack sapling = fc == null ? ItemStack.EMPTY : ItemStackHelper.toMcStack(new btw.modern.ItemStack(
                fc.idDropped(meta, FC_RANDOM, 0), 1, fc.damageDropped(meta)));
        if (sapling.isEmpty() || !sapling.is(ItemTags.SAPLINGS)) {
            LOGGER.warn("[LEAVES] {}: FC's leaves drop {}, which is not a sapling; its own leaf loot is kept",
                    BuiltInRegistries.BLOCK.getKey(block), sapling);
            return KEEP;
        }
        return new Resolution(sapling.copyWithCount(1), meta & 3, ALWAYS, Set.of());
    }

    /** The name rule: the same-namespace {@code #minecraft:saplings} item for {@code <stem>_leaves}. */
    private static Resolution nameResolution(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        String path = key.getPath();
        Item found = null;
        if (path.endsWith(LEAVES_SUFFIX) && path.length() > LEAVES_SUFFIX.length()) {
            String stem = path.substring(0, path.length() - LEAVES_SUFFIX.length());
            Item named = null;
            Item bare = null;
            Set<Item> prefixed = new HashSet<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.SAPLINGS)) {
                if (!holder.isBound()) continue;
                Item item = holder.value();
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (!key.getNamespace().equals(id.getNamespace())) continue;
                String p = id.getPath();
                if (p.equals(stem + "_sapling")) named = item;
                else if (p.equals(stem)) bare = item;
                else if (p.startsWith(stem + "_")) prefixed.add(item);
            }
            found = named != null ? named : bare != null ? bare
                    : prefixed.size() == 1 ? prefixed.iterator().next() : null;
        }
        if (found == null) {
            LOGGER.info("[LEAVES] {}: no sapling in #minecraft:saplings; its own leaf loot is kept", key);
            return KEEP;
        }
        return new Resolution(new ItemStack(found), -1, ALWAYS, Set.of());
    }

    /** Server tag loads (world start, /reload) rebind #minecraft:saplings: resolve again. */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) STATIC.clear();
    }
}
