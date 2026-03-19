package btw.forge;

import btw.modern.NBTTagCompound;
import btw.modern.TileEntity;
import btw.modern.World;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bridges a modern MC {@link BlockEntity} to an FC {@link TileEntity}.
 *
 * FC code accesses tile entities via {@code world.getBlockTileEntity(x, y, z)}
 * and casts them to specific subclasses (e.g. {@code FCTileEntityHopper},
 * {@code TileEntityFurnace}).  This bridge wraps the real MC BlockEntity and
 * exposes it through the FC TileEntity API, providing:
 * <ul>
 *   <li>Position fields ({@code xCoord}, {@code yCoord}, {@code zCoord})</li>
 *   <li>World reference ({@code worldObj})</li>
 *   <li>NBT read/write delegation to the real BlockEntity</li>
 *   <li>{@code markDirty()} delegation</li>
 *   <li>{@code invalidate()} / {@code validate()} tracking</li>
 * </ul>
 *
 * Instances are cached per-BlockEntity so that identity checks like
 * {@code world.getBlockTileEntity(x,y,z) == this} work correctly in FC code.
 */
public class TileEntityBridge extends TileEntity {

    private static final Logger LOGGER = LogManager.getLogger("BTW-TileEntityBridge");

    /**
     * Cache of BlockEntity -> TileEntityBridge so the same wrapper is
     * returned for repeated lookups.  Uses weak keys so wrappers are GC'd
     * when the BlockEntity is unloaded.
     */
    private static final Map<BlockEntity, TileEntityBridge> cache = new WeakHashMap<>();

    private final BlockEntity blockEntity;

    private TileEntityBridge(BlockEntity blockEntity, World world) {
        this.blockEntity = blockEntity;
        BlockPos pos = blockEntity.getBlockPos();
        this.xCoord = pos.getX();
        this.yCoord = pos.getY();
        this.zCoord = pos.getZ();
        this.worldObj = world;
        this.tileEntityInvalid = blockEntity.isRemoved();
    }

    /**
     * Returns (or creates) a TileEntityBridge wrapping the given BlockEntity.
     * Returns null if {@code be} is null.
     */
    public static TileEntityBridge getOrCreate(BlockEntity be, World world) {
        if (be == null) return null;
        return cache.computeIfAbsent(be, key -> new TileEntityBridge(key, world));
    }

    /**
     * Removes the cached bridge for a BlockEntity (e.g. when it is removed
     * from the world).
     */
    public static void uncache(BlockEntity be) {
        if (be != null) {
            cache.remove(be);
        }
    }

    /**
     * Returns the underlying MC BlockEntity.
     */
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    // ------------------------------------------------------------------
    // NBT delegation
    // ------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        if (compound instanceof ForgeNBTCompound fnbt) {
            blockEntity.load(fnbt.getTag());
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        if (compound instanceof ForgeNBTCompound fnbt) {
            // saveWithoutMetadata() is public and saves tile entity data without pos/id
            net.minecraft.nbt.CompoundTag saved = blockEntity.saveWithoutMetadata();
            // Merge into the target tag
            for (String key : saved.getAllKeys()) {
                fnbt.getTag().put(key, saved.get(key));
            }
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void updateEntity() {
        // The real BlockEntity is ticked by the MC engine; nothing to do here.
    }

    @Override
    public void invalidate() {
        super.invalidate();
        // Note: we do NOT call blockEntity.setRemoved() here because the MC
        // engine manages BlockEntity lifecycle.  We only track the FC-side flag.
    }

    @Override
    public void validate() {
        super.validate();
    }

    @Override
    public boolean isInvalid() {
        // Reflect the real state from the MC BlockEntity
        return blockEntity.isRemoved() || super.isInvalid();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        blockEntity.setChanged();
    }

    @Override
    public void onInventoryChanged() {
        blockEntity.setChanged();
    }

    // ------------------------------------------------------------------
    // Metadata / block type
    // ------------------------------------------------------------------

    @Override
    public int getBlockMetadata() {
        if (worldObj != null) {
            return worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        }
        return 0;
    }

    @Override
    public btw.modern.Block getBlockType() {
        if (worldObj != null) {
            int id = worldObj.getBlockId(xCoord, yCoord, zCoord);
            if (id > 0 && id < btw.modern.Block.blocksList.length) {
                return btw.modern.Block.blocksList[id];
            }
        }
        return null;
    }
}
