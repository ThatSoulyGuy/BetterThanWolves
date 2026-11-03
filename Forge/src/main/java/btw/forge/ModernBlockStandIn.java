package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What FC sees, through WorldBridge.getBlockId, for a colliding 1.20.1 block state with no usable 1.5.2 id
 * (tuff, granite/diorite/andesite, calcite, dripstone, polished deepslate, copper, mud, concrete, hay,
 * terracotta, ...). In 1.5.2 every block in the world had a blocksList entry and World.getCollidingBoundingBoxes
 * collided with all of them (vanilla/client World.java:1601-1625). Only the world-read path uses this
 * (ProxyRegistry.getWorldBlockId): identity routing (getFcBlock / BlockBehaviorMixin / items) never returns a
 * stand-in id, so no FC callback runs on a modern block. Collision/solidity/pathing/hardness are bridged to
 * the live MC state, and WorldBridge.setBlock(standInId) is a no-op (getModernBlock(standIn) == null), so FC can never
 * write one. FC's machine hooks (the saw, the stump remover, fertilizer) answer through {@link ModernBlockHooks}: inert
 * (false) unless an integration registered a hook for the live state (INTEGRATIONS #022).
 *
 * <p>FC's fire and plant rules read two more properties through the id. Fire: FCBlockFire, which owns the FIRE tick,
 * reads only its own tables (BlockFire.abilityToCatchFire / chanceToEncourageFire, indexed by getBlockId), and FC lava
 * reads Material.getCanBurn (FCBlockLavaStationary.java:64-67). A colliding state that 1.20.1 fire burns (bamboo planks
 * and blocks, hay, carpets, scaffolding, beehives, ...) therefore reads as a flammable stand-in carrying its 1.20.1
 * odds, so it keeps burning and feeding fire as it does today (the acacia, dark oak, mangrove and cherry planks are FC
 * planks 5:5-8, ModernWoodSubBlocks, and burn by FC's planks entries), and a 1.20.1 fire source
 * (magma block) keeps FC fire on it burning (DoesInfiniteBurnToFacing). Soil: modern soils answer
 * FC's soil hooks from their 1.20.1 tags (maintainer decision D3), so plants 1.20.1 world generation put on podzol,
 * moss, mud, rooted or coarse dirt and red sand survive FC's survival rules.</p>
 *
 * <p>Plants: a governed modded crop on BTW soil that takes it (btw.forge.farming.FcSoil.governsAt) reads as the plant
 * stand-in ({@link ModernPlantStandIn}, INTEGRATIONS #026), allocated with the plain stand-ins only when the pack has
 * a modded crop; every other non-colliding state still reads as air.</p>
 *
 * <p>Support relatives (FcAttachSupport.SUPPORT_RELATIVE, ruling 2026-10-31): a modern fence, wall or stained glass
 * pane FC reads through a stand-in answers FC's support questions as its BTW relative (the oak fence, the cobblestone
 * wall, the glass pane): the centre, small and large hard points, the ground-cover rest and its resting offset are
 * handed to the relative's FC block on a WorldBridge read (CanSupportFallingBlocks follows the centre hard point). Every
 * other method here (collision, pathing, fire odds, hardness, sounds, drops, the saw, mobility, the machine and soil
 * hooks) and the stand-in id itself stay as they are.</p>
 */
final class ModernBlockStandIn extends btw.modern.Block {

    private static final Logger LOGGER = LogManager.getLogger("BTW-StandIn");

    // Unused by 1.5.2 vanilla (Block.java ends at dropper, 158) and by FC (ParseID defaults start at 175,
    // FCBetterThanWolves.java:914). Kept < 175 so the blocksList[175..] loops (BTWRegistration:85,
    // BlockModelBridge:413/424) never give a stand-in a ProxyBlock or a model. 159 and 170-173 are 1.6+ ids the
    // vanilla table maps (isLegacyIdMapped), so 160-169 and 174 are free. The FC plant twins (TwinRegistry) and log2
    // (ModernWoods, 165) share the range: both are built at FC start-up, before any stand-in; the twins take the free
    // ids from the top (174, 169, 168, 167, 166). Stand-ins allocate from the bottom (160 solid, 161 collider, the
    // plant stand-in when the pack has a modded crop, then the flammable ones).
    static final int FIRST_FREE_ID = 159;
    static final int LAST_FREE_ID = 174;
    /** FCBlockBedrock: immovable, not BD-retrievable, spared by id in EntityWither (vanilla/client EntityWither.java:369-371). */
    private static final int BEDROCK_ID = 7;

    private static volatile boolean created;   // written last in ensureCreated -> publishes the ids below
    private static int solidId;                // full, solid-render collision cube (1.5.2 "normal cube")
    private static int colliderId;             // any other colliding shape (slab, stair, wall, fence, glass, carpet, mud)
    private static int plantId;                // governed modded crops on BTW soil (ModernPlantStandIn); 0 = none
    /** Guards the plant check against re-entry: FcSoil reads the soil through FC's world while idFor runs. */
    private static final ThreadLocal<Boolean> IN_PLANT_CHECK = new ThreadLocal<>();
    private static final Set<net.minecraft.world.level.block.Block> LOGGED = ConcurrentHashMap.newKeySet();
    private static volatile boolean idForFailureLogged;
    /** One FC StepSound per 1.20.1 SoundType (identity keys: SoundType has no equals; the instances are shared). */
    private static final Map<SoundType, btw.modern.StepSound> STEP_SOUNDS = new ConcurrentHashMap<>();
    private static volatile boolean stepSoundFailureLogged;

    /**
     * 1.5.2 Material.rock (vanilla/client Material.java) that can burn: the material of a flammable stand-in, so FC
     * lava's CanBlockMaterialBurn (FCBlockLavaStationary.java:64-67) ignites beside it as 1.20.1 LavaFluid does beside
     * any block with flammability (LavaFluid.java:114-116, IForgeBlock.isFlammable). Everything else matches rock.
     */
    private static final btw.modern.Material FLAMMABLE_ROCK =
            new btw.modern.Material(btw.modern.MapColor.stoneColor).setRequiresTool().setBurning();
    /**
     * Flammable stand-ins by (shape class, 1.20.1 ignite odds, burn odds). Copy-on-write under the class lock; the
     * volatile write publishes each new stand-in to readers on other threads (mesh threads read ids too).
     */
    private static volatile FlammableBucket[] flammableBuckets = new FlammableBucket[0];
    private static int flammableCreated;               // guarded by the class lock; names btwModernFlammable<n>
    private static volatile boolean flammableExhaustedLogged;
    private static volatile boolean fireOddsFailureLogged;

    /** One bucket: {@code id} is a dedicated stand-in, or (dedicated == false) the nearest one reused for it. */
    private record FlammableBucket(boolean solid, int ignite, int burn, int id, boolean dedicated) { }

    private final boolean m_bSolidCube;

    private ModernBlockStandIn(int id, boolean solidCube) {
        this(id, solidCube, btw.modern.Material.rock, solidCube ? "btwModernSolid" : "btwModernCollider");
    }

    private ModernBlockStandIn(int id, boolean solidCube, btw.modern.Material material, String name) {
        super(id, material);   // registers blocksList[id] (+ an ItemBlock, never exposed)
        this.m_bSolidCube = solidCube;
        // 1.5.2 opaque-cube table (vanilla/server Block.java:484): the Block constructor evaluated isOpaqueCube()
        // before m_bSolidCube was set, and stand-ins are created after BTWLifecycle filled the table. The solid
        // stand-in (tuff, calcite, ...) must stay opaque to FC's tree and huge mushroom generators.
        opaqueCubeLookup[id] = solidCube;
        // Stone defaults (vanilla/client Block.java:72). stepSound must be non-null: frozen
        // Entity.playStepSound dereferences GetStepSound(...) (Entity.java:1028).
        setHardness(1.5F);
        setResistance(10.0F);
        setStepSound(soundStoneFootstep);
        setUnlocalizedName(name);
    }

    /** Legacy id FC should see for {@code state}; 0 if it has no collision (plants, light...: unchanged). */
    static int idFor(BlockState state, BlockGetter level, BlockPos pos) {
        try {
            if (!created) ensureCreated();
            if (solidId <= 0) return 0;                                  // allocation failed -> legacy (air)
            // FCMOD-INTEGRATION(1.20.1): modded_plant_rules: a governed modded crop on BTW soil that takes it reads as the plant stand-in, not as air, so FC's weeds, animals and fire treat it as an FC crop
            if (plantId > 0 && isGovernedCropOnBtwSoil(state, level, pos)) {
                net.minecraft.world.level.block.Block mc = state.getBlock();
                if (!LOGGED.contains(mc) && LOGGED.add(mc)) {
                    LOGGER.info("[STAND-IN] {} on BTW soil reads as legacy id {} (plant) to FC",
                            BuiltInRegistries.BLOCK.getKey(mc), plantId);
                }
                return plantId;
            }
            VoxelShape shape = state.getCollisionShape(level, pos);      // MC-cached for non-dynamic shapes
            // FCMOD-INTEGRATION(1.20.1): fc_machine_hooks: MC builds that cache at BlockPos.ZERO in an empty world (BlockBehaviour.BlockStateBase.Cache); a block whose shape follows its neighbours without dynamicShape (Dynamic Trees' TrunkShellBlock follows its core) caches empty there, though 1.20.1 entities collide with it at its live position, so ask again at the live position
            if (shape.isEmpty() && !state.getBlock().hasDynamicShape()) {
                shape = state.getCollisionShape(level, pos, CollisionContext.empty());
            }
            if (shape.isEmpty()) return 0;
            boolean full = net.minecraft.world.level.block.Block.isShapeFullBlock(shape);
            int id = full && state.is(BlockTags.WITHER_IMMUNE) ? BEDROCK_ID
                    : full && state.isSolidRender(level, pos) ? solidId : colliderId;
            if (id != BEDROCK_ID) {
                id = withFireOdds(state, level, pos, id == solidId, id);
            }
            net.minecraft.world.level.block.Block mc = state.getBlock();
            if (!LOGGED.contains(mc) && LOGGED.add(mc)) {
                LOGGER.info("[STAND-IN] {} reads as legacy id {} to FC", BuiltInRegistries.BLOCK.getKey(mc), id);
            }
            return id;
        } catch (Throwable t) {
            // World reads run inside FC entity/block ticks: never throw out of them.
            if (!idForFailureLogged) {
                idForFailureLogged = true;
                LOGGER.warn("[STAND-IN] stand-in lookup failed for {}; it reads as air to FC (logged once)", state, t);
            }
            return 0;
        }
    }

    /**
     * True for a governed modded crop standing on BTW soil that takes it, read on the server thread of a ServerLevel
     * (the FC world reads that see a live plant; world generation and other threads keep air).
     */
    private static boolean isGovernedCropOnBtwSoil(BlockState state, BlockGetter level, BlockPos pos) {
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock)
                || !(level instanceof ServerLevel sl) || !sl.getServer().isSameThread()
                || IN_PLANT_CHECK.get() != null) {
            return false;
        }
        IN_PLANT_CHECK.set(Boolean.TRUE);
        try {
            return btw.forge.farming.FcSoil.governsAt(sl, pos, state);
        } finally {
            IN_PLANT_CHECK.remove();
        }
    }

    /** The plant stand-in's id (allocating the stand-ins first if needed), 0 when the pack has no modded crop. */
    static int plantStandInId() {
        if (!created) ensureCreated();
        return plantId;
    }

    /** The plant stand-in's id if the stand-ins exist, else 0 (no allocation). */
    static int plantId() {
        return created ? plantId : 0;
    }

    private static synchronized void ensureCreated() {
        if (created) return;
        try {
            int a = nextFreeId(FIRST_FREE_ID);
            int b = a > 0 ? nextFreeId(a + 1) : -1;
            if (a > 0 && b > 0) {
                new ModernBlockStandIn(a, true);
                new ModernBlockStandIn(b, false);
                solidId = a;
                colliderId = b;
                LOGGER.info("[STAND-IN] modern-only blocks read by FC as legacy {} (solid cube) / {} (shaped collider)", a, b);
                createPlantStandIn(b);
            } else {
                LOGGER.warn("[STAND-IN] no two free legacy ids in {}..{}; modern-only blocks stay air to FC",
                        FIRST_FREE_ID, LAST_FREE_ID);
            }
        } catch (Throwable t) {
            solidId = 0;
            LOGGER.error("[STAND-IN] creation failed; modern-only blocks stay air to FC", t);
        } finally {
            created = true;
        }
    }

    /**
     * The plant stand-in, right after the collider, only when the pack has a modded crop (ModernPlantStandIn.isNeeded):
     * plain BTW keeps three free ids (162-164) for flammable stand-ins, a pack with modded crops two. A failure leaves
     * modded crops reading as air, as before.
     */
    // FCMOD-INTEGRATION(1.20.1): modded_plant_rules: one free legacy id for FC's view of modded crops on BTW soil, only when a modded crop exists
    private static void createPlantStandIn(int collider) {
        try {
            if (!ModernPlantStandIn.isNeeded()) return;
            int p = nextFreeId(collider + 1);
            if (p <= 0) {
                LOGGER.warn("[STAND-IN] no free legacy id in {}..{} for the plant stand-in; modded crops stay air "
                        + "to FC", FIRST_FREE_ID, LAST_FREE_ID);
                return;
            }
            new ModernPlantStandIn(p);
            plantId = p;
            LOGGER.info("[STAND-IN] modded crops on BTW soil read by FC as legacy {} (plant)", p);
        } catch (Throwable t) {
            plantId = 0;
            LOGGER.error("[STAND-IN] plant stand-in creation failed; modded crops stay air to FC", t);
        }
    }

    /** Free = empty block AND item slot, and no 1.20.1 block maps to it (else getFcBlock would route MC callbacks here). */
    private static int nextFreeId(int from) {
        for (int id = from; id <= LAST_FREE_ID; id++) {
            if (isFreeId(id)) {
                return id;
            }
        }
        return -1;
    }

    static boolean isFreeId(int id) {
        return btw.modern.Block.blocksList[id] == null && btw.modern.Item.itemsList[id] == null
                && !ProxyRegistry.isLegacyIdMapped(id);
    }

    /** True for a legacy id FC reads as a stand-in (FcAttachSupport.supportRelative's test, the same on both sides). */
    static boolean isStandIn(int id) {
        return id > 0 && id < btw.modern.Block.blocksList.length
                && btw.modern.Block.blocksList[id] instanceof ModernBlockStandIn;
    }

    // ---- flammable stand-ins

    /**
     * {@code plainId}, or the flammable stand-in of the same shape class for a state 1.20.1 fire burns. The odds are
     * the ones 1.20.1 FireBlock consults (Forge getFireSpreadSpeed / getFlammability, FireBlock.java:219, 257; their
     * IForgeBlock defaults read FireBlock's table, IForgeBlock.java:614-617, 655-658), so modded blocks with their own
     * values count too. FireBlockMixin does not touch them: the block is unmapped (getBlockId == 0).
     */
    private static int withFireOdds(BlockState state, BlockGetter level, BlockPos pos, boolean solid, int plainId) {
        int ignite;
        int burn;
        try {
            ignite = Math.max(0, state.getFireSpreadSpeed(level, pos, Direction.UP));
            burn = Math.max(0, state.getFlammability(level, pos, Direction.UP));
        } catch (Throwable t) {
            if (!fireOddsFailureLogged) {
                fireOddsFailureLogged = true;
                LOGGER.warn("[STAND-IN] fire odds lookup failed for {}; it reads as non-flammable to FC (logged once)",
                        state, t);
            }
            return plainId;
        }
        if (ignite == 0 && burn == 0) return plainId;
        for (FlammableBucket b : flammableBuckets) {
            if (b.solid() == solid && b.ignite() == ignite && b.burn() == burn) return b.id();
        }
        return flammableIdFor(solid, ignite, burn, plainId);
    }

    /**
     * Creates the stand-in for a new (shape, ignite, burn) bucket from the free range (the ids left between
     * FIRST_FREE_ID and LAST_FREE_ID after the two plain stand-ins, the FC plant twins, log2 and, in a pack with modded
     * crops, the plant stand-in: three, or two), with
     * SetFireProperties(ignite, burn) as FC's own blocks set
     * theirs (vanilla/server Block.java:1975-1981). The last free id is kept for the other shape class until that
     * class has a flammable stand-in: a solid cube and a shaped collider are not interchangeable (collision,
     * isNormalCube). Once the range is used up a bucket reuses the stand-in of its shape class with the nearest odds
     * (logged once); with none, it stays on the plain stand-in and is fireproof to FC.
     */
    private static synchronized int flammableIdFor(boolean solid, int ignite, int burn, int plainId) {
        FlammableBucket[] current = flammableBuckets;
        boolean sameShapeHasOne = false;
        boolean otherShapeHasOne = false;
        for (FlammableBucket b : current) {
            // another thread created this bucket while we waited for the lock
            if (b.solid() == solid && b.ignite() == ignite && b.burn() == burn) return b.id();
            if (b.dedicated()) {
                if (b.solid() == solid) sameShapeHasOne = true;
                else otherShapeHasOne = true;
            }
        }

        int free = 0;
        for (int id = FIRST_FREE_ID; id <= LAST_FREE_ID; id++) {
            if (isFreeId(id)) free++;
        }
        boolean mayCreate = free > 1 || (free == 1 && (otherShapeHasOne || !sameShapeHasOne));

        int id = 0;
        boolean dedicated = false;
        if (mayCreate) {
            int newId = nextFreeId(FIRST_FREE_ID);
            if (newId > 0) {
                try {
                    new ModernBlockStandIn(newId, solid, FLAMMABLE_ROCK, "btwModernFlammable" + flammableCreated++)
                            .SetFireProperties(ignite, burn);
                    id = newId;
                    dedicated = true;
                    LOGGER.info("[STAND-IN] flammable {} stand-in {}: ignite {} / burn {} (1.20.1 fire odds)",
                            solid ? "solid cube" : "shaped collider", newId, ignite, burn);
                } catch (Throwable t) {
                    LOGGER.error("[STAND-IN] flammable stand-in creation failed at id {}", newId, t);
                }
            }
        }
        if (!dedicated) {
            int best = Integer.MAX_VALUE;
            for (FlammableBucket b : current) {
                if (!b.dedicated() || b.solid() != solid) continue;
                int distance = Math.abs(b.ignite() - ignite) + Math.abs(b.burn() - burn);
                if (distance < best) {
                    best = distance;
                    id = b.id();
                }
            }
            if (!flammableExhaustedLogged) {
                flammableExhaustedLogged = true;
                LOGGER.warn("[STAND-IN] free legacy ids {}..{} used up for flammable stand-ins; new fire-odds buckets"
                        + " reuse the nearest stand-in of their shape (logged once)", FIRST_FREE_ID, LAST_FREE_ID);
            }
            if (id == 0) id = plainId;
        }

        FlammableBucket[] next = java.util.Arrays.copyOf(current, current.length + 1);
        next[current.length] = new FlammableBucket(solid, ignite, burn, id, dedicated);
        flammableBuckets = next;
        return id;
    }

    /** Live level behind an FC block access: WorldBridge, or (PathFinder's ChunkCache) the pathing entity's world. */
    private static ServerLevel levelOf(Object access, btw.modern.Entity entity) {
        if (access instanceof WorldBridge wb) return wb.getServerLevel();
        if (entity != null && entity.worldObj instanceof WorldBridge wb) return wb.getServerLevel();
        return null;
    }

    // ---- solidity: Block.isNormalCube(id) / IsNormalCube read these (Modern-Common Block.java:340, 913)
    @Override public boolean isOpaqueCube() { return m_bSolidCube; }
    @Override public boolean renderAsNormalBlock() { return m_bSolidCube; }

    // ---- collision: 1.5.2 World.getCollidingBoundingBoxes -> addCollisionBoxesToList (World.java:1617-1624)
    @Override
    @SuppressWarnings("unchecked")
    public void addCollisionBoxesToList(btw.modern.World world, int x, int y, int z,
                                        btw.modern.AxisAlignedBB mask, List list, btw.modern.Entity entity) {
        ServerLevel level = m_bSolidCube ? null : levelOf(world, entity);
        if (level == null) {                    // solid cube (or no live level): inherited full-cube box
            super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        for (AABB b : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
            btw.modern.AxisAlignedBB bb = btw.modern.AxisAlignedBB.getBoundingBox(
                    x + b.minX, y + b.minY, z + b.minZ, x + b.maxX, y + b.maxY, z + b.maxZ);
            if (mask.intersectsWith(bb)) list.add(bb);
        }
    }

    @Override
    public btw.modern.AxisAlignedBB getCollisionBoundingBoxFromPool(btw.modern.World world, int x, int y, int z) {
        ServerLevel level = m_bSolidCube ? null : levelOf(world, null);
        if (level == null) return super.getCollisionBoundingBoxFromPool(world, x, y, z);
        BlockPos pos = new BlockPos(x, y, z);
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty()) return null;
        AABB b = shape.bounds();
        return btw.modern.AxisAlignedBB.getBoundingBox(
                x + b.minX, y + b.minY, z + b.minZ, x + b.maxX, y + b.maxY, z + b.maxZ);
    }

    // ---- pathing: getBlocksMovement is misnamed, true = passable (vanilla/client Block.java:563, 2213-2219)
    @Override
    public boolean getBlocksMovement(btw.modern.IBlockAccess access, int x, int y, int z) {
        return !m_bSolidCube && isPathable(levelOf(access, null), x, y, z);
    }

    @Override
    public boolean CanPathThroughBlock(btw.modern.IBlockAccess access, int i, int j, int k,
                                       btw.modern.Entity entity, btw.modern.PathFinder pathFinder) {
        return !m_bSolidCube && isPathable(levelOf(access, entity), i, j, k);
    }

    private static boolean isPathable(ServerLevel level, int x, int y, int z) {
        if (level == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).isPathfindable(level, pos, PathComputationType.LAND);
    }

    // FCBlockFence.java:136 / FCBlockWall.java:40 / FCBlockFenceGate.java:43 return -3 so A* never plans a
    // hop over a 1.5-high collider; mirror it for modern fences/walls/gates.
    @Override
    public int GetWeightOnPathBlocked(btw.modern.IBlockAccess access, int i, int j, int k) {
        ServerLevel level = m_bSolidCube ? null : levelOf(access, null);
        if (level == null) return 0;
        BlockPos pos = new BlockPos(i, j, k);
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) > 1.0D ? -3 : 0;
    }

    // ---- hardpoints (collider only; the solid cube keeps the isBlockNormalCube default). FC facing 0..5 ==
    // MC Direction 3D data value (DOWN,UP,NORTH,SOUTH,WEST,EAST), cf. WorldBridge.directionToLegacyFacing.
    // A support relative (a modern fence, wall or stained glass pane) answers with its BTW relative's FC block first.
    @Override
    public boolean HasLargeCenterHardPointToFacing(btw.modern.IBlockAccess access, int i, int j, int k,
                                                   int iFacing, boolean bIgnoreTransparency) {
        btw.modern.Block relative = supportRelative(access, i, j, k);
        if (relative != null) {
            return relative.HasLargeCenterHardPointToFacing(access, i, j, k, iFacing, bIgnoreTransparency);
        }
        if (m_bSolidCube) return super.HasLargeCenterHardPointToFacing(access, i, j, k, iFacing, bIgnoreTransparency);
        ServerLevel level = levelOf(access, null);
        if (level == null || iFacing < 0 || iFacing > 5) return false;
        BlockPos pos = new BlockPos(i, j, k);
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.from3DDataValue(iFacing));
    }

    @Override
    public boolean HasSmallCenterHardPointToFacing(btw.modern.IBlockAccess access, int i, int j, int k,
                                                   int iFacing, boolean bIgnoreTransparency) {
        btw.modern.Block relative = supportRelative(access, i, j, k);
        if (relative != null) {
            return relative.HasSmallCenterHardPointToFacing(access, i, j, k, iFacing, bIgnoreTransparency);
        }
        if (m_bSolidCube) return super.HasSmallCenterHardPointToFacing(access, i, j, k, iFacing, bIgnoreTransparency);
        ServerLevel level = levelOf(access, null);
        if (level == null || iFacing < 0 || iFacing > 5) return false;
        return net.minecraft.world.level.block.Block.canSupportCenter(
                level, new BlockPos(i, j, k), Direction.from3DDataValue(iFacing));
    }

    // FCMOD-INTEGRATION(1.20.1): support_relatives: a modern fence, wall or stained glass pane answers FC's support questions (hard points, ground cover resting on it and its offset) as its BTW relative, on WorldBridge reads; identity, drops, hooks, collision and rendering stay the stand-in's (FcAttachSupport.SUPPORT_RELATIVE)
    @Override
    public boolean HasCenterHardPointToFacing(btw.modern.IBlockAccess access, int i, int j, int k,
                                              int iFacing, boolean bIgnoreTransparency) {
        btw.modern.Block relative = supportRelative(access, i, j, k);
        return relative != null ? relative.HasCenterHardPointToFacing(access, i, j, k, iFacing, bIgnoreTransparency)
                : super.HasCenterHardPointToFacing(access, i, j, k, iFacing, bIgnoreTransparency);
    }

    @Override
    public boolean CanGroundCoverRestOnBlock(btw.modern.World world, int i, int j, int k) {
        btw.modern.Block relative = supportRelative(world, i, j, k);
        return relative != null ? relative.CanGroundCoverRestOnBlock(world, i, j, k)
                : super.CanGroundCoverRestOnBlock(world, i, j, k);
    }

    @Override
    public float GroundCoverRestingOnVisualOffset(btw.modern.IBlockAccess access, int i, int j, int k) {
        btw.modern.Block relative = supportRelative(access, i, j, k);
        return relative != null ? relative.GroundCoverRestingOnVisualOffset(access, i, j, k)
                : super.GroundCoverRestingOnVisualOffset(access, i, j, k);
    }

    /**
     * The BTW relative's FC block for the live state at (i, j, k) of a WorldBridge read (FcAttachSupport.supportRelative),
     * else null: any other access (PathFinder's ChunkCache, ProxyBlock's shape and display worlds, FCBakedModel's live
     * access) keeps the stand-in's own answers. The solid stand-in answers null before any read: no fence, wall or
     * pane state is a full solid cube, so it never stands in for a support relative, and its hard points (WorldBridge's
     * solid top, falling blocks, attachments on the most common modern blocks) read no extra block state.
     */
    private btw.modern.Block supportRelative(Object access, int i, int j, int k) {
        if (m_bSolidCube) return null;
        ServerLevel level = levelOf(access, null);
        if (level == null) return null;
        BlockPos pos = new BlockPos(i, j, k);
        return FcAttachSupport.supportRelative(level.getBlockState(pos), level, pos);
    }

    // ---- eternal fire: FCBlockFire, which owns the FIRE tick, keeps a fire alive over netherrack, End bedrock or a
    // neighbour whose DoesInfiniteBurnToFacing names the fire's side (Server FCBlockFire.java:49-62, 157-176). The 1.20.1
    // fire sources it does not know (magma blocks, datapack additions to the infiniburn tags) answer from the live
    // state: 1.20.1 asks only the block below the fire, facing up (FireBlock.tick:136, isFireSource(level, below, UP)),
    // so a stand-in answers for that side only.
    @Override
    public boolean DoesInfiniteBurnToFacing(btw.modern.IBlockAccess access, int i, int j, int k, int iFacing) {
        if (iFacing != 1) return false;   // 1 = up (FC facing == Direction 3D data value)
        ServerLevel level = levelOf(access, null);
        if (level == null) return false;
        try {
            BlockPos pos = new BlockPos(i, j, k);
            return level.getBlockState(pos).isFireSource(level, pos, Direction.UP);
        } catch (Throwable t) {
            return false;   // read inside FC's fire tick: never throw out of it
        }
    }

    // ---- FC pistons (run on MC pistons via BlockBehaviorMixin.btw$neighborChanged) copy id+meta to move a
    // block; a stand-in id cannot be written back (WorldBridge.setBlock no-op), so pushing one would delete
    // the modern block. 2 = immovable (1.5.2 Material mobility, like obsidian).
    @Override public int getMobilityFlag() { return 2; }

    // ---- hardness / blast resistance from the live state (1.5.2 units: stone 1.5 / 10*3/5 = 6 == MC values)
    @Override
    public float getBlockHardness(btw.modern.World world, int x, int y, int z) {
        ServerLevel level = levelOf(world, null);
        if (level == null) return super.getBlockHardness(world, x, y, z);
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).getDestroySpeed(level, pos);
    }

    @Override
    public float getExplosionResistance(btw.modern.Entity entity, btw.modern.World world, int i, int j, int k) {
        ServerLevel level = levelOf(world, entity);
        if (level != null) {
            try {
                BlockPos pos = new BlockPos(i, j, k);
                return level.getBlockState(pos).getExplosionResistance(level, pos, null);
            } catch (Throwable ignored) { }
        }
        return super.getExplosionResistance(entity, world, i, j, k);
    }

    // ---- footsteps: frozen Entity.playStepSound plays GetStepSound(world, x, y, z).getStepSound() at
    // getVolume() * 0.15F, getPitch() (vanilla/client Entity.java:1016-1044), the formula 1.20.1
    // Entity.playStepSound applies to a SoundType. The constructor's stone made every FC puppet walking on
    // tuff, polished deepslate, concrete, copper, mud, ... step on stone; the live block's own SoundType is
    // reported instead, as namespaced 1.20.1 ids that WorldBridge.resolveSoundEvent passes straight through.
    @Override
    public btw.modern.StepSound GetStepSound(btw.modern.World world, int i, int j, int k) {
        ServerLevel level = levelOf(world, null);
        if (level != null) {
            try {
                BlockPos pos = new BlockPos(i, j, k);
                SoundType type = level.getBlockState(pos).getSoundType(level, pos, null);
                if (type != null) {
                    return STEP_SOUNDS.computeIfAbsent(type, ModernStepSound::new);
                }
            } catch (Throwable t) {
                // Read inside FC entity movement: never throw out of it.
                if (!stepSoundFailureLogged) {
                    stepSoundFailureLogged = true;
                    LOGGER.warn("[STAND-IN] step sound lookup failed at {},{},{}; stone is used (logged once)",
                            i, j, k, t);
                }
            }
        }
        return super.GetStepSound(world, i, j, k);
    }

    /** A 1.20.1 SoundType seen as a 1.5.2 StepSound: same volume / pitch, the events as namespaced ids. */
    private static final class ModernStepSound extends btw.modern.StepSound {
        private final String m_sStep;
        private final String m_sBreak;
        private final String m_sPlace;

        ModernStepSound(SoundType type) {
            // the name only feeds the default "dig." / "step." getters, all overridden below
            super("stone", type.getVolume(), type.getPitch());
            m_sStep = type.getStepSound().getLocation().toString();
            m_sBreak = type.getBreakSound().getLocation().toString();
            m_sPlace = type.getPlaceSound().getLocation().toString();
        }

        @Override public String getStepSound() { return m_sStep; }
        @Override public String getStepResourcePath() { return m_sStep; }
        @Override public String getBreakSound() { return m_sBreak; }
        @Override public String getPlaceSound() { return m_sPlace; }
    }

    // ---- never mint FC stacks for a phantom id; FC-destroyed modern blocks (FCExplosionMining.DestroyBlock,
    // harvest/saw paths) drop the MC loot of the live state instead.
    @Override public int idDropped(int metadata, Random random, int fortune) { return 0; }

    @Override
    public void dropBlockAsItemWithChance(btw.modern.World world, int x, int y, int z,
                                          int metadata, float chance, int fortune) {
        // 1.5.2 Block.dropBlockAsItemWithChance rolls `chance` per drop (vanilla/client Block.java:859-878);
        // the MC loot table is dropped as one roll.
        if (chance < 1.0F && world != null && world.rand != null && world.rand.nextFloat() > chance) return;
        dropMcLoot(world, x, y, z);
    }

    @Override
    public void DropItemsOnDestroyedByExplosion(btw.modern.World world, int i, int j, int k,
                                                btw.modern.Explosion explosion) {
        // FCMOD-INTEGRATION(1.20.1): fc_blast_rules: a class-S log (stems, hyphae, other mods' logs; FCLogRules.isForeignLog) breaks under a mining charge into FCBlockLog's components (Server FCBlockLog.java:123-130): 6 sawdust and, while it has bark, its bark; every other stand-in drops its 1.20.1 loot
        ServerLevel level = levelOf(world, null);
        if (level != null) {
            BlockPos pos = new BlockPos(i, j, k);
            BlockState state = level.getBlockState(pos);
            if (FCLogRules.isForeignLog(state)) {
                float chance = explosion != null ? 1.0F / explosion.explosionSize : 1.0F;
                popEach(world, level, pos, FCLogRules.sawdust(1), 6, chance);
                if (FCLogRules.barked(level, pos, state)) {
                    popEach(world, level, pos, FCLogRules.bark(FCLogRules.barkSpecies(state)), 1, chance);
                }
                return;
            }
        }
        dropMcLoot(world, i, j, k);
    }

    /** 1.5.2 Block.DropItemsIndividualy (vanilla/server Block.java:1703-1714): n single items, each at the chance. */
    private static void popEach(btw.modern.World world, ServerLevel level, BlockPos pos,
                                net.minecraft.world.item.ItemStack one, int n, float chance) {
        if (one.isEmpty()) return;                                 // bark(-1): the stems
        for (int c = 0; c < n; c++) {
            if (world.rand.nextFloat() <= chance) {
                net.minecraft.world.level.block.Block.popResource(level, pos, one.copy());
            }
        }
    }

    static void dropMcLoot(btw.modern.World world, int x, int y, int z) {
        ServerLevel level = levelOf(world, null);
        if (level == null) return;
        try {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                net.minecraft.world.level.block.Block.dropResources(state, level, pos, level.getBlockEntity(pos));
            }
        } catch (Throwable t) {
            LOGGER.debug("[STAND-IN] MC loot drop failed at {},{},{}: {}", x, y, z, t.toString());
        }
    }

    // ---- the saw never breaks on a modern block (modern blocks were air to FCBlockSaw.SawBlockToFront before): no
    // broken saws on modern wood/stone. GetStackRetrievedByBlockDispenser (null) / IsBlockDestroyedByBlockDispenser
    // (false) keep Modern-Common's inert defaults.
    @Override public boolean DoesBlockBreakSaw(btw.modern.World world, int i, int j, int k) { return false; }
    @Override public boolean OnBlockSawed(btw.modern.World world, int i, int j, int k) { return false; }

    // ---- FC machine hooks (INTEGRATIONS #022): FCBlockSaw (the 7-argument OnBlockSawed it calls, javap offset 167),
    // FCItemStumpRemover (GetDoesStumpRemoverWorkOnBlock) and FCItemDye (AttemptToApplyFertilizerTo) act on a modern
    // block only through a hook an integration registered for the live state (ModernBlockHooks); with none, or when the
    // hook throws, FC's inert answer (false) stands.
    // FCMOD-INTEGRATION(1.20.1): fc_machine_hooks: FC's saw, stump remover and fertilizer reach modern blocks through registered hooks instead of always doing nothing

    @Override
    public boolean OnBlockSawed(btw.modern.World world, int i, int j, int k, int iSawPosI, int iSawPosJ,
                                int iSawPosK) {
        Direction sawFacing = Direction.fromDelta(i - iSawPosI, j - iSawPosJ, k - iSawPosK);
        if (sawFacing == null) return false;
        return askHook(world, i, j, k, (hook, level, pos, state) -> hook.onSawed(level, pos, state, sawFacing));
    }

    @Override
    public boolean GetDoesStumpRemoverWorkOnBlock(btw.modern.IBlockAccess access, int i, int j, int k) {
        return askHook(access, i, j, k, ModernBlockHooks.Hook::stumpRemoverWorks);
    }

    @Override
    public boolean AttemptToApplyFertilizerTo(btw.modern.World world, int i, int j, int k) {
        return askHook(world, i, j, k, ModernBlockHooks.Hook::applyFertilizer);
    }

    /** One FC machine question to a {@link ModernBlockHooks.Hook}. */
    @FunctionalInterface
    private interface HookCall {
        boolean ask(ModernBlockHooks.Hook hook, ServerLevel level, BlockPos pos, BlockState state);
    }

    private static volatile boolean hookFailureLogged;

    /** The registered hook's answer for the live state at (i, j, k); false without a live level or hook, or on a throw. */
    private static boolean askHook(Object access, int i, int j, int k, HookCall call) {
        ServerLevel level = levelOf(access, null);
        if (level == null) return false;
        BlockPos pos = new BlockPos(i, j, k);
        try {
            BlockState state = level.getBlockState(pos);
            ModernBlockHooks.Hook hook = ModernBlockHooks.find(state);
            return hook != null && call.ask(hook, level, pos, state);
        } catch (Throwable t) {
            // Runs inside FC item use and the saw's tick: never throw out of them.
            if (!hookFailureLogged) {
                hookFailureLogged = true;
                LOGGER.warn("[STAND-IN] FC machine hook failed at {}; FC's inert answer stands (logged once)", pos, t);
            }
            return false;
        }
    }

    // ---- soils (maintainer decision D3). FC's plant rules ask the block below through these hooks: BlockFlower /
    // FCBlockPlants.CanGrowOnBlock (Server FCBlockPlants.java:167-171), FCBlockSapling (:218-222), FCBlockCactus
    // .canBlockStay (:67-78), FCBlockReed.canPlaceBlockAt (:72-80). Modern soils answer from their live 1.20.1 tags,
    // the soils 1.20.1 itself grows these plants on; without this, ferns, flowers, saplings, cactus and cane that
    // 1.20.1 world generation placed on podzol, moss, mud, rooted or coarse dirt or red sand pop as soon as FC's
    // survival rules run. Domesticated crops, nether wart and hydration stay FC-only (farmland, soul sand, planters).
    @Override
    public boolean CanWildVegetationGrowOnBlock(btw.modern.World world, int i, int j, int k) {
        return liveStateIs(world, i, j, k, BlockTags.DIRT, null);
    }

    @Override
    public boolean CanSaplingsGrowOnBlock(btw.modern.World world, int i, int j, int k) {
        return liveStateIs(world, i, j, k, BlockTags.DIRT, null);
    }

    @Override
    public boolean CanReedsGrowOnBlock(btw.modern.World world, int i, int j, int k) {
        return liveStateIs(world, i, j, k, BlockTags.DIRT, BlockTags.SAND);
    }

    @Override
    public boolean CanCactusGrowOnBlock(btw.modern.World world, int i, int j, int k) {
        return liveStateIs(world, i, j, k, BlockTags.SAND, null);
    }

    @Override public boolean CanDomesticatedCropsGrowOnBlock(btw.modern.World world, int i, int j, int k) { return false; }
    @Override public boolean CanNetherWartGrowOnBlock(btw.modern.World world, int i, int j, int k) { return false; }
    @Override public boolean IsBlockHydratedForPlantGrowthOn(btw.modern.World world, int i, int j, int k) { return false; }

    /** True if the live state at (i, j, k) carries {@code tag} or {@code orTag}; false without a live level. */
    private static boolean liveStateIs(btw.modern.World world, int i, int j, int k,
                                       TagKey<net.minecraft.world.level.block.Block> tag,
                                       TagKey<net.minecraft.world.level.block.Block> orTag) {
        ServerLevel level = levelOf(world, null);
        if (level == null) return false;
        BlockState state = level.getBlockState(new BlockPos(i, j, k));
        return state.is(tag) || (orTag != null && state.is(orTag));
    }
}
