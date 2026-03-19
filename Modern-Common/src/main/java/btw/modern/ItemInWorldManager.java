package btw.modern;

public class ItemInWorldManager {
    public World theWorld;
    public EntityPlayerMP thisPlayerMP;
    public EnumGameType gameType;

    public ItemInWorldManager(World world) {
        this.theWorld = world;
    }

    public void setGameType(EnumGameType type) { this.gameType = type; }
    public EnumGameType getGameType() { return this.gameType; }
    public boolean isCreative() { return false; }
    public void initBlockRemoving() {}
}
