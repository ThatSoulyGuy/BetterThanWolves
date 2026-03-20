package btw.modern;

import java.util.Random;

/**
 * Mob spawning logic. Mirrors net.minecraft.src.SpawnerAnimals.
 *
 * FC modifies findChunksForSpawning to use the world's active chunk list
 * instead of the vanilla 17x17 player-centric approach, adds custom spawn
 * cap counting, and introduces several helper methods for spawn-location
 * validation.
 *
 * In the modern port, actual mob spawning is handled by the Forge/NeoForge
 * backend. These stubs exist so FC code compiles and can be called at
 * runtime; the methods that would normally drive spawning simply return 0.
 */
public final class SpawnerAnimals {

    /**
     * FC signature: called from WorldServer.tick().
     * Original vanilla: findChunksForSpawning(WorldServer, boolean, boolean, boolean)
     *
     * Returns the number of mobs spawned this tick (0 in stub).
     */
    public static int findChunksForSpawning(WorldServer world,
                                            boolean spawnHostileMobs,
                                            boolean spawnPeacefulMobs,
                                            boolean spawnAnimals) {
        return 0;
    }

    /**
     * Returns whether a creature of the given type can spawn at the specified
     * block position.  FC replaces the vanilla implementation with one that
     * delegates to Block.CanMobsSpawnOn().
     */
    public static boolean canCreatureTypeSpawnAtLocation(EnumCreatureType type,
                                                        World world,
                                                        int x, int y, int z) {
        if (type.getCreatureMaterial() == Material.water) {
            return world.getBlockMaterial(x, y, z).isLiquid()
                && world.getBlockMaterial(x, y - 1, z).isLiquid()
                && !world.isBlockNormalCube(x, y + 1, z);
        }

        if (!world.isBlockNormalCube(x, y, z)
            && !world.getBlockMaterial(x, y, z).isLiquid()) {
            int belowId = world.getBlockId(x, y - 1, z);
            Block blockBelow = (belowId >= 0 && belowId < Block.blocksList.length)
                ? Block.blocksList[belowId] : null;
            return blockBelow != null && blockBelow.CanMobsSpawnOn(world, x, y - 1, z);
        }

        return false;
    }

    /**
     * FC-added: simplified spawn check used during initial world generation
     * for animals.  Prevents spawns inside leaves and performs fewer tests.
     */
    public static boolean CanAnimalSpawnAtLocationDuringWorldGen(EnumCreatureType type,
                                                                World world,
                                                                int x, int y, int z) {
        int blockId = world.getBlockId(x, y, z);

        if (!Block.isNormalCube(blockId)
            && !world.getBlockMaterial(x, y, z).isLiquid()
            && blockId != Block.leaves.blockID) {
            int aboveId = world.getBlockId(x, y + 1, z);

            if (!Block.isNormalCube(aboveId) && aboveId != Block.leaves.blockID) {
                int belowId = world.getBlockId(x, y - 1, z);
                return Block.isNormalCube(belowId)
                    && belowId != Block.bedrock.blockID;
            }
        }

        return false;
    }

    /**
     * FC-added: spawn check for witches during world generation.
     * More permissive than the animal variant -- allows spawning over water
     * and leaves to improve chances around generated witch huts.
     */
    public static boolean CanWitchSpawnAtLocationDuringWorldGen(World world,
                                                               int x, int y, int z) {
        int blockId = world.getBlockId(x, y, z);

        if (!Block.isNormalCube(blockId)
            && !world.getBlockMaterial(x, y, z).isLiquid()
            && blockId != Block.leaves.blockID) {
            int aboveId = world.getBlockId(x, y + 1, z);

            if (!Block.isNormalCube(aboveId) && aboveId != Block.leaves.blockID) {
                int belowId = world.getBlockId(x, y - 1, z);
                return belowId != Block.bedrock.blockID
                    && (Block.isNormalCube(belowId)
                        || world.getBlockMaterial(x, y - 1, z) == Material.water
                        || belowId == Block.leaves.blockID);
            }
        }

        return false;
    }

    /**
     * FC-added: returns the vertical offset to apply when placing a mob at a
     * spawn position, delegating to the block below.
     */
    public static float GetVerticalOffsetForPos(EnumCreatureType type,
                                                World world,
                                                int x, int y, int z) {
        if (type.getCreatureMaterial() != Material.water) {
            int belowId = world.getBlockId(x, y - 1, z);
            Block blockBelow = (belowId >= 0 && belowId < Block.blocksList.length)
                ? Block.blocksList[belowId] : null;
            if (blockBelow != null) {
                return blockBelow.MobSpawnOnVerticalOffset(world, x, y - 1, z);
            }
        }
        return 0F;
    }

    /**
     * FC-added: checks whether a creature type's material is compatible with
     * the material at a spawn position.
     */
    public static boolean CanCreatureTypeSpawnInMaterial(EnumCreatureType type,
                                                        Material material) {
        if (material == Material.water) {
            return type.getCreatureMaterial() == Material.water;
        } else {
            return type.getCreatureMaterial() != Material.water;
        }
    }

    /**
     * Vanilla method called during chunk generation to place initial animals.
     * FC modifies it to use CanAnimalSpawnAtLocationDuringWorldGen instead of
     * the vanilla canCreatureTypeSpawnAtLocation, and calls PreInitCreature().
     *
     * In the modern port, world-gen spawning is handled by the backend.
     */
    public static void performWorldGenSpawning(World world,
                                               BiomeGenBase biome,
                                               int centerX, int centerZ,
                                               int sizeX, int sizeZ,
                                               Random random) {
        // Stub -- world-gen spawning handled by Forge/NeoForge backend
    }

    /**
     * Vanilla helper: determines special spawn init for certain mobs
     * (e.g. skeleton riding spider, colored sheep).
     */
    public static void creatureSpecificInit(EntityLiving entity,
                                            World world,
                                            float x, float y, float z) {
        // Stub
    }
}
