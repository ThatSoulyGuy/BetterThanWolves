package btw.modern;

public class ItemColored extends ItemBlock {

    public ItemColored(int id, boolean hasSubtypes) {
        super(id);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemColored.getIconFromDamage (vanilla/client
    // ItemColored.java:42-45): the block's bottom icon for the damage. 1.5.2 keeps the block in blockRef, which is the
    // block at this item's own id (:25), so it is read there. ItemBlock's answer while the block or its icon is
    // missing.
    @Override
    public Icon getIconFromDamage(int damage) {
        int id = this.getBlockID();
        Block block = id >= 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
        Icon icon = block != null ? block.getIcon(0, damage) : null;
        return icon != null ? icon : super.getIconFromDamage(damage);
    }
}
