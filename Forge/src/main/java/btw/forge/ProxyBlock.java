package btw.forge;

import btw.forge.farming.FcSoil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fml.DistExecutor;
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
public class ProxyBlock extends Block implements EntityBlock {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyBlock");

    /** 4-bit metadata property, matching legacy MC 1.5.2 block data. */
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    // ThreadLocal to pass fcBlock through super() constructor call
    private static final ThreadLocal<btw.modern.Block> CONSTRUCTING = new ThreadLocal<>();

    private final int legacyId;
    private final btw.modern.Block fcBlock;

    /**
     * Cached flag: true when the FC block's {@code createNewTileEntity(null)}
     * returns a non-null value, meaning this block needs a BlockEntity.
     * Computed once in the constructor and never changes.
     */
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
     * Derives the placement METADATA the way FC's FCItemPlacesAsBlock.onItemUse does before it sets the block
     * (Common .../FCItemPlacesAsBlock.java:61-98; the 1.5.2 ItemBlock extends it, vanilla/client ItemBlock.java:21):
     * <ol>
     *   <li>initial metadata = the held item's getMetadata(itemDamage) (:94), read from the stack in hand, not
     *       from this block's own item: btw.forge.ProxyBlockItem places the block FC's GetBlockIDToPlace names and
     *       delegates here with the same context;</li>
     *   <li>a replaced ground cover (snow layer, ash) forces facing 1 (:79-82);</li>
     *   <li>onBlockPlaced(target, facing, click, metadata) (:96), with the click relative to the CLICKED block as
     *       1.5.2 PlayerControllerMP.onPlayerRightClick passes it, not to the target cell;</li>
     *   <li>PreBlockPlacedBy(target, metadata, player) (:98), the only orientation hook of FC stairs, anvils,
     *       baskets/hampers and shovels (MPR-5: they always faced 0 when placed against a non-FC block).</li>
     * </ol>
     * onBlockPlacedBy and onPostBlockPlaced follow in {@link #setPlacedBy}. Runs on both sides: the client predicts
     * with a read-only FC view and a placer facade carrying the player's position and rotation, the server with
     * the WorldBridge and the player's PlayerBridge, so both compute the same META. FC's `side` numbering
     * (0/1=down/up, 2/3=N/S, 4/5=W/E) matches Direction.get3DDataValue exactly.
     */
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        BlockState base = super.getStateForPlacement(ctx);
        if (base == null) base = this.defaultBlockState();
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        int side = face.get3DDataValue();
        BlockPos clicked = ctx.replacingClickedOnBlock() ? pos : pos.relative(face.getOpposite());
        Vec3 hit = ctx.getClickLocation();
        float hx = (float) (hit.x - clicked.getX());
        float hy = (float) (hit.y - clicked.getY());
        float hz = (float) (hit.z - clicked.getZ());
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        btw.modern.World fcWorld;
        try {
            fcWorld = level instanceof ServerLevel sl ? WorldBridge.getOrCreate(sl) : createFcWorld(level, true);
        } catch (Throwable t) {
            placementFailure("PLACE-WORLD", level, pos, t);
            return base;
        }
        if (ctx.replacingClickedOnBlock() && isGroundCoverAt(level, pos)) {
            side = 1;
        }
        int meta = initialPlacementMetadata(ctx.getItemInHand(), level, pos);
        try {
            meta = fc().onBlockPlaced(fcWorld, x, y, z, side, hx, hy, hz, meta);
        } catch (Throwable t) {
            placementFailure("PLACE", level, pos, t);
        }
        Player player = ctx.getPlayer();
        if (player != null) { // the dispenser path (FCItemPlacesAsBlock.java:138-169) has no PreBlockPlacedBy
            try {
                meta = fc().PreBlockPlacedBy(fcWorld, x, y, z, meta, placingEntity(player, fcWorld));
            } catch (Throwable t) {
                placementFailure("PRE-PLACE", level, pos, t);
            }
        }
        return base.setValue(META, meta & 15);
    }

    /**
     * FCItemPlacesAsBlock.java:94 {@code getMetadata(itemStack.getItemDamage())} of the held item. A ProxyBlock's
     * BlockItem is FC item id == block id (1.5.2 ItemBlock(id - 256), FCBetterThanWolves.java:1778-1780); any other
     * item answers its ProxyRegistry legacy id. 0 when the item has no FC item (1.5.2 ItemBlock default).
     */
    private int initialPlacementMetadata(ItemStack held, Level level, BlockPos pos) {
        if (held == null || held.isEmpty()) return 0;
        try {
            int itemId = held.getItem() instanceof net.minecraft.world.item.BlockItem bi
                    && bi.getBlock() instanceof ProxyBlock pb
                    ? pb.getLegacyId() : ProxyRegistry.getItemId(held.getItem());
            if (itemId <= 0 || itemId >= btw.modern.Item.itemsList.length) return 0;
            btw.modern.Item fcItem = btw.modern.Item.itemsList[itemId];
            return fcItem != null ? fcItem.getMetadata(held.getDamageValue()) : 0;
        } catch (Throwable t) {
            placementFailure("PLACE-ITEM-META", level, pos, t);
            return 0;
        }
    }

    /** FCItemPlacesAsBlock.java:79-82: Block.IsGroundCover() of the block being replaced (read with live-world ids). */
    private static boolean isGroundCoverAt(Level level, BlockPos pos) {
        try {
            int id = ProxyRegistry.getWorldBlockId(level.getBlockState(pos), level, pos);
            btw.modern.Block old = id > 0 && id < btw.modern.Block.blocksList.length
                    ? btw.modern.Block.blocksList[id] : null;
            return old != null && old.IsGroundCover();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * The placer FC sees in PreBlockPlacedBy. Server: the player's PlayerBridge (getOrCreate syncs position and
     * rotation from the real player). Client: a facade with the same position and rotation, which is all the
     * FC PreBlockPlacedBy overrides read (rotationYaw/rotationPitch through FCUtilsMisc, position and yOffset in
     * the piston's determineOrientation), so the predicted META matches the server's. It is built on a null world
     * (the 1.5.2 Entity constructor reads world.provider, which the read-only view lacks) and then bound to it.
     */
    private static btw.modern.EntityLiving placingEntity(Player player, btw.modern.World fcWorld) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            return PlayerBridge.getOrCreate(sp);
        }
        btw.modern.EntityLiving facade = new btw.modern.EntityLiving((btw.modern.World) null) {
            public int getMaxHealth() { return 20; }
        };
        facade.worldObj = fcWorld;
        facade.setPosition(player.getX(), player.getY(), player.getZ());
        facade.prevPosX = facade.posX;
        facade.prevPosY = facade.posY;
        facade.prevPosZ = facade.posZ;
        facade.rotationYaw = facade.prevRotationYaw = player.getYRot();
        facade.rotationPitch = facade.prevRotationPitch = player.getXRot();
        return facade;
    }

    /**
     * A placement hook threw. Server: one WARN per throw site (warnTickFailureOnce). Client: the read-only view can
     * lack data FC reads during prediction; the server's state replaces the prediction, so only DEBUG.
     */
    private void placementFailure(String stage, Level level, BlockPos pos, Throwable t) {
        if (level != null && level.isClientSide()) {
            LOGGER.debug("[{}] client prediction for {} (id {}) at {} threw {}", stage,
                    fc().getClass().getSimpleName(), legacyId, pos, t.toString());
        } else {
            warnTickFailureOnce(stage, pos, t);
        }
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

        // Always disable occlusion for FC ProxyBlocks. The FC block instance
        // in blocksList[] may not be the final replaced version yet at this point
        // (BTWLifecycle replaces stubs after registration), so we can't trust
        // isOpaqueCube()/renderAsNormalBlock() here. The VoxelShape from
        // getOcclusionShape() handles actual occlusion correctly at runtime.
        props = props.noOcclusion();

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
        // D1 (TickPolicy.HONOUR_DO_FIRE_TICK): FC's own fires (FCBlockFireStoked, ...) obey doFireTick like fire 51
        if (TickPolicy.fireTickSuppressed(fc(), level)) {
            TickPolicy.rescheduleSuppressedFire(fc(), level, pos);
            return;
        }
        btw.modern.World world = WorldBridge.getOrCreate(level);
        try {
            fc().updateTick(world, pos.getX(), pos.getY(), pos.getZ(), new Random(random.nextLong()));
        } catch (Exception e) {
            warnTickFailureOnce("TICK", pos, e); // was silent for every block but kilns
        }
    }

