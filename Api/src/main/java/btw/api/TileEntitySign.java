package btw.api;

public class TileEntitySign extends TileEntity {
    public String[] signText = new String[]{"", "", "", ""};
    public int lineBeingEdited = -1;
    private boolean isEditable = true;

    public boolean isEditable() { return isEditable; }
    public void setEditable(boolean editable) { this.isEditable = editable; }
}
