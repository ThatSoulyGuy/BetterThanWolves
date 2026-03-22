package btw.modern;

/**
 * GL11 replacement with a functional matrix stack for entity/tile entity rendering.
 *
 * <p>Tracks translate/rotate/scale transforms in a software matrix stack.
 * The Tessellator reads the current matrix via {@link #getMatrix()} and
 * applies it to vertex positions, producing correctly-transformed geometry
 * for FC entity renderers (windmill, water wheel, baskets, etc.).</p>
 *
 * <p>Enable/disable, color, blend, and other GL state calls remain no-ops
 * since MC 1.20.1 uses its own rendering pipeline for these.</p>
 */
public class GL11 {

    // --- Constants (same values as real GL11) ---
    public static final int GL_ALPHA_TEST = 0x0BC0;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_COLOR_MATERIAL = 0x0B57;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_LIGHTING = 0x0B50;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_SMOOTH = 0x1D01;
    public static final int GL_FLAT = 0x1D00;
    public static final int GL_FRONT_AND_BACK = 0x0408;
    public static final int GL_AMBIENT_AND_DIFFUSE = 0x1602;
    public static final int GL_MODELVIEW = 0x1700;
    public static final int GL_PROJECTION = 0x1701;
    public static final int GL_QUADS = 0x0007;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_NORMALIZE = 0x0BA1;
    public static final int GL_RESCALE_NORMAL = 0x803A;
    public static final int GL_FOG = 0x0B60;

    // ================================================================
    // Software matrix stack
    // ================================================================

    /** 4x4 matrix stored as float[16] in column-major order (OpenGL convention). */
    private static final int STACK_DEPTH = 32;
    private static final float[][] matrixStack = new float[STACK_DEPTH][16];
    private static int stackPointer = 0;

    /** Whether matrix tracking is active (only during entity/TE rendering capture). */
    private static boolean matrixTrackingEnabled = false;

    static {
        loadIdentity(matrixStack[0]);
    }

    /** Enable matrix tracking for entity rendering capture. */
    public static void enableMatrixTracking() {
        matrixTrackingEnabled = true;
        stackPointer = 0;
        loadIdentity(matrixStack[0]);
    }

    /** Disable matrix tracking (returns to no-op mode for block rendering). */
    public static void disableMatrixTracking() {
        matrixTrackingEnabled = false;
    }

    public static boolean isMatrixTrackingEnabled() {
        return matrixTrackingEnabled;
    }

    /** Returns a copy of the current 4x4 matrix (column-major). */
    public static float[] getMatrix() {
        return matrixStack[stackPointer].clone();
    }

    /** Transforms a point (x,y,z) by the current matrix. Returns {x',y',z'}. */
    public static float[] transformPoint(float x, float y, float z) {
        float[] m = matrixStack[stackPointer];
        float rx = m[0]*x + m[4]*y + m[8]*z  + m[12];
        float ry = m[1]*x + m[5]*y + m[9]*z  + m[13];
        float rz = m[2]*x + m[6]*y + m[10]*z + m[14];
        return new float[]{rx, ry, rz};
    }

    /** Transforms a normal (x,y,z) by the current matrix (no translation). */
    public static float[] transformNormal(float nx, float ny, float nz) {
        float[] m = matrixStack[stackPointer];
        float rx = m[0]*nx + m[4]*ny + m[8]*nz;
        float ry = m[1]*nx + m[5]*ny + m[9]*nz;
        float rz = m[2]*nx + m[6]*ny + m[10]*nz;
        float len = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
        if (len > 0.0001f) { rx /= len; ry /= len; rz /= len; }
        return new float[]{rx, ry, rz};
    }

    // --- Matrix operations ---

    public static void glPushMatrix() {
        if (!matrixTrackingEnabled) return;
        if (stackPointer < STACK_DEPTH - 1) {
            System.arraycopy(matrixStack[stackPointer], 0, matrixStack[stackPointer + 1], 0, 16);
            stackPointer++;
        }
    }

    public static void glPopMatrix() {
        if (!matrixTrackingEnabled) return;
        if (stackPointer > 0) stackPointer--;
    }

    public static void glTranslatef(float x, float y, float z) {
        if (!matrixTrackingEnabled) return;
        float[] m = matrixStack[stackPointer];
        m[12] += m[0]*x + m[4]*y + m[8]*z;
        m[13] += m[1]*x + m[5]*y + m[9]*z;
        m[14] += m[2]*x + m[6]*y + m[10]*z;
    }

    public static void glTranslated(double x, double y, double z) {
        glTranslatef((float)x, (float)y, (float)z);
    }

