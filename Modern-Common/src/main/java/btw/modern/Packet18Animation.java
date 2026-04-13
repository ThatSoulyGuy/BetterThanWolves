package btw.modern;

/**
 * Vanilla 1.5.2 packet for entity animation events (swing arm, hurt, etc.).
 * Type-only stub.
 */
public class Packet18Animation extends Packet {
    public int entityId;
    public int animate;

    public Packet18Animation() {}

    public Packet18Animation(Entity entity, int animate) {
        this.entityId = entity == null ? 0 : entity.entityId;
        this.animate = animate;
    }

    public int getPacketSize() { return 5; }
}
