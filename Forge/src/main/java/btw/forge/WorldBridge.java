package btw.forge;

import btw.modern.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * Full {@link btw.modern.WorldServer} implementation backed by a Forge 1.20.1
 * {@link ServerLevel}: the single bridge between FC legacy code (int IDs, block coords)
 * and the modern block-state system, and the object BTWLifecycle.wireWorlds puts in
 * MinecraftServer.worldServers[] (1.5.2 order: overworld, nether, end).
 *
 * Instances are cached per-ServerLevel via {@link #getOrCreate(ServerLevel)} and
 * released by {@link #clearCache()} when the server stops.
 */
// Extends WorldServer: in 1.5.2 every server-side worldObj IS a WorldServer and FC/frozen code
// casts to it unguarded -- FCEntityCow.java:528 / FCEntitySquid.java:1069
// (SendPacketToAllPlayersTrackingEntity), frozen EntityCreature.java:623
// (TransmitAttackTargetToClients), EntityLiving.java:864 (equipment sync), :2981 (onItemPickup);
// the ClassCastException aborted the rest of the entity's onUpdate. The frozen
// Entity.onEntityUpdate portal block (vanilla/client Entity.java:456-459) that forced the
// 2026-07-09 revert now links: remapFcCode relocates net.minecraft.server.MinecraftServer to
// btw.modern.MinecraftServer and Modern-Common WorldServer declares getMinecraftServer(); its
// travelToDimension branch stays inert because ForgeMinecraftServerWrapper.getAllowNether()
// is false (1.20.1 owns dimension travel of puppets).
// Every WorldServer stub body overrides an abstract World method that this class implements
// against the live level, so inheriting WorldServer changes no dispatch (adds getEntityTracker).
public class WorldBridge extends btw.modern.WorldServer {

    private static final Logger LOGGER = LogManager.getLogger("BTW-WorldBridge");

    /**
     * Metadata storage for vanilla blocks that don't have ProxyBlock.META.
     * FC code sets metadata on vanilla blocks (sapling growth stage, leaf decay flag, stem growth 0-14,
     * jack o'lantern placed flag), but vanilla MC blocks have no META property and their state cannot
     * hold every bit. The overlay stores the FC metadata keyed by block position so getBlockMetadata can
     * return it, and is persisted with the level (FcMetaOverlay, data/btw_fc_meta.dat): it used to be an
     * in-memory map cleared at server stop, so FC lost all of it on every restart.
     * Entries are removed when FC changes the block at that position, and ignored (then dropped) once the
     * MC block there is no longer the one FC wrote to, or no longer derives to what it derived to after FC's
     * write: MC replaces and changes blocks without going through WorldBridge (explosions, player placement,
     * pistons, fluids, bone meal), and a stale value used to win over the derived one. Only metadata that
     * differs from the derived value is stored (see putMetaOverride).
     * Null once detached (the data stays with the level's storage).
     */
    private FcMetaOverlay vanillaMetaOverrides;

    private static final Map<ServerLevel, WorldBridge> cache = new WeakHashMap<>();

    /**
     * Returns (or creates) the WorldBridge for the given ServerLevel.
     */
    public static WorldBridge getOrCreate(ServerLevel level) {
        return cache.computeIfAbsent(level, WorldBridge::new);
    }

    /**
     * Drops every cached bridge and detaches it from its level. Called once the server has
     * fully stopped (BTWLifecycle.onServerStopped, ServerStoppedEvent). Each bridge holds its
     * ServerLevel key strongly (directly, and through the proxies in fcToForgeEntity), so the
     * WeakHashMap could never evict an entry: every closed world's server stayed reachable for
     * the life of the JVM (triage RE2). A ServerLevel lives exactly as long as its server, so
     * releasing at stop is leak-free; detach also drops the level and the proxy map so a bridge
     * still referenced from elsewhere (a stale PlayerBridge, an FC object) cannot pin them
     * (WorldInfoBridge only holds the level weakly).
     */
    static void clearCache() {
        List<WorldBridge> bridges = new ArrayList<>(cache.values());
        cache.clear();
        for (WorldBridge bridge : bridges) {
            bridge.detach();
        }
    }

    private void detach() {
        fcToForgeEntity.clear();
        projectileTargets.clear();
        // Persisted: the level's DimensionDataStorage already saved it at stop; only drop the reference.
        vanillaMetaOverrides = null;
        level = null;
    }

    // Not final: nulled by detach() once the server has stopped (see clearCache).
    private ServerLevel level;

    public WorldBridge(ServerLevel level) {
        this.level = level;
        this.rand = new Random();
        this.isRemote = false;
        // Eagerly initialise FC-only data holders so FC code never
        // encounters null when accessing these lists.
        this.m_MagneticPointList = new FCMagneticPointList();
        this.m_SpawnLocationList = new FCSpawnLocationList();
        this.m_LootingBeaconLocationList = new FCBeaconEffectLocationList();

        // Set difficulty (FC checks this for food/starvation logic)
        // MC 1.20.1: PEACEFUL=0, EASY=1, NORMAL=2, HARD=3
        this.difficultySetting = level.getDifficulty().getId();

        // WorldInfo bridge — FC code accesses world.worldInfo directly
        this.worldInfo = new WorldInfoBridge(level);

        // Set up the WorldProvider so FC code can read dimensionId.
        this.provider = new btw.modern.WorldProvider() {};
        this.provider.worldObj = this;
        if (level.dimension() == net.minecraft.world.level.Level.NETHER) {
            this.provider.dimensionId = -1;
            this.provider.isHellWorld = true;
            this.provider.hasNoSky = true;
            // 1.5.2 WorldProviderHell.generateLightBrightnessTable — same curve as the
            // base table (generated in the WorldProvider shim ctor) but with a 0.1F
            // ambient floor so the Nether is never pitch black.
            float fAmbient = 0.1F;
            for (int i = 0; i <= 15; ++i) {
                float f = 1.0F - (float) i / 15.0F;
                this.provider.lightBrightnessTable[i] =
                        (1.0F - f) / (f * 3.0F + 1.0F) * (1.0F - fAmbient) + fAmbient;
            }
        } else if (level.dimension() == net.minecraft.world.level.Level.END) {
            this.provider.dimensionId = 1;
            this.provider.hasNoSky = true;
        } else {
            this.provider.dimensionId = 0;
        }

        // 1.5.2 World.setItemData/loadItemData/getUniqueDataId delegate to mapStorage
        // (vanilla/server World.java:3873-3894) — FCItemEmptyMap.java:41 map crafting.
        // Unique IDs come from the live level's map-ID counter so BTW-crafted maps
        // never collide with modern filled maps; the data itself is held in-memory.
        this.mapStorage = new MapStorage() {
            private final Map<String, WorldSavedData> loadedDataMap = new HashMap<>();

            @Override
            public void setData(String key, WorldSavedData data) {
                loadedDataMap.put(key, data);
            }

            @Override
            public WorldSavedData loadData(Class dataClass, String key) {
                return loadedDataMap.get(key);
            }

            @Override
            public int getUniqueDataId(String key) {
                // Read the bridge's field, not the constructor parameter: capturing the
                // parameter would pin the ServerLevel past detach() (RE2).
                ServerLevel live = WorldBridge.this.level;
                return live != null ? live.getFreeMapId() : 0;
            }
        };

        // 1.5.2 World.m_localEnderChestInventory / m_localLowPowerEnderChestInventory
        // (BTW-patched vanilla/server World.java:4861-4862) — FCBlockEnderChest
        // antenna tiers 1/2 open these per-dimension communal inventories. Persisted
        // per-dimension like vanilla SaveHandler.java:435-479 ("FCEnderItems"/"FCLPEnderItems").
        this.m_localEnderChestInventory = new InventoryEnderChest();
        this.m_localLowPowerEnderChestInventory = new InventoryEnderChest();
        try {
            FcWorldSavedData.attach(this);
        } catch (Throwable t) {
            // Runs inside getOrCreate during chunk/entity loads -- never let it escape (RE1).
            LOGGER.warn("Could not attach FC world saved data: {}", t.toString());
        }
        // FC metadata of vanilla blocks, per dimension; never throws (falls back to an unpersisted overlay).
        this.vanillaMetaOverrides = FcMetaOverlay.attach(level);
    }

    /**
     * Returns the underlying modern ServerLevel.
     */
    public ServerLevel getServerLevel() {
        return level;
    }

    // ================================================================
    // IChunkProvider (required abstract)
    // ================================================================

    
    public IChunkProvider createChunkProvider() {
        // FC code should not call this on the Forge backend
        return null;
    }

    // ================================================================
    // Block access
    // ================================================================


    // Never an id with an empty blocksList slot (frozen PathFinder/Entity/EntityLiving dereference
    // blocksList[id] unguarded, triage HAI-4); colliding modern-only states read as ModernBlockStandIn,
    // not air (design_blockmap U4); double slabs read as 43/125; seagrass/kelp/bubble columns as water (D5).
    public int getBlockId(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return ProxyRegistry.getWorldBlockId(level.getBlockState(pos), level, pos);
    }


    public int getBlockMetadata(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(ProxyBlock.META)) {
            return state.getValue(ProxyBlock.META);
        }
        // Derive metadata from MC 1.20.1 BlockState properties for vanilla blocks
        int derived = deriveVanillaMetadata(level, state, pos);

        // The persisted overlay holds the FC bits the state cannot (never stored or read where the MC state is the
        // whole truth, see isMetaFullyDerived), valid only while the block FC wrote is still there and still derives
        // to what it derived to after FC's write
        FcMetaOverlay overlay = vanillaMetaOverrides;
        if (overlay != null) {
            FcMetaOverlay.MetaOverride override = overlay.get(pos);
            if (override != null) {
                // FCMOD-INTEGRATION(1.20.1): placement_validity: a fully derived block never reads the overlay, so an entry saved before its block joined isMetaFullyDerived (a hook FC placed before the hook codec: meta 0 saved against a derived 2) is dropped, not read
                if (override.block() == state.getBlock() && override.derivedAtWrite() == derived
                        && !isMetaFullyDerived(state.getBlock())) {
                    return override.meta();
                }
                overlay.remove(pos, override); // MC changed the block behind FC's back, or its state is the whole truth: derive instead
            }
        }
        return derived;
    }

    /**
     * FC metadata {@code meta} written at {@code pos}, whose 1.20.1 state is (or is about to be) {@code stateAfterWrite}:
     * kept in the overlay only where the state does not already derive to it. Stored before the 1.20.1 write, as
     * before: the write's neighbour notifications run FC code that reads this position back.
     */
    private void putMetaOverride(BlockPos pos, BlockState stateAfterWrite, int meta) {
        FcMetaOverlay overlay = vanillaMetaOverrides;
        if (overlay == null) return;
        net.minecraft.world.level.block.Block block = stateAfterWrite.getBlock();
        int derived = block instanceof ProxyBlock || isMetaFullyDerived(block)
                ? meta : deriveVanillaMetadata(level, stateAfterWrite, pos);
        if (derived == meta) {
            overlay.remove(pos);
        } else {
            overlay.put(pos, block, derived, meta);
        }
    }

    /** Drops any FC metadata stored for {@code pos} (FC replaced the block there). */
    private void clearMetaOverride(BlockPos pos) {
        FcMetaOverlay overlay = vanillaMetaOverrides;
        if (overlay != null) {
            overlay.remove(pos);
        }
    }

    private static boolean isMetaFullyDerived(net.minecraft.world.level.block.Block block) {
        if (TwinRegistry.isTwinBlock(block)) return false;
        return block instanceof net.minecraft.world.level.block.FenceGateBlock
                || block instanceof net.minecraft.world.level.block.SlabBlock
                || block instanceof net.minecraft.world.level.block.TrapDoorBlock
                || block instanceof net.minecraft.world.level.block.StairBlock
                || block instanceof net.minecraft.world.level.block.CropBlock
                || block instanceof net.minecraft.world.level.block.NetherWartBlock
                || block instanceof net.minecraft.world.level.block.CactusBlock
                || block instanceof net.minecraft.world.level.block.SugarCaneBlock
                || block instanceof net.minecraft.world.level.block.FireBlock
                || block instanceof net.minecraft.world.level.block.FarmBlock
                || block instanceof net.minecraft.world.level.block.CocoaBlock
                || block instanceof net.minecraft.world.level.block.VineBlock
                || block instanceof net.minecraft.world.level.block.TripWireHookBlock
                || block instanceof net.minecraft.world.level.block.LeverBlock
                || block instanceof net.minecraft.world.level.block.ButtonBlock;
    }

    private static int faceAttachedToLegacyMeta(BlockState state) {
        net.minecraft.core.Direction facing = state.getValue(
                net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACING);
        boolean alongZ = facing.getAxis() == net.minecraft.core.Direction.Axis.Z;
        int meta = switch (state.getValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE)) {
            case FLOOR -> alongZ ? 5 : 6;
            case CEILING -> alongZ ? 7 : 0;
            case WALL -> switch (facing) {
                case EAST -> 1;
                case WEST -> 2;
                case SOUTH -> 3;
                default -> 4;
            };
        };
        return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED) ? meta | 8 : meta;
    }

    private static BlockState legacyMetaToFaceAttached(BlockState state, int metadata) {
        net.minecraft.world.level.block.state.properties.AttachFace face = switch (metadata & 7) {
            case 5, 6 -> net.minecraft.world.level.block.state.properties.AttachFace.FLOOR;
            case 0, 7 -> net.minecraft.world.level.block.state.properties.AttachFace.CEILING;
            default -> net.minecraft.world.level.block.state.properties.AttachFace.WALL;
        };
        net.minecraft.core.Direction facing = switch (metadata & 7) {
            case 1 -> net.minecraft.core.Direction.EAST;
            case 0, 2, 6 -> net.minecraft.core.Direction.WEST;
            case 3 -> net.minecraft.core.Direction.SOUTH;
            default -> net.minecraft.core.Direction.NORTH;
        };
        return state.setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE, face)
                .setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACING, facing)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, (metadata & 8) != 0);
    }

    /**
     * Derives legacy MC 1.5.2 metadata from MC 1.20.1 BlockState properties.
     * For logs: axis → orientation bits, and bottom-of-tree → stump flag.
     *
     * <p>Side-agnostic and static: client code with no WorldBridge (ProxyEntity.createDummyClientWorld, the
     * FCMiningSpeed dig prediction, FCBakedModel.LiveAccess) derives the same metadata as the server, so
     * e.g. a stump log (FCBlockLog hardness x3 for meta 12) is not predicted to break 3x early on the client
     * (w1 review P1). Never throws: any failure answers 0. Server-only vanillaMetaOverrides are applied by
     * {@link #getBlockMetadata}, not here.</p>
     */
    public static int deriveVanillaMetadata(net.minecraft.world.level.BlockGetter level, BlockState state,
                                            BlockPos pos) {
        try {
            return deriveVanillaMetadataUnchecked(level, state, pos);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int deriveVanillaMetadataUnchecked(net.minecraft.world.level.BlockGetter level,
                                                      BlockState state, BlockPos pos) {
        if (state == null) return 0;
        net.minecraft.world.level.block.Block block = state.getBlock();

        // FC plant twins (TwinRegistry): species or the growth level of AGE. First: a waterlogged planted propagule
        // would answer 0 in the water-holder branch below, and beetroots / torchflower crops are CropBlocks whose AGE
        // is not FC's level. The FC stage / the levels in between live in the overlay (getBlockMetadata).
        int twinMeta = TwinRegistry.deriveMeta(state);
        if (twinMeta >= 0) return twinMeta;

        // Fluids: MC LEVEL (0 = source, 1-7 = flowing, 8+ = falling) has the same
        // semantics as 1.5.2 fluid metadata -- required by World.handleMaterialAcceleration's
        // fluid-surface check and BlockFluid.getFlowVector (vanilla World.java:2186).
        if (block instanceof net.minecraft.world.level.block.LiquidBlock) {
            if (state.hasProperty(net.minecraft.world.level.block.LiquidBlock.LEVEL)) {
                return state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL);
            }
            return 0;
        }

        // Unmapped water-holders (seagrass, kelp, bubble columns, coral wall fans, dripleaf, waterlogged modern
        // blocks) read as water id 8 (ProxyRegistry.getWorldBlockId, triage D5) or as a ModernBlockStandIn, which
        // ignores metadata. They only ever hold a SOURCE: 1.5.2 fluid level 0. Answered before the FACING /
        // POWERED branches below, whose 2-5 / 8 would read as flowing / falling water to BlockFluid and
        // Entity.handleMaterialAcceleration (a spurious current at every coral wall fan).
        if (ProxyRegistry.getBlockId(block) == 0
                && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            return 0;
        }

        // Logs: axis property → orientation, bottom log → stump. Also the bark and stripped forms of the four species,
        // which FC reads as log 17 (ProxyRegistry.getWorldBlockId).
        if (block instanceof net.minecraft.world.level.block.RotatedPillarBlock) {
            int species = ProxyRegistry.getTreeLogSpecies(block);
            if (species >= 0) { // Block.wood (log)
                int meta = 0;
                // Axis → orientation: Y=0, X(east-west)=1<<2=4, Z(north-south)=2<<2=8
                if (state.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) {
                    net.minecraft.core.Direction.Axis axis = state.getValue(
                            net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
                    if (axis == net.minecraft.core.Direction.Axis.X) meta |= 4;      // east-west
                    else if (axis == net.minecraft.core.Direction.Axis.Z) meta |= 8;  // north-south
                    // Y = 0 (default, vertical)
                }

                // Stump detection: one rule for every log (FCLogRules.isStump: vertical, on a solid block that is not
                // air and not in #minecraft:logs), shared with the class-F/S log rule (ModernLogRules).
                if ((meta & 12) == 0 && FCLogRules.isStump(level, pos, state)) {
                    meta = 12; // stump: both orientation bits set (3 << 2)
                }

                // Wood type (1.5.2 log species, bits 0-1)
                return (meta & 12) | species;
            }
        }

        // Slabs: 1.5.2 BlockHalfSlab meta = species (meta & 7, BlockStep.java:23 / BlockWoodSlab.java:23)
        // | 8 for the upper half (FCMOD GetIsUpsideDown, vanilla/client BlockHalfSlab.java:282-290). A DOUBLE
        // slab reads as id 43/125 (ProxyRegistry.getWorldBlockId) with the species only. Modern-Common
        // BlockHalfSlab re-applies the 1.5.2 half-slab bounds on this encoding.
        if (block instanceof net.minecraft.world.level.block.SlabBlock
                && state.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
            int species = Math.max(ProxyRegistry.getLegacyVariantMeta(block), 0);
            return state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)
                    == net.minecraft.world.level.block.state.properties.SlabType.TOP ? species | 8 : species;
        }

        // Fence gates: 1.5.2 BlockFenceGate meta = direction (S=0, W=1, N=2, E=3: the placer's facing,
        // vanilla/client BlockFenceGate.java:101-105, same as 1.20.1 Direction.get2DDataValue) | 4 when open.
        // Must precede the generic HORIZONTAL_FACING branch (N=2, S=3, W=4, E=5, OPEN lost), which made closed
        // east/west gates read as open. Modern-Common BlockFenceGate re-applies the 1.5.2 collision on this.
        if (block instanceof net.minecraft.world.level.block.FenceGateBlock) {
            int meta = state.getValue(net.minecraft.world.level.block.FenceGateBlock.FACING).get2DDataValue();
            return state.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN) ? meta | 4 : meta;
        }

        // Stairs: 1.5.2 BlockStairs meta = direction of the high half (0=E, 1=W, 2=S, 3=N, from the placer's yaw,
        // vanilla/client BlockStairs.java:533-556; bounds :82-223) | 4 when upside-down. Must precede the generic
        // HORIZONTAL_FACING branch, which used to return first (2-5): east stairs (5) decoded as west + upside-down.
        // Matches the reverse in applyFcMetadataToVanillaBlock. Keyed on StairBlock, not on HALF: trapdoors also
        // carry HORIZONTAL_FACING + HALF with a different encoding (below).
        if (block instanceof net.minecraft.world.level.block.StairBlock
                && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)) {
            net.minecraft.core.Direction dir = state.getValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            int meta;
            switch (dir) {
                case EAST: meta = 0; break;
                case WEST: meta = 1; break;
                case SOUTH: meta = 2; break;
                case NORTH: meta = 3; break;
                default: meta = 0;
            }
            if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)
                    == net.minecraft.world.level.block.state.properties.Half.TOP) {
                meta |= 4;
            }
            return meta;
        }

        // Trapdoors: 1.5.2 BlockTrapDoor meta = hinge direction (0-3) | 4 when open | 8 for the upper half
        // (vanilla/client BlockTrapDoor.java:94-129 bounds, :226-256 placement, :297-300 isTrapdoorOpen).
        // Placement on side 2/3/4/5 gives 0/1/2/3, the same face 1.20.1 TrapDoorBlock.getStateForPlacement
        // stores as FACING NORTH/SOUTH/WEST/EAST, and the open bounds agree (meta 0 -> z 0.8125..1 =
        // NORTH_OPEN_AABB). Block.trapdoor is FCBlockTrapDoor (vanilla/client Block.java:314): its pathing,
        // zombie barricade-breaking and collision read this meta (Server FCBlockTrapDoor.java:81-115, :149-196).
        // The generic HORIZONTAL_FACING branch lost OPEN/HALF and made closed west/east trapdoors (4/5) read open.
        if (block instanceof net.minecraft.world.level.block.TrapDoorBlock
                && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            int meta;
            switch (state.getValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                case SOUTH: meta = 1; break;
                case WEST: meta = 2; break;
                case EAST: meta = 3; break;
                default: meta = 0; // NORTH
            }
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
                meta |= 4;
            }
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)
                            == net.minecraft.world.level.block.state.properties.Half.TOP) {
                meta |= 8;
            }
            return meta;
        }

        // Farmland: MC MOISTURE (0-7) → FC legacy metadata (0-7, >0 = hydrated)
        if (block instanceof net.minecraft.world.level.block.FarmBlock) {
            if (state.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE)) {
                return state.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE);
            }
            return 0;
        }

        // Plant and fire growth counters. Every branch below the farmland one must precede the generic FACING /
        // HORIZONTAL_FACING / POWERED branches (cocoa, attached stems and carved pumpkins carry HORIZONTAL_FACING
        // with their own encoding). Each one is reversed in applyFcMetadataToVanillaBlock.
        // Crops (wheat 59, carrots 141, potatoes 142): 1.5.2 growth level = meta & 7 (vanilla/server
        // BlockCrops.java:338-359, GetGrowthLevel / SetGrowthLevel) = AGE 0-7. Keyed on AGE_7 itself: the
        // modern-only BeetrootBlock / TorchflowerCropBlock extend CropBlock with other age properties.
        if (block instanceof net.minecraft.world.level.block.CropBlock
                && state.hasProperty(net.minecraft.world.level.block.CropBlock.AGE)) {
            return state.getValue(net.minecraft.world.level.block.CropBlock.AGE);
        }
        // Nether wart: 1.5.2 growth meta 0-3 (vanilla/server BlockNetherStalk.java:51-62) = AGE_3.
        if (block instanceof net.minecraft.world.level.block.NetherWartBlock) {
            return state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE);
        }
        // Cactus / sugar cane: 1.5.2 growth counter 0-15 before the next segment (vanilla/server
        // BlockCactus.java:43-53, BlockReed.java:44-53) = AGE_15.
        if (block instanceof net.minecraft.world.level.block.CactusBlock) {
            return state.getValue(net.minecraft.world.level.block.CactusBlock.AGE);
        }
        if (block instanceof net.minecraft.world.level.block.SugarCaneBlock) {
            return state.getValue(net.minecraft.world.level.block.SugarCaneBlock.AGE);
        }
        // Fire: 1.5.2 fire age 0-15 (vanilla/server BlockFire.java:162-166) = AGE_15. The face booleans are 1.20.1
        // rendering only; soul fire (no AGE) is not FC fire.
        if (block instanceof net.minecraft.world.level.block.FireBlock
                && state.hasProperty(net.minecraft.world.level.block.FireBlock.AGE)) {
            return state.getValue(net.minecraft.world.level.block.FireBlock.AGE);
        }
        // Cocoa: 1.5.2 meta = direction of the jungle log the pod hangs on (0=S, 1=W, 2=N, 3=E: Direction.offsetX /
        // offsetZ in canBlockStay, vanilla/server BlockCocoa.java:54-61) | age << 2 (0-2, :31-49). 1.20.1 FACING
        // also points at the log (CocoaBlock.canSurvive), and get2DDataValue numbers it the same way. The generic
        // HORIZONTAL_FACING branch answered 2-5, so FC's canBlockStay checked the wrong side and dropped the pod.
        if (block instanceof net.minecraft.world.level.block.CocoaBlock) {
            return state.getValue(net.minecraft.world.level.block.CocoaBlock.FACING).get2DDataValue()
                    | state.getValue(net.minecraft.world.level.block.CocoaBlock.AGE) << 2;
        }
        // Vines: 1.5.2 meta = the faces the vine covers, 1 = south, 2 = west, 4 = north, 8 = east (the bounds in
        // vanilla/server BlockVine.java:61-130); 0 = hanging from the block above only. 1.20.1 UP is that
        // ceiling attachment, which 1.5.2 never stored: a vine with side faces reads its sides only.
        if (block instanceof net.minecraft.world.level.block.VineBlock) {
            int meta = 0;
            if (state.getValue(net.minecraft.world.level.block.VineBlock.SOUTH)) meta |= 1;
            if (state.getValue(net.minecraft.world.level.block.VineBlock.WEST)) meta |= 2;
            if (state.getValue(net.minecraft.world.level.block.VineBlock.NORTH)) meta |= 4;
            if (state.getValue(net.minecraft.world.level.block.VineBlock.EAST)) meta |= 8;
            return meta;
        }
        // Stems: FC growth 0-14 and 15 = fruit attached (Server FCBlockStem.java:202-272, ValidateFruitState /
        // CheckForGrowth). An attached stem is 15 (the generic HORIZONTAL_FACING branch answered 2-5, so FC never
        // saw an attached stem); a StemBlock reads AGE * 2, its exact 0-14 value lives in the overlay.
        if (block instanceof net.minecraft.world.level.block.AttachedStemBlock) {
            return 15;
        }
        if (block instanceof net.minecraft.world.level.block.StemBlock) {
            return state.getValue(net.minecraft.world.level.block.StemBlock.AGE) * 2;
        }
        // Leaves: species (variant table) | 4 when player placed (1.5.2 ItemLeaves.getMetadata: damage | 4, the
        // no-decay bit FCBlockLeaves.updateTick checks, Server FCBlockLeaves.java:96) = PERSISTENT. It was written
        // on apply but never read back, so player-placed leaves read as natural to FC. The decay-check bit 8
        // (vanilla/server BlockLeaves.java:52-53) has no state and lives in the overlay; DISTANCE is not read.
        if (block instanceof net.minecraft.world.level.block.LeavesBlock
                && state.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)) {
            int species = Math.max(ProxyRegistry.getLegacyVariantMeta(block), 0);
            return state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT) ? species | 4 : species;
        }
        if (block instanceof net.minecraft.world.level.block.CarvedPumpkinBlock) {
            return state.getValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING).get2DDataValue();
        }

        if (block instanceof net.minecraft.world.level.block.LeverBlock
                || block instanceof net.minecraft.world.level.block.ButtonBlock) {
            return faceAttachedToLegacyMeta(state);
        }

        if (block instanceof net.minecraft.world.level.block.TripWireHookBlock) {
            int meta = state.getValue(net.minecraft.world.level.block.TripWireHookBlock.FACING).get2DDataValue();
            if (state.getValue(net.minecraft.world.level.block.TripWireHookBlock.ATTACHED)) meta |= 4;
            if (state.getValue(net.minecraft.world.level.block.TripWireHookBlock.POWERED)) meta |= 8;
            return meta;
        }

        // Wall torches: FC uses 1=east, 2=west, 3=south, 4=north, 5=floor
        if (block instanceof net.minecraft.world.level.block.WallTorchBlock
                || block instanceof net.minecraft.world.level.block.RedstoneWallTorchBlock) {
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                net.minecraft.core.Direction dir = state.getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                switch (dir) {
                    case EAST: return 1;
                    case WEST: return 2;
                    case SOUTH: return 3;
                    case NORTH: return 4;
                    default: return 5;
                }
            }
            return 5; // floor
        }
        // Floor torches
        if (block instanceof net.minecraft.world.level.block.TorchBlock) {
            return 5; // floor torch
        }

        // Blocks with FACING property (furnace, dispenser, dropper, piston, chest, etc.)
        // MC 1.5.2 metadata: 0=down, 1=up, 2=north, 3=south, 4=west, 5=east
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            net.minecraft.core.Direction dir = state.getValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            int meta = directionToLegacyFacing(dir);
            // Piston: bit 3 = extended
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED)) {
                if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED)) {
                    meta |= 8;
                }
            }
            // Dispenser/dropper: bit 3 = triggered
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.TRIGGERED)) {
                if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.TRIGGERED)) {
                    meta |= 8;
                }
            }
            return meta;
        }

        // Blocks with HORIZONTAL_FACING (furnace, chest, ladder, wall signs, etc.)
        // MC 1.5.2: 2=north, 3=south, 4=west, 5=east
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            net.minecraft.core.Direction dir = state.getValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            int meta = directionToLegacyFacing(dir);
            // Furnace: bit 3 = lit (for burning furnace)
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                // Lit furnace was a separate block in 1.5.2, not a metadata flag
                // but some FC code checks it
            }
            return meta;
        }

        // Lever/button: powered state
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
            int meta = 0;
            if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
                meta |= 8;
            }
            return meta;
        }

        // Flattened subtypes: wool colour, plank/sapling/leaf species, sandstone / stone-brick / infested /
        // wall type, tall grass vs fern (ProxyRegistry variant table, the 1.5.2 subtype orders). A sapling reads
        // its species only: FC's growth stage (meta bits 2-3, Server FCBlockSapling.java:40-64) does not fit STAGE
        // (0-1), lives in the overlay, and STAGE is left as 1.20.1 has it.
        int variant = ProxyRegistry.getLegacyVariantMeta(block);
        if (variant >= 0) return variant;

        // Everything else (unmapped water-holders already answered 0 above, triage D5).
        return 0;
    }

    /** Converts MC 1.20.1 Direction to MC 1.5.2 facing metadata value. */
    private static int directionToLegacyFacing(net.minecraft.core.Direction dir) {
        switch (dir) {
            case DOWN: return 0;
            case UP: return 1;
            case NORTH: return 2;
            case SOUTH: return 3;
            case WEST: return 4;
            case EAST: return 5;
            default: return 0;
        }
    }

    /** Converts MC 1.5.2 facing metadata to MC 1.20.1 Direction. */
    private static net.minecraft.core.Direction legacyFacingToDirection(int facing) {
        switch (facing) {
            case 0: return net.minecraft.core.Direction.DOWN;
            case 1: return net.minecraft.core.Direction.UP;
            case 2: return net.minecraft.core.Direction.NORTH;
            case 3: return net.minecraft.core.Direction.SOUTH;
            case 4: return net.minecraft.core.Direction.WEST;
            case 5: return net.minecraft.core.Direction.EAST;
            default: return net.minecraft.core.Direction.NORTH;
        }
    }

    /**
     * Applies FC metadata to a vanilla MC block state (reverse of deriveVanillaMetadata).
     * Translates FC's integer metadata into the appropriate MC 1.20.1 BlockState properties.
     * {@code pos} is where the state goes: fire faces, vine ceiling attachment and attached stems depend on
     * the neighbours. May answer a state of another block (StemBlock <-> AttachedStemBlock).
     */
    private BlockState applyFcMetadataToVanillaBlock(BlockState current, int metadata, BlockPos pos) {
        net.minecraft.world.level.block.Block block = current.getBlock();

        // Plant and fire growth counters: reverses of the deriveVanillaMetadata branches. All of them precede the
        // generic HORIZONTAL_FACING / FACING branches below (cocoa and stems carry HORIZONTAL_FACING). Values
        // outside a property's range are clamped: setValue throws on them.
        if (block instanceof net.minecraft.world.level.block.CropBlock
                && current.hasProperty(net.minecraft.world.level.block.CropBlock.AGE)) {
            return current.setValue(net.minecraft.world.level.block.CropBlock.AGE, metadata & 7);
        }
        if (block instanceof net.minecraft.world.level.block.NetherWartBlock) {
            return current.setValue(net.minecraft.world.level.block.NetherWartBlock.AGE, clampMeta(metadata, 3));
        }
        if (block instanceof net.minecraft.world.level.block.CactusBlock) {
            return current.setValue(net.minecraft.world.level.block.CactusBlock.AGE, metadata & 15);
        }
        if (block instanceof net.minecraft.world.level.block.SugarCaneBlock) {
            return current.setValue(net.minecraft.world.level.block.SugarCaneBlock.AGE, metadata & 15);
        }
        // Fire: the faces 1.20.1 would give a fire placed here (BaseFireBlock.getState -> FireBlock
        // getStateForPlacement, as FireBlock.updateShape recomputes them), with FC's age. Where that would be soul
        // fire (soul sand / soil below), the FC fire stays FIRE: 1.5.2 has no soul fire, and FC owns FIRE's ticks.
        if (block instanceof net.minecraft.world.level.block.FireBlock
                && current.hasProperty(net.minecraft.world.level.block.FireBlock.AGE)) {
            BlockState placed = net.minecraft.world.level.block.BaseFireBlock.getState(level, pos);
            BlockState base = placed.is(block) ? placed : current;
            return base.setValue(net.minecraft.world.level.block.FireBlock.AGE, metadata & 15);
        }
        // Farmland: MOISTURE = 1.5.2 hydration 0-7 (FCBlockFarmlandLegacyBase SetFullyHydrated / DryIncrementally,
        // Common FCBlockFarmlandLegacyBase.java:62-74). Derive existed; FC's writes used to reach only the overlay.
        if (block instanceof net.minecraft.world.level.block.FarmBlock) {
            return current.setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, metadata & 7);
        }
        // Cocoa: direction of the log (0=S, 1=W, 2=N, 3=E) | age << 2, see deriveVanillaMetadata. The generic
        // HORIZONTAL_FACING branch read meta & 7 as a 3D facing (2-5).
        if (block instanceof net.minecraft.world.level.block.CocoaBlock) {
            return current
                    .setValue(net.minecraft.world.level.block.CocoaBlock.FACING,
                            net.minecraft.core.Direction.from2DDataValue(metadata & 3))
                    .setValue(net.minecraft.world.level.block.CocoaBlock.AGE, clampMeta((metadata >> 2) & 3, 2));
        }
        // Vines: side bits 1 = south, 2 = west, 4 = north, 8 = east; UP when the vine only hangs from the block above
        // (meta 0) or the block above holds it as 1.20.1 attaches vines (VineBlock.isAcceptableNeighbour, the rule
        // VineBlock.updateShape keeps UP by). 1.5.2 drew the top face whenever that block was there.
        if (block instanceof net.minecraft.world.level.block.VineBlock) {
            boolean up = (metadata & 15) == 0 || net.minecraft.world.level.block.VineBlock.isAcceptableNeighbour(
                    level, pos.above(), net.minecraft.core.Direction.DOWN);
            return current
                    .setValue(net.minecraft.world.level.block.VineBlock.SOUTH, (metadata & 1) != 0)
                    .setValue(net.minecraft.world.level.block.VineBlock.WEST, (metadata & 2) != 0)
                    .setValue(net.minecraft.world.level.block.VineBlock.NORTH, (metadata & 4) != 0)
                    .setValue(net.minecraft.world.level.block.VineBlock.EAST, (metadata & 8) != 0)
                    .setValue(net.minecraft.world.level.block.VineBlock.UP, up);
        }
        // Stems: growth 0-14 -> StemBlock AGE = growth / 2 (the exact value stays in the overlay); 15 -> the attached
        // stem facing the fruit FC attached to it, or AGE 7 when there is none (FC's ValidateFruitState then resets
        // the stem itself, Server FCBlockStem.java:202-212).
        if (block instanceof net.minecraft.world.level.block.StemBlock
                || block instanceof net.minecraft.world.level.block.AttachedStemBlock) {
            return stemStateFor(ProxyRegistry.getBlockId(block), metadata, pos);
        }

        // Carved pumpkins / jack o'lanterns: face direction 0=S, 1=W, 2=N, 3=E (bit 8, the jack o'lantern's
        // player-placed flag, stays in the overlay). The generic HORIZONTAL_FACING branch read it as a 3D facing.
        if (block instanceof net.minecraft.world.level.block.CarvedPumpkinBlock) {
            return current.setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING,
                    net.minecraft.core.Direction.from2DDataValue(metadata & 3));
        }

        // Logs: metadata bits 2-3 encode axis orientation
        if (block instanceof net.minecraft.world.level.block.RotatedPillarBlock
                && current.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) {
            int axisBits = (metadata >> 2) & 3;
            net.minecraft.core.Direction.Axis axis;
            switch (axisBits) {
                case 1: axis = net.minecraft.core.Direction.Axis.X; break;   // east-west
                case 2: axis = net.minecraft.core.Direction.Axis.Z; break;   // north-south
                default: axis = net.minecraft.core.Direction.Axis.Y; break;  // vertical
            }
            return current.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, axis);
        }

        // Leaves: 1.5.2 bit 4 = no decay (player placed: ItemLeaves.getMetadata returns damage | 4, vanilla/client
        // ItemLeaves.java:29); the species bits picked the variant already (getModernBlockVariant(18, mask 3)).
        if (block instanceof net.minecraft.world.level.block.LeavesBlock
                && current.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)) {
            return current.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, (metadata & 4) != 0);
        }

        // Fence gates: reverse of deriveVanillaMetadata's 1.5.2 gate encoding (direction 0-3 | 4 = open)
        if (block instanceof net.minecraft.world.level.block.FenceGateBlock) {
            return current
                    .setValue(net.minecraft.world.level.block.FenceGateBlock.FACING,
                            net.minecraft.core.Direction.from2DDataValue(metadata & 3))
                    .setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, (metadata & 4) != 0);
        }

        // Slabs: bit 8 = upper half (a double slab stays double; resolveVanillaBlockState makes 43/125 double)
        if (block instanceof net.minecraft.world.level.block.SlabBlock
                && current.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
            if (current.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)
                    == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE) {
                return current;
            }
            return current.setValue(net.minecraft.world.level.block.SlabBlock.TYPE, (metadata & 8) != 0
                    ? net.minecraft.world.level.block.state.properties.SlabType.TOP
                    : net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
        }

        // Stairs: reverse of deriveVanillaMetadata's 1.5.2 stairs encoding (0=E, 1=W, 2=S, 3=N | 4 = upside-down).
        // Ahead of the generic HORIZONTAL_FACING branch, which skipped meta 0/1 (DOWN/UP: not horizontal), so FC
        // writes of east/west bottom stairs were dropped.
        if (block instanceof net.minecraft.world.level.block.StairBlock
                && current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                && current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)) {
            net.minecraft.core.Direction stairDir;
            switch (metadata & 3) {
                case 0: stairDir = net.minecraft.core.Direction.EAST; break;
                case 1: stairDir = net.minecraft.core.Direction.WEST; break;
                case 2: stairDir = net.minecraft.core.Direction.SOUTH; break;
                default: stairDir = net.minecraft.core.Direction.NORTH; break;
            }
            return current
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                            stairDir)
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF,
                            (metadata & 4) != 0
                                    ? net.minecraft.world.level.block.state.properties.Half.TOP
                                    : net.minecraft.world.level.block.state.properties.Half.BOTTOM);
        }

        // Trapdoors: reverse of deriveVanillaMetadata's 1.5.2 trapdoor encoding (0=N, 1=S, 2=W, 3=E | 4 = open
        // | 8 = upper half). Used to fall into the stairs mapping above (shared HORIZONTAL_FACING + HALF).
        if (block instanceof net.minecraft.world.level.block.TrapDoorBlock
                && current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                && current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
                && current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)) {
            net.minecraft.core.Direction trapDir;
            switch (metadata & 3) {
                case 1: trapDir = net.minecraft.core.Direction.SOUTH; break;
                case 2: trapDir = net.minecraft.core.Direction.WEST; break;
                case 3: trapDir = net.minecraft.core.Direction.EAST; break;
                default: trapDir = net.minecraft.core.Direction.NORTH; break;
            }
            return current
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                            trapDir)
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN,
                            (metadata & 4) != 0)
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF,
                            (metadata & 8) != 0
                                    ? net.minecraft.world.level.block.state.properties.Half.TOP
                                    : net.minecraft.world.level.block.state.properties.Half.BOTTOM);
        }

        if (block instanceof net.minecraft.world.level.block.LeverBlock
                || block instanceof net.minecraft.world.level.block.ButtonBlock) {
            return legacyMetaToFaceAttached(current, metadata);
        }

        if (block instanceof net.minecraft.world.level.block.TripWireHookBlock) {
            return current
                    .setValue(net.minecraft.world.level.block.TripWireHookBlock.FACING,
                            net.minecraft.core.Direction.from2DDataValue(metadata & 3))
                    .setValue(net.minecraft.world.level.block.TripWireHookBlock.ATTACHED, (metadata & 4) != 0)
                    .setValue(net.minecraft.world.level.block.TripWireHookBlock.POWERED, (metadata & 8) != 0);
        }

        // Blocks with HORIZONTAL_FACING (furnace, chest, ladder, etc.)
        // FC metadata: 2=north, 3=south, 4=west, 5=east
        if (current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            int facingBits = metadata & 7; // lower 3 bits for facing
            net.minecraft.core.Direction dir = legacyFacingToDirection(facingBits);
            // HORIZONTAL_FACING only accepts horizontal directions
            if (dir.getAxis().isHorizontal()) {
                return current.setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, dir);
            }
        }

        // Blocks with full FACING (dispenser, dropper, piston, observer, etc.)
        if (current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            int facingBits = metadata & 7;
            net.minecraft.core.Direction dir = legacyFacingToDirection(facingBits);
            BlockState result = current.setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, dir);
            if (current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED)) {
                result = result.setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED,
                        (metadata & 8) != 0);
            }
            if (current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.TRIGGERED)) {
                result = result.setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.TRIGGERED,
                        (metadata & 8) != 0);
            }
            return result;
        }

        // Torches on walls: FC metadata 1-4 maps to facing
        // FC: 1=east(+x), 2=west(-x), 3=south(+z), 4=north(-z), 5=floor
        if (block instanceof net.minecraft.world.level.block.WallTorchBlock
                && current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            net.minecraft.core.Direction dir;
            switch (metadata) {
                case 1: dir = net.minecraft.core.Direction.EAST; break;
                case 2: dir = net.minecraft.core.Direction.WEST; break;
                case 3: dir = net.minecraft.core.Direction.SOUTH; break;
                case 4: dir = net.minecraft.core.Direction.NORTH; break;
                default: return current; // floor torch or unknown
            }
            return current.setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, dir);
        }

        // No translation available — return unchanged
        return current;
    }

    /** {@code metadata} limited to a property's range 0..max (BlockState.setValue throws outside it). */
    private static int clampMeta(int metadata, int max) {
        return Math.max(0, Math.min(max, metadata));
    }

    /**
     * 1.20.1 state of the pumpkin (104) / melon (105) stem with FC metadata {@code metadata} at {@code pos}: growth
     * 0-14 -> StemBlock AGE min(7, growth / 2) (1.20.1's 8 stages; the exact growth stays in the overlay); 15 ->
     * ATTACHED_*_STEM facing the fruit attached to it, else StemBlock AGE 7 (the overlay keeps the 15).
     */
    private BlockState stemStateFor(int stemId, int metadata, BlockPos pos) {
        boolean melon = stemId == 105;
        net.minecraft.world.level.block.Block stem = melon ? Blocks.MELON_STEM : Blocks.PUMPKIN_STEM;
        if (metadata < 15) {
            return stem.defaultBlockState().setValue(net.minecraft.world.level.block.StemBlock.AGE,
                    Math.min(7, Math.max(0, metadata) / 2));
        }
        net.minecraft.core.Direction toFruit = attachedFruitDirection(stemId, pos);
        if (toFruit != null) {
            return (melon ? Blocks.ATTACHED_MELON_STEM : Blocks.ATTACHED_PUMPKIN_STEM).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.AttachedStemBlock.FACING, toFruit);
        }
        return stem.defaultBlockState().setValue(net.minecraft.world.level.block.StemBlock.AGE, 7);
    }

    /**
     * Side of the stem at {@code pos} whose fruit is attached to it, by FC's own test: FCBlockStem
     * GetConnectedFruitDirection (Server FCBlockStem.java:182-200) scans facings 2-5 for the stem's fruit id whose
     * IsBlockAttachedToFacing names the stem (FCBlockGourd keeps that facing in its metadata, Server
     * FCBlockGourd.java:134-148). The fruit is the FC fresh pumpkin 1006 (FCBetterThanWolves.java:1115-1116) or the
     * melon 103; the live blocksList entry answers, not the stem's fruitType object. Null if none.
     */
    private net.minecraft.core.Direction attachedFruitDirection(int stemId, BlockPos pos) {
        try {
            btw.modern.Block[] blocks = btw.modern.Block.blocksList;
            btw.modern.Block fcStem = stemId > 0 && stemId < blocks.length ? blocks[stemId] : null;
            if (!(fcStem instanceof btw.modern.BlockStem stemBlock) || stemBlock.fruitType == null) return null;
            int fruitId = stemBlock.fruitType.blockID;
            btw.modern.Block fruit = fruitId > 0 && fruitId < blocks.length ? blocks[fruitId] : null;
            if (fruit == null) return null;
            for (int facing = 2; facing <= 5; facing++) {
                net.minecraft.core.Direction dir = legacyFacingToDirection(facing);
                BlockPos fruitPos = pos.relative(dir);
                if (getBlockId(fruitPos.getX(), fruitPos.getY(), fruitPos.getZ()) == fruitId
                        && fruit.IsBlockAttachedToFacing(this, fruitPos.getX(), fruitPos.getY(), fruitPos.getZ(),
                                directionToLegacyFacing(dir.getOpposite()))) {
                    return dir;
                }
            }
        } catch (Throwable t) {
            warnOncePerThrowSite("STEM-FRUIT", "finding the fruit of the stem at " + pos, t);
        }
        return null;
    }

    /**
     * Resolves an FC block ID + metadata into the correct MC 1.20.1 BlockState.
     * Handles split blocks (e.g., floor torch vs wall torch) and applies facing/axis.
     */
    private BlockState resolveVanillaBlockState(int blockID, int metadata, BlockPos pos) {
        // Torches: FC metadata 1-4 = wall, 5 = floor
        // ID 50 = torch, ID 75 = redstone torch off, ID 76 = redstone torch on
        if (blockID == 50) {
            if (metadata >= 1 && metadata <= 4) {
                BlockState state = Blocks.WALL_TORCH.defaultBlockState();
                return applyTorchFacing(state, metadata);
            }
            return Blocks.TORCH.defaultBlockState(); // floor
        }
        if (blockID == 75) {
            // Redstone torch off — MC 1.20.1 doesn't have a separate block for off state
            // Use the normal redstone torch (it manages its own state)
            if (metadata >= 1 && metadata <= 4) {
                BlockState state = Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false);
                return applyTorchFacing(state, metadata);
            }
            return Blocks.REDSTONE_TORCH.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false);
        }
        if (blockID == 76) {
            if (metadata >= 1 && metadata <= 4) {
                BlockState state = Blocks.REDSTONE_WALL_TORCH.defaultBlockState();
                return applyTorchFacing(state, metadata);
            }
            return Blocks.REDSTONE_TORCH.defaultBlockState();
        }

        // Huge mushroom caps 99/100 resolve to the cap block for every metadata, the trunk's 10 / 15 included: FC writes
        // them only to have FCBlockMushroomCapLegacy.onBlockAdded turn them into its flammable caps 1078/1079 with the
        // same metadata (Common FCBlockMushroomCapLegacy.java:31-47, through BlockBehaviorMixin.btw$onPlace), as 4.B's
        // WorldGenBigMushroom trunks became FCBlockMushroomCap meta 10 (vanilla/server WorldGenBigMushroom.java:211). A
        // MUSHROOM_STEM here has no legacy id, so the conversion never ran and FC read the trunk as a stand-in (not
        // flammable, no FC drops, mobs spawn on it).

        // For other vanilla blocks, get the modern block and apply metadata. The subtype bits pick the
        // flattened variant first (spruce log 17:1, orange wool 35:1, cut sandstone 24:2, cobble slab 44:3),
        // else the id's canonical block; 43/125 are the double forms of the 44/126 slabs.
        net.minecraft.world.level.block.Block mcBlock = ProxyRegistry.getModernBlockVariant(blockID, metadata);
        if (mcBlock == null) {
            mcBlock = ProxyRegistry.getModernBlock(blockID);
        }
        if (mcBlock != null) {
            BlockState state = applyFcMetadataToVanillaBlock(mcBlock.defaultBlockState(), metadata, pos);
            if ((blockID == 43 || blockID == 125)
                    && state.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
                state = state.setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
            }
            return state;
        }
        return null;
    }

    /** Applies FC torch metadata (1=east, 2=west, 3=south, 4=north) to a wall torch BlockState. */
    private BlockState applyTorchFacing(BlockState state, int metadata) {
        if (!state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state;
        }
        net.minecraft.core.Direction dir;
        switch (metadata) {
            case 1: dir = net.minecraft.core.Direction.EAST; break;
            case 2: dir = net.minecraft.core.Direction.WEST; break;
            case 3: dir = net.minecraft.core.Direction.SOUTH; break;
            case 4: dir = net.minecraft.core.Direction.NORTH; break;
            default: return state;
        }
        return state.setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, dir);
    }


    public Material getBlockMaterial(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id > 0 && id < btw.modern.Block.blocksList.length) {
            btw.modern.Block fcBlock = btw.modern.Block.blocksList[id];
            // A waterlogged twin cell (a planted mangrove propagule under water) is water to FC, as every other
            // water-holding modern plant cell is (triage D5); its id is the twin's so the twin's tick finds it.
            if (btw.modern.ModernTwins.isTwin(id)
                    && level.getFluidState(new BlockPos(x, y, z)).is(net.minecraft.tags.FluidTags.WATER)) {
                return Material.water;
            }
            if (fcBlock != null) {
                return fcBlock.blockMaterial;
            }
        }
        return Material.air;
    }

    
    public boolean isAirBlock(int x, int y, int z) {
        return level.isEmptyBlock(new BlockPos(x, y, z));
    }

    
    public boolean isBlockNormalCube(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        return btw.modern.Block.isNormalCube(id);
    }

    
    public TileEntity getBlockTileEntity(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;

        // If this is a ProxyBlockEntity, return the FC tile entity directly
        // so that FC code can cast it to the expected subclass (e.g.
        // FCTileEntityCampfire, FCTileEntityHopper).
        if (be instanceof ProxyBlockEntity proxy) {
            return proxy.getFcTileEntity();
        }

        // For vanilla BlockEntities, wrap via TileEntityBridge
        return TileEntityBridge.getOrCreate(be, this);
    }

    // ================================================================
    // Block modification
    // ================================================================

    
    public boolean setBlock(int x, int y, int z, int blockID, int metadata, int flags) {
        BlockPos pos = new BlockPos(x, y, z);
        clearMetaOverride(pos); // clear metadata override on block change

        // Block ID 0 = air — handle directly since air has no proxy/mapping
        if (blockID == 0) {
            return level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
        }

        // Preserve tile entity data across block changes when FC says to keep it.
        // (e.g., campfire fire level changes: block type changes but tile entity stays)
        net.minecraft.nbt.CompoundTag savedTileData = null;
        int oldBlockId = getBlockId(x, y, z);
        if (oldBlockId > 0 && oldBlockId != blockID) {
            btw.modern.Block oldFcBlock = btw.modern.Block.blocksList[oldBlockId];
            if (oldFcBlock != null && !oldFcBlock.ShouldDeleteTileEntityOnBlockChange(blockID)) {
                BlockEntity oldBe = level.getBlockEntity(pos);
                if (oldBe instanceof ProxyBlockEntity oldProxy) {
                    btw.modern.TileEntity fcTe = oldProxy.getFcTileEntity();
                    if (fcTe != null) {
                        savedTileData = new net.minecraft.nbt.CompoundTag();
                        ForgeNBTCompound wrapper = new ForgeNBTCompound(savedTileData);
                        try { fcTe.writeToNBT(wrapper); } catch (Exception ignored) {}
                    }
                }
            }
        }

        ProxyBlock proxy = ProxyRegistry.getProxy(blockID);
        if (proxy != null) {
            BlockState state = proxy.defaultBlockState()
                    .setValue(ProxyBlock.META, Math.min(Math.max(metadata, 0), 15));
            boolean result = level.setBlock(pos, state, flags);

            // Restore saved tile entity data
            if (result && savedTileData != null && !savedTileData.isEmpty()) {
                BlockEntity newBe = level.getBlockEntity(pos);
                if (newBe instanceof ProxyBlockEntity newProxy) {
                    btw.modern.TileEntity fcTe = newProxy.getFcTileEntity();
                    if (fcTe != null) {
                        ForgeNBTCompound wrapper = new ForgeNBTCompound(savedTileData);
                        try { fcTe.readFromNBT(wrapper); } catch (Exception ignored) {}
                        fcTe.xCoord = x;
                        fcTe.yCoord = y;
                        fcTe.zCoord = z;
                    }
                }
            }
            return result;
        }
        // For vanilla block IDs without a ProxyBlock, try mapping to the MC block
        // and apply FC metadata → MC block state translation
        BlockState vanillaState = resolveVanillaBlockState(blockID, metadata, pos);
        if (vanillaState != null) {
            putMetaOverride(pos, vanillaState, metadata);
            // Use flags WITHOUT block-update bit for blocks that attach to faces
            // (torches, levers, etc.) — FC manages its own update chain and MC's
            // neighborChanged would break them via canSurvive checks during turntable
            // rotation before they're fully positioned.
            net.minecraft.world.level.block.Block vanillaBlock = vanillaState.getBlock();
            int placeFlags = flags;
            if (vanillaBlock instanceof net.minecraft.world.level.block.WallTorchBlock
                    || vanillaBlock instanceof net.minecraft.world.level.block.RedstoneWallTorchBlock) {
                placeFlags = flags & ~1; // strip BLOCK_UPDATE, keep SEND_TO_CLIENTS
            }
            boolean placed = level.setBlock(pos, vanillaState, placeFlags);
            if (placed) {
                scheduleLeafDistanceUpdate(pos, vanillaState);
            }
            return placed;
        }
        return false;
    }

    /**
     * Leaves FC places (FC tree generators, FCUtilsTrees / FCUtilsGenBigTree) come out of resolveVanillaBlockState
     * with the default DISTANCE 7, and 1.20.1 only recomputes DISTANCE in LeavesBlock.tick, which its updateShape
     * schedules for the neighbours a write touches, not for the placed leaf itself (LeavesBlock.java:58-77), so an
     * FC canopy kept DISTANCE 7. One 1-tick LeavesBlock tick lets 1.20.1 compute it, as getStateForPlacement and
     * its tree features do for the leaves 1.20.1 places. That tick stays 1.20.1's (TickPolicy: LeavesBlock
     * overrides tick).
     */
    private void scheduleLeafDistanceUpdate(BlockPos pos, BlockState placed) {
        if (placed.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) {
            level.scheduleTick(pos, placed.getBlock(), 1);
        }
    }

    
    public boolean setBlockToAir(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        clearMetaOverride(pos);
        return level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }


    /**
     * The cell setBlockMetadata is switching to another 1.20.1 block for an FC metadata-only write. 1.5.2
     * Chunk.setBlockMetadata never calls breakBlock / onBlockAdded (only setBlockIDWithMetadata does), so
     * BlockBehaviorMixin's onRemove / onPlace hooks skip FC for this cell. Scoped to the cell: neighbour updates
     * fired inside the same setBlock still reach FC normally.
     */
    private static final ThreadLocal<BlockPos> METADATA_ONLY_WRITE = new ThreadLocal<>();

    public static boolean isMetadataOnlyWrite(BlockPos pos) {
        BlockPos writing = METADATA_ONLY_WRITE.get();
        return writing != null && writing.equals(pos);
    }

    public boolean setBlockMetadata(int x, int y, int z, int metadata, int flags) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState current = level.getBlockState(pos);
        if (current.hasProperty(ProxyBlock.META)) {
            BlockState newState = current.setValue(ProxyBlock.META, Math.min(Math.max(metadata, 0), 15));
            clearMetaOverride(pos); // clear any override
            return level.setBlock(pos, newState, flags);
        }
        if (TwinRegistry.isTwinState(current)) {
            return setTwinMetadata(pos, current, metadata, flags);
        }
        // Vanilla block without META — translate FC metadata to MC block state
        int iOldMetadata = getBlockMetadata(x, y, z);   // as FC saw it, before the override below
        // A flattened subtype id switches block (1.5.2 metadata IS the subtype); else the state takes the bits
        BlockState newState = variantStateFor(current, pos, metadata);
        if (newState == null) {
            newState = applyFcMetadataToVanillaBlock(current, metadata, pos);
        }
        putMetaOverride(pos, newState, metadata);
        if (newState != current) {
            if (newState.getBlock() == current.getBlock()) {
                return level.setBlock(pos, newState, flags);
            }
            // The 1.20.1 block changes (flattened subtype, stem <-> attached stem) but FC only wrote metadata. The
            // previous value is restored, not removed, should such a write ever nest inside another one.
            BlockPos outer = METADATA_ONLY_WRITE.get();
            METADATA_ONLY_WRITE.set(pos);
            try {
                return level.setBlock(pos, newState, flags);
            } finally {
                if (outer != null) {
                    METADATA_ONLY_WRITE.set(outer);
                } else {
                    METADATA_ONLY_WRITE.remove();
                }
            }
        }
        // Same MC state, only FC's stored metadata moved: the rest of 1.5.2 World.setBlockMetadata
        // (vanilla/server World.java:558-584), flag by flag -- nothing if the metadata did not change
        // (Chunk.setBlockMetadata returns false), else bit 2 -> markBlockForUpdate (client resync), bit 1 ->
        // notifyBlockChange (neighbour update). It used to resend the block for any flags and never notify.
        if (iOldMetadata == metadata) {
            return false;
        }
        if ((flags & 2) != 0) {
            markBlockForUpdate(x, y, z);
        }
        if ((flags & 1) != 0) {
            notifyBlockChange(x, y, z, getBlockId(x, y, z));
        }
        return true;
    }

    
    /**
     * An FC metadata write at a twin cell (TwinRegistry: FC's sapling stage, a crop's growth level). The 1.20.1 state
     * takes what it can show ({@link TwinRegistry#stateForMeta}) and the overlay keeps the rest, as for every block:
     * <ul>
     *   <li>saplings, azaleas, fungi: the state stays, the stage lives in the overlay;</li>
     *   <li>beetroots, torchflower crop, pitcher crop: AGE moves with FC's level;</li>
     *   <li>the torchflower crop's level 7 is the TORCHFLOWER block: a block switch FC sees as a metadata write, so
     *       BlockBehaviorMixin's remove / place hooks stay out of it (METADATA_ONLY_WRITE) and no overlay entry is left
     *       for the flower;</li>
     *   <li>the pitcher crop's AGE 3 and 4 have an upper half, placed as PitcherCropBlock.grow places it (lower half,
     *       then the upper half above it). Without room above (canGrowInto: air or pitcher) the write is refused and
     *       FC's level stays, as 1.20.1's own growth would not happen either.</li>
     * </ul>
     * Unchanged state: the rest of 1.5.2 World.setBlockMetadata, as in {@link #setBlockMetadata}.
     */
    private boolean setTwinMetadata(BlockPos pos, BlockState current, int metadata, int flags) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int iOldMetadata = getBlockMetadata(x, y, z);
        BlockState target = TwinRegistry.stateForMeta(current, metadata);
        if (target.getBlock() != current.getBlock()) {
            clearMetaOverride(pos);
            BlockPos outer = METADATA_ONLY_WRITE.get();
            METADATA_ONLY_WRITE.set(pos);
            try {
                return level.setBlock(pos, target, flags);
            } finally {
                if (outer != null) {
                    METADATA_ONLY_WRITE.set(outer);
                } else {
                    METADATA_ONLY_WRITE.remove();
                }
            }
        }
        if (target != current && TwinRegistry.needsUpperHalf(target)) {
            if (!TwinRegistry.canGrowUpperHalf(level, pos)) {
                return false;
            }
            putMetaOverride(pos, target, metadata);
            boolean placed = level.setBlock(pos, target, 2);
            BlockPos above = pos.above();
            level.setBlock(above, net.minecraft.world.level.block.DoublePlantBlock.copyWaterloggedFrom(
                    level, above, TwinRegistry.upperHalfFor(target)), 3);
            return placed;
        }
        putMetaOverride(pos, target, metadata);
        if (target != current) {
            return level.setBlock(pos, target, flags);
        }
        if (iOldMetadata == metadata) {
            return false;
        }
        if ((flags & 2) != 0) {
            markBlockForUpdate(x, y, z);
        }
        if ((flags & 1) != 0) {
            notifyBlockChange(x, y, z, getBlockId(x, y, z));
        }
        return true;
    }

    /**
     * The state an FC metadata write on a flattened-subtype id switches to (ProxyRegistry.isVariantId: wool,
     * planks, logs, leaves, saplings, sandstone, slabs, infested blocks, stone bricks, walls, tall grass), or null
     * when the subtype bits still pick the block that is there. In 1.5.2 the subtype IS the metadata, so
     * FCBlockStoneBrick.updateTick's setBlockMetadataWithNotify(1 / 2) makes the bricks mossy / cracked (Server
     * FCBlockStoneBrick.java:42-74); the block used to stay STONE_BRICKS with the metadata in the overlay only.
     * The new block takes over the 1.20.1-only properties both blocks share (WATERLOGGED, wall connections,
     * leaf DISTANCE), then FC's metadata is applied to it as resolveVanillaBlockState would (slab half, log axis,
     * leaf PERSISTENT); a double slab stays double. Keyed on the id FC reads (43 / 125 for double slabs). Only a block
     * that is itself one of the table's variants switches: a 1.20.1 block that merely reads as a variant id (smooth
     * sandstone 24, stone slab 44, the mossy / cracked / chiseled infested bricks 97, stripped logs and wood 17) keeps
     * its identity, and FC's bits are applied to it in place.
     */
    private BlockState variantStateFor(BlockState current, BlockPos pos, int metadata) {
        if (ProxyRegistry.getLegacyVariantMeta(current.getBlock()) < 0) return null;
        int legacyId = ProxyRegistry.getWorldBlockId(current, level, pos);
        if (!ProxyRegistry.isVariantId(legacyId)) return null;
        net.minecraft.world.level.block.Block variant = ProxyRegistry.getModernBlockVariant(legacyId, metadata);
        if (variant == null || variant == current.getBlock()) return null;
        BlockState target = variant.defaultBlockState();
        for (net.minecraft.world.level.block.state.properties.Property<?> property : current.getProperties()) {
            if (target.hasProperty(property)) {
                target = copyProperty(current, target, property);
            }
        }
        return applyFcMetadataToVanillaBlock(target, metadata, pos);
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState from, BlockState to, net.minecraft.world.level.block.state.properties.Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    public boolean destroyBlock(int x, int y, int z, boolean dropItems) {
        return level.destroyBlock(new BlockPos(x, y, z), dropItems);
    }

    // ================================================================
    // Tile entity management
    // ================================================================

    
    public void setBlockTileEntity(int x, int y, int z, TileEntity tileEntity) {
        BlockPos pos = new BlockPos(x, y, z);
        if (tileEntity instanceof TileEntityBridge bridge) {
            // Re-use the real BlockEntity wrapped by the bridge (null once that BlockEntity
            // has been collected -- the bridge only holds it weakly)
            BlockEntity wrapped = bridge.getBlockEntity();
            if (wrapped != null) {
                level.setBlockEntity(wrapped);
            }
        } else if (tileEntity != null) {
            // FC code created a pure FC TileEntity. Check if a
            // ProxyBlockEntity already exists at this position and
            // inject the FC tile entity into it.
            BlockEntity existing = level.getBlockEntity(pos);
            if (existing instanceof ProxyBlockEntity proxy) {
                proxy.setFcTileEntity(tileEntity);
            } else {
                // No ProxyBlockEntity exists yet. If the block at this
                // position is a ProxyBlock, create a new ProxyBlockEntity.
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof ProxyBlock pb && pb.hasFcTileEntity()) {
                    ProxyBlockEntity newProxy = new ProxyBlockEntity(
                            pos, state, tileEntity, pb.getLegacyId());
                    level.setBlockEntity(newProxy);
                } else {
                    LOGGER.debug("setBlockTileEntity: cannot place pure FC TileEntity {} at {}",
                            tileEntity.getClass().getSimpleName(), pos);
                }
            }
        }
    }

    
    public void removeBlockTileEntity(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            if (!(be instanceof ProxyBlockEntity)) {
                TileEntityBridge.uncache(be);
            }
            level.removeBlockEntity(pos);
        }
    }

    // ================================================================
    // Notifications
    // ================================================================

    // 1.5.2 World.notifyBlockChange IS a neighbour update (vanilla/server World.java:649-652 ->
    // notifyBlocksOfNeighborChange; setBlock flag 1 is implemented with it, :572-580). It used to be mapped by
    // name to sendBlockUpdated, the 1.20.1 markBlockForUpdate (client resync), so FC code that wakes its
    // neighbours with it (FCBlockGearBox / FCBlockSaw.ToggleFacing, FCBlockDoor's other half, FCBlockStake,
    // FCEntityUrn, ...) woke nobody.
    @Override
    public void notifyBlockChange(int x, int y, int z, int blockID) {
        notifyBlocksOfNeighborChange(x, y, z, blockID);
    }

    // vanilla/server World.java:694-702: onNeighborBlockChange(..., blockID) on the six neighbours, in the
    // -x,+x,-y,+y,-z,+z order 1.20.1 updateNeighborsAt also uses (NeighborUpdater.UPDATE_ORDER). 1.5.2
    // notifies unconditionally: an id with no 1.20.1 block (0 after FC cleared the cell, an unmapped id)
    // used to notify nobody; it now uses neighbourSource's fallback.
    @Override
    public void notifyBlocksOfNeighborChange(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        level.updateNeighborsAt(pos, neighbourSource(pos, blockID));
    }

    // vanilla/server World.java:744-778 (one neighbour). 1.5.2 passes no source position; the cell itself is
    // handed over as fromPos, as before.
    @Override
    public void notifyBlockOfNeighborChange(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block sourceBlock = blockID > 0 ? ProxyRegistry.getModernBlock(blockID) : null;
        level.getBlockState(pos).neighborChanged(level, pos,
                sourceBlock != null ? sourceBlock : Blocks.AIR, pos, false);
    }

    /**
     * The 1.20.1 block that stands for legacy id {@code blockID} as the source of a neighbour update at
     * {@code pos}: the variant actually there when it reads as that id ({@link #modernBlockAt}), else the id's
     * canonical block; air for id 0 (FC passes 0 after clearing the cell, FCEntityUrn.java:315-317); for an
     * unmapped id, whatever is at {@code pos}. Every neighbour's FC onNeighborBlockChange (ProxyBlock and
     * BlockBehaviorMixin) maps the source back through ProxyRegistry.getNeighbourSourceId, so it sees
     * {@code blockID} as 1.5.2 passes it (World.java:694-702); air reads as 0.
     */
    private net.minecraft.world.level.block.Block neighbourSource(BlockPos pos, int blockID) {
        if (blockID <= 0) {
            return Blocks.AIR;
        }
        net.minecraft.world.level.block.Block source = modernBlockAt(pos, blockID);
        return source != null ? source : level.getBlockState(pos).getBlock();
    }

    // 1.5.2 markBlockForUpdate: "on the server, sends the block to the client" (vanilla/server
    // World.java:638-644, WorldManager.markBlockForUpdate). The only primitive that maps to sendBlockUpdated.
    public void markBlockForUpdate(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        // Render updates are client-side; on server this is a no-op
    }

    // ================================================================
    // Scheduled updates
    // ================================================================

    
    public void scheduleBlockUpdate(int x, int y, int z, int blockID, int delay) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block modernBlock = modernBlockAt(pos, blockID);
        if (modernBlock != null) {
            level.scheduleTick(pos, modernBlock, delay);
        }
    }

    /**
     * The MC block FC means by a legacy id at a position: the block actually there when it reads as that id
     * (spruce leaves / orange wool for 18 / 35), else the id's canonical block. MC drops a scheduled tick or
     * block event whose block is not the one at the position (ServerLevel.tickBlock / doBlockEvent check
     * state.is(block)), so ticks FC scheduled on any non-canonical variant were lost.
     */
    private net.minecraft.world.level.block.Block modernBlockAt(BlockPos pos, int blockID) {
        net.minecraft.world.level.block.Block here = level.getBlockState(pos).getBlock();
        if (blockID > 0 && ProxyRegistry.getBlockId(here) == blockID) {
            return here;
        }
        return ProxyRegistry.getModernBlock(blockID);
    }

    
    public void func_82740_a(int x, int y, int z, int blockID, int delay, int priority) {
        // Same as scheduleBlockUpdate with priority (ignored for simplicity)
        scheduleBlockUpdate(x, y, z, blockID, delay);
    }

    // The three tick queries follow their 1.5.2 bodies, not their names (they used to share hasScheduledTick):
    //  - isBlockTickScheduled: WorldServer.java:421-425, field_94579_S = the ticks drained for THIS tick and not
    //    yet run (tickUpdates adds at :574, removes each entry at :584 before its updateTick) == willTickThisTick
    //    (LevelTicks.toRunThisTickSet loses each tick before it runs, LevelTicks.runCollectedTicks);
    //  - IsUpdatePendingThisTickForBlock: World.java:4100-4107, a wrapper for isBlockTickScheduled;
    //  - IsUpdateScheduledForBlock: WorldServer.java:1258-1263, field_73064_N = the future (not yet drained)
    //    set == LevelTicks.hasScheduledTick (LevelChunkTicks.poll drops a drained tick from ticksPerPosition).
    // FCBlockGearBox.java:128-130 (and the clutch, millstone, saw, ...) guard their re-schedule with the
    // this-tick query; answering "scheduled in the future" there skipped or duplicated ticks.
    @Override
    public boolean isBlockTickScheduled(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block modernBlock = modernBlockAt(pos, blockID);
        if (modernBlock != null) {
            return level.getBlockTicks().willTickThisTick(pos, modernBlock);
        }
        return false;
    }

    @Override
    public boolean IsUpdateScheduledForBlock(int i, int j, int k, int iBlockID) {
        BlockPos pos = new BlockPos(i, j, k);
        net.minecraft.world.level.block.Block modernBlock = modernBlockAt(pos, iBlockID);
        if (modernBlock != null) {
            return level.getBlockTicks().hasScheduledTick(pos, modernBlock);
        }
        return false;
    }

    @Override
    public boolean IsUpdatePendingThisTickForBlock(int i, int j, int k, int iBlockID) {
        return isBlockTickScheduled(i, j, k, iBlockID);
    }

    // ================================================================
    // Light methods
    // ================================================================

    // 1.5.2 World.getLightBrightness -> provider.lightBrightnessTable[getBlockLightValue]
    // with the overworld generateLightBrightnessTable curve (ambient 0). MUST read the
    // inherited public skylightSubtracted field: EntityPlayerMP.IsInGloom temporarily
    // overrides it around its two calls to probe "value at night" (gloom only runs in
    // dimension 0, so the overworld curve is correct). Without this override the shim
    // returns 0 and gloom reads perpetual darkness.
    // 1.5.2 World.getLightBrightness = provider.lightBrightnessTable[getBlockLightValue].
    // sky is reduced by skylightSubtracted (ticked in ModSpecificTick; also transiently
    // overridden by EntityPlayerMP.IsInGloom's night-probe), and the provider TABLE — not
    // an inline curve — supplies the value so the Nether's 0.1 ambient floor is honored.
    @Override
    public float getLightBrightness(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) - this.skylightSubtracted;
        int block = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        int light = Math.min(Math.max(block, Math.max(sky, 0)), 15);
        return provider.lightBrightnessTable[light];
    }

    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return level.canSeeSky(new BlockPos(x, y, z));
    }

    // 1.5.2 World.getFullBlockLightValue (vanilla/server World.java:798-813) = Chunk.getBlockLightValue(x, y, z, 0):
    // max(sky - 0, block), stored light NOT darkened by the time of day, which getBlockLightValue applies
    // (skylightSubtracted). It used to answer getMaxLocalRawBrightness, i.e. getSkyDarken subtracted: FC's light
    // rules on it read night as darkness -- BlockFlower.canBlockStay (full light >= 8 or sky, vanilla/server
    // BlockFlower.java:143-147) would drop sky-lit plants under a roof every night, FCBlockMushroom (< 13, Common
    // FCBlockMushroom.java:70) and FCBlockMushroomBrown (== 0, Common FCBlockMushroomBrown.java:60) would spread
    // where 4.B forbids it. getRawBrightness(pos, 0) is exactly the undarkened max.
    public int getFullBlockLightValue(int x, int y, int z) {
        return level.getRawBrightness(new BlockPos(x, y, z), 0);
    }

    @Override
    public int getBlockLightValue(int x, int y, int z) {
        return level.getMaxLocalRawBrightness(new BlockPos(x, y, z));
    }

    @Override
    public int getBlockLightValue_do(int x, int y, int z, boolean useNeighbors) {
        return level.getMaxLocalRawBrightness(new BlockPos(x, y, z));
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        int block = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        if (block < lightValue) block = lightValue;
        return (sky << 20) | (block << 4);
    }

    // GetBlockNaturalLightValue is intentionally NOT overridden here: the Modern-Common
    // World implementation applies skylightSubtracted and the neighbor-brightness pass
    // (needed by the gloom probe), delegating to getSavedLightValue/getChunkFromChunkCoords
    // which this bridge provides. A raw-sky override here would dead-shadow it.

    @Override
    public int getSavedLightValue(EnumSkyBlock enumSkyBlock, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (enumSkyBlock == EnumSkyBlock.Sky) {
            return level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        } else {
            return level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        }
    }

    
    public void setLightValue(EnumSkyBlock enumSkyBlock, int x, int y, int z, int lightValue) {
        // Modern MC handles lighting internally; this is largely a no-op on the server.
    }

    // ================================================================
    // Height methods
    // ================================================================

    @Override
    public int getHeightValue(int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    @Override
    public int getTopSolidOrLiquidBlock(int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    @Override
    public int getPrecipitationHeight(int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    @Override
    public int getFirstUncoveredBlock(int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    @Override
    public int getChunkHeightMapMinimum(int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    // ================================================================
    // Chunk methods
    // ================================================================

    
    public boolean blockExists(int x, int y, int z) {
        return level.isLoaded(new BlockPos(x, y, z));
    }

    @Override
    public boolean doChunksNearChunkExist(int x, int y, int z, int range) {
        return true;
    }

    @Override
    public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) {
        return true;
    }


    public boolean chunkExists(int chunkX, int chunkZ) {
        return level.hasChunk(chunkX, chunkZ);
    }

    @Override
    public btw.modern.Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
        // Return a lightweight Chunk that delegates to WorldBridge for block access
        return new btw.modern.Chunk(this, chunkX, chunkZ);
    }

    @Override
    public btw.modern.Chunk getChunkFromBlockCoords(int x, int z) {
        return getChunkFromChunkCoords(x >> 4, z >> 4);
    }

    // ================================================================
    // Biome
    // ================================================================

    
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        Holder<Biome> holder = level.getBiome(new BlockPos(x, 64, z));
        return mapBiomeToFc(holder);
    }

    /**
     * Maps a modern Biome holder to the closest FC BiomeGenBase static
     * instance.  Falls back to {@link BiomeGenBase#plains} if the biome
     * is unknown or FC biomes have not been initialised yet.
     */
    private static BiomeGenBase mapBiomeToFc(Holder<Biome> holder) {
        // Safety: if FC biomes were never initialised, return a non-null default
        if (BiomeGenBase.plains == null) {
            return new BiomeGenBase(1) {};
        }

        ResourceKey<Biome> key = holder.unwrapKey().orElse(null);
        if (key == null) return BiomeGenBase.plains;

        ResourceLocation loc = key.location();
        String path = loc.getPath(); // e.g. "plains", "desert", "frozen_ocean"

        return switch (path) {
            case "ocean", "deep_ocean",
                 "deep_lukewarm_ocean", "deep_cold_ocean",
                 "deep_frozen_ocean", "warm_ocean",
                 "lukewarm_ocean", "cold_ocean" -> BiomeGenBase.ocean;
            case "plains", "sunflower_plains",
                 "meadow" -> BiomeGenBase.plains;
            case "desert" -> BiomeGenBase.desert;
            case "windswept_hills", "windswept_gravelly_hills",
                 "windswept_forest", "stony_peaks" -> BiomeGenBase.extremeHills;
            case "forest", "flower_forest",
                 "birch_forest", "old_growth_birch_forest",
                 "dark_forest" -> BiomeGenBase.forest;
            case "taiga", "old_growth_pine_taiga",
                 "old_growth_spruce_taiga" -> BiomeGenBase.taiga;
            case "swamp", "mangrove_swamp" -> BiomeGenBase.swampland;
            case "river" -> BiomeGenBase.river;
            case "nether_wastes", "soul_sand_valley",
                 "crimson_forest", "warped_forest",
                 "basalt_deltas" -> BiomeGenBase.hell;
            case "the_end", "end_highlands",
                 "end_midlands", "end_barrens",
                 "small_end_islands", "the_void" -> BiomeGenBase.sky;
            case "frozen_ocean" -> BiomeGenBase.frozenOcean;
            case "frozen_river" -> BiomeGenBase.frozenRiver;
            case "snowy_plains", "snowy_slopes",
                 "frozen_peaks", "ice_spikes" -> BiomeGenBase.icePlains;
            case "snowy_taiga", "grove",
                 "jagged_peaks" -> BiomeGenBase.iceMountains;
            case "mushroom_fields" -> BiomeGenBase.mushroomIsland;
            case "beach", "stony_shore" -> BiomeGenBase.beach;
            case "jungle", "sparse_jungle",
                 "bamboo_jungle" -> BiomeGenBase.jungle;
            case "snowy_beach" -> BiomeGenBase.frozenOcean;
            case "savanna", "savanna_plateau",
                 "windswept_savanna" -> BiomeGenBase.plains;
            case "badlands", "eroded_badlands",
                 "wooded_badlands" -> BiomeGenBase.desert;
            case "cherry_grove" -> BiomeGenBase.forest;
            case "dripstone_caves", "lush_caves",
                 "deep_dark" -> BiomeGenBase.plains;
            default -> BiomeGenBase.plains;
        };
    }

    
    /**
     * Lazy-initialised WorldChunkManager bridge that delegates biome
     * lookups to the real MC level.
     */
    private WorldChunkManager chunkManagerBridge;

    public WorldChunkManager getWorldChunkManager() {
        if (chunkManagerBridge == null) {
            chunkManagerBridge = new WorldChunkManager() {
                @Override
                public BiomeGenBase getBiomeGenAt(int x, int z) {
                    return getBiomeGenForCoords(x, z);
                }
            };
        }
        return chunkManagerBridge;
    }

    // ================================================================
    // Entity methods
    // ================================================================

    /**
     * Maps FC entity instances to their Forge proxy counterpart so we
     * can look up the proxy for removal, position sync, etc.
     * Values are held weakly too: the proxy owns its FC entity, so a strong value pinned
     * the weak key and every FC-spawned proxy stayed reachable for the world's lifetime (RE2).
     */
    private final WeakValueMap<btw.modern.Entity, net.minecraft.world.entity.Entity> fcToForgeEntity =
            new WeakValueMap<>();

    /**
     * FC views of non-living vanilla entities for FC projectile hit tests ({@link VanillaProjectileTarget}).
     * Weak keys; a view holds its entity weakly and otherwise reaches only this bridge, so an entry dies with
     * its entity and the whole map with the bridge. Server thread only.
     */
    private final Map<net.minecraft.world.entity.Entity, VanillaProjectileTarget> projectileTargets =
            new WeakHashMap<>();

    /**
     * WeakHashMap whose values are WeakReferences, exposing the get/put/remove/clear subset
     * fcToForgeEntity uses. A value is resolvable while something else (the level's entity
     * storage) keeps it alive; once collected, get() answers null like a missing mapping.
     */
    private static final class WeakValueMap<K, V> {
        private final Map<K, java.lang.ref.WeakReference<V>> map = new WeakHashMap<>();

        V get(Object key) {
            java.lang.ref.WeakReference<V> ref = map.get(key);
            return ref != null ? ref.get() : null;
        }

        V put(K key, V value) {
            java.lang.ref.WeakReference<V> old = map.put(key, new java.lang.ref.WeakReference<>(value));
            return old != null ? old.get() : null;
        }

        V remove(Object key) {
            java.lang.ref.WeakReference<V> old = map.remove(key);
            return old != null ? old.get() : null;
        }

        void clear() {
            map.clear();
        }
    }

    
    public boolean spawnEntityInWorld(btw.modern.Entity entity) {
        if (entity == null) return false;
        try {
            // DIAGNOSTIC (design_build_logging U10 F): overworld ghasts come only from possessed-squid
            // conversion. A [GHAST-SPAWN] in dim=minecraft:overworld confirms the possession chain fired --
            // correlate with the BTW-Possession log (PossessionDiagnostics). Kept while STUB_CATALOGUE (f) is
            // open; remove together with PossessionDiagnostics.
            if ("FCEntityGhast".equals(entity.getClass().getSimpleName())) {
                LOGGER.warn("[GHAST-SPAWN] FCEntityGhast (squid->ghast conversion) dim={} pos=({}, {}, {})",
                        level.dimension().location(), entity.posX, entity.posY, entity.posZ);
            }
            net.minecraft.world.entity.Entity proxy =
                    EntityProxyFactory.createProxy(entity, level);
            if (proxy == null) {
                LOGGER.warn("EntityProxyFactory returned null for {}",
                        entity.getClass().getSimpleName());
                return false;
            }
            fcToForgeEntity.put(entity, proxy);
            return level.addFreshEntity(proxy);
        } catch (Exception e) {
            // Was DEBUG with the message only: a failing FC spawn (drops, projectiles, offspring) was invisible.
            warnOncePerThrowSite("SPAWN", "spawnEntityInWorld(" + entity.getClass().getSimpleName() + ")", e);
            return false;
        }
    }

    
    public void removeEntity(btw.modern.Entity entity) {
        if (entity == null) return;
        net.minecraft.world.entity.Entity proxy = fcToForgeEntity.remove(entity);
        if (proxy != null) {
            proxy.discard();
        }
    }

    
    @SuppressWarnings("unchecked")
    public List getEntitiesWithinAABB(Class entityClass, AxisAlignedBB aabb) {
        net.minecraft.world.phys.AABB forgeAABB = toForgeAABB(aabb);
        // 1.5.2 World.getEntitiesWithinAABB returned EntityItems like any entity. MC ItemEntities are not
        // proxies (EntityProxyFactory.createItemEntity), so they are exposed through their ItemEntityBridge
        // view whenever the requested class admits an EntityItem: EntityAnimal.CheckForLooseFood
        // (vanilla/server EntityAnimal.java:1322-1368), FCEntityAIMoveToLooseFood:59, FCBlockCookingVessel:60,
        // FCBlockBellows:432, the mob CheckForLooseFood variants, and frozen EntityLiving loot pickup
        // (EntityLiving.java:1933-2012; its onItemPickup cast to WorldServer, :2850, holds since WorldBridge
        // became one, so the picked-up item is setDead() instead of duplicated).
        final boolean wantItems = entityClass.isAssignableFrom(ItemEntityBridge.class);
        List<net.minecraft.world.entity.Entity> forgeEntities =
                level.getEntities((net.minecraft.world.entity.Entity) null, forgeAABB,
                        e -> e instanceof net.minecraft.world.entity.item.ItemEntity
                                ? wantItems : matchesFcClass(e, entityClass));
        // Return the FC entity instances rather than the proxies so FC
        // code can cast them to the expected FC types.
        List result = new ArrayList();
        for (net.minecraft.world.entity.Entity forgeEntity : forgeEntities) {
            btw.modern.Entity fc = extractFcEntityOrItem(forgeEntity);
            if (fc != null && entityClass.isInstance(fc)) {
                result.add(fc);
            }
        }
        return result;
    }

    
    public List getEntitiesWithinAABBExcludingEntity(btw.modern.Entity entity, AxisAlignedBB aabb) {
        net.minecraft.world.phys.AABB forgeAABB = toForgeAABB(aabb);
        net.minecraft.world.entity.Entity excludeProxy = entity instanceof ItemEntityBridge ib
                ? ib.getRealEntity() : fcToForgeEntity.get(entity);
        // 1.5.2 returned every entity in the box. FC projectiles test them all (EntityArrow.java:234-259,
        // EntityThrowable.java:164, EntityFireball.java:133: canBeCollidedWith + boundingBox intercept), so for
        // them vanilla MC entities are wrapped too -- FC arrows / thrown items / fireballs used to fly through
        // every vanilla mob, boat, end crystal and dragon part (wrapVanillaForFc). Other queries keep FC
        // entities and items only: frozen EntityLiving.func_85033_bc pushes what it finds, and MC already
        // pushes vanilla entities apart.
        final boolean wrapVanilla = isFcProjectile(entity);
        List<net.minecraft.world.entity.Entity> forgeEntities =
                level.getEntities(excludeProxy, forgeAABB, e -> true);
        List result = new ArrayList();
        for (net.minecraft.world.entity.Entity forgeEntity : forgeEntities) {
            btw.modern.Entity fc = extractFcEntityOrItem(forgeEntity);
            if (fc == null && wrapVanilla) {
                fc = wrapVanillaForFc(forgeEntity);
            }
            // `fc != entity`: puppets VanillaMobReplacer/chunk loading created are not in fcToForgeEntity, so
            // excludeProxy is null for them and 1.5.2's "excluding entity" must be applied here
            if (fc != null && fc != entity) {
                result.add(fc);
            }
        }
        return result;
    }

    /**
     * 1.5.2 World.checkNoEntityCollision(AxisAlignedBB) (vanilla/server World.java:1990-1993): the two-argument rule
     * with no entity excluded. FCMOD-INTEGRATION(1.20.1): placement_validity (decision 5(a)): it answered false for any
     * entity in the box, items and XP orbs included, so FC's IsTargetFreeOfObstructingEntities
     * (Common FCItemPlacesAsBlock.java:217-223), its mob spawn checks (Modern-Common EntityLiving.java:393) and the
     * ghast's and the beacon's checks refused where 4.B did not.
     */
    public boolean checkNoEntityCollision(AxisAlignedBB aabb) {
        return checkNoEntityCollision(aabb, null);
    }

    /**
     * 1.5.2 World.checkNoEntityCollision(AxisAlignedBB, Entity) (vanilla/server World.java:1998-2013): true unless a live
     * entity other than {@code except} that prevents entity spawning is in the box. Modern-Common World's body is a stub
     * that answered true (World.java:669-671), so the windmill and the water wheel (FCEntityMechPower.java:294-298) and
     * every FCItemPlacesAsBlock placement (canPlaceEntityOnSide, below) ignored entities.
     *
     * <p>FCMOD-INTEGRATION(1.20.1): placement_validity: 1.5.2's preventEntitySpawning is 1.20.1's
     * {@code Entity.blocksBuilding} (living entities, boats, minecarts, falling blocks, primed TNT and end crystals:
     * LivingEntity.java:236, Boat:94, AbstractMinecart:93, FallingBlockEntity:69, PrimedTnt:26, EndCrystal:30), plus the
     * FC object's own flag for the FC entities whose 1.20.1 proxy does not block building (windmills, water wheels,
     * platforms, dynamite, mining charges: Server FCEntityMechPower.java:52, ...). Removed entities and spectators never
     * count (1.20.1's own placement test, Level.isUnobstructed, skips both), nor does {@code except}'s 1.20.1
     * counterpart. Never throws (the falling-block and dispenser callers have no guard): a failure is logged once and
     * counts as a collision, so it refuses, as canPlaceEntityOnSide's own guard does.</p>
     */
    public boolean checkNoEntityCollision(AxisAlignedBB aabb, btw.modern.Entity except) {
        if (aabb == null) return true;
        try {
            net.minecraft.world.entity.Entity exceptMc = except == null ? null
                    : except instanceof PlayerBridge pb ? pb.getRealPlayer()
                    : except instanceof ItemEntityBridge ib ? ib.getRealEntity()
                    : fcToForgeEntity.get(except);
            for (net.minecraft.world.entity.Entity e : level.getEntities(
                    (net.minecraft.world.entity.Entity) null, toForgeAABB(aabb),
                    net.minecraft.world.entity.EntitySelector.NO_SPECTATORS)) {
                if (e.isRemoved() || e == exceptMc) continue;
                // A puppet (VanillaMobReplacer, chunk loading) is not in fcToForgeEntity: compare its FC object
                if (except != null && exceptMc == null && extractFcEntity(e) == except) continue;
                if (e.blocksBuilding) return false;
                btw.modern.Entity fc = extractFcEntity(e);
                if (fc != null && fc != except && !fc.isDead && fc.preventEntitySpawning) return false;
            }
            return true;
        } catch (Throwable t) {
            warnOncePerThrowSite("ENTITY-COLLISION", "checkNoEntityCollision", t);
            return false;
        }
    }

    /**
     * 1.5.2 World.canPlaceEntityOnSide (vanilla/server World.java:3377-3407, FCMOD): whether block {@code blockID} may
     * be placed at (x, y, z) against {@code side}. Callers: item placement (Modern-Common ItemBlock and ItemReed, Common
     * FCItemPlacesAsBlock and FCItemBloodMossSpores), both block-dispenser paths and FC falling blocks
     * ({@code skipEntities}). In 1.5.2's order:
     * <ol>
     *   <li>the entity check: the new block's collision box holds no entity that prevents spawning, other than an
     *       {@code entity} that is not a player ({@link #checkNoEntityCollision(AxisAlignedBB, btw.modern.Entity)};
     *       skipped for {@code skipEntities}). The placing player counts: 1.5.2 excluded it here, but 4.B's client never
     *       sent a placement its own pre-check refused, and that pre-check passes a null entity (Common
     *       FCItemPlacesAsBlock.canPlaceItemBlockOnSide :202, Modern-Common ItemReed :162). The 1.20.1 client never runs
     *       it, so the server does: no block goes into the placing player's own space (as Modern-Common
     *       ItemBlock.java:177-181);</li>
     *   <li>a free target: air, water, lava, fire, a replaceable material, FC's detector logic blocks or stoked
     *       fire;</li>
     *   <li>an anvil may take a circuits cell (a rail, wire, a torch);</li>
     *   <li>otherwise the target must be free and the new block's {@code canPlaceBlockOnSide} must agree (the soil of a
     *       plant, the wall of a hook, the support of a rail).</li>
     * </ol>
     *
     * <p>FCMOD-INTEGRATION(1.20.1): placement_validity (decisions 1(a) and 2(a); triage CF2-P8a). The port's body only
     * checked that no entity of any kind stood in the cell, and answered true for {@code skipEntities}: no free-target
     * test and no canPlaceBlockOnSide, so the FC route put flowers on stone and slabs, a hook into the superflat grass
     * and any block over a flower, and falling sand deleted rails. Port rules:</p>
     * <ul>
     *   <li>a cell FC does not model (no legacy id and no twin: post-1.5.2 and modded blocks, water-holders, bark and
     *       stripped logs) is free by 1.20.1's own rule for the item, {@code BlockState.canBeReplaced(BlockPlaceContext)}
     *       (BlockBehaviour.java:238-240), so block overrides answer (candles, sea pickles, modded plants), falling back
     *       to {@code canBeReplaced()} (:809-811);</li>
     *   <li>water, lava, fire and the anvil are the Modern-Common statics, which BTWLifecycle rebinds to FC's
     *       blocks;</li>
     *   <li>FC's detector logic blocks and stoked fire are read once by reflection (their ids are configurable, Client
     *       FCBetterThanWolves.java:1007/1069/1093);</li>
     *   <li>the FC calls are guarded: a throw is logged once and refuses, as the falling-block and dispenser callers
     *       have no guard.</li>
     * </ul>
     */
    public boolean canPlaceEntityOnSide(int blockID, int x, int y, int z,
                                        boolean skipEntities, int side,
                                        btw.modern.Entity entity, ItemStack stack) {
        btw.modern.Block[] blocks = btw.modern.Block.blocksList;
        btw.modern.Block newBlock = blockID > 0 && blockID < blocks.length ? blocks[blockID] : null;
        if (newBlock == null) return false; // 1.5.2: par1 > 0, and blocksList[par1] dereferenced unguarded
        BlockPos pos = new BlockPos(x, y, z);
        try {
            if (!skipEntities) {
                AxisAlignedBB box = newBlock.getCollisionBoundingBoxFromPool(this, x, y, z);
                // FCMOD-INTEGRATION(1.20.1): placement_validity: the placing player counts, as 4.B's client pre-check did (null entity: Common FCItemPlacesAsBlock.canPlaceItemBlockOnSide :202, Modern-Common ItemReed :162); the 1.20.1 client never runs it, so the server does (as Modern-Common ItemBlock.java:177-181)
                btw.modern.Entity except = entity instanceof btw.modern.EntityPlayer ? null : entity;
                if (box != null && !checkNoEntityCollision(box, except)) return false;
            }

            BlockState cur = level.getBlockState(pos);
            btw.modern.Block old = null;
            boolean free;
            if (cur.isAir()) {
                free = true;
            } else if (ProxyRegistry.getBlockId(cur.getBlock()) == 0 && TwinRegistry.worldIdFor(cur) == 0) {
                free = isFreeUnmodelledCell(cur, pos, side, stack);
            } else {
                int oldId = getBlockId(x, y, z);
                old = oldId > 0 && oldId < blocks.length ? blocks[oldId] : null;
                free = old == null || old == btw.modern.Block.waterMoving || old == btw.modern.Block.waterStill
                        || old == btw.modern.Block.lavaMoving || old == btw.modern.Block.lavaStill
                        || old == btw.modern.Block.fire
                        || (old.blockMaterial != null && old.blockMaterial.isReplaceable())
                        || isFcFreeCellBlock(old);
            }

            if (!free) {
                // 1.5.2: var10 != null && var10.blockMaterial == Material.circuits && var11 == Block.anvil
                return old != null && old.blockMaterial == Material.circuits && newBlock == btw.modern.Block.anvil;
            }
            return newBlock.canPlaceBlockOnSide(this, x, y, z, side, stack);
        } catch (Throwable t) {
            warnOncePerThrowSite("PLACE-CHECK", "canPlaceEntityOnSide(" + newBlock.getClass().getSimpleName()
                    + ") at " + pos, t);
            return false;
        }
    }

    /** 1.20.1's replaceable rule for a cell FC does not model, for the item being placed (see canPlaceEntityOnSide). */
    private boolean isFreeUnmodelledCell(BlockState cur, BlockPos pos, int side, ItemStack stack) {
        try {
            net.minecraft.world.item.ItemStack held = stack != null ? ItemStackHelper.toMcStack(stack) : null;
            net.minecraft.core.Direction dir = net.minecraft.core.Direction.from3DDataValue(side);
            return cur.canBeReplaced(new net.minecraft.world.item.context.DirectionalPlaceContext(level, pos, dir,
                    held != null ? held : net.minecraft.world.item.ItemStack.EMPTY, dir));
        } catch (Throwable t) {
            warnOncePerThrowSite("PLACE-CHECK", "the 1.20.1 replaceable rule at " + pos, t);
            return cur.canBeReplaced();
        }
    }

    /**
     * FC's detector logic blocks and stoked fire, which 1.5.2 World.canPlaceEntityOnSide treats as free (FCMOD,
     * vanilla/server World.java:3399-3402). Read from FCBetterThanWolves by reflection (FC classes are not on the
     * bridge's compile path: Forge/build.gradle compiles against Api and Modern-Common), and kept once FC has set all
     * three.
     */
    private static volatile btw.modern.Block[] fcFreeCellBlocks;

    private static boolean isFcFreeCellBlock(btw.modern.Block block) {
        btw.modern.Block[] free = fcFreeCellBlocks;
        if (free == null) {
            free = new btw.modern.Block[3];
            String[] names = {"fcBlockDetectorLogic", "fcBlockDetectorGlowingLogic", "fcBlockFireStoked"};
            boolean all = true;
            try {
                Class<?> fc = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves");
                for (int i = 0; i < names.length; i++) {
                    Object value = fc.getField(names[i]).get(null);
                    if (value instanceof btw.modern.Block b) {
                        free[i] = b;
                    } else {
                        all = false;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                warnOncePerThrowSite("PLACE-CHECK", "reading FCBetterThanWolves' detector logic and stoked fire", e);
                all = false;
            }
            if (all) fcFreeCellBlocks = free;
        }
        for (btw.modern.Block b : free) {
            if (b != null && b == block) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Entity bridge helpers
    // ------------------------------------------------------------------

    /**
     * Converts an FC AxisAlignedBB to a Forge AABB.
     */
    private static net.minecraft.world.phys.AABB toForgeAABB(AxisAlignedBB aabb) {
        return new net.minecraft.world.phys.AABB(
                aabb.minX, aabb.minY, aabb.minZ,
                aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    /**
     * Extracts the FC entity from a Forge proxy, or null if the entity
     * is not a BTW proxy.
     */
    private static btw.modern.Entity extractFcEntity(net.minecraft.world.entity.Entity forgeEntity) {
        if (forgeEntity instanceof ProxyMob pm) return pm.getFcEntity();
        if (forgeEntity instanceof ProxyAnimal pa) return pa.getFcEntity();
        if (forgeEntity instanceof ProxyPathfinderMob pp) return pp.getFcEntity();
        if (forgeEntity instanceof ProxyEntity pe) return pe.getFcEntity();
        // Players are not proxies — they have a PlayerBridge that wraps the
        // MC Player as an FC EntityPlayerMP. Without this, FC AI tasks that
        // query getEntitiesWithinAABB(EntityPlayer.class, ...) cannot see
        // any players, and mobs never acquire attack targets.
        if (forgeEntity instanceof net.minecraft.world.entity.player.Player p) {
            return PlayerBridge.getOrCreate(p);
        }
        return null;
    }

    /**
     * {@link #extractFcEntity} plus MC ItemEntities exposed as their FC EntityItem view (design_items U3).
     * Spawn-cap counting, the squid-possession cap and GetClosestEntityMatchingCriteriaWithinRange stay on
     * extractFcEntity: items never count there, and it avoids wrapping every item in the level.
     */
    private static btw.modern.Entity extractFcEntityOrItem(net.minecraft.world.entity.Entity forgeEntity) {
        if (forgeEntity instanceof net.minecraft.world.entity.item.ItemEntity ie) {
            return ItemEntityBridge.wrap(ie);
        }
        return extractFcEntity(forgeEntity);
    }

    /** FC projectiles whose 1.5.2 onUpdate hit-tests every entity getEntitiesWithinAABBExcludingEntity returns. */
    private static boolean isFcProjectile(btw.modern.Entity entity) {
        return entity instanceof btw.modern.EntityArrow
                || entity instanceof btw.modern.EntityThrowable
                || entity instanceof btw.modern.EntityFireball;
    }

    /**
     * A vanilla MC entity with no FC counterpart, wrapped for an FC projectile's hit test, or null. Only
     * entities a vanilla projectile could hit are returned (Projectile.canHitEntity -> canBeHitByProjectile =
     * isAlive && isPickable): the ender dragon body is not pickable but its parts are, as in 1.5.2
     * (EntityDragonPart). Living ones get the cached LivingEntityBridge (attackEntityFrom -> MC hurt,
     * addVelocity -> MC knockback, setFire -> MC fire), everything else its cached
     * {@link VanillaProjectileTarget} (boats, minecarts, paintings, item frames, End crystals, dragon parts,
     * fireballs), which forwards the hit to MC's own hurt() -- the shared EntityBridge answers
     * attackEntityFrom with false, which the frozen projectiles treat as a ricochet. Re-synced, with its box
     * set from MC: the bridges' syncFromReal leaves boundingBox at the constructor's empty box, and the
     * projectile intersects exactly that box (EntityArrow.java:245-247).
     */
    private btw.modern.Entity wrapVanillaForFc(net.minecraft.world.entity.Entity forgeEntity) {
        // Items, proxies and players have their own FC object (extractFcEntityOrItem); one that is not available
        // (removed item, puppet without its FC entity yet) must not be wrapped as a generic vanilla entity.
        if (forgeEntity instanceof net.minecraft.world.entity.item.ItemEntity
                || forgeEntity instanceof ProxyMob || forgeEntity instanceof ProxyAnimal
                || forgeEntity instanceof ProxyPathfinderMob || forgeEntity instanceof ProxyEntity
                || forgeEntity instanceof net.minecraft.world.entity.player.Player) {
            return null;
        }
        try {
            if (!forgeEntity.canBeHitByProjectile()) return null;
            btw.modern.Entity fc;
            if (forgeEntity instanceof net.minecraft.world.entity.LivingEntity living) {
                btw.modern.EntityLiving wrapped = LivingEntityBridge.wrapLiving(living);
                if (wrapped instanceof LivingEntityBridge lb) {
                    lb.syncFromReal();
                }
                fc = wrapped;
            } else {
                VanillaProjectileTarget target = projectileTargets.get(forgeEntity);
                if (target == null) {
                    target = new VanillaProjectileTarget(forgeEntity, this);
                    projectileTargets.put(forgeEntity, target);
                }
                target.syncFromReal(forgeEntity);
                fc = target;
            }
            if (fc == null) return null;
            net.minecraft.world.phys.AABB bb = forgeEntity.getBoundingBox();
            fc.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
            return fc;
        } catch (Throwable t) {
            warnOncePerThrowSite("WRAP", "wrapping " + forgeEntity.getClass().getSimpleName() + " for FC", t);
            return null;
        }
    }

    /**
     * The MC entity an FC entity stands for: the real entity behind a player / living / item / vanilla-target
     * view, or an FC entity's proxy ({@link #proxyFor}). Null if none.
     */
    private net.minecraft.world.entity.Entity toMcEntity(btw.modern.Entity fc) {
        if (fc == null) return null;
        if (fc instanceof PlayerBridge pb) return pb.getRealPlayer();
        if (fc instanceof LivingEntityBridge lb) return lb.getRealEntity();
        if (fc instanceof EntityBridge eb) return eb.getRealEntity();
        if (fc instanceof ItemEntityBridge ib) return ib.getRealEntity();
        if (fc instanceof VanillaProjectileTarget target) return target.real();
        return proxyFor(fc);
    }

    /**
     * MC damage source for an FC one that hits an MC entity. The 1.5.2 projectile sources -- DamageSource
     * causeArrowDamage / causeThrownDamage / causeFireballDamage / causeIndirectMagicDamage, all
     * EntityDamageSourceIndirect(type, projectile, shooter) -- become the matching 1.20.1 type with the
     * projectile's MC entity as the direct and the shooter's as the causing entity, so the victim credits and
     * retaliates against the shooter, shields and projectile protection apply, and the death message reads
     * "shot by". DamageSources.arrow / fireball take an AbstractArrow / Fireball, which an FC projectile's
     * ProxyEntity is not, so the source is built from the registry holder exactly as DamageSources.source(key,
     * direct, causing) does. Everything else goes through {@link DamageSourceMapping#getModern}. Never throws.
     */
    public static net.minecraft.world.damagesource.DamageSource toMcDamageSource(
            btw.modern.DamageSource fcSource, ServerLevel level) {
        if (fcSource instanceof btw.modern.EntityDamageSourceIndirect && fcSource.damageType != null) {
            ResourceKey<net.minecraft.world.damagesource.DamageType> type = switch (fcSource.damageType) {
                case "arrow" -> net.minecraft.world.damagesource.DamageTypes.ARROW;
                case "thrown" -> net.minecraft.world.damagesource.DamageTypes.THROWN;
                case "fireball" -> net.minecraft.world.damagesource.DamageTypes.FIREBALL;
                // causeFireballDamage without a shooter: EntityDamageSourceIndirect("onFire", fireball, fireball)
                case "onFire" -> net.minecraft.world.damagesource.DamageTypes.UNATTRIBUTED_FIREBALL;
                case "indirectMagic" -> net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC;
                default -> null;
            };
            if (type != null) {
                try {
                    WorldBridge bridge = getOrCreate(level);
                    net.minecraft.world.entity.Entity direct = bridge.toMcEntity(fcSource.getSourceOfDamage());
                    net.minecraft.world.entity.Entity causing = bridge.toMcEntity(fcSource.getEntity());
                    if (causing == null && type == net.minecraft.world.damagesource.DamageTypes.FIREBALL) {
                        type = net.minecraft.world.damagesource.DamageTypes.UNATTRIBUTED_FIREBALL; // as DamageSources.fireball
                    }
                    net.minecraft.core.Registry<net.minecraft.world.damagesource.DamageType> types =
                            level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
                    return new net.minecraft.world.damagesource.DamageSource(
                            types.getHolderOrThrow(type), direct, causing);
                } catch (Throwable t) {
                    warnOncePerThrowSite("DAMAGE-SOURCE", "mapping FC damage source '" + fcSource.damageType + "'", t);
                }
            }
        }
        return DamageSourceMapping.getModern(fcSource, level);
    }

    /**
     * FC view of a NON-living vanilla entity for an FC projectile's hit test (boat, minecart, painting, item
     * frame, End crystal, ender dragon part, ghast fireball, shulker bullet, ...). In 1.5.2 the projectile
     * damaged these through their own attackEntityFrom (EntityBoat, EntityHanging, EntityEnderCrystal,
     * EntityDragonPart -> EntityDragon.attackEntityFromPart); here the hit is MC's own hurt() with the
     * projectile's MC damage source ({@link #toMcDamageSource}), and hurt()'s result is returned: boats and
     * minecarts take damage, hanging entities break, the crystal explodes, EnderDragonPart.hurt forwards to the
     * dragon with the head multiplier, and a rejected hit (falling block, invulnerable entity) ricochets
     * (vanilla/server EntityArrow.java:308-347), exactly as a vanilla arrow does (AbstractArrow.onHitEntity).
     * Cached per entity ({@link #projectileTargets}); holds the entity weakly, like EntityBridge.
     */
    private static final class VanillaProjectileTarget extends btw.modern.Entity {
        private final java.lang.ref.WeakReference<net.minecraft.world.entity.Entity> realRef;

        VanillaProjectileTarget(net.minecraft.world.entity.Entity real, WorldBridge world) {
            super(null); // btw.modern.Entity(World), as EntityBridge
            this.realRef = new java.lang.ref.WeakReference<>(real);
            this.worldObj = world;
        }

        /** The wrapped entity; null while super(null) runs or once it has been collected. */
        net.minecraft.world.entity.Entity real() {
            return realRef != null ? realRef.get() : null;
        }

        /** Position, motion and liveness from MC before each hit test (the box is set by wrapVanillaForFc). */
        void syncFromReal(net.minecraft.world.entity.Entity real) {
            this.entityId = real.getId();
            this.posX = real.getX();
            this.posY = real.getY();
            this.posZ = real.getZ();
            this.prevPosX = real.xOld;
            this.prevPosY = real.yOld;
            this.prevPosZ = real.zOld;
            net.minecraft.world.phys.Vec3 v = real.getDeltaMovement();
            this.motionX = v.x;
            this.motionY = v.y;
            this.motionZ = v.z;
            this.rotationYaw = real.getYRot();
            this.rotationPitch = real.getXRot();
            this.onGround = real.onGround();
            this.isDead = real.isRemoved();
            this.width = real.getBbWidth();
            this.height = real.getBbHeight();
        }

        // Abstract in the frozen 1.5.2 Entity (its constructor calls entityInit); never saved.
        @Override
        public void entityInit() {
        }

        @Override
        public void readEntityFromNBT(btw.modern.NBTTagCompound tag) {
        }

        @Override
        public void writeEntityToNBT(btw.modern.NBTTagCompound tag) {
        }

        @Override
        public boolean canBeCollidedWith() {
            net.minecraft.world.entity.Entity r = real();
            return r != null && r.canBeHitByProjectile();
        }

        @Override
        public boolean attackEntityFrom(btw.modern.DamageSource damageSource, int amount) {
            net.minecraft.world.entity.Entity r = real();
            if (r == null || !(r.level() instanceof ServerLevel sl)) return false;
            try {
                return r.hurt(WorldBridge.toMcDamageSource(damageSource, sl), (float) amount);
            } catch (Throwable t) {
                WorldBridge.warnOncePerThrowSite("PROJECTILE-HIT",
                        "FC projectile hit on " + r.getClass().getSimpleName(), t);
                return false;
            }
        }

        @Override
        public void setFire(int seconds) {
            net.minecraft.world.entity.Entity r = real();
            if (r != null) {
                r.setSecondsOnFire(seconds); // AbstractArrow.onHitEntity: a burning arrow ignites what it hits
            }
        }

        @Override
        public boolean isBurning() {
            net.minecraft.world.entity.Entity r = real();
            return r != null && r.isOnFire();
        }

        @Override
        public boolean isEntityAlive() {
            net.minecraft.world.entity.Entity r = real();
            return r != null && r.isAlive();
        }

        @Override
        public boolean canBePushed() {
            net.minecraft.world.entity.Entity r = real();
            return r != null && r.isPushable();
        }

        @Override
        public void addVelocity(double x, double y, double z) {
            this.motionX += x;
            this.motionY += y;
            this.motionZ += z;
            net.minecraft.world.entity.Entity r = real();
            if (r != null) {
                r.push(x, y, z);
            }
        }

        @Override
        public void setDead() {
            super.setDead();
            net.minecraft.world.entity.Entity r = real();
            if (r != null) {
                r.discard();
            }
        }

        @Override
        public String getEntityName() {
            net.minecraft.world.entity.Entity r = real();
            return r != null ? r.getName().getString() : super.getEntityName();
        }
    }

    /** Failure sites already reported by {@link #warnOncePerThrowSite}. */
    private static final java.util.Set<String> LOGGED_FAILURE_SITES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * One WARN with its stack trace per distinct throw site (stage + exception class + top frame), triage
     * HAI-10: these failures were dropped at DEBUG with the message only (or logged without a trace), which hid
     * how often an FC path fails and where. Reflection wrappers are unwrapped to the FC exception first.
     */
    private static void warnOncePerThrowSite(String stage, String what, Throwable t) {
        Throwable cause = t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null
                ? t.getCause() : t;
        StackTraceElement[] frames = cause.getStackTrace();
        String site = stage + '|' + cause.getClass().getName() + '@' + (frames.length > 0 ? frames[0] : "?");
        if (LOGGED_FAILURE_SITES.add(site)) {
            LOGGER.warn("[{}] {} threw {} (logged once per throw site)", stage, what, cause.toString(), cause);
        }
    }

    /**
     * Checks whether a Forge entity wraps an FC entity that is an
     * instance of the given FC class.
     */
    private static boolean matchesFcClass(
            net.minecraft.world.entity.Entity forgeEntity, Class<?> fcClass) {
        btw.modern.Entity fc = extractFcEntity(forgeEntity);
        return fc != null && fcClass.isInstance(fc);
    }

    // ================================================================
    // Sound methods
    // ================================================================


    public void playSoundEffect(double x, double y, double z,
                                String sound, float volume, float pitch) {
        playSoundEffectExcept(PREDICTING_ACTOR.get(), x, y, z, sound, volume, pitch);
    }

    /**
     * The player whose 1.20.1 client already played this interaction's sound itself, or null. 1.5.2 clients
     * predicted placements silently (World.playSoundEffect -> RenderGlobal.playSound is a no-op,
     * vanilla/client RenderGlobal.java:1812), so the server's sound reached the placer too. A 1.20.1 client
     * predicts BlockItem.useOn and plays the place sound locally (MultiPlayerGameMode.performUseItemOn), so
     * while FC places on the server for such an interaction its sounds leave that player out. Server thread
     * only; set through {@link #withPredictingActor}.
     */
    private static final ThreadLocal<ServerPlayer> PREDICTING_ACTOR = new ThreadLocal<>();

    /** Runs {@code body} with {@code actor} excluded from playSoundEffect (see {@link #PREDICTING_ACTOR}). */
    public static <T> T withPredictingActor(ServerPlayer actor, java.util.function.Supplier<T> body) {
        ServerPlayer previous = PREDICTING_ACTOR.get();
        PREDICTING_ACTOR.set(actor);
        try {
            return body.get();
        } finally {
            PREDICTING_ACTOR.set(previous);
        }
    }

    /**
     * playSoundEffect for every player in range except {@code except} (null: everyone): the sounds this bridge
     * plays itself for an aux-SFX that 1.5.2 sent to all but the actor (WorldManager.playAuxSFX,
     * vanilla/server WorldManager.java:103-106).
     */
    private void playSoundEffectExcept(ServerPlayer except, double x, double y, double z,
                                       String sound, float volume, float pitch) {
        try {
            SoundEvent event = resolveSoundEvent(sound);
            if (event != null) {
                level.playSound(except, x, y, z, event,
                        SoundSource.BLOCKS, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not play sound '{}': {}", sound, e.getMessage());
        }
    }


    public void playSoundAtEntity(btw.modern.Entity entity, String sound,
                                  float volume, float pitch) {
        // A puppet's own hurt sound raised inside its onUpdate() is held back until FCEnvHurtGuard knows
        // whether the hit was FC's spurious inWall self-hit (dropped) or a real one (replayed) -- w1 P4.
        if (FCEnvHurtGuard.deferSelfSound(entity, sound, volume, pitch)) return;
        try {
            SoundEvent event = resolveSoundEvent(sound);
            if (event != null && entity != null) {
                // Use entity's FC position to play the sound at its location
                level.playSound(null, entity.posX, entity.posY, entity.posZ,
                        event, SoundSource.NEUTRAL, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not play sound '{}' at entity: {}",
                    sound, e.getMessage());
        }
    }

    @Override
    public void playSoundToNearExcept(EntityPlayer player, String sound,
                                      float volume, float pitch) {
        try {
            SoundEvent event = resolveSoundEvent(sound);
            if (event != null && player != null) {
                // Play sound to all nearby players EXCEPT the given player.
                // If the player is a PlayerBridge, use the real ServerPlayer
                // so level.playSound excludes them from hearing it.
                ServerPlayer excluded = null;
                if (player instanceof PlayerBridge pb) {
                    excluded = pb.getServerPlayer();
                }
                level.playSound(excluded, player.posX, player.posY, player.posZ,
                        event, SoundSource.PLAYERS, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not play sound '{}' near-except: {}",
                    sound, e.getMessage());
        }
    }


    // 1.5.2 World.spawnParticle — bridges FC particle names to ServerLevel.sendParticles.
    // Name set enumerated from FC spawnParticle callers (smoke, largesmoke, largeexplode,
    // reddust, snowballpoof, iconcrack_*, fcwhitesmoke/fcwhitecloud/fcsmallflame/fccinders, ...).
    // count=0 with speed=1 preserves the 1.5.2 single-particle-with-velocity semantics.
    public void spawnParticle(String particle, double x, double y, double z,
                              double velocityX, double velocityY, double velocityZ) {
        if (particle == null || particle.isEmpty()) return;
        try {
            // reddust encodes its color in the velocity args (1.5.2 EntityReddustFX:
            // red defaults to 1.0 when velocityX is 0)
            if (particle.equals("reddust")) {
                float red = velocityX == 0.0D ? 1.0F : (float) velocityX;
                net.minecraft.core.particles.DustParticleOptions dust =
                        new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(red, (float) velocityY, (float) velocityZ), 1.0F);
                level.sendParticles(dust, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                return;
            }
            net.minecraft.core.particles.ParticleOptions options = resolveParticleOptions(particle);
            if (options != null) {
                level.sendParticles(options, x, y, z, 0, velocityX, velocityY, velocityZ, 1.0D);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not spawn particle '{}': {}", particle, e.getMessage());
        }
    }

    /**
     * Maps a 1.5.2 particle name (as used by FC code) to a modern ParticleOptions.
     * Returns null for names with no modern equivalent.
     */
    private static net.minecraft.core.particles.ParticleOptions resolveParticleOptions(String name) {
        // iconcrack_<itemID> → item break particles for the legacy item ID
        if (name.startsWith("iconcrack_")) {
            try {
                int legacyItemId = Integer.parseInt(name.substring("iconcrack_".length()));
                net.minecraft.world.item.Item item = ProxyRegistry.getModernItem(legacyItemId);
                if (item != null) {
                    return new net.minecraft.core.particles.ItemParticleOption(
                            net.minecraft.core.particles.ParticleTypes.ITEM,
                            new net.minecraft.world.item.ItemStack(item));
                }
            } catch (NumberFormatException ignored) {}
            return null;
        }
        return switch (name) {
            case "smoke" -> net.minecraft.core.particles.ParticleTypes.SMOKE;
            case "largesmoke" -> net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
            case "largeexplode" -> net.minecraft.core.particles.ParticleTypes.EXPLOSION;
            case "hugeexplosion" -> net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER;
            case "explode" -> net.minecraft.core.particles.ParticleTypes.POOF;
            case "flame" -> net.minecraft.core.particles.ParticleTypes.FLAME;
            case "lava" -> net.minecraft.core.particles.ParticleTypes.LAVA;
            case "note" -> net.minecraft.core.particles.ParticleTypes.NOTE;
            case "portal" -> net.minecraft.core.particles.ParticleTypes.PORTAL;
            case "enchantmenttable" -> net.minecraft.core.particles.ParticleTypes.ENCHANT;
            case "townaura" -> net.minecraft.core.particles.ParticleTypes.MYCELIUM;
            case "dripWater" -> net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER;
            case "dripLava" -> net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA;
            case "bubble" -> net.minecraft.core.particles.ParticleTypes.BUBBLE;
            case "splash" -> net.minecraft.core.particles.ParticleTypes.SPLASH;
            // snowshovel/snowballpoof both rendered snowball puffs in 1.5.2
            case "snowshovel", "snowballpoof" -> net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL;
            case "slime" -> net.minecraft.core.particles.ParticleTypes.ITEM_SLIME;
            case "heart" -> net.minecraft.core.particles.ParticleTypes.HEART;
            case "angryVillager" -> net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER;
            case "happyVillager" -> net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER;
            case "witchMagic" -> net.minecraft.core.particles.ParticleTypes.WITCH;
            case "mobSpell" -> net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT;
            case "mobSpellAmbient" -> net.minecraft.core.particles.ParticleTypes.AMBIENT_ENTITY_EFFECT;
            case "spell" -> net.minecraft.core.particles.ParticleTypes.EFFECT;
            case "instantSpell" -> net.minecraft.core.particles.ParticleTypes.INSTANT_EFFECT;
            case "crit" -> net.minecraft.core.particles.ParticleTypes.CRIT;
            case "magicCrit" -> net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT;
            // FC custom particles — closest modern visual equivalents
            case "fcwhitesmoke", "fcwhitecloud" -> net.minecraft.core.particles.ParticleTypes.CLOUD;
            case "fcsmallflame" -> net.minecraft.core.particles.ParticleTypes.SMALL_FLAME;
            case "fccinders" -> net.minecraft.core.particles.ParticleTypes.ASH;
            default -> null;
        };
    }

    // 1.5.2 World.playAuxSFX (vanilla/server World.java:3912-3915) = playAuxSFXAtEntity(null, ...): no actor.
    public void playAuxSFX(int effectID, int x, int y, int z, int data) {
        playAuxSFXAtEntity(null, effectID, x, y, z, data);
    }

    /** First FC custom aux-FX id (FCBetterThanWolves.m_iAnimalBirthingAuxFXID; the ids run to 2285). */
    private static final int FIRST_FC_AUX_SFX_ID = 2222;

    /** 1.5.2 ids already reported as having no 1.20.1 equivalent (logged once each). */
    private static final java.util.Set<Integer> DROPPED_AUX_SFX = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * A 1.5.2 vanilla aux-SFX (id &lt; 2222; vanilla/client RenderGlobal.playAuxSFX :2169-2399), translated to
     * its 1.20.1 level event by {@link AuxSfxMapping}. The ids are numbered differently in 1.20.1 and used to
     * be forwarded raw: every fc_bat takeoff (1015, a 0.05-volume flutter) played GHAST_WARN at volume 10 to
     * everyone within 64 blocks (triage GH1), a zombie infecting / curing a villager (1016 / 1017) a ghast /
     * dragon fireball (GH3), a torch fizz (1004) a firework launch. 1.5.2 effects with no 1.20.1 event are
     * played here (1003 door) or dropped -- never passed raw. {@code except} (null: nobody) is left out like
     * the actor of 1.5.2 WorldManager.playAuxSFX (vanilla/server WorldManager.java:103-106).
     */
    private void playLegacyAuxSFX(ServerPlayer except, int effectID, int x, int y, int z, int data) {
        try {
            BlockPos pos = new BlockPos(x, y, z);
            int modern = AuxSfxMapping.toModernEvent(effectID, data);
            int modernData = modern != AuxSfxMapping.NONE ? AuxSfxMapping.toModernData(effectID, data) : 0;
            if (modern == 2001) {
                int live = liveBlockStateId(pos, data);
                if (live != AuxSfxMapping.INVALID_DATA) {
                    modernData = live;
                }
            }
            if (modern != AuxSfxMapping.NONE && modernData != AuxSfxMapping.INVALID_DATA) {
                level.levelEvent(except, modern, pos, modernData);
            }
            double px = x + 0.5D, py = y + 0.5D, pz = z + 0.5D;
            switch (effectID) {
                case 1003: // door open/close, picked at random (RenderGlobal.java:2198-2208); no 1.20.1 event
                    playSoundEffectExcept(except, px, py, pz,
                            rand.nextBoolean() ? "random.door_open" : "random.door_close",
                            1.0F, rand.nextFloat() * 0.1F + 0.9F);
                    break;
                case 1016: // FCMOD: extra slow zombie moan on villager infection (RenderGlobal.java:2260-2263)
                    playSoundEffectExcept(except, px, py, pz, "mob.zombie.say", 1.0F + rand.nextFloat(),
                            rand.nextFloat() * 0.2F + 0.5F);
                    break;
                case 2001: // block 1.20.1 cannot express: no particles, but still its FC break sound
                    if (modernData == AuxSfxMapping.INVALID_DATA) {
                        playFcBlockSound(except, px, py, pz, data & 4095, true);
                    }
                    break;
                default:
                    if (modern == AuxSfxMapping.NONE && DROPPED_AUX_SFX.add(effectID)) {
                        LOGGER.debug("1.5.2 aux-SFX {} has no 1.20.1 level event; dropped", effectID);
                    }
                    break;
            }
        } catch (Throwable t) {
            if (DROPPED_AUX_SFX.add(-effectID - 1)) {   // negative key: "failed", logged once per id
                LOGGER.warn("aux-SFX {} at {},{},{} failed: {}", effectID, x, y, z, t.toString());
            }
        }
    }

    /**
     * 1.20.1 state id of the block still at {@code pos} when it is the block a 2001 payload names (legacy id
     * {@code data & 4095}, metadata {@code data >> 12}), else {@link AuxSfxMapping#INVALID_DATA}. 1.5.2 raises
     * 2001 before the block goes (World.destroyBlock, vanilla/server World.java:604-611; ItemInWorldManager
     * :432 / :458), and the live state carries what the canonical id + meta lookup cannot: the 1.20.1 variant
     * (new woods, crop age, stair shape) and the post-1.5.2 block behind a ModernBlockStandIn id.
     */
    private int liveBlockStateId(BlockPos pos, int data) {
        BlockState live = level.getBlockState(pos);
        if (live.isAir()
                || ProxyRegistry.getWorldBlockId(live, level, pos) != (data & 4095)
                || (getBlockMetadata(pos.getX(), pos.getY(), pos.getZ()) & 15) != (data >> 12 & 15)) {
            return AuxSfxMapping.INVALID_DATA;
        }
        return net.minecraft.world.level.block.Block.getId(live);
    }

    /**
     * Sends an FC custom aux-FX (ids &gt;= 2222) to the clients, where FC's own
     * FCAddOnHandler.ClientPlayCustomAuxFX plays it (exact sounds, data-dependent branches, particles), as the
     * 1.5.2 server did with Packet61 (WorldManager.playAuxSFX: 64 blocks around, all but the actor;
     * broadcastSound for func_82739_e: every player, vanilla/server WorldManager.java:103-111; client dispatch
     * vanilla/client RenderGlobal.java:2159 / :2394). This replaces the hand-copied server table
     * (playFcAuxSFX), which had drifted from FC's handler (wrong sounds / volumes / pitches, missing sounds, no
     * particles; audit S3).
     */
    private void sendFcAuxSfx(ServerPlayer except, int effectID, int x, int y, int z, int data, boolean global) {
        try {
            BTWNetwork.sendFcAuxSfx(level, except, effectID, x, y, z, data, global);
        } catch (Throwable t) {
            if (DROPPED_AUX_SFX.add(-effectID - 1)) {   // negative key: "failed", logged once per id
                LOGGER.warn("FC aux-SFX {} at {},{},{} could not be sent: {}", effectID, x, y, z, t.toString());
            }
        }
    }

    /**
     * Plays a block's break/step sound for a 2001 whose block 1.20.1 cannot express (FC's
     * {@code block.stepSound.getBreakSound()/getStepSound()} at (v+1)/2, p*0.8 like RenderGlobal 2001).
     */
    private void playFcBlockSound(ServerPlayer except, double px, double py, double pz, int blockId, boolean breakSound) {
        try {
            if (blockId > 0 && blockId < btw.modern.Block.blocksList.length) {
                btw.modern.Block blk = btw.modern.Block.blocksList[blockId];
                if (blk != null && blk.stepSound != null) {
                    String s = breakSound ? blk.stepSound.getBreakSound() : blk.stepSound.getStepSound();
                    playSoundEffectExcept(except, px, py, pz, s,
                            (blk.stepSound.getVolume() + 1.0F) / 2.0F, blk.stepSound.getPitch() * 0.8F);
                }
            }
        } catch (Throwable ignored) {}
    }

    /** The ServerPlayer behind an FC player (null for none / not a server player). */
    private static ServerPlayer serverPlayerOf(EntityPlayer player) {
        return player instanceof PlayerBridge pb ? pb.getServerPlayer() : null;
    }

    // 1.5.2 World.playAuxSFXAtEntity (vanilla/server World.java:3920-3939) -> WorldManager.playAuxSFX: Packet61
    // to everyone within 64 blocks EXCEPT `player`, whose client already played the event. That exclusion is
    // honoured again where the actor's 1.20.1 client really played it: ItemInWorldManager.tryHarvestBlock's
    // 2001, whose breaker's client already played the break (MultiPlayerGameMode.destroyBlock ->
    // playerWillDestroy). An event raised at a ProxyBlock is not predicted (ProxyBlock.use & co. run FC on the
    // server only), so its actor is kept: the FC wood door's 1003 (FCBlockDoor.onBlockActivated, Client
    // FCBlockDoor.java:63; block 1036), which the 1.5.2 client played from its own onBlockActivated, would
    // otherwise be silent for the player who opened it. FC ids go to FC's client handler, vanilla ids through
    // AuxSfxMapping.
    @Override
    public void playAuxSFXAtEntity(EntityPlayer player, int effectID,
                                   int x, int y, int z, int data) {
        ServerPlayer except = null;
        try {
            except = serverPlayerOf(player);
            if (except != null && effectID != 2001
                    && level.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof ProxyBlock) {
                except = null;
            }
        } catch (Throwable ignored) {}
        if (effectID >= FIRST_FC_AUX_SFX_ID) {
            sendFcAuxSfx(except, effectID, x, y, z, data, false);
            return;
        }
        playLegacyAuxSFX(except, effectID, x, y, z, data);
    }

    // 1.5.2 World.func_82739_e (vanilla/server World.java:3900-3906) -> WorldManager.broadcastSound: Packet61
    // (global) to every player regardless of distance. Live callers: FCEntityAIWolfHowl.java:117 /
    // FCEntityAIWolfDireHowl.java:78 (howls), FCEntityWolf.java:895 (dire conversion),
    // FCEntityLightningBolt.java:83 (strike FX): FC ids, played by FC's client handler (whose howl volume
    // depends on the local player's height, FCBetterThanWolves.java:3466-3480).
    @Override
    public void func_82739_e(int effectID, int x, int y, int z, int data) {
        if (effectID >= FIRST_FC_AUX_SFX_ID) {
            sendFcAuxSfx(null, effectID, x, y, z, data, true);
            return;
        }
        try {
            // Vanilla broadcast IDs: 1.5.2 RenderGlobal.broadcastSound plays only 1013 (wither spawn)
            // and 1018 (dragon death), which 1.20.1 numbers 1023 / 1028; a global level event reaches
            // every player like broadcastSound. Any other id played nothing in 1.5.2 (and was
            // forwarded raw here before).
            int global = AuxSfxMapping.toModernGlobalEvent(effectID);
            if (global != AuxSfxMapping.NONE) {
                level.globalLevelEvent(global, new BlockPos(x, y, z), data);
            }
        } catch (Throwable t) {
            if (DROPPED_AUX_SFX.add(-effectID - 1)) {
                LOGGER.warn("global aux-SFX {} at {},{},{} failed: {}", effectID, x, y, z, t.toString());
            }
        }
    }

    // ================================================================
    // Weather
    // ================================================================

    
    public boolean isRaining() {
        return level.isRaining();
    }


    public boolean isThundering() {
        return level.isThundering();
    }

    @Override
    public float getRainStrength(float partialTicks) {
        return level.getRainLevel(partialTicks);
    }

    @Override
    public float getWeightedThunderStrength(float partialTicks) {
        return level.getThunderLevel(partialTicks);
    }

    @Override
    public boolean IsRainingAtPos(int i, int j, int k) {
        return level.isRainingAt(new net.minecraft.core.BlockPos(i, j, k));
    }

    @Override
    public boolean IsSnowingAtPos(int i, int j, int k) {
        if (!isRaining()) return false;
        if (!canBlockSeeTheSky(i, j, k)) return false;
        if (getPrecipitationHeight(i, k) > j) return false;
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(i, j, k);
        // FCMOD-INTEGRATION(1.20.1): climate_seams: a registered live climate (a season mod) decides snow here, so FC's snow reversion and cover follow the seasons; 1.20.1's verdict without one
        return btw.forge.climate.BTWClimate.coldEnoughToSnow(level, level.getBiome(pos), pos);
    }

    @Override
    public boolean IsPrecipitatingAtPos(int i, int j, int k) {
        return IsRainingAtPos(i, j, k) || IsSnowingAtPos(i, j, k);
    }

    // ================================================================
    // Redstone
    // ================================================================

    // Only these two 1.5.2 power queries exist on btw.modern.World (getBlockPowerInput, isBlockProvidingPowerTo,
    // getIndirectPowerLevelTo and getStrongestIndirectPower have no declaration and no FC/frozen caller); each
    // maps to the 1.20.1 SignalGetter query with the same body. Side order and indices agree: 1.5.2 sides 0-5
    // (below, above, z-1, z+1, x-1, x+1) == Direction DOWN, UP, NORTH, SOUTH, WEST, EAST get3DDataValue, and
    // ProxyBlock.getDirectSignal / getSignal forward FC isProvidingStrongPower / isProvidingWeakPower with it.

    // FCMOD World.isBlockGettingPowered (vanilla/server World.java:4157-4160) = getBlockPowerInput > 0: the
    // strongest isProvidingStrongPower of the six neighbours (:3452-3510), i.e. STRONG power only ==
    // SignalGetter.getDirectSignalTo. It was hasNeighborSignal (weak / relayed power), so the redstone clutch,
    // pulley, block dispenser and turntable reacted to redstone blocks and powered conductors that 1.5.2
    // ignores (prover D6: a redstone block under a clutch kept it disengaged).
    @Override
    public boolean isBlockGettingPowered(int x, int y, int z) {
        return level.getDirectSignalTo(new BlockPos(x, y, z)) > 0;
    }

    // World.isBlockIndirectlyGettingPowered (vanilla/server World.java:3558-3561) = any of the six
    // getIndirectPowerLevelTo(neighbour, side) > 0 (:3525-3552: weak power, plus the strong power a normal cube
    // relays -- FCMOD: a cube that is itself a power source gives the larger of the two) ==
    // SignalGetter.hasNeighborSignal (getSignal = max(weak, conductor ? getDirectSignalTo : 0)), same order.
    @Override
    public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) {
        return level.hasNeighborSignal(new BlockPos(x, y, z));
    }

    // ================================================================
    // Solid top surface
    // ================================================================

    
    public boolean doesBlockHaveSolidTopSurface(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    // ================================================================
    // Player
    // ================================================================


    public boolean canMineBlock(EntityPlayer player, int x, int y, int z) {
        // Check player permissions via the real ServerPlayer if available
        if (player instanceof PlayerBridge pb) {
            ServerPlayer sp = pb.getServerPlayer();
            if (sp != null) {
                BlockPos pos = new BlockPos(x, y, z);
                if (sp.isSpectator()) return false;
                if (!sp.mayBuild()) return false;
                if (sp.server.isUnderSpawnProtection(level, pos, sp)) return false;
            }
            return true;
        }
        return true;
    }


    public EntityPlayer getClosestPlayer(double x, double y, double z, double range) {
        // Delegate to modern level -- range<0 means unlimited
        net.minecraft.world.entity.player.Player nearest =
                level.getNearestPlayer(x, y, z, range, false);
        if (nearest instanceof ServerPlayer sp) {
            return PlayerBridge.getOrCreate(sp);
        }
        return null;
    }

    @Override
    public EntityPlayer getClosestPlayerToEntity(btw.modern.Entity entity, double range) {
        if (entity == null) return null;
        return getClosestPlayer(entity.posX, entity.posY, entity.posZ, range);
    }

    @Override
    public EntityPlayer getPlayerEntityByName(String name) {
        // Called every AI tick by tamed-animal owner lookups (empty name for
        // untamed) — keep the miss path cheap and allocation-free.
        if (name == null || name.isEmpty()) return null;
        for (ServerPlayer player : level.players()) {
            if (name.equals(player.getGameProfile().getName())) {
                return PlayerBridge.getOrCreate(player);
            }
        }
        return null;
    }

    @Override
    public EntityPlayer getClosestVulnerablePlayerToEntity(btw.modern.Entity entity, double range) {
        if (entity == null) return null;
        return getClosestVulnerablePlayer(entity.posX, entity.posY, entity.posZ, range);
    }

    @Override
    public EntityPlayer getClosestVulnerablePlayer(double x, double y, double z, double range) {
        // "Vulnerable" in FC means not in creative/spectator mode
        net.minecraft.world.entity.player.Player nearest =
                level.getNearestPlayer(x, y, z, range, true); // true = ignore creative
        if (nearest instanceof ServerPlayer sp) {
            // Also exclude spectators
            if (sp.isSpectator()) return null;
            return PlayerBridge.getOrCreate(sp);
        }
        return null;
    }

    // ================================================================
    // Entity lookup
    // ================================================================

    @Override
    public btw.modern.Entity getEntityByID(int id) {
        net.minecraft.world.entity.Entity mcEntity = level.getEntity(id);
        if (mcEntity == null) return null;
        // Every proxy type, players, and dropped items (their FC EntityItem view: 1.5.2 World.getEntityByID
        // resolved EntityItems like any entity)
        return extractFcEntityOrItem(mcEntity);
    }

    // 1.5.2 World.CountEntitiesThatApplyToSpawnCap (BTW-patched vanilla/server
    // World.java:4634) — FCTileEntityBeacon.CheckForEndermanSpawn (:584/:649)
    // population cap. MC owns the entity list, so iterate the live level and
    // unwrap FC entities from their proxies.
    @Override
    public int CountEntitiesThatApplyToSpawnCap(Class classToCount) {
        int iEntityCount = 0;

        for (net.minecraft.world.entity.Entity forgeEntity : level.getAllEntities()) {
            btw.modern.Entity tempEntity = extractFcEntity(forgeEntity);

            if (tempEntity != null && tempEntity.DoesEntityApplyToSpawnCap()
                    && classToCount.isAssignableFrom(tempEntity.getClass())) {
                ++iEntityCount;
            }
        }

        return iEntityCount;
    }

    // 1.5.2 World fields backing GetNumEntitiesThatApplyToSquidPossessionCap's
    // once-per-tick cache (vanilla/server World.java:4655).
    private long m_lTimeOfLastSquidPossessionCapCount = -1L;
    private int m_iLastSquidPossessionCapCount = 0;

    // 1.5.2 World.GetNumEntitiesThatApplyToSquidPossessionCap (vanilla/server
    // World.java:4655) — FCEntitySquid.GetCanCreatureBePossessedFromDistance (:630)
    // possessed-squid performance cap.
    @Override
    public int GetNumEntitiesThatApplyToSquidPossessionCap() {
        long lCurrentTime = getWorldTime();

        if (lCurrentTime != m_lTimeOfLastSquidPossessionCapCount) {
            m_iLastSquidPossessionCapCount = 0;
            m_lTimeOfLastSquidPossessionCapCount = lCurrentTime;

            for (net.minecraft.world.entity.Entity forgeEntity : level.getAllEntities()) {
                btw.modern.Entity tempEntity = extractFcEntity(forgeEntity);

                if (tempEntity != null && tempEntity.DoesEntityApplyToSquidPossessionCap()) {
                    m_iLastSquidPossessionCapCount++;
                }
            }
        }

        return m_iLastSquidPossessionCapCount;
    }

    // Cached reflective handles for net.minecraft.src.btw.util.FCClosestEntityInfo /
    // FCClosestEntitySelectionCriteria — FC-only classes that are not on the Forge
    // compile classpath (same reflective FC access pattern as BTWLifecycle).
    private static java.lang.reflect.Constructor<?> closestEntityInfoCtor;
    private static java.lang.reflect.Method closestEntityProcessMethod;
    private static java.lang.reflect.Field closestEntityField;

    // 1.5.2 World.GetClosestEntityMatchingCriteriaWithinRange (BTW-patched
    // vanilla/server World.java:4595) — FCEntitySquid.java:849 staggered secondary
    // possession-target scan. MC owns the chunk entity lists, so iterate live
    // entities in range and apply the FC criteria object's ProcessEntity
    // (selection logic stays in FC code, exactly as Chunk.
    // GetClosestEntityMatchingCriteriaWithinRangeSq does per entity).
    @Override
    public btw.modern.Entity GetClosestEntityMatchingCriteriaWithinRange(
            double dSourcePosX, double dSourcePosY, double dSourcePosZ,
            double dRange, Object criteria) {
        if (criteria == null) return null;
        try {
            if (closestEntityInfoCtor == null) {
                Class<?> infoClass = Class.forName("net.minecraft.src.btw.util.FCClosestEntityInfo");
                Class<?> criteriaClass = Class.forName("net.minecraft.src.btw.util.FCClosestEntitySelectionCriteria");
                closestEntityInfoCtor = infoClass.getConstructor(
                        double.class, double.class, double.class, double.class,
                        btw.modern.Entity.class, criteriaClass, int.class, int.class);
                closestEntityProcessMethod = criteriaClass.getMethod(
                        "ProcessEntity", infoClass, btw.modern.Entity.class);
                closestEntityField = infoClass.getField("m_closestEntity");
            }

            // m_dClosestDistanceSq starts at range² so ProcessEntity rejects anything
            // out of range; the vertical section indices (0,15) are only chunk-list
            // bookkeeping in vanilla and unused by the AABB iteration below.
            Object closestEntityInfo = closestEntityInfoCtor.newInstance(
                    dSourcePosX, dSourcePosY, dSourcePosZ, dRange * dRange,
                    null, criteria, 0, 15);

            net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
                    dSourcePosX - dRange, dSourcePosY - dRange, dSourcePosZ - dRange,
                    dSourcePosX + dRange, dSourcePosY + dRange, dSourcePosZ + dRange);

            for (net.minecraft.world.entity.Entity forgeEntity :
                    level.getEntities((net.minecraft.world.entity.Entity) null, searchBox, e -> true)) {
                btw.modern.Entity fc = extractFcEntity(forgeEntity);
                if (fc != null) {
                    closestEntityProcessMethod.invoke(criteria, closestEntityInfo, fc);
                }
            }

            return (btw.modern.Entity) closestEntityField.get(closestEntityInfo);
        } catch (Exception e) {
            warnOncePerThrowSite("CLOSEST-ENTITY", "GetClosestEntityMatchingCriteriaWithinRange", e);
            return null;
        }
    }

    // ================================================================
    // Entity state
    // ================================================================

    @Override
    public void setEntityState(btw.modern.Entity entity, byte state) {
        if (entity == null) return;
        // Drop the hurt flash (event 2) FC raises on a puppet inside its OWN onUpdate() -- that's
        // its spurious self-suffocation (isEntityInsideOpaqueBlock false-fires on FC-driven
        // puppets). Hits it lands on other entities pass the victim (!= updater) and still flash,
        // and real combat runs outside onUpdate, so only self-inflicted flashes are swallowed.
        if (state == 2 && FCEnvHurtGuard.isSelfHurt(entity)) return;
        net.minecraft.world.entity.Entity proxy = proxyFor(entity);
        if (proxy != null) {
            level.broadcastEntityEvent(proxy, state);
        }
    }

    /**
     * The MC entity standing for an FC entity: the proxy spawnEntityInWorld registered, or -- for the
     * puppets VanillaMobReplacer / MC spawning / chunk loading created, which never pass through
     * spawnEntityInWorld -- the level entity with the FC entity's id whose FC entity is this very object
     * (the proxies keep fcEntity.entityId == getId() every tick; the identity check rejects id collisions
     * with FC's own counter). Players are left out: their events come from MC itself. Null if none.
     */
    private net.minecraft.world.entity.Entity proxyFor(btw.modern.Entity entity) {
        if (entity == null || level == null) return null;
        net.minecraft.world.entity.Entity proxy = fcToForgeEntity.get(entity);
        if (proxy == null && entity.entityId != 0) {
            net.minecraft.world.entity.Entity byId = level.getEntity(entity.entityId);
            if (byId != null && !(byId instanceof net.minecraft.world.entity.player.Player)
                    && extractFcEntity(byId) == entity) {
                proxy = byId;
            }
        }
        return proxy;
    }

    // ================================================================
    // Explosion
    // ================================================================

    
    // 1.5.2 World.createExplosion(entity, x, y, z, size, flag) == newExplosion(entity, x, y, z, size, false, flag)
    // (vanilla/server World.java:2287-2290): the flag is isSmoking -- block damage, never fire. Creepers pass
    // the mobGriefing gamerule as that flag (EntityCreeper.java:153-160). It used to be stored as isFlaming.
    public Explosion createExplosion(btw.modern.Entity entity, double x, double y, double z,
                                     float strength, boolean isSmoking) {
        return newExplosion(entity, x, y, z, strength, false, isSmoking);
    }


    // 1.5.2 World.newExplosion: isFlaming sets fires, isSmoking destroys blocks (Explosion.doExplosionB).
    // The FC entity's proxy is the MC source, so deaths read "blown up by Creeper" and the exploder is not
    // hurt by its own blast (1.5.2 Explosion.doExplosionA excludes the exploder too) -- triage HAI-7.
    public Explosion newExplosion(btw.modern.Entity entity, double x, double y, double z,
                                  float strength, boolean isFlaming, boolean isSmoking) {
        try {
            net.minecraft.world.entity.Entity source = proxyFor(entity);
            level.explode(source, x, y, z, strength, isFlaming, explosionInteraction(source, isSmoking));
        } catch (Throwable t) {
            warnOncePerThrowSite("EXPLOSION", "explosion at " + x + "," + y + "," + z + " (size " + strength + ")", t);
        }
        Explosion fcExplosion = new Explosion(this, entity, x, y, z, strength);
        fcExplosion.isFlaming = isFlaming;
        fcExplosion.isSmoking = isSmoking;
        return fcExplosion;
    }

    /**
     * 1.20.1 interaction for a 1.5.2 isSmoking flag. false -> NONE (no block damage, e.g. a creeper or a
     * wither skull with mobGriefing off). true from a mob (creeper, wither) -> MOB: the 1.5.2 caller already
     * read mobGriefing, and MOB re-checks it through Forge's EntityMobGriefingEvent like a 1.20.1 creeper.
     * true from anything else -> BLOCK, as before: FC dynamite, blasting oil, vase, cooking vessel and TNT
     * always destroy blocks in 1.5.2, independent of mobGriefing (ghast fireballs and wither skulls pass the
     * gamerule themselves).
     */
    private static net.minecraft.world.level.Level.ExplosionInteraction explosionInteraction(
            net.minecraft.world.entity.Entity source, boolean isSmoking) {
        if (!isSmoking) return net.minecraft.world.level.Level.ExplosionInteraction.NONE;
        return source instanceof net.minecraft.world.entity.Mob
                ? net.minecraft.world.level.Level.ExplosionInteraction.MOB
                : net.minecraft.world.level.Level.ExplosionInteraction.BLOCK;
    }

    // 1.5.2 World.NewExplosionNoFX (BTW-patched vanilla/server World.java:4902) —
    // FCBlockLogSmouldering.Explode (FCBlockLogSmouldering.java:389); copy of
    // newExplosion with FX suppressed. Built directly instead of level.explode so
    // no ClientboundExplodePacket (sound/particles) is sent — entity damage, fire
    // (isFlaming) and block destruction (isSmoking) still apply, matching
    // doExplosionA()/doExplosionB(false).
    @Override
    public Explosion NewExplosionNoFX(btw.modern.Entity entity, double x, double y, double z,
                                      float strength, boolean isFlaming, boolean isSmoking) {
        net.minecraft.world.entity.Entity source = proxyFor(entity);
        net.minecraft.world.level.Explosion explosion = new net.minecraft.world.level.Explosion(
                level, source, x, y, z, strength, isFlaming,
                isSmoking ? net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
                          : net.minecraft.world.level.Explosion.BlockInteraction.KEEP);
        explosion.explode();
        explosion.finalizeExplosion(false);

        Explosion fcExplosion = new Explosion(this, entity, x, y, z, strength);
        fcExplosion.isFlaming = isFlaming;
        fcExplosion.isSmoking = isSmoking;
        fcExplosion.m_bSuppressFX = true;
        return fcExplosion;
    }

    // ================================================================
    // Time
    // ================================================================

    
    public long getWorldTime() {
        return level.getDayTime();
    }

    
    public void setWorldTime(long time) {
        level.setDayTime(time);
    }

    
    public long getTotalWorldTime() {
        return level.getGameTime();
    }

    @Override
    public float getCelestialAngle(float partialTicks) {
        return level.getTimeOfDay(partialTicks);
    }

    @Override
    public boolean isDaytime() {
        return level.isDay();
    }

    // 1.5.2 World.getMoonPhase (vanilla/server World.java:1576) — FCEntityWolf.
    // IsWildAndHostile full-moon hostility (phase 0), FCEntityAIWolfHowl howl trigger,
    // frozen EntitySlime.getCanSpawnHere; MC 1.20.1 uses the same (time/24000 % 8)
    // numbering with 0 = full moon.
    @Override
    public int getMoonPhase() {
        return level.getMoonPhase();
    }

    // ================================================================
    // Spawn point
    // ================================================================

    
    public ChunkCoordinates getSpawnPoint() {
        BlockPos sp = level.getSharedSpawnPos();
        return new ChunkCoordinates(sp.getX(), sp.getY(), sp.getZ());
    }

    // ================================================================
    // World height
    // ================================================================

    // 1.5.2 World.getHeight / getActualHeight are the TOP of the build range (256, and 128 under a no-sky
    // provider's roof; vanilla/server World.java:3944-3955, the bottom was always 0). Level.getHeight is the
    // range's SIZE (384 in the overworld, -64..320), which put the top 64 layers above the world
    // (FCTileEntityBeacon enderman targets, FCEntityGhast destinations).
    public int getHeight() {
        return level.getMaxBuildHeight();
    }

    
    public int getActualHeight() {
        int top = level.getMaxBuildHeight();
        return provider != null && provider.hasNoSky ? Math.min(128, top) : top;
    }

    // Build-range contract with Modern-Common World.getFcMinBuildHeight / getFcMaxBuildHeight (1.5.2: 0 / 256),
    // which the bridge's frozen-code consumers (pathing below y=0, deepslate caves, superflat at -60) read
    // instead of the hard-coded 1.5.2 range. Block reads here already go straight to the level
    // (Level.getBlockState answers VOID_AIR outside its build range), so no 0..256 clamp applies to them.
    // Null-safe: detach() clears the level when the server stops.
    public int getFcMinBuildHeight() {
        ServerLevel live = level;
        return live != null ? live.getMinBuildHeight() : 0;
    }

    public int getFcMaxBuildHeight() {
        ServerLevel live = level;
        return live != null ? live.getMaxBuildHeight() : 256;
    }

    // FC's falling-block floor (Modern-Common Block.CheckForFall) reads the same contract: World.GetMinBuildHeight is
    // an alias of getFcMinBuildHeight in the base class, so it is not overridden here.

    /**
     * Modern-Common World.GetModernSoilPlantSurvival (maintainer decision D3), asked by BlockFlower.checkFlowerChange
     * for the FC plant at (x, y, z): when the block below has no 1.5.2 identity (a modern-only block that FC reads as
     * a ModernBlockStandIn, water-holding plant or air: moss, podzol, nylium, mud, rooted dirt, red sand, terracotta,
     * mangrove roots, ...), 1.20.1 put the plant there and its own survival rule decides: {@code canSurvive} of the
     * live state, 1 to stay or 0 to go. On a 1.5.2 block (a mapped vanilla id or an FC block) it answers -1 and FC's
     * 1.5.2 canBlockStay decides. Without it FC's light rule (BlockFlower.canBlockStay: light 8 or sky) removed the
     * grass 1.20.1 grows on moss in dark lush caves (CaveFeatures MOSS_VEGETATION), and FCBlockMushroom's (light
     * below 13) the mushrooms 1.20.1 keeps on podzol and nylium in full light (MushroomBlock.canSurvive). The live
     * rule asks Block.canSustainPlant of the modern soil, which PlantSoilMixin leaves to 1.20.1. FC's own plants
     * (ProxyBlocks) are never placed by 1.20.1, so FC's rule keeps deciding for them (-1).
     */
    @Override
    public int GetModernSoilPlantSurvival(int x, int y, int z) {
        ServerLevel live = level;
        if (live == null) return -1;
        try {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState soil = live.getBlockState(pos.below());
            if (ProxyRegistry.getBlockId(soil.getBlock()) != 0) return -1;
            BlockState plant = live.getBlockState(pos);
            if (plant.getBlock() instanceof ProxyBlock) return -1;
            return plant.canSurvive(live, pos) ? 1 : 0;
        } catch (Throwable t) {
            // Runs inside FC plant ticks and neighbour changes: never throw out of them; FC's rule decides instead.
            warnOncePerThrowSite("MODERN-SOIL", "the live survival check of the plant at " + x + "," + y + "," + z, t);
            return -1;
        }
    }

    /**
     * Modern-Common World.DropModernBlockLoot: the 1.20.1 loot of the live block behind an FC plant twin
     * (btw.modern.ModernTwins), where FC's code drops the twin (BlockFlower.checkFlowerChange, the saw, FC pistons).
     * The caller removes the block, as FC removes its own plants after dropping them.
     */
    @Override
    public void DropModernBlockLoot(int x, int y, int z) {
        ServerLevel live = level;
        if (live == null) return;
        try {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = live.getBlockState(pos);
            if (!state.isAir()) {
                net.minecraft.world.level.block.Block.dropResources(state, live, pos, live.getBlockEntity(pos));
            }
        } catch (Throwable t) {
            // Runs inside FC plant ticks: never throw out of them.
            warnOncePerThrowSite("TWIN-LOOT", "the loot of the plant at " + x + "," + y + "," + z, t);
        }
    }

    public static boolean isCollapsedFlower(BlockState state) {
        return state.getBlock() != Blocks.POPPY && ProxyRegistry.getBlockId(state.getBlock()) == 38;
    }

    @Override
    public boolean UsesModernBlockLoot(int x, int y, int z, int blockID) {
        ServerLevel live = level;
        if (live == null || blockID <= 0) return false;
        try {
            BlockPos pos = new BlockPos(x, y, z);
            if (!live.isLoaded(pos)) return false;
            BlockState state = live.getBlockState(pos);
            return (VanillaPairDrops.isRuled(state) || isCollapsedFlower(state))
                    && ProxyRegistry.getBlockId(state.getBlock()) == blockID;
        } catch (Throwable t) {
            warnOncePerThrowSite("PAIR-LOOT", "the pair-drop check at " + x + "," + y + "," + z, t);
            return false;
        }
    }

    // ================================================================
    // Block events
    // ================================================================

    
    public void addBlockEvent(int x, int y, int z, int blockID, int eventID, int eventParam) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block modernBlock = modernBlockAt(pos, blockID);
        if (modernBlock != null) {
            level.blockEvent(pos, modernBlock, eventID, eventParam);
        }
    }

    // ================================================================
    // Game rules
    // ================================================================

    /**
     * Lazy-initialised wrapper that delegates btw.modern.GameRules methods
     * to the real MC 1.20.1 GameRules stored in the ServerLevel.
     */
    private GameRules gameRulesBridge;


    public GameRules getGameRules() {
        if (gameRulesBridge == null) {
            gameRulesBridge = new GameRules() {
                @Override
                public String getGameRuleStringValue(String name) {
                    net.minecraft.world.level.GameRules.Key<? extends net.minecraft.world.level.GameRules.Value<?>> key =
                            findRuleKey(name);
                    if (key != null) {
                        return level.getGameRules().getRule(key).toString();
                    }
                    return "";
                }

                @Override
                public boolean getGameRuleBooleanValue(String name) {
                    net.minecraft.world.level.GameRules.Key<BooleanValue> key =
                            findBooleanRuleKey(name);
                    if (key != null) {
                        return level.getGameRules().getBoolean(key);
                    }
                    return false;
                }

                @Override
                public boolean hasRule(String name) {
                    return findRuleKey(name) != null;
                }

                /**
                 * Finds a GameRules key by name. MC 1.20.1 uses typed keys
                 * so we check the well-known set.
                 */
                private net.minecraft.world.level.GameRules.Key<? extends net.minecraft.world.level.GameRules.Value<?>> findRuleKey(String name) {
                    return switch (name) {
                        case "doFireTick" -> net.minecraft.world.level.GameRules.RULE_DOFIRETICK;
                        case "mobGriefing" -> net.minecraft.world.level.GameRules.RULE_MOBGRIEFING;
                        case "keepInventory" -> net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY;
                        case "doMobSpawning" -> net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING;
                        case "doMobLoot" -> net.minecraft.world.level.GameRules.RULE_DOMOBLOOT;
                        case "doTileDrops" -> net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS;
                        case "commandBlockOutput" -> net.minecraft.world.level.GameRules.RULE_COMMANDBLOCKOUTPUT;
                        case "naturalRegeneration" -> net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION;
                        case "doDaylightCycle" -> net.minecraft.world.level.GameRules.RULE_DAYLIGHT;
                        // doWeatherCycle not in MC 1.5.2 FC, skip
                        case "showDeathMessages" -> net.minecraft.world.level.GameRules.RULE_SHOWDEATHMESSAGES;
                        default -> null;
                    };
                }

                @SuppressWarnings("unchecked")
                private net.minecraft.world.level.GameRules.Key<BooleanValue> findBooleanRuleKey(String name) {
                    var key = findRuleKey(name);
                    if (key != null) {
                        // All the rules mapped above are BooleanValue rules
                        return (net.minecraft.world.level.GameRules.Key<BooleanValue>) key;
                    }
                    return null;
                }
            };
        }
        return gameRulesBridge;
    }

    // ================================================================
    // FC-only data holders (magnetic points, spawn locations, beacons)
    // ================================================================

    /**
     * Initialises the FC-only list fields that FC code expects to be
     * non-null on a World instance.  Called lazily / from the constructor.
     */
    private void initFcDataHolders() {
        if (m_MagneticPointList == null) {
            m_MagneticPointList = new FCMagneticPointList();
        }
        if (m_SpawnLocationList == null) {
            m_SpawnLocationList = new FCSpawnLocationList();
        }
        if (m_LootingBeaconLocationList == null) {
            m_LootingBeaconLocationList = new FCBeaconEffectLocationList();
        }
    }

    @Override
    public FCMagneticPointList GetMagneticPointList() {
        initFcDataHolders();
        return m_MagneticPointList;
    }

    @Override
    public FCSpawnLocationList GetSpawnLocationList() {
        initFcDataHolders();
        return m_SpawnLocationList;
    }

    @Override
    public FCBeaconEffectLocationList GetLootingBeaconLocationList() {
        initFcDataHolders();
        return m_LootingBeaconLocationList;
    }

    // ================================================================
    // Tile entity collection
    // ================================================================

    @Override
    @SuppressWarnings("unchecked")
    public List getAllTileEntityInBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        List result = new ArrayList();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
                    if (be != null) {
                        if (be instanceof ProxyBlockEntity proxy) {
                            TileEntity fcTe = proxy.getFcTileEntity();
                            if (fcTe != null) {
                                result.add(fcTe);
                            }
                        } else {
                            result.add(TileEntityBridge.getOrCreate(be, this));
                        }
                    }
                }
            }
        }
        return result;
    }

    // ================================================================
    // Celestial angle / sun brightness
    // ================================================================

    @Override
    public float ComputeOverworldSunBrightnessWithMoonPhases() {
        // Compute sun angle from time-of-day
        float celestialAngle = level.getTimeOfDay(0);

        // Sun brightness curve: 1.0 at noon, 0.0 at midnight, with
        // smooth transitions at dawn/dusk.
        float sunBrightness;
        if (celestialAngle < 0.25F) {
            // Night to sunrise (0.0 = midnight, 0.25 = sunrise)
            sunBrightness = 0.0F;
        } else if (celestialAngle < 0.27F) {
            // Dawn transition
            sunBrightness = (celestialAngle - 0.25F) / 0.02F;
        } else if (celestialAngle < 0.73F) {
            // Daytime
            sunBrightness = 1.0F;
        } else if (celestialAngle < 0.75F) {
            // Dusk transition
            sunBrightness = 1.0F - (celestialAngle - 0.73F) / 0.02F;
        } else {
            // Night
            sunBrightness = 0.0F;
        }

        // Modulate by moon phase (0=full, 4=new). Full moon adds some
        // night brightness; new moon means total darkness.
        int moonPhase = level.getMoonPhase();
        float moonBrightness = switch (moonPhase) {
            case 0 -> 1.0F;   // full moon
            case 1, 7 -> 0.75F;
            case 2, 6 -> 0.5F;
            case 3, 5 -> 0.25F;
            case 4 -> 0.0F;   // new moon
            default -> 0.5F;
        };

        // When the sun is up, moon has no effect. At night, use moon
        // brightness scaled down.
        if (sunBrightness > 0.0F) {
            return sunBrightness;
        }
        return moonBrightness * 0.25F;
    }

    // ================================================================
    // Sound resolution helper
    // ================================================================

    /**
     * Resolves the sound name FC code hands the world to a 1.20.1 SoundEvent, or null (nothing plays).
     * <ul>
     *   <li>A 1.5.2 name ("random.drink", "mob.cow.say4") goes through the single legacy resolver,
     *       {@link LegacySounds#resolve}, which plays the 1.5.2 file pool. The old fallback
     *       (minecraft:&lt;name with '.' -&gt; '_'&gt;) named events that never exist: clients logged
     *       "Unable to play unknown soundEvent" and played nothing (audit S4).</li>
     *   <li>A namespaced id ("minecraft:block.deepslate.step") is a 1.20.1 event handed back to FC on purpose:
     *       {@link ModernBlockStandIn#GetStepSound} reports the live block's SoundType that way, so a puppet
     *       walking on a post-1.5.2 block steps with that block's sound. 1.5.2 names never contain ':'.</li>
     * </ul>
     */
    private static SoundEvent resolveSoundEvent(String fcSoundName) {
        if (fcSoundName == null || fcSoundName.isEmpty()) return null;
        if (fcSoundName.indexOf(':') > 0) {
            ResourceLocation loc = ResourceLocation.tryParse(fcSoundName);
            return loc != null ? net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(loc) : null;
        }
        return LegacySounds.resolve(fcSoundName);
    }

    // ================================================================
    // Ray tracing
    // ================================================================

    @Override
    public btw.modern.MovingObjectPosition rayTraceBlocks_do(btw.modern.Vec3 startVec, btw.modern.Vec3 endVec, boolean hitFluids) {
        net.minecraft.world.phys.Vec3 mcStart = new net.minecraft.world.phys.Vec3(startVec.xCoord, startVec.yCoord, startVec.zCoord);
        net.minecraft.world.phys.Vec3 mcEnd = new net.minecraft.world.phys.Vec3(endVec.xCoord, endVec.yCoord, endVec.zCoord);
        net.minecraft.world.level.ClipContext ctx = new net.minecraft.world.level.ClipContext(
                mcStart, mcEnd,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                hitFluids ? net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY : net.minecraft.world.level.ClipContext.Fluid.NONE,
                null);
        net.minecraft.world.phys.BlockHitResult result = level.clip(ctx);
        if (result == null || result.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
        net.minecraft.core.BlockPos hitPos = result.getBlockPos();
        btw.modern.MovingObjectPosition mop = new btw.modern.MovingObjectPosition(
                hitPos.getX(), hitPos.getY(), hitPos.getZ(),
                result.getDirection().get3DDataValue(),
                btw.modern.Vec3.createVectorHelper(result.getLocation().x, result.getLocation().y, result.getLocation().z));
        return mop;
    }

    @Override
    public btw.modern.MovingObjectPosition rayTraceBlocks(btw.modern.Vec3 startVec, btw.modern.Vec3 endVec) {
        return rayTraceBlocks_do(startVec, endVec, false);
    }

    @Override
    public btw.modern.MovingObjectPosition rayTraceBlocks_do_do(btw.modern.Vec3 startVec, btw.modern.Vec3 endVec, boolean hitFluids, boolean ignoreNonMovement) {
        return rayTraceBlocks_do(startVec, endVec, hitFluids);
    }

    // ================================================================
    // Collision detection
    // ================================================================

    @Override
    @SuppressWarnings("unchecked")
    public java.util.List getCollidingBoundingBoxes(btw.modern.Entity entity, btw.modern.AxisAlignedBB aabb) {
        java.util.List list = new java.util.ArrayList();
        int minX = btw.modern.MathHelper.floor_double(aabb.minX);
        int maxX = btw.modern.MathHelper.floor_double(aabb.maxX + 1.0D);
        // 1.5.2 World.getCollidingBoundingBoxes scans from minY - 1 (vanilla/client World.java:1617) so a
        // fence/wall's 1.5-high box in the block below still collides (stops puppets hopping fences).
        int minY = btw.modern.MathHelper.floor_double(aabb.minY) - 1;
        int maxY = btw.modern.MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = btw.modern.MathHelper.floor_double(aabb.minZ);
        int maxZ = btw.modern.MathHelper.floor_double(aabb.maxZ + 1.0D);
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    int blockId = getBlockId(x, y, z);
                    if (blockId > 0) {
                        if (btw.modern.ModernTwins.isTwin(blockId)) {
                            // FC plant twin: the live plant's own collision (azaleas, pitcher crops collide; FC's
                            // plant classes have no box), as the shaped stand-in these cells read as before.
                            addLiveCollisionBoxes(x, y, z, aabb, list);
                            continue;
                        }
                        btw.modern.Block block = btw.modern.Block.blocksList[blockId];
                        if (block != null) {
                            block.addCollisionBoxesToList(this, x, y, z, aabb, list, entity);
                        }
                    }
                }
            }
        }
        return list;
    }

    /** The live block's 1.20.1 collision boxes at (x, y, z) that intersect {@code mask} (ModernBlockStandIn's rule). */
    @SuppressWarnings("unchecked")
    private void addLiveCollisionBoxes(int x, int y, int z, btw.modern.AxisAlignedBB mask, java.util.List list) {
        BlockPos pos = new BlockPos(x, y, z);
        for (net.minecraft.world.phys.AABB b : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
            btw.modern.AxisAlignedBB bb = btw.modern.AxisAlignedBB.getBoundingBox(
                    x + b.minX, y + b.minY, z + b.minZ, x + b.maxX, y + b.maxY, z + b.maxZ);
            if (mask.intersectsWith(bb)) list.add(bb);
        }
    }

    // ================================================================
    // Liquid / material checks
    // ================================================================

    // ================================================================
    // Liquid / material checks
    // ================================================================

    @Override
    public boolean isAnyLiquid(btw.modern.AxisAlignedBB aabb) {
        int minX = btw.modern.MathHelper.floor_double(aabb.minX);
        int maxX = btw.modern.MathHelper.floor_double(aabb.maxX + 1.0D);
        int minY = btw.modern.MathHelper.floor_double(aabb.minY);
        int maxY = btw.modern.MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = btw.modern.MathHelper.floor_double(aabb.minZ);
        int maxZ = btw.modern.MathHelper.floor_double(aabb.maxZ + 1.0D);
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    btw.modern.Material mat = getBlockMaterial(x, y, z);
                    if (mat != null && mat.isLiquid()) return true;
                    if (fluidMaterialAt(x, y, z) != null) return true;
                }
            }
        }
        return false;
    }

    /**
     * 1.5.2 material of the fluid in a cell (water / lava), or null. 1.20.1 keeps water in the FluidState of
     * non-fluid blocks (waterlogged stairs/slabs/fences/...): those keep their own legacy id -- their collision
     * matters to FC -- so the id-based material checks miss the water they hold (triage D5). The liquid
     * queries below also ask this.
     */
    private btw.modern.Material fluidMaterialAt(int x, int y, int z) {
        net.minecraft.world.level.material.FluidState fluid = level.getFluidState(new BlockPos(x, y, z));
        if (fluid.isEmpty()) return null;
        if (fluid.is(net.minecraft.tags.FluidTags.WATER)) return btw.modern.Material.water;
        if (fluid.is(net.minecraft.tags.FluidTags.LAVA)) return btw.modern.Material.lava;
        return null;
    }

    // 1.5.2 World.handleMaterialAcceleration (vanilla/server World.java:2158), the inWater source of frozen
    // Entity.handleWaterMovement. The id-based scan (Modern-Common World) sees mapped water and the cells
    // getWorldBlockId reports as water (seagrass, kelp, bubble columns); a waterlogged block keeps its own
    // id, so its water is added here as a source cell (1.5.2 level 0: no flow push). Without it an FC squid
    // in a waterlogged cell read inWater=false and drowned FC-side (EntityWaterMob.java:63-86, triage D5).
    @Override
    public boolean handleMaterialAcceleration(btw.modern.AxisAlignedBB aabb, btw.modern.Material material,
                                              btw.modern.Entity entity) {
        boolean found = super.handleMaterialAcceleration(aabb, material, entity);
        if (found || material != btw.modern.Material.water || level == null) return found;
        int minX = btw.modern.MathHelper.floor_double(aabb.minX);
        int maxX = btw.modern.MathHelper.floor_double(aabb.maxX + 1.0D);
        int minY = btw.modern.MathHelper.floor_double(aabb.minY);
        int maxY = btw.modern.MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = btw.modern.MathHelper.floor_double(aabb.minZ);
        int maxZ = btw.modern.MathHelper.floor_double(aabb.maxZ + 1.0D);
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                for (int z = minZ; z < maxZ; ++z) {
                    if (fluidMaterialAt(x, y, z) == btw.modern.Material.water) {
                        double fluidTop = (double) ((float) (y + 1) - btw.modern.BlockFluid.getFluidHeightPercent(0));
                        if ((double) maxY >= fluidTop) return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isMaterialInBB(btw.modern.AxisAlignedBB aabb, btw.modern.Material material) {
        int minX = btw.modern.MathHelper.floor_double(aabb.minX);
        int maxX = btw.modern.MathHelper.floor_double(aabb.maxX + 1.0D);
        int minY = btw.modern.MathHelper.floor_double(aabb.minY);
        int maxY = btw.modern.MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = btw.modern.MathHelper.floor_double(aabb.minZ);
        int maxZ = btw.modern.MathHelper.floor_double(aabb.maxZ + 1.0D);
        boolean liquid = material != null && material.isLiquid();
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    btw.modern.Material mat = getBlockMaterial(x, y, z);
                    if (mat == material) return true;
                    // water held by a waterlogged block (D5); non-liquid queries keep the block's material
                    if (liquid && fluidMaterialAt(x, y, z) == material) return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isAABBInMaterial(btw.modern.AxisAlignedBB aabb, btw.modern.Material material) {
        return isMaterialInBB(aabb, material);
    }

    // 1.5.2 WorldServer.tick — villageCollectionObj.tick() drives village door
    // discovery, center/radius updates, aggressor decay, and reputation for the
    // frozen villager AI (EntityAIMoveIndoors/MoveThroughVillage/RestrictOpenDoor/
    // VillagerMate/DefendVillage). Runs once per server tick via ServerLevelMixin's
    // ModSpecificTick hook; func_82566_a re-binds this world first because the
    // World shim field-initializes villageCollectionObj without one.
    @Override
    public void ModSpecificTick() {
        // 1.5.2 WorldServer.tick updates skylightSubtracted with time of day each tick
        // (World.calculateSkylightSubtracted). getLightBrightness / GetBlockNaturalLightValue
        // and the gloom probe read this field; getSkyDarken is the modern equivalent.
        this.skylightSubtracted = level.getSkyDarken();
        villageCollectionObj.func_82566_a(this);
        villageCollectionObj.tick();
    }

    // Live world seed — Chunk.getRandomWithSeed (slime-chunk determination) needs the real seed.
    @Override
    public long getSeed() {
        return level.getSeed();
    }
}
