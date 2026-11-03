package btw.forge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;

import java.lang.ref.WeakReference;

/**
 * Bridges MC 1.20.1 ServerLevel data to FC's WorldInfo.
 * FC code accesses world.worldInfo.getWorldTime() etc. directly.
 *
 * The level is held weakly: this object hangs off WorldBridge.worldInfo, the value of
 * WorldBridge's ServerLevel-keyed WeakHashMap, so a strong reference would pin the key (RE2).
 * While the server runs the level is always alive; once it is gone the inherited WorldInfo
 * fields answer instead. The FC-only state (global ender inventory, progress flags) lives in
 * the inherited fields and is persisted by FcWorldSavedData.
 */
public class WorldInfoBridge extends btw.modern.WorldInfo {

    private final WeakReference<ServerLevel> levelRef;

    public WorldInfoBridge(ServerLevel level) {
        this.levelRef = new WeakReference<>(level);
    }

    @Override
    public long getWorldTime() {
        ServerLevel level = levelRef.get();
        return level != null ? level.getDayTime() : super.getWorldTime();
    }

    @Override
    public void setWorldTime(long time) {
        ServerLevel level = levelRef.get();
        if (level != null) {
            level.setDayTime(time);
        } else {
            super.setWorldTime(time);
        }
    }

    @Override
    public long getTotalWorldTime() {
        ServerLevel level = levelRef.get();
        return level != null ? level.getGameTime() : super.getTotalWorldTime();
    }

    @Override
    public boolean isRaining() {
        ServerLevel level = levelRef.get();
        return level != null ? level.isRaining() : super.isRaining();
    }

    @Override
    public boolean isThundering() {
        ServerLevel level = levelRef.get();
        return level != null ? level.isThundering() : super.isThundering();
    }

    @Override
    public int getSpawnX() {
        ServerLevel level = levelRef.get();
        return level != null ? level.getSharedSpawnPos().getX() : super.getSpawnX();
    }

    @Override
    public int getSpawnY() {
        ServerLevel level = levelRef.get();
        return level != null ? level.getSharedSpawnPos().getY() : super.getSpawnY();
    }

    @Override
    public int getSpawnZ() {
        ServerLevel level = levelRef.get();
        return level != null ? level.getSharedSpawnPos().getZ() : super.getSpawnZ();
    }

    @Override
    public long getSeed() {
        ServerLevel level = levelRef.get();
        return level != null ? level.getSeed() : super.getSeed();
    }

    @Override
    public btw.modern.WorldType getTerrainType() {
        ServerLevel level = levelRef.get();
        if (level != null && level.getServer().overworld().getChunkSource().getGenerator()
                instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator gen
                && gen.stable(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.LARGE_BIOMES)) {
            return btw.modern.WorldType.LARGE_BIOMES;
        }
        return super.getTerrainType();
    }

    /**
     * The port's one creative-world rule. FCMOD-INTEGRATION(1.20.1): creative_world (used by zombified_villages and
     * hardcore_respawn). 4.B's world game type, 1.5.2 WorldInfo.getGameType, is 1.20.1's default game type,
     * MinecraftServer.getDefaultGameType() = WorldData.getGameType(), saved in level.dat as GameType
     * (PrimaryLevelData.java:163). A world gets it when it is created: the mode chosen on the creation screen
     * (CreateWorldScreen.java:237; Hardcore creates a SURVIVAL world, WorldCreationUiState.java:280-283) or a dedicated
     * server's gamemode property (server Main.java:183). Only an operator changes it: /defaultgamemode
     * (DefaultGameModeCommands.java:21-24) and, on a dedicated server, the gamemode property, which DedicatedServer.java:121
     * applies at every start. 1.5.2 did the same (vanilla/server MinecraftServer.java:270-273, :1375-1381) and 4.B's own
     * creative tests read that value (ComponentVillage.java:178). The hardcore flag is not a game type and takes no part.
     */
    public static boolean isCreativeWorld(MinecraftServer server) {
        return server != null && server.getDefaultGameType() == GameType.CREATIVE;
    }

    /** isCreativeWorld's source as 4.B's EnumGameType (1.20.1 ids 0-2 are 1.5.2's); SPECTATOR, 1.20.1 only (debug worlds),
     *  reads SURVIVAL, as EnumGameType.getByID would. FC reads it on every tick of every possessable creature (getGameType;
     *  vanilla/client EntityCreature.java:402-410, :486), so it maps without allocating (getByID copies values()). */
    public static btw.modern.EnumGameType worldGameType(MinecraftServer server) {
        GameType type = server.getDefaultGameType();
        return type == GameType.CREATIVE ? btw.modern.EnumGameType.CREATIVE
                : type == GameType.ADVENTURE ? btw.modern.EnumGameType.ADVENTURE : btw.modern.EnumGameType.SURVIVAL;
    }

    /**
     * 1.5.2 WorldInfo.getGameType, answered from isCreativeWorld's source. FC's own creative-world tests read it: Client
     * FCEntityCow.java:266, FCEntityVillager.java:271, FCEntityWolf.java:767 and :889, and FC's builds of
     * EntityCreature.HandlePossession and EntityLiving.onDeath's scroll roll (vanilla/client EntityCreature.java:486,
     * EntityLiving.java:3503; shipped by Forge/build.gradle:235, :240). Before this override they read the stand-in's
     * SURVIVAL in every world (Modern-Common WorldInfo.java:23, never set). FCMOD-INTEGRATION(1.20.1): creative_world
     */
    @Override
    public btw.modern.EnumGameType getGameType() {
        ServerLevel level = levelRef.get();
        return level != null ? worldGameType(level.getServer()) : super.getGameType();
    }

    @Override
    public String getWorldName() {
        ServerLevel level = levelRef.get();
        return level != null ? level.getServer().getWorldData().getLevelName() : super.getWorldName();
    }
}
