package btw.modern;

/**
 * Stub for vanilla 1.5.2's Achievement class. Vanilla EntityItem calls
 * {@code Player.triggerAchievement(AchievementList.X)} on certain pickups
 * (mineWood, killCow, diamonds, blazeRod, etc.). FC doesn't use the
 * achievement system for any gameplay logic, so we expose just enough of
 * the API to keep classloading happy.
 */
public class Achievement extends StatBase {
    public final int displayColumn;
    public final int displayRow;
    public final Achievement parentAchievement;
    public final ItemStack theItemStack;

    public Achievement(int id, String name, int col, int row, ItemStack icon, Achievement parent) {
        super(id, name);
        this.displayColumn = col;
        this.displayRow = row;
        this.theItemStack = icon;
        this.parentAchievement = parent;
    }

    public Achievement(int id, String name, int col, int row, Item icon, Achievement parent) {
        this(id, name, col, row, icon != null ? new ItemStack(icon) : null, parent);
    }

    public Achievement(int id, String name, int col, int row, Block icon, Achievement parent) {
        this(id, name, col, row, icon != null ? new ItemStack(icon) : null, parent);
    }

    public Achievement setSpecial() { return this; }
    public Achievement setIndependent() { return this; }
    public Achievement registerAchievement() { return this; }
}
