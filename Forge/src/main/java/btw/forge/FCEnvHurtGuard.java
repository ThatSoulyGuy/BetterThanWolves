package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Neutralises FC's spurious <em>suffocation</em> on puppet mobs — and only suffocation.
 *
 * <p>FC applies suffocation inside {@code fcEntity.onUpdate()}
 * ({@code EntityLiving.onEntityUpdate} -> {@code isEntityInsideOpaqueBlock()} ->
 * {@code attackEntityFrom(inWall, 1)}, vanilla/client EntityLiving.java:606-609). Its
 * {@code isEntityInsideOpaqueBlock()} check uses FC collision that doesn't match 1.20.1 terrain, so
 * on a position-driven puppet it false-fires: {@code attackEntityFrom} both drains
 * {@code fcEntity.health} and raises the hurt event ({@code worldObj.setEntityState(this, (byte) 2)}),
 * which {@link WorldBridge#setEntityState} broadcasts as a red flash. Mobs end up constantly
 * flashing red and slowly dying with nothing hitting them.</p>
 *
 * <p>The three puppet classes run their FC tick through {@link #runOnUpdate}, the puppet's 1.5.2
 * world update step: the creative/spectator target rule first ({@link FcTargetRule}), then
 * {@code updateRidden()} for a rider or {@code onUpdate()} ({@link FcRiding}). While that update
 * executes the entity is marked on this thread; during that window the hurt flash is
 * suppressed, and afterwards the damage FC applied to itself <b>with {@code DamageSource.inWall}</b>
 * is undone. The source is read from FC's own record of the hit: 1.5.2
 * {@code EntityLiving.damageEntity} (vanilla/client EntityLiving.java:1277-1286) logs every
 * health-changing hit, with its source and post-armour amount, in the entity's
 * {@code CombatTracker} (CombatTracker.java:58-67). Nothing re-probes MC geometry, whose
 * {@code isSuffocating} diverges from FC's {@code isBlockNormalCube} (85fdd2d). FC's other
 * self-damage is left untouched:</p>
 * <ul>
 *   <li><b>Fall / cactus / lava / fire / drowning</b> still hurt and kill. Reverting them would
 *       nullify fall damage (MC never applies it here — {@code travel()} is a no-op) and, on a
 *       lethal hit, {@code onDeath} fires inside {@code onUpdate} (dropping loot + broadcasting the
 *       death event) before we could revert, so reverting would dupe loot and revive the corpse.</li>
 *   <li>We never revert when the entity ends the update dead ({@code health <= 0}), so a mob that
 *       dies stays dead.</li>
 *   <li>Hits the mob lands on OTHER entities during its AI tick land in the victim's tracker, and
 *       external combat via MC {@code hurt()} runs outside the window; neither is touched.</li>
 * </ul>
 * <p>The revert also runs when {@code onUpdate()} throws. The hurt sound a spurious hit plays is
 * held back during the update ({@link #deferSelfSound}) and dropped with the hit.</p>
 */
public final class FCEnvHurtGuard {

    private static final Logger LOGGER = LogManager.getLogger("BTW-EnvHurtGuard");

    private FCEnvHurtGuard() {}

    /** The entity whose onUpdate() is currently running on this thread (or null). */
    private static final ThreadLocal<btw.modern.Entity> UPDATING = new ThreadLocal<>();

    /** A hurt sound the updating entity played on itself, held until the update's hits are known. */
    private record PendingSound(String name, float volume, float pitch) {}

    /** Self-hurt sounds of the entity in {@link #UPDATING} (see {@link #deferSelfSound}). */
    private static final ThreadLocal<List<PendingSound>> DEFERRED_SOUNDS =
            ThreadLocal.withInitial(ArrayList::new);

    private static volatile boolean logReadFailureLogged, revertFailureLogged, soundFailureLogged;

    // Frozen 1.5.2 combat log: EntityLiving.field_94063_bt (CombatTracker) -> field_94556_a
    // (List<CombatEntry>); CombatEntry.field_94569_a is the source and field_94568_c the amount
    // (CombatEntry.java:26-31). Modern-Common declares none of these, so they are resolved
    // reflectively on the classes that actually run.
    private static volatile boolean trackerResolved;
    private static Field trackerField, entriesField, entrySourceField, entryAmountField;

    /** FC entity whose onUpdate() is running on this thread, or null (WorldBridge loot-query guard). */
    public static btw.modern.Entity currentUpdating() { return UPDATING.get(); }

    /**
     * Runs the puppet's FC update (the target rule, then {@code updateRidden()} or {@code onUpdate()})
     * with FC's spurious suffocation neutralised. {@code mcProxy} is
     * the Forge entity puppeting {@code fc} (its level for the riding rule, and the fallback probe
     * when FC's combat log can't be read).
     */
    public static void runOnUpdate(btw.modern.EntityLiving fc, net.minecraft.world.entity.Entity mcProxy) {
        // FCMOD-INTEGRATION(1.20.1): creative_targets: before the update reads a target, drop the players 1.20.1 refuses as enemies
        FcTargetRule.sweep(fc);
        int hpBefore = fc.health;
        int hurtTimeBefore = fc.hurtTime;
        int resistBefore = fc.hurtResistantTime;
        List<?> log = combatLog(fc);
        int sizeBefore = log != null ? log.size() : 0;
        Object lastBefore = sizeBefore > 0 ? log.get(sizeBefore - 1) : null;
        List<PendingSound> sounds = DEFERRED_SOUNDS.get();
        sounds.clear();
        UPDATING.set(fc);
        try {
            // CF2-MX3, STUB (bl): 1.5.2's per-entity world update, a rider runs updateRidden so its mount carries it
            FcRiding.update(fc, mcProxy);
        } finally {
            UPDATING.remove();
            // Also when onUpdate threw: the inWall hit lands early in onEntityUpdate (:606-609),
            // before the AI and movement that bridge gaps make throw, and the proxies mirror
            // whatever HP loss is left onto MC health afterwards (FCPuppetSupport.HealthLink).
            // Nothing here may mask the original exception.
            boolean onlySpuriousHits = false;
            try {
                onlySpuriousHits = revertSpuriousInWall(fc, mcProxy, log, lastBefore,
                        hpBefore, hurtTimeBefore, resistBefore);
            } catch (Throwable t) {
                if (!revertFailureLogged) {
                    revertFailureLogged = true;
                    LOGGER.warn("Suffocation revert for {} failed: {}", fc.getClass().getSimpleName(), t.toString());
                }
            }
            flushDeferredSounds(fc, sounds, onlySpuriousHits);
        }
    }

    /**
     * Undoes the HP, hurtTime and hurt resistance FC's spurious inWall hits cost during this
     * update. Returns true when those reverted hits were the update's only self-inflicted hits,
     * so the hurt sounds deferred during it belong to them alone.
     */
    private static boolean revertSpuriousInWall(btw.modern.EntityLiving fc, net.minecraft.world.entity.Entity mcProxy,
                                                List<?> log, Object lastBefore,
                                                int hpBefore, int hurtTimeBefore, int resistBefore) {
        if (log == null) {
            return revertByProbe(fc, mcProxy, hpBefore);
        }

        // Hits FC applied to itself during this update. func_94549_h may have cleared the list
        // before appending (CombatTracker.java:226-236), so find the old tail by identity.
        int start = 0;
        if (lastBefore != null) {
            for (int i = log.size() - 1; i >= 0; i--) {
                if (log.get(i) == lastBefore) {
                    start = i + 1;
                    break;
                }
            }
        }
        boolean sawInWall = false, sawOther = false;
        int inWallDamage = 0;
        try {
            for (int i = start; i < log.size(); i++) {
                Object entry = log.get(i);
                if (entrySourceField.get(entry) == btw.modern.DamageSource.inWall) {
                    sawInWall = true;
                    inWallDamage += entryAmountField.getInt(entry);
                } else {
                    sawOther = true;
                }
            }
        } catch (Throwable t) {
            if (!logReadFailureLogged) {
                logReadFailureLogged = true;
                LOGGER.warn("Could not read FC combat log for {}: {}", fc.getClass().getSimpleName(), t.toString());
            }
            return false;
        }
        if (!sawInWall || fc.health <= 0) return false;

        fc.health += inWallDamage;
        if (!sawOther) {
            // The spurious hit also set hurtTime (synced to clients, FC RenderLiving's dark-red
            // pass) and the 20-tick hurt-resistance window that would swallow real hits. Put both
            // back where this update's own countdown (EntityLiving.java:656-664) would have left them.
            fc.hurtTime = Math.max(0, hurtTimeBefore - 1);
            fc.hurtResistantTime = Math.max(0, resistBefore - 1);
        }
        return !sawOther;
    }

    /**
     * Holds back a hurt sound an entity plays on itself inside its own {@code onUpdate()}.
     * 1.5.2 {@code attackEntityFrom} plays {@code getHurtSound()} for every accepted hit
     * (vanilla/client EntityLiving.java:1161-1163), including FC's spurious inWall hit, which
     * {@link #runOnUpdate} reverts only after the update. Returns true when the sound was taken;
     * {@link #runOnUpdate} replays it once the update ends, unless the update's only
     * self-inflicted hits were reverted inWall hits. Meant to be called first thing by
     * {@link WorldBridge#playSoundAtEntity}; every other sound, entity and moment passes straight
     * through.
     */
    public static boolean deferSelfSound(btw.modern.Entity entity, String sound, float volume, float pitch) {
        if (entity == null || sound == null || entity != UPDATING.get()) return false;
        if (!(entity instanceof btw.modern.EntityLiving living)) return false;
        try {
            if (!sound.equals(living.getHurtSound())) return false;
        } catch (Throwable t) {
            return false;
        }
        DEFERRED_SOUNDS.get().add(new PendingSound(sound, volume, pitch));
        return true;
    }

    /** Replays (or, for reverted spurious hits, drops) the hurt sounds deferred during the update. */
    private static void flushDeferredSounds(btw.modern.EntityLiving fc, List<PendingSound> sounds, boolean drop) {
        if (sounds.isEmpty()) return;
        try {
            if (!drop && fc.worldObj != null) {
                for (PendingSound s : sounds) {
                    fc.worldObj.playSoundAtEntity(fc, s.name(), s.volume(), s.pitch());
                }
            }
        } catch (Throwable t) {
            if (!soundFailureLogged) {
                soundFailureLogged = true;
                LOGGER.warn("Replaying hurt sound for {} failed: {}", fc.getClass().getSimpleName(), t.toString());
            }
        } finally {
            sounds.clear();
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
     * this only ever swallows a mob's own self-inflicted flash. The proxies re-broadcast the flash
     * for real (non-suffocation) self-damage once it has been mirrored onto MC health
     * ({@link FCPuppetSupport.HealthLink}).
     */
    public static boolean isSelfHurt(btw.modern.Entity entity) {
        return entity != null && entity == UPDATING.get();
    }

    /** FC's combat-log list for {@code fc}, or null if the frozen tracker can't be reached. */
    private static List<?> combatLog(btw.modern.EntityLiving fc) {
        if (!trackerResolved) resolveTracker();
        if (trackerField == null) return null;
        try {
            Object tracker = trackerField.get(fc);
            return tracker != null ? (List<?>) entriesField.get(tracker) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static synchronized void resolveTracker() {
        if (trackerResolved) return;
        try {
            Field tracker = btw.modern.EntityLiving.class.getField("field_94063_bt");
            Class<?> trackerClass = tracker.getType();
            Field entries = trackerClass.getDeclaredField("field_94556_a");
            entries.setAccessible(true);
            Class<?> entryClass = trackerClass.getClassLoader().loadClass(
                    trackerClass.getPackageName() + ".CombatEntry");
            Field source = entryClass.getDeclaredField("field_94569_a");
            Field amount = entryClass.getDeclaredField("field_94568_c");
            source.setAccessible(true);
            amount.setAccessible(true);
            trackerField = tracker;
            entriesField = entries;
            entrySourceField = source;
            entryAmountField = amount;
        } catch (Throwable t) {
            trackerField = null;
            LOGGER.warn("FC CombatTracker unavailable ({}); falling back to the MC suffocation probe", t.toString());
        }
        trackerResolved = true;
    }

    /**
     * Fallback only (FC combat log unreadable): the pre-R5 heuristic that treats a self-inflicted,
     * non-lethal HP loss as suffocation when MC geometry says the eye is in a suffocating block.
     * Returns true when it reverted such a loss.
     */
    private static boolean revertByProbe(btw.modern.EntityLiving fc, net.minecraft.world.entity.Entity mcProxy, int hpBefore) {
        if (eyeInSuffocatingBlock(fc, mcProxy)) {
            fc.hurtTime = 0;
            if (fc.health > 0 && fc.health < hpBefore) {
                fc.health = hpBefore;
                return true;
            }
        }
        return false;
    }

    /**
     * MC-side approximation of FC's {@code isEntityInsideOpaqueBlock}: probes the same 8 corner
     * points FC does (a box of side {@code width*0.8} and height 0.1 centred on the eye).
     */
    private static boolean eyeInSuffocatingBlock(btw.modern.Entity fc, net.minecraft.world.entity.Entity mcProxy) {
        if (mcProxy == null) return false;
        Level level = mcProxy.level();
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
