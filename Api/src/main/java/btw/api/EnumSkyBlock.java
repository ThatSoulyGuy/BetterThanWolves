package btw.api;

public enum EnumSkyBlock {
    Sky(15),
    Block(0);

    public final int defaultLightValue;

    EnumSkyBlock(int defaultLight) {
        this.defaultLightValue = defaultLight;
    }
}
