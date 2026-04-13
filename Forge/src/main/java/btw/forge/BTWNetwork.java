package btw.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * Bridges BTW's legacy packet system to Forge's SimpleChannel networking.
 *
 * <p>Handles two concerns:</p>
 * <ul>
 *   <li>Legacy BTW packets (FCPacket166StartBlockHarvest, etc.)</li>
 *   <li>Penalty level sync: sends FC penalty levels (gloom, fat, hunger,
 *       health) and the FC food level from server to client each tick,
 *       so the client-side HUD overlay can display them.</li>
 * </ul>
 *
 * <p>FC originally synced penalty levels via DataWatcher entries (IDs 22-31).
 * In MC 1.20.1, injecting custom {@code SynchedEntityData} accessors onto
 * {@code Player} is complex. Instead, we use a simple custom packet that
 * sends the five integer values to the owning client every tick.</p>
 */
public class BTWNetwork {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Network");
    private static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel CHANNEL;

    // -----------------------------------------------------------------
    // Client-side storage for penalty levels (received from server).
    // These are read by BTWHudOverlay for rendering.
    // -----------------------------------------------------------------

    /** FC gloom level (0 = none, 1 = Gloom, 2 = Dread, 3 = Terror). */
    public static int clientGloomLevel = 0;

    /** FC fat penalty level (0 = none, 1-4 = Plump/Chubby/Fat/Obese). */
    public static int clientFatPenalty = 0;

    /** FC hunger penalty level (0 = none, 1-5 = Peckish/Hungry/Famished/Starving/Dying). */
    public static int clientHungerPenalty = 0;

    /** FC health penalty level (0 = none, 1-5 = Hurt/Injured/Wounded/Crippled/Dying). */
    public static int clientHealthPenalty = 0;

    /** FC food level on the 0-60 scale (3x vanilla resolution). */
    public static int clientFoodLevel = 60;

    /** FC saturation level (0-20). */
    public static float clientSaturation = 0f;

    private static int nextMessageId = 0;

    public static void register() {
        LOGGER.info("Registering BTW network channel...");

        CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BTWForgeMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        // Penalty sync: server -> client
        CHANNEL.registerMessage(nextMessageId++, PenaltySync.class,
                PenaltySync::encode, PenaltySync::decode, PenaltySync::handle);

        // FC entity state sync: server -> client (NBT + DataWatcher snapshot)
        CHANNEL.registerMessage(nextMessageId++, FCEntityStateSync.class,
                FCEntityStateSync::encode, FCEntityStateSync::decode, FCEntityStateSync::handle);

        // StartBlockHarvest packet (FC's Packet166):
        // In legacy FC, the client sends FCPacket166StartBlockHarvest to the server
        // when beginning to mine a block. It carries the block position, hit face,
        // and the client's computed mining speed modifier (penalties applied).
        // In the Forge 1.20.1 port, mining speed modifiers are applied server-side
        // via PlayerEvent.BreakSpeed (see BTWLifecycle), so this packet is not
        // currently needed. If client-authoritative mining speed is required later,
        // register a StartBlockHarvest message here:
        // CHANNEL.registerMessage(nextMessageId++, StartBlockHarvestPacket.class,
        //         StartBlockHarvestPacket::encode, StartBlockHarvestPacket::decode,
        //         StartBlockHarvestPacket::handle);

        LOGGER.info("BTW network channel registered.");
    }

