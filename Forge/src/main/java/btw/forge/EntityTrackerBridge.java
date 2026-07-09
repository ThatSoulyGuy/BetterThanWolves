package btw.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bridges {@link btw.modern.EntityTracker} packet sends into the Forge
 * SimpleChannel ({@link BTWNetwork#CHANNEL}).
 *
 * <p>1.5.2 flow: FCEntityCow.TransmitKickAttackToClients (Server
 * FCEntityCow.java:511) and FCEntitySquid.TransmitTentacleAttackToClients
 * (Common FCEntitySquid.java:1049) build a Packet250CustomPayload on the
 * "FC|EV" channel and send it via
 * WorldServer.getEntityTracker().sendPacketToTrackedPlayers; the client
 * dispatcher in FCBetterThanWolves.HandleCustomPacket (Client
 * FCBetterThanWolves.java:2685-2744) decodes it and calls
 * OnClientNotifiedOfKickAttack / OnClientNotifiedOfTentacleAttack /
 * EntityCreature.setTarget.</p>
 *
 * <p>Here the raw FC|EV payload is re-wrapped in a BTWNetwork message sent
 * to players tracking the proxy entity (same TRACKING_ENTITY mechanism as
 * {@link BTWNetwork#broadcastFCEntityState}), and the client handler ports
 * the vanilla dispatcher with entity lookup adapted to the proxy layer.</p>
 */
public class EntityTrackerBridge {

    private static final Logger LOGGER = LogManager.getLogger("BTW-EntityTrackerBridge");

    /** FCBetterThanWolves.fcCustomPacketChannelCustomEntityEvent (Server FCBetterThanWolves.java:666). */
    static final String CHANNEL_CUSTOM_ENTITY_EVENT = "FC|EV";
    /** FCBetterThanWolves.fcCustomEntityEventPacketType* (Server FCBetterThanWolves.java:684-686). */
    static final int EVENT_SET_ATTACK_TARGET = 0;
    static final int EVENT_SQUID_TENTACLE_ATTACK = 1;
    static final int EVENT_COW_KICK_ATTACK = 2;

    /** Explicit SimpleChannel index, well clear of BTWNetwork's sequential ids. */
    private static final int MESSAGE_ID = 100;

    private static boolean installed = false;

    /**
     * Registers the FCCustomEntityEvent message and installs the
     * EntityTracker packet sink. Called from
     * {@link BTWEntityRegistration#registerEntities()} on both sides, after
     * BTWNetwork.register() (mod constructor) has created the channel.
     */
    public static void install() {
        if (installed) return;
        installed = true;
        BTWNetwork.CHANNEL.registerMessage(MESSAGE_ID, FCCustomEntityEvent.class,
                FCCustomEntityEvent::encode, FCCustomEntityEvent::decode, FCCustomEntityEvent::handle);
        btw.modern.EntityTracker.setPacketSink(new btw.modern.EntityTracker.PacketSink() {
            @Override
            public void sendPacketToTrackedPlayers(btw.modern.Entity entity, btw.modern.Packet packet) {
                EntityTrackerBridge.sendToTracking(entity, packet);
            }

            @Override
            public void sendPacketToAllPlayers(btw.modern.Packet packet) {
                if (packet instanceof btw.modern.Packet250CustomPayload payload
                        && CHANNEL_CUSTOM_ENTITY_EVENT.equals(payload.channel)
                        && payload.data != null) {
                    BTWNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                            new FCCustomEntityEvent(payload.data));
                }
            }
        });
        LOGGER.info("EntityTracker packet bridge installed (FC|EV custom entity events).");
    }

    private static void sendToTracking(btw.modern.Entity fcEntity, btw.modern.Packet packet) {
        if (fcEntity == null || !(packet instanceof btw.modern.Packet250CustomPayload payload)) return;
        if (!CHANNEL_CUSTOM_ENTITY_EVENT.equals(payload.channel) || payload.data == null) {
            // Only the FC|EV custom-entity-event channel flows through the
            // tracker today (FCEntityCow / FCEntitySquid / EntityCreature).
            return;
        }
        if (!(fcEntity.worldObj instanceof WorldBridge wb)) return;
        // fcEntity.entityId is kept in sync with the proxy's MC entity id by
        // the proxy tick (see ProxyMob.tick / ProxyEntity.syncToFc).
        net.minecraft.world.entity.Entity proxy = wb.getServerLevel().getEntity(fcEntity.entityId);
        if (proxy == null) return;
        BTWNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> proxy),
                new FCCustomEntityEvent(payload.data));
    }

    // =================================================================
    // Packet: FCCustomEntityEvent (server -> client) — raw FC|EV payload
    // =================================================================

    public static class FCCustomEntityEvent {
        final byte[] payload;

        public FCCustomEntityEvent(byte[] payload) {
            this.payload = payload;
        }

        public static void encode(FCCustomEntityEvent msg, FriendlyByteBuf buf) {
            buf.writeByteArray(msg.payload);
        }

        public static FCCustomEntityEvent decode(FriendlyByteBuf buf) {
            return new FCCustomEntityEvent(buf.readByteArray());
        }

        public static void handle(FCCustomEntityEvent msg,
                java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctxSupplier) {
            var ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> handleOnClient(msg.payload));
            ctx.setPacketHandled(true);
        }

        /**
         * Client dispatcher — 1.5.2 FCBetterThanWolves.HandleCustomPacket
         * FC|EV branch (Client FCBetterThanWolves.java:2685-2744), with
         * entity lookup adapted to the proxy layer. The squid/cow notify
         * methods live on FC classes the Forge module does not compile
         * against, so they are invoked reflectively (same pattern as
         * BTWLifecycle's FC access).
         */
        private static void handleOnClient(byte[] payload) {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level == null) return;
                java.io.DataInputStream dataStream = new java.io.DataInputStream(
                        new java.io.ByteArrayInputStream(payload));
                int entityId = dataStream.readInt();
                net.minecraft.world.entity.Entity mcEntity = mc.level.getEntity(entityId);
                btw.modern.Entity fc = extractFc(mcEntity);
                if (fc == null) return;
                int eventType = dataStream.readByte();
                if (eventType == EVENT_SET_ATTACK_TARGET) {
                    if (fc instanceof btw.modern.EntityCreature creature) {
                        int targetId = dataStream.readInt();
                        btw.modern.Entity fcTarget = null;
                        if (targetId >= 0) {
                            net.minecraft.world.entity.Entity mcTarget = mc.level.getEntity(targetId);
                            if (mcTarget instanceof net.minecraft.world.entity.player.Player p) {
                                fcTarget = PlayerBridge.getOrCreate(p);
                            } else {
                                fcTarget = extractFc(mcTarget);
                            }
                        }
                        creature.setTarget(fcTarget);
                    }
                } else if (eventType == EVENT_SQUID_TENTACLE_ATTACK) {
                    double targetX = ((double) dataStream.readInt()) / 32D;
                    double targetY = ((double) dataStream.readInt()) / 32D;
                    double targetZ = ((double) dataStream.readInt()) / 32D;
                    var m = fc.getClass().getMethod("OnClientNotifiedOfTentacleAttack",
                            double.class, double.class, double.class);
                    m.invoke(fc, targetX, targetY, targetZ);
                } else if (eventType == EVENT_COW_KICK_ATTACK) {
                    var m = fc.getClass().getMethod("OnClientNotifiedOfKickAttack");
                    m.invoke(fc);
                }
            } catch (Throwable t) {
                Throwable root = t;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                LOGGER.warn("Failed to handle FC custom entity event: {}: {}",
                        root.getClass().getSimpleName(), root.getMessage());
            }
        }

        private static btw.modern.Entity extractFc(net.minecraft.world.entity.Entity e) {
            if (e instanceof ProxyMob pm) return pm.getFcEntity();
            if (e instanceof ProxyAnimal pa) return pa.getFcEntity();
            if (e instanceof ProxyPathfinderMob pp) return pp.getFcEntity();
            if (e instanceof ProxyEntity pe) return pe.getFcEntity();
            return null;
        }
    }
}
