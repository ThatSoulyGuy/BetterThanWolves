package btw.modern;

/**
 * No-op replacement for org.lwjgl.opengl.GL11 fixed-function calls.
 *
 * <p>FC CLIENT code calls GL11.glTranslatef, glPushMatrix, etc. for display
 * transforms during item rendering. MC 1.20.1 uses an OpenGL core profile
 * where these fixed-function calls don't exist (fatal JVM abort).
 *
 * <p>The shadow remapping rewrites {@code org.lwjgl.opengl.GL11} references
 * in FC bytecode to {@code btw.modern.GL11}, so FC code calls these no-ops
 * instead of the real LWJGL GL11 class. This is safe because:
 * <ul>
 *   <li>GL transforms (translate, rotate, scale) don't affect Tessellator
 *       vertex data — they're applied by the GPU after vertex submission</li>
 *   <li>We only capture the Tessellator vertex output, not the GL state</li>
 *   <li>Enable/disable calls are display state, not vertex data</li>
 * </ul>
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

    // --- Matrix operations (no-op) ---
    public static void glPushMatrix() {}
    public static void glPopMatrix() {}
    public static void glTranslatef(float x, float y, float z) {}
    public static void glTranslated(double x, double y, double z) {}
    public static void glRotatef(float angle, float x, float y, float z) {}
    public static void glScalef(float x, float y, float z) {}
    public static void glScaled(double x, double y, double z) {}
    public static void glLoadIdentity() {}
    public static void glMatrixMode(int mode) {}
    public static void glMultMatrixf(java.nio.FloatBuffer m) {}
    public static void glOrtho(double l, double r, double b, double t, double n, double f) {}

    // --- State enable/disable (no-op) ---
    public static void glEnable(int cap) {}
    public static void glDisable(int cap) {}

    // --- Color/material (no-op) ---
    public static void glColor3f(float r, float g, float b) {}
    public static void glColor4f(float r, float g, float b, float a) {}
    public static void glColorMaterial(int face, int mode) {}

    // --- Depth (no-op) ---
    public static void glDepthMask(boolean flag) {}

    // --- Normal (no-op) ---
    public static void glNormal3f(float x, float y, float z) {}

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
}
