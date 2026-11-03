package btw.modern;

public class BlockCarrot extends BlockCrops {

    protected BlockCarrot(int id) {
        super(id);
    }

    /** 1.5.2 BlockCarrot.getSeedItem (vanilla/server BlockCarrot.java:26-29): a carrot replants the crop. */
    @Override
    public int getSeedItem() {
        return Item.carrot.itemID;
    }

    /** 1.5.2 BlockCarrot.getCropItem (vanilla/server BlockCarrot.java:34-37). */
    @Override
    public int getCropItem() {
        return Item.carrot.itemID;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockCarrot's icon members (vanilla/client BlockCarrot.java:19,
    // :29-44, :66-74): four growth stages over the eight metas.
    private Icon[] iconArray;

    // 1.5.2 BlockCarrot.getIcon (vanilla/client BlockCarrot.java:29-44); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.iconArray == null) {
            return super.getIcon(side, metadata);
        }
        if (metadata < 7) {
            if (metadata == 6) {
                metadata = 5;
            }
            return this.iconArray[metadata >> 1];
        }
        return this.iconArray[3];
    }

    // 1.5.2 BlockCarrot.registerIcons (vanilla/client BlockCarrot.java:66-74)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[4];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon("carrots_" + i);
        }
        this.iconArray = icons;
    }
}
