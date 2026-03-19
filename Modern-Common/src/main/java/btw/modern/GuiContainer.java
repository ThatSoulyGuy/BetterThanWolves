package btw.modern;

import java.util.List;
import java.util.Set;

public abstract class GuiContainer extends GuiScreen {

    /** The X size of the inventory window in pixels. */
    protected int xSize = 176;

    /** The Y size of the inventory window in pixels. */
    protected int ySize = 166;

    /** A list of the players inventory slots. */
    public Container inventorySlots;

    /** Starting X position for the Gui. */
    protected int guiLeft;

    /** Starting Y position for the Gui. */
    protected int guiTop;

    protected final Set field_94077_p;
    protected boolean field_94076_q;

    public GuiContainer(Container container) {
        this.inventorySlots = container;
        this.field_94077_p = new java.util.HashSet();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {}

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {}

    /**
     * Draw the background layer for the GuiContainer (everything behind the items)
     */
    protected abstract void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY);

    protected void drawItemStackTooltip(ItemStack itemStack, int x, int y) {}

    protected void drawCreativeTabHoveringText(String text, int x, int y) {}

    protected void func_102021_a(List tooltipLines, int x, int y) {}

    protected boolean isPointInRegion(int left, int top, int width, int height, int pointX, int pointY) {
        return false;
    }

    protected void handleMouseClick(Slot slot, int slotNumber, int mouseButton, int type) {}

    protected boolean checkHotbarKeys(int keyCode) {
        return false;
    }

    @Override
    public void onGuiClosed() {}

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {}

    public static boolean InstallationIntegrityTest() { return true; }
}
