package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializes / deserializes FC entity state so it can flow between
 * server and client.
 *
 * <p>FC entities store significant visual state in two places that
 * are not part of MC 1.20.1's native entity sync:</p>
 * <ul>
 *   <li>NBT-serialized fields (via {@code writeEntityToNBT}) — things
 *       like windmill axis alignment, rotation angle, blade colors.</li>
 *   <li>The FC {@link btw.modern.DataWatcher} — things like mechanical
 *       rotation speed.</li>
 * </ul>
 *
 * <p>This codec writes both into a {@link FriendlyByteBuf} and restores
 * them on the other side. It is intentionally tolerant of nulls so that
 * proxies without an FC entity yet (e.g. during early spawn) emit a
 * tombstone that the client can skip over.</p>
 */
public final class FCEntityStateCodec {

    private static final Logger LOGGER = LogManager.getLogger("BTW-FCEntityStateCodec");

    // DataWatcher value type tags
    private static final byte TYPE_BYTE = 0;
    private static final byte TYPE_SHORT = 1;
    private static final byte TYPE_INT = 2;
    private static final byte TYPE_FLOAT = 3;
    private static final byte TYPE_STRING = 4;

    private FCEntityStateCodec() {}

    /**
     * Writes the FC entity's NBT + DataWatcher snapshot to {@code buf}.
     * Writes an empty-state marker if {@code fc} is null.
     */
    public static void writeState(FriendlyByteBuf buf, btw.modern.Entity fc) {
        if (fc == null) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);

        // --- Animation fields (for client rendering) ---
        // FC's RenderLiving reads these directly from the FC entity.
        // Server computes them during onUpdate; client needs them bridged.
        if (fc instanceof btw.modern.EntityLiving el) {
            buf.writeBoolean(true);
            buf.writeFloat(el.renderYawOffset);
            buf.writeFloat(el.prevRenderYawOffset);
            buf.writeFloat(el.rotationYawHead);
            buf.writeFloat(el.prevRotationYawHead);
            buf.writeFloat(el.rotationPitch);
            buf.writeFloat(el.prevRotationPitch);
            buf.writeFloat(el.rotationYaw);
            buf.writeFloat(el.prevRotationYaw);
            buf.writeFloat(el.limbSwing);
            buf.writeFloat(el.limbYaw);
            buf.writeFloat(el.prevLimbYaw);
        } else {
            buf.writeBoolean(false);
        }

        // --- NBT ---
        CompoundTag tag = new CompoundTag();
        ForgeNBTCompound wrapper = new ForgeNBTCompound(tag);
        try {
            fc.writeEntityToNBT(wrapper);
        } catch (Throwable e) {
            LOGGER.debug("writeEntityToNBT threw for {}: {}", fc.getClass().getSimpleName(), e.getMessage());
        }
        buf.writeNbt(tag);

        // --- DataWatcher ---
        Map<Integer, Object> snap = (fc.dataWatcher != null) ? fc.dataWatcher.snapshot() : new HashMap<>();
        buf.writeVarInt(snap.size());
        for (Map.Entry<Integer, Object> e : snap.entrySet()) {
            buf.writeVarInt(e.getKey());
            Object v = e.getValue();
            if (v instanceof Byte b) {
                buf.writeByte(TYPE_BYTE);
                buf.writeByte(b);
            } else if (v instanceof Short s) {
                buf.writeByte(TYPE_SHORT);
                buf.writeShort(s);
            } else if (v instanceof Integer i) {
                buf.writeByte(TYPE_INT);
                buf.writeVarInt(i);
            } else if (v instanceof Float f) {
                buf.writeByte(TYPE_FLOAT);
                buf.writeFloat(f);
            } else if (v instanceof String str) {
                buf.writeByte(TYPE_STRING);
                buf.writeUtf(str);
            } else {
                // Unknown type — skip by writing byte sentinel; reader must
                // accept unknown type tags by discarding their payload. To
                // keep it simple, encode as int 0.
                buf.writeByte(TYPE_INT);
                buf.writeVarInt(0);
            }
        }
    }

    /**
     * Reads an FC entity state blob and applies it to {@code fc}.
     * Safe to call with {@code fc == null} — the payload is still drained
     * from the buffer so reader position stays correct.
     */
    public static void applyState(FriendlyByteBuf buf, btw.modern.Entity fc) {
        boolean hasState = buf.readBoolean();
        if (!hasState) return;

        // --- Animation fields ---
        boolean hasAnim = buf.readBoolean();
        if (hasAnim) {
            float renderYawOffset = buf.readFloat();
            float prevRenderYawOffset = buf.readFloat();
            float rotationYawHead = buf.readFloat();
            float prevRotationYawHead = buf.readFloat();
            float rotationPitch = buf.readFloat();
            float prevRotationPitch = buf.readFloat();
            float rotationYaw = buf.readFloat();
            float prevRotationYaw = buf.readFloat();
            float limbSwing = buf.readFloat();
            float limbYaw = buf.readFloat();
            float prevLimbYaw = buf.readFloat();
            if (fc instanceof btw.modern.EntityLiving el) {
                el.renderYawOffset = renderYawOffset;
                el.prevRenderYawOffset = prevRenderYawOffset;
                el.rotationYawHead = rotationYawHead;
                el.prevRotationYawHead = prevRotationYawHead;
                el.rotationPitch = rotationPitch;
                el.prevRotationPitch = prevRotationPitch;
                el.rotationYaw = rotationYaw;
                el.prevRotationYaw = prevRotationYaw;
                el.limbSwing = limbSwing;
                el.limbYaw = limbYaw;
                el.prevLimbYaw = prevLimbYaw;
            }
        }

        // --- NBT ---
        CompoundTag tag = buf.readNbt();
        if (tag != null && fc != null) {
            ForgeNBTCompound wrapper = new ForgeNBTCompound(tag);
            try {
                fc.readEntityFromNBT(wrapper);
            } catch (Throwable e) {
                LOGGER.debug("readEntityFromNBT threw for {}: {}", fc.getClass().getSimpleName(), e.getMessage());
            }
        }

        // --- DataWatcher ---
        int count = buf.readVarInt();
        Map<Integer, Object> snap = new HashMap<>(count * 2);
        for (int n = 0; n < count; n++) {
            int id = buf.readVarInt();
            byte type = buf.readByte();
            Object v;
            switch (type) {
                case TYPE_BYTE  -> v = buf.readByte();
                case TYPE_SHORT -> v = buf.readShort();
                case TYPE_INT   -> v = buf.readVarInt();
                case TYPE_FLOAT -> v = buf.readFloat();
                case TYPE_STRING -> v = buf.readUtf();
                default -> { v = 0; /* unknown — already written as varint 0 */ }
            }
            snap.put(id, v);
        }
        if (fc != null && fc.dataWatcher != null) {
            fc.dataWatcher.applySnapshot(snap);
        }
    }
}
