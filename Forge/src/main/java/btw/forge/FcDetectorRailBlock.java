package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_redstone (INTEGRATIONS #056)
 *
 * <p><b>Feature:</b> 1.20.1's minecarts ride a block only when it is a {@code BaseRailBlock} in {@code #minecraft:rails}
 * (BaseRailBlock.isRail, BaseRailBlock.java:32-34; AbstractMinecart.java:309-322), and a detector rail's power comes from
 * its own cart check (DetectorRailBlock.checkPressed, entityInside, tick, getAnalogOutputSignal).</p>
 *
 * <p><b>Conflict with FC:</b> BTW's detector rails, the wooden 235 and the soulforged steel 236 (FCBlockDetectorRail,
 * Src FCBetterThanWolves.java:1060-1064), were ProxyBlocks: carts derailed on them, FC's cart check never saw a 1.20.1
 * cart (Client FCBlockDetectorRail.java:107), and Modern-Common's BlockDetectorRail had no power answers, so they never
 * powered anything. In 4.B they are real rails (the 1.5.2 rail check accepts any BlockRailBase) with BTW's trigger rule
 * (FCBlockDetectorRail.ShouldPlateActivateBasedOnMinecart, :138-174): the wooden rail any cart, the soulforged steel rail
 * a cart carrying a player, and the vanilla detector rail 28 a cart that is not empty.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend). Ids 235 and 236 are registered as this 1.20.1 detector rail under their
 * ProxyBlock names ({@code betterthanwolves:block_235}, {@code block_236}; ruling Q-RS1 (a)), keeping their FC identity
 * (ProxyRegistry.registerFcOwnBlock): FC reads and writes them through WorldBridge's detector rail codec (shape | 8
 * pressed), and they drop FC's own item. 1.20.1 runs the rail: carts, power, the comparator reading and the re-check.
 * The carts it counts are filtered by FC's rule ({@link #cartTriggers}, asked by btw.forge.mixin.DetectorRailBlockMixin
 * for every DetectorRailBlock, 28 included: ruling Q-RS2 (a)), with each cart's 1.5.2 type and rider taken from its FC
 * view (VanillaEntityViews), as FC's own check reads them.</p>
 *
 * <p><b>Rebalance:</b> none (4.B parity). BTW detector rails already in a world reload as north-south, unpowered rails
 * (their saved ProxyBlock metadata has no 1.20.1 property); they never worked before.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> making ProxyBlock a rail (it cannot extend BaseRailBlock); a separate
 * cart filter per rail id (FC's rule already names each id).</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_redstone: BTW's detector rails 235 and 236 are 1.20.1 detector rails with FC's identity and FC's trigger rule
public final class FcDetectorRailBlock extends DetectorRailBlock {

    private static final Logger LOGGER = LogManager.getLogger("BTW-DetectorRail");

    /** FC's detector rail class (Client FCBlockDetectorRail) and its trigger rule. */
    private static final String FC_DETECTOR_RAIL = "net.minecraft.src.btw.block.FCBlockDetectorRail";

    /** The legacy ids registered as this class (BTWRegistration). */
    public static final int WOOD_ID = 235;
    public static final int SOULFORGED_STEEL_ID = 236;

    private final int legacyId;

    public FcDetectorRailBlock(int legacyId) {
        // the vanilla detector rail's properties (Blocks.DETECTOR_RAIL: no collision, 0.7, metal), as FC's 235 and 236
        // copy the vanilla rail's hardness and step sound (Src FCBetterThanWolves.java:1060-1064)
        super(BlockBehaviour.Properties.copy(Blocks.DETECTOR_RAIL));
        this.legacyId = legacyId;
    }

    public int getLegacyId() {
        return legacyId;
    }

    /** True for the FC ids registered as this class instead of a ProxyBlock. */
    public static boolean isFcRailId(int legacyId) {
        return legacyId == WOOD_ID || legacyId == SOULFORGED_STEEL_ID;
    }

    /** FC's drops (its own item), as a ProxyBlock drops them (ProxyBlock.fcDrops). */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        btw.modern.Block fc = fcBlock();
        if (fc == null) return List.of();
        Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        BlockPos pos = origin != null ? BlockPos.containing(origin) : null;
        int meta = WorldBridge.deriveVanillaMetadata(builder.getLevel(), state, pos);
        return ProxyBlock.fcDrops(fc, legacyId, meta, builder);
    }

    private @Nullable btw.modern.Block fcBlock() {
        return legacyId > 0 && legacyId < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[legacyId] : null;
    }

    /**
     * [SELF-AUDIT/rails] (BridgeSelfAudit.runServerStart, tags bound): 235 and 236 are this class with their FC identity
     * and in {@code #minecraft:rails} (else carts derail on them), and FC's trigger rule resolves on the FC blocks of 28,
     * 235 and 236. One INFO line, a WARN per problem.
     */
    public static void audit() {
        int warnings = 0;
        for (int id : new int[] {WOOD_ID, SOULFORGED_STEEL_ID}) {
            net.minecraft.world.level.block.Block block = ProxyRegistry.getFcOwnBlock(id);
            if (!(block instanceof FcDetectorRailBlock rail) || ProxyRegistry.getBlockId(rail) != id) {
                LOGGER.warn("[SELF-AUDIT/rails] FC block {} is not registered as a 1.20.1 detector rail with its FC identity", id);
                warnings++;
                continue;
            }
            if (!rail.defaultBlockState().is(net.minecraft.tags.BlockTags.RAILS)) {
                LOGGER.warn("[SELF-AUDIT/rails] betterthanwolves:block_{} is not in #minecraft:rails: minecarts derail on it", id);
                warnings++;
            }
        }
        int rules = 0;
        for (int id : new int[] {28, WOOD_ID, SOULFORGED_STEEL_ID}) {
            btw.modern.Block fc = id < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[id] : null;
            if (fc != null && RULE.get(fc.getClass()).isPresent()) {
                rules++;
            } else {
                LOGGER.warn("[SELF-AUDIT/rails] FC's detector rail rule does not resolve for id {}: 1.20.1's cart rule stands", id);
                warnings++;
            }
        }
        LOGGER.info("[SELF-AUDIT/rails] BTW detector rails 235 and 236 as 1.20.1 detector rails, FC's cart rule on {}/3 ids,"
                + " {} warnings (0 expected)", rules, warnings);
    }

    // ================================================================
    // FC's trigger rule (DetectorRailBlockMixin)
    // ================================================================

    private static final ClassValue<Optional<MethodHandle>> RULE = new ClassValue<>() {
        @Override
        protected Optional<MethodHandle> computeValue(Class<?> type) {
            try {
                Class<?> fcRail = Class.forName(FC_DETECTOR_RAIL, false, type.getClassLoader());
                if (!fcRail.isAssignableFrom(type)) return Optional.empty();
                return Optional.of(MethodHandles.lookup().unreflect(type.getMethod("ShouldPlateActivateBasedOnMinecart",
                        btw.modern.World.class, int.class, int.class, int.class, int.class, btw.modern.Entity.class)));
            } catch (Throwable t) {
                if (!ruleWarned) {
                    ruleWarned = true;
                    LOGGER.warn("FC's detector rail rule could not be resolved on {}; 1.20.1's cart rule stands", type.getName(), t);
                }
                return Optional.empty();
            }
        }
    };

    private static volatile boolean ruleWarned;
    private static volatile boolean failureWarned;

    /**
     * True when the detector rail at {@code pos} is pressed by {@code cart}: FC's ShouldPlateActivateBasedOnMinecart for
     * the rail's FC block (28, 235, 236), given the cart's 1.5.2 type and rider from its FC view. True (1.20.1's
     * own rule: every cart) on a client, before FC is ready, on a rail without FC's rule, and on any failure (logged
     * once).
     */
    public static boolean cartTriggers(Level level, BlockPos pos, Entity cart) {
        if (!(level instanceof ServerLevel sl) || !BTWLifecycle.isFcReady()) return true;
        try {
            btw.modern.Block fc = ProxyRegistry.getFcBlock(sl.getBlockState(pos).getBlock());
            if (fc == null) return true;
            Optional<MethodHandle> rule = RULE.get(fc.getClass());
            if (rule.isEmpty()) return true;
            btw.modern.Entity view = VanillaEntityViews.of(cart);
            if (!(view instanceof btw.modern.EntityMinecart minecart)) return true;
            return (boolean) rule.get().invoke(fc, WorldBridge.getOrCreate(sl), pos.getX(), pos.getY(), pos.getZ(),
                    minecart.getMinecartType(), minecart.riddenByEntity);
        } catch (Throwable t) {
            if (!failureWarned) {
                failureWarned = true;
                LOGGER.warn("FC's detector rail rule failed at {}; 1.20.1's cart rule stands (logged once)", pos, t);
            }
            return true;
        }
    }
}
