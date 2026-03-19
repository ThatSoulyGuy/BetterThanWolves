package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A real {@link net.minecraft.world.level.block.Block} that forwards all
 * callbacks to a legacy FC block ({@link btw.modern.Block}).
 *
 * Each BTW block ID (175+) gets one ProxyBlock instance registered with
 * Forge registries.  The 4-bit metadata is encoded as an {@link IntegerProperty}
 * named "meta" (0-15), matching the legacy 1.5.2 data values.
 *
 * Every vanilla Block callback that has a corresponding method on
 * btw.modern.Block is forwarded here.
 */
public class ProxyBlock extends Block {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyBlock");

    /** 4-bit metadata property, matching legacy MC 1.5.2 block data. */
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    // ThreadLocal to pass fcBlock through super() constructor call
    private static final ThreadLocal<btw.modern.Block> CONSTRUCTING = new ThreadLocal<>();

    private final int legacyId;
    private final btw.modern.Block fcBlock;

    public ProxyBlock(int legacyId, btw.modern.Block fcBlock) {
        super(initAndBuildProperties(fcBlock));
        this.legacyId = legacyId;
        this.fcBlock = fcBlock;
        CONSTRUCTING.remove();
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    private static BlockBehaviour.Properties initAndBuildProperties(btw.modern.Block fcBlock) {
        CONSTRUCTING.set(fcBlock);
        return buildProperties(fcBlock);
    }

    /** Get the FC block — works during construction via ThreadLocal */
    private btw.modern.Block fc() {
        btw.modern.Block b = this.fcBlock;
        return b != null ? b : CONSTRUCTING.get();
    }

    /**
     * Builds modern Block.Properties from the legacy FC block's fields.
     */
    private static BlockBehaviour.Properties buildProperties(btw.modern.Block fcBlock) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of();

        float hardness = fcBlock.blockHardness;
        float resistance = fcBlock.getExplosionResistance(null);
        if (hardness < 0) {
            props = props.strength(-1.0F, 3600000.0F);
        } else {
            props = props.strength(hardness, resistance);
        }

        int lightVal = btw.modern.Block.lightValue[fcBlock.blockID];
        if (lightVal > 0) {
            final int lv = lightVal;
            props = props.lightLevel(state -> lv);
        }

        if (fcBlock.getTickRandomly()) {
            props = props.randomTicks();
        }

        if (!fcBlock.isOpaqueCube()) {
            props = props.noOcclusion();
        }

        return props;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    /**
     * Returns the underlying legacy FC block.
     */
    public btw.modern.Block getFcBlock() {
        return fc();
    }

    /**
     * Returns the legacy block ID.
     */
    public int getLegacyId() {
        return legacyId;
    }

    // ================================================================
    // Forwarded callbacks
    // ================================================================

    // --- tick (scheduled update) ---

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        btw.modern.World world = WorldBridge.getOrCreate(level);
        fc().updateTick(world, pos.getX(), pos.getY(), pos.getZ(), new Random(random.nextLong()));
    }

