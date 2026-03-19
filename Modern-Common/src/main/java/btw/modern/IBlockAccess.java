package btw.modern;

public interface IBlockAccess {

    int getBlockId(int x, int y, int z);

    TileEntity getBlockTileEntity(int x, int y, int z);

    int getBlockMetadata(int x, int y, int z);

    Material getBlockMaterial(int x, int y, int z);

    boolean isBlockNormalCube(int x, int y, int z);

    default Vec3Pool getWorldVec3Pool() { return new Vec3Pool(); }

    // --- Client-side rendering methods ---

    default boolean isAirBlock(int x, int y, int z) {
        return getBlockId(x, y, z) == 0;
    }

    default boolean isBlockOpaqueCube(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        return id > 0 && id < Block.opaqueCubeLookup.length && Block.opaqueCubeLookup[id];
    }

    default int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return 0;
    }

    default float getLightBrightness(int x, int y, int z) {
        return 0;
    }

    default BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return null;
    }
}
