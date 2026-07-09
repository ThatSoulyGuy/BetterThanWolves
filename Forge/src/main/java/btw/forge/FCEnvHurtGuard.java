package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Neutralises FC's spurious <em>suffocation</em> on puppet mobs — and only suffocation.
 *
 * <p>FC applies suffocation inside {@code fcEntity.onUpdate()}
 * ({@code EntityLiving.onLivingUpdate} -> {@code isEntityInsideOpaqueBlock()} ->
 * {@code attackEntityFrom(inWall, 1)}). Its {@code isEntityInsideOpaqueBlock()} check uses FC
 * collision that doesn't match 1.20.1 terrain, so on a position-driven puppet it false-fires:
 * {@code attackEntityFrom} both drains {@code fcEntity.health} and raises the hurt event
 * ({@code worldObj.setEntityState(this, (byte) 2)}), which {@link WorldBridge#setEntityState}
 * broadcasts as a red flash. Mobs end up constantly flashing red and slowly dying with nothing
 * hitting them.</p>
 *
 * <p>The three puppet classes run their FC tick through {@link #runOnUpdate}. While an entity's
 * own {@code onUpdate()} executes it is marked on this thread; during that window we suppress
 * the hurt flash and revert the health loss <b>only when the damage is suffocation</b> — i.e.
 * the same entity is hurting itself AND its eye is genuinely inside a suffocating block (checked
 * MC-side). This deliberately leaves FC's other self-damage untouched:</p>
 * <ul>
 *   <li><b>Fall / cactus / lava</b> still hurt and kill. Reverting them would nullify fall
 *       damage (MC never applies it here — {@code travel()} is a no-op) and, on a lethal fall,
 *       {@code onDeath} fires inside {@code onUpdate} (dropping loot + broadcasting the death
 *       event) before we could revert, so reverting would dupe loot and revive the corpse.</li>
 *   <li>We never revert a lethal hit ({@code health <= 0}), so a mob that dies stays dead.</li>
 *   <li>Hits the mob lands on OTHER entities during its AI tick, and external combat via MC
 *       {@code hurt()} (outside {@code onUpdate}), carry a different entity / run outside the
 *       window and are never touched.</li>
 * </ul>
 */
public final class FCEnvHurtGuard {

    private FCEnvHurtGuard() {}

    /** The entity whose onUpdate() is currently running on this thread (or null). */
    private static final ThreadLocal<btw.modern.Entity> UPDATING = new ThreadLocal<>();

    /**
     * Runs {@code fc.onUpdate()} with FC's spurious suffocation neutralised. {@code mcProxy} is
     * the Forge entity puppeting {@code fc} (used to read the MC world + eye height).
     */
    public static void runOnUpdate(btw.modern.EntityLiving fc, net.minecraft.world.entity.Entity mcProxy) {
        int hpBefore = fc.health;
        UPDATING.set(fc);
        try {
            fc.onUpdate();
        } finally {
            UPDATING.remove();
        }
        // Only undo SUFFOCATION: still alive, lost health this tick, and eye is in a solid block.
        if (eyeInSuffocatingBlock(fc, mcProxy)) {
            // Spurious suffocation sets BOTH a health drain and hurtTime. hurtTime syncs to the
            // client (FCEntityStateCodec -> writeEntityToNBT/HurtTime) and drives FC's
            // RenderLiving dark-red damage pass (glColor4f(b,0,0,0.4)); clearing it here removes
            // the stuck-on red tint. Health is only reverted when actually drained and non-lethal
            // (so fall/lava and real deaths are untouched -- see class javadoc).
            fc.hurtTime = 0;
            if (fc.health > 0 && fc.health < hpBefore) {
                fc.health = hpBefore;
            }
        }
    }

    /**
     * True if {@code entity} is hurting <b>itself</b> inside its own {@code onUpdate()}.
     * {@link WorldBridge#setEntityState} uses this to drop the phantom red flash. We deliberately
     * do NOT re-check suffocation geometry here: FC raises the hurt event mid-{@code onUpdate} at
     * the pre-movement position, where an MC-side {@code isSuffocating} re-probe diverges from
     * FC's {@code isBlockNormalCube} and misses it, letting the flash leak through and stick on.
     * A mob hurting another entity passes the VICTIM to {@code setEntityState} (so that entity
     * != the updater and still flashes), and external combat runs outside {@code onUpdate}, so
     * this only ever swallows a mob's own self-inflicted flash (suffocation, and harmlessly the
     * rare self fall/lava flash). Health is still reverted only for real suffocation, in
     * {@link #runOnUpdate}, so fall/lava damage itself is untouched.
     */
    public static boolean isSelfHurt(btw.modern.Entity entity) {
        return entity != null && entity == UPDATING.get();
    }

    /**
     * MC-side equivalent of FC's {@code isEntityInsideOpaqueBlock}: probes the same 8 corner
     * points FC does (a box of side {@code width*0.8} and height 0.1 centred on the eye) so we
     * revert exactly when FC would have suffocated — including a mob flush against a wall whose
     * eye-centre is in air but a corner is in the block.
     */
    private static boolean eyeInSuffocatingBlock(btw.modern.Entity fc, net.minecraft.world.entity.Entity mcProxy) {
        if (mcProxy == null) return false;
        Level level = mcProxy.level();
        // Use FC's OWN eye height (height*0.85) and width so this probe floors into the same
        // blocks FC's isEntityInsideOpaqueBlock did -- MC's dimensions differ enough to sample a
        // different block and miss the suffocation, letting the red flash leak back.
        double eyeY = fc.posY + fc.getEyeHeight();
        double halfW = fc.width * 0.8 * 0.5; // FC probes +/- width*0.8/2 horizontally
        for (int i = 0; i < 8; i++) {
            double dx = ((i & 1) - 0.5) * 2.0 * halfW;
            double dy = (((i >> 1) & 1) - 0.5) * 0.1;
            double dz = (((i >> 2) & 1) - 0.5) * 2.0 * halfW;
            BlockPos pos = BlockPos.containing(fc.posX + dx, eyeY + dy, fc.posZ + dz);
            BlockState state = level.getBlockState(pos);
            if (state.isSuffocating(level, pos)) return true;
        }
        return false;
    }
}
