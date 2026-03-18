package btw.api;

public class Packet13PlayerLookMove extends Packet {
    public double xPosition;
    public double yPosition;
    public double zPosition;
    public double stance;
    public float yaw;
    public float pitch;
    public boolean onGround;

    public Packet13PlayerLookMove() {}
    public Packet13PlayerLookMove(double x, double stance, double y, double z, float yaw, float pitch, boolean onGround) {}

    public int getPacketSize() { return 0; }
}
