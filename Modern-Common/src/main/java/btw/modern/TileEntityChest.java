package btw.modern;

public class TileEntityChest extends TileEntity {

    public boolean adjacentChestChecked = false;
    public TileEntityChest adjacentChestZNeg;
    public TileEntityChest adjacentChestXPos;
    public TileEntityChest adjacentChestXNeg;
    public TileEntityChest adjacentChestZPosition;
    public int numUsingPlayers;
    public float lidAngle;
    public float prevLidAngle;

    public void openChest() {}
    public void closeChest() {}
    public void checkForAdjacentChests() {}
    public int func_98041_l() { return 0; }
}
