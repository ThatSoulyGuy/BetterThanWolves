package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A 1.20.1 block entity backs an FC tile entity (INTEGRATIONS #053, tag fc_beacon). A vanilla-id block whose FC block is
 * a BlockContainer is FC-backed when its 1.20.1 block entity carries FC's tile entity as an attachment
 * ({@link FcBackedBlockEntity}): FC's world sees FC's tile entity, FC's block hooks run on it, the 1.20.1 ticker runs
 * FC's updateEntity on both sides, FC's NBT rides in the block entity's tag under {@link #TAG}, and the 1.20.1 block
 * entity keeps its type id, renderer and other mods' view of it. One row today: the beacon.
 */
// FCMOD-INTEGRATION(1.20.1): fc_beacon: a 1.20.1 block entity backs FC's tile entity (attachment, fcData sub-tag, FC world, both sides); the beacon is the first row
public final class FcBackedBlockEntities {
    private FcBackedBlockEntities() {}

    private static final Logger LOGGER = LogManager.getLogger("BTW-FcBacked");
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    public static final String TAG = "fcData";

    /**
     * One row per bridged block entity: the 1.20.1 block class whose cells it fills, its block entity class, FC's legacy
     * id, and what a copied state (fcData naming another cell) must forget so that its first FC update at the new cell
     * is the one a new block gets.
     */
    record Row(Class<? extends Block> block, Class<? extends BlockEntity> blockEntity, int legacyId,
               Consumer<btw.modern.TileEntity> onCopied) {}

    /** The beacon: a copy restarts unpowered (levels -1, 1.5.2's default, vanilla/server TileEntityBeacon.java:26), so
     *  FCTileEntityBeacon.SetPowerState sees a power-up edge and adds this cell's magnetic and looting points (Common
     *  FCTileEntityBeacon.java:270-363). */
    static final List<Row> ROWS = List.of(
            new Row(BeaconBlock.class, BeaconBlockEntity.class, 138, te -> {
                if (te instanceof btw.modern.TileEntityBeacon b) { b.setLevelsServerSafe(-1); b.SetIsOn(false); }
            }));

    /** Client FC world per client level; installed by BTWClientEvents (a dedicated server never sets it). */
    private static volatile Function<Level, btw.modern.World> clientWorlds;

    public static void setClientWorlds(Function<Level, btw.modern.World> f) { clientWorlds = f; }

    /** HookPolicy rule 3 (pure, no instance): blocks of this class get an FC-backed block entity, and the mixin that
     *  makes the block entity class FC-backed was applied (class literal: loads, never initialises). */
    public static boolean backs(Block block) {
        for (Row r : ROWS) {
            if (r.block().isInstance(block)) return FcBackedBlockEntity.class.isAssignableFrom(r.blockEntity());
        }
        return false;
    }

    /** True when this block entity's cell is FC's: FC ready, a row's class, and the cell's block maps to the row's id. */
    public static boolean isBacked(BlockEntity be) {
        if (!BTWLifecycle.isFcReady() || !(be instanceof FcBackedBlockEntity)) return false;
        Row r = rowFor(be);
        return r != null && ProxyRegistry.getBlockId(be.getBlockState().getBlock()) == r.legacyId();
    }

    /** The FC world for a level: WorldBridge on the server, the installed client world on the client. */
    @Nullable
    public static btw.modern.World worldFor(Level level) {
        if (level instanceof ServerLevel sl) return WorldBridge.getOrCreate(sl);
        Function<Level, btw.modern.World> f = clientWorlds;
        return f == null ? null : f.apply(level);
    }

    // ------------------------------------------------------------------
    // Mixin entry points (BeaconBlockEntityMixin). Public: the mixin's code is merged into
    // net.minecraft.world.level.block.entity.BeaconBlockEntity, outside this package.
    // ------------------------------------------------------------------

    /** Creates FC's tile entity for be (null when not backed or no FC world for its level), applying pending fcData. */
    @Nullable
    public static btw.modern.TileEntity create(BlockEntity be, @Nullable CompoundTag pending) {
        if (be.getLevel() == null || !isBacked(be)) return null;
        btw.modern.World world = worldFor(be.getLevel());
        if (world == null) return null;
        Row r = rowFor(be);
        try {
            btw.modern.TileEntity te = btw.modern.Block.blocksList[r.legacyId()].createNewTileEntity(null);
            if (te == null) return null;
            if (pending != null) readFc(te, be, pending, r);
            bind(te, be, world);
            return te;
        } catch (Throwable t) {
            logOnce("create", r.legacyId(), t);
            return null;
        }
    }

    /** Position (always the block entity's) and world. */
    public static void bind(btw.modern.TileEntity te, BlockEntity be) {
        btw.modern.World world = be.getLevel() == null ? null : worldFor(be.getLevel());
        bind(te, be, world);
    }

    /** load TAIL: fcData into the live tile entity, or kept until it is created. Returns the new pending tag. */
    @Nullable
    public static CompoundTag load(@Nullable btw.modern.TileEntity te, BlockEntity be, CompoundTag tag,
                                   @Nullable CompoundTag pending) {
        if (!tag.contains(TAG, Tag.TAG_COMPOUND)) return pending;
        CompoundTag fc = tag.getCompound(TAG);
        if (te == null) return fc.copy();
        try {
            readFc(te, be, fc, rowFor(be));
            bind(te, be);
        } catch (Throwable t) {
            logOnce("load", -1, t);
        }
        return null;
    }

    /** saveAdditional TAIL: FC's NBT, or the unread pending tag (a block entity saved before its first tick). Never throws
     *  (ChunkMap.save and the stop save must not see an FC throw; as ProxyBlockEntity.saveAdditional). */
    public static void save(@Nullable btw.modern.TileEntity te, @Nullable CompoundTag pending, CompoundTag tag) {
        try {
            if (te != null) {
                CompoundTag fc = new CompoundTag();
                te.writeToNBT(new ForgeNBTCompound(fc));
                tag.put(TAG, fc);
            } else if (pending != null) {
                tag.put(TAG, pending.copy());
            }
        } catch (Throwable t) {
            logOnce("save", -1, t);
        }
    }

    // ------------------------------------------------------------------

    /** fcData into te. FC's x/y/z ride in fcData (Modern-Common TileEntity.writeToNBT :30-32); when all three are there
     *  and name another cell (/clone, a structure, a picked block's BlockEntityTag), the state is a copy: the row's
     *  onCopied runs before bind moves it here. A tag without coordinates (hand-written) is read as this cell's. */
    private static void readFc(btw.modern.TileEntity te, BlockEntity be, CompoundTag fc, @Nullable Row r) {
        te.readFromNBT(new ForgeNBTCompound(fc.copy()));
        BlockPos p = be.getBlockPos();
        boolean hasPos = fc.contains("x", Tag.TAG_ANY_NUMERIC) && fc.contains("y", Tag.TAG_ANY_NUMERIC)
                && fc.contains("z", Tag.TAG_ANY_NUMERIC);
        if (r != null && hasPos && (fc.getInt("x") != p.getX() || fc.getInt("y") != p.getY() || fc.getInt("z") != p.getZ())) {
            r.onCopied().accept(te);
        }
    }

    private static void bind(btw.modern.TileEntity te, BlockEntity be, @Nullable btw.modern.World world) {
        BlockPos p = be.getBlockPos();
        te.xCoord = p.getX();
        te.yCoord = p.getY();
        te.zCoord = p.getZ();
        if (world != null) te.setWorldObj(world);
        te.validate();
    }

    @Nullable
    static Row rowFor(BlockEntity be) {
        for (Row r : ROWS) if (r.blockEntity().isInstance(be)) return r;
        return null;
    }

    private static void logOnce(String site, int id, Throwable t) {
        if (WARNED.add(site + ":" + id + ":" + t.getClass().getName())) {
            LOGGER.warn("FC-backed block entity {} failed for block {} (further failures are not logged)", site, id, t);
        }
    }
}