    // --- randomTick ---

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        btw.modern.World world = WorldBridge.getOrCreate(level);
        fc().RandomUpdateTick(world, pos.getX(), pos.getY(), pos.getZ(), new Random(random.nextLong()));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return fc().getTickRandomly();
    }

    // --- neighborChanged ---

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos neighborPos, boolean moving) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            // Resolve the neighbor's legacy block ID: if it is a ProxyBlock
            // we can read the ID directly, otherwise fall back to 0.
            int neighborId = 0;
            if (block instanceof ProxyBlock pb) {
                neighborId = pb.getLegacyId();
            }
            fc().onNeighborBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), neighborId);
        }
    }

    // --- onPlace ---

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean moving) {
        if (!state.is(oldState.getBlock())) {
            if (level instanceof ServerLevel sl) {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                fc().onBlockAdded(world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    // --- onRemove ---

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level instanceof ServerLevel sl) {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                fc().breakBlock(world, pos.getX(), pos.getY(), pos.getZ(),
                        legacyId, state.getValue(META));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    // --- use (onBlockActivated) ---

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            int side = hit.getDirection().get3DDataValue();
            Vec3 hitLoc = hit.getLocation();
            // Player wrapping is not yet implemented -- pass null for now.
            boolean result = fc().onBlockActivated(world, pos.getX(), pos.getY(), pos.getZ(),
                    null, side,
                    (float)(hitLoc.x - pos.getX()),
                    (float)(hitLoc.y - pos.getY()),
                    (float)(hitLoc.z - pos.getZ()));
            return result ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    // --- attack (onBlockClicked) ---

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            // Player wrapping not yet implemented
            fc().onBlockClicked(world, pos.getX(), pos.getY(), pos.getZ(), null);
        }
    }

    // --- entityInside (onEntityCollidedWithBlock) ---

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            // Entity wrapping not yet implemented -- pass null for now.
            fc().onEntityCollidedWithBlock(world, pos.getX(), pos.getY(), pos.getZ(), null);
        }
    }

    // --- stepOn (onEntityWalking) ---

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            fc().onEntityWalking(world, pos.getX(), pos.getY(), pos.getZ(), null);
        }
    }

    // --- fallOn (onFallenUpon) ---

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity,
                       float fallDistance) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            fc().onFallenUpon(world, pos.getX(), pos.getY(), pos.getZ(), null, fallDistance);
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    // --- getDestroySpeed (getPlayerRelativeBlockHardness mapped to destroy speed) ---

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // FC computes hardness; if < 0 the block is unbreakable.
        float hardness = fc().blockHardness;
        if (hardness < 0.0F) {
            return 0.0F;
        }
        // Vanilla formula: 1 / (hardness * 30) for non-tool, 1 / (hardness * 100) for tool.
        // We return the base speed; Forge applies tool modifiers on top.
        return 1.0F / (hardness * 30.0F);
    }

    // --- getExplosionResistance ---

    @Override
    public float getExplosionResistance() {
        return fc().getExplosionResistance(null);
    }

    // --- canSurvive (canBlockStay) ---

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        // canBlockStay needs a btw.modern.World, but LevelReader is not always a ServerLevel.
        // For non-server contexts, default to true.
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            return fc().canBlockStay(world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    // --- getDrops ---

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        int meta = state.getValue(META);
        Random rand = new Random();
        int droppedId = fc().idDropped(meta, rand, 0);
        int quantity = fc().quantityDropped(rand);
        int damage = fc().damageDropped(meta);
        if (droppedId > 0 && quantity > 0) {
            net.minecraft.world.item.Item modernItem = null;
            // If the dropped ID is a block ID, look it up as a block item first
            net.minecraft.world.level.block.Block droppedBlock = ProxyRegistry.getModernBlock(droppedId);
            if (droppedBlock != null) {
                modernItem = droppedBlock.asItem();
            }
            if (modernItem == null) {
                modernItem = ProxyRegistry.getModernItem(droppedId);
            }
            if (modernItem != null) {
                drops.add(new ItemStack(modernItem, quantity));
            }
        }
        return drops;
    }

    // --- isSignalSource (canProvidePower) ---

    @Override
    public boolean isSignalSource(BlockState state) {
        return fc().canProvidePower();
    }

    // --- getSignal (isProvidingWeakPower) ---

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // FC's isProvidingWeakPower uses IBlockAccess; we cannot easily construct one
        // from BlockGetter without a full world. Return 0 for the general case;
        // when called from a ServerLevel context, the correct value is forwarded.
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            return fc().isProvidingWeakPower(world, pos.getX(), pos.getY(), pos.getZ(),
                    direction.get3DDataValue());
        }
        return 0;
    }

    // --- getDirectSignal (isProvidingStrongPower) ---

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            return fc().isProvidingStrongPower(world, pos.getX(), pos.getY(), pos.getZ(),
                    direction.get3DDataValue());
        }
        return 0;
    }

    // --- hasAnalogOutputSignal (hasComparatorInputOverride) ---

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return fc().hasComparatorInputOverride();
    }

    // --- getAnalogOutputSignal (getComparatorInputOverride) ---

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            int side = 0;
            return fc().getComparatorInputOverride(world, pos.getX(), pos.getY(), pos.getZ(), side);
        }
        return 0;
    }

    // ================================================================
    // Passive property queries — engine reads these constantly
    // ================================================================

    @Override
    public float getSpeedFactor() {
        float modifier = fc().GetMovementModifier(null, 0, 0, 0);
        return modifier > 0 ? modifier : 1.0F;
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        double minX = fc().minX, minY = fc().minY, minZ = fc().minZ;
        double maxX = fc().maxX, maxY = fc().maxY, maxZ = fc().maxZ;
        return net.minecraft.world.phys.shapes.Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return getCollisionShape(state, level, pos, ctx);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return btw.modern.Block.lightOpacity[legacyId];
    }

    @Override
    public net.minecraft.world.level.block.SoundType getSoundType(BlockState state) {
        btw.modern.StepSound ss = fc().stepSound;
        if (ss != null) {
            return translateStepSound(ss);
        }
        return super.getSoundType(state);
    }

    // ================================================================
    // Step sound mapping (replaces the removed StepSound.translateToModern)
    // ================================================================

    private static final Map<String, net.minecraft.world.level.block.SoundType> SOUND_MAP = new HashMap<>();

    static {
        SOUND_MAP.put("stone",  net.minecraft.world.level.block.SoundType.STONE);
        SOUND_MAP.put("wood",   net.minecraft.world.level.block.SoundType.WOOD);
        SOUND_MAP.put("gravel", net.minecraft.world.level.block.SoundType.GRAVEL);
        SOUND_MAP.put("grass",  net.minecraft.world.level.block.SoundType.GRASS);
        SOUND_MAP.put("cloth",  net.minecraft.world.level.block.SoundType.WOOL);
        SOUND_MAP.put("sand",   net.minecraft.world.level.block.SoundType.SAND);
        SOUND_MAP.put("snow",   net.minecraft.world.level.block.SoundType.SNOW);
        SOUND_MAP.put("ladder", net.minecraft.world.level.block.SoundType.LADDER);
        SOUND_MAP.put("anvil",  net.minecraft.world.level.block.SoundType.ANVIL);
        SOUND_MAP.put("metal",  net.minecraft.world.level.block.SoundType.METAL);
        SOUND_MAP.put("glass",  net.minecraft.world.level.block.SoundType.GLASS);
    }

    private static net.minecraft.world.level.block.SoundType translateStepSound(btw.modern.StepSound ss) {
        net.minecraft.world.level.block.SoundType mapped = SOUND_MAP.get(ss.stepSoundName);
        return mapped != null ? mapped : net.minecraft.world.level.block.SoundType.STONE;
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return fc().IsAirBlock();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return btw.modern.Block.lightOpacity[legacyId] == 0;
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return fc().renderAsNormalBlock() ?
            net.minecraft.world.level.block.RenderShape.MODEL :
            net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return !fc().isOpaqueCube();
    }

}
