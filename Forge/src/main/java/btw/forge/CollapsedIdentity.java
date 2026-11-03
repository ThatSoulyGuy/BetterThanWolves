package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FCMOD-INTEGRATION(1.20.1): collapsed_identity (triage CF2-DR3, CF2-P8c; flowers default 2026-10-21)
 *
 * <p><b>Feature:</b> 1.20.1 blocks that share one 1.5.2 id with other blocks: the ten small flowers beside the poppy
 * that BTW reads as the rose 38 (blue orchid, allium, azure bluet, the four tulips, oxeye daisy, cornflower, lily of
 * the valley), wet sponge (19), the stone slab and smooth sandstone (44:0 and 24:2 spellings), the skull types (144),
 * and the colour, species and type variants whose 1.5.2 subtype the port's Modern-Common bases do not drop (wool 35,
 * sandstone 24, stone and wooden slabs 44 / 126, the mossy cobblestone wall 139, the quartz forms 155).</p>
 *
 * <p><b>Conflict with FC:</b> FC drops a block as {@code ItemStack(idDropped, 1, damageDropped(meta))}
 * (Modern-Common Block.dropBlockAsItemWithChance, harvestBlock, createStackedBlock). The stack's damage is FC's
 * subtype, and ItemStackHelper.toMcStack turns it back into an item: when it is 0 where the broken 1.20.1 block is
 * another member of the id, the drop is that id's base item. An oxeye daisy drops the poppy (FCBlockFlowerBlossom keeps
 * Block's damageDropped 0), orange wool white wool (Modern-Common BlockCloth has no damageDropped; 1.5.2
 * BlockCloth.java:27-30 returned the metadata), a wither skeleton skull a skeleton skull (FCBlockSkull lost 1.5.2
 * BlockSkull's idDropped / damageDropped), wet sponge a dry sponge (one 1.5.2 subtype).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend). FC's drop code runs unchanged (whether, how many, fortune, silk touch,
 * the harvest's tool and hunger rules); at the one funnel every FC block drop passes, Block.dropBlockAsItem_do, a
 * self-drop (the stack is the dropping block's own id, or the 1.20.1 block's legacy id: the double slabs 43 / 125
 * drop 44 / 126) whose item is its id's base item while the 1.20.1 block at the cell has another item of its own
 * becomes that block's own item: its FC stack when FC can hold it (38:8 for the daisy, 35:1 for orange wool, 397:1 for
 * the wither skull: FC's EntityItem goes on), else the 1.20.1 item dropped at the cell (Block.popResource: wet sponge,
 * stone slab, smooth sandstone). The cell's block is the live one (FC's pops drop before they remove:
 * BlockFlower.checkFlowerChange, the saw, explosions) or, for a player's harvest, which removes first
 * (ItemInWorldManager.tryHarvestBlock), the state this thread's harvest record holds
 * (ServerPlayerGameModeMixin.btw$destroyBlock opens it around tryHarvestBlock).</p>
 *
 * <p><b>Rebalance:</b> none. 4.B drops every subtyped block as its own subtype (1.5.2 damageDropped,
 * createStackedBlock with getHasSubtypes, vanilla/server Block.java:1192-1202); the rule restores that and gives the
 * 1.20.1-only members their own item.</p>
 *
 * <p><b>Injection:</b> no injector. Modern-Common World.ResolveModernSelfDrop (base: the stack, unchanged), overridden
 * by WorldBridge, which calls {@link #resolve}; the harvest record is a body edit of an existing mixin method. FC's
 * deliberate subtypes are left alone: a drop that is not its id's base item (a strata subtype, a species FC already
 * carries) is never touched, nor is a drop of another id (loose stone bricks, seeds, the bark, metal fragments), a
 * tagged stack, a ProxyBlock's drop, a twin's or a ruled bed's or door's (vanilla_pair_drops), or a block another
 * integration declares {@link #exempt}. {@link #audit} names every member at server start.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> (1) Every collapsed member's 1.20.1 loot (DropModernBlockLoot, as
 * twins and ruled beds do): it drops a whole loot table where FC decided one item, skips FC's count and silk-touch
 * choice, and needs the live block, which a player's harvest has already removed. (2) Skipping FC's harvest for
 * non-canonical members, as twins do: it loses FC's harvest hunger, tool rules and animal panic, and fixes neither the
 * pops nor FC's other drop paths. (3) A damageDropped override per class (D7): it fixes only the variants a 1.5.2
 * class carried, not the 1.20.1-only members (wet sponge, the stone slab, smooth sandstone, the flowers, whose 4.B
 * class has no subtypes at all).</p>
 *
 * <p>Server thread only. Never throws into FC: WorldBridge catches, logs once and keeps FC's stack.</p>
 */
