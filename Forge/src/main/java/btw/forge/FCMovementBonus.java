package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * BTW's "20% hard-surface speed bonus" ({@link btw.modern.Block#GetMovementModifier}, which
 * returns 1.2 for any non-soil/non-grass block and 1.0 otherwise) applied the way FlowerChild
 * applied it: to the movement <em>input</em> speed (EntityLiving.moveEntityWithHeading did
 * {@code fMoveSpeed *= GetMovementModifier}), gated on {@code onGround} and keyed on the block
 * <em>below</em> the entity.
 *
 * <p>The bridge previously routed this modifier through {@code Block.getSpeedFactor()}
 * (BlockMixin / ProxyBlock). MC 1.20.1 multiplies {@code getSpeedFactor()} into the carried
 * {@code deltaMovement} <em>every tick</em> in {@code Entity.move()} — so a factor &gt; 1.0
 * compounds into momentum. On the ground that is bounded (~+32%), but while airborne
 * (sprint-jumping over a hard block) the retention product becomes {@code 0.91 × 1.2 = 1.092
 * &gt; 1}, turning vanilla's momentum decay into geometric growth: the "practically flying"
 * bug. Applying the bonus to {@code getSpeed()} instead makes it a bounded top-speed bonus
 * that never enters carried momentum (airborne horizontal accel uses {@code getFlyingSpeed()},
 * not {@code getSpeed()}), exactly matching FC.</p>
 *
 * <p>Must be applied identically on the client ({@code LocalPlayer}) and server
 * ({@code ServerPlayer}) getSpeed hooks so client-authoritative movement prediction agrees
 * with the server and doesn't rubber-band.</p>
 */
public final class FCMovementBonus {

    private FCMovementBonus() {}

    /**
     * Returns the FC block-movement modifier (1.0 or 1.2) for the block the entity is standing
     * on, or 1.0 when airborne, off a mapped FC block, or on soil/grass. Never &lt; 1.0
     * (GetMovementModifier has no override that returns a slowdown — those come from the block's
     * own getSpeedFactor/friction, which are left untouched).
     */
    public static float getBlockBelowSpeedBonus(LivingEntity self) {
        if (self == null || !self.onGround()) {
            return 1.0F;
        }
        try {
            // Block below the feet — mirrors Entity.getBlockPosBelowThatAffectsMyMovement()'s
            // default branch (getOnPos(0.500001): floor of position.y - 0.5). That method is
            // protected, so we reconstruct the position from public accessors.
            BlockPos below = BlockPos.containing(self.getX(), self.getY() - 0.5000001D, self.getZ());
            btw.modern.Block fcBlock = ProxyRegistry.getFcBlock(self.level().getBlockState(below).getBlock());
            if (fcBlock == null) {
                return 1.0F;
            }
            float modifier = fcBlock.GetMovementModifier(null, 0, 0, 0);
            return modifier > 0.0F ? modifier : 1.0F;
        } catch (Throwable t) {
            return 1.0F;
        }
    }
}
