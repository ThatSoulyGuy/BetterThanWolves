package btw.modern;

import java.util.LinkedList;
import java.util.List;

public class EntityPlayerMP extends EntityPlayer implements ICrafting {
    public NetServerHandler playerNetServerHandler;
    public MinecraftServer mcServer;
    public ItemInWorldManager theItemInWorldManager;
    public LinkedList<ChunkCoordIntPair> m_chunksToBeSentToClient = new LinkedList<ChunkCoordIntPair>();
    public double managedPosX;
    public double managedPosZ;
    public final List loadedChunks = new LinkedList();
    public final List destroyedItemsNetCache = new LinkedList();
    public boolean isChangingQuantityOnly;
    public int ping;
    public boolean playerConqueredTheEnd = false;
    public long m_lTimeOfLastSpawnAssignment;
    public long m_lRespawnAssignmentCooldownTimer;
    public ChunkCoordinates m_HardcoreSpawnChunk;

    public EntityPlayerMP(World world) {
        super(world);
    }

    public void entityInit() {}
    public void sendChatToPlayer(String message) {}
    public void sendContainerToPlayer(Container container) {}
    public void sendContainerAndContentsToPlayer(Container container, List items) {}
    public void sendSlotContents(Container container, int slot, ItemStack stack) {}
    public void sendProgressBarUpdate(Container container, int id, int value) {}
    public void updateCraftingInventory(Container container, List items) {}
    public int getMaxHealth() { return 20; }
}
