package btw.api;

/**
 * Item for placing doors.
 * Mirrors net.minecraft.src.ItemDoor.
 */
public class ItemDoor extends Item {

    public ItemDoor(int id, Material material) {
        super(id);
    }

    public static void placeDoorBlock(World world, int x, int y, int z, int direction, Block doorBlock) {}
}
