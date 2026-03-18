package btw.api;

public abstract class Packet {
    public boolean isChunkDataPacket = false;
    public final long creationTimeMillis = System.currentTimeMillis();

    public static void addIdClassMapping(int id, boolean client, boolean server, Class clazz) {}
    public abstract int getPacketSize();
}
