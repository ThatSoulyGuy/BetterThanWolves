package btw.modern;

/**
 * A cache of chunks used for render-time block queries.
 * Mirrors net.minecraft.src.ChunkCache with identical field/method names.
 *
 * Most methods delegate to the world object or return safe defaults,
 * since the real rendering uses MC 1.20.1's chunk system.
 */
public class ChunkCache implements IBlockAccess {

    private final int chunkX;
    private final int chunkZ;
    private final Chunk[][] chunks;
    private final World worldObj;

    public ChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.worldObj = world;
        this.chunkX = minX >> 4;
        this.chunkZ = minZ >> 4;
        int maxChunkX = maxX >> 4;
        int maxChunkZ = maxZ >> 4;
        this.chunks = new Chunk[maxChunkX - this.chunkX + 1][maxChunkZ - this.chunkZ + 1];

        for (int cx = this.chunkX; cx <= maxChunkX; cx++) {
            for (int cz = this.chunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                if (chunk != null) {
                    chunks[cx - this.chunkX][cz - this.chunkZ] = chunk;
                }
            }
        }
    }

    @Override
    public int getBlockId(int x, int y, int z) {
        if (y < 0 || y >= 256) {
            return 0;
        }
        int cx = (x >> 4) - this.chunkX;
        int cz = (z >> 4) - this.chunkZ;
        if (cx >= 0 && cx < chunks.length && cz >= 0 && cz < chunks[cx].length && chunks[cx][cz] != null) {
            return chunks[cx][cz].getBlockID(x & 15, y, z & 15);
        }
        return 0;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (y < 0 || y >= 256) {
            return 0;
        }
        int cx = (x >> 4) - this.chunkX;
        int cz = (z >> 4) - this.chunkZ;
        if (cx >= 0 && cx < chunks.length && cz >= 0 && cz < chunks[cx].length && chunks[cx][cz] != null) {
            return chunks[cx][cz].getBlockMetadata(x & 15, y, z & 15);
        }
        return 0;
    }

    @Override
    public TileEntity getBlockTileEntity(int x, int y, int z) {
        return null;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return 0xF000F0; // full bright
    }

    @Override
    public Material getBlockMaterial(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id > 0 && id < Block.blocksList.length) {
            Block block = Block.blocksList[id];
            if (block != null) {
                return block.blockMaterial;
            }
        }
        return Material.air;
    }

    @Override
    public boolean isBlockNormalCube(int x, int y, int z) {
        return Block.isNormalCube(getBlockId(x, y, z));
    }

    @Override
    public boolean isBlockOpaqueCube(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        return id > 0 && id < Block.opaqueCubeLookup.length && Block.opaqueCubeLookup[id];
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlockId(x, y, z) == 0;
    }
}
