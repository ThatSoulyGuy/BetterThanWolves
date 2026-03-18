package btw.api;

public class CreativeTabs {

    public static final CreativeTabs[] creativeTabArray = new CreativeTabs[12];
    public static final CreativeTabs tabBlock = new CreativeTabs(0, "buildingBlocks");
    public static final CreativeTabs tabDecorations = new CreativeTabs(1, "decorations");
    public static final CreativeTabs tabRedstone = new CreativeTabs(2, "redstone");
    public static final CreativeTabs tabTransport = new CreativeTabs(3, "transportation");
    public static final CreativeTabs tabMisc = new CreativeTabs(4, "misc");
    public static final CreativeTabs tabAllSearch = new CreativeTabs(5, "search");
    public static final CreativeTabs tabFood = new CreativeTabs(6, "food");
    public static final CreativeTabs tabTools = new CreativeTabs(7, "tools");
    public static final CreativeTabs tabCombat = new CreativeTabs(8, "combat");
    public static final CreativeTabs tabBrewing = new CreativeTabs(9, "brewing");
    public static final CreativeTabs tabMaterials = new CreativeTabs(10, "materials");
    public static final CreativeTabs tabInventory = new CreativeTabs(11, "inventory");

    private final int tabIndex;
    private final String tabLabel;

    public CreativeTabs(int index, String label) {
        this.tabIndex = index;
        this.tabLabel = label;
        if (index >= 0 && index < creativeTabArray.length) {
            creativeTabArray[index] = this;
        }
    }

    public int getTabIndex() {
        return this.tabIndex;
    }

    public String getTabLabel() {
        return this.tabLabel;
    }
}
