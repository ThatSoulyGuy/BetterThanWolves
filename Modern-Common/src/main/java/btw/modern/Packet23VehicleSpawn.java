package btw.modern;

public class Packet23VehicleSpawn extends Packet {
    public int entityId;
    public int xPosition;
    public int yPosition;
    public int zPosition;
    public int speedX;
    public int speedY;
    public int speedZ;
    public int type;
    public int throwerEntityId;

    public Packet23VehicleSpawn() {}
    public Packet23VehicleSpawn(Entity entity, int type) {
        this.entityId = entity.entityId;
        this.type = type;
    }
    public Packet23VehicleSpawn(Entity entity, int type, int thrower) {
        this(entity, type);
        this.throwerEntityId = thrower;
    }

    public int getPacketSize() { return 0; }
}
