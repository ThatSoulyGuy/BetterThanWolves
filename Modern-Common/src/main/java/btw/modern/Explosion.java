package btw.modern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Explosion {

    public boolean isFlaming;
    public boolean isSmoking = true;
    public boolean m_bSuppressFX = false;
    public double explosionX;
    public double explosionY;
    public double explosionZ;
    public Entity exploder;
    public float explosionSize;
    public List affectedBlockPositions = new ArrayList();
    public World worldObj;
    private Map field_77288_k = new HashMap();
    private Random explosionRNG = new Random();

    /** Secondary explosions queued during this explosion (e.g. smouldering logs chain-exploding). */
    public List m_SecondaryExplosionList = null;

    public Explosion(World world, Entity entity, double x, double y, double z, float size) {
        this.worldObj = world;
        this.exploder = entity;
        this.explosionSize = size;
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;
    }

    /**
     * Phase A: Calculate which blocks are affected by the explosion.
     * Uses a ray-cast approach from the center outward, checking block resistance.
     * This is a simplified but functional implementation matching the vanilla 1.5.2 algorithm.
     */
    public void doExplosionA() {
        if (worldObj == null) {
            return;
        }

        HashSet affectedSet = new HashSet();
        float stepFactor = 0.3F;

        // Cast rays in all directions from the explosion center
        for (int xDir = 0; xDir < 16; ++xDir) {
            for (int yDir = 0; yDir < 16; ++yDir) {
                for (int zDir = 0; zDir < 16; ++zDir) {
                    // Only process rays from the surface of the 16x16x16 cube
                    if (xDir == 0 || xDir == 15 || yDir == 0 || yDir == 15 || zDir == 0 || zDir == 15) {
                        double dirX = (double) xDir / 15.0D * 2.0D - 1.0D;
                        double dirY = (double) yDir / 15.0D * 2.0D - 1.0D;
                        double dirZ = (double) zDir / 15.0D * 2.0D - 1.0D;
                        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                        dirX /= length;
                        dirY /= length;
                        dirZ /= length;
                        float strength = this.explosionSize * (0.7F + worldObj.rand.nextFloat() * 0.6F);
                        double posX = this.explosionX;
                        double posY = this.explosionY;
                        double posZ = this.explosionZ;

                        while (strength > 0.0F) {
                            int blockX = MathHelper.floor_double(posX);
                            int blockY = MathHelper.floor_double(posY);
                            int blockZ = MathHelper.floor_double(posZ);
                            int blockId = worldObj.getBlockId(blockX, blockY, blockZ);

                            if (blockId > 0) {
                                Block block = Block.blocksList[blockId];
                                if (block != null) {
                                    float resistance = this.exploder != null
                                            ? block.getExplosionResistance(this.exploder)
                                            : block.getExplosionResistance(this.exploder);
                                    strength -= (resistance + 0.3F) * stepFactor;
                                }
                            }

                            if (strength > 0.0F) {
                                affectedSet.add(new ChunkPosition(blockX, blockY, blockZ));
                            }

                            posX += dirX * (double) stepFactor;
                            posY += dirY * (double) stepFactor;
                            posZ += dirZ * (double) stepFactor;
                            strength -= stepFactor * 0.75F;
                        }
                    }
                }
            }
        }

        this.affectedBlockPositions.addAll(affectedSet);

        // Damage/push entities in range
        float doubleSize = this.explosionSize * 2.0F;
        int minX = MathHelper.floor_double(this.explosionX - (double) doubleSize - 1.0D);
        int maxX = MathHelper.floor_double(this.explosionX + (double) doubleSize + 1.0D);
        int minY = MathHelper.floor_double(this.explosionY - (double) doubleSize - 1.0D);
        int maxY = MathHelper.floor_double(this.explosionY + (double) doubleSize + 1.0D);
        int minZ = MathHelper.floor_double(this.explosionZ - (double) doubleSize - 1.0D);
        int maxZ = MathHelper.floor_double(this.explosionZ + (double) doubleSize + 1.0D);

        List entitiesInRange = worldObj.getEntitiesWithinAABBExcludingEntity(
                this.exploder,
                AxisAlignedBB.getAABBPool().getAABB(minX, minY, minZ, maxX, maxY, maxZ));

        for (int i = 0; i < entitiesInRange.size(); ++i) {
            Entity entity = (Entity) entitiesInRange.get(i);
            double distX = entity.posX - this.explosionX;
            double distY = entity.posY - this.explosionY;
            double distZ = entity.posZ - this.explosionZ;
            double dist = Math.sqrt(distX * distX + distY * distY + distZ * distZ) / (double) doubleSize;

            if (dist <= 1.0D) {
                double normDist = 1.0D - dist;
                // Store knockback info for phase B
                field_77288_k.put(entity, new double[]{distX, distY, distZ, normDist});
            }
        }
    }

    /**
     * Phase B: Destroy affected blocks and apply entity knockback/damage.
     * @param spawnParticles if true, spawn visual block destruction effects
     */
    public void doExplosionB(boolean spawnParticles) {
        if (worldObj == null) {
            return;
        }

        // Play explosion sound and particles (unless FX suppressed)
        if (!m_bSuppressFX) {
            worldObj.playSoundEffect(this.explosionX, this.explosionY, this.explosionZ,
                    "random.explode", 4.0F,
                    (1.0F + (worldObj.rand.nextFloat() - worldObj.rand.nextFloat()) * 0.2F) * 0.7F);

            if (this.explosionSize >= 2.0F && this.isSmoking) {
                worldObj.spawnParticle("hugeexplosion", this.explosionX, this.explosionY, this.explosionZ,
                        1.0D, 0.0D, 0.0D);
            } else {
                worldObj.spawnParticle("largeexplode", this.explosionX, this.explosionY, this.explosionZ,
                        1.0D, 0.0D, 0.0D);
            }
        }

        // Destroy blocks
        Iterator blockIter;
        ChunkPosition chunkPos;

        if (this.isSmoking) {
            blockIter = this.affectedBlockPositions.iterator();

            while (blockIter.hasNext()) {
                chunkPos = (ChunkPosition) blockIter.next();
                int x = chunkPos.x;
                int y = chunkPos.y;
                int z = chunkPos.z;
                int blockId = worldObj.getBlockId(x, y, z);

                // Spawn particle effects for destroyed blocks
                if (spawnParticles) {
                    double px = (double) x + worldObj.rand.nextFloat();
                    double py = (double) y + worldObj.rand.nextFloat();
                    double pz = (double) z + worldObj.rand.nextFloat();
                    double dx = px - this.explosionX;
                    double dy = py - this.explosionY;
                    double dz = pz - this.explosionZ;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= dist;
                    dy /= dist;
                    dz /= dist;
                    double scale = 0.5D / (dist / (double) this.explosionSize + 0.1D);
                    scale *= (double) (worldObj.rand.nextFloat() * worldObj.rand.nextFloat() + 0.3F);
                    worldObj.spawnParticle("explode", (px + this.explosionX) / 2.0D,
                            (py + this.explosionY) / 2.0D, (pz + this.explosionZ) / 2.0D,
                            dx, dy, dz);
                    worldObj.spawnParticle("smoke", px, py, pz, dx, dy, dz);
                }

                if (blockId > 0) {
                    Block block = Block.blocksList[blockId];

                    if (block != null) {
                        // Drop items from explosion
                        if (block.canDropFromExplosion(this)) {
                            block.dropBlockAsItemWithChance(worldObj, x, y, z,
                                    worldObj.getBlockMetadata(x, y, z),
                                    1.0F / this.explosionSize, 0);
                        }

                        // Notify block of destruction, then remove it
                        block.onBlockDestroyedByExplosion(worldObj, x, y, z, this);
                        worldObj.setBlock(x, y, z, 0, 0, 3);
                    }
                }
            }
        }

        // Place fire blocks if explosion is flaming
        if (this.isFlaming) {
            blockIter = this.affectedBlockPositions.iterator();

            while (blockIter.hasNext()) {
                chunkPos = (ChunkPosition) blockIter.next();
                int x = chunkPos.x;
                int y = chunkPos.y;
                int z = chunkPos.z;

                int blockId = worldObj.getBlockId(x, y, z);
                int belowId = worldObj.getBlockId(x, y - 1, z);

                if (blockId == 0 && belowId > 0) {
                    Block belowBlock = Block.blocksList[belowId];
                    if (belowBlock != null && belowBlock.isOpaqueCube() && explosionRNG.nextInt(3) == 0) {
                        if (Block.fire != null) {
                            worldObj.setBlock(x, y, z, Block.fire.blockID);
                        }
                    }
                }
            }
        }

        // Apply entity knockback and damage
        Iterator entityIter = field_77288_k.entrySet().iterator();
        while (entityIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entityIter.next();
            Entity entity = (Entity) entry.getKey();
            double[] data = (double[]) entry.getValue();
            // data[3] is the normalized distance factor (1.0 = at center, 0.0 = at edge)
            // Simplified: apply motion change
            double factor = data[3];
            double dist = Math.sqrt(data[0] * data[0] + data[1] * data[1] + data[2] * data[2]);
            if (dist > 0.0D) {
                entity.motionX += data[0] / dist * factor;
                entity.motionY += data[1] / dist * factor;
                entity.motionZ += data[2] / dist * factor;
            }
        }

        // Perform any queued secondary explosions
        PerformSecondaryExplosions();
    }

    /**
     * Queue a secondary explosion to occur after this explosion completes phase B.
     * Used by blocks like smouldering logs to chain-react without recursive overhead.
     */
    public void AddSecondaryExplosionNoFX(double x, double y, double z,
            float strength, boolean isFlaming, boolean isSmoking) {
        if (m_SecondaryExplosionList == null) {
            m_SecondaryExplosionList = new ArrayList();
        }

        Explosion explosion = new Explosion(worldObj, null, x, y, z, strength);
        explosion.isFlaming = isFlaming;
        explosion.isSmoking = isSmoking;
        explosion.m_bSuppressFX = true;

        m_SecondaryExplosionList.add(explosion);
    }

    /**
     * Execute all queued secondary explosions.
     */
    private void PerformSecondaryExplosions() {
        if (m_SecondaryExplosionList != null) {
            Iterator iter = m_SecondaryExplosionList.iterator();

            while (iter.hasNext()) {
                Explosion tempExplosion = (Explosion) iter.next();

                tempExplosion.doExplosionA();
                tempExplosion.doExplosionB(false);
            }
        }
    }

    /**
     * Returns the living entity that caused this explosion (e.g. the TNT placer).
     */
    public EntityLiving func_94613_c() {
        return this.exploder == null ? null :
                (this.exploder instanceof EntityLiving ? (EntityLiving) this.exploder : null);
    }

    /**
     * Returns the map of affected entities to their knockback data.
     */
    public Map func_77277_b() {
        return this.field_77288_k;
    }
}
