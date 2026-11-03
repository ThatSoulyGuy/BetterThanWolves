package btw.forge.mixin;

import btw.forge.VanillaEntityViews;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_redstone (INTEGRATIONS #056)
 *
 * <p><b>Feature:</b> 1.20.1's wooden button stays pressed while an arrow touches it: ButtonBlock.checkPressed
 * (ButtonBlock.java:166-181) counts the AbstractArrows in the button's shape, from entityInside and from its scheduled
 * re-check (tick, every ticksToStayPressed).</p>
 *
 * <p><b>Conflict with FC:</b> every arrow FC spawns (FC skeletons, the composite bow, broadhead and rotten arrows, FC
 * dispensers) is a BTW puppet (ProxyEntity), not an AbstractArrow, and never runs 1.20.1's movement. FC's own contact
 * (Modern-Common BlockButton, 1.5.2's func_82535_o: vanilla/client BlockButton.java:342-385) presses the wooden button
 * and schedules its re-check, which is 1.20.1's tick (TickPolicy: ButtonBlock overrides tick); that tick did not count
 * FC's arrow and released at once a button 4.B kept pressed while the arrow stayed (updateTick, :304-327).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend). 1.20.1's check counts FC's arrows as arrows: when an arrows-pressable
 * button holds no AbstractArrow but an FC arrow ({@link VanillaEntityViews#isFcArrow}), the check runs 1.20.1's own
 * pressed branch (press, neighbours, sound, game event, the re-check in ticksToStayPressed) instead of releasing.</p>
 *
 * <p><b>Rebalance:</b> none (4.B parity). The stone button is not arrow-pressable in either version.</p>
 *
 * <p><b>Injection strategy:</b> {@link Inject} at HEAD of checkPressed (SRG m_51120_), cancellable, only when the
 * button's own AbstractArrow query is empty and an FC arrow is in the same box; otherwise 1.20.1's body runs as before.
 * The shadows (javap of Forge 47.3.0's mapped jar): the fields arrowsCanPress (f_243959_) and ticksToStayPressed
 * (f_244105_), updateNeighbours (m_51124_) and playSound (m_51067_).</p>
 *
 * <p><b>Alternatives considered and rejected:</b> a modify-variable on checkPressed's local boolean (locals are untyped
 * in a reobfuscated jar); a redirect of the AbstractArrow query (its result is typed AbstractArrow and cast, so a puppet
 * cannot be added to it).</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_redstone: 1.20.1's wooden button re-check holds a button an FC arrow still touches
@Mixin(ButtonBlock.class)
public abstract class ButtonBlockMixin {

    @Shadow @Final private int ticksToStayPressed;
    @Shadow @Final private boolean arrowsCanPress;

    @Shadow
    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void playSound(@Nullable Player player, LevelAccessor level, BlockPos pos, boolean pressed);

    @Inject(method = "checkPressed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"), cancellable = true)
    private void btw$fcArrows(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (!this.arrowsCanPress || level.isClientSide) return;
        AABB box = state.getShape(level, pos).bounds().move(pos);
        if (!level.getEntitiesOfClass(AbstractArrow.class, box).isEmpty()) return;
        if (level.getEntities((Entity) null, box, VanillaEntityViews::isFcArrow).isEmpty()) return;
        if (!state.getValue(ButtonBlock.POWERED)) {
            level.setBlock(pos, state.setValue(ButtonBlock.POWERED, Boolean.TRUE), 3);
            this.updateNeighbours(state, level, pos);
            this.playSound(null, level, pos, true);
            level.gameEvent((Entity) null, GameEvent.BLOCK_ACTIVATE, pos);
        }
        level.scheduleTick(new BlockPos(pos), (Block) (Object) this, this.ticksToStayPressed);
        ci.cancel();
    }
}
