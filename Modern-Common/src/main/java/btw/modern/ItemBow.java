package btw.modern;

public class ItemBow extends Item {

    public ItemBow(int id) {
        super(id);
        this.maxStackSize = 1;
        // vanilla/client ItemBow.java:26. FCItemBow inherits it (its damageItem wear, FCItemBow.java:82,
        // was a no-op at max damage 0); FCItemCompositeBow raises it to 576 in its own constructor.
        this.setMaxDamage(384);
        // vanilla/client ItemBow.java:27; FCItemBow / FCItemCompositeBow set no tab of their own
        this.setCreativeTab(CreativeTabs.tabCombat);
    }

    /**
     * vanilla/client ItemBow.java:106-109. FCItemBow.onItemRightClick starts the draw with it
     * (setItemInUse, FCItemBow.java:102) and GetCurrentPullStrength measures ticks in use against it
     * (FCItemBow.java:144-146); at 0 the FC composite bow could never be drawn.
     */
    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    /** vanilla/client ItemBow.java:114-117 (the bow-pull pose). */
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.bow;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemBow's icon members (vanilla/client ItemBow.java:19-20,
    // :140-157): the bow's own icon and the three pull stages. FCItemBow and FCItemCompositeBow inherit them.
    public static final String[] bowPullIconNameArray = new String[] {"bow_pull_0", "bow_pull_1", "bow_pull_2"};
    private Icon[] pullIcons;

    @Override
    public void registerIcons(IconRegister register) {
        super.registerIcons(register);
        Icon[] icons = new Icon[bowPullIconNameArray.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(bowPullIconNameArray[i]);
        }
        this.pullIcons = icons;
    }

    // 1.5.2 ItemBow.getItemIconForUseDuration (vanilla/client ItemBow.java:154-157); the bow's own icon until
    // registerIcons runs (client only)
    public Icon getItemIconForUseDuration(int stage) {
        return this.pullIcons != null ? this.pullIcons[stage] : this.itemIcon;
    }
}
