package btw.modern;

public class Tessellator {

    /** The static instance of the Tessellator. */
    public static final Tessellator instance = new Tessellator();

    protected Tessellator() {}

    /**
     * Draws the data set up in this tessellator and resets the state to prepare for new drawing.
     */
    public int draw() {
        return 0;
    }

    /**
     * Sets draw mode in the tessellator to draw quads.
     */
    public void startDrawingQuads() {}

    /**
     * Resets tessellator state and prepares for drawing (with the specified draw mode).
     */
    public void startDrawing(int drawMode) {}

    /**
     * Sets the texture coordinates.
     */
    public void setTextureUV(double u, double v) {}

    public void setBrightness(int brightness) {}

    /**
     * Sets the RGB values as specified, converting from floats between 0 and 1 to integers from 0-255.
     */
    public void setColorOpaque_F(float r, float g, float b) {}

    /**
     * Sets the RGBA values for the color, converting from floats between 0 and 1 to integers from 0-255.
     */
    public void setColorRGBA_F(float r, float g, float b, float a) {}

    /**
     * Sets the RGB values as specified, and sets alpha to opaque.
     */
    public void setColorOpaque(int r, int g, int b) {}

    /**
     * Sets the RGBA values for the color. Also clamps them to 0-255.
     */
    public void setColorRGBA(int r, int g, int b, int a) {}

    /**
     * Adds a vertex specifying both x,y,z and the texture u,v for it.
     */
    public void addVertexWithUV(double x, double y, double z, double u, double v) {}

    /**
     * Adds a vertex with the specified x,y,z to the current draw call.
     */
    public void addVertex(double x, double y, double z) {}

    /**
     * Sets the color to the given opaque value (stored as byte values packed in an integer).
     */
    public void setColorOpaque_I(int color) {}

    /**
     * Sets the color to the given color (packed as bytes in integer) and alpha values.
     */
    public void setColorRGBA_I(int color, int alpha) {}

    /**
     * Disables colors for the current draw call.
     */
    public void disableColor() {}

    /**
     * Sets the normal for the current draw call.
     */
    public void setNormal(float x, float y, float z) {}

    /**
     * Sets the translation for all vertices in the current draw call.
     */
    public void setTranslation(double x, double y, double z) {}

    /**
     * Offsets the translation for all vertices in the current draw call.
     */
    public void addTranslation(float x, float y, float z) {}
}
