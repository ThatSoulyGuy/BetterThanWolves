package btw.modern;

public class BlockPotato extends BlockCrops {

    protected BlockPotato(int id) {
        super(id);
    }

    /** 1.5.2 BlockPotato.getSeedItem (vanilla/server BlockPotato.java:26-29): a potato replants the crop. */
    @Override
    public int getSeedItem() {
        return Item.potato.itemID;
    }

    /** 1.5.2 BlockPotato.getCropItem (vanilla/server BlockPotato.java:34-37). */
    @Override
    public int getCropItem() {
        return Item.potato.itemID;
    }

    /**
     * 1.5.2 BlockPotato.dropBlockAsItemWithChance (vanilla/server BlockPotato.java:42-53): a fully grown potato
     * also drops a poisonous potato one time in 50.
     */
    @Override
    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int metadata,
            float chance, int fortune) {
        super.dropBlockAsItemWithChance(world, x, y, z, metadata, chance, fortune);

        if (!world.isRemote) {
            if (metadata >= 7 && world.rand.nextInt(50) == 0) {
                dropBlockAsItem_do(world, x, y, z, new ItemStack(Item.poisonousPotato));
            }
        }
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockPotato's icon members (vanilla/client BlockPotato.java:19,
    // :29-44, :82-90): four growth stages over the eight metas.
    private Icon[] iconArray;

    // 1.5.2 BlockPotato.getIcon (vanilla/client BlockPotato.java:29-44); Block's icon until registerIcons runs (client
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

    // 1.5.2 BlockPotato.registerIcons (vanilla/client BlockPotato.java:82-90)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[4];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon("potatoes_" + i);
        }
        this.iconArray = icons;
    }
}
