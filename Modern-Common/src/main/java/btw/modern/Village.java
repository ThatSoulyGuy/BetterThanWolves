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

    public ChunkCoordinates getCenter() { return center; }
    public int getVillageRadius() { return villageRadius; }
    public int getNumVillageDoors() { return villageDoorInfoList.size(); }
    public int func_82784_g() { return 0; }

    public boolean isPlayerReputationTooLow(String playerName) { return false; }
    public int getReputationForPlayer(String playerName) { return 0; }

    public void addOrRenewAgressor(EntityLiving aggressor) {}
}
