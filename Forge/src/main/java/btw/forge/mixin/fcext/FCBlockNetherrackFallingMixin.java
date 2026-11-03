package btw.forge.mixin.fcext;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * FCMOD-INTEGRATION(1.20.1): falling_blocks (burning netherrack below y = 0)
 *
 * <p><b>Feature:</b> the 1.20.1 build range. A level spans minBuildHeight..maxBuildHeight - 1 (the overworld
 * -64..319), and BTW's loose blocks fall below y = 0 (Modern-Common Block.CheckForFall, World.getFcMinBuildHeight).</p>
 *
 * <p><b>Conflict with FC:</b> a falling netherrack with fire on top keeps burning: FCBlockNetherrackFalling.onStartFalling
 * (Server FCBlockNetherrackFalling.java:65-85) sets the falling entity's metadata to 1 when the cell above the block is
 * fire, and onFinishFalling (:87-103) lights fire above the landed block. It finds the block's row with
 * {@code (int) entity.posY} (:70). The entity starts at the block's centre, {@code j + 0.5} (Block.CheckForFall). A cast
 * truncates toward zero, so for j &lt; 0 it gives j + 1: at j = -30, (int) -29.5 = -29, and FC tests the cell two
 * above the block (-28) for fire. The flag stayed 0, no fire was relit on landing, and the client drew no smoke. In
 * 4.B every y is &gt;= 0, where the cast and floor are the same.</p>
 *
 * <p><b>Integration pattern:</b> bridge correctness (4.B parity), not a rebalance. Rule: an FC truncating cast of a y
 * coordinate means floor, the value it has everywhere in a 4.B world. extendFcPathingBuildRange (Forge/build.gradle)
 * applies the same rule to the frozen pathing classes, and World.getPathEntityToEntity floors target.posY.</p>
 *
 * <p><b>Rebalance:</b> none. For y &gt;= 0, {@code (int) Math.floor(y) == (int) y}, so nothing changes above the 4.B
 * floor; below it, FC tests the cell directly above the block, as in 4.B. The other {@code getfield posY; d2i} sites
 * in the FC classes are sound and particle positions, comparisons that come out the same below 0, or are exact for an
 * entity standing on a full block; their list is in the R1-FIX notes.</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap of
 * Forge/build/classes/java/fc: {@code public void onStartFalling(btw.modern.EntityFallingSand)}, offset 14
 * {@code getfield btw/modern/EntityFallingSand.posY:D}, offset 17 {@code d2i}, offset 18 {@code istore_3}).
 * {@code @Redirect} of that first {@code posY} read (ordinal 0) returns {@code Math.floor(posY)}, so the d2i that
 * follows yields floor(posY). The second read (offset 85) is already {@code MathHelper.floor_double} and is left
 * alone.</p>
 *
 * <p><b>Alternative considered and rejected:</b> moving the falling entity's start position (it is FC's j + 0.5,
 * and FC's fall, landing and client simulation all read it); a build-time d2i rewrite of the class
 * (extendFcPathingBuildRange (b) style), which is equivalent but is not checked by the source compile and needs the
 * extracted fc set to be rebuilt.</p>
 */
// FCMOD-INTEGRATION(1.20.1): falling_blocks: a burning netherrack below y = 0 finds the fire above it: FC's (int) posY means floor, as at every 4.B height
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.block.FCBlockNetherrackFalling", remap = false)
public abstract class FCBlockNetherrackFallingMixin {

    /** onStartFalling's {@code int j = (int) entity.posY} (Server FCBlockNetherrackFalling.java:70): the block's row. */
    @Redirect(method = "onStartFalling(Lbtw/modern/EntityFallingSand;)V",
            at = @At(value = "FIELD", target = "Lbtw/modern/EntityFallingSand;posY:D",
                    opcode = Opcodes.GETFIELD, ordinal = 0, remap = false),
            require = 1, allow = 1, remap = false)
    private double btw$blockRowY(btw.modern.EntityFallingSand entity) {
        return Math.floor(entity.posY);
    }
}
