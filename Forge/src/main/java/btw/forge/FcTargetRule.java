package btw.forge;

import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * FCMOD-INTEGRATION(1.20.1): creative_targets
 * Maintainer ruling 2026-10-21 (triage CF2-M7): BTW mobs never take or keep, by any path, a player that 1.20.1 refuses as
 * an enemy: Player.canBeSeenAsEnemy (Player.java:853-855, LivingEntity.java:862-868: creative or spectator through
 * abilities.invulnerable, the Invulnerable tag, spectator, dead or removed). 4.B re-checked a player only when it chose a
 * target (vanilla/client EntityAITarget.java:138), so a mob kept chasing a player who switched to creative, and hits,
 * anger, a startled or fed wolf took one. Run first in FCEnvHurtGuard.runOnUpdate, once per FC mob per server tick.
 * Survival players are never touched. Never throws.
 */
public final class FcTargetRule {

    private static final Logger LOGGER = LogManager.getLogger("BTW-TargetRule");

    private FcTargetRule() {}

    /** 1.20.1's test (TargetGoal.canContinueToUse -> LivingEntity.canAttack -> canBeSeenAsEnemy); MX2's lookup and the
     *  head-crab latch (PlayerBridge.GetCanBeHeadCrabbed) use it too. */
    public static boolean isTargetable(ServerPlayer player) {
        return player.canBeSeenAsEnemy();
    }

    /** True for the FC view of a server player that may not be a target. A client bridge is never one. */
    public static boolean isProtectedPlayer(btw.modern.Entity e) {
        if (!(e instanceof PlayerBridge pb)) return false;
        ServerPlayer sp = pb.getServerPlayer();
        return sp != null && !isTargetable(sp);
    }

    /**
     * {@link #isProtectedPlayer} for a player who is alive and in the world: protected by game mode or the Invulnerable
     * tag. Only such a player loses what a target brought with it (a pigman's anger, a head crab). A dead or departed
     * player keeps 4.B's own rules: a pigman stays angry once its target dies or leaves (1.5.2 never lowers angerLevel,
     * vanilla/client EntityPigZombie.java:101, :148), and a head crab lets go of a dead mount with its burp (Common
     * FCEntitySquid.java:202-209) or of a departed one through FcRiding.
     */
    private static boolean isProtectedLivingPlayer(btw.modern.Entity e) {
        if (!(e instanceof PlayerBridge pb)) return false;
        ServerPlayer sp = pb.getServerPlayer();
        return sp != null && sp.isAlive() && !isTargetable(sp);
    }

    static void sweep(btw.modern.EntityLiving fc) {
        try {
            List<btw.modern.Entity> dropped = null;
            btw.modern.EntityLiving attack = fc.getAttackTarget();
            if (isProtectedPlayer(attack)) {
                fc.setAttackTarget(null);
                dropped = add(dropped, attack);
            }
            if (fc instanceof btw.modern.EntityCreature c && isProtectedPlayer(c.entityToAttack)) {
                btw.modern.Entity target = c.entityToAttack;
                dropped = add(dropped, target);
                c.setTarget(null);         // FCEntitySquid.setTarget sends the change to clients
                c.setPathToEntity(null);   // the old-AI path toward the player
                if (c instanceof btw.modern.EntityPigZombie pz && isProtectedLivingPlayer(target)) {
                    // becomeAngryAt set all three (vanilla/client EntityPigZombie.java:144-149): no anger, no angry grunt
                    pz.angerLevel = 0;
                    pz.randomSoundDelay = 0;
                }
            }
            if (fc instanceof btw.modern.EntityWither w && fc.worldObj != null) {
                for (int head = 1; head <= 2; head++) {
                    int id = w.getWatchedTargetId(head);
                    if (id <= 0) continue;
                    btw.modern.Entity t = fc.worldObj.getEntityByID(id);
                    if (isProtectedPlayer(t)) {
                        w.func_82211_c(head, 0);
                        dropped = add(dropped, t);
                    }
                }
            }
            Field ghast = ghastTarget();
            if (ghast != null && ghast.getDeclaringClass().isInstance(fc)
                    && ghast.get(fc) instanceof btw.modern.Entity t && isProtectedPlayer(t)) {
                ghast.set(fc, null);
                dropped = add(dropped, t);
            }
            if (isProtectedLivingPlayer(fc.ridingEntity)) {   // a squid head crab (Common FCEntitySquid.java:856-894)
                dropped = add(dropped, fc.ridingEntity);
                fc.mountEntity(null);
            }
            if (dropped != null) {
                List<btw.modern.Entity> gone = dropped;
                fc.tasks.resetExecutingTasks(task -> holdsAny(task, gone));
                fc.targetTasks.resetExecutingTasks(task -> holdsAny(task, gone));
            }
        } catch (Throwable t) {
            if (FCPuppetSupport.firstThrowSite("target-rule", fc, t)) {
                LOGGER.warn("Target rule failed for {} (logged once per throw site): {}",
                        fc.getClass().getSimpleName(), t.toString(), t);
            }
        }
    }

