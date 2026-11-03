package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * The face a player breaks a block from: the side FC's conversions eject their items toward
 * ({@code FCUtilsItem.EjectStackFromBlockTowardsFacing}, Common FCUtilsItem.java:118-158; FC's {@code ConvertBlock}
 * gets it as {@code iFromSide}). One rule for every break BTW handles: FC's harvest
 * ({@code ServerPlayerGameModeMixin.btw$destroyBlock}), the log rule for logs FC has no identity for
 * ({@link ModernLogRules}) and Dynamic Trees' branches, the last two through {@link FCLogRules#hitFace}.
 * <ol>
 *   <li>The dig packet's face ({@link DigFaceAccess}): 1.5.2 passed the clicked face from the dig packet to
 *       {@code tryHarvestBlock}, the START packet's for a block that breaks at once and the STOP packet's for the
 *       rest, and kept the STOP face for a break the server finishes later (vanilla/server ItemInWorldManager.java:
 *       155-188, :208-241, :120; NetServerHandler.java:449, :460).</li>
 *   <li>Without a dig packet (another mod's fake player or machine, a vein miner): the face of the block
 *       the breaker's look ray hits, the rule {@link FCLogRules#hitFace} already applied to the log rule.</li>
 *   <li>Otherwise UP, 1.5.2's own default ({@code tryHarvestBlock(i, j, k)} and {@code blockRemoving(i, j, k)} pass
 *       1, vanilla/server ItemInWorldManager.java:207, :420).</li>
 * </ol>
 * Server thread only, like every caller.
 */
// FCMOD-INTEGRATION(1.20.1): fc_dig_face: BTW's breaks eject toward the face the player dug, from the dig packet (kept for a delayed break), else the look ray, else up
public final class DigFace {

    private static final Logger LOGGER = LogManager.getLogger("BTW-DigFace");
    private static volatile boolean lookFailureLogged;

    private DigFace() {}

    /** The face {@code player} is breaking {@code pos} from; UP for a null player. Never throws. */
    public static Direction of(@Nullable ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) return Direction.UP;
        ServerPlayerGameMode gameMode = player.gameMode;
        if (gameMode instanceof DigFaceAccess access) {
            int face = access.btw$digFace(pos);
            if (face >= 0 && face < 6) return Direction.from3DDataValue(face);
        }
        return lookFace(player, pos);
    }

    /** The face of {@code pos} the player's look ray hits, else UP (the rule FCLogRules.hitFace had alone). */
    static Direction lookFace(ServerPlayer player, BlockPos pos) {
        try {
            HitResult h = player.pick(player.getBlockReach(), 1.0F, false);
            if (h instanceof BlockHitResult bh && h.getType() == HitResult.Type.BLOCK && bh.getBlockPos().equals(pos)) {
                return bh.getDirection();
            }
        } catch (RuntimeException | LinkageError t) {
            if (!lookFailureLogged) {
                lookFailureLogged = true;
                LOGGER.warn("[DIG-FACE] the look ray failed; breaks without a dig packet eject up (logged once)", t);
            }
        }
        return Direction.UP;
    }

    /**
     * Self-audit (BridgeSelfAudit.runCommon): the dig-face record is on ServerPlayerGameMode, so the mixin's
     * handleBlockBreakAction and tick injects applied. A required mixin that failed would have stopped the game at
     * load; this line is the positive evidence a proof run greps for.
     */
    public static void audit() {
        boolean wired = DigFaceAccess.class.isAssignableFrom(ServerPlayerGameMode.class);
        if (wired) {
            LOGGER.info("[SELF-AUDIT/digface] ServerPlayerGameMode carries the dig face: BTW's breaks eject toward it");
        } else {
            LOGGER.warn("[SELF-AUDIT/digface] ServerPlayerGameMode has no dig face (ServerPlayerGameModeMixin not "
                    + "applied?): BTW's breaks fall back to the look ray, then up");
        }
    }
}
