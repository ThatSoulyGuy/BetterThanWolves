package btw.api;

public abstract class NetHandler {
    public boolean isServerHandler() { return false; }
    public void HandleStartBlockHarvest(Object packet) {}
}
