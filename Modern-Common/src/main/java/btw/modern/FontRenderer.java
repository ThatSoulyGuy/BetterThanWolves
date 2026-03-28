package btw.modern;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub FontRenderer that FC GUI code compiles against.
 *
 * <p>At runtime, container text rendering is handled by Forge's
 * {@code FCContainerScreen} using modern MC's {@code Font} / {@code GuiGraphics}.
 * These methods exist so that legacy FC GUI classes (FCClientGuiHopper,
 * FCClientGuiInfernalEnchanter, etc.) compile and produce reasonable layout
 * values if ever called. They cannot perform real GL rendering, but they
 * return correct advance values so text positioning math does not break
 * (e.g. centering via {@code xSize / 2 - getStringWidth(s) / 2}).</p>
 *
 * <p>Width estimation uses 6 pixels per character (vanilla's average glyph
 * width for the default font), which is close enough for layout math.</p>
 */
public class FontRenderer {

    /** Average character width in pixels for layout estimation. */
    private static final int CHAR_WIDTH = 6;

    /** The height in pixels of default text. */
    public int FONT_HEIGHT = 9;

    /**
     * Draws the specified string with a shadow.
     * Cannot perform real rendering without a GL context, but returns
     * the x-advance so callers that use the return value for layout
     * (e.g. right-aligning level cost text) get correct positions.
     */
    public int drawStringWithShadow(String text, int x, int y, int color) {
        return x + getStringWidth(text);
    }

    /**
     * Draws the specified string.
     */
    public int drawString(String text, int x, int y, int color) {
        return x + getStringWidth(text);
    }

    /**
     * Draws the specified string. Args: string, x, y, color, dropShadow
     */
    public int drawString(String text, int x, int y, int color, boolean dropShadow) {
        return x + getStringWidth(text);
    }

    /**
     * Returns the width of this string. Equivalent of FontMetrics.stringWidth(String s).
     * Uses 6 pixels per character as a reasonable approximation of vanilla's
     * default font metrics. Handles null gracefully.
     */
    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int width = 0;
        boolean isBold = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Handle MC formatting codes (section sign + format char)
            if (c == '\u00a7' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                // 'l' = bold (adds 1 pixel per char), 'r' = reset
                if (code == 'l' || code == 'L') {
                    isBold = true;
                } else if (code == 'r' || code == 'R') {
                    isBold = false;
                }
                i++; // skip the format character
                continue;
            }

            width += CHAR_WIDTH;
            if (isBold) {
                width += 1;
            }
        }

        return width;
    }

    /**
     * Returns the width of this character as rendered.
     */
    public int getCharWidth(char c) {
        if (c == '\u00a7') {
            return -1; // formatting codes have no width, vanilla returns -1
        }
        return CHAR_WIDTH;
    }

    /**
     * Trims a string to fit a specified width.
     */
    public String trimStringToWidth(String text, int width) {
        return trimStringToWidth(text, width, false);
    }

    /**
     * Trims a string to a specified width, optionally from the end (reverse).
     */
    public String trimStringToWidth(String text, int width, boolean reverse) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (getStringWidth(text) <= width) {
            return text;
        }

        if (reverse) {
            // Trim from the beginning
            for (int i = text.length() - 1; i >= 0; i--) {
                String sub = text.substring(i);
                if (getStringWidth(sub) <= width) {
                    return sub;
                }
            }
            return "";
        } else {
            // Trim from the end
            for (int i = text.length(); i > 0; i--) {
                String sub = text.substring(0, i);
                if (getStringWidth(sub) <= width) {
                    return sub;
                }
            }
            return "";
        }
    }

    /**
     * Splits and draws a String with wordwrap (maximum length is parameter k).
     * No-op for rendering, but matches the expected signature.
     */
    public void drawSplitString(String text, int x, int y, int wrapWidth, int color) {
        // Cannot render without GL context; no-op
    }

    /**
     * Returns the height of the wordwrapped String (number of lines * FONT_HEIGHT).
     */
    public int splitStringWidth(String text, int wrapWidth) {
        if (text == null || text.isEmpty() || wrapWidth <= 0) {
            return 0;
        }
        List lines = listFormattedStringToWidth(text, wrapWidth);
        return lines.size() * FONT_HEIGHT;
    }

    /**
     * Breaks a string into a list of pieces that will fit a specified width.
     * Simple word-wrap implementation for layout estimation.
     */
    @SuppressWarnings("unchecked")
    public List listFormattedStringToWidth(String text, int wrapWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        if (wrapWidth <= 0) {
            lines.add(text);
            return lines;
        }

        String remaining = text;
        while (getStringWidth(remaining) > wrapWidth) {
            // Find the longest prefix that fits
            String trimmed = trimStringToWidth(remaining, wrapWidth);
            if (trimmed.isEmpty()) {
                // Even one character doesn't fit; force at least one char to avoid infinite loop
                trimmed = remaining.substring(0, 1);
            }

            // Try to break at last space for word-wrap
            int lastSpace = trimmed.lastIndexOf(' ');
            if (lastSpace >= 0 && lastSpace < remaining.length() - 1) {
                trimmed = trimmed.substring(0, lastSpace + 1);
            }

            lines.add(trimmed);
            remaining = remaining.substring(trimmed.length());
        }

        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }

        return lines;
    }

    private boolean unicodeFlag = false;
    private boolean bidiFlag = false;

    public void setUnicodeFlag(boolean flag) {
        this.unicodeFlag = flag;
    }

    public boolean getUnicodeFlag() {
        return this.unicodeFlag;
    }

    public void setBidiFlag(boolean flag) {
        this.bidiFlag = flag;
    }

    public boolean getBidiFlag() {
        return this.bidiFlag;
    }

    public void readFontData() {}
}
