package btw.forge.mixin;

import btw.forge.HardcoreRespawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * FCMOD-INTEGRATION(1.20.1): hardcore_respawn (INTEGRATIONS #054). The rules are {@link HardcoreRespawn}'s.
 *
 * <p><b>Feature:</b> 1.20.1's PlayerList.respawn (PlayerList.java:425-494), the one entry for a death (PERFORM_RESPAWN,
 * ServerGamePacketListenerImpl.java:1517) and the End exit (respawn(player, true), :1510).</p>
 *
 * <p><b>Conflict with FC:</b> 4.B decides the respawn in recreatePlayerEntity (vanilla/server
 * ServerConfigurationManager.java:1082-1219): FC's beacon bind is validated by 4.B's rules, the End exit returns to the
 * original spawn, and a respawn with no usable point is FCUtilsHardcoreSpawn's random, soft or shared respawn.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend). 1.20.1 still resolves its own points (anchors, /spawnpoint, modded
 * respawn blocks); 4.B resolves FC's bind and every respawn without a point.</p>
 *
 * <p><b>Injection strategy:</b> five non-exclusive injectors on respawn (SRG m_11236_; javap of Forge 47.3.0's mapped
 * jar, round3/_impl/RESPAWN/javap): HEAD (4.B's decision on the old point); a ModifyVariable at the STORE of the only
 * BlockPos local (slot 3, bytecode 26: null when 4.B ignores the point, so :434-436 resolve nothing and :466-468 send no
 * message); a ModifyVariable at both STOREs of the only Optional local (slot 7, bytecode 72 and 80: a validated beacon
 * position); an inject at the INVOKE of ForgeEventFactory.firePlayerRespawnEvent (bytecode 733, a Forge method, so
 * remap = false: 4.B's retain and line, before listeners see the player); RETURN (context cleanup). The two
 * ModifyVariables capture the target's arguments (Mixin 0.8.5 ModifyVariableInjector), so every hook receives the dying
 * player. The placement itself is made at ServerPlayerMixin's restoreFrom TAIL. Implements {@link HardcoreRespawn.Hooked}
 * so the server-start self-audit proves this mixin applied.</p>
 *
 * <p><b>Disables:</b> nothing of 1.20.1's; ServerPlayerMixin's former respawn getters, which made FC's hardcore home a
 * forced 1.20.1 point, are gone. <b>Known limitation:</b> a mod that moves the player in PlayerRespawnEvent still wins,
 * as it did before.</p>
 */
// FCMOD-INTEGRATION(1.20.1): hardcore_respawn: 4.B's random, soft and shared respawn and its beacon validation run at PlayerList.respawn
@Mixin(PlayerList.class)
public abstract class PlayerListRespawnMixin implements HardcoreRespawn.Hooked {

    @Inject(method = "respawn", at = @At("HEAD"))
    private void btw$hcBegin(ServerPlayer old, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
        HardcoreRespawn.begin(old, keepEverything);                     // SCM:1104-1149
    }

    @ModifyVariable(method = "respawn", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private BlockPos btw$hcPoint(BlockPos point, ServerPlayer old, boolean keepEverything) {
        return HardcoreRespawn.respawnPosition(point, old);             // null when 4.B ignores the point
    }

    @ModifyVariable(method = "respawn", at = @At("STORE"), ordinal = 0)
    private Optional<Vec3> btw$hcResolved(Optional<Vec3> resolved, ServerPlayer old, boolean keepEverything) {
        return HardcoreRespawn.respawnResult(resolved, old);            // the validated beacon position (code 0)
    }

    @Inject(method = "respawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/event/ForgeEventFactory;firePlayerRespawnEvent(Lnet/minecraft/world/entity/player/Player;Z)V",
            remap = false))
    private void btw$hcRetain(ServerPlayer old, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
        HardcoreRespawn.beforeRespawnEvent(old);                        // SCM:1187-1195, before listeners see the player
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void btw$hcEnd(ServerPlayer old, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
        HardcoreRespawn.end(old);                                       // context cleanup only
    }
}
