package btw.api;

/**
 * Tracks entities for multiplayer. Mirrors net.minecraft.src.EntityTracker.
 */
public class EntityTracker {
    public void addEntityToTracker(Entity entity) {}
    public void removeEntityFromAllTrackingPlayers(Entity entity) {}
    public void sendPacketToTrackedPlayers(Entity entity, Packet packet) {}
    public void sendPacketToAllTrackedPlayers(Packet packet) {}
    public void sendPacketToAllPlayersTrackingEntity(Entity entity, Packet packet) {}
}