    public static void glRotatef(float angle, float ax, float ay, float az) {
        if (!matrixTrackingEnabled) return;
        float rad = (float) Math.toRadians(angle);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);
        float len = (float) Math.sqrt(ax*ax + ay*ay + az*az);
        if (len < 0.0001f) return;
        ax /= len; ay /= len; az /= len;
        float t = 1 - c;

        float[] r = new float[16];
        r[0]  = t*ax*ax + c;      r[4]  = t*ax*ay - s*az;  r[8]  = t*ax*az + s*ay;  r[12] = 0;
        r[1]  = t*ax*ay + s*az;   r[5]  = t*ay*ay + c;     r[9]  = t*ay*az - s*ax;  r[13] = 0;
        r[2]  = t*ax*az - s*ay;   r[6]  = t*ay*az + s*ax;  r[10] = t*az*az + c;     r[14] = 0;
        r[3]  = 0;                 r[7]  = 0;               r[11] = 0;               r[15] = 1;

        multiplyMatrix(matrixStack[stackPointer], r);
    }

    public static void glScalef(float sx, float sy, float sz) {
        if (!matrixTrackingEnabled) return;
        float[] m = matrixStack[stackPointer];
        m[0] *= sx; m[1] *= sx; m[2] *= sx;
        m[4] *= sy; m[5] *= sy; m[6] *= sy;
        m[8] *= sz; m[9] *= sz; m[10] *= sz;
    }

    public static void glScaled(double x, double y, double z) {
        glScalef((float)x, (float)y, (float)z);
    }

    public static void glLoadIdentity() {
        if (!matrixTrackingEnabled) return;
        loadIdentity(matrixStack[stackPointer]);
    }

    public static void glMatrixMode(int mode) {}
    public static void glMultMatrixf(java.nio.FloatBuffer m) {}
    public static void glOrtho(double l, double r, double b, double t, double n, double f) {}

    // --- State enable/disable (no-op) ---
    public static void glEnable(int cap) {}
    public static void glDisable(int cap) {}

    // --- Color (tracked for vertex coloring) ---
    private static float colorR = 1, colorG = 1, colorB = 1, colorA = 1;

    public static void glColor3f(float r, float g, float b) {
        colorR = r; colorG = g; colorB = b; colorA = 1;
    }
    public static void glColor4f(float r, float g, float b, float a) {
        colorR = r; colorG = g; colorB = b; colorA = a;
    }
    public static float[] getColor() { return new float[]{colorR, colorG, colorB, colorA}; }

    public static void glColorMaterial(int face, int mode) {}

    // --- Depth (no-op) ---
    public static void glDepthMask(boolean flag) {}

    // --- Normal (tracked) ---
    private static float normalX, normalY = 1, normalZ;
    public static void glNormal3f(float x, float y, float z) {
        normalX = x; normalY = y; normalZ = z;
    }
    public static float[] getNormal() { return new float[]{normalX, normalY, normalZ}; }

    // --- Blending (no-op) ---
    public static void glBlendFunc(int sfactor, int dfactor) {}

    // --- Texture (no-op) ---
    public static void glBindTexture(int target, int texture) {}
    public static void glTexParameteri(int target, int pname, int param) {}

    // --- Misc (no-op) ---
    public static void glShadeModel(int mode) {}
    public static void glLineWidth(float width) {}
    public static void glClearColor(float r, float g, float b, float a) {}
    public static void glClear(int mask) {}
    public static void glViewport(int x, int y, int w, int h) {}
    public static void glFlush() {}

    // --- Vertex immediate mode (no-op — Tessellator handles this) ---
    public static void glBegin(int mode) {}
    public static void glEnd() {}
    public static void glVertex3f(float x, float y, float z) {}
    public static void glVertex3d(double x, double y, double z) {}
    public static void glTexCoord2f(float u, float v) {}

    // ================================================================
    // Internal matrix utilities
    // ================================================================

    private static void loadIdentity(float[] m) {
        java.util.Arrays.fill(m, 0);
        m[0] = m[5] = m[10] = m[15] = 1;
    }

    /** result = a * b, stored in a */
    private static void multiplyMatrix(float[] a, float[] b) {
        float[] tmp = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                tmp[col * 4 + row] =
                        a[0*4+row]*b[col*4+0] + a[1*4+row]*b[col*4+1] +
                        a[2*4+row]*b[col*4+2] + a[3*4+row]*b[col*4+3];
            }
        }
        System.arraycopy(tmp, 0, a, 0, 16);
    }
}
