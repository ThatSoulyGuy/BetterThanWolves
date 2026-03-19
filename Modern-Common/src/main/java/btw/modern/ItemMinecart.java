package btw.modern;

public class ItemMinecart extends Item {

    public int minecartType;

    public ItemMinecart(int id, int minecartType) {
        super(id);
        this.maxStackSize = 1;
        this.minecartType = minecartType;
    }
}
