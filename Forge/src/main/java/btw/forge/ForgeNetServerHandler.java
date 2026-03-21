package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bridges FC's packet system to MC 1.20.1's networking.
 * When FC code calls {@code playerNetServerHandler.sendPacketToPlayer(packet)},
 * this translates the FC packet to the corresponding MC 1.20.1 packet
 * and sends it to the real ServerPlayer.
 */
public class ForgeNetServerHandler extends btw.modern.NetServerHandler {

    private static final Logger LOGGER = LogManager.getLogger("BTW-NetHandler");
    private final Player realPlayer;

    public ForgeNetServerHandler(Player player) {
        this.realPlayer = player;
    }

    @Override
    public void sendPacketToPlayer(btw.modern.Packet packet) {
        if (!(realPlayer instanceof ServerPlayer sp)) return;

        try {
            if (packet instanceof btw.modern.Packet132TileEntityData tePacket) {
                // Tile entity data sync → MC BlockEntityDataPacket
                BlockPos pos = new BlockPos(tePacket.xPosition, tePacket.yPosition, tePacket.zPosition);
                BlockEntity be = sp.level().getBlockEntity(pos);
                if (be instanceof ProxyBlockEntity proxy) {
                    // Convert FC NBT to MC CompoundTag and send
                    btw.modern.TileEntity fcTe = proxy.getFcTileEntity();
                    if (fcTe != null && tePacket.customParam1 != null) {
                        // FC is sending custom tile entity data — apply it and sync
                        if (tePacket.customParam1 instanceof ForgeNBTCompound fnbt) {
                            fcTe.readFromNBT(fnbt);
                        } else {
                            // Plain NBTTagCompound — wrap and read
                            CompoundTag mcTag = new CompoundTag();
                            ForgeNBTCompound wrapper = new ForgeNBTCompound(mcTag);
                            // Copy from FC tag to wrapper
                            fcTe.readFromNBT(tePacket.customParam1);
                        }
                    }
                    // Trigger MC's own sync mechanism
                    proxy.syncToClients();
                }

            } else if (packet instanceof btw.modern.Packet3Chat chatPacket) {
                // Chat message → MC system chat
                sp.sendSystemMessage(Component.literal(chatPacket.message));

            } else if (packet instanceof btw.modern.Packet250CustomPayload customPacket) {
                // Custom payload — FC uses these for GUI open packets etc.
                // The channel and data are FC-specific. For GUI opens,
                // ContainerBridge.checkAndOpenContainer handles it separately.
                // Log for debugging but don't crash.
                LOGGER.debug("FC custom packet on channel '{}' ({} bytes)",
                        customPacket.channel, customPacket.length);

            } else if (packet instanceof btw.modern.Packet70GameEvent gamePacket) {
                // Game events (weather sync, etc.) — MC handles these natively
                LOGGER.debug("FC game event packet: {}", gamePacket);

            } else {
                LOGGER.debug("Unhandled FC packet type: {}", packet.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.warn("Error sending FC packet {}: {}", packet.getClass().getSimpleName(), e.getMessage());
        }
    }
}
