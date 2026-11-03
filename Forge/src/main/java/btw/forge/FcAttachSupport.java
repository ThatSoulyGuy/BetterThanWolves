package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * FCMOD-INTEGRATION(1.20.1): attach_support_rules (INTEGRATIONS #055)
 *
 * <p>FC's own attachment rules, asked for a 1.20.1 attachment state: the veto of
 * btw.forge.mixin.AttachableSupportMixin (1.20.1's canSurvive of the torch, wall torch, ladder, snow layer, pressure
 * plates and rails), of btw.forge.ProxyBlock's canSurvive (FC's own blocks in these families), the rails' survival in
 * btw.forge.mixin.RailSupportMixin and the soul torches' neighbour check in btw.forge.mixin.BlockBehaviorMixin.</p>
 *
 * <p><b>The rule.</b> {@link #refuses} is true when the state's rule block belongs to one of FC's attachment families
 * and that family's 4.B predicate refuses the state's orientation at its position; every other answer is false (no
 * veto). The rule block is the state's FC block ({@link ProxyRegistry#getFcBlock}) or, for a 1.20.1 block a ruling
 * gives another FC block's attachment rule, its support twin's FC block ({@link #SUPPORT_TWIN}). The orientation is FC's
 * metadata of the state asked about ({@link WorldBridge#deriveVanillaMetadata}), not of the cell, so 1.20.1's placement
 * probes are answered for the facing they try. FC's own blocks (ProxyBlocks: BTW's torches and ladders, ash, the
 * soulforged plate) reach canSurvive only from 1.20.1's BlockItem placement (a sneaking player, a clicked block without
 * Stage 2), so their rule is the test 4.B's ItemBlock asks before it places, FC's own {@code canPlaceBlockAt(world, i, j,
 * k)} (vanilla/server World.canPlaceEntityOnSide, World.java:3377-3407, through canPlaceBlockOnSide): a side a ladder
 * can hang from (Server FCBlockLadderBase.java:70-82), a side or the block below for a torch (Server
 * FCBlockTorchBase.java:63-78). It reads no orientation, so their META (ProxyBlock.getStateForPlacement) is not read.
 * The families, keyed on FC's class hierarchy:</p>
 * <ul>
 *   <li>torch (FCBlockTorchBase): orientation 5 stands on the block below, {@code canPlaceTorchOn(world, i, j - 1, k)};
 *       orientations 1-4 hang from the block behind, facing {@code 6 - orientation},
 *       {@code FCUtilsWorld.DoesBlockHaveCenterHardpointToFacing}; orientation 0 is not tested (Client
 *       FCBlockTorchBase.ValidateState, FCBlockTorchBase.java:241-281);</li>
 *   <li>ladder (FCBlockLadderBase): {@code CanAttachToFacing(world, i, j, k, GetOppositeFacing(GetFacing(meta)))} (its
 *       neighbour check, FCBlockLadderBase.java:111-122 and :182-193);</li>
 *   <li>redstone torch (Modern-Common btw.modern.BlockTorch, the 1.5.2 BlockTorch 4.B's redstone torches 75 and 76 are:
 *       vanilla/client Block.java:244-245, BlockTorch.java:65-119): orientation 5 stands on {@code canPlaceTorchOn(world,
 *       i, j - 1, k)}, orientations 1-4 hang from a normal cube behind ({@code World.isBlockNormalCube}, FC's
 *       isBlockNormalCubeDefault); orientation 0 is not tested. Ruling Q-P1 (a), the FOUND unit: 1.20.1's redstone torch
 *       and redstone wall torch are refused, and their survival ends, where 4.B's redstone torch rule refuses
 *       (RedstoneTorchBlock inherits TorchBlock.canSurvive; RedstoneWallTorchBlock.canSurvive asks WALL_TORCH's with its
 *       own state, keyed here on that state, FC 76);</li>
 *   <li>ground cover (FCBlockGroundCover: the snow layer 78, minecraft:snow, and ash 1046): {@code canPlaceBlockAt(world,
 *       i, j, k)}, the block below's CanGroundCoverRestOnBlock and, for snow, a block below that is not warm (Server
 *       FCBlockGroundCover.java:53-65, FCBlockSnowCover.java:36-48). It is the test FC's neighbour check repeats
 *       (FCBlockGroundCover.java:67-74) and the one 4.B's weather asks before it lays snow (vanilla/server
 *       World.canSnowAt, World.java:2970-3002);</li>
 *   <li>pressure plate (FCBlockPressurePlate: 70, 72 and the soulforged plate 230): {@code canPlaceBlockAt}, a solid top
 *       below ({@code World.doesBlockHaveSolidTopSurface}, Server FCBlockPressurePlate.java:38-44; its neighbour check
 *       :84-104);</li>
 *   <li>rail (Modern-Common btw.modern.BlockRailBase, the 1.5.2 class 4.B's rails are: FC's 66, 28, 235 and 236, the
 *       powered rail through its support twin): {@code canPlaceBlockAt}, a solid top below (BlockRailBase.java:124-127;
 *       4.B vanilla/server BlockRailBase.java:144-147). 4.B's neighbour check also drops an ascending rail whose climb
 *       has no solid top (:169-208): {@link #railDropped}.</li>
 * </ul>
 * <p>The three floor predicates read the block below and never the cell itself (javap of the shipped classes), so one
 * call answers a placement probe and the survival of the block already there. Their support must be air, have an FC
 * identity both views read alike, or be a support relative ({@link #sameOnBothViews}), on the server too: any other
 * modern block's stand-in answers 1.20.1's full sturdy face, which is not 1.20.1's snow (a full collision face and two
 * block tags), plate (rigid or centre support) or rail (rigid support) rule, so FC's answer there would be no 4.B rule;
 * 1.20.1's answer stands. (FC's own neighbour check of the snow layer and of plates 70 and 72, BlockBehaviorMixin's
 * NOTIFY, still asks the stand-in when a neighbour changes, as before: a snow layer on a honey block goes then. A plate
 * on a modern fence, wall or stained glass pane, a support relative, is refused at placement, as on its relative.)</p>
 * <p>The methods are FC's own, called on the rule block, so subclass overrides answer. FC classes are not on the
 * bridge's compile path: they are resolved by name on first use (one INFO line), and a missing name logs one WARN and
 * vetoes nothing. Nothing here is read before FC's block table is ready ({@link BTWLifecycle#isFcReady}); the class's
 * static state is the twin and relative tables alone.</p>
 *
 * <p><b>Support twins</b> (maintainer ruling 2026-10-23: soul torches take the torch's 4.B support rule): the soul
 * torch asks the torch's FC block (50) and the soul wall torch the wall torch's (50), each with its own orientation.
 * A read path only, like ProxyRegistry's log forms: the soul blocks keep no FC identity (no FC callback, drop, item route
 * or world id), and this is not a {@link TwinRegistry} twin (no world id, no tick owner). Keyed on the exact block,
 * never on a class, so the redstone torch and other mods' torches are not twins. Ruling 2026-10-25: a soul torch also
 * drops at any neighbour change on a support 4.B refuses, as the torch does ({@link #isSupportTwin}, BlockBehaviorMixin).
 * Ruling 2026-10-31 names the powered rail among FC's rails: it asks the rail's FC block (66). 4.B's powered rail is a
 * BlockRailPowered, a BlockRailBase with the base class's placement rule (vanilla/server Block.java:193), while the
 * port's FC block at 27 is a plain block. A twin entry is read before the block's own FC block; the powered rail keeps
 * its FC identity 27 for everything else. Ruling 2026-10-31 (1): the modern pressure plates take their nearest BTW
 * relative's 4.B plate rule, as the soul torches took the torch's: the wooden plates of spruce, birch, jungle, acacia,
 * dark oak, mangrove, cherry, bamboo, crimson and warped ask the oak plate's FC block (72), the polished blackstone
 * plate the stone plate's (70); the weighted plates stay FC's stubs (no rule). A plate twin also drops at a neighbour
 * change on a support 4.B refuses, through BlockBehaviorMixin's twin branch, as FC's plate neighbour check drops 70 and
 * 72 (Server FCBlockPressurePlate.java:84-104).</p>
 *
 * <p><b>Support relatives</b> (ruling 2026-10-31 (2), {@link #SUPPORT_RELATIVE}): the other side of the question. A
 * modern fence, wall or stained glass pane answers every support question FC asks of the block it rests on or hangs
 * from as its BTW relative does (the oak fence 85, the cobblestone wall 139, the glass pane 102), through
 * {@link #supportRelative}: the server's stand-in hands its support methods to the relative's FC block
 * (ModernBlockStandIn), ClientFcWorld's support view reads the relative's id, 1.5.2's redstone torch id list reads
 * {@code World.getSupportBlockId}, and FC's placed tools ask the relative which tool sticks ({@link #supportBlock}, ruling
 * 2026-11-02). A relative is a read path only: FC's identity, drops, hooks, ticks, connections, collision and rendering
 * stay the stand-in's.</p>
 *
 * <p><b>Worlds.</b> A {@link ServerLevel}: its WorldBridge. A client level on the render thread: the read-only
 * ClientFcWorld's support view (btw.forge.client.ClientAttachSupport, ClientFcWorld.forSupport), and only when the
 * support reads alike on both views (air, {@link ProxyRegistry#getBlockId} equal to
 * {@link ProxyRegistry#getWorldBlockId}, or a support relative, which both views read as its relative's FC block):
 * ClientFcWorld reads any other modern block as air where the server reads a stand-in that answers 1.20.1's own faces,
 * and a double slab as the half slab, so the client never refuses where the server accepts. FC's own torches and
 * ladders are predicted when every block their canPlaceBlockAt reads (the four sides, and the block below for a torch)
 * reads alike: the client then refuses exactly where the server does, and does not shrink its stack for a placement
 * the server refuses. Any other reader (a WorldGenRegion, another thread): no veto. Reads only; every throw is logged
 * once and vetoes nothing.</p>
 */
// FCMOD-INTEGRATION(1.20.1): attach_support_rules: one predicate answers FC's torch, ladder, redstone torch, ground cover, pressure plate and rail attachment rules for 1.20.1's attachment states, the soul torches, the powered rail and the modern pressure plates through their support twins, over modern fences, walls and stained glass panes read as their support relatives
public final class FcAttachSupport {

    private static final Logger LOGGER = LogManager.getLogger("BTW-AttachSupport");

    private static final String FC_TORCH_BASE = "net.minecraft.src.btw.block.FCBlockTorchBase";
    private static final String FC_LADDER_BASE = "net.minecraft.src.btw.block.FCBlockLadderBase";
    private static final String FC_GROUND_COVER = "net.minecraft.src.btw.block.FCBlockGroundCover";
    private static final String FC_PRESSURE_PLATE = "net.minecraft.src.btw.block.FCBlockPressurePlate";
    private static final String FC_UTILS_WORLD = "net.minecraft.src.btw.util.FCUtilsWorld";

    /** FC torch orientation of a standing torch (FCBlockTorchBase: 1-4 on a wall, 5 on the floor). */
    private static final int STANDING = 5;

    /**
     * 1.20.1 blocks that a ruling gives another FC block's attachment rule, to the 1.20.1 block whose FC block holds that
     * rule: the attaching blocks (soul torches, the powered rail, the modern pressure plates). Read by {@link #ruleBlock}
     * and {@link #isSupportTwin} only. Never written after class initialisation.
     */
    private static final Map<Block, Block> SUPPORT_TWIN = new IdentityHashMap<>();

    /**
     * FCMOD-INTEGRATION(1.20.1): attach_support_rules, support_relatives (ruling 2026-10-31 (2))
     *
     * <p>1.20.1 supports that count as a BTW block for every support question, to the 1.20.1 block whose FC block is
     * that relative: the ten wooden modern fences to the oak fence (85, FCBlockFenceWood), the twenty modern walls to the
     * cobblestone wall (139, FCBlockWall), the sixteen stained glass panes to the glass pane (102, FCBlockPane). Read by
     * {@link #supportRelative} and {@link #supportRelativeId} only, never by {@link #ruleBlock}: a fence, wall or pane
     * attaches to nothing. Never written after class initialisation.</p>
     *
     * <p><b>Feature:</b> FC asks the block an attachment rests on or hangs from through six per-block methods, two world
     * reads and one id list (FCUtilsWorld's hard point helpers, Server FCUtilsWorld.java:442-517; FCBlockGroundCover's
     * rest and resting offset, :52-155; Block.CanSupportFallingBlocks; World.doesBlockHaveSolidTopSurface and
     * isBlockNormalCube; 1.5.2 BlockTorch.canPlaceTorchOn's fence, glass and wall ids): torches, candles and chunk ore
     * on top, iron spikes on a side, stakes, placed tools, lightning rods and falling blocks above and below, snow and
     * ash resting on the ground under a fence, plates, rails and the standing redstone torch.</p>
     *
     * <p><b>Conflict with FC:</b> 4.B answers these questions for its fence (85; the nether brick fence 113 is the same
     * class), its cobblestone wall (139) and its glass pane (102). 1.20.1 adds ten more wooden fences (crimson and warped
     * among them; the ruling names the oak fence for all ten, not the wood sidings' fence subtype), twenty walls and
     * sixteen stained panes, which FC reads as a stand-in (ModernBlockStandIn) answering 1.20.1's own faces: an oak fence
     * held a stake and rested snow on the ground under it while a spruce fence did not; a stained pane held torches,
     * candles and ash a glass pane refuses; the floor families' gate left every modern fence, wall and pane to 1.20.1's
     * plate rule; and the server refused a standing redstone torch on a modern fence (its id is no 85) while the
     * client, gated, predicted it and shrank its stack.</p>
     *
     * <p><b>Integration pattern:</b> D (FC-extend), a read only: a related support answers FC's support methods with its
     * relative's FC block, on the same world and cell. Server: the stand-in delegates its hard points (centre, small and
     * large), the ground-cover rest and the resting offset (CanSupportFallingBlocks follows the centre hard point); a
     * method is handed over when every consumer of it asks what may rest on, hang from or stand against the block, so
     * IsNormalCube (also suffocation, push-out and path finding) stays the stand-in's, and answers alike on every related
     * state anyway. Client: ClientFcWorld's support view, the world FcAttachSupport predicts on, reads the relative's id;
     * the shared client view keeps identity reads. Id list: btw.modern.World.getSupportBlockId. Placed tools (ruling
     * 2026-11-02): whether a tool may stick into the block is a support question too, so FC's tool test
     * (FCItemTool.CanToolStickInBlock, the tool's efficiency against the block's material) is asked of the relative's FC
     * block ({@link #supportBlock}); the stuck-in block FC keeps, its CanToolsStickInBlock and the placement sound stay
     * the stand-in's.</p>
     *
     * <p><b>Rebalance:</b> none (4.B's answers for the relative). 1.20.1's own snow on fences and walls stays refused (the
     * veto never grants).</p>
     *
     * <p><b>Injection strategy:</b> the stand-in, the bridge and the client views are the bridge's own classes, and
     * Modern-Common's BlockTorch asks the new world read; one load-time mixin on FC's tool placement,
     * btw.forge.mixin.fcext.FCItemToolStickRelativeMixin (FCItemTool.onItemUse's CanToolStickInBlock argument).</p>
     *
     * <p><b>Alternative considered:</b> the relative's id in ProxyRegistry.getWorldBlockId (rejected: every FC read would
     * see an oak fence, so drops, the saw, pistons, the dispenser, collision, fire odds and hardness would change, which
     * the ruling excludes); @Pseudo mixins at each FC dispatch site (rejected: more than eight injects, and a new FC
     * caller would be missed); the 1.20.1 tags as the key (rejected: datapack data that changes on /reload and lets any
     * pack's fence take the oak fence's answers; {@link #audit} checks the table against the tags instead).</p>
     */
    private static final Map<Block, Block> SUPPORT_RELATIVE = new IdentityHashMap<>();

    static {
        SUPPORT_TWIN.put(Blocks.SOUL_TORCH, Blocks.TORCH);
        SUPPORT_TWIN.put(Blocks.SOUL_WALL_TORCH, Blocks.WALL_TORCH);
        SUPPORT_TWIN.put(Blocks.POWERED_RAIL, Blocks.RAIL);
        for (Block plate : new Block[] {Blocks.SPRUCE_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE,
                Blocks.JUNGLE_PRESSURE_PLATE, Blocks.ACACIA_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE,
                Blocks.MANGROVE_PRESSURE_PLATE, Blocks.CHERRY_PRESSURE_PLATE, Blocks.BAMBOO_PRESSURE_PLATE,
                Blocks.CRIMSON_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE}) {
            SUPPORT_TWIN.put(plate, Blocks.OAK_PRESSURE_PLATE);
        }
        SUPPORT_TWIN.put(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE);

        for (Block fence : new Block[] {Blocks.SPRUCE_FENCE, Blocks.BIRCH_FENCE, Blocks.JUNGLE_FENCE,
                Blocks.ACACIA_FENCE, Blocks.DARK_OAK_FENCE, Blocks.MANGROVE_FENCE, Blocks.CHERRY_FENCE,
                Blocks.BAMBOO_FENCE, Blocks.CRIMSON_FENCE, Blocks.WARPED_FENCE}) {
            SUPPORT_RELATIVE.put(fence, Blocks.OAK_FENCE);
        }
        for (Block wall : new Block[] {Blocks.BRICK_WALL, Blocks.PRISMARINE_WALL, Blocks.RED_SANDSTONE_WALL,
                Blocks.MOSSY_STONE_BRICK_WALL, Blocks.GRANITE_WALL, Blocks.STONE_BRICK_WALL, Blocks.MUD_BRICK_WALL,
                Blocks.NETHER_BRICK_WALL, Blocks.ANDESITE_WALL, Blocks.RED_NETHER_BRICK_WALL, Blocks.SANDSTONE_WALL,
                Blocks.END_STONE_BRICK_WALL, Blocks.DIORITE_WALL, Blocks.BLACKSTONE_WALL,
                Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_WALL, Blocks.COBBLED_DEEPSLATE_WALL,
                Blocks.POLISHED_DEEPSLATE_WALL, Blocks.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_BRICK_WALL}) {
            SUPPORT_RELATIVE.put(wall, Blocks.COBBLESTONE_WALL);
        }
        for (Block pane : new Block[] {Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE,
                Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
                Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS_PANE,
                Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS_PANE,
                Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS_PANE,
                Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS_PANE}) {
            SUPPORT_RELATIVE.put(pane, Blocks.GLASS_PANE);
        }
    }

    /** FC's families and predicates, resolved once. */
    private record Fc(Class<?> torchBase, Class<?> ladderBase, Class<?> groundCover, Class<?> pressurePlate,
                      MethodHandle canPlaceTorchOn, MethodHandle centerHardpoint, MethodHandle canAttachToFacing) {}

    private static volatile Fc fc;
    private static volatile boolean unresolved;
    private static volatile boolean failureLogged;
    private static volatile boolean relativeFailureLogged;

    private FcAttachSupport() {}

    /**
     * True for a block that takes an FC attachment rule through a support twin (the soul torches, the powered rail, the
     * modern pressure plates); never for a support relative.
     */
    public static boolean isSupportTwin(@Nullable Block block) {
        return block != null && SUPPORT_TWIN.containsKey(block);
    }

    /**
     * The FC block {@code state} answers FC's support questions with at {@code pos}: its support relative's FC block
     * ({@link #SUPPORT_RELATIVE}) when the state's block is in the table, has no FC identity
     * ({@link ProxyRegistry#getBlockId} 0) and FC reads the cell through a stand-in
     * ({@link ModernBlockStandIn#isStandIn} of {@link ProxyRegistry#getWorldBlockId}, the same reading on both sides);
     * else null (the cell keeps its own answers). Never throws (a failure is logged once and answers null).
     */
    public static @Nullable btw.modern.Block supportRelative(@Nullable BlockState state, @Nullable BlockGetter level,
                                                             @Nullable BlockPos pos) {
        if (state == null || level == null || pos == null || !BTWLifecycle.isFcReady()) return null;
        Block relative = SUPPORT_RELATIVE.get(state.getBlock());
        if (relative == null) return null;
        try {
            if (ProxyRegistry.getBlockId(state.getBlock()) != 0) return null;
            if (!ModernBlockStandIn.isStandIn(ProxyRegistry.getWorldBlockId(state, level, pos))) return null;
            return ProxyRegistry.getFcBlock(relative);
        } catch (Throwable t) {
            if (!relativeFailureLogged) {
                relativeFailureLogged = true;
                LOGGER.warn("Support relative lookup failed for {} at {}; the cell keeps its own answers (logged once)",
                        state, pos, t);
            }
            return null;
        }
    }

    /** The FC id of {@link #supportRelative}, or 0 when the cell has none. */
    public static int supportRelativeId(@Nullable BlockState state, @Nullable BlockGetter level, @Nullable BlockPos pos) {
        btw.modern.Block relative = supportRelative(state, level, pos);
        return relative != null ? relative.blockID : 0;
    }

    /**
     * The FC block a support question about (i, j, k) is asked of, given {@code block}, the block FC read there
     * ({@code Block.blocksList[world.getBlockId(i, j, k)]}): when {@code block} is a stand-in (ModernBlockStandIn) and
     * {@code world} a WorldBridge, the cell's {@link #supportRelative} if it has one; else {@code block}. Server reads
     * only, as the stand-in's own delegation (any other world keeps the stand-in's answers). Asked by FC's placed tools
     * for the tool test (btw.forge.mixin.fcext.FCItemToolStickRelativeMixin, ruling 2026-11-02). Never throws.
     */
    // FCMOD-INTEGRATION(1.20.1): support_relatives: which placed tool sticks into a modern fence, wall or stained glass pane is asked of its BTW relative's FC block
    public static @Nullable btw.modern.Block supportBlock(@Nullable btw.modern.Block block,
                                                          @Nullable btw.modern.World world, int i, int j, int k) {
        if (block == null || !(world instanceof WorldBridge wb)) return block;
        if (!ModernBlockStandIn.isStandIn(block.blockID)) return block;
        ServerLevel level = wb.getServerLevel();
        if (level == null) return block;
        BlockPos pos = new BlockPos(i, j, k);
        btw.modern.Block relative = supportRelative(level.getBlockState(pos), level, pos);
        return relative != null ? relative : block;
    }

    /**
     * True when FC's attachment rule for {@code state}'s rule block refuses {@code state} at {@code pos} of
     * {@code level}; false when it accepts, when the block has no FC attachment rule and for any reader FC's rule is
     * not asked on (see the class javadoc).
     */
    public static boolean refuses(@Nullable BlockState state, @Nullable LevelReader level, @Nullable BlockPos pos) {
        if (state == null || level == null || pos == null || !BTWLifecycle.isFcReady()) return false;
        try {
            btw.modern.Block rule = ruleBlock(state.getBlock());
            if (rule == null) return false;
            // FCMOD-INTEGRATION(1.20.1): attach_support_rules: the third family, the redstone torch (btw.modern.BlockTorch),
            // is Modern-Common's own class and needs no FC name; the torch and ladder families are FC's
            boolean redstoneTorch = rule instanceof btw.modern.BlockTorch;
            // FCMOD-INTEGRATION(1.20.1): attach_support_rules: the floor families, ground cover and pressure plate (FC's
            // classes) and rail (Modern-Common's 1.5.2 BlockRailBase, no FC name), ask FC's canPlaceBlockAt of the cell
            boolean rail = rule instanceof btw.modern.BlockRailBase;
            Fc f = redstoneTorch || rail ? null : fc();
            if (!redstoneTorch && !rail && f == null) return false;
            boolean torch = f != null && f.torchBase().isInstance(rule);
            boolean floor = rail || f != null && (f.groundCover().isInstance(rule) || f.pressurePlate().isInstance(rule));
            if (!redstoneTorch && !torch && !floor && !f.ladderBase().isInstance(rule)) return false;

            btw.modern.World world;
            boolean client;
            if (level instanceof ServerLevel sl) {
                world = WorldBridge.getOrCreate(sl);
                client = false;
            } else if (level instanceof Level l && l.isClientSide()) {
                world = DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                        () -> () -> btw.forge.client.ClientAttachSupport.worldFor(level));
                client = true;
            } else {
                return false;
            }
            if (world == null) return false;

            if (floor) {
                // FC's own canPlaceBlockAt reads only the block below (FCBlockGroundCover / FCBlockSnowCover,
                // FCBlockPressurePlate, BlockRailBase), the support both views must read alike, on the server too
                if (!sameOnBothViews(level, pos.below())) return false;
                return !rule.canPlaceBlockAt(world, pos.getX(), pos.getY(), pos.getZ());
            }
            if (state.getBlock() instanceof ProxyBlock) {
                // FC's own torches and ladders: 1.20.1's BlockItem placement is their only canSurvive caller, so they take
                // the test 4.B's ItemBlock asks, FC's canPlaceBlockAt (FCBlockTorchBase: a side or the block below;
                // FCBlockLadderBase: a side), which reads no orientation; the client predicts it when each block it
                // reads reads alike
                if (client && !supportsReadAlike(level, pos, torch || redstoneTorch)) return false;
                return !rule.canPlaceBlockAt(world, pos.getX(), pos.getY(), pos.getZ());
            }
            int meta = WorldBridge.deriveVanillaMetadata(level, state, pos);
            if (redstoneTorch) {
                // 1.5.2 BlockTorch.canPlaceTorchOn below a standing torch, a normal cube behind a wall torch (the tests of
                // BlockTorch.onBlockPlaced and func_94397_d, vanilla/client BlockTorch.java:89-119, :175-217)
                btw.modern.BlockTorch redstone = (btw.modern.BlockTorch) rule;
                int orientation = meta & 7;
                if (orientation == STANDING) {
                    BlockPos below = pos.below();
                    if (client && !sameOnBothViews(level, below)) return false;
                    return !redstone.canPlaceTorchOn(world, below.getX(), below.getY(), below.getZ());
                }
                if (orientation < 1 || orientation > 4) return false;
                BlockPos support = pos.relative(Direction.from3DDataValue(6 - orientation).getOpposite());
                if (client && !sameOnBothViews(level, support)) return false;
                return !world.isBlockNormalCube(support.getX(), support.getY(), support.getZ());
            }
            if (torch) {
                int orientation = meta & 7;
                if (orientation == STANDING) {
                    BlockPos below = pos.below();
                    if (client && !sameOnBothViews(level, below)) return false;
                    boolean accepted = (boolean) f.canPlaceTorchOn().invoke(rule, world,
                            below.getX(), below.getY(), below.getZ());
                    return !accepted;
                }
                if (orientation < 1 || orientation > 4) return false;
                int facing = 6 - orientation;
                BlockPos support = pos.relative(Direction.from3DDataValue(facing).getOpposite());
                if (client && !sameOnBothViews(level, support)) return false;
                boolean accepted = (boolean) f.centerHardpoint().invoke(world,
                        support.getX(), support.getY(), support.getZ(), facing);
                return !accepted;
            }
            int facing = rule.GetFacing(meta);
            if (facing < 2 || facing > 5) return false;
            BlockPos support = pos.relative(Direction.from3DDataValue(facing).getOpposite());
            if (client && !sameOnBothViews(level, support)) return false;
            boolean accepted = (boolean) f.canAttachToFacing().invoke(rule, world,
                    pos.getX(), pos.getY(), pos.getZ(), btw.modern.Block.GetOppositeFacing(facing));
            return !accepted;
        } catch (Throwable t) {
            logFailure(state, pos, t);
            return false;
        }
    }

    /**
     * True when 4.B's rail neighbour check drops the rail at {@code pos} of a server level: FC's rail rule refuses the
     * block below it ({@link #refuses}) or, for an ascending {@code shape}, the block it climbs onto has no solid top in
     * FC's World (1.5.2 BlockRailBase.onNeighborBlockChange, vanilla/server BlockRailBase.java:169-208: metadata 2-5
     * test x + 1, x - 1, z - 1 and z + 1, the blocks 1.20.1's ascending shapes climb onto). The climb's support takes
     * the floor families' gate ({@link #sameOnBothViews}). False for any other reader, for a block outside the rail
     * family and on a failure (logged once).
     */
    public static boolean railDropped(@Nullable Level level, @Nullable BlockPos pos, @Nullable RailShape shape) {
        if (!(level instanceof ServerLevel sl) || pos == null || shape == null || !BTWLifecycle.isFcReady()) return false;
        BlockState state = sl.getBlockState(pos);
        if (refuses(state, sl, pos)) return true;
        Direction climb = switch (shape) {
            case ASCENDING_EAST -> Direction.EAST;
            case ASCENDING_WEST -> Direction.WEST;
            case ASCENDING_NORTH -> Direction.NORTH;
            case ASCENDING_SOUTH -> Direction.SOUTH;
            default -> null;
        };
        if (climb == null) return false;
        try {
            if (!(ruleBlock(state.getBlock()) instanceof btw.modern.BlockRailBase)) return false;
            BlockPos onto = pos.relative(climb);
            if (!sameOnBothViews(sl, onto)) return false;
            return !WorldBridge.getOrCreate(sl).doesBlockHaveSolidTopSurface(onto.getX(), onto.getY(), onto.getZ());
        } catch (Throwable t) {
            logFailure(state, pos, t);
            return false;
        }
    }

    /** The FC block whose attachment rule {@code block} takes: its support twin's, else its own; null for neither. */
    static @Nullable btw.modern.Block ruleBlock(Block block) {
        Block twin = SUPPORT_TWIN.get(block);
        return ProxyRegistry.getFcBlock(twin != null ? twin : block);
    }

    /**
     * True when the block at {@code pos} reads alike in ClientFcWorld's support view and the server's world: air, which
     * every family refuses on both, an FC identity both read (never a stand-in), or a support relative, which both read
     * as the relative's FC block (the server's stand-in answers through it, the client view reads its id).
     */
    private static boolean sameOnBothViews(LevelReader level, BlockPos pos) {
        BlockState support = level.getBlockState(pos);
        if (support.isAir()) return true;
        int id = ProxyRegistry.getBlockId(support.getBlock());
        // FCMOD-INTEGRATION(1.20.1): support_relatives: a modern fence, wall or stained pane reads as its BTW relative on both views
        return id > 0 && id == ProxyRegistry.getWorldBlockId(support, level, pos)
                || supportRelative(support, level, pos) != null;
    }

    /** True when the four blocks beside {@code pos}, and with {@code andBelow} the block below it, read alike. */
    private static boolean supportsReadAlike(LevelReader level, BlockPos pos, boolean andBelow) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!sameOnBothViews(level, pos.relative(side))) return false;
        }
        return !andBelow || sameOnBothViews(level, pos.below());
    }

    private static void logFailure(BlockState state, BlockPos pos, Throwable t) {
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn("FC's attachment rule failed for {} at {}; 1.20.1's answer stands (logged once)",
                    state, pos, t);
        }
    }

    /**
     * Server-start self-audit (BridgeSelfAudit.runServerStart, after the block tags are bound): one INFO line with the
     * support relatives and the rule twins by FC target; a WARN naming an entry whose target has no FC block (inert:
     * {@link #supportRelative} and {@link #ruleBlock} answer null for it); and a WARN naming every minecraft fence, wall
     * ({@code #minecraft:fences}, {@code #minecraft:walls}) or stained glass pane without FC identity that
     * {@link #SUPPORT_RELATIVE} misses (expected: none).
     */
    public static void audit() {
        if (!BTWLifecycle.isFcReady()) return;
        try {
            Map<String, Integer> relatives = new java.util.TreeMap<>();
            Map<String, Integer> twins = new java.util.TreeMap<>();
            Set<String> inert = new TreeSet<>();
            for (Map<Block, Block> table : java.util.List.of(SUPPORT_RELATIVE, SUPPORT_TWIN)) {
                for (Map.Entry<Block, Block> e : table.entrySet()) {
                    btw.modern.Block target = ProxyRegistry.getFcBlock(e.getValue());
                    if (target == null) {
                        inert.add(ForgeRegistries.BLOCKS.getKey(e.getKey()) + " -> "
                                + ForgeRegistries.BLOCKS.getKey(e.getValue()));
                    } else {
                        (table == SUPPORT_RELATIVE ? relatives : twins).merge(
                                target.getClass().getSimpleName() + " " + target.blockID, 1, Integer::sum);
                    }
                }
            }
            Set<String> missed = new TreeSet<>();
            for (Block block : ForgeRegistries.BLOCKS) {
                ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
                if (key == null || !"minecraft".equals(key.getNamespace())) continue;
                BlockState state = block.defaultBlockState();
                boolean family = state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)
                        || block instanceof StainedGlassPaneBlock;
                if (family && ProxyRegistry.getBlockId(block) == 0 && !SUPPORT_RELATIVE.containsKey(block)) {
                    missed.add(key.toString());
                }
            }
            LOGGER.info("Support relatives: {} supports answer as {}; rule twins: {} blocks take the rule of {}",
                    SUPPORT_RELATIVE.size(), relatives, SUPPORT_TWIN.size(), twins);
            if (!inert.isEmpty()) {
                LOGGER.warn("Support relative or rule twin target without an FC block (inert): {}", inert);
            }
            if (!missed.isEmpty()) {
                LOGGER.warn("Fences, walls or stained glass panes without FC identity and without a support relative"
                        + " (they keep their stand-in's answers): {}", missed);
            }
        } catch (Throwable t) {
            LOGGER.warn("Support relative audit failed", t);
        }
    }

    /** FC's families and predicates, or null when a name could not be resolved (logged once; nothing is vetoed). */
    private static @Nullable Fc fc() {
        Fc f = fc;
        if (f != null || unresolved) return f;
        synchronized (FcAttachSupport.class) {
            if (fc != null || unresolved) return fc;
            try {
                Class<?> torchBase = Class.forName(FC_TORCH_BASE);
                Class<?> ladderBase = Class.forName(FC_LADDER_BASE);
                Class<?> groundCover = Class.forName(FC_GROUND_COVER);
                Class<?> pressurePlate = Class.forName(FC_PRESSURE_PLATE);
                Class<?> utilsWorld = Class.forName(FC_UTILS_WORLD);
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle canPlaceTorchOn = lookup.unreflect(torchBase.getMethod("canPlaceTorchOn",
                        btw.modern.World.class, int.class, int.class, int.class));
                MethodHandle centerHardpoint = lookup.unreflect(utilsWorld.getMethod(
                        "DoesBlockHaveCenterHardpointToFacing", btw.modern.IBlockAccess.class,
                        int.class, int.class, int.class, int.class));
                MethodHandle canAttachToFacing = lookup.unreflect(ladderBase.getMethod("CanAttachToFacing",
                        btw.modern.World.class, int.class, int.class, int.class, int.class));
                fc = new Fc(torchBase, ladderBase, groundCover, pressurePlate, canPlaceTorchOn, centerHardpoint,
                        canAttachToFacing);
                LOGGER.info("FC attachment rules resolved: {}, {}, {} and {} answer 1.20.1's torch, ladder, snow layer"
                        + " and pressure plate supports", torchBase.getSimpleName(), ladderBase.getSimpleName(),
                        groundCover.getSimpleName(), pressurePlate.getSimpleName());
            } catch (Throwable t) {
                unresolved = true;
                LOGGER.warn("FC's torch, ladder, ground cover or pressure plate attachment rule could not be resolved;"
                        + " 1.20.1's torch, ladder, snow layer and pressure plate supports are not vetoed", t);
            }
            return fc;
        }
    }
}
