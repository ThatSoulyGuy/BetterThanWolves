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
}
