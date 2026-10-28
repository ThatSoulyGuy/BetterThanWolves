package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Behaviour shared by the three living puppet proxies ({@link ProxyMob}, {@link ProxyAnimal},
 * {@link ProxyPathfinderMob}): kill credit, the MC/FC health link, ground contact, and the
 * client-side animation tick that stands in for 1.5.2's client {@code onUpdate}.
 *
 * <p>MC owns puppet HP. FC still changes its own {@code health} field inside {@code onUpdate}
 * (fall, drowning, fire/lava, cactus, starvation), when another FC mob hits it, and when it heals.
 * {@link HealthLink} carries those FC-side changes onto MC (and MC-side heals onto FC) so the two
 * pools agree; MC-side hits keep flowing MC -> FC through the proxies' {@code hurt()}.</p>
 */
final class FCPuppetSupport {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Puppet");

    private static final Set<String> LOGGED_FAILURES = ConcurrentHashMap.newKeySet();

    private FCPuppetSupport() {}

    static void dropProtectedPlayerTargets(btw.modern.EntityLiving fc) {
        if (isProtectedPlayer(fc.getAttackTarget())) fc.setAttackTarget(null);
        if (isProtectedPlayer(fc.getAITarget())) fc.setRevengeTarget(null);
        if (fc instanceof btw.modern.EntityCreature creature && isProtectedPlayer(creature.getEntityToAttack())) {
            creature.setTarget(null);
        }
    }

    private static boolean isProtectedPlayer(btw.modern.Entity entity) {
        return entity instanceof PlayerBridge bridge && bridge.getRealPlayer() != null
                && (bridge.getRealPlayer().getAbilities().invulnerable || bridge.getRealPlayer().isSpectator());
    }

    static boolean firstFailure(String key) {
        return LOGGED_FAILURES.add(key);
    }

    /** HAI-10: throw sites already reported ("context|owner class|exception class@top frame"); bounded. */
    private static final Set<String> LOGGED_THROW_SITES = ConcurrentHashMap.newKeySet();
    private static final int MAX_LOGGED_THROW_SITES = 512;

    /**
     * HAI-10: true the first time {@code t}'s throw site (see {@link #throwSite}) is seen for
     * {@code owner}'s class. Lets a per-tick catch log one stack trace per distinct failure instead of
     * sampling by tick count, which hid most failures and repeated the sampled one every 100 ticks.
     */
    static boolean firstThrowSite(Object owner, Throwable t) {
        return firstThrowSite("", owner, t);
    }

    /** As {@link #firstThrowSite(Object, Throwable)}, with a separate set of sites per {@code context}. */
    static boolean firstThrowSite(String context, Object owner, Throwable t) {
        if (LOGGED_THROW_SITES.size() >= MAX_LOGGED_THROW_SITES) return false;
        String ownerName = owner != null ? owner.getClass().getName() : "null";
        return LOGGED_THROW_SITES.add(context + '|' + ownerName + '|' + throwSite(t));
    }

    /**
     * ML-1: the innermost cause of {@code t}. Follows getCause(), which also steps into a
     * btw.modern.ReportedException's CrashReport: frozen 1.5.2 code wraps failures in one ("Ticking
     * mob effect instance", vanilla/server EntityLiving.java:2422; "Saving/Loading entity NBT",
     * Entity.java:1470 / :2972), and the Modern-Common copy that runs overrides getCause() to return
     * the report's cause, as vanilla/server ReportedException.java:34-37 does. The explicit
     * getCrashCause() step below predates that override; it is now redundant but harmless. Null-safe,
     * at most 16 steps, so a cause cycle ends. Never throws.
     */
    static Throwable unwrap(Throwable t) {
        Throwable root = t;
        try {
            for (int i = 0; i < 16 && root != null; i++) {
                Throwable next = root.getCause();
                if (next == null && root instanceof btw.modern.ReportedException reported) {
                    btw.modern.CrashReport report = reported.getCrashReport();
                    next = report != null ? report.getCrashCause() : null;
                }
                if (next == null || next == root || next == t) break;
                root = next;
            }
        } catch (Throwable ignored) {
            // failure path: keep what was reached
        }
        return root;
    }

    /**
     * Exception class and top stack frame of the innermost cause ({@link #unwrap}): the real throw
     * site even when frozen 1.5.2 code wrapped it in a ReportedException. "?" when the JIT elided
     * the stack trace.
     */
    static String throwSite(Throwable t) {
        return siteOf(unwrap(t));
    }

    private static String siteOf(Throwable root) {
        if (root == null) return "null";
        StackTraceElement[] frames = root.getStackTrace();
        return root.getClass().getName() + '@' + (frames.length > 0 ? frames[0].toString() : "?");
    }

    /** HAI-10: an FC {@code onUpdate()} failure, with stack trace, once per distinct throw site. Never throws. */
    static void logUpdateFailureOnce(Logger logger, btw.modern.Entity fc, Throwable t) {
        logFailureOnce(logger, "FC onUpdate()", fc, null, t);
    }

    /**
     * HAI-10: one WARN carrying the real failure's stack trace ({@link #unwrap}) per distinct throw
     * site of {@code what} for {@code owner}'s class; repeats (every tick, every hit) are dropped.
     * Never throws.
     */
    static void logFailureOnce(Logger logger, String what, Object owner, Throwable t) {
        logFailureOnce(logger, what, owner, null, t);
    }

