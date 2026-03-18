package btw.api;

public class Packet3Chat extends Packet {
    public String message;
    public boolean isServer;

    public Packet3Chat() {}
    public Packet3Chat(String message) { this.message = message; }
    public Packet3Chat(String message, boolean isServer) {
        this.message = message;
        this.isServer = isServer;
    }

    public int getPacketSize() { return 0; }
}
