package btw.modern;

public abstract class NetHandler {
    public boolean isServerHandler() { return false; }
    public void HandleStartBlockHarvest(Object packet) {}
}
