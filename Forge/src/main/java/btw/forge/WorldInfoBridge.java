package btw.forge;

import net.minecraft.server.level.ServerLevel;

/**
 * Bridges MC 1.20.1 ServerLevel data to FC's WorldInfo.
 * FC code accesses world.worldInfo.getWorldTime() etc. directly.
 */
public class WorldInfoBridge extends btw.modern.WorldInfo {

    private final ServerLevel level;

    public WorldInfoBridge(ServerLevel level) {
        this.level = level;
    }

    @Override
    public long getWorldTime() {
        return level.getDayTime();
    }

    @Override
    public void setWorldTime(long time) {
        level.setDayTime(time);
    }

    @Override
    public long getTotalWorldTime() {
        return level.getGameTime();
    }

    @Override
    public boolean isRaining() {
        return level.isRaining();
    }

    @Override
    public boolean isThundering() {
        return level.isThundering();
    }

    @Override
    public int getSpawnX() {
        return level.getSharedSpawnPos().getX();
    }

    @Override
    public int getSpawnY() {
        return level.getSharedSpawnPos().getY();
    }

    @Override
    public int getSpawnZ() {
        return level.getSharedSpawnPos().getZ();
    }

    @Override
    public long getSeed() {
        return level.getSeed();
    }

    @Override
    public String getWorldName() {
        return level.getServer().getWorldData().getLevelName();
    }
}
