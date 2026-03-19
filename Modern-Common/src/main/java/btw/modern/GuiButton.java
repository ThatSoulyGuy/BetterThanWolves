package btw.modern;

public class GuiButton extends Gui {
    public int id;
    public int xPosition;
    public int yPosition;
    public int width;
    public int height;
    public String displayString;
    public boolean enabled;
    public boolean drawButton;

    public GuiButton(int id, int x, int y, String text) {
        this.id = id;
        this.xPosition = x;
        this.yPosition = y;
        this.displayString = text;
    }

    public GuiButton(int id, int x, int y, int width, int height, String text) {
        this.id = id;
        this.xPosition = x;
        this.yPosition = y;
        this.width = width;
        this.height = height;
        this.displayString = text;
    }
}
