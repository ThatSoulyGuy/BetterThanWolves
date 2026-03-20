package btw.forge;

import btw.modern.TileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Forge 1.20.1 {@link BlockEntity} that wraps an FC {@link TileEntity}.
 *
 * <p>FC blocks that extend {@code BlockContainer} (e.g. campfires, hoppers,
 * furnaces, cauldrons) create their own FC tile entity via
 * {@code createNewTileEntity(World)}.  This class provides the MC BlockEntity
 * shell so that the FC tile entity is properly persisted, ticked, and
 * accessible via {@code world.getBlockTileEntity(x, y, z)}.</p>
 *
 * <p>A single {@link BlockEntityType} is shared by all ProxyBlockEntities
 * regardless of which FC block created them.  The FC tile entity class is
 * determined at runtime by the FC block at the position.</p>
 */
public class ProxyBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyBlockEntity");

    /**
     * The shared BlockEntityType for all ProxyBlockEntities.
     * Registered during the BLOCK_ENTITY_TYPES registry event.
     */
    public static BlockEntityType<ProxyBlockEntity> TYPE;

    /**
     * The wrapped FC tile entity. May be null briefly during load
     * (before {@link #load(CompoundTag)} is called and the FC tile entity
     * is recreated from NBT).
     */
    private TileEntity fcTileEntity;

    /**
     * The legacy FC block ID, saved to NBT so we can recreate the correct
     * FC tile entity type on load.
     */
    private int fcBlockId = -1;

    public ProxyBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    /**
     * Creates a ProxyBlockEntity with the given FC tile entity pre-set.
     */
    public ProxyBlockEntity(BlockPos pos, BlockState state, TileEntity fcTe, int legacyBlockId) {
        super(TYPE, pos, state);
        this.fcTileEntity = fcTe;
        this.fcBlockId = legacyBlockId;
        initFcTileEntity();
    }

    /**
     * Returns the wrapped FC tile entity, or null if not yet initialised.
     */
    public TileEntity getFcTileEntity() {
        return fcTileEntity;
    }

    /**
     * Sets the wrapped FC tile entity. Called when FC code uses
     * {@code world.setBlockTileEntity(x, y, z, te)} to replace the
     * tile entity at this position.
     */
    public void setFcTileEntity(TileEntity te) {
        this.fcTileEntity = te;
        if (te != null) {
            initFcTileEntity();
        }
        setChanged();
    }

    /**
     * Initialises the FC tile entity's position and world references
     * to match this BlockEntity's position.
     */
    private void initFcTileEntity() {
        if (fcTileEntity == null) return;
        fcTileEntity.xCoord = getBlockPos().getX();
        fcTileEntity.yCoord = getBlockPos().getY();
        fcTileEntity.zCoord = getBlockPos().getZ();
        if (level != null) {
            fcTileEntity.setWorldObj(WorldBridge.getOrCreate(
                    (net.minecraft.server.level.ServerLevel) level));
        }
        fcTileEntity.validate();
    }

    // ------------------------------------------------------------------
    // Level set override — update FC tile entity's world reference
    // ------------------------------------------------------------------

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (fcTileEntity != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
            fcTileEntity.setWorldObj(WorldBridge.getOrCreate(sl));
        }
    }

    // ------------------------------------------------------------------
    // NBT persistence
    // ------------------------------------------------------------------

    private static final String TAG_FC_BLOCK_ID = "fcBlockId";
    private static final String TAG_FC_DATA = "fcData";

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_FC_BLOCK_ID, fcBlockId);
        if (fcTileEntity != null) {
            CompoundTag fcData = new CompoundTag();
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcData);
            try {
                fcTileEntity.writeToNBT(wrapper);
            } catch (Exception e) {
                LOGGER.debug("Failed to save FC tile entity data for block {}: {}",
                        fcBlockId, e.getMessage());
            }
            tag.put(TAG_FC_DATA, fcData);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fcBlockId = tag.getInt(TAG_FC_BLOCK_ID);

        // Recreate the FC tile entity from the FC block
        if (fcBlockId > 0 && fcBlockId < btw.modern.Block.blocksList.length) {
            btw.modern.Block fcBlock = btw.modern.Block.blocksList[fcBlockId];
            if (fcBlock != null) {
                try {
                    fcTileEntity = fcBlock.createNewTileEntity(null);
                } catch (Exception e) {
                    LOGGER.debug("Failed to create FC tile entity for block {}: {}",
                            fcBlockId, e.getMessage());
                }
            }
        }

        // Load saved FC data into the tile entity
        if (fcTileEntity != null && tag.contains(TAG_FC_DATA)) {
            CompoundTag fcData = tag.getCompound(TAG_FC_DATA);
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcData);
            try {
                fcTileEntity.readFromNBT(wrapper);
            } catch (Exception e) {
                LOGGER.debug("Failed to load FC tile entity data for block {}: {}",
                        fcBlockId, e.getMessage());
            }
        }

        initFcTileEntity();
    }

    // ------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------

    /**
     * Static tick method used by the block entity ticker.
     * Called every server tick for ProxyBlockEntities whose FC block
     * has a tile entity.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, ProxyBlockEntity be) {
        if (be.fcTileEntity != null) {
            try {
                be.fcTileEntity.updateEntity();
            } catch (Exception e) {
                LOGGER.debug("FC tile entity tick failed at {}: {}",
                        pos, e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (fcTileEntity != null) {
            fcTileEntity.invalidate();
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (fcTileEntity != null) {
            fcTileEntity.validate();
        }
    }
}
