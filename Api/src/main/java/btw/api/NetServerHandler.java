package btw.api;

public class NetServerHandler extends NetHandler {
    public Object netManager;
    public Object mcServer;
    public boolean connectionClosed = false;
    public EntityPlayerMP playerEntity;

    public NetServerHandler() {}

    public void sendPacketToPlayer(Packet packet) {}
    public void sendPacket(Packet packet) { sendPacketToPlayer(packet); }
    public boolean isServerHandler() { return true; }
}
