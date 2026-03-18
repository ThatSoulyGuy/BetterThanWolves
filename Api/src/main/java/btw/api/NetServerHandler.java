package btw.api;

public class NetServerHandler extends NetHandler {
    public INetworkManager netManager;
    public MinecraftServer mcServer;
    public boolean connectionClosed = false;
    public EntityPlayerMP playerEntity;

    public NetServerHandler() {}

    public void sendPacketToPlayer(Packet packet) {}
    public void sendPacket(Packet packet) { sendPacketToPlayer(packet); }
    public boolean isServerHandler() { return true; }
}
