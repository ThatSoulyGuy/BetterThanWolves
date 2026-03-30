package btw.modern;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a village. Mirrors net.minecraft.src.Village.
 */
public class Village {

    private World worldObj;
    private List villageDoorInfoList = new ArrayList();
    private ChunkCoordinates center = new ChunkCoordinates(0, 0, 0);
    private int villageRadius;
    private int tickCounter;
    private List villageAgressors = new ArrayList();

    public ChunkCoordinates getCenter() { return center; }
    public int getVillageRadius() { return villageRadius; }
    public int getNumVillageDoors() { return villageDoorInfoList.size(); }
    public int func_82784_g() { return 0; }

    public boolean isPlayerReputationTooLow(String playerName) { return false; }
    public int getReputationForPlayer(String playerName) { return 0; }

    /**
     * Adds or renews an aggressor to the village's aggressor list.
     * Matches vanilla 1.5.2 Village.addOrRenewAgressor().
     * Called by FCEntityVillager when attacked.
     */
    public void addOrRenewAgressor(EntityLiving aggressor) {
        for (int i = 0; i < this.villageAgressors.size(); i++) {
            VillageAgressor existing = (VillageAgressor) this.villageAgressors.get(i);
            if (existing.agressor == aggressor) {
                existing.agressionTime = this.tickCounter;
                return;
            }
        }
        this.villageAgressors.add(new VillageAgressor(aggressor, this.tickCounter));
    }

    public EntityLiving findNearestVillageAggressor(EntityLiving entity) {
        double closestDist = Double.MAX_VALUE;
        VillageAgressor closest = null;

        for (int i = 0; i < this.villageAgressors.size(); i++) {
            VillageAgressor va = (VillageAgressor) this.villageAgressors.get(i);
            double dist = va.agressor.getDistanceSqToEntity(entity);
            if (dist <= closestDist) {
                closest = va;
                closestDist = dist;
            }
        }

        return closest != null ? closest.agressor : null;
    }

    /**
     * Simple inner class tracking village aggressors and their last aggression time.
     */
    private static class VillageAgressor {
        public EntityLiving agressor;
        public int agressionTime;

        VillageAgressor(EntityLiving agressor, int time) {
            this.agressor = agressor;
            this.agressionTime = time;
        }
    }
}
