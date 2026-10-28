package btw.modern;

public class ItemCarrotOnAStick extends Item {

    public ItemCarrotOnAStick(int id) {
        super(id);
        this.maxStackSize = 1;
        this.setMaxDamage(25);
    }
}
