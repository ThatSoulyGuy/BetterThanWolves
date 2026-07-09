package btw.modern;

/**
 * Tracks entities for multiplayer. Mirrors net.minecraft.src.EntityTracker.
 *
 * The actual per-player tracking sets live in the modern engine (ChunkMap /
 * TRACKING_ENTITY packet distribution), so the 1.5.2 send methods delegate to
 * a bridge sink installed by btw.forge.EntityTrackerBridge. Live callers:
 * FCEntityCow.TransmitKickAttackToClients and
 * FCEntitySquid.TransmitTentacleAttackToClients via
 * FCUtilsWorld.SendPacketToAllPlayersTrackingEntity.
 */
public class EntityTracker {

    /**
     * Bridge into the modern packet layer — installed once at startup by
     * btw.forge.EntityTrackerBridge.install().
     */
    public interface PacketSink {
        void sendPacketToTrackedPlayers(Entity entity, Packet packet);
        void sendPacketToAllPlayers(Packet packet);
    }

    private static PacketSink sink;

    public static void setPacketSink(PacketSink packetSink) {
        sink = packetSink;
    }

    // 1.5.2 EntityTracker.addEntityToTracker — tracking membership is owned
    // by the modern engine (ChunkMap tracks the proxy entities); nothing to
    // mirror here.
    public void addEntityToTracker(Entity entity) {}

    public void removeEntityFromAllTrackingPlayers(Entity entity) {}

    // 1.5.2 EntityTracker.sendPacketToTrackedPlayers — server-side name used
    // by FCUtilsWorld.SendPacketToAllPlayersTrackingEntity (cow kick / squid
    // tentacle custom entity events).
    public void sendPacketToTrackedPlayers(Entity entity, Packet packet) {
        if (sink != null) {
            sink.sendPacketToTrackedPlayers(entity, packet);
        }
    }

    // 1.5.2 EntityTracker.sendPacketToAllTrackedPlayers — broadcast to every
    // player tracking anything (FC uses it for global notifications).
    public void sendPacketToAllTrackedPlayers(Packet packet) {
        if (sink != null) {
            sink.sendPacketToAllPlayers(packet);
        }
    }

    // 1.5.2 client-mapped name of sendPacketToTrackedPlayers. (In vanilla the
    // two differ only in whether a tracked *player* entity receives its own
    // packet; FC only sends these for non-player entities, so delegating is
    // equivalent.)
    public void sendPacketToAllPlayersTrackingEntity(Entity entity, Packet packet) {
        sendPacketToTrackedPlayers(entity, packet);
    }
}
