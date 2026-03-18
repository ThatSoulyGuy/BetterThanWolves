package btw.api;

public class OpenGlHelper {

    public static int defaultTexUnit;
    public static int lightmapTexUnit;
    public static float lastBrightnessX;
    public static float lastBrightnessY;

    public static void initializeTextures() {
        defaultTexUnit = 33984;
        lightmapTexUnit = 33985;
    }

    public static void setActiveTexture(int texture) {}

    public static void setClientActiveTexture(int texture) {}

    public static void setLightmapTextureCoords(int target, float s, float t) {
        lastBrightnessX = s;
        lastBrightnessY = t;
    }

    public static void glBlendFunc(int sFactorRGB, int dFactorRGB, int sFactorAlpha, int dFactorAlpha) {}
}