    /** As {@link #logFailureOnce(Logger, String, Object, Throwable)}; {@code where} (e.g. a block position) is not keyed. */
    static void logFailureOnce(Logger logger, String what, Object owner, Object where, Throwable t) {
        try {
            if (firstThrowSite(what, owner, t)) warnWithStack(logger, what, owner, where, t);
        } catch (Throwable ignored) {
            // tick path: logging must never throw
        }
    }

    /**
     * ML-1: an FC entity NBT read/write failure. Each one is a mob whose data was not saved or
     * restored, so every failure gets a line naming the real error (frozen Entity.writeToNBT /
     * readFromNBT wrap it in a ReportedException, see {@link #unwrap}); the stack trace is logged
     * once per throw site. Never throws.
     */
    static void logNbtFailure(Logger logger, String what, Object owner, Throwable t) {
        try {
            if (firstThrowSite(what, owner, t)) {
                warnWithStack(logger, what, owner, null, t);
            } else {
                Throwable root = unwrap(t);
                logger.warn("{} for {} failed again: {}", what,
                        owner != null ? owner.getClass().getSimpleName() : "null", root);
            }
        } catch (Throwable ignored) {
            // save/load path: logging must never throw
        }
    }

    private static void warnWithStack(Logger logger, String what, Object owner, Object where, Throwable t) {
        Throwable root = unwrap(t);
        String site = siteOf(root);
        String ownerName = owner != null ? owner.getClass().getSimpleName() : "null";
        String at = where != null ? " at " + where : "";
        // Log the unwrapped failure, so the real throw site is the logged stack trace, not the
        // wrapper's (log4j would print it only as the last "Caused by" of a ReportedException).
        String wrapped = root != t && t != null
                ? " (wrapped in " + t.getClass().getName() + ": " + t.getMessage() + ")" : "";
        logger.warn("{} for {}{} threw {}{} (logged once per throw site)", what, ownerName, at, site, wrapped, root);
        if (site.endsWith("@?")) {
            logger.warn("    (stack trace was elided by the JIT; re-run with -XX:-OmitStackTraceInFastThrow)");
        }
    }

    // ------------------------------------------------------------------
    // Health
    // ------------------------------------------------------------------

    /**
     * MC float health -> FC int health. A living MC entity never maps to 0: (int) truncation turned
     * MC 0.5 into FC 0, which started FC's onDeathUpdate (discard after 20 ticks) without onDeath
     * or drops while MC still had the mob alive.
     */
    static int mcHpToFc(float hp) {
        return hp <= 0.0F ? 0 : Math.max(1, (int) hp);
    }

    /**
     * Mirrors FC's max health onto MC's MAX_HEALTH attribute and, when {@code seedCurrent}, sets
     * MC's current health from FC's (an FC-spawned entity arrives with FC's own health already set).
     */
    static void seedHealthFromFc(LivingEntity mc, btw.modern.EntityLiving fc, boolean seedCurrent) {
        try {
            int fcMaxHp = fc.getMaxHealth();
            if (fcMaxHp <= 0) return;
            var attr = mc.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(fcMaxHp);
            if (seedCurrent && fc.health > 0) {
                mc.setHealth(Math.min(fc.health, fcMaxHp));
            }
        } catch (Throwable t) {
            logFailureOnce(LOGGER, "Seeding MC health from FC", fc, t);
        }
    }

    /** Receives FC-originated damage that was written straight onto MC health (see {@link HealthLink}). */
    @FunctionalInterface
    interface MirroredDamage {
        void apply(float amount);
    }

    /**
     * D6: one HP pool for a puppet. Records the FC and MC health values both sides last agreed on;
     * {@link #reconcile} attributes any later difference to the side that changed and copies it over.
     * Server thread only.
     */
    static final class HealthLink {
        private int fcHp;
        private float mcHp;
        private boolean primed;

        /** Records the current values as agreed (after a sync point such as the MC->FC hurt mirror). */
        void mark(LivingEntity mc, btw.modern.EntityLiving fc) {
            fcHp = fc.health;
            mcHp = mc.getHealth();
            primed = true;
        }

        /**
         * Copies FC-originated HP changes onto MC and MC-originated ones (regeneration, healing
         * potions, commands) onto FC. Never re-forwards into FC attackEntityFrom and never revives
         * either side. An FC-side death becomes an MC death through {@code mc.die(...)}, whose proxy
         * override skips FC onDeath (FC already ran it) and suppresses vanilla loot, and which
         * broadcasts entity event 3 so clients see the death (1.5.2 EntityLivingOnDeath ->
         * setEntityState(3), vanilla/client EntityLiving.java:3510).
         */
        void reconcile(LivingEntity mc, btw.modern.EntityLiving fc, MirroredDamage onDamage) {
            if (!primed) {
                mark(mc, fc);
                return;
            }
            int fcNow = fc.health;
            float mcNow = mc.getHealth();
            if (fcNow != fcHp) {
                if (mcNow > 0.0F) {
                    if (fcNow <= 0) {
                        // FC died on its own (fall, drowning, burning, FC mob melee): its onDeath and
                        // drops already ran inside FC code.
                        mc.setHealth(0.0F);
                        mc.die(mc.damageSources().generic());
                    } else {
                        int delta = fcNow - fcHp;
                        float target = mcNow + delta;
                        if (target < 1.0F) target = 1.0F; // FC is alive, so MC stays alive
                        mc.setHealth(target);
                        if (delta < 0) {
                            onDamage.apply(-delta);
                            // 1.5.2 attackEntityFrom -> setEntityState(2) (vanilla/client
                            // EntityLiving.java:1123): clients flash. WorldBridge swallows the FC
                            // self-hurt event (FCEnvHurtGuard.isSelfHurt) and only reaches
                            // FC-spawned proxies; a duplicate event 2 is idempotent on the client.
                            mc.level().broadcastEntityEvent(mc, (byte) 2);
                        }
                        fc.health = mcHpToFc(mc.getHealth());
                    }
                }
            } else if (mcNow != mcHp && mcNow > 0.0F && fcNow > 0) {
                fc.health = mcHpToFc(mcNow);
            }
            mark(mc, fc);
        }
    }

