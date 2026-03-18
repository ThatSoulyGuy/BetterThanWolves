package btw.api;

import java.util.ArrayList;
import java.util.List;

public class WorldClient extends World {

    public WorldClient() {
        super();
    }

    public Object createChunkProvider() { return null; }
    public int getBlockId(int x, int y, int z) { return 0; }
    public int getBlockMetadata(int x, int y, int z) { return 0; }
    public Material getBlockMaterial(int x, int y, int z) { return Material.air; }
    public boolean isAirBlock(int x, int y, int z) { return true; }
    public TileEntity getBlockTileEntity(int x, int y, int z) { return null; }
    public boolean setBlock(int x, int y, int z, int blockID, int metadata, int flags) { return false; }
    public boolean setBlockMetadata(int x, int y, int z, int metadata, int flags) { return false; }
    public boolean setBlockToAir(int x, int y, int z) { return false; }
    public boolean destroyBlock(int x, int y, int z, boolean dropItems) { return false; }
    public void notifyBlockChange(int x, int y, int z, int blockID) {}
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}
    public boolean canBlockSeeTheSky(int x, int y, int z) { return false; }
    public int getFullBlockLightValue(int x, int y, int z) { return 0; }
    public int getSavedLightValue(EnumSkyBlock enumSkyBlock, int x, int y, int z) { return 0; }
    public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) { return false; }
    public BiomeGenBase getBiomeGenForCoords(int x, int z) { return null; }
    public WorldChunkManager getWorldChunkManager() { return null; }
    public boolean spawnEntityInWorld(Entity entity) { return false; }
    public List getEntitiesWithinAABB(Class entityClass, AxisAlignedBB aabb) { return new ArrayList(); }
    public List getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB aabb) { return new ArrayList(); }
    public boolean checkNoEntityCollision(AxisAlignedBB aabb) { return true; }
    public boolean canPlaceEntityOnSide(int blockID, int x, int y, int z, boolean skipEntities, int side, Entity entity, ItemStack stack) { return false; }
    public void playSoundEffect(double x, double y, double z, String sound, float volume, float pitch) {}
    public void playSoundAtEntity(Entity entity, String sound, float volume, float pitch) {}
    public void spawnParticle(String particle, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {}
    public void playAuxSFX(int effectID, int x, int y, int z, int data) {}
    public boolean isRaining() { return false; }
    public boolean isBlockGettingPowered(int i, int j, int k) { return false; }
    public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) { return false; }
    public boolean doesBlockHaveSolidTopSurface(int i, int j, int k) { return false; }
    public boolean canMineBlock(EntityPlayer player, int x, int y, int z) { return false; }
    public void addEntityToWorld(int entityId, Entity entity) {}
    public Entity getEntityByID(int entityId) { return null; }
}
