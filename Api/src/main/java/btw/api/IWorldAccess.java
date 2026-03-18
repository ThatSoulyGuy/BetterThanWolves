package btw.api;

public interface IWorldAccess {
    void markBlockForUpdate(int x, int y, int z);
    void markBlockForRenderUpdate(int x, int y, int z);
    void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2);
    void playSound(String name, double x, double y, double z, float volume, float pitch);
    void spawnParticle(String name, double x, double y, double z, double velX, double velY, double velZ);
}
