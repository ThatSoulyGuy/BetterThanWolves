package btw.api;

import java.util.List;

public class GuiScreen extends Gui {

    /** Reference to the Minecraft object. */
    protected Minecraft mc;

    /** The width of the screen object. */
    public int width;

    /** The height of the screen object. */
    public int height;

    /** A list of all the buttons in this container. */
    protected List buttonList;

    public boolean allowUserInput = false;

    /** The FontRenderer used by GuiScreen */
    protected FontRenderer fontRenderer;

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {}

    /**
     * Fired when a key is typed.
     */
    protected void keyTyped(char typedChar, int keyCode) {}

    /**
     * Called when the mouse is clicked.
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {}

    /**
     * Called when the mouse is moved or a mouse button is released.
     */
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {}

    protected void func_85041_a(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {}

    protected void actionPerformed(GuiButton button) {}

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui() {}

    public void handleInput() {}
    public void handleMouseInput() {}
    public void handleKeyboardInput() {}

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen() {}

    /**
     * Called when the screen is unloaded.
     */
    public void onGuiClosed() {}

    public void drawDefaultBackground() {}
    public void drawWorldBackground(int tint) {}
    public void drawBackground(int tint) {}

    /**
     * Returns true if this GUI should pause the game when it is displayed in single-player
     */
    public boolean doesGuiPauseGame() {
        return true;
    }

    public void confirmClicked(boolean result, int id) {}

    public static boolean isCtrlKeyDown() {
        return false;
    }

    public static boolean isShiftKeyDown() {
        return false;
    }
}
