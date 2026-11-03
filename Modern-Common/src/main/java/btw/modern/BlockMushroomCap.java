package btw.modern;

public class BlockMushroomCap extends Block {

    protected BlockMushroomCap(int id, Material material) {
        super(id, material);
    }

    protected BlockMushroomCap(int id, Material material, int mushroomType) {
        super(id, material);
        this.mushroomType = mushroomType;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockMushroomCap's icon members (vanilla/client
    // BlockMushroomCap.java:21-41, :78-89): the brown or red skin (by the constructor's type), the stem and the pores
    // by meta and side. FCBlockMushroomCap (1078 brown, 1079 red) and the legacy caps 99/100 inherit both; Block's
    // default registered the unlocalized "mushroom", the red skin on every face. The port-only 2-argument constructor
    // is type 0 (brown).
    private static final String[] SKIN_ICON_NAMES = new String[] {"mushroom_skin_brown", "mushroom_skin_red"};
    private int mushroomType;
    private Icon[] skinIcons;
    private Icon stemIcon;
    private Icon insideIcon;

    // 1.5.2 BlockMushroomCap.getIcon (vanilla/client BlockMushroomCap.java:38-41); Block's icon until registerIcons
    // runs (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.skinIcons == null) {
            return super.getIcon(side, metadata);
        }
        Icon skin = this.skinIcons[this.mushroomType];
        return metadata == 10 && side > 1 ? this.stemIcon
                : (metadata >= 1 && metadata <= 9 && side == 1 ? skin
                : (metadata >= 1 && metadata <= 3 && side == 2 ? skin
                : (metadata >= 7 && metadata <= 9 && side == 3 ? skin
                : ((metadata == 1 || metadata == 4 || metadata == 7) && side == 4 ? skin
                : ((metadata == 3 || metadata == 6 || metadata == 9) && side == 5 ? skin
                : (metadata == 14 ? skin
                : (metadata == 15 ? this.stemIcon : this.insideIcon)))))));
    }

    // 1.5.2 BlockMushroomCap.registerIcons (vanilla/client BlockMushroomCap.java:78-89)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[SKIN_ICON_NAMES.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(SKIN_ICON_NAMES[i]);
        }
        this.insideIcon = register.registerIcon("mushroom_inside");
        this.stemIcon = register.registerIcon("mushroom_skin_stem");
        this.skinIcons = icons;
    }
}
