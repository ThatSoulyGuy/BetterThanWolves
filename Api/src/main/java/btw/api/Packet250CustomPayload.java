package btw.api;

public class Packet250CustomPayload extends Packet {
    public String channel;
    public int length;
    public byte[] data;

    public Packet250CustomPayload() {}

    public Packet250CustomPayload(String channel, byte[] data) {
        this.channel = channel;
        this.data = data;
        if (data != null) this.length = data.length;
    }

    public int getPacketSize() { return 0; }
}
