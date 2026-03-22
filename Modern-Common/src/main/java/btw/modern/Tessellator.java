package btw.modern;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures vertex data from FC's rendering code.
 *
 * FC code calls addVertexWithUV/addVertex to build quads.
 * In MC 1.5.2 these went to OpenGL. Here we capture them
 * so the Forge bridge can convert them to BakedQuads.
 *
 * When capturing is enabled, every 4 vertices form a quad
 * stored in the {@link #capturedQuads} list.
 */
public class Tessellator {

    /** The static instance of the Tessellator. */
    public static final Tessellator instance = new Tessellator();

    /** Whether vertex capture is active. */
    private boolean capturing = false;

    /** Captured quads — each quad is 4 CapturedVertex objects. */
    private final List<CapturedQuad> capturedQuads = new ArrayList<>();

    /** Vertices being accumulated for the current quad. */
    private final List<CapturedVertex> currentVertices = new ArrayList<>();

    // Current state applied to each vertex
    private double curU, curV;
    private float curR = 1, curG = 1, curB = 1, curA = 1;
    private float curNX, curNY, curNZ;
    private int curBrightness = 0xF000F0;
    private double transX, transY, transZ;

    /** The texture name to associate with captured quads (set before adding vertices). */
    private String currentTextureName;

    protected Tessellator() {}

    /** A single captured vertex with position, UV, color, normal. */
    public static class CapturedVertex {
        public final double x, y, z;
        public final double u, v;
        public final float r, g, b, a;
        public final float nx, ny, nz;
        public final int brightness;

        public CapturedVertex(double x, double y, double z, double u, double v,
                              float r, float g, float b, float a,
                              float nx, float ny, float nz, int brightness) {
            this.x = x; this.y = y; this.z = z;
            this.u = u; this.v = v;
            this.r = r; this.g = g; this.b = b; this.a = a;
            this.nx = nx; this.ny = ny; this.nz = nz;
            this.brightness = brightness;
        }
    }

    /** A captured quad (4 vertices) with its associated texture name. */
    public static class CapturedQuad {
        public final CapturedVertex[] vertices = new CapturedVertex[4];
        /** The texture/icon name for this quad (from the Icon passed to the face method). */
        public String textureName;
    }

    // ================================================================
    // Capture control
    // ================================================================

    /** Enable vertex capture mode. Clears any previously captured data. */
    public void startCapturing() {
        capturing = true;
        capturedQuads.clear();
        currentVertices.clear();
        currentTextureName = null;
    }

    /** Disable capture mode and return captured quads. */
    public List<CapturedQuad> stopCapturing() {
        capturing = false;
        List<CapturedQuad> result = new ArrayList<>(capturedQuads);
        capturedQuads.clear();
        currentVertices.clear();
        return result;
    }

    /** Whether capture mode is active. */
    public boolean isCapturing() {
        return capturing;
    }

    // ================================================================
    // Drawing lifecycle
    // ================================================================

    public int draw() {
        currentVertices.clear();
        return 0;
    }

    public void startDrawingQuads() {
        currentVertices.clear();
    }

    public void startDrawing(int drawMode) {
        currentVertices.clear();
    }

    // ================================================================
    // Vertex state
    // ================================================================

    public void setTextureUV(double u, double v) {
        this.curU = u;
        this.curV = v;
    }

    public void setBrightness(int brightness) {
        this.curBrightness = brightness;
    }

    public void setColorOpaque_F(float r, float g, float b) {
        this.curR = r; this.curG = g; this.curB = b; this.curA = 1.0f;
    }

    public void setColorRGBA_F(float r, float g, float b, float a) {
        this.curR = r; this.curG = g; this.curB = b; this.curA = a;
    }

    public void setColorOpaque(int r, int g, int b) {
        this.curR = r / 255f; this.curG = g / 255f; this.curB = b / 255f; this.curA = 1.0f;
    }

    public void setColorRGBA(int r, int g, int b, int a) {
        this.curR = Math.min(255, Math.max(0, r)) / 255f;
        this.curG = Math.min(255, Math.max(0, g)) / 255f;
        this.curB = Math.min(255, Math.max(0, b)) / 255f;
        this.curA = Math.min(255, Math.max(0, a)) / 255f;
    }

    public void setColorOpaque_I(int color) {
        this.curR = ((color >> 16) & 0xFF) / 255f;
        this.curG = ((color >> 8) & 0xFF) / 255f;
        this.curB = (color & 0xFF) / 255f;
        this.curA = 1.0f;
    }

    public void setColorRGBA_I(int color, int alpha) {
        this.curR = ((color >> 16) & 0xFF) / 255f;
        this.curG = ((color >> 8) & 0xFF) / 255f;
        this.curB = (color & 0xFF) / 255f;
        this.curA = alpha / 255f;
    }

    public void disableColor() {
        this.curR = 1; this.curG = 1; this.curB = 1; this.curA = 1;
    }

    public void setNormal(float x, float y, float z) {
        this.curNX = x; this.curNY = y; this.curNZ = z;
    }

    public void setTranslation(double x, double y, double z) {
        this.transX = x; this.transY = y; this.transZ = z;
    }

    public void addTranslation(float x, float y, float z) {
        this.transX += x; this.transY += y; this.transZ += z;
    }

    /**
     * Sets the texture name to associate with subsequently captured quads.
     * Called by face rendering methods before emitting vertices so that
     * the captured quad knows which sprite to resolve later.
     */
    public void setCurrentTextureName(String name) {
        this.currentTextureName = name;
    }

    // ================================================================
    // Vertex submission
    // ================================================================

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
        this.curU = u;
        this.curV = v;
        addVertex(x, y, z);
    }

    public void addVertex(double x, double y, double z) {
        if (!capturing) return;

        double vx = x + transX, vy = y + transY, vz = z + transZ;
        double nx = curNX, ny = curNY, nz = curNZ;

        // Apply GL11 matrix stack if tracking is active (entity/TE rendering)
        if (GL11.isMatrixTrackingEnabled()) {
            float[] p = GL11.transformPoint((float) vx, (float) vy, (float) vz);
            vx = p[0]; vy = p[1]; vz = p[2];
            float[] n = GL11.transformNormal((float) nx, (float) ny, (float) nz);
            nx = n[0]; ny = n[1]; nz = n[2];
        }

        CapturedVertex vert = new CapturedVertex(
                vx, vy, vz,
                curU, curV,
                curR, curG, curB, curA,
                (float) nx, (float) ny, (float) nz,
                curBrightness
        );
        currentVertices.add(vert);

        // Every 4 vertices = one quad
        if (currentVertices.size() == 4) {
            CapturedQuad quad = new CapturedQuad();
            for (int i = 0; i < 4; i++) {
                quad.vertices[i] = currentVertices.get(i);
            }
            quad.textureName = currentTextureName;
            capturedQuads.add(quad);
            currentVertices.clear();
        }
    }
}
