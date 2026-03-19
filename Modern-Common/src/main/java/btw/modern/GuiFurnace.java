package btw.modern;

public class GuiFurnace extends GuiContainer {

    private TileEntityFurnace furnaceInventory;

    public GuiFurnace(InventoryPlayer inventoryPlayer, TileEntityFurnace tileEntityFurnace) {
        super(new Container() {
            public boolean canInteractWith(EntityPlayer player) { return true; }
        });
        this.furnaceInventory = tileEntityFurnace;
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {}

    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {}
}
