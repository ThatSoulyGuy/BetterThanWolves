package btw.forge;

import javax.annotation.Nullable;

/** A 1.20.1 block entity that carries FC's tile entity (FcBackedBlockEntities). Implemented by mixins. */
public interface FcBackedBlockEntity {
    /** FC's tile entity for this cell, created on first use; null when the cell has no FC identity, FC is not ready,
     *  the block entity has no level yet, or (client) no client FC world is installed. */
    @Nullable btw.modern.TileEntity btw$fcTile();

    /** Replaces it (WorldBridge.setBlockTileEntity with a pure FC tile entity). */
    void btw$setFcTile(@Nullable btw.modern.TileEntity te);
}
