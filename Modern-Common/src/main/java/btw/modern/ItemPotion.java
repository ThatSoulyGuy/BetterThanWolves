package btw.modern;

public class ItemPotion extends Item {

    public ItemPotion(int id) {
        super(id);
    }

    public static boolean isSplash(int metadata) {
        return (metadata & 16384) != 0;
    }

    public java.util.List getEffects(ItemStack stack) { return null; }
    public java.util.List getEffects(int metadata) { return null; }
}
