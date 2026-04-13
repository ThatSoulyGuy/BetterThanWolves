package btw.modern;

/**
 * Vanilla 1.5.2 packet for syncing equipment between server/client.
 * Referenced (transitively) from vanilla EntityLiving, but never actually
 * sent in our bridge — MC 1.20.1's own equipment sync handles that.
 * Type-only stub.
 */
public class Packet5PlayerInventory extends Packet {
    public int entityID;
    public int slot;
    private ItemStack itemStack;

    public Packet5PlayerInventory() {}

    public Packet5PlayerInventory(int entityID, int slot, ItemStack stack) {
        this.entityID = entityID;
        this.slot = slot;
        this.itemStack = stack;
    }

    public ItemStack getItemSlot() { return this.itemStack; }

    public int getPacketSize() { return 0; }
}
