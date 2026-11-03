package btw.modern;

public class ItemBlockWithMetadata extends ItemBlock {

    public ItemBlockWithMetadata(int id, Block block) {
        super(id);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemBlockWithMetadata.getIconFromDamage (vanilla/client
    // ItemBlockWithMetadata.java:32-35): the block's side icon for the damage. 1.5.2 keeps the block in a field; it is
    // the block at this item's own id (FCBetterThanWolves.java:1880-1896, FCItemBlockSnow), so it is read there.
    // ItemBlock's answer while the block or its icon is missing.
    @Override
    public Icon getIconFromDamage(int damage) {
        int id = this.getBlockID();
        Block block = id >= 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
        Icon icon = block != null ? block.getIcon(2, damage) : null;
        return icon != null ? icon : super.getIconFromDamage(damage);
    }
}