    private static List<btw.modern.Entity> add(List<btw.modern.Entity> l, btw.modern.Entity e) {
        if (l == null) l = new ArrayList<>(2);
        l.add(e);
        return l;
    }

    /** Non-static fields of a task class (and its supers) that can hold an entity, made accessible once per class. */
    private static final ClassValue<Field[]> ENTITY_FIELDS = new ClassValue<>() {
        @Override
        protected Field[] computeValue(Class<?> type) {
            List<Field> out = new ArrayList<>();
            for (Class<?> k = type; k != null && k != Object.class; k = k.getSuperclass()) {
                for (Field f : k.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || !btw.modern.Entity.class.isAssignableFrom(f.getType())) continue;
                    try {
                        f.setAccessible(true);
                        out.add(f);
                    } catch (RuntimeException ignored) {
                        // a field we cannot read cannot be checked; the slot clear above still holds
                    }
                }
            }
            return out.toArray(new Field[0]);
        }
    };

    private static boolean holdsAny(Object task, List<btw.modern.Entity> gone) {
        for (Field f : ENTITY_FIELDS.get(task.getClass())) {
            try {
                Object v = f.get(task);
                for (btw.modern.Entity g : gone) if (v == g) return true;
            } catch (IllegalAccessException ignored) {
            }
        }
        return false;
    }

    private static volatile boolean ghastResolved;
    private static Field ghastTarget;

    private static Field ghastTarget() {
        if (!ghastResolved) resolveGhast();
        return ghastTarget;
    }

    private static synchronized void resolveGhast() {
        if (ghastResolved) return;
        try {
            Class<?> k = Class.forName("net.minecraft.src.btw.entity.FCEntityGhast", false,
                    btw.modern.Entity.class.getClassLoader());
            Field f = k.getDeclaredField("m_entityTargeted");
            f.setAccessible(true);
            ghastTarget = f;
        } catch (Throwable t) {
            ghastTarget = null;
        }
        ghastResolved = true;
    }

    /** BridgeSelfAudit.runServerStart: the one reflective slot resolves. */
    public static void audit() {
        Field f = ghastTarget();
        if (f != null) {
            LOGGER.info("[SELF-AUDIT/targets] creative/spectator target rule: attackTarget, entityToAttack (+pig zombie anger), "
                    + "wither heads 1-2, {}.{}, head-crab mount; AI tasks still holding the player are reset",
                    f.getDeclaringClass().getSimpleName(), f.getName());
        } else {
            LOGGER.warn("[SELF-AUDIT/targets] FCEntityGhast.m_entityTargeted not found: an FC ghast keeps a creative player "
                    + "until its 20-tick re-scan");
        }
    }
}
