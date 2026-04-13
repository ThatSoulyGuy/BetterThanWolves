package btw.modern;

/**
 * Vanilla 1.5.2 packet for "entity X picked up entity Y" sync.
 * Type-only stub — never actually sent in our bridge.
 */
public class Packet22Collect extends Packet {
    public int collectedEntityId;
    public int collectorEntityId;

    public Packet22Collect() {}

    public Packet22Collect(int collectedEntityId, int collectorEntityId) {
        this.collectedEntityId = collectedEntityId;
        this.collectorEntityId = collectorEntityId;
    }

    public int getPacketSize() { return 8; }
}
