package btw.modern;

public class ItemFishingRod extends Item {

    public ItemFishingRod(int id) {
        super(id);
        this.maxStackSize = 1;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemFishingRod's icon members (vanilla/client
    // ItemFishingRod.java:19, :72-81): the rod's own icon and the cast (empty) rod. FCItemFishingRod and
    // FCItemFishingRodBaited (Client FCItemFishingRodBaited.java:78) read func_94597_g.
    private Icon castIcon;

    @Override
    public void registerIcons(IconRegister register) {
        super.registerIcons(register);
        this.castIcon = register.registerIcon("fishingRod_empty");
    }

    // 1.5.2 ItemFishingRod.func_94597_g (vanilla/client ItemFishingRod.java:78-81); Item's answer until registerIcons
    // runs (client only)
    @Override
    public Icon func_94597_g() {
        return this.castIcon != null ? this.castIcon : super.func_94597_g();
    }
}
