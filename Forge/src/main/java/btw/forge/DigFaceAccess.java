package btw.forge;

import net.minecraft.core.BlockPos;

/**
 * The face a player is digging a block from, as the server received it in the dig packet: a mixin duck interface that
 * {@code btw.forge.mixin.ServerPlayerGameModeMixin} implements on every {@code ServerPlayerGameMode} (read it through
 * {@link DigFace#of}, never directly). 1.5.2 kept the same value in {@code ItemInWorldManager.m_iHarvestingFromFacing}
 * (vanilla/server ItemInWorldManager.java:44, :236-240).
 */
public interface DigFaceAccess {

    /**
     * The 1.5.2 facing (0 down, 1 up, 2 north, 3 south, 4 west, 5 east; {@code Direction.get3DDataValue}) of the dig
     * that is breaking {@code pos} right now: inside {@code handleBlockBreakAction} the START or STOP packet's face, and
     * inside {@code tick}'s delayed destroy the face of the STOP packet that scheduled it. -1 for any other break of
     * any other cell (a direct {@code destroyBlock} caller).
     */
    int btw$digFace(BlockPos pos);
}
