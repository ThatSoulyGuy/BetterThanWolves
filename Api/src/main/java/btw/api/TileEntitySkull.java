package btw.api;

public class TileEntitySkull extends TileEntity {
    private int skullType;
    private int skullRotation;
    private String extraType = "";

    public int getSkullType() { return skullType; }
    public int getSkullRotation() { return skullRotation; }
    public void setSkullType(int type, String extra) { this.skullType = type; this.extraType = extra; }
    public String getExtraType() { return extraType; }
    public void setSkullRotation(int rotation) { this.skullRotation = rotation; }
    public int GetSkullRotationServerSafe() { return skullRotation; }
}
