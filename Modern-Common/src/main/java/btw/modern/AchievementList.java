package btw.modern;

/**
 * Stub for vanilla 1.5.2's AchievementList. Vanilla 1.5.2 EntityItem
 * triggers a small number of milestones on player pickup. FC doesn't read
 * the achievement system, so the static fields are just non-null sentinels;
 * we deliberately do NOT instantiate them with Item/Block icons because
 * doing so would drag the static-init chain back through Item.&lt;clinit&gt;
 * → FCBetterThanWolves and break the boot order.
 */
public class AchievementList {
    public static final Achievement mineWood = new Achievement(1, "mineWood", 2, 1, (ItemStack)null, null);
    public static final Achievement killCow = new Achievement(14, "killCow", 7, -3, (ItemStack)null, null);
    public static final Achievement diamonds = new Achievement(17, "diamonds", -1, 5, (ItemStack)null, null);
    public static final Achievement blazeRod = new Achievement(20, "blazeRod", 7, 8, (ItemStack)null, null);
    public static final Achievement openInventory = new Achievement(0, "openInventory", 0, 0, (ItemStack)null, null);
    public static final Achievement onARail = new Achievement(11, "onARail", 2, 3, (ItemStack)null, null);
    public static final Achievement killEnemy = new Achievement(13, "killEnemy", 8, -1, (ItemStack)null, null);
    public static final Achievement overkill = new Achievement(28, "overkill", 8, 4, (ItemStack)null, null);
    public static final Achievement snipeSkeleton = new Achievement(29, "snipeSkeleton", 7, -3, (ItemStack)null, null);
    public static final Achievement ghast = new Achievement(30, "ghast", 4, 8, (ItemStack)null, null);
    public static final Achievement flyPig = new Achievement(31, "flyPig", 6, 9, (ItemStack)null, null);
    public static final Achievement portal = new Achievement(32, "portal", 0, 6, (ItemStack)null, null);
    public static final Achievement theEnd = new Achievement(33, "theEnd", 1, 7, (ItemStack)null, null);
    public static final Achievement theEnd2 = new Achievement(34, "theEnd2", 2, 7, (ItemStack)null, null);

    // 1.5.2 crafting achievements (vanilla/client AchievementList.java:44-114) — read by
    // SlotCrafting.onCrafting's dispatch chain; same null-icon sentinel pattern as above.
    public static final Achievement buildWorkBench = new Achievement(2, "buildWorkBench", 4, -1, (ItemStack)null, mineWood);
    public static final Achievement buildPickaxe = new Achievement(3, "buildPickaxe", 4, 2, (ItemStack)null, buildWorkBench);
    public static final Achievement buildFurnace = new Achievement(4, "buildFurnace", 3, 4, (ItemStack)null, buildPickaxe);
    public static final Achievement buildHoe = new Achievement(6, "buildHoe", 2, -3, (ItemStack)null, buildWorkBench);
    public static final Achievement makeBread = new Achievement(7, "makeBread", -1, -3, (ItemStack)null, buildHoe);
    public static final Achievement bakeCake = new Achievement(8, "bakeCake", 0, -5, (ItemStack)null, buildHoe);
    public static final Achievement buildBetterPickaxe = new Achievement(9, "buildBetterPickaxe", 6, 2, (ItemStack)null, buildPickaxe);
    public static final Achievement buildSword = new Achievement(12, "buildSword", 6, -1, (ItemStack)null, buildWorkBench);
    public static final Achievement enchantments = new Achievement(24, "enchantments", -4, 4, (ItemStack)null, diamonds);
    public static final Achievement bookcase = new Achievement(26, "bookcase", -3, 6, (ItemStack)null, enchantments);
    // Achievements referenced by SlotFurnace/SlotBrewingStandPotion (fc-frozen, currently dead)
    public static final Achievement acquireIron = new Achievement(5, "acquireIron", 1, 4, (ItemStack)null, null);
    public static final Achievement cookFish = new Achievement(15, "cookFish", 2, 6, (ItemStack)null, null);
    public static final Achievement potion = new Achievement(21, "potion", 5, 8, (ItemStack)null, null);
}
