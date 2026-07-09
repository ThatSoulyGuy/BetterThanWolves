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
 * Full {@link btw.modern.World} implementation backed by a Forge 1.20.1
 * {@link ServerLevel}.  This replaces {@link ForgeWorldServerWrapper} as the
 * canonical bridge between FC legacy code (int IDs, block coords) and the
 * modern block-state system.
 *
 * Instances are cached per-ServerLevel via {@link #getOrCreate(ServerLevel)}.
 */
// Extends World, NOT WorldServer. Making it a WorldServer would satisfy
// FCUtilsWorld's (WorldServer)worldObj cast for entity-tracker packets, BUT it also
// makes `worldObj instanceof WorldServer` true in frozen Entity.onEntityUpdate, whose
// portal block then calls ((WorldServer)worldObj).getMinecraftServer() every tick — a
// method that returns the modern net.minecraft.server.MinecraftServer type Modern-Common
// cannot declare (NoSuchMethodError, caught but per-tick). The entity-tracker packet sync
// (cow-kick / squid-tentacle client visuals) stays a caught no-op instead — a far smaller
// cost than aborting every FC entity's onUpdate at the portal check.
public class WorldBridge extends btw.modern.World {

    private static final Logger LOGGER = LogManager.getLogger("BTW-WorldBridge");

    /**
     * Metadata storage for vanilla blocks that don't have ProxyBlock.META.
     * FC code sets metadata on vanilla blocks (e.g., cracked flag on stone),
     * but vanilla MC blocks have no META property. This map stores the FC
     * metadata keyed by block position so getBlockMetadata can return it.
     * Entries are removed when the block at that position changes.
     */
    private final Map<BlockPos, Integer> vanillaMetaOverrides = new HashMap<>();

    private static final Map<ServerLevel, WorldBridge> cache = new WeakHashMap<>();

    /**
     * Returns (or creates) the WorldBridge for the given ServerLevel.
     */
    public static WorldBridge getOrCreate(ServerLevel level) {
        return cache.computeIfAbsent(level, WorldBridge::new);
    }

    private final ServerLevel level;

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
                return level.getFreeMapId();
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
        } catch (Exception e) {
            LOGGER.warn("Could not attach FC world saved data: {}", e.getMessage());
        }
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


    public int getBlockId(int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return ProxyRegistry.getBlockId(state.getBlock());
    }

    
    public int getBlockMetadata(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(ProxyBlock.META)) {
            return state.getValue(ProxyBlock.META);
        }
        // Check side map for vanilla blocks with FC metadata overrides
        Integer override = vanillaMetaOverrides.get(pos);
        if (override != null) return override;

        // Derive metadata from MC 1.20.1 BlockState properties for vanilla blocks
        return deriveVanillaMetadata(state, pos);
    }

    /**
     * Derives legacy MC 1.5.2 metadata from MC 1.20.1 BlockState properties.
     * For logs: axis → orientation bits, and bottom-of-tree → stump flag.
     */
    private int deriveVanillaMetadata(BlockState state, BlockPos pos) {
        net.minecraft.world.level.block.Block block = state.getBlock();

        // Fluids: MC LEVEL (0 = source, 1-7 = flowing, 8+ = falling) has the same
        // semantics as 1.5.2 fluid metadata -- required by World.handleMaterialAcceleration's
        // fluid-surface check and BlockFluid.getFlowVector (vanilla World.java:2186).
        if (block instanceof net.minecraft.world.level.block.LiquidBlock) {
            if (state.hasProperty(net.minecraft.world.level.block.LiquidBlock.LEVEL)) {
                return state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL);
            }
            return 0;
        }

        // Logs: axis property → orientation, bottom log → stump
        if (block instanceof net.minecraft.world.level.block.RotatedPillarBlock) {
            int legacyId = ProxyRegistry.getBlockId(block);
            if (legacyId == 17) { // Block.wood (log)
                int meta = 0; // wood type defaults to 0 (oak)
                // Axis → orientation: Y=0, X(east-west)=1<<2=4, Z(north-south)=2<<2=8
                if (state.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) {
                    net.minecraft.core.Direction.Axis axis = state.getValue(
                            net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
                    if (axis == net.minecraft.core.Direction.Axis.X) meta |= 4;      // east-west
                    else if (axis == net.minecraft.core.Direction.Axis.Z) meta |= 8;  // north-south
                    // Y = 0 (default, vertical)
                }

                // Stump detection: vertical log with solid non-log block below
                if ((meta & 12) == 0) { // only for vertical logs
                    BlockPos below = pos.below();
                    BlockState belowState = level.getBlockState(below);
                    boolean logBelow = belowState.getBlock() instanceof net.minecraft.world.level.block.RotatedPillarBlock
                            && ProxyRegistry.getBlockId(belowState.getBlock()) == 17;
                    if (!logBelow && !belowState.isAir() && belowState.isSolidRender(level, below)) {
                        meta = 12; // stump: both orientation bits set (3 << 2)
                    }
                }

                // Wood type from block type
                if (block == net.minecraft.world.level.block.Blocks.SPRUCE_LOG) meta = (meta & 12) | 1;
                else if (block == net.minecraft.world.level.block.Blocks.BIRCH_LOG) meta = (meta & 12) | 2;
                else if (block == net.minecraft.world.level.block.Blocks.JUNGLE_LOG) meta = (meta & 12) | 3;

                return meta;
            }
        }

        // Farmland: MC MOISTURE (0-7) → FC legacy metadata (0-7, >0 = hydrated)
        if (block instanceof net.minecraft.world.level.block.FarmBlock) {
            if (state.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE)) {
                return state.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE);
            }
            return 0;
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

        // Stairs: bits 0-1 = facing, bit 2 = upside-down
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
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

        // Lever/button: powered state
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
            int meta = 0;
            if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
                meta |= 8;
            }
            return meta;
        }

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
     */
    private BlockState applyFcMetadataToVanillaBlock(BlockState current, int metadata) {
        net.minecraft.world.level.block.Block block = current.getBlock();

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

        // Blocks with HORIZONTAL_FACING (furnace, chest, ladder, etc.)
        // FC metadata: 2=north, 3=south, 4=west, 5=east
        if (current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            int facingBits = metadata & 7; // lower 3 bits for facing
            net.minecraft.core.Direction dir = legacyFacingToDirection(facingBits);
            // HORIZONTAL_FACING only accepts horizontal directions
            if (dir.getAxis().isHorizontal()) {
                BlockState result = current.setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, dir);
                // Stairs: bit 2 = upside-down
                if (current.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)) {
                    // Stairs use different facing encoding: 0=east,1=west,2=south,3=north
                    net.minecraft.core.Direction stairDir;
                    switch (metadata & 3) {
                        case 0: stairDir = net.minecraft.core.Direction.EAST; break;
                        case 1: stairDir = net.minecraft.core.Direction.WEST; break;
                        case 2: stairDir = net.minecraft.core.Direction.SOUTH; break;
                        case 3: stairDir = net.minecraft.core.Direction.NORTH; break;
                        default: stairDir = net.minecraft.core.Direction.EAST;
                    }
                    result = current.setValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, stairDir);
                    result = result.setValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF,
                            (metadata & 4) != 0
                                    ? net.minecraft.world.level.block.state.properties.Half.TOP
                                    : net.minecraft.world.level.block.state.properties.Half.BOTTOM);
                }
                return result;
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

    /**
     * Resolves an FC block ID + metadata into the correct MC 1.20.1 BlockState.
     * Handles split blocks (e.g., floor torch vs wall torch) and applies facing/axis.
     */
    private BlockState resolveVanillaBlockState(int blockID, int metadata) {
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

        // For other vanilla blocks, get the modern block and apply metadata
        net.minecraft.world.level.block.Block mcBlock = ProxyRegistry.getModernBlock(blockID);
        if (mcBlock != null) {
            BlockState state = mcBlock.defaultBlockState();
            return applyFcMetadataToVanillaBlock(state, metadata);
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
        vanillaMetaOverrides.remove(pos); // clear metadata override on block change

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
            if (blockID == 241) {
                LOGGER.info("[PLACE-HEMP] at {} proxy={} randomTick={} state={}",
                        pos, proxy, state.isRandomlyTicking(), state);
            }
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
        BlockState vanillaState = resolveVanillaBlockState(blockID, metadata);
        if (vanillaState != null) {
            vanillaMetaOverrides.put(pos.immutable(), metadata);
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
            return level.setBlock(pos, vanillaState, placeFlags);
        }
        return false;
    }

    
    public boolean setBlockToAir(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        vanillaMetaOverrides.remove(pos);
        return level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    
    public boolean setBlockMetadata(int x, int y, int z, int metadata, int flags) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState current = level.getBlockState(pos);
        if (current.hasProperty(ProxyBlock.META)) {
            int oldMeta = current.getValue(ProxyBlock.META);
            BlockState newState = current.setValue(ProxyBlock.META, Math.min(Math.max(metadata, 0), 15));
            vanillaMetaOverrides.remove(pos); // clear any override
            boolean result = level.setBlock(pos, newState, flags);
            LOGGER.info("[SETMETA] at {} oldMeta={} newMeta={} flags={} result={} block={}",
                    pos, oldMeta, metadata, flags, result, current.getBlock());
            return result;
        }
        // Vanilla block without META — translate FC metadata to MC block state
        vanillaMetaOverrides.put(pos.immutable(), metadata);
        BlockState newState = applyFcMetadataToVanillaBlock(current, metadata);
        if (newState != current) {
            return level.setBlock(pos, newState, flags);
        }
        level.sendBlockUpdated(pos, current, current, flags);
        return true;
    }

    
    public boolean destroyBlock(int x, int y, int z, boolean dropItems) {
        return level.destroyBlock(new BlockPos(x, y, z), dropItems);
    }

    // ================================================================
    // Tile entity management
    // ================================================================

    
    public void setBlockTileEntity(int x, int y, int z, TileEntity tileEntity) {
        BlockPos pos = new BlockPos(x, y, z);
        LOGGER.info("[SET-TE] pos={} fcTe={} burnTime={}",
                pos, tileEntity != null ? tileEntity.getClass().getSimpleName() : "null",
                tileEntity instanceof btw.modern.TileEntityFurnace f ? f.furnaceBurnTime : "N/A");
        if (tileEntity instanceof TileEntityBridge bridge) {
            // Re-use the real BlockEntity wrapped by the bridge
            level.setBlockEntity(bridge.getBlockEntity());
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

    
    public void notifyBlockChange(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    
    public void notifyBlocksOfNeighborChange(int x, int y, int z, int blockID) {
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(blockID);
        if (modernBlock != null) {
            level.updateNeighborsAt(new BlockPos(x, y, z), modernBlock);
        }
    }

    
    public void notifyBlockOfNeighborChange(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block sourceBlock = ProxyRegistry.getModernBlock(blockID);
        if (sourceBlock != null) {
            level.getBlockState(pos).neighborChanged(level, pos, sourceBlock, pos, false);
        }
    }

    
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
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(blockID);
        if (modernBlock != null) {
            level.scheduleTick(pos, modernBlock, delay);
        }
    }

    
    public void func_82740_a(int x, int y, int z, int blockID, int delay, int priority) {
        // Same as scheduleBlockUpdate with priority (ignored for simplicity)
        scheduleBlockUpdate(x, y, z, blockID, delay);
    }

    @Override
    public boolean isBlockTickScheduled(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(blockID);
        if (modernBlock != null) {
            return level.getBlockTicks().hasScheduledTick(pos, modernBlock);
        }
        return false;
    }

    @Override
    public boolean IsUpdateScheduledForBlock(int i, int j, int k, int iBlockID) {
        return isBlockTickScheduled(i, j, k, iBlockID);
    }

    @Override
    public boolean IsUpdatePendingThisTickForBlock(int i, int j, int k, int iBlockID) {
        // MC 1.20.1 does not expose "pending this tick" easily; approximate
        // by checking if any tick is scheduled for the block.
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

    
    public int getFullBlockLightValue(int x, int y, int z) {
        return level.getMaxLocalRawBrightness(new BlockPos(x, y, z));
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
     */
    private final Map<btw.modern.Entity, net.minecraft.world.entity.Entity> fcToForgeEntity =
            new WeakHashMap<>();

    
    public boolean spawnEntityInWorld(btw.modern.Entity entity) {
        if (entity == null) return false;
        try {
            LOGGER.info("spawnEntityInWorld: {} at ({}, {}, {})",
                    entity.getClass().getSimpleName(), entity.posX, entity.posY, entity.posZ);
            net.minecraft.world.entity.Entity proxy =
                    EntityProxyFactory.createProxy(entity, level);
            if (proxy == null) {
                LOGGER.warn("EntityProxyFactory returned null for {}",
                        entity.getClass().getSimpleName());
                return false;
            }
            fcToForgeEntity.put(entity, proxy);
            boolean result = level.addFreshEntity(proxy);
            LOGGER.info("spawnEntityInWorld result: {} for {}", result, entity.getClass().getSimpleName());
            return result;
        } catch (Exception e) {
            LOGGER.debug("Failed to spawn proxy for {}: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
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
        List<net.minecraft.world.entity.Entity> forgeEntities =
                level.getEntities((net.minecraft.world.entity.Entity) null, forgeAABB,
                        e -> matchesFcClass(e, entityClass));
        // Return the FC entity instances rather than the proxies so FC
        // code can cast them to the expected FC types.
        List result = new ArrayList();
        for (net.minecraft.world.entity.Entity forgeEntity : forgeEntities) {
            btw.modern.Entity fc = extractFcEntity(forgeEntity);
            if (fc != null && entityClass.isInstance(fc)) {
                result.add(fc);
            }
        }
        return result;
    }

    
    public List getEntitiesWithinAABBExcludingEntity(btw.modern.Entity entity, AxisAlignedBB aabb) {
        net.minecraft.world.phys.AABB forgeAABB = toForgeAABB(aabb);
        net.minecraft.world.entity.Entity excludeProxy = fcToForgeEntity.get(entity);
        List<net.minecraft.world.entity.Entity> forgeEntities =
                level.getEntities(excludeProxy, forgeAABB, e -> true);
        List result = new ArrayList();
        for (net.minecraft.world.entity.Entity forgeEntity : forgeEntities) {
            btw.modern.Entity fc = extractFcEntity(forgeEntity);
            if (fc != null) {
                result.add(fc);
            }
        }
        return result;
    }

    
    public boolean checkNoEntityCollision(AxisAlignedBB aabb) {
        net.minecraft.world.phys.AABB forgeAABB = toForgeAABB(aabb);
        return level.getEntities((net.minecraft.world.entity.Entity) null,
                forgeAABB, e -> true).isEmpty();
    }

    
    public boolean canPlaceEntityOnSide(int blockID, int x, int y, int z,
                                        boolean skipEntities, int side,
                                        btw.modern.Entity entity, ItemStack stack) {
        if (skipEntities) return true;
        net.minecraft.world.phys.AABB checkBox = new net.minecraft.world.phys.AABB(
                x, y, z, x + 1, y + 1, z + 1);
        return level.getEntities((net.minecraft.world.entity.Entity) null,
                checkBox, e -> true).isEmpty();
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
        try {
            SoundEvent event = resolveSoundEvent(sound);
            if (event != null) {
                level.playSound(null, x, y, z, event,
                        SoundSource.BLOCKS, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not play sound '{}': {}", sound, e.getMessage());
        }
    }


    public void playSoundAtEntity(btw.modern.Entity entity, String sound,
                                  float volume, float pitch) {
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

    
    public void playAuxSFX(int effectID, int x, int y, int z, int data) {
        // FC custom effect IDs (2222+) need special handling — MC 1.20.1's
        // levelEvent only knows vanilla IDs. Map FC effects to sound playback.
        if (effectID >= 2222) {
            playFcAuxSFX(effectID, x, y, z, data);
            return;
        }
        level.levelEvent(effectID, new BlockPos(x, y, z), data);
    }

    /** Handles FC-custom AuxSFX IDs by playing the corresponding sounds. */
    private void playFcAuxSFX(int effectID, int x, int y, int z, int data) {
        double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
        java.util.Random rand = new java.util.Random();
        // Map FC effect IDs to their sound equivalents (from FC's client handler)
        switch (effectID) {
            case 2226: // burp
                playSoundEffect(px, py, pz, "random.burp", 0.5F, rand.nextFloat() * 0.1F + 0.9F);
                break;
            case 2227: // fire fizz
                playSoundEffect(px, py, pz, "random.fizz", 0.5F, 2.6F + (rand.nextFloat() - rand.nextFloat()) * 0.8F);
                break;
            case 2231: // item collection pop
            case 2232: // XP eject pop
                playSoundEffect(px, py, pz, "random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1F) * 2F);
                break;
            case 2233: // hopper close
                playSoundEffect(px, py, pz, "random.chestclosed", 0.1F, rand.nextFloat() * 0.1F + 0.9F);
                break;
            case 2234: // redstone power click
                playSoundEffect(px, py, pz, "random.click", 0.3F, 0.6F);
                break;
            case 2236: // block place
                playSoundEffect(px, py, pz, "dig.stone", 1F, 0.8F);
                break;
            case 2237: // dynamite fuse
                playSoundEffect(px, py, pz, "random.fuse", 1F, 1F);
                break;
            case 2238: // click low pitch
                playSoundEffect(px, py, pz, "random.click", 0.3F, 0.5F);
                break;
            case 2267: // stump removed
                playSoundEffect(px, py, pz, "mob.slime.attack", 1F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 0.6F);
                break;
            case 2268: // shaft ripped off log
            case 2271: // wood block destroyed
                playSoundEffect(px, py, pz, "mob.zombie.woodbreak", 0.25F, 1F + rand.nextFloat() * 0.25F);
                break;
            case 2269: // stone ripped off
                playSoundEffect(px, py, pz, "random.anvil_land", 0.5F, rand.nextFloat() * 0.25F + 1.75F);
                break;
            case 2270: // gravel ripped off stone
                playSoundEffect(px, py, pz, "random.anvil_land", 0.25F, rand.nextFloat() * 0.25F + 1.5F);
                playSoundEffect(px, py, pz, "step.gravel", 1F, rand.nextFloat() * 0.25F + 1F);
                break;
            case 2274: // mortar applied
                playSoundEffect(px, py, pz, "mob.slime.attack", 0.7F + rand.nextFloat() * 0.1F, 0.85F + rand.nextFloat() * 0.1F);
                break;
            case 2275: // loose block on mortar
                playSoundEffect(px, py, pz, "mob.slime.attack", 0.15F + rand.nextFloat() * 0.1F, 0.6F + rand.nextFloat() * 0.1F);
                break;
            case 2276: // log smouldering fall
                playSoundEffect(px, py, pz, "mob.zombie.woodbreak", 1.25F, 0.5F + rand.nextFloat() * 0.1F);
                playSoundEffect(px, py, pz, "mob.ghast.fireball", 1F, 0.5F + rand.nextFloat() * 0.1F);
                break;
            case 2278: // water evaporate
                playSoundEffect(px, py, pz, "random.fizz", 0.5F, 2.6F + (rand.nextFloat() - rand.nextFloat()) * 0.8F);
                break;
            case 2280: // lightning strike
                playSoundEffect(px, py, pz, "ambient.weather.thunder", 10000F, 0.8F + rand.nextFloat() * 0.2F);
                break;
            case 2283: // animal eat
            case 2284: // wolf eat
                playSoundEffect(px, py, pz, "random.eat", 0.5F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1F);
                break;
            case 2285: // eat fail
                playSoundEffect(px, py, pz, "random.eat", 0.25F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 0.5F);
                break;
            default:
                // Unknown FC effect — try vanilla levelEvent as fallback
                try {
                    level.levelEvent(effectID, new BlockPos(x, y, z), data);
                } catch (Exception ignored) {}
                break;
        }
    }

    
    public void playAuxSFXAtEntity(EntityPlayer player, int effectID,
                                   int x, int y, int z, int data) {
        level.levelEvent(null, effectID, new BlockPos(x, y, z), data);
    }

    // 1.5.2 World.func_82739_e (vanilla/server World.java:3900) — sends an auxSFX to all
    // players regardless of distance. Live callers: FCEntityAIWolfHowl.java:117 /
    // FCEntityAIWolfDireHowl.java:78 (howls), FCEntityWolf.java:894 (dire conversion),
    // FCEntityLightningBolt.java:82 (strike FX). Sound handling ported from the FC
    // client aux-FX handler (Client FCBetterThanWolves.java:3448+); high volumes give
    // the long audible range MC derives from volume.
    @Override
    public void func_82739_e(int effectID, int x, int y, int z, int data) {
        double px = x + 0.5D, py = y + 0.5D, pz = z + 0.5D;
        switch (effectID) {
            case 2256: { // FCBetterThanWolves.m_iWolfHowlAuxFXID
                float fSoundVolume;
                float fSoundPitch;
                if (data > 0) {
                    // dire howl
                    fSoundVolume = 10F;
                    fSoundPitch = (rand.nextFloat() - rand.nextFloat()) * 0.05F + 0.55F;
                } else {
                    // regular wolf howl
                    fSoundVolume = 8.5F;
                    fSoundPitch = (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1F;
                }
                playSoundEffect(px, py, pz, "mob.wolf.howl", fSoundVolume, fSoundPitch);
                break;
            }
            case 2257: // FCBetterThanWolves.m_iWolfConvertToDireAuxFXID
                spawnParticle("largeexplode", px, py, pz, 0.0D, 0.0D, 0.0D);
                playSoundEffect(px, py, pz, "mob.slime.attack", 1.0F,
                        (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
                playSoundEffect(px, py, pz, "mob.wolf.growl", 8.5F,
                        (rand.nextFloat() - rand.nextFloat()) * 0.05F + 0.55F);
                break;
            default:
                if (effectID >= 2222) {
                    // Other FC custom IDs (e.g. 2280 lightning strike) share the
                    // playAuxSFX sound mapping.
                    playFcAuxSFX(effectID, x, y, z, data);
                } else {
                    // Vanilla broadcast IDs (1013 wither spawn, 1018 dragon death)
                    // — global level event reaches every player like broadcastSound.
                    level.globalLevelEvent(effectID, new BlockPos(x, y, z), data);
                }
                break;
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
        net.minecraft.world.level.biome.Biome biome = level.getBiome(pos).value();
        return biome.coldEnoughToSnow(pos);
    }

    @Override
    public boolean IsPrecipitatingAtPos(int i, int j, int k) {
        return IsRainingAtPos(i, j, k) || IsSnowingAtPos(i, j, k);
    }

    // ================================================================
    // Redstone
    // ================================================================

    
    public boolean isBlockGettingPowered(int x, int y, int z) {
        return level.hasNeighborSignal(new BlockPos(x, y, z));
    }

    
    public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) {
        return level.getBestNeighborSignal(new BlockPos(x, y, z)) > 0;
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
        // Reuse the existing extractFcEntity helper which handles all proxy types
        return extractFcEntity(mcEntity);
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
            LOGGER.debug("GetClosestEntityMatchingCriteriaWithinRange failed: {}", e.getMessage());
            return null;
        }
    }

    // ================================================================
    // Entity state
    // ================================================================

    @Override
    public void setEntityState(btw.modern.Entity entity, byte state) {
        if (entity == null) return;
        net.minecraft.world.entity.Entity proxy = fcToForgeEntity.get(entity);
        if (proxy != null) {
            level.broadcastEntityEvent(proxy, state);
        }
    }

    // ================================================================
    // Explosion
    // ================================================================

    
    public Explosion createExplosion(btw.modern.Entity entity, double x, double y, double z,
                                     float strength, boolean isFlaming) {
        level.explode(null, x, y, z, strength,
                net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        Explosion fcExplosion = new Explosion(this, entity, x, y, z, strength);
        fcExplosion.isFlaming = isFlaming;
        return fcExplosion;
    }


    public Explosion newExplosion(btw.modern.Entity entity, double x, double y, double z,
                                  float strength, boolean isFlaming, boolean isSmoking) {
        level.explode(null, x, y, z, strength,
                net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        Explosion fcExplosion = new Explosion(this, entity, x, y, z, strength);
        fcExplosion.isFlaming = isFlaming;
        fcExplosion.isSmoking = isSmoking;
        return fcExplosion;
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
        net.minecraft.world.entity.Entity source =
                entity != null ? fcToForgeEntity.get(entity) : null;
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

    
    public int getHeight() {
        return level.getHeight();
    }

    
    public int getActualHeight() {
        return level.getHeight();
    }

    // ================================================================
    // Block events
    // ================================================================

    
    public void addBlockEvent(int x, int y, int z, int blockID, int eventID, int eventParam) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(blockID);
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
     * Resolves a legacy FC sound name (e.g. "random.drink") to a modern
     * SoundEvent.  Uses {@link SoundEvent#createVariableRangeEvent} with
     * the FC name as a ResourceLocation under the "minecraft" namespace,
     * converting dots to underscores so "random.drink" becomes
     * "minecraft:random/drink" (matching vanilla's path convention where
     * applicable).  Returns null if the name is blank.
     */
    private static SoundEvent resolveSoundEvent(String fcSoundName) {
        if (fcSoundName == null || fcSoundName.isEmpty()) return null;
        // Use the centralized SoundMapping which maps 1.5.2 names → 1.20.1 SoundEvents
        SoundEvent mapped = SoundMapping.get(fcSoundName);
        if (mapped != null) return mapped;
        // Fallback: try creating a SoundEvent from the raw name (for sounds
        // that happen to have the same name in 1.20.1, or custom registered sounds)
        try {
            String path = fcSoundName.replace('.', '_');
            ResourceLocation loc = new ResourceLocation("minecraft", path);
            return SoundEvent.createVariableRangeEvent(loc);
        } catch (Exception e) {
            return null;
        }
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
        int minY = btw.modern.MathHelper.floor_double(aabb.minY);
        int maxY = btw.modern.MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = btw.modern.MathHelper.floor_double(aabb.minZ);
        int maxZ = btw.modern.MathHelper.floor_double(aabb.maxZ + 1.0D);
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    int blockId = getBlockId(x, y, z);
                    if (blockId > 0) {
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
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    btw.modern.Material mat = getBlockMaterial(x, y, z);
                    if (mat == material) return true;
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
