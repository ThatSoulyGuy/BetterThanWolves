package btw.api;

public class ItemHoe extends Item {
    protected EnumToolMaterial theToolMaterial;

    public ItemHoe(int id, EnumToolMaterial material) {
        super(id);
        this.theToolMaterial = material;
    }
}