    // --- randomTick ---

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (TickPolicy.fireTickSuppressed(fc(), level)) return;   // D1, as tick() above
        btw.modern.World world = WorldBridge.getOrCreate(level);
        try {
            fc().RandomUpdateTick(world, pos.getX(), pos.getY(), pos.getZ(), new Random(random.nextLong()));
        } catch (Throwable e) {
            warnTickFailureOnce("RANDOM-TICK", pos, e); // was an ERROR with stack on every failing random tick
        }
    }

    /** Tick failure sites already reported by {@link #warnTickFailureOnce}. */
    private static final java.util.Set<String> LOGGED_TICK_FAILURES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * One WARN with its stack trace per distinct throw site (stage, block id, exception class and top frame),
     * triage HAI-10 / design_build_logging U10: a block failing every tick neither floods the log nor hides.
     */
    private void warnTickFailureOnce(String stage, BlockPos pos, Throwable e) {
        StackTraceElement[] frames = e.getStackTrace();
        String site = stage + '|' + legacyId + '|' + e.getClass().getName() + '@' + (frames.length > 0 ? frames[0] : "?");
        if (LOGGED_TICK_FAILURES.add(site)) {
            LOGGER.warn("[{}] {} (id {}) at {} threw {}", stage, fc().getClass().getSimpleName(), legacyId, pos,
                    e.toString(), e);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return fc().getTickRandomly();
    }

    // --- animateTick (client-side particle effects) ---

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        try {
            // Lightweight FC World over the modern level (see createFcWorld); client particle display, so
            // neighbours read identity ids (ProxyRegistry.getBlockId).
            btw.modern.World clientWorld = createFcWorld(level, false);
            fc().randomDisplayTick(clientWorld, pos.getX(), pos.getY(), pos.getZ(),
                    new Random(random.nextLong()));
        } catch (Exception ignored) {
        }
        weedCue(level, pos, random);
    }

    /**
     * Client cue for weeds under a modded crop (INTEGRATIONS #026): FC draws its weeds as part of its own crops' model
     * (FCBlockCrops bounds and RenderCrops), which a modded crop's model lacks, so FC soil with weeds under a governed
     * modded CropBlock (one the plant stand-in stands for) gives off one or two composter specks at the crop's base.
     * Display only: the weeds themselves are the soil's metadata, which FC grows and the weed pull clears.
     */
    // FCMOD-INTEGRATION(1.20.1): modded_plant_rules: weeds under a modded crop show as particles, since the crop's own model cannot draw FC's weeds
    private void weedCue(Level level, BlockPos pos, RandomSource random) {
        try {
            if (!ModernPlantStandIn.isNeeded() || random.nextInt(4) != 0) return;
            BlockPos above = pos.above();
            BlockState plant = level.getBlockState(above);
            if (!(plant.getBlock() instanceof CropBlock) || !FcSoil.isGovernedPlant(plant)) return;
            if (fc().GetWeedsGrowthLevel(createFcWorld(level, false), pos.getX(), pos.getY(), pos.getZ()) <= 0) return;
            int specks = 1 + random.nextInt(2);
            for (int n = 0; n < specks; n++) {
                double x = above.getX() + 0.2D + random.nextDouble() * 0.6D;
                double y = above.getY() + 0.05D + random.nextDouble() * 0.2D;
                double z = above.getZ() + 0.2D + random.nextDouble() * 0.6D;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.COMPOSTER, x, y, z, 0.0D, 0.0D, 0.0D);
            }
        } catch (RuntimeException ignored) {
            // display only: a failing read draws nothing
        }
    }

    public static net.minecraft.core.particles.ParticleOptions mapParticle(String name) {
        if (name == null) return null;
        if (name.equals("smoke")) return net.minecraft.core.particles.ParticleTypes.SMOKE;
        if (name.equals("largesmoke")) return net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
        if (name.equals("flame")) return net.minecraft.core.particles.ParticleTypes.FLAME;
        if (name.equals("lava")) return net.minecraft.core.particles.ParticleTypes.LAVA;
        if (name.equals("splash")) return net.minecraft.core.particles.ParticleTypes.SPLASH;
        if (name.equals("bubble")) return net.minecraft.core.particles.ParticleTypes.BUBBLE;
        if (name.equals("reddust")) return new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
        if (name.equals("snowballpoof")) return net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL;
        if (name.equals("explode")) return net.minecraft.core.particles.ParticleTypes.EXPLOSION;
        if (name.equals("largeexplode")) return net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER;
        if (name.equals("townaura")) return net.minecraft.core.particles.ParticleTypes.MYCELIUM;
        if (name.equals("crit")) return net.minecraft.core.particles.ParticleTypes.CRIT;
        if (name.equals("magicCrit")) return net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT;
        if (name.equals("happyVillager")) return net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER;
        if (name.equals("note")) return net.minecraft.core.particles.ParticleTypes.NOTE;
        if (name.equals("portal")) return net.minecraft.core.particles.ParticleTypes.PORTAL;
        if (name.equals("enchantmenttable")) return net.minecraft.core.particles.ParticleTypes.ENCHANT;
        if (name.equals("witchMagic")) return net.minecraft.core.particles.ParticleTypes.WITCH;
        if (name.equals("snowshovel")) return net.minecraft.core.particles.ParticleTypes.POOF;
        if (name.equals("dripWater")) return net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER;
        if (name.equals("dripLava")) return net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA;
        if (name.equals("suspended")) return net.minecraft.core.particles.ParticleTypes.UNDERWATER;
        if (name.equals("heart")) return net.minecraft.core.particles.ParticleTypes.HEART;
        if (name.equals("fcwhitesmoke")) return net.minecraft.core.particles.ParticleTypes.CLOUD;
        if (name.equals("fcblacksmoke")) return net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
        if (name.equals("fcsteam")) return net.minecraft.core.particles.ParticleTypes.CLOUD;
        if (name.equals("angryVillager")) return net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER;
        if (name.startsWith("iconcrack_")) return net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL;
        if (name.startsWith("blockcrack_")) return net.minecraft.core.particles.ParticleTypes.POOF;
        if (name.equals("fccinders")) return net.minecraft.core.particles.ParticleTypes.FLAME;
        if (name.equals("fcwhitecloud")) return net.minecraft.core.particles.ParticleTypes.CLOUD;
        return null;
    }

    // --- neighborChanged ---

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos neighborPos, boolean moving) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            if (untillUnderRemovedModdedCrop(state, sl, world, pos, block, neighborPos)) return;
            int neighborId = ProxyRegistry.getNeighbourSourceId(block, level, neighborPos);
            fc().onNeighborBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), neighborId);
        }
    }

    /**
     * FCBlockPlants.breakBlock for a governed modded crop (INTEGRATIONS #026): FC's own crops tell the soil below that
     * they were removed (Server FCBlockPlants.java:47-56), and FC farmland untills to loose dirt
     * (FCBlockFarmland.NotifyOfPlantAboveRemoved, Common FCBlockFarmland.java:68-76, the only implementer, which never
     * reads the plant). A modded crop has no legacy id, so BlockBehaviorMixin.onRemove never runs FC's breakBlock for
     * it; its removal reaches this soil only as the neighbour update Level.markAndNotifyBlock sends with the old block
     * (Level.java:250-251). Twins untill through ProxyRegistry.getRemovalLegacyId (S16) and are not governed here. A
     * cell that now holds the same block or another plant grew or was replaced by FC's weeds (a growth transform),
     * not removed. True when the soil changed, so its own neighbour logic must not run on the stale state.
     */
    // FCMOD-INTEGRATION(1.20.1): modded_plant_rules: removing a modded crop untills FC farmland under it, as FC's breakBlock does for its own crops
    private boolean untillUnderRemovedModdedCrop(BlockState state, ServerLevel level, btw.modern.World world,
                                                 BlockPos pos, Block removed, BlockPos neighborPos) {
        if (!(removed instanceof CropBlock) || !neighborPos.equals(pos.above())) return false;
        try {
            if (!FcSoil.isGovernedPlantBlock(removed)) return false;
            Block now = level.getBlockState(neighborPos).getBlock();
            if (now == removed || now instanceof BushBlock || now instanceof IPlantable) return false;
            fc().NotifyOfPlantAboveRemoved(world, pos.getX(), pos.getY(), pos.getZ(), null);
        } catch (Throwable t) {
            warnTickFailureOnce("PLANT-REMOVED", pos, t);
            return false;
        }
        return level.getBlockState(pos) != state;
    }

    // --- isFertile (Forge: CropBlock.getGrowthSpeed's soil bonus) ---

    /**
     * FC's hydration answer for this soil (IsBlockHydratedForPlantGrowthOn: FC farmland's moisture, planter soil):
     * IForgeBlock.isFertile knows only Blocks.FARMLAND, so every FC soil read as infertile to 1.20.1 growth-speed
     * code. FC answers from the soil cell's own metadata, so the cell is resolved first: Forge 47.3.0's
     * CropBlock.getGrowthSpeed (CropBlock.java:102-105, also reached from StemBlock and PitcherCropBlock) passes the
     * PLANT cell above the soil ({@code pos.offset}, not {@code pos.below().offset}), other callers the soil itself;
     * Forge's default ignores the position, FC's metadata read does not. Server only; a position holding neither this
     * soil nor a cell above it, other readers, and an FC failure (logged once) keep Forge's default.
     */
    // FCMOD-INTEGRATION(1.20.1): modded_plant_rules: Forge's soil fertility question gets FC's hydration answer on FC soils, read at the soil cell whichever cell the caller passes
    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            try {
                BlockPos soil = sl.getBlockState(pos) == state ? pos
                        : sl.getBlockState(pos.below()) == state ? pos.below() : null;
                if (soil != null) {
                    return fc().IsBlockHydratedForPlantGrowthOn(WorldBridge.getOrCreate(sl), soil.getX(),
                            soil.getY(), soil.getZ());
                }
            } catch (Throwable t) {
                warnTickFailureOnce("FERTILE", pos, t);
            }
        }
        return super.isFertile(state, level, pos);
    }

    // --- onPlace ---

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean moving) {
        if (!state.is(oldState.getBlock())) {
            if (level instanceof ServerLevel sl) {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                fc().onBlockAdded(world, pos.getX(), pos.getY(), pos.getZ());
                // onBlockPlacedBy is called by setPlacedBy() which MC fires
                // synchronously with the correct placer entity and rotation.
            }
        }
    }

    // --- setPlacedBy (onBlockPlacedBy) ---

    /**
     * FCItemPlacesAsBlock.onItemUse:103-108 once the block is set and still there (BlockItem.place calls this only
     * then): onBlockPlacedBy(player, stack), then onPostBlockPlaced(metadata) (FCBlockLens beam setup, ...), then a
     * player's NotifyNearbyAnimalsOfPlayerBlockAddOrRemove. Without a placer only onPostBlockPlaced runs, as on FC's
     * dispenser path (FCItemPlacesAsBlock.java:162).
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        if (!(level instanceof ServerLevel sl)) return;
        btw.modern.World world = WorldBridge.getOrCreate(sl);
        btw.modern.EntityLiving fcPlacer = null;
        if (placer != null) {
            try {
                if (placer instanceof net.minecraft.server.level.ServerPlayer sp) {
                    fcPlacer = PlayerBridge.getOrCreate(sp);
                    ((PlayerBridge) fcPlacer).syncFromReal();
                }
                if (fcPlacer == null) {
                    // Create a minimal EntityLiving with position/rotation for facing calc
                    fcPlacer = new btw.modern.EntityLiving(world) {
                        public int getMaxHealth() { return 20; }
                    };
                    fcPlacer.posX = placer.getX();
                    fcPlacer.posY = placer.getY();
                    fcPlacer.posZ = placer.getZ();
                    fcPlacer.rotationYaw = placer.getYRot();
                    fcPlacer.rotationPitch = placer.getXRot();
                }
                // FCItemPlacesAsBlock.java:105 passes the stack being placed (before its stackSize--): BlockItem.place
                // hands it over before its shrink, and it is the OFF-hand stack when the main hand passed, so the
                // player's equipped item would give FCBlockTorchFiniteBurning (:93-117) the wrong "outTime" or none.
                btw.modern.ItemStack fcStack = placedFcStack(stack, pos);
                if (fcStack == null && fcPlacer instanceof PlayerBridge pb) {
                    fcStack = pb.getCurrentEquippedItem();
                }
                fc().onBlockPlacedBy(world, pos.getX(), pos.getY(), pos.getZ(), fcPlacer, fcStack);
            } catch (Throwable t) {
                warnTickFailureOnce("PLACED-BY", pos, t);
            }
        }
        try {
            fc().onPostBlockPlaced(world, pos.getX(), pos.getY(), pos.getZ(), state.getValue(META));
        } catch (Throwable t) {
            warnTickFailureOnce("POST-PLACE", pos, t);
        }
        if (fcPlacer instanceof PlayerBridge pb) {
            // FCItemPlacesAsBlock.java:110-111: a player's placement spooks nearby animals (World.java:4678).
            try {
                world.NotifyNearbyAnimalsOfPlayerBlockAddOrRemove(pb, fc(), pos.getX(), pos.getY(), pos.getZ());
            } catch (Throwable t) {
                warnTickFailureOnce("PLACE-NOTIFY", pos, t);
            }
        }
    }

    /** The FC view (count, damage, NBT) of the stack BlockItem.place placed; null when absent or unconvertible. */
    private btw.modern.ItemStack placedFcStack(ItemStack stack, BlockPos pos) {
        if (stack == null || stack.isEmpty()) return null;
        try {
            return ItemStackHelper.toFcStack(stack);
        } catch (Throwable t) {
            warnTickFailureOnce("PLACED-BY-STACK", pos, t);
            return null;
        }
    }

    // --- onRemove --- (moved to EntityBlock section at bottom of class)

    // --- use (onBlockActivated) ---

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: Stage 1 and Stage 2 run for the main hand only, as 1.5.2's one hand; the off-hand pass leaves the block alone
            // An OFF_HAND use (the 1.20.1 client's second pass after a main-hand pass that did not consume,
            // Minecraft.java:1662, :1698-1720, or another mod's call) would re-run Stage 1 and use the MAIN-hand FC item
            // (the FC player has only the main hand): a second activation or a stuck main-hand tool from one click.
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            PlayerBridge fcPlayer = PlayerBridge.getOrCreate(player);
            fcPlayer.syncFromReal();
            int side = hit.getDirection().get3DDataValue();
            Vec3 hitLoc = hit.getLocation();
            float hitX = (float)(hitLoc.x - pos.getX());
            float hitY = (float)(hitLoc.y - pos.getY());
            float hitZ = (float)(hitLoc.z - pos.getZ());

            btw.modern.ItemStack fcHeld = fcPlayer.getCurrentEquippedItem();

            // Snapshot openContainer before FC call to detect GUI opens
            btw.modern.Container prevContainer = fcPlayer.openContainer;

            // Stage 1: FC block activation
            boolean result = fc().onBlockActivated(world, pos.getX(), pos.getY(), pos.getZ(),
                    fcPlayer, side, hitX, hitY, hitZ);

            if (!result) {
                // Stage 2: FC item use on block (DENY, click mark, creative restore and stat: tryPlaceHeldItem)
                result = tryPlaceHeldItem(fcHeld, player, fcPlayer, world, pos, side, hitX, hitY, hitZ);
            }

            // Check if FC opened a container GUI and open the MC menu
            boolean containerOpened = ContainerBridge.checkAndOpenContainer(fcPlayer, prevContainer);

            // Sync FC item state (damage, stackSize) back to MC inventory.
            if (fcPlayer.inventory instanceof InventoryBridge invBridge) {
                invBridge.writeBackAll();
                // INV-RESYNC: the client sees the held stack FC left, as after every 1.5.2 place packet
                // (NetServerHandler.java:551-569)
                if (result) invBridge.resendHeldSlot();
            }

            // Force immediate tile entity sync to clients so visual changes
            // (cook stack, spit, metadata) are visible without waiting for
            // the periodic 20-tick sync.
            if (result) {
                net.minecraft.world.level.block.entity.BlockEntity be = sl.getBlockEntity(pos);
                if (be instanceof ProxyBlockEntity pbe) {
                    pbe.syncToClients();
                }
            }

            // Always return SUCCESS if a container was opened or FC handled the activation
            // This prevents MC from calling use() again for the off hand
            return (result || containerOpened) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        // CLIENT: the client cannot predict FC's activation, so it claims the click. MultiPlayerGameMode.useItemOn sends
        // ServerboundUseItemOnPacket whatever this returns (MultiPlayerGameMode.java:284-296); the SUCCESS only stops
        // the client's own item step, its use-item packet and its off-hand pass (Minecraft.java:1698-1707), so the
        // server decides the click alone.
        return InteractionResult.SUCCESS;
    }

    /**
     * Stage 2 of 1.5.2 ItemInWorldManager.activateBlockOrUseItem (vanilla/server ItemInWorldManager.java:390-404):
     * the held stack's tryPlaceItemIntoWorld, with its damage and stackSize restored afterwards in creative
     * (:394-402; Stage 1, onBlockActivated, is never restored). 1.20.1 restores the creative count only around
     * ItemStack.useOn (ServerPlayerGameMode.useItemOn), not around Block.use, where the bridge runs this, so
     * without the restore the write-back that follows commits FC's in-place decrement / damage (a block placed
     * against FC terrain, a planted seed, flint and steel) to a creative inventory.
     *
     * <p>FCMOD-INTEGRATION(1.20.1): fc_item_use_order: this is the single FC {@code onItemUse} point of a click on a
     * block. Its callers are Stage 2 inside the clicked block's use (ProxyBlock.use; BlockBehaviorMixin.btw$use for
     * vanilla-id blocks with an FC replacement) and 1.20.1's item step for the blocks Stage 2 did not serve
     * (ServerPlayerGameModeMixin.btw$fcItemStep). In order: a RightClickBlock {@code useItem} DENY of this click
     * refuses with nothing recorded (FcItemUseOrder; 1.20.1 refuses its own item step on it,
     * ServerPlayerGameMode.java:351); the click frame records that FC ran here, so the item step never runs it a second
     * time; and a true result awards {@code Stats.ITEM_USED} for the clicked item, as 1.5.2 tryPlaceItemIntoWorld did
     * (vanilla/server ItemStack.java:127-130). The ITEM_USED_ON_BLOCK criterion is not triggered here: a Stage 2
     * success makes block use consume, which triggers it (ServerPlayerGameMode.java:344-345), and the item step
     * triggers it itself.</p>
     *
     * <p>FCMOD-INTEGRATION(1.20.1): placement_validity: two more steps. An FC door item ({@code FCItemDoor}) is declined
     * before FC runs, with nothing recorded, so 1.20.1's DoorItem places the door (see {@link #isPortGapDoorItem}). And
     * when the clicked item is a 1.20.1 BlockItem whose FC item is of the 4.B ItemBlock family
     * ({@link #isItemBlockFamily}), the click frame records FC's verdict ({@link PlayerBridge.Stage2}): PLACED, with the
     * cell FC put the held block in, or REFUSED (false, or a throw) for a player who may build. A REFUSED placement is
     * final: the item step does not let 1.20.1's BlockItem place it instead (ServerPlayerGameModeMixin.btw$fcItemStep).
     * An adventure-mode refusal records nothing, so 1.20.1's CanPlaceOn rule decides at the item step.</p>
     */
    public static boolean tryPlaceHeldItem(btw.modern.ItemStack fcHeld, Player player, PlayerBridge fcPlayer,
                                           btw.modern.World world, BlockPos pos, int side,
                                           float hitX, float hitY, float hitZ) {
        if (fcHeld == null) return false;
        // Only bone meal is FC fertilizer (BoneMealPolicy). 1.20.1 white dye reads as FC (351, 15|0x100) = 271 through
        // LegacyVariants.MODERN_SPELLING, never FC dye 15, so FCItemDye declines it; this guard keeps any other spelling
        // that reads as dye 15 away from FCItemDye.
        if (!BoneMealPolicy.mayUseAsFcItem(player.getMainHandItem(), fcHeld)) return false;
        // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: a claim mod's useItem DENY stops FC item use here as 1.20.1 stops its item step; a refusal records nothing
        if (fcPlayer.isFcUseDenied()) return false;
        // FCMOD-INTEGRATION(1.20.1): placement_validity: the oak door (FC item 324) is placed by 1.20.1's DoorItem until the door codec exists (hook delta A8/G3); declining records nothing, it is a port gap, not a 4.B verdict
        if (isPortGapDoorItem(fcHeld)) return false;
        // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: FC onItemUse runs once per click; the item step skips a block it already ran at
        fcPlayer.markFcItemUse(pos);
        // The clicked item, read before FC runs: FC may use up the stack
        net.minecraft.world.item.Item mcItem = player.getMainHandItem().getItem();
        // FCMOD-INTEGRATION(1.20.1): placement_validity: FC's verdict on a 1.20.1 BlockItem of the 4.B ItemBlock family is recorded on the click frame (PLACED with its cell, or REFUSED)
        // The two cells FC's ItemBlock can place in (the clicked cell when it is ground cover or replaceable, else the
        // one beside the clicked face, Modern-Common ItemBlock.java:163-175) are read before FC runs, so the record
        // names the cell FC filled, whatever the stack holds afterwards (the last item of a stack leaves it empty).
        Block heldBlock = mcItem instanceof net.minecraft.world.item.BlockItem bi && isItemBlockFamily(fcHeld)
                ? bi.getBlock() : null;
        Level level = heldBlock != null ? player.level() : null;
        BlockPos sidePos = heldBlock != null ? pos.relative(Direction.from3DDataValue(side)) : null;
        BlockState clickedBefore = level != null ? level.getBlockState(pos) : null;
        BlockState sideBefore = level != null ? level.getBlockState(sidePos) : null;
        boolean used = runFcItemUse(fcHeld, player, fcPlayer, world, pos, side, hitX, hitY, hitZ);
        // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: 4.B's item-used stat, awarded once on both routes (vanilla/server ItemStack.java:127-130)
        if (used && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(mcItem));
        }
        if (heldBlock != null) {
            if (used) {
                BlockPos placedAt = null;
                BlockState sideNow = level.getBlockState(sidePos);
                BlockState clickedNow = level.getBlockState(pos);
                if (sideNow != sideBefore && sideNow.is(heldBlock)) {
                    placedAt = sidePos;
                } else if (clickedNow != clickedBefore && clickedNow.is(heldBlock)) {
                    placedAt = pos;
                }
                fcPlayer.setStage2(PlayerBridge.Stage2.PLACED, placedAt);
            } else if (player.getAbilities().mayBuild) {
                // FCMOD-INTEGRATION(1.20.1): placement_validity: REFUSED only for a player who may build; an adventure-mode refusal is not 4.B's verdict and records nothing: FC's canPlayerEdit refuses every placement without mayBuild (CapabilitiesBridge.java:24, allowEdit), and 1.5.2 had no CanPlaceOn, so 1.20.1's rule decides it at the item step (ForgeHooks.onPlaceItemIntoWorld)
                fcPlayer.setStage2(PlayerBridge.Stage2.REFUSED);
            }
        }
        return used;
    }

    /** {@code net.minecraft.src.btw.item.FCItemPlacesAsBlock} (Common), resolved once; FC classes are not on the
     *  bridge's compile path (Forge/build.gradle compiles against Api and Modern-Common only). */
    private static volatile Class<?> fcItemPlacesAsBlockClass;
    /** {@code net.minecraft.src.btw.item.FCItemDoor} (Common), resolved once. */
    private static volatile Class<?> fcItemDoorClass;
    /** Set once a class above could not be resolved, so the failure is logged a single time per class. */
    private static final java.util.Set<String> FC_CLASS_FAILURES = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Marks a class that is not there (the lookup is not retried). */
    private static final Class<?> FC_CLASS_MISSING = Void.class;

    /** The FC class {@code name}, or null; looked up once, logged once when it is missing. */
    private static Class<?> fcClass(String name) {
        try {
            return Class.forName(name);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            if (FC_CLASS_FAILURES.add(name)) {
                LOGGER.warn("[PLACEMENT] FC class {} not found ({}); placement treats no item as one", name,
                        e.toString());
            }
            return null;
        }
    }

    private static Class<?> fcItemPlacesAsBlockClass() {
        Class<?> c = fcItemPlacesAsBlockClass;
        if (c == null) {
            c = fcClass("net.minecraft.src.btw.item.FCItemPlacesAsBlock");
            fcItemPlacesAsBlockClass = c = c != null ? c : FC_CLASS_MISSING;
        }
        return c != FC_CLASS_MISSING ? c : null;
    }

    private static Class<?> fcItemDoorClass() {
        Class<?> c = fcItemDoorClass;
        if (c == null) {
            c = fcClass("net.minecraft.src.btw.item.FCItemDoor");
            fcItemDoorClass = c = c != null ? c : FC_CLASS_MISSING;
        }
        return c != FC_CLASS_MISSING ? c : null;
    }

    /**
     * True when {@code fcHeld}'s item places a block the 4.B way, so that its onItemUse "false" is 4.B's verdict on the
     * placement: Modern-Common {@code ItemBlock} (1.5.2's ItemBlock extends FCItemPlacesAsBlock, vanilla/server
     * ItemBlock.java:16-19; its port ports that onItemUse, Modern-Common ItemBlock.java:144-223, and covers the
     * ItemMultiTextureTile / ItemBlockWithMetadata / ItemColored stubs FC extends) and Common
     * {@code FCItemPlacesAsBlock} (FCItemRedstone, bricks, candles, ...). Not the FC door and sign items, whose
     * Modern-Common bases are stubs (ItemDoor.placeDoorBlock, ItemSign), nor headless items (a plain Item).
     */
    static boolean isItemBlockFamily(btw.modern.ItemStack fcHeld) {
        btw.modern.Item item = fcHeld != null ? fcHeld.getItem() : null;
        if (item == null) return false;
        if (item instanceof btw.modern.ItemBlock) return true;
        Class<?> placesAsBlock = fcItemPlacesAsBlockClass();
        return placesAsBlock != null && placesAsBlock.isInstance(item);
    }

    /**
     * Hook delta A8/G3 (recon R18(ii)), until the door codec of hook P4: FCItemDoor.onItemUse (Common
     * FCItemDoor.java:33-53) calls Modern-Common ItemDoor.placeDoorBlock, an empty stub (ItemDoor.java:13), then uses
     * up the door and returns true, so a top-face click consumed an oak door and placed nothing. FCItemDoor is abstract
     * and FCItemDoorWood (item 324, BTWLifecycle.java:1486) its only subclass; the iron door (330) is a plain Item and
     * already reaches 1.20.1.
     */
    static boolean isPortGapDoorItem(btw.modern.ItemStack fcHeld) {
        btw.modern.Item item = fcHeld != null ? fcHeld.getItem() : null;
        Class<?> door = item != null ? fcItemDoorClass() : null;
        return door != null && door.isInstance(item);
    }

    /** FC onItemUse through tryPlaceItemIntoWorld, with the 1.5.2 creative restore (see {@link #tryPlaceHeldItem}). */
    private static boolean runFcItemUse(btw.modern.ItemStack fcHeld, Player player, PlayerBridge fcPlayer,
                                        btw.modern.World world, BlockPos pos, int side,
                                        float hitX, float hitY, float hitZ) {
        // Runs inside the use-item packet handler with no catch above it: an FC onItemUse failure is logged
        // once and treated as "nothing placed" instead of taking the server down.
        if (!player.isCreative()) {
            try {
                return fcHeld.tryPlaceItemIntoWorld(fcPlayer, world, pos.getX(), pos.getY(), pos.getZ(),
                        side, hitX, hitY, hitZ);
            } catch (Throwable t) {
                ProxyItem.logFcFailureOnce("onItemUse", fcHeld.itemID, t);
                return false;
            }
        }
        int damage = fcHeld.getItemDamage();
        int size = fcHeld.stackSize;
        // INV-RESYNC: a shadow scope keeps fcHeld the slot's own object through any nested sync until the restore
        // below lands on it. Without it two nested syncs after FC's decrement (FCItemBlockAestheticVegetation's
        // AngerPigmen, then FCEntityPigZombie.AngerNearbyPigmen) wrote the used-up stack and then dropped the object,
        // and a creative player's last item was lost.
        InventoryBridge shadow = fcPlayer.inventory instanceof InventoryBridge ib ? ib : null;
        if (shadow != null) shadow.beginShadow();
        try {
            return fcHeld.tryPlaceItemIntoWorld(fcPlayer, world, pos.getX(), pos.getY(), pos.getZ(),
                    side, hitX, hitY, hitZ);
        } catch (Throwable t) {
            ProxyItem.logFcFailureOnce("onItemUse", fcHeld.itemID, t);
            return false;
        } finally {
            fcHeld.setItemDamage(damage);
            fcHeld.stackSize = size;
            if (shadow != null) shadow.endShadow();
        }
    }

    // --- attack (onBlockClicked) ---

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            PlayerBridge fcPlayer = PlayerBridge.getOrCreate(player);
            fcPlayer.syncFromReal();
            fc().onBlockClicked(world, pos.getX(), pos.getY(), pos.getZ(), fcPlayer);
        }
    }

    // --- entityInside (onEntityCollidedWithBlock) ---

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // FC web (1037, FCBlockWeb extends BlockWeb): no collision box (vanilla/server BlockWeb.java:47-50,
        // so getCollisionShape is empty), and onEntityCollidedWithBlock -> setInWeb (BlockWeb.java:31) makes
        // Entity.moveEntity scale that tick's move by 0.25/0.05/0.25 and zero the motion (Entity.java:592-600).
        // The bridges don't carry isInWeb back to MC, so apply the identical 1.20.1 mechanism
        // (WebBlock.entityInside -> makeStuckInBlock) on BOTH sides: the client predicts its own movement.
        if (fc() instanceof btw.modern.BlockWeb) {
            entity.makeStuckInBlock(state, new Vec3(0.25D, (double) 0.05F, 0.25D));
        }
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            btw.modern.Entity fcEntity = wrapEntity(entity);
            if (fcEntity == null) return; // item consumed earlier in this checkInsideBlocks pass (see wrapEntity)
            fc().onEntityCollidedWithBlock(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
            applyFcContactFire(sl, world, pos, entity, fcEntity);
        }
    }

    private volatile boolean loggedFireProbeFailure;

    /**
     * Per-entity 1.5.2 contact counter for {@link #applyFcContactFire}: {last game tick burned, ticks of contact}.
     * Server thread only. Weak keys: an entry dies with its entity.
     */
    private static final Map<Entity, long[]> FIRE_CONTACT = new java.util.WeakHashMap<>();

    /**
     * 1.5.2 contact fire: Entity.moveEntity runs doBlockCollisions, then World.isBoundingBoxBurning asks every
     * overlapped block GetDoesFireDamageToEntities (FCMOD, vanilla/server World.java:2112-2142), then
     * dealFireDamage(1) and, unless wet, ++fire, igniting (setFire(8)) when the counter, which starts at
     * -fireResistance (Entity 1, EntityPlayer 20), reaches 0 (FCMOD Entity.java:2842-2862). FC fire ProxyBlocks
     * (stoked fire, ladder on fire, lit campfires) have no MC fire behaviour, so MC entities, players included,
     * took no damage, and stoked fire and campfires are now walk-through (null collision, getCollisionShape).
     * Same per-cell hook as 1.20.1 BaseFireBlock.entityInside, but the contact counter is kept here: these
     * blocks are not in BlockTags.FIRE, so Entity.move resets an unlit entity's remainingFireTicks to
     * -getFireImmuneTicks() right after entityInside, and a +1 on it would never ignite a player. FC puppets
     * are skipped: their FC entity's own moveEntity already burns them through WorldBridge, and the proxies
     * drop MC fire damage (FCPuppetSupport.isFcSimulatedFireDamage).
     */
    private void applyFcContactFire(ServerLevel level, btw.modern.World world, BlockPos pos,
                                    Entity entity, btw.modern.Entity fcEntity) {
        if (fcEntity == null || isFcPuppet(entity)) return;
        try {
            if (!fc().GetDoesFireDamageToEntities(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity)) return;
            long now = level.getGameTime();
            long[] contact = FIRE_CONTACT.get(entity);
            if (contact == null) {
                contact = new long[] {Long.MIN_VALUE / 2, 0L};
                FIRE_CONTACT.put(entity, contact);
            }
            if (contact[0] == now) return; // once per tick (one isBoundingBoxBurning per moveEntity), not per cell
            // Leaving the fire resets the counter (1.5.2: fire = -fireResistance on a move outside it). The server
            // sees a still player's move only every 20 ticks (LocalPlayer position reminder), so a gap up to that
            // is still contact.
            if (now - contact[0] > 20L) contact[1] = 0L;
            contact[0] = now;
            entity.hurt(level.damageSources().inFire(), 1.0F); // dealFireDamage(1)
            // !isWet(); a damage-immune (creative) player is extinguished every tick (EntityPlayer.java:287-289)
            if (!entity.fireImmune() && !entity.isInWaterRainOrBubble()
                    && !(entity instanceof Player p && p.getAbilities().invulnerable)) {
                int remaining = entity.getRemainingFireTicks();
                if (remaining > 0) {
                    entity.setRemainingFireTicks(remaining + 1); // burning: ++fire
                    contact[1] = 0L;
                } else if (++contact[1] >= (entity instanceof Player ? 20L : 1L)) {
                    contact[1] = 0L;
                    entity.setSecondsOnFire(8); // fire reached 0: setFire(8)
                }
            }
        } catch (Throwable t) {
            if (!loggedFireProbeFailure) {
                loggedFireProbeFailure = true;
                LOGGER.warn("[FIRE] contact fire for {} (id {}) at {} failed: {}",
                        fc().getClass().getSimpleName(), legacyId, pos, t.toString());
            }
        }
    }

    /** FC puppets (MC proxies owning an FC entity whose own physics runs FC's block contact effects). */
    private static boolean isFcPuppet(Entity entity) {
        return entity instanceof ProxyMob || entity instanceof ProxyAnimal
                || entity instanceof ProxyPathfinderMob || entity instanceof ProxyEntity;
    }

    // --- stepOn (onEntityWalking) ---

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // MC calls stepOn for every grounded entity; 1.5.2 Entity.moveEntity gates onEntityWalking on
        // canTriggerWalking() (vanilla/server Entity.java:841), which EntityItem answers false (EntityItem.java:59).
        // Checked before wrapping so a resting item is not re-synced every move.
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) return;
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            btw.modern.Entity fcEntity = wrapEntity(entity);
            if (fcEntity != null) {
                fc().onEntityWalking(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
            }
        }
    }

    // --- fallOn (onFallenUpon) ---

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity,
                       float fallDistance) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            btw.modern.Entity fcEntity = wrapEntity(entity);
            if (fcEntity != null) {
                fc().onFallenUpon(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity, fallDistance);
            }
        }
        // Always call super to apply vanilla fall damage (entity.causeFallDamage).
        // FC's onFallenUpon is a notification hook, not a replacement for damage.
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    // --- getDestroyProgress (FC getPlayerRelativeBlockHardness) ---
    // 1.5.2 digs with Block.getPlayerRelativeBlockHardness on both sides (PlayerControllerMP.java:242,316;
    // ItemInWorldManager.java:106,139,181,219); FC's body (Block.java:2340-2361) folds in the held tool.
    // Vanilla's BlockBehaviour.getDestroyProgress applies the tool INSIDE itself (player.getDigSpeed /
    // hardness / 30|100) - nothing is applied on top - so the old 1/(hardness*30) ignored every tool.

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float result = FCMiningSpeed.relativeHardness(fc(), player, level, pos);
        if (!Float.isNaN(result)) {
            return result;
        }
        // FC threw (logged by FCMiningSpeed): previous tool-blind behaviour as a safe fallback.
        float hardness = fc().blockHardness;
        return hardness < 0.0F ? 0.0F : 1.0F / (hardness * 30.0F);
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

    // NOTE: getSpeedFactor() is deliberately NOT overridden to apply FC's GetMovementModifier.
    // MC 1.20.1 multiplies getSpeedFactor into carried deltaMovement every tick (Entity.move),
    // so the +20% hard-surface bonus compounded into airborne momentum → the sprint-jump
    // "flying" bug. FC applied that bonus to the INPUT speed instead, which the bridge now
    // reproduces in PlayerMixin.btw$applyBlockMovementModifier (Player.getSpeed, via
    // FCMovementBonus). The inherited getSpeedFactor (the block's own speed factor, e.g. soul
    // sand's slowdown) is left intact.

    /**
     * Whether the FC class overrides getCollisionBoundingBoxFromPool (0 = not yet known, 1 = yes, 2 = no).
     * Only an override can return null - the base btw.modern.Block always returns its bounds - so other
     * blocks skip the probe below; getCollisionShape is hot (every entity move scans the blocks it sweeps).
     */
    private volatile byte fcCollisionOverride;
    private volatile boolean loggedCollisionProbeFailure;

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        // 1.5.2: a null getCollisionBoundingBoxFromPool means NO collision - Block.addCollisionBoxesToList
        // only adds a non-null box (vanilla/server Block.java:658-666), so rails, plants, torches,
        // campfires, candles, webs, ground cover... are walk-through, and projectiles ignore them
        // (World.rayTraceBlocks_do_do, World.java:1062). Selection (getShape) keeps the FC bounds.
        // If the FC probe throws, keep the bounds shape: a failure must never make a possibly solid
        // block passable.
        if (fcCollisionMayBeNull()) {
            try {
                if (fc().getCollisionBoundingBoxFromPool(createFcWorld(level, true),
                        pos.getX(), pos.getY(), pos.getZ()) == null) {
                    return net.minecraft.world.phys.shapes.Shapes.empty();
                }
            } catch (Throwable t) {
                if (!loggedCollisionProbeFailure) {
                    loggedCollisionProbeFailure = true;
                    LOGGER.warn("[COLLISION] FC getCollisionBoundingBoxFromPool threw for {} (id {}) at {}; "
                            + "keeping the bounds shape: {}", fc().getClass().getSimpleName(), legacyId, pos, t.toString());
                }
            }
        }
        return getFcShape(level, pos);
    }

    private boolean fcCollisionMayBeNull() {
        byte v = fcCollisionOverride;
        if (v == 0) {
            boolean overridden;
            try {
                overridden = fc().getClass().getMethod("getCollisionBoundingBoxFromPool",
                        btw.modern.World.class, int.class, int.class, int.class)
                        .getDeclaringClass() != btw.modern.Block.class;
            } catch (Throwable t) {
                overridden = true; // unknown: probe (the probe itself is guarded)
            }
            v = overridden ? (byte) 1 : (byte) 2;
            fcCollisionOverride = v;
        }
        return v == 1;
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return getFcShape(level, pos);
    }

    /**
     * Block support shape determines if torches, levers, etc. can attach to faces.
     * For full-cube FC blocks, return the full block shape so attachments work.
     */
    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (fc().isOpaqueCube()) {
            return net.minecraft.world.phys.shapes.Shapes.block();
        }
        return getFcShape(level, pos);
    }

    private net.minecraft.world.phys.shapes.VoxelShape getFcShape(BlockGetter level, BlockPos pos) {
        btw.modern.Block fcBlock = fc();
        btw.modern.AxisAlignedBB bounds = null;
        try {
            btw.modern.IBlockAccess access = new btw.modern.IBlockAccess() {
                @Override public int getBlockId(int x, int y, int z) {
                    BlockPos p = new BlockPos(x, y, z);
                    return ProxyRegistry.getWorldBlockId(level.getBlockState(p), level, p);
                }
                @Override public int getBlockMetadata(int x, int y, int z) {
                    BlockState s = level.getBlockState(new BlockPos(x, y, z));
                    return s.hasProperty(META) ? s.getValue(META) : 0;
                }
                @Override public btw.modern.TileEntity getBlockTileEntity(int x, int y, int z) { return null; }
                @Override public boolean isBlockOpaqueCube(int x, int y, int z) { return false; }
                @Override public boolean isBlockNormalCube(int x, int y, int z) { return false; }
                @Override public btw.modern.Material getBlockMaterial(int x, int y, int z) {
                    return fcBlock.blockMaterial;
                }
            };
            bounds = fcBlock.GetStateBoundsNoWrite(access, pos.getX(), pos.getY(), pos.getZ());
        } catch (Exception ignored) {
        }
        if (bounds == null) bounds = fcBlock.GetFixedBlockBoundsFromPool();

        boolean fullCube = bounds.minX <= 0.001 && bounds.minY <= 0.001 && bounds.minZ <= 0.001
                && bounds.maxX >= 0.999 && bounds.maxY >= 0.999 && bounds.maxZ >= 0.999;
        if (fullCube && !fcBlock.renderAsNormalBlock()) {
            try {
                btw.modern.AxisAlignedBB bb = fcBlock.getCollisionBoundingBoxFromPool(
                        createFcWorld(level, true), pos.getX(), pos.getY(), pos.getZ());
                if (bb != null) {
                    double ox = pos.getX(), oy = pos.getY(), oz = pos.getZ();
                    return net.minecraft.world.phys.shapes.Shapes.box(
                            clamp01(bb.minX - ox), clamp01(bb.minY - oy), clamp01(bb.minZ - oz),
                            clamp01(bb.maxX - ox), clamp01(bb.maxY - oy), clamp01(bb.maxZ - oz));
                }
            } catch (Throwable ignored) {
            }
        }

        return net.minecraft.world.phys.shapes.Shapes.box(
                bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /**
     * Builds a lightweight {@link btw.modern.World} over a modern {@link net.minecraft.world.level.BlockGetter},
     * enough for FC block queries that only read the world (randomDisplayTick particles;
     * getCollisionBoundingBoxFromPool bounds, which read metadata). Level-only calls are guarded
     * so it also works from collision queries where the accessor may not be a full Level.
     *
     * @param worldIds true for FC logic (collision bounds): neighbours read with the live-world ids FC sees
     *                 through WorldBridge ({@link ProxyRegistry#getWorldBlockId}); false for client display
     *                 (randomDisplayTick particles), which reads identity ids ({@link ProxyRegistry#getBlockId})
     *
     * <p>The client display view over a client level is FC's 1.5.2 WorldClient + RenderGlobal (S1): playSound
     * (both overloads; vanilla/client World.java:4994-4997 delegates the 6-arg one with no delay) plays locally like
     * WorldClient.playSound (vanilla/client WorldClient.java:483-505), playAuxSFX / playAuxSFXAtEntity play locally
     * like RenderGlobal.playAuxSFX (RenderGlobal.java:2169-2397, FC ids through FC's own client handler), both via
     * btw.forge.client.LegacyClientSound, func_82739_e like RenderGlobal.broadcastSound; markBlockRangeForRenderUpdate,
     * markBlockForRenderUpdate and markBlockForUpdate re-mesh like RenderGlobal.markBlockRangeForRenderUpdate /
     * markBlockForUpdate (MPR-6), as ClientFcWorld does. playSoundEffect / playSoundAtEntity stay no-ops
     * (RenderGlobal.java:1812/1817). Every other view (worldIds, or a server / generic accessor, e.g. collision
     * queries on the server thread) stays silent.</p>
     */
    private btw.modern.World createFcWorld(net.minecraft.world.level.BlockGetter level, boolean worldIds) {
        final boolean clientView = !worldIds && level instanceof Level lvl && lvl.isClientSide();
        return new btw.modern.World() {
            { this.isRemote = true; this.rand = new Random(); }
            public int getBlockId(int x, int y, int z) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState s = level.getBlockState(p);
                return worldIds ? ProxyRegistry.getWorldBlockId(s, level, p) : ProxyRegistry.getBlockId(s.getBlock());
            }
            public int getBlockMetadata(int x, int y, int z) {
                BlockState s = level.getBlockState(new BlockPos(x, y, z));
                return s.hasProperty(META) ? s.getValue(META) : 0;
            }
            public btw.modern.Material getBlockMaterial(int x, int y, int z) { return fc().blockMaterial; }
            public boolean isAirBlock(int x, int y, int z) { return level.getBlockState(new BlockPos(x, y, z)).isAir(); }
            public boolean isBlockNormalCube(int x, int y, int z) { return false; }
            public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
                try {
                    net.minecraft.core.particles.ParticleOptions particle = mapParticle(name);
                    if (particle != null && level instanceof net.minecraft.world.level.Level lvl) {
                        lvl.addParticle(particle, x, y, z, vx, vy, vz);
                    }
                } catch (Exception ignored) {}
            }
            public void playSoundEffect(double x, double y, double z, String sound, float vol, float pitch) {}
            public boolean canBlockSeeTheSky(int x, int y, int z) {
                return !(level instanceof net.minecraft.world.level.LevelReader lr)
                        || lr.canSeeSky(new BlockPos(x, y, z));
            }
            public boolean setBlock(int x, int y, int z, int id, int meta, int flags) { return false; }
            public btw.modern.TileEntity getBlockTileEntity(int x, int y, int z) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
                if (be instanceof ProxyBlockEntity pbe) return pbe.getFcTileEntity();
                return null;
            }
            public boolean canPlaceEntityOnSide(int id, int x, int y, int z, boolean b, int s, btw.modern.Entity e, btw.modern.ItemStack st) { return false; }
            public java.util.List getEntitiesWithinAABB(Class c, btw.modern.AxisAlignedBB bb) { return java.util.Collections.emptyList(); }
            public java.util.List getEntitiesWithinAABBExcludingEntity(btw.modern.Entity e, btw.modern.AxisAlignedBB bb) { return java.util.Collections.emptyList(); }
            public void scheduleBlockUpdate(int x, int y, int z, int id, int delay) {}
            public void notifyBlockChange(int x, int y, int z, int id) {}
            public boolean spawnEntityInWorld(btw.modern.Entity e) { return false; }
            public boolean canMineBlock(btw.modern.EntityPlayer p, int x, int y, int z) { return true; }
            public boolean doesBlockHaveSolidTopSurface(int x, int y, int z) { return true; }
            public int getSavedLightValue(btw.modern.EnumSkyBlock type, int x, int y, int z) { return 15; }
            public int getFullBlockLightValue(int x, int y, int z) { return 15; }
            public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) { return true; }
            public boolean checkNoEntityCollision(btw.modern.AxisAlignedBB bb) { return true; }
            public void playSoundAtEntity(btw.modern.Entity e, String s, float v, float p) {}
            public void playSound(double x, double y, double z, String sound, float vol, float pitch, boolean delay) {
                if (clientView) playClientLocal(x, y, z, sound, vol, pitch, delay);
            }
            public void playSound(double x, double y, double z, String sound, float vol, float pitch) {
                if (clientView) playClientLocal(x, y, z, sound, vol, pitch, false);
            }
            public void playAuxSFX(int id, int x, int y, int z, int data) {
                if (clientView) playClientAux(id, x, y, z, data);
            }
            public void playAuxSFXAtEntity(btw.modern.EntityPlayer p, int id, int x, int y, int z, int data) {
                if (clientView) playClientAux(id, x, y, z, data); // RenderGlobal ignores the player client-side
            }
            public void func_82739_e(int id, int x, int y, int z, int data) { // World -> RenderGlobal.broadcastSound
                if (clientView) playClientAuxGlobal(id, x, y, z, data);
            }
            public boolean isRaining() { return false; }
            public boolean isBlockGettingPowered(int x, int y, int z) { return false; }
            public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) { return false; }
            public btw.modern.IChunkProvider createChunkProvider() { return null; }
            public boolean destroyBlock(int x, int y, int z, boolean drop) { return false; }
            public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
                if (clientView) markClientRenderRange(level, x1, y1, z1, x2, y2, z2);
            }
            public void markBlockForRenderUpdate(int x, int y, int z) {
                if (clientView) markClientRenderRange(level, x, y, z, x, y, z);
            }
            public void markBlockForUpdate(int x, int y, int z) { // RenderGlobal.markBlockForUpdate re-meshes too
                if (clientView) markClientRenderRange(level, x, y, z, x, y, z);
            }
            public btw.modern.WorldChunkManager getWorldChunkManager() { return null; }
            public boolean setBlockToAir(int x, int y, int z) { return false; }
            public boolean setBlockMetadata(int x, int y, int z, int meta, int flags) { return false; }
            public btw.modern.BiomeGenBase getBiomeGenForCoords(int x, int z) { return btw.modern.BiomeGenBase.plains; }
        };
    }

    private static volatile boolean loggedClientSoundFailure;
    private static volatile boolean loggedClientRenderFailure;

    /** Client view playSound: LegacyClientSound.playLocal (CONTRACT C2), reached only on the client dist. */
    private static void playClientLocal(double x, double y, double z, String sound, float vol, float pitch,
                                        boolean delay) {
        try {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    btw.forge.client.LegacyClientSound.playLocal(x, y, z, sound, vol, pitch, delay));
        } catch (Throwable t) {
            logClientSoundFailure(t);
        }
    }

    /** Client view playAuxSFX: LegacyClientSound.playAux (CONTRACT C2), reached only on the client dist. */
    private static void playClientAux(int id, int x, int y, int z, int data) {
        try {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    btw.forge.client.LegacyClientSound.playAux(id, x, y, z, data));
        } catch (Throwable t) {
            logClientSoundFailure(t);
        }
    }

    /** Client view func_82739_e: LegacyClientSound.playAux(..., global = true), as ClientFcWorld.func_82739_e. */
    private static void playClientAuxGlobal(int id, int x, int y, int z, int data) {
        try {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    btw.forge.client.LegacyClientSound.playAux(id, x, y, z, data, true));
        } catch (Throwable t) {
            logClientSoundFailure(t);
        }
    }

    private static void logClientSoundFailure(Throwable t) {
        if (!loggedClientSoundFailure) {
            loggedClientSoundFailure = true;
            LOGGER.warn("[SOUND] client FC world sound failed (further failures suppressed): {}", t.toString());
        }
    }

    /**
     * 1.5.2 RenderGlobal.markBlockRangeForRenderUpdate(x1..z2) -> markBlocksForUpdate(x1 - 1 .. z2 + 1): the 1.20.1
     * LevelRenderer.setBlocksDirty marks the same one-block-padded range's sections, on the client thread. Only for
     * the level being rendered (FC's ±3 axle/gear box re-mesh, Client FCBlockGearBox.java:440).
     */
    private static void markClientRenderRange(net.minecraft.world.level.BlockGetter level,
                                              int x1, int y1, int z1, int x2, int y2, int z2) {
        try {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                Runnable mark = () -> {
                    if (mc.level != null && mc.level == level && mc.levelRenderer != null) {
                        mc.levelRenderer.setBlocksDirty(x1, y1, z1, x2, y2, z2);
                    }
                };
                if (mc.isSameThread()) mark.run(); else mc.execute(mark);
            });
        } catch (Throwable t) {
            if (!loggedClientRenderFailure) {
                loggedClientRenderFailure = true;
                LOGGER.warn("[RENDER] client FC world re-mesh request failed (further failures suppressed): {}",
                        t.toString());
            }
        }
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return btw.modern.Block.lightOpacity[legacyId];
    }

    @Override
    public net.minecraft.world.level.block.SoundType getSoundType(BlockState state) {
        btw.modern.StepSound ss = fc().stepSound;
        if (ss != null) {
            return soundTypeFor(ss, ss);
        }
        return super.getSoundType(state);
    }

    /**
     * S5: the block's sounds come from FC's own StepSound object (its getStepSound / getBreakSound / getPlaceSound
     * names, volume and pitch), not from a name-to-SoundType table, so FCStepSoundSquish blocks squish and the
     * glass/ladder/anvil step sounds keep their 1.5.2 names. The per-position FC override GetStepSound(world, x, y,
     * z) (dung, bone, dirt/sand slabs) is consulted the way 1.5.2 does: only footsteps use it
     * (vanilla/client Entity.java:1028); break (RenderGlobal.java:2309), hit (PlayerControllerMP.java:320), fall
     * (EntityLiving.java:1470) and place (FCItemPlacesAsBlock.PlayPlaceSound) use the block's stepSound, volume and
     * pitch included (a 1.20.1 SoundType has one volume/pitch for all its events, so a footstep SoundType must not
     * answer the others: FCStepSoundSquish's pitch 0.1 would drop a dung block's break/place/fall to the 0.5 floor).
     * See {@link #isFootstepQuery}. Vanilla-id FC replacements (clay, glowstone, vanilla web) are vanilla blocks and
     * never get here.
     */
    @Override
    public net.minecraft.world.level.block.SoundType getSoundType(BlockState state,
            net.minecraft.world.level.LevelReader level, BlockPos pos, @javax.annotation.Nullable Entity entity) {
        btw.modern.StepSound base = fc().stepSound;
        if (base == null) {
            return getSoundType(state); // IForgeBlock's default does the same
        }
        btw.modern.StepSound step = base;
        if (level != null && pos != null && fcStepSoundVaries() && isFootstepQuery(pos, entity)) {
            try {
                btw.modern.World world = level instanceof ServerLevel sl
                        ? WorldBridge.getOrCreate(sl) : createFcWorld(level, true);
                btw.modern.StepSound positional = fc().GetStepSound(world, pos.getX(), pos.getY(), pos.getZ());
                if (positional != null) step = positional;
            } catch (Throwable t) {
                if (!loggedStepSoundFailure) {
                    loggedStepSoundFailure = true;
                    LOGGER.warn("[SOUND] FC GetStepSound threw for {} (id {}) at {}; using its stepSound: {}",
                            fc().getClass().getSimpleName(), legacyId, pos, t.toString());
                }
            }
        }
        return soundTypeFor(step, base);
    }

    /**
     * Whether a 4-arg getSoundType query is a footstep: 1.20.1 Entity.playStepSound / playMuffledStepSound (and
     * AbstractHorse's) pass the walking entity and its getOnPosLegacy() block, on the ground and after
     * Entity.checkFallDamage has reset fallDistance. The other queries are not: break passes no entity
     * (LevelRenderer levelEvent 2001), the fall sound runs inside fallOn while fallDistance is still positive
     * (LivingEntity.playBlockFallSound), and a place targets a cell the placer is not standing on (BlockItem.place).
     * Residual: the client hit sound of a block dug from on top of it (MultiPlayerGameMode.continueDestroyBlock)
     * reads as a footstep; for squish dung that only changes its volume ((0.5 + 1) / 8 vs 1.5.2's (1 + 1) / 8), since
     * both pitches (0.05, 0.5) land on 1.20.1's 0.5 floor.
     */
    private static boolean isFootstepQuery(BlockPos pos, @javax.annotation.Nullable Entity entity) {
        try {
            return entity != null && entity.fallDistance <= 0.0F && entity.onGround()
                    && pos.equals(entity.getOnPosLegacy());
        } catch (Throwable t) {
            return false;
        }
    }

    /** Whether the FC class overrides GetStepSound (0 = not yet known, 1 = yes, 2 = no); only 4 FC classes do. */
    private volatile byte fcStepSoundOverride;
    private volatile boolean loggedStepSoundFailure;

    private boolean fcStepSoundVaries() {
        byte v = fcStepSoundOverride;
        if (v == 0) {
            boolean overridden;
            try {
                overridden = fc().getClass().getMethod("GetStepSound",
                        btw.modern.World.class, int.class, int.class, int.class)
                        .getDeclaringClass() != btw.modern.Block.class;
            } catch (Throwable t) {
                overridden = false; // unknown: the block's stepSound, as before
            }
            v = overridden ? (byte) 1 : (byte) 2;
            fcStepSoundOverride = v;
        }
        return v == 1;
    }

    /**
     * MISSED-3: 1.5.2 pick block (vanilla/client Minecraft.java:2348-2380) takes FC's idPicked (a stick for a
     * shaft, the seed for a crop, the idle oven for a lit one, idDropped for a kiln or a siding) and the damage of
     * Block.blocksList[picked < 256 && !flower pot ? picked : clicked].getDamageValue(world, x, y, z); id 0 picks
     * nothing. Previously the clicked ProxyBlock handed out its own (often hidden technical) item at meta 0.
     */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        try {
            btw.modern.World world = level instanceof ServerLevel sl
                    ? WorldBridge.getOrCreate(sl) : createFcWorld(level, true);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            int picked = fc().idPicked(world, x, y, z);
            if (picked <= 0) {
                return ItemStack.EMPTY;
            }
            btw.modern.Block damageBlock = fc();
            if (picked < 256 && !(fc() instanceof btw.modern.BlockFlowerPot)
                    && btw.modern.Block.blocksList[picked] != null) {
                damageBlock = btw.modern.Block.blocksList[picked];
            }
            int damage = damageBlock.getDamageValue(world, x, y, z);
            // EMPTY when the picked id has no 1.20.1 item (ItemStackHelper warns once): nothing is picked rather
            // than this block's own item.
            return ItemStackHelper.fcIdToMcStack(picked, 1, damage);
        } catch (Throwable t) {
            warnTickFailureOnce("PICK", pos, t);
            return super.getCloneItemStack(level, pos, state);
        }
    }

    // ================================================================
    // Step sound mapping (replaces the removed StepSound.translateToModern)
    // ================================================================

    /**
     * Name-based fallback per FC StepSound name, used for any event whose 1.5.2 sound name
     * {@link LegacySounds#resolve} does not know.
     */
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

    /** FC footstep StepSound + the block's own StepSound; identity (StepSound has no equals) — FC's are singletons. */
    private record SoundKey(btw.modern.StepSound step, btw.modern.StepSound base) {}

    private static final Map<SoundKey, net.minecraft.world.level.block.SoundType> SOUND_TYPES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The 1.20.1 SoundType for an FC StepSound pair (see the 4-arg getSoundType): step = the footstep StepSound's
     * getStepSound(); break / place from the block's getBreakSound() / getPlaceSound(); hit and fall = the block's
     * getStepSound() (PlayerControllerMP.java:320, EntityLiving.java:1470). Volume and pitch come from the footstep
     * StepSound, so a pair with step != base is only handed out for footstep queries (it differs in volume/pitch only
     * for the squish dung subtypes). Each name resolves through {@link LegacySounds#resolve} (CONTRACT C1); a name
     * 1.5.2 had no sound file for (LegacySounds.isSilentIn152, e.g. "step.anvil") stays silent; other unknown names
     * fall back to the name-mapped vanilla SoundType's event. Suppliers retry an unresolved name, so a call before
     * registration completes is not cached as a fallback.
     */
    private static net.minecraft.world.level.block.SoundType soundTypeFor(btw.modern.StepSound step,
                                                                         btw.modern.StepSound base) {
        return SOUND_TYPES.computeIfAbsent(new SoundKey(step, base), key -> {
            net.minecraft.world.level.block.SoundType stepFallback = translateStepSound(step);
            net.minecraft.world.level.block.SoundType baseFallback = translateStepSound(base);
            return new net.minecraftforge.common.util.ForgeSoundType(step.getVolume(), step.getPitch(),
                    new LegacySoundSupplier(base.getBreakSound(), baseFallback.getBreakSound()),
                    new LegacySoundSupplier(step.getStepSound(), stepFallback.getStepSound()),
                    new LegacySoundSupplier(base.getPlaceSound(), baseFallback.getPlaceSound()),
                    new LegacySoundSupplier(base.getStepSound(), baseFallback.getHitSound()),
                    new LegacySoundSupplier(base.getStepSound(), baseFallback.getFallSound()));
        });
    }

    private static volatile boolean loggedSoundResolveFailure;

    /** A 1.5.2 sound name resolved lazily through LegacySounds, memoized once found; the fallback until then. */
    private static final class LegacySoundSupplier
            implements java.util.function.Supplier<net.minecraft.sounds.SoundEvent> {
        private final String legacyName;
        private final net.minecraft.sounds.SoundEvent fallback;
        private volatile net.minecraft.sounds.SoundEvent resolved;

        LegacySoundSupplier(String legacyName, net.minecraft.sounds.SoundEvent fallback) {
            this.legacyName = legacyName;
            this.fallback = fallback;
        }

        @Override
        public net.minecraft.sounds.SoundEvent get() {
            net.minecraft.sounds.SoundEvent r = resolved;
            if (r != null) return r;
            if (legacyName != null) {
                try {
                    r = LegacySounds.resolve(legacyName);
                } catch (Throwable t) {
                    if (!loggedSoundResolveFailure) {
                        loggedSoundResolveFailure = true;
                        LOGGER.warn("[SOUND] LegacySounds.resolve('{}') threw; using the mapped vanilla sound: {}",
                                legacyName, t.toString());
                    }
                }
                if (r != null) {
                    resolved = r;
                    return r;
                }
                // 1.5.2 played nothing for it (StepSoundAnvil's "step.anvil": FC anvil footsteps, hits and falls);
                // intentionally_empty is 1.20.1's silent event (SoundEngine skips it). A name check, not registration
                // dependent, so it is memoized at once.
                if (LegacySounds.isSilentIn152(legacyName)) {
                    resolved = net.minecraft.sounds.SoundEvents.EMPTY;
                    return net.minecraft.sounds.SoundEvents.EMPTY;
                }
            }
            return fallback;
        }
    }

    /**
     * 1.5.2 places a block INTO this cell when it is FC air (IsAirBlock: detector logic), ground cover
     * (FCItemPlacesAsBlock.java:79-82 replaces a clicked IsGroundCover block, facing 1, which getStateForPlacement
     * applies through isGroundCoverAt) or of a replaceable material (:83-86 and World.canPlaceEntityOnSide,
     * vanilla/server World.java:3399-3401, which also names fcBlockFireStoked): FC ash and stoked fire. Answering
     * only IsAirBlock refused every BlockItem placement into ash and stoked fire.
     */
    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext ctx) {
        btw.modern.Block b = fc();
        try {
            return b.IsAirBlock() || b.IsGroundCover()
                    || (b.blockMaterial != null && b.blockMaterial.isReplaceable());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return btw.modern.Block.lightOpacity[legacyId] == 0;
    }

    // Live per-position block rendering is supplied by FCBakedModel.getModelData (the
    // IForgeBakedModel hook ChunkRenderDispatcher calls); IForgeBlock has no getModelData,
    // so a Block-side provider here would never be called.

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        // Always use MODEL — the JSON model system renders all ProxyBlocks.
        // ENTITYBLOCK_ANIMATED would make the block invisible since we have
        // no BlockEntityRenderer for ProxyBlocks.
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return !fc().isOpaqueCube();
    }

    // ================================================================
    // Entity wrapping helper
    // ================================================================

    /**
     * Wraps a vanilla MC entity as a btw.modern.Entity for FC callbacks.
     * Returns a {@link PlayerBridge} for Player entities (richer state and
     * correct EntityPlayer type), a {@link LivingEntityBridge} for non-player
     * living entities, an {@link ItemEntityBridge} for dropped items, or an
     * {@link EntityBridge} for other entities. Null for a dropped item without
     * an FC view; callers then skip the FC callback.
     */
    private static btw.modern.Entity wrapEntity(Entity entity) {
        if (entity instanceof Player player) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            return pb;
        }
        if (entity instanceof LivingEntity living) {
            LivingEntityBridge lb = LivingEntityBridge.getOrCreate(living);
            lb.syncFromReal();
            return lb;
        }
        // MC ItemEntity -> its FC EntityItem view (design_items U3b): 1.5.2 EntityItem passes itself to the block
        // (vanilla/server EntityItem.java:483-500), so `instanceof EntityItem` callbacks work (FCBlockHopper:162,
        // FCBlockCampfire:442, FCBlockFarmlandBase:153, FCBlockPlanterBase:61, FCBlockNetherGrowth:276) and
        // EntityItem.CanEntityTriggerTripwire() == false applies. In-place FC changes (bone meal stackSize--) are
        // landed by the ItemEntityMixin getItem() hook / end-of-tick flush; setDead discards immediately.
        // Never the generic EntityBridge: wrap() is null for an item an FC block in an earlier cell of the same
        // checkInsideBlocks pass consumed, an empty one, or one that failed to sync, and as an EntityBridge it
        // would answer CanEntityTriggerTripwire() == true and fail every `instanceof EntityItem` branch.
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity ie) {
            return ItemEntityBridge.wrap(ie);
        }
        EntityBridge eb = EntityBridge.getOrCreate(entity);
        eb.syncFromReal();
        return eb;
    }

    // ================================================================
    // EntityBlock implementation — FC tile entity bridge
    // ================================================================

    /**
     * Routes block events (WorldBridge.addBlockEvent -> Level.blockEvent) into
     * the FC tile entity, mirroring 1.5.2 BlockContainer.onBlockEventReceived ->
     * TileEntity.receiveClientEvent. Chest lid viewer-count sync (event 1) and
     * any other FC TE events flow through here on both sides.
     */
    @Override
    public boolean triggerEvent(net.minecraft.world.level.block.state.BlockState state,
                                 net.minecraft.world.level.Level level,
                                 BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        if (level.getBlockEntity(pos) instanceof ProxyBlockEntity pbe
                && pbe.getFcTileEntity() != null) {
            return pbe.getFcTileEntity().receiveClientEvent(id, param);
        }
        return false;
    }

    /**
     * Returns whether this ProxyBlock's FC block creates a tile entity.
     */
    public boolean hasFcTileEntity() {
        try {
            return fc().createNewTileEntity(null) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Creates a new {@link ProxyBlockEntity} wrapping the FC tile entity
     * produced by the FC block's {@code createNewTileEntity()} method.
     *
     * <p>Returns null if the FC block does not use tile entities, which
     * tells MC not to store a BlockEntity at this position.</p>
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (!hasFcTileEntity()) return null;
        if (ProxyBlockEntity.TYPE == null) return null; // not yet registered
        try {
            btw.modern.TileEntity fcTe = fc().createNewTileEntity(null);
            if (fcTe != null) {
                return new ProxyBlockEntity(pos, state, fcTe, legacyId);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to create FC tile entity for block {}: {}",
                    legacyId, e.getMessage());
        }
        return null;
    }

    /**
     * Returns a ticker for ProxyBlockEntities that calls the FC tile
     * entity's {@code updateEntity()} method every server tick.
     *
     * <p>Only returns a ticker on the server side (when the level is a
     * {@link ServerLevel}); client-side ticking is not needed for FC
     * tile entities which are server-only.</p>
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        if (!hasFcTileEntity()) return null;
        // Tick on BOTH server and client — FC tile entities check worldObj.isRemote
        // internally (server: cooking/crafting, client: particle spawning)
        if (type == ProxyBlockEntity.TYPE) {
            @SuppressWarnings("unchecked")
            BlockEntityTicker<T> ticker = (BlockEntityTicker<T>) (BlockEntityTicker<ProxyBlockEntity>) ProxyBlockEntity::tick;
            return ticker;
        }
        return null;
    }

    // ================================================================
    // onRemove — must remove BlockEntity AFTER FC breakBlock runs
    // ================================================================

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level instanceof ServerLevel sl) {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                try {
                    fc().breakBlock(world, pos.getX(), pos.getY(), pos.getZ(),
                            legacyId, state.getValue(META));
                } catch (NullPointerException e) {
                    // FC block may expect a tile entity that doesn't exist yet
                    // (tile entity bridge not fully implemented for all FC blocks)
                }
            }
        }
        // super.onRemove handles BlockEntity removal — must happen AFTER
        // breakBlock so FC code can still access the tile entity during cleanup.
        super.onRemove(state, level, pos, newState, moving);
    }

}