    /**
     * D8: environmental fire damage that FC's own simulation applies to a puppet inside
     * {@code fc.onUpdate()}: burning (Entity.onEntityUpdate, vanilla/server Entity.java:467-487, onFire
     * 1 per 20 fire ticks), lava (handleLavaMovement -> setOnFireFromLava, :489-493 / :521-528) and fire
     * blocks (moveEntity -> isBoundingBoxBurning -> dealFireDamage, :890-901 / :1032-1038). The
     * {@link HealthLink} mirrors it onto MC, so MC's own copy (Entity.baseTick on_fire / lavaHurt,
     * fire-block entityInside) is a second application of the same damage -- and one that ignores FC
     * fire immunity (blaze, ghast, pig zombie, magma cube, wither skeleton), because no fc_* EntityType
     * is fireImmune(). The proxies drop these MC-side sources.
     */
    static boolean isFcSimulatedFireDamage(DamageSource source) {
        return source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE);
    }

    /**
     * Whether FC would ever despawn this mob (1.5.2 FCMOD EntityLiving.despawnEntity, vanilla/server
     * EntityLiving.java:3583-3611: never when persistent or !canDespawn() -- villagers, golems,
     * tamed/animal mobs, mobs that picked up loot). MC's Mob.checkDespawn runs on the puppet as well
     * and must not discard what FC keeps.
     */
    static boolean fcCanDespawn(btw.modern.EntityLiving fc) {
        try {
            return !fc.GetIsPersistent() && fc.canDespawn();
        } catch (Throwable t) {
            logFailureOnce(LOGGER, "FC canDespawn()", fc, t);
            return true;
        }
    }

    // ------------------------------------------------------------------
    // Kill credit (R2)
    // ------------------------------------------------------------------

    /**
     * 1.5.2 EntityLiving.attackEntityFrom credit (vanilla/client EntityLiving.java:1095-1118): a
     * player hit sets recentlyHit=100 and attackingPlayer; a tamed wolf's hit sets recentlyHit=100
     * with no player. FC onDeathUpdate (:728-748) reads them at deathTime 20 to choose normal XP
     * orbs over the unowned "dragon" orb. MC-side hits that FC never sees (the killing blow, hits
     * inside FC's hurt-resistance window) are credited here.
     */
    static void creditAttacker(btw.modern.EntityLiving fc, DamageSource source) {
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (attacker == null) return;
        try {
            if (attacker instanceof ServerPlayer sp) {
                fc.recentlyHit = 100;
                fc.attackingPlayer = PlayerBridge.getOrCreate(sp);
            } else if (attacker instanceof TamableAnimal tame) {
                // An unreplaced 1.20.1 tamed wolf/cat: same credit as a tamed 1.5.2 EntityWolf.
                if (tame.isTame()) {
                    fc.recentlyHit = 100;
                    fc.attackingPlayer = null;
                }
            } else if (ProxyMob.wrapAttacker(source) instanceof btw.modern.EntityWolf wolf && wolf.isTamed()) {
                fc.recentlyHit = 100;
                fc.attackingPlayer = null;
            }
        } catch (Throwable t) {
            logFailureOnce(LOGGER, "Kill credit", fc, t);
        }
    }

    /**
     * 1.5.2 attackEntityFrom plays the death sound right before onDeath (vanilla/client
     * EntityLiving.java:1152-1157). The MC killing blow is not forwarded to FC attackEntityFrom and
     * the proxies silence MC's generic death sound, so the proxies' die() plays FC's own here.
     */
    static void playFcDeathSound(btw.modern.EntityLiving fc) {
        try {
            String sound = fc.getDeathSound();
            if (sound != null) {
                fc.playSound(sound, fc.getSoundVolume(), fc.getSoundPitch());
            }
        } catch (Throwable t) {
            logFailureOnce(LOGGER, "FC death sound", fc, t);
        }
    }

    // ------------------------------------------------------------------
    // Ground contact (SW4)
    // ------------------------------------------------------------------

    /**
     * Ground contact for the post-onUpdate onGround patch (9e9cb23: FC's moveEntity can miss the
     * floor when an entity-collision push produces large horizontal motion). Only a block whose
     * collision shape reaches the feet counts: water, lava, grass, flowers, single snow layers and
     * other shape-less blocks are not ground (1.5.2 sets onGround only from moveEntity's vertical
     * clip, vanilla/client Entity.java:2924-2925, so a swimming mob is airborne).
     */
    static boolean hasGroundBelow(Level level, btw.modern.Entity fc) {
        double feetY = fc.boundingBox.minY;
        int bx = net.minecraft.util.Mth.floor(fc.posX);
        int by = net.minecraft.util.Mth.floor(feetY - 0.01);
        int bz = net.minecraft.util.Mth.floor(fc.posZ);
        BlockPos pos = new BlockPos(bx, by, bz);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) return false;
        return by + shape.max(Direction.Axis.Y) >= feetY - 0.01;
    }

    // ------------------------------------------------------------------
    // Client animation (R8, HAI-7)
    // ------------------------------------------------------------------

    /**
     * Client-side animation targets for one puppet: the server FC entity's body yaw, head yaw and
     * swing progress, delivered every tick they change by {@link BTWNetwork.FCAnimSync}.
     * Written by the network handler and read by the client tick, both on the client main thread.
     */
    static final class ClientAnim {
        float body, head, swing;
        boolean has;

        void accept(float body, float head, float swing) {
            this.body = body;
            this.head = head;
            this.swing = swing;
            this.has = true;
        }
    }

    /** Server-side change filter for {@link BTWNetwork#broadcastFCAnim}. */
    static final class AnimSender {
        private float body = Float.NaN, head = Float.NaN, swing = Float.NaN;

        void send(net.minecraft.world.entity.Entity mc, btw.modern.EntityLiving fc) {
            float b = fc.renderYawOffset, h = fc.rotationYawHead, s = fc.swingProgress;
            if (!Float.isFinite(b) || !Float.isFinite(h) || !Float.isFinite(s)) return;
            if (Math.abs(b - body) < 1.0E-3F && Math.abs(h - head) < 1.0E-3F && Math.abs(s - swing) < 1.0E-4F) {
                return;
            }
            body = b;
            head = h;
            swing = s;
            try {
                BTWNetwork.broadcastFCAnim(mc, b, h, s);
            } catch (Throwable t) {
                logFailureOnce(LOGGER, "FC animation sync", fc, t);
            }
        }
    }

    /**
     * Render-safe stand-in for the 1.5.2 client tick of an FC mob. Client FC mobs are never ticked
     * (re-simulating FC physics client-side fought MC's position interpolation, 5030245), so this
     * only advances the fields the FC renderers read, once per client tick, after MC has moved the
     * proxy: position from MC's interpolation, body/head yaw and swing from the server snapshot,
     * limbs computed from the interpolated motion as the 1.5.2 client did, and the hurt/death
     * timers driven by the entity events forwarded in {@link #forwardEntityEvent}.
     */
    static void clientAnimTick(LivingEntity mc, btw.modern.EntityLiving fc, ClientAnim anim) {
        try {
            if (!anim.has) {
                // Seeded from the spawn snapshot (FCEntityStateCodec, non-live apply).
                anim.accept(fc.renderYawOffset, fc.rotationYawHead, fc.swingProgress);
            }
            // World.updateEntityWithOptionalForce / Entity.onEntityUpdate prev* bookkeeping.
            fc.lastTickPosX = fc.posX;
            fc.lastTickPosY = fc.posY;
            fc.lastTickPosZ = fc.posZ;
            fc.prevPosX = fc.posX;
            fc.prevPosY = fc.posY;
            fc.prevPosZ = fc.posZ;
            fc.posX = mc.getX();
            fc.posY = mc.getY();
            fc.posZ = mc.getZ();
            fc.prevRotationYaw = fc.rotationYaw;
            fc.prevRotationPitch = fc.rotationPitch;
            fc.rotationYaw = mc.getYRot();
            fc.rotationPitch = mc.getXRot();
            // EntityLiving.onEntityUpdate:596 and :709-713.
            fc.prevSwingProgress = fc.swingProgress;
            fc.prevRenderYawOffset = fc.renderYawOffset;
            fc.prevRotationYawHead = fc.rotationYawHead;
            fc.renderYawOffset = anim.body;
            fc.rotationYawHead = anim.head;
            fc.swingProgress = anim.swing;
            // EntityLiving.onUpdate:953-991 range checks: interpolate the short way round.
            fc.prevRotationYaw = unwrapPrev(fc.prevRotationYaw, fc.rotationYaw);
            fc.prevRenderYawOffset = unwrapPrev(fc.prevRenderYawOffset, fc.renderYawOffset);
            fc.prevRotationPitch = unwrapPrev(fc.prevRotationPitch, fc.rotationPitch);
            fc.prevRotationYawHead = unwrapPrev(fc.prevRotationYawHead, fc.rotationYawHead);
            // Entity.onEntityUpdate's ++ticksExisted; RenderLiving.handleRotationFloat (ticksExisted +
            // partial tick) drives ModelSilverfish's body wave and other age-driven model motion.
            fc.ticksExisted = mc.tickCount;
            // Contact state the 1.5.2 client computed itself (moveEntity / handleWaterMovement);
            // MC's copy comes from the server's move packets.
            boolean wasOnGround = fc.onGround;
            fc.onGround = mc.onGround();
            fc.inWater = mc.isInWater();
            // EntityLiving.onEntityUpdate:656-668 timers; onDeathUpdate:722 counts the death tilt
            // (the 1.5.2 client's own setDead at 20 is moot: MC removes the proxy).
            if (fc.hurtTime > 0) --fc.hurtTime;
            if (fc.hurtResistantTime > 0) --fc.hurtResistantTime;
            if (fc.health <= 0 && fc.deathTime < 20) ++fc.deathTime;
            // EntityLiving.moveEntityWithHeading:1666-1677 limb swing from this tick's motion.
            fc.prevLimbYaw = fc.limbYaw;
            double dx = fc.posX - fc.prevPosX;
            double dz = fc.posZ - fc.prevPosZ;
            float speed = (float) Math.sqrt(dx * dx + dz * dz) * 4.0F;
            if (speed > 1.0F) speed = 1.0F;
            fc.limbYaw += (speed - fc.limbYaw) * 0.4F;
            fc.limbSwing += fc.limbYaw;
            // Client-only countdowns started by forwarded events; without them an event would
            // freeze its pose half-way. EntityAnimal.ModSpecificOnLivingUpdate:580-589 (event 10,
            // grazing: sheep eating), FCEntityWolf.java:219 / FCEntityWolfDire.java:166 (event 10,
            // howling).
            if (fc instanceof btw.modern.EntityAnimal animal && animal.m_iGrazeProgressCounter > 0) {
                animal.m_iGrazeProgressCounter--;
            }
            Field howl = HOWL_COUNTDOWN.get(fc.getClass()).orElse(null);
            if (howl != null) {
                int n = howl.getInt(fc);
                if (n > 0) howl.setInt(fc, n - 1);
            }
            // 1.5.2 EntityIronGolem.onLivingUpdate (vanilla/client EntityIronGolem.java:113-121): the
            // arm-swing and rose timers that forwarded events 4 / 11 start (handleHealthUpdate :179-188)
            // count down once per tick; ModelIronGolem / RenderIronGolem read them every frame.
            if (fc instanceof btw.modern.EntityIronGolem) {
                Field[] golemTimers = GOLEM_TIMERS.get(fc.getClass()).orElse(null);
                if (golemTimers != null) {
                    for (Field f : golemTimers) {
                        int n = f.getInt(fc);
                        if (n > 0) f.setInt(fc, n - 1);
                    }
                }
            }
            if (fc instanceof btw.modern.EntityWolf wolf) {
                tickWolf(wolf, dx * dx + dz * dz > 1.0E-6);
            }
            if (fc instanceof btw.modern.EntityCreeper creeper) {
                tickCreeperFuse(creeper);
            }
            if (fc instanceof btw.modern.EntitySlime slime) {
                tickSlime(slime, wasOnGround);
            }
            if (fc instanceof btw.modern.EntityChicken chicken) {
                tickChicken(chicken);
            }
            if (fc instanceof btw.modern.EntityGhast ghast && ghast.dataWatcher != null) {
                // 1.5.2 EntityGhast.onUpdate (vanilla/client EntityGhast.java:79-84): the firing
                // face follows the synced DataWatcher 16.
                ghast.texture = ghast.dataWatcher.getWatchableObjectByte(16) == 1
                        ? "/mob/ghast_fire.png" : "/mob/ghast.png";
            }
            if (fc instanceof btw.modern.EntityWither wither) {
                tickWither(mc, wither);
            }
        } catch (Throwable t) {
            // HAI-10 (ML-2): one stack trace per distinct throw site; a throw skips the rest of
            // this tick's per-type branches, so every site matters.
            logFailureOnce(LOGGER, "Client animation tick", fc, t);
        }
    }

    private static float unwrapPrev(float prev, float cur) {
        while (cur - prev < -180.0F) prev -= 360.0F;
        while (cur - prev >= 180.0F) prev += 360.0F;
        return prev;
    }

    /** FC wolves' public howl countdown (declared per FC class), if the class has one. */
    private static final ClassValue<java.util.Optional<Field>> HOWL_COUNTDOWN = new ClassValue<>() {
        @Override
        protected java.util.Optional<Field> computeValue(Class<?> type) {
            try {
                Field f = type.getField("m_iHowlingCountdown");
                return f.getType() == int.class ? java.util.Optional.of(f) : java.util.Optional.empty();
            } catch (Throwable t) {
                return java.util.Optional.empty();
            }
        }
    };

    /**
     * The frozen 1.5.2 EntityIronGolem's {@code attackTimer} / {@code holdRoseTick} (vanilla/client
     * EntityIronGolem.java:22-23, private there, public after build.gradle openUpEntityAccess); the
     * Modern-Common stub declares neither. Only queried for EntityIronGolem instances.
     */
    private static final ClassValue<java.util.Optional<Field[]>> GOLEM_TIMERS = new ClassValue<>() {
        @Override
        protected java.util.Optional<Field[]> computeValue(Class<?> type) {
            try {
                Field attack = type.getField("attackTimer");
                Field rose = type.getField("holdRoseTick");
                if (attack.getType() == int.class && rose.getType() == int.class) {
                    return java.util.Optional.of(new Field[] {attack, rose});
                }
                LOGGER.warn("Iron golem timers on {} are not ints; client arm swing / rose will not time out", type.getName());
            } catch (Throwable t) {
                LOGGER.warn("Iron golem timers unavailable on {}; client arm swing / rose will not time out: {}",
                        type.getName(), t.toString());
            }
            return java.util.Optional.empty();
        }
    };

    // 1.5.2 EntityWolf client animation state. Modern-Common's EntityWolf stub declares none of
    // it; the frozen class that runs has it public.
    private static volatile boolean wolfFieldsResolved;
    private static Field wolfTilt, wolfPrevTilt, wolfShaking, wolfShakeStarted, wolfShakeTime, wolfPrevShakeTime;
    private static java.lang.reflect.Method wolfBegging;

    private static void resolveWolfFields() {
        if (wolfFieldsResolved) return;
        synchronized (FCPuppetSupport.class) {
            if (wolfFieldsResolved) return;
            try {
                Class<?> c = btw.modern.EntityWolf.class;
                Field tilt = c.getField("field_70926_e");
                Field prevTilt = c.getField("field_70924_f");
                Field shaking = c.getField("isShaking");
                Field shakeStarted = c.getField("field_70928_h");
                Field shakeTime = c.getField("timeWolfIsShaking");
                Field prevShakeTime = c.getField("prevTimeWolfIsShaking");
                java.lang.reflect.Method begging = c.getMethod("func_70922_bv");
                wolfPrevTilt = prevTilt;
                wolfShaking = shaking;
                wolfShakeStarted = shakeStarted;
                wolfShakeTime = shakeTime;
                wolfPrevShakeTime = prevShakeTime;
                wolfBegging = begging;
                wolfTilt = tilt; // last: non-null means every wolf member resolved
            } catch (Throwable t) {
                LOGGER.warn("Wolf animation fields unavailable; client wolf shake/tilt disabled: {}", t.toString());
            }
            wolfFieldsResolved = true;
        }
    }

    /**
     * 1.5.2 EntityWolf.onUpdate (vanilla/client EntityWolf.java:207-264) client half: the begging
     * head tilt, and the shake that event 8 starts (handleHealthUpdate :438-445). The shake sound
     * and splash particles stay server-side / are dropped by the client dummy world.
     * {@code moving}: the proxy moved horizontally this tick.
     */
    private static void tickWolf(btw.modern.EntityWolf wolf, boolean moving) throws ReflectiveOperationException {
        resolveWolfFields();
        if (wolfTilt == null) return;
        float tilt = wolfTilt.getFloat(wolf);
        wolfPrevTilt.setFloat(wolf, tilt);
        boolean begging = Boolean.TRUE.equals(wolfBegging.invoke(wolf));
        wolfTilt.setFloat(wolf, tilt + ((begging ? 1.0F : 0.0F) - tilt) * 0.4F);
        // 1.5.2 starts the shake on the server once a wet wolf is out of the water, on the ground
        // and without a path, and tells clients with event 8 (EntityWolf.onLivingUpdate,
        // vanilla/client EntityWolf.java:191-202, which runs before the shake branch below). Event 8
        // only reaches proxies WorldBridge spawned itself, and only the shake clears isShaking, so a
        // replacer-spawned or reloaded wolf that swam stayed dark (RenderWolf's wet pass) for good.
        // The client starts it under the same condition (no path ~ standing still); a later event 8
        // is dropped while this shake runs (forwardEntityEvent).
        if (!wolf.inWater && wolf.onGround && !moving
                && wolfShaking.getBoolean(wolf) && !wolfShakeStarted.getBoolean(wolf)) {
            wolfShakeStarted.setBoolean(wolf, true);
            wolfShakeTime.setFloat(wolf, 0.0F);
            wolfPrevShakeTime.setFloat(wolf, 0.0F);
        }
        if (wolf.inWater) {
            // isWet() (the rain half is not tracked on the client).
            wolfShaking.setBoolean(wolf, true);
            wolfShakeStarted.setBoolean(wolf, false);
            wolfShakeTime.setFloat(wolf, 0.0F);
            wolfPrevShakeTime.setFloat(wolf, 0.0F);
        } else if (wolfShakeStarted.getBoolean(wolf)) {
            float time = wolfShakeTime.getFloat(wolf);
            wolfPrevShakeTime.setFloat(wolf, time);
            wolfShakeTime.setFloat(wolf, time + 0.05F);
            if (time >= 2.0F) {
                wolfShaking.setBoolean(wolf, false);
                wolfShakeStarted.setBoolean(wolf, false);
                wolfPrevShakeTime.setFloat(wolf, 0.0F);
                wolfShakeTime.setFloat(wolf, 0.0F);
            }
        }
    }

    /** True while the client wolf's shake (field_70928_h) is in progress. */
    private static boolean isWolfShakeRunning(btw.modern.EntityWolf wolf) {
        try {
            resolveWolfFields();
            return wolfTilt != null && wolfShakeStarted.getBoolean(wolf);
        } catch (Throwable t) {
            return false;
        }
    }

    // 1.5.2 EntitySlime squish and EntityChicken wing-flap state. The Modern-Common stubs declare
    // none of it; the frozen classes that run have it public (RenderSlime, RenderMagmaCube and
    // RenderChicken read it reflectively too).
    private static volatile boolean slimeFieldsResolved, chickenFieldsResolved;
    private static Field slimeSquishTarget, slimeSquish, slimePrevSquish;
    private static java.lang.reflect.Method slimeSquishDecay;
    private static Field chickenFlap, chickenPrevFlap, chickenDest, chickenPrevDest, chickenFlapSpeed;

    /**
     * 1.5.2 EntitySlime.onUpdate (vanilla/client EntitySlime.java:106-151) client half: the squish
     * when a slime lands (-0.5) or takes off (1.0), eased every tick and decayed by func_70808_l
     * (x0.6, EntityMagmaCube x0.9). The landing particles and sound stay server-side.
     */
    private static void tickSlime(btw.modern.EntitySlime slime, boolean wasOnGround) throws ReflectiveOperationException {
        if (!slimeFieldsResolved) {
            synchronized (FCPuppetSupport.class) {
                if (!slimeFieldsResolved) {
                    try {
                        Class<?> c = btw.modern.EntitySlime.class;
                        Field target = c.getField("field_70813_a");
                        Field squish = c.getField("field_70811_b");
                        Field prev = c.getField("field_70812_c");
                        java.lang.reflect.Method decay = c.getMethod("func_70808_l");
                        slimeSquish = squish;
                        slimePrevSquish = prev;
                        slimeSquishDecay = decay;
                        slimeSquishTarget = target; // last: non-null means every member resolved
                    } catch (Throwable t) {
                        LOGGER.warn("Slime squish fields unavailable; client slime squish disabled: {}", t.toString());
                    }
                    slimeFieldsResolved = true;
                }
            }
        }
        if (slimeSquishTarget == null) return;
        float target = slimeSquishTarget.getFloat(slime);
        float squish = slimeSquish.getFloat(slime);
        squish += (target - squish) * 0.5F;
        slimeSquish.setFloat(slime, squish);
        slimePrevSquish.setFloat(slime, squish);
        if (slime.onGround && !wasOnGround) {
            slimeSquishTarget.setFloat(slime, -0.5F);
        } else if (!slime.onGround && wasOnGround) {
            slimeSquishTarget.setFloat(slime, 1.0F);
        }
        slimeSquishDecay.invoke(slime);
    }

    /** 1.5.2 EntityChicken.onLivingUpdate (vanilla/client EntityChicken.java:65-95) wing flap, minus the slowed fall. */
    private static void tickChicken(btw.modern.EntityChicken chicken) throws IllegalAccessException {
        if (!chickenFieldsResolved) {
            synchronized (FCPuppetSupport.class) {
                if (!chickenFieldsResolved) {
                    try {
                        Class<?> c = btw.modern.EntityChicken.class;
                        Field prevFlap = c.getField("field_70888_h");
                        Field dest = c.getField("destPos");
                        Field prevDest = c.getField("field_70884_g");
                        Field speed = c.getField("field_70889_i");
                        Field flap = c.getField("field_70886_e");
                        chickenPrevFlap = prevFlap;
                        chickenDest = dest;
                        chickenPrevDest = prevDest;
                        chickenFlapSpeed = speed;
                        chickenFlap = flap; // last: non-null means every field resolved
                    } catch (Throwable t) {
                        LOGGER.warn("Chicken wing fields unavailable; client wing flap disabled: {}", t.toString());
                    }
                    chickenFieldsResolved = true;
                }
            }
        }
        if (chickenFlap == null) return;
        float flap = chickenFlap.getFloat(chicken);
        float dest = chickenDest.getFloat(chicken);
        chickenPrevFlap.setFloat(chicken, flap);
        chickenPrevDest.setFloat(chicken, dest);
        dest = (float) ((double) dest + (double) (chicken.onGround ? -1 : 4) * 0.3D);
        if (dest < 0.0F) dest = 0.0F;
        if (dest > 1.0F) dest = 1.0F;
        chickenDest.setFloat(chicken, dest);
        float speed = chickenFlapSpeed.getFloat(chicken);
        if (!chicken.onGround && speed < 1.0F) speed = 1.0F;
        speed = (float) ((double) speed * 0.9D);
        chickenFlapSpeed.setFloat(chicken, speed);
        chickenFlap.setFloat(chicken, flap + speed * 2.0F);
    }

    // 1.5.2 EntityWither side-head angles: field_82220_d pitch / field_82221_e yaw per side head, and
    // their previous values field_82217_f / field_82218_g. The Modern-Common stub declares none of
    // them; the frozen class that runs has them public (ModelWither reads them via func_82207_a /
    // func_82210_r).
    private static volatile boolean witherFieldsResolved;
    private static Field witherPitch, witherYaw, witherPrevPitch, witherPrevYaw;

    /**
     * 1.5.2 EntityWither.onLivingUpdate (vanilla/client EntityWither.java:171-207) head half, which the
     * 1.5.2 client ran itself: each side head eases toward the entity in its watched-target slot
     * (DataWatcher 18/19, synced by FCEntityStateCodec) or back to the body yaw. Target ids are MC ids
     * on both sides (the proxies keep fc.entityId == getId(), PlayerBridge too), so the client looks
     * them up in the MC level. The smoke / mobSpell particles stay server-side.
     */
    private static void tickWither(LivingEntity mc, btw.modern.EntityWither wither) throws IllegalAccessException {
        if (!witherFieldsResolved) {
            synchronized (FCPuppetSupport.class) {
                if (!witherFieldsResolved) {
                    try {
                        Class<?> c = btw.modern.EntityWither.class;
                        Field yaw = c.getField("field_82221_e");
                        Field prevPitch = c.getField("field_82217_f");
                        Field prevYaw = c.getField("field_82218_g");
                        Field pitch = c.getField("field_82220_d");
                        witherYaw = yaw;
                        witherPrevPitch = prevPitch;
                        witherPrevYaw = prevYaw;
                        witherPitch = pitch; // last: non-null means every field resolved
                    } catch (Throwable t) {
                        LOGGER.warn("Wither head fields unavailable; client wither side heads disabled: {}", t.toString());
                    }
                    witherFieldsResolved = true;
                }
            }
        }
        if (witherPitch == null) return;
        float[] pitch = (float[]) witherPitch.get(wither);
        float[] yaw = (float[]) witherYaw.get(wither);
        float[] prevPitch = (float[]) witherPrevPitch.get(wither);
        float[] prevYaw = (float[]) witherPrevYaw.get(wither);
        if (pitch == null || yaw == null || prevPitch == null || prevYaw == null) return;
        for (int i = 0; i < 2; ++i) {
            prevYaw[i] = yaw[i];
            prevPitch[i] = pitch[i];
        }
        for (int i = 0; i < 2; ++i) {
            int targetId = wither.getWatchedTargetId(i + 1);
            net.minecraft.world.entity.Entity target = targetId > 0 ? mc.level().getEntity(targetId) : null;
            if (target != null) {
                // func_82214_u / func_82208_v / func_82213_w (:413-444): side head i+1 sits 1.3 to the
                // side of the body and 2.2 up.
                float a = (wither.renderYawOffset + (float) (180 * i)) / 180.0F * (float) Math.PI;
                double headX = wither.posX + (double) net.minecraft.util.Mth.cos(a) * 1.3D;
                double headY = wither.posY + 2.2D;
                double headZ = wither.posZ + (double) net.minecraft.util.Mth.sin(a) * 1.3D;
                double dx = target.getX() - headX;
                double dy = target.getEyeY() - headY;
                double dz = target.getZ() - headZ;
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                float targetYaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                float targetPitch = (float) (-(Math.atan2(dy, horizontal) * 180.0D / Math.PI));
                pitch[i] = approachAngle(pitch[i], targetPitch, 40.0F);
                yaw[i] = approachAngle(yaw[i], targetYaw, 10.0F);
            } else {
                yaw[i] = approachAngle(yaw[i], wither.renderYawOffset, 10.0F);
            }
        }
    }

    /** EntityWither.func_82204_b (vanilla/client EntityWither.java:446-460): step toward an angle, capped. */
    private static float approachAngle(float current, float target, float maxStep) {
        float delta = net.minecraft.util.Mth.wrapDegrees(target - current);
        if (delta > maxStep) delta = maxStep;
        if (delta < -maxStep) delta = -maxStep;
        return current + delta;
    }

    // 1.5.2 EntityCreeper swell counters. Modern-Common's EntityCreeper stub does not declare them;
    // the frozen class that runs (shadow list: build.gradle modernCommonRuntime) has them public (openUpEntityAccess).
    private static volatile boolean creeperFieldsResolved;
    private static Field creeperLastActive, creeperIgnited, creeperFuse;

    /**
     * HAI-7 client half: the 1.5.2 client ran EntityCreeper.onUpdate (vanilla/client
     * EntityCreeper.java:130-150) and so advanced lastActiveTime/timeSinceIgnited from the synced
     * creeper state (DataWatcher 16, carried by FCEntityStateCodec); RenderCreeper's
     * getCreeperFlashIntensity (:218-221) reads them. The fuse sound and the explosion stay server-side.
     */
    private static void tickCreeperFuse(btw.modern.EntityCreeper creeper) throws IllegalAccessException {
        if (!creeperFieldsResolved) {
            synchronized (FCPuppetSupport.class) {
                if (!creeperFieldsResolved) {
                    try {
                        Class<?> c = btw.modern.EntityCreeper.class;
                        creeperLastActive = c.getField("lastActiveTime");
                        creeperIgnited = c.getField("timeSinceIgnited");
                        creeperFuse = c.getField("fuseTime");
                    } catch (Throwable t) {
                        creeperLastActive = creeperIgnited = creeperFuse = null;
                        LOGGER.warn("Creeper swell fields unavailable; client swell animation disabled: {}", t.toString());
                    }
                    creeperFieldsResolved = true;
                }
            }
        }
        if (creeperIgnited == null || !creeper.isEntityAlive()) return;
        int ignited = creeperIgnited.getInt(creeper);
        creeperLastActive.setInt(creeper, ignited);
        ignited += creeper.getCreeperState();
        if (ignited < 0) ignited = 0;
        int fuse = creeperFuse.getInt(creeper);
        if (ignited >= fuse) ignited = fuse;
        creeperIgnited.setInt(creeper, ignited);
    }

    /**
     * MC entity events the 1.5.2 client handled in handleHealthUpdate: 2 hurt, 3 death
     * (EntityLiving.java:2483-2504), 4/11 iron golem, 6/7 taming, 8 wolf shake, 10 eating (sheep,
     * animals, FC wolves), 12-14 villager, 15 witch, 16 zombie cure. MC-only events (18 love, 20,
     * 29/30 shield, 46-55, 60 death poof) are not FC's and are not forwarded.
     */
    static boolean isFcEntityEvent(byte id) {
        switch (id) {
            case 2: case 3: case 4: case 6: case 7: case 8: case 10:
            case 11: case 12: case 13: case 14: case 15: case 16:
                return true;
            default:
                return false;
        }
    }

    /**
     * Client: hands an entity event to the client FC entity's handleHealthUpdate, as 1.5.2's
     * NetClientHandler did for Packet38EntityStatus. A repeated death event (MC die() and FC
     * onDeath both broadcast 3) is dropped once the client FC entity is already dead, and a wolf's
     * shake event 8 while the client already runs that shake (tickWolf starts it itself) would
     * only restart it.
     */
    static void forwardEntityEvent(btw.modern.EntityLiving fc, byte id) {
        if (!isFcEntityEvent(id)) return;
        if (id == 3 && fc.health <= 0) return;
        if (id == 8 && fc instanceof btw.modern.EntityWolf wolf && isWolfShakeRunning(wolf)) return;
        try {
            fc.handleHealthUpdate(id);
        } catch (Throwable t) {
            logFailureOnce(LOGGER, "FC handleHealthUpdate(" + id + ")", fc, t);
        }
    }
}
