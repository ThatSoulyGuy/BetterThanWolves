package btw.forge;

/**
 * Simple Icon implementation that stores the texture name.
 * Used by the model bridge to capture texture names from FC's registerIcons calls.
 */
public class NamedIcon implements btw.modern.Icon {
    private final String name;

    public NamedIcon(String name) {
        // Normalize: lowercase, strip leading "fcBlock"/"fcItem" if present
        this.name = name.toLowerCase();
    }

    @Override public String getIconName() { return name; }
    @Override public int getOriginX() { return 0; }
    @Override public int getOriginY() { return 0; }
    @Override public float getMinU() { return 0; }
    @Override public float getMaxU() { return 1; }
    @Override public float getInterpolatedU(double d) { return (float)(d / 16.0); }
    @Override public float getMinV() { return 0; }
    @Override public float getMaxV() { return 1; }
    @Override public float getInterpolatedV(double d) { return (float)(d / 16.0); }
    @Override public int getSheetWidth() { return 256; }
    @Override public int getSheetHeight() { return 256; }
    @Override public int getIconWidth() { return 16; }
    @Override public int getIconHeight() { return 16; }
}
