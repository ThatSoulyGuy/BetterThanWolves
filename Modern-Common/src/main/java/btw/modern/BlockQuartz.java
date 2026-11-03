package btw.modern;

public class BlockQuartz extends Block {

    protected BlockQuartz(int id) {
        super(id, Material.rock);
    }

    // 1.5.2 BlockQuartz.getRenderType (vanilla/client BlockQuartz.java:117-120). FCBlockBlackStone extends it and
    // does not override it.
    @Override
    public int getRenderType() {
        return 39;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockQuartz's icon members (vanilla/client BlockQuartz.java:22-27,
    // :38-67, :136-156): the plain, chiseled and pillar sides (the pillar's end by its axis meta 2-4), their tops and
    // the bottom. FCBlockBlackStone (155) registers and answers its own (Client FCBlockBlackStone.java:42-90) without
    // calling these, so nothing draws them.
    private static final String[] quartzBlockTextureTypes = new String[] {"quartzblock_side", "quartzblock_chiseled",
            "quartzblock_lines", null, null};
    private Icon[] quartzblockIcons;
    private Icon quartzblockChiseledTopIcon;
    private Icon quartzblockLinesTopIcon;
    private Icon quartzblockTopIcon;
    private Icon quartzblockBottomIcon;

    // 1.5.2 BlockQuartz.getIcon (vanilla/client BlockQuartz.java:38-67); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.quartzblockIcons == null || this.quartzblockTopIcon == null || this.quartzblockBottomIcon == null
                || this.quartzblockChiseledTopIcon == null || this.quartzblockLinesTopIcon == null) {
            return super.getIcon(side, metadata);
        }
        if (metadata != 2 && metadata != 3 && metadata != 4) {
            if (side != 1 && (side != 0 || metadata != 1)) {
                if (side == 0) {
                    return this.quartzblockBottomIcon;
                }
                if (metadata < 0 || metadata >= this.quartzblockIcons.length) {
                    metadata = 0;
                }
                return this.quartzblockIcons[metadata];
            }
            return metadata == 1 ? this.quartzblockChiseledTopIcon : this.quartzblockTopIcon;
        }
        return metadata == 2 && (side == 1 || side == 0) ? this.quartzblockLinesTopIcon
                : (metadata == 3 && (side == 5 || side == 4) ? this.quartzblockLinesTopIcon
                : (metadata == 4 && (side == 2 || side == 3) ? this.quartzblockLinesTopIcon
                : this.quartzblockIcons[metadata]));
    }

    // 1.5.2 BlockQuartz.registerIcons (vanilla/client BlockQuartz.java:136-156)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[quartzBlockTextureTypes.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = quartzBlockTextureTypes[i] == null ? icons[i - 1]
                    : register.registerIcon(quartzBlockTextureTypes[i]);
        }
        this.quartzblockTopIcon = register.registerIcon("quartzblock_top");
        this.quartzblockChiseledTopIcon = register.registerIcon("quartzblock_chiseled_top");
        this.quartzblockLinesTopIcon = register.registerIcon("quartzblock_lines_top");
        this.quartzblockBottomIcon = register.registerIcon("quartzblock_bottom");
        this.quartzblockIcons = icons;
    }
}
