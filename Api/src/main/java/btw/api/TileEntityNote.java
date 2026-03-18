package btw.api;

public class TileEntityNote extends TileEntity {
    public byte note = 0;
    public boolean previousRedstoneState = false;

    public void changePitch() {
        this.note = (byte)((this.note + 1) % 25);
    }

    public void triggerNote(World world, int x, int y, int z) {}
}
