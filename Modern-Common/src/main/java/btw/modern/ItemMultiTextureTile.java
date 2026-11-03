package btw.modern;

public class ItemMultiTextureTile extends ItemBlock {

    public ItemMultiTextureTile(int id, Block block, String[] names) {
        super(id);
        // FCMOD-INTEGRATION(1.20.1): item_subtypes: 1.5.2 ItemMultiTextureTile declares subtypes (vanilla/server ItemMultiTextureTile.java:27): FC's planks (5) and logs (FCItemBlockLog at 17 and log2 165) stack by species in BTW storage, and FC's log trades take their own species
        this.setHasSubtypes(true);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemMultiTextureTile.getIconFromDamage (vanilla/client
    // ItemMultiTextureTile.java:34-37): the block's side icon for the damage. 1.5.2 keeps the block in a field; FC
    // passes the block at this item's own id (FCBetterThanWolves.java:1880-1881, ModernWoods.java:210), so it is read
    // there. ItemBlock's answer while the block or its icon is missing.
    @Override
    public Icon getIconFromDamage(int damage) {
        int id = this.getBlockID();
        Block block = id >= 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
        Icon icon = block != null ? block.getIcon(2, damage) : null;
        return icon != null ? icon : super.getIconFromDamage(damage);
    }
}