    /**
     * Sends the current penalty levels and food level to the given player's
     * client. Called once per tick from {@code ServerPlayerMixin.btw$tick()}.
     */
    public static void sendPenaltySync(ServerPlayer player, PlayerBridge pb) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PenaltySync(
                        pb.GetGloomLevel(),
                        pb.GetFatPenaltyLevel(),
                        pb.GetHungerPenaltyLevel(),
                        pb.GetHealthPenaltyLevel(),
                        pb.foodStats.getFoodLevel(),
                        pb.foodStats.getSaturationLevel()
                ));
    }

    // =================================================================
    // Packet: PenaltySync (server -> client)
    // =================================================================

    /**
     * Carries the five FC penalty/status values from server to client.
     * All values are small non-negative integers (0-5 for penalties,
     * 0-60 for food level), so {@code writeVarInt}/{@code readVarInt}
     * keeps the packet compact.
     */
    public static class PenaltySync {
        final int gloom;
        final int fat;
        final int hunger;
        final int health;
        final int food;
        final float saturation;

        public PenaltySync(int gloom, int fat, int hunger, int health, int food, float saturation) {
            this.gloom = gloom;
            this.fat = fat;
            this.hunger = hunger;
            this.health = health;
            this.food = food;
            this.saturation = saturation;
        }

        public static void encode(PenaltySync msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.gloom);
            buf.writeVarInt(msg.fat);
            buf.writeVarInt(msg.hunger);
            buf.writeVarInt(msg.health);
            buf.writeVarInt(msg.food);
            buf.writeFloat(msg.saturation);
        }

        public static PenaltySync decode(FriendlyByteBuf buf) {
            return new PenaltySync(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readFloat()
            );
        }

        public static void handle(PenaltySync msg,
                Supplier<net.minecraftforge.network.NetworkEvent.Context> ctxSupplier) {
            net.minecraftforge.network.NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                clientGloomLevel = msg.gloom;
                clientFatPenalty = msg.fat;
                clientHungerPenalty = msg.hunger;
                clientHealthPenalty = msg.health;
                clientFoodLevel = msg.food;
                clientSaturation = msg.saturation;
            });
            ctx.setPacketHandled(true);
        }
    }

    // =================================================================
    // Packet: FCEntityStateSync (server -> client)
    // =================================================================

    /**
     * Carries an FC entity's live state (NBT fields + DataWatcher snapshot)
     * to the client so proxy-wrapped FC entities render with the correct
     * dynamic state (windmill rotation speed, blade colors, etc.).
     */
    public static class FCEntityStateSync {
        final int entityId;
        final byte[] payload;

        public FCEntityStateSync(int entityId, byte[] payload) {
            this.entityId = entityId;
            this.payload = payload;
        }

        public static void encode(FCEntityStateSync msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.entityId);
            buf.writeByteArray(msg.payload);
        }

        public static FCEntityStateSync decode(FriendlyByteBuf buf) {
            int id = buf.readVarInt();
            byte[] payload = buf.readByteArray();
            return new FCEntityStateSync(id, payload);
        }

        public static void handle(FCEntityStateSync msg,
                Supplier<net.minecraftforge.network.NetworkEvent.Context> ctxSupplier) {
            net.minecraftforge.network.NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> applyOnClient(msg));
            ctx.setPacketHandled(true);
        }

        private static void applyOnClient(FCEntityStateSync msg) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;
            net.minecraft.world.entity.Entity e = mc.level.getEntity(msg.entityId);
            btw.modern.Entity fc = null;
            if (e instanceof ProxyEntity pe) fc = pe.getFcEntity();
            else if (e instanceof ProxyMob pm) fc = pm.getFcEntity();
            else if (e instanceof ProxyAnimal pa) fc = pa.getFcEntity();
            else if (e instanceof ProxyPathfinderMob pp) fc = pp.getFcEntity();
            else return;
            if (fc == null) return;
            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(msg.payload));
            FCEntityStateCodec.applyState(buf, fc);
        }
    }

    /**
     * Encodes the current FC entity state and broadcasts to all players
     * tracking the entity. No-op if the entity has no FC entity attached.
     */
    public static void broadcastFCEntityState(net.minecraft.world.entity.Entity mcEntity,
                                              btw.modern.Entity fcEntity) {
        if (fcEntity == null || !(mcEntity.level() instanceof net.minecraft.server.level.ServerLevel)) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        FCEntityStateCodec.writeState(buf, fcEntity);
        byte[] payload = new byte[buf.readableBytes()];
        buf.readBytes(payload);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mcEntity),
                new FCEntityStateSync(mcEntity.getId(), payload));
    }
}