public final class CollapsedIdentity {

    private static final Logger LOGGER = LogManager.getLogger("BTW-CollapsedIdentity");

    /** The rose's legacy id: the flower subtypes 38:0-10 (ProxyRegistry / LegacyVariants flower_subtypes rows). */
    private static final int FLOWER_ID = 38;
    /**
     * 38:0-10: 1.7.2's red_flower order for 0-8, then the port-only 9 (cornflower) and 10 (lily of the valley); the
     * audit's W1 / W2 expectations.
     */
    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET,
            Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY};

    /** The block a player's FC harvest is removing, for the drop that follows the removal. */
    private record Harvest(ServerLevel level, BlockPos pos, BlockState state) {}

    private static final ThreadLocal<Harvest> HARVEST = new ThreadLocal<>();
    /** Blocks whose FC drop another integration declares authoritative (none today). */
    private static final Set<Block> EXEMPT = ConcurrentHashMap.newKeySet();

    private CollapsedIdentity() {}

    /**
     * Opens this thread's harvest record for the FC harvest of {@code state} at {@code pos}; returns the enclosing
     * record, which {@link #endHarvest} restores (a harvest nested in another one keeps both). Public: called from
     * btw.forge.mixin.ServerPlayerGameModeMixin.
     */
    public static Object beginHarvest(ServerLevel level, BlockPos pos, BlockState state) {
        Harvest previous = HARVEST.get();
        if (level != null && pos != null && state != null) {
            HARVEST.set(new Harvest(level, pos.immutable(), state));
        }
        return previous;
    }

    /** Closes the record {@link #beginHarvest} opened: always in a finally. */
    public static void endHarvest(Object previous) {
        if (previous instanceof Harvest h) {
            HARVEST.set(h);
        } else {
            HARVEST.remove();
        }
    }

    /** Leaves {@code blocks} out of the rule: their FC drop is authoritative. Call during mod setup. */
    public static void exempt(Block... blocks) {
        for (Block b : blocks) {
            if (b != null) EXEMPT.add(b);
        }
    }

    /**
     * The stack FC's drop of {@code stack} by the FC block {@code blockID} at {@code pos} becomes: {@code stack}
     * unchanged, the FC stack of the cell's block's own item, or null after this method dropped that 1.20.1 item
     * itself. Called by WorldBridge.ResolveModernSelfDrop for every FC block drop (Modern-Common
     * Block.dropBlockAsItem_do).
     */
    static btw.modern.ItemStack resolve(ServerLevel level, BlockPos pos, int blockID, btw.modern.ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || stack.itemID <= 0 || stack.hasTagCompound()) return stack;
        MinecraftServer server = level.getServer();
        if (server != null && !server.isSameThread()) return stack;
        BlockState owner = ownerAt(level, pos, blockID);
        if (owner == null) return stack;
        Block block = owner.getBlock();
        // a self-drop: FC's own id, or the 1.20.1 block's legacy id (a double slab, 43 / 125, drops 44 / 126)
        if (stack.itemID != blockID && stack.itemID != ProxyRegistry.getBlockId(block)) return stack;
        Item own = block.asItem();
        if (own == null || own == Items.AIR) return stack;
        Item dropped = ItemStackHelper.toMcStack(stack).getItem();
        if (dropped == Items.AIR || dropped == own) return stack;
        // FC's subtype stands for something else on purpose (a strata subtype, a species FC already keeps)
        Item base = ItemStackHelper.toMcStack(new btw.modern.ItemStack(stack.itemID, 1, 0)).getItem();
        if (dropped != base) return stack;
        net.minecraft.world.item.ItemStack mine = new net.minecraft.world.item.ItemStack(own, stack.stackSize);
        btw.modern.ItemStack fcMine = ItemStackHelper.toFcStack(mine);
        if (fcMine != null && ItemStackHelper.toMcStack(fcMine).getItem() == own) {
            return fcMine;   // FC holds it: FC's own EntityItem goes on (38:8, 35:1, 397:1)
        }
        Block.popResource(level, pos, mine);   // no FC form (wet sponge, stone slab, smooth sandstone)
        return null;
    }

    /**
     * The 1.20.1 block that FC's {@code blockID} drops for at {@code pos}: the live state when FC reads it as
     * {@code blockID}, else the state this thread's player harvest removed there; null when neither applies.
     */
    static BlockState ownerAt(ServerLevel level, BlockPos pos, int blockID) {
        BlockState live = level.getBlockState(pos);
        if (eligible(live) && ProxyRegistry.getWorldBlockId(live, level, pos) == blockID) return live;
        Harvest h = HARVEST.get();
        if (h != null && h.level() == level && h.pos().equals(pos) && eligible(h.state())
                && ProxyRegistry.getWorldBlockId(h.state(), level, pos) == blockID) {
            return h.state();
        }
        return null;
    }

    /** A vanilla-id block with an FC identity, not a ProxyBlock, not a ruled bed or door, not exempt. */
    private static boolean eligible(BlockState s) {
        if (s == null || s.isAir()) return false;
        Block b = s.getBlock();
        return !(b instanceof ProxyBlock) && !EXEMPT.contains(b) && !VanillaPairDrops.isRuled(b)
                && ProxyRegistry.getBlockId(b) > 0;
    }

    /**
     * [SELF-AUDIT/identity], once at server start (BridgeSelfAudit.runServerStart): every block the rule covers on a
     * normal FC drop at its default state (FC's quantityDropped / idDropped / damageDropped at its subtype,
     * ProxyRegistry.getLegacyVariantMeta, probed with a private Random), one INFO line each and a summary. WARNs:
     * W1 one of the ten flowers 38:1-10 that is not a member dropped as its own FC subtype (the variant tables or the
     * rule are missing);
     * W2 ProxyRegistry's 38 variant table and LegacyVariants disagree on a flower subtype;
     * W3 a member dropped as an FC subtype whose FC item does not declare subtypes (FC's merges in BTW storage would
     * fold it into its id's other subtypes: Modern-Common ItemBlock.getHasSubtypes, ItemMultiTextureTile, Item.skull).
     */
    public static void audit() {
        if (!BTWLifecycle.isFcReady()) {
            LOGGER.warn("[SELF-AUDIT/identity] FC is not initialised; FC self-drops keep FC's subtype");
            return;
        }
        int members = 0, fcForm = 0, modernForm = 0, warnings = 0;
        Map<Integer, List<String>> byId = new TreeMap<>();
        Map<Block, Integer> flowerForm = new IdentityHashMap<>();
        Random probe = new Random(0L);
        for (Block b : ForgeRegistries.BLOCKS) {
            BlockState s = b.defaultBlockState();
            if (!eligible(s)) continue;
            Object key = ForgeRegistries.BLOCKS.getKey(b);
            try {
                int id = ProxyRegistry.getBlockId(b);
                btw.modern.Block fc = btw.modern.Block.blocksList[id];
                if (fc == null) continue;
                int meta = Math.max(ProxyRegistry.getLegacyVariantMeta(b), 0);
                if (fc.quantityDropped(probe) <= 0) continue;   // drops nothing (infested blocks, 1.5.2 BlockSilverfish)
                int dropId = fc.idDropped(meta, probe, 0);
                if (dropId != id) continue;
                btw.modern.ItemStack drop = new btw.modern.ItemStack(dropId, 1, fc.damageDropped(meta));
                Item own = b.asItem();
                if (own == null || own == Items.AIR) continue;
                Item dropped = ItemStackHelper.toMcStack(drop).getItem();
                if (dropped == Items.AIR || dropped == own) continue;
                Item base = ItemStackHelper.toMcStack(new btw.modern.ItemStack(dropId, 1, 0)).getItem();
                if (dropped != base) continue;
                btw.modern.ItemStack fcMine = ItemStackHelper.toFcStack(new net.minecraft.world.item.ItemStack(own));
                boolean holds = fcMine != null && ItemStackHelper.toMcStack(fcMine).getItem() == own;
                members++;
                if (holds) fcForm++; else modernForm++;
                if (holds && fcMine.itemID == FLOWER_ID) flowerForm.put(b, fcMine.getItemDamage());
                byId.computeIfAbsent(id, k -> new ArrayList<>()).add(String.valueOf(key));
                LOGGER.info("[SELF-AUDIT/identity] {} id={} meta={}: FC drops {}:{} = {}; dropped as {} ({})", key, id,
                        meta, dropId, drop.getItemDamage(), ForgeRegistries.ITEMS.getKey(dropped),
                        ForgeRegistries.ITEMS.getKey(own),
                        holds ? "FC " + fcMine.itemID + ":" + fcMine.getItemDamage() : "1.20.1 item, no FC form");
                if (holds && fcMine.getItemDamage() > 0) {
                    btw.modern.Item fcItem = fcMine.itemID < btw.modern.Item.itemsList.length
                            ? btw.modern.Item.itemsList[fcMine.itemID] : null;
                    if (fcItem == null || !fcItem.getHasSubtypes()) {
                        warnings++;
                        LOGGER.warn("[SELF-AUDIT/identity] W3 {} drops as FC {}:{}, but FC item {} declares no "
                                + "subtypes: BTW storage would merge it into the id's other subtypes", key,
                                fcMine.itemID, fcMine.getItemDamage(), fcMine.itemID);
                    }
                }
            } catch (Throwable t) {
                warnings++;
                LOGGER.warn("[SELF-AUDIT/identity] could not audit {}", key, t);
            }
        }
        for (int m = 1; m < FLOWERS.length; m++) {
            Integer sub = flowerForm.get(FLOWERS[m]);
            if (sub == null || sub != m) {
                warnings++;
                LOGGER.warn("[SELF-AUDIT/identity] W1 {} is not dropped as FC 38:{} (got {}): the flower_subtypes "
                        + "tables or the rule are missing", ForgeRegistries.BLOCKS.getKey(FLOWERS[m]), m, sub);
            }
        }
        for (int m = 0; m < FLOWERS.length; m++) {
            Block variant = ProxyRegistry.getModernBlockVariant(FLOWER_ID, m);
            Item item = null;
            try {
                item = LegacyVariants.toModern(FLOWER_ID, m);
            } catch (Throwable ignored) {
                // LegacyVariants logs its own init failure
            }
            if (variant != FLOWERS[m] || item != FLOWERS[m].asItem()) {
                warnings++;
                LOGGER.warn("[SELF-AUDIT/identity] W2 38:{}: ProxyRegistry places {}, LegacyVariants reads {}; both "
                        + "must be {}", m, variant, item, ForgeRegistries.BLOCKS.getKey(FLOWERS[m]));
            }
        }
        if (!EXEMPT.isEmpty()) {
            LOGGER.info("[SELF-AUDIT/identity] exempt (FC's drop wins): {}", EXEMPT);
        }
        LOGGER.info("[SELF-AUDIT/identity] {} blocks keep their identity on FC self-drops ({} as FC subtypes, {} as "
                + "1.20.1 items) over ids {}; {} warnings (0 expected)", members, fcForm, modernForm, byId.keySet(),
                warnings);
    }
}
