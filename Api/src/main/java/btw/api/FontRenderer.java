package btw.api;

import java.util.List;

public class FontRenderer {

    /** the height in pixels of default text */
    public int FONT_HEIGHT = 9;

    /**
     * Draws the specified string with a shadow.
     */
    public int drawStringWithShadow(String text, int x, int y, int color) {
        return 0;
    }

    /**
     * Draws the specified string.
     */
    public int drawString(String text, int x, int y, int color) {
        return 0;
    }

    /**
     * Draws the specified string. Args: string, x, y, color, dropShadow
     */
    public int drawString(String text, int x, int y, int color, boolean dropShadow) {
        return 0;
    }

    /**
     * Returns the width of this string. Equivalent of FontMetrics.stringWidth(String s).
     */
    public int getStringWidth(String text) {
        return 0;
    }

    /**
     * Returns the width of this character as rendered.
     */
    public int getCharWidth(char c) {
        return 0;
    }

    /**
     * Trims a string to fit a specified Width.
     */
    public String trimStringToWidth(String text, int width) {
        return text;
    }

    /**
     * Trims a string to a specified width, and will reverse it if par3 is set.
     */
    public String trimStringToWidth(String text, int width, boolean reverse) {
        return text;
    }

    /**
     * Splits and draws a String with wordwrap (maximum length is parameter k)
     */
    public void drawSplitString(String text, int x, int y, int wrapWidth, int color) {}

    /**
     * Returns the width of the wordwrapped String (maximum length is parameter k)
     */
    public int splitStringWidth(String text, int wrapWidth) {
        return 0;
    }

    /**
     * Breaks a string into a list of pieces that will fit a specified width.
     */
    public List listFormattedStringToWidth(String text, int wrapWidth) {
        return null;
    }

    public void setUnicodeFlag(boolean flag) {}

    public boolean getUnicodeFlag() {
        return false;
    }

    public void setBidiFlag(boolean flag) {}

    public boolean getBidiFlag() {
        return false;
    }

    public void readFontData() {}
}
