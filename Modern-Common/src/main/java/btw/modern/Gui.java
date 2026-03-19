package btw.modern;

public class Gui {

    protected float zLevel = 0.0F;

    protected void drawHorizontalLine(int x1, int x2, int y, int color) {}

    protected void drawVerticalLine(int x, int y1, int y2, int color) {}

    public static void drawRect(int x1, int y1, int x2, int y2, int color) {}

    protected void drawGradientRect(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {}

    public void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int color) {}

    public void drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {}

    public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height) {}

    public void drawTexturedModelRectFromIcon(int x, int y, Icon icon, int width, int height) {}
}
