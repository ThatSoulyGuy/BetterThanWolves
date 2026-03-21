package btw.forge;

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
    private final boolean hasTileEntity;

    public ProxyBlock(int legacyId, btw.modern.Block fcBlock) {
        super(initAndBuildProperties(fcBlock));
        this.legacyId = legacyId;
        this.fcBlock = fcBlock;
        CONSTRUCTING.remove();
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));

        // Probe the FC block to see if it creates a tile entity
        boolean needsTe = false;
        try {
            btw.modern.TileEntity te = fcBlock.createNewTileEntity(null);
            needsTe = (te != null);
        } catch (Exception e) {
            // Some FC blocks may NPE when passed null world; treat as no tile entity
        }
        this.hasTileEntity = needsTe;
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
        try {
            fc().updateTick(world, pos.getX(), pos.getY(), pos.getZ(), new Random(random.nextLong()));
        } catch (NullPointerException e) {
            // FC block may expect tile entity or world state not yet bridged
        }
    }

    // --- randomTick ---

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        btw.modern.World world = WorldBridge.getOrCreate(level);
        try {
            fc().RandomUpdateTick(world, pos.getX(), pos.getY(), pos.getZ(), new Random(random.nextLong()));
        } catch (NullPointerException e) {
            // FC block may expect tile entity or world state not yet bridged
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
            // Create a lightweight client-side World wrapper for particle spawning
            btw.modern.World clientWorld = new btw.modern.World() {
                { this.isRemote = true; this.rand = new Random(); }
                public int getBlockId(int x, int y, int z) {
                    return ProxyRegistry.getBlockId(level.getBlockState(new BlockPos(x, y, z)).getBlock());
                }
                public int getBlockMetadata(int x, int y, int z) {
                    BlockState s = level.getBlockState(new BlockPos(x, y, z));
                    return s.hasProperty(META) ? s.getValue(META) : 0;
                }
                public btw.modern.Material getBlockMaterial(int x, int y, int z) {
                    return fc().blockMaterial;
                }
                public boolean isAirBlock(int x, int y, int z) {
                    return level.getBlockState(new BlockPos(x, y, z)).isAir();
                }
                public boolean isBlockNormalCube(int x, int y, int z) { return false; }
                public void spawnParticle(String name, double x, double y, double z,
                                          double vx, double vy, double vz) {
                    try {
                        // Map FC particle names to MC 1.20.1 ParticleTypes
                        net.minecraft.core.particles.ParticleOptions particle = mapParticle(name);
                        if (particle != null) {
                            level.addParticle(particle, x, y, z, vx, vy, vz);
                        }
                    } catch (Exception ignored) {}
                }
                public void playSoundEffect(double x, double y, double z,
                                             String sound, float vol, float pitch) {}
                public boolean canBlockSeeTheSky(int x, int y, int z) {
                    return level.canSeeSky(new BlockPos(x, y, z));
                }
                public boolean setBlock(int x, int y, int z, int id, int meta, int flags) { return false; }
                public btw.modern.TileEntity getBlockTileEntity(int x, int y, int z) { return null; }
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
                public void playAuxSFX(int id, int x, int y, int z, int data) {}
                public void playAuxSFXAtEntity(btw.modern.EntityPlayer p, int id, int x, int y, int z, int data) {}
                public boolean isRaining() { return false; }
                public boolean isBlockGettingPowered(int x, int y, int z) { return false; }
                public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) { return false; }
                public btw.modern.IChunkProvider createChunkProvider() { return null; }
                public boolean destroyBlock(int x, int y, int z, boolean drop) { return false; }
                public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}
                public btw.modern.WorldChunkManager getWorldChunkManager() { return null; }
                public boolean setBlockToAir(int x, int y, int z) { return false; }
                public boolean setBlockMetadata(int x, int y, int z, int meta, int flags) { return false; }
                public btw.modern.BiomeGenBase getBiomeGenForCoords(int x, int z) { return btw.modern.BiomeGenBase.plains; }
            };
            fc().randomDisplayTick(clientWorld, pos.getX(), pos.getY(), pos.getZ(),
                    new Random(random.nextLong()));
        } catch (Exception ignored) {
        }
    }

    /** Maps FC particle names to MC 1.20.1 ParticleOptions. */
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
        if (name.equals("angryVillager")) return net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER;
        if (name.startsWith("iconcrack_")) return net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL;
        if (name.startsWith("blockcrack_")) return net.minecraft.core.particles.ParticleTypes.POOF;
        // FC custom particles
        if (name.equals("fcwhitesmoke")) return net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE;
        if (name.equals("fccinders")) return net.minecraft.core.particles.ParticleTypes.FLAME;
        return null;
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

    // --- onRemove --- (moved to EntityBlock section at bottom of class)

    // --- use (onBlockActivated) ---

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
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
                // Stage 2: FC item use on block
                if (fcHeld != null) {
                    result = fcHeld.tryPlaceItemIntoWorld(
                            fcPlayer, world, pos.getX(), pos.getY(), pos.getZ(),
                            side, hitX, hitY, hitZ);
                }
            }

            // Check if FC opened a container GUI and open the MC menu
            boolean containerOpened = ContainerBridge.checkAndOpenContainer(fcPlayer, prevContainer);

            // Sync FC item state (damage, stackSize) back to MC inventory
            if (fcPlayer.inventory instanceof InventoryBridge invBridge) {
                invBridge.writeBackCurrentItem(fcHeld);
            }

            // Always return SUCCESS if a container was opened or FC handled the activation
            // This prevents MC from calling use() again for the off hand
            return (result || containerOpened) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
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
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            btw.modern.Entity fcEntity = wrapEntity(entity);
            fc().onEntityCollidedWithBlock(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
        }
    }

    // --- stepOn (onEntityWalking) ---

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            btw.modern.Entity fcEntity = wrapEntity(entity);
            fc().onEntityWalking(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
        }
    }

    // --- fallOn (onFallenUpon) ---

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity,
                       float fallDistance) {
        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            btw.modern.Entity fcEntity = wrapEntity(entity);
            fc().onFallenUpon(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity, fallDistance);
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
        // getSpeedFactor() has no world/position context, but FC's GetMovementModifier
        // may query block metadata which requires a world. Catch NPE for blocks that
        // need metadata (like dirt slabs) — the mixin handles position-aware speed.
        try {
            float modifier = fc().GetMovementModifier(null, 0, 0, 0);
            return modifier > 0 ? modifier : 1.0F;
        } catch (NullPointerException e) {
            return 1.0F;
        }
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return getFcShape(level, pos);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return getFcShape(level, pos);
    }

    /**
     * Gets the VoxelShape from FC's block bounds. Calls setBlockBoundsBasedOnState
     * with a real IBlockAccess so FC blocks with state-dependent bounds (campfire,
     * axle, etc.) return the correct shape.
     */
    private net.minecraft.world.phys.shapes.VoxelShape getFcShape(BlockGetter level, BlockPos pos) {
        btw.modern.Block fcBlock = fc();
        try {
            // Create a lightweight IBlockAccess from the BlockGetter
            btw.modern.IBlockAccess access = new btw.modern.IBlockAccess() {
                @Override public int getBlockId(int x, int y, int z) {
                    return ProxyRegistry.getBlockId(level.getBlockState(new BlockPos(x, y, z)).getBlock());
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
            fcBlock.setBlockBoundsBasedOnState(access, pos.getX(), pos.getY(), pos.getZ());
        } catch (Exception ignored) {
            // If setBlockBoundsBasedOnState fails, use default bounds
        }
        return net.minecraft.world.phys.shapes.Shapes.box(
                fcBlock.minX, fcBlock.minY, fcBlock.minZ,
                fcBlock.maxX, fcBlock.maxY, fcBlock.maxZ);
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

    /**
     * Provides real world data to FCBakedModel so FC's RenderBlock runs with
     * real neighbor access during chunk rebuilds — matching MC 1.5.2 behavior.
     */
    public net.minecraftforge.client.model.data.ModelData getModelData(
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
            BlockState state, net.minecraftforge.client.model.data.ModelData modelData) {
        return modelData.derive()
                .with(btw.forge.client.FCBakedModel.BLOCK_GETTER, level)
                .with(btw.forge.client.FCBakedModel.BLOCK_POS, pos)
                .build();
    }

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
     * living entities, or an {@link EntityBridge} for other entities.
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
        EntityBridge eb = EntityBridge.getOrCreate(entity);
        eb.syncFromReal();
        return eb;
    }

    // ================================================================
    // EntityBlock implementation — FC tile entity bridge
    // ================================================================

    /**
     * Returns whether this ProxyBlock's FC block creates a tile entity.
     */
    public boolean hasFcTileEntity() {
        return hasTileEntity;
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
        if (!hasTileEntity) return null;
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
        if (!hasTileEntity) return null;
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
