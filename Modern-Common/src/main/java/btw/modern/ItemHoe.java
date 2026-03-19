package btw.modern;

public class ItemHoe extends Item {
    protected EnumToolMaterial theToolMaterial;

    public ItemHoe(int id, EnumToolMaterial material) {
        super(id);
        this.theToolMaterial = material;
    }
}
