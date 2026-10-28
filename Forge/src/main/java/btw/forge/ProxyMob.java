package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge-side proxy that extends {@link Mob} and forwards all lifecycle
 * callbacks to an FC {@link btw.modern.EntityLiving} instance.
 *
 * Used for FC entities whose legacy hierarchy roots at
 * {@link btw.modern.EntityMob} (creeper, zombie, skeleton, spider, blaze,
 * enderman, witch, wither, pig zombie, slime, etc.) as well as
 * {@link btw.modern.EntityFlying} (ghast) and other hostile mob variants.
 */
public class ProxyMob extends Mob
        implements net.minecraftforge.entity.IEntityAdditionalSpawnData {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyMob");

    private static final java.util.Set<String> LOGGED_REMOVALS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Catches every removal path so we can diagnose disappearing entities (debug level only).
     */
    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (LOGGER.isDebugEnabled()) {
            String key = (fcEntity != null ? fcEntity.getClass().getSimpleName() : "null") + "|" + reason;
            if (LOGGED_REMOVALS.add(key)) {
                LOGGER.debug("ProxyMob.remove({}) — fc={}, id={}, pos={},{},{}",
                        reason,
                        fcEntity != null ? fcEntity.getClass().getSimpleName() : "null",
                        getId(), getX(), getY(), getZ());
                StackTraceElement[] st = new Throwable().getStackTrace();
                for (int i = 1; i < Math.min(12, st.length); i++) {
                    LOGGER.debug("    at {}", st[i]);
                }
            }
        }
        super.remove(reason);
    }

    private static boolean isFiniteState(btw.modern.EntityLiving fc) {
        return Double.isFinite(fc.posX) && Double.isFinite(fc.posY) && Double.isFinite(fc.posZ)
            && Double.isFinite(fc.motionX) && Double.isFinite(fc.motionY) && Double.isFinite(fc.motionZ)
            && Float.isFinite(fc.rotationYaw) && Float.isFinite(fc.rotationPitch);
    }

    private btw.modern.EntityLiving fcEntity;
    private String fcClassName = "";
    private boolean pendingKnockback = false;
    // Last known finite position — used for NaN recovery. Updated every tick
    // that posX/Y/Z are finite. If posX becomes NaN (moveEntity corruption
    // from NaN motionX/Z), the NaN guard restores from here instead of
    // the per-tick snapshot (which would also be NaN).
    private double lastGoodPosX, lastGoodPosY, lastGoodPosZ;
    /** D6: MC/FC health agreement (server). */
    private final FCPuppetSupport.HealthLink healthLink = new FCPuppetSupport.HealthLink();
    /** R8: client animation targets fed by BTWNetwork.FCAnimSync. */
    private final FCPuppetSupport.ClientAnim clientAnim = new FCPuppetSupport.ClientAnim();
    private final FCPuppetSupport.AnimSender animSender = new FCPuppetSupport.AnimSender();
    /** RE7: set while readAdditionalSaveData runs, so ensureFcEntity keeps the loaded Health. */
    private boolean loadingSaveData = false;
    /**
     * W7: category of the vanilla mob this proxy replaced (set by VanillaMobReplacer, saved as
     * {@link #SPAWN_CATEGORY_TAG}); null for FC-spawned proxies, spawn eggs of fc_* types and saves
     * without the tag, which keep their registered category. See {@link #getClassification}.
     */
    private MobCategory replacedCategory;

    /** W7: NBT key of the replaced category (MobCategory.getName()), saved next to FCClassName. */
    static final String SPAWN_CATEGORY_TAG = "BTWSpawnCategory";

    public ProxyMob(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // ------------------------------------------------------------------
    // FC entity linkage
    // ------------------------------------------------------------------

    public void setFcEntity(btw.modern.EntityLiving fc) {
        this.fcEntity = fc;
        this.fcClassName = fc.getClass().getName();
        if (level() instanceof ServerLevel sl) {
            fc.worldObj = WorldBridge.getOrCreate(sl);
            // FC-spawned (EntityProxyFactory): FC's max and current health are authoritative,
            // otherwise MC keeps this type's default MAX_HEALTH and the pools disagree from birth.
            FCPuppetSupport.seedHealthFromFc(this, fc, true);
            healthLink.mark(this, fc);
        }
        syncToFc();
    }

    FCPuppetSupport.ClientAnim getClientAnim() {
        return clientAnim;
    }

    public btw.modern.EntityLiving getFcEntity() {
        if (fcEntity == null) {
            ensureFcEntity();
        }
        if (fcEntity != null) syncToFc();
        return fcEntity;
    }

    public String getFcClassName() {
        if (fcClassName.isEmpty()) {
            fcClassName = BTWEntityRegistration.getFcClassName(getType());
        }
        return fcClassName;
    }

    /** W7: VanillaMobReplacer passes the replaced vanilla type's category before adding the proxy. */
    public void setReplacedCategory(MobCategory category) {
        this.replacedCategory = category;
    }

    /**
     * W7: a replacement counts for natural-spawn caps in the category of the vanilla mob it replaced.
     * Forge's NaturalSpawner.createState (NaturalSpawner.java:67-90) counts every loaded mob under
     * getClassification(true); a glow squid (UNDERGROUND_WATER_CREATURE, cap 5) made into fc_squid
     * (registered WATER_CREATURE) left its own category's count at ~0, so glow squids spawned, and were
     * converted, without limit. 1.5.2 counted each cap by the creature class it spawns
     * (vanilla/server SpawnerAnimals.java:107, EnumCreatureType.java:25), so a mob always counted where
     * it spawned. Every other caller gets the registered category.
     */
    @Override
    public MobCategory getClassification(boolean forSpawnCount) {
        if (forSpawnCount && replacedCategory != null) return replacedCategory;
        return super.getClassification(forSpawnCount);
    }

    /**
     * W7: the saved replaced category, or {@code current} when the tag is absent. An unknown name (an
     * extended category whose mod is gone) reads as null, i.e. the registered category. Shared by the
     * three proxies.
     */
    static MobCategory readSpawnCategory(CompoundTag tag, MobCategory current, Logger logger) {
        if (!tag.contains(SPAWN_CATEGORY_TAG, net.minecraft.nbt.Tag.TAG_STRING)) return current;
        String name = tag.getString(SPAWN_CATEGORY_TAG);
        MobCategory category = MobCategory.byName(name);
        if (category == null && FCPuppetSupport.firstFailure("spawncategory|" + name)) {
            logger.warn("Unknown saved spawn category '{}'; the registered category is used", name);
        }
        return category;
    }

    private void ensureFcEntity() {
        String className = getFcClassName();
        if (className == null || className.isEmpty()) return;
        btw.modern.World world;
        if (level().isClientSide) {
            world = ProxyEntity.createDummyClientWorld(level());
        } else if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            world = WorldBridge.getOrCreate(sl);
        } else {
            return;
        }
        try {
            Class<?> fcClass = Class.forName(className);
            try {
                var ctor = fcClass.getConstructor(btw.modern.World.class);
                fcEntity = (btw.modern.EntityLiving) ctor.newInstance(world);
            } catch (NoSuchMethodException e) {
                var ctor = fcClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                fcEntity = (btw.modern.EntityLiving) ctor.newInstance();
            }
            if (fcEntity != null) {
                fcEntity.worldObj = world;
                // CRITICAL: seed the FC entity's position from MC's spawn
                // position. In puppet mode, FC owns position from this point
                // forward — but the very first tick must start at the MC
                // entity's spawn coords (where VanillaMobReplacer placed us
                // or where /summon put us). Without this, the FC entity
                // spawns at (0,0,0), syncFromFc teleports the MC entity to
                // the void on the first tick, and it gets discarded.
                fcEntity.setLocationAndAngles(getX(), getY(), getZ(), getYRot(), getXRot());
                lastGoodPosX = getX();
                lastGoodPosY = getY();
                lastGoodPosZ = getZ();
                fcEntity.entityId = getId();
                // Mirror FC's max health onto MC's MAX_HEALTH attribute so
                // MC's hurt() pipeline kills the entity at the right time.
                int fcMaxHp = fcEntity.getMaxHealth();
                if (fcMaxHp > 0) {
                    // RE7: only a fresh spawn takes the new FC entity's max and starts at full
                    // health. On the NBT-load path LivingEntity.readAdditionalSaveData has already
                    // restored the saved Attributes and Health, and this FC entity's max may still
                    // be a default (EntitySlime re-rolls its size in the constructor, vanilla/server
                    // EntitySlime.java:29-36); readAdditionalSaveData re-seeds MAX_HEALTH once FC's
                    // own NBT is read.
                    if (!loadingSaveData) {
                        var attr = getAttribute(Attributes.MAX_HEALTH);
                        if (attr != null) attr.setBaseValue(fcMaxHp);
                        setHealth(fcMaxHp);
                    }
                    fcEntity.health = FCPuppetSupport.mcHpToFc(getHealth());
                }
                if (!level().isClientSide) healthLink.mark(this, fcEntity);
                ProxyMobTaskDumper.dumpFcTasks(fcEntity);
            }
        } catch (Throwable e) {
            if (FCPuppetSupport.firstFailure("create|" + className)) {
                LOGGER.warn("Could not create FC entity {}: {}", className, e.getMessage(), e);
            }
        }
    }

    @Override
    public void writeSpawnData(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeUtf(fcClassName);
        FCEntityStateCodec.writeState(buf, fcEntity);
    }

    @Override
    public void readSpawnData(net.minecraft.network.FriendlyByteBuf buf) {
        fcClassName = buf.readUtf();
        if (!fcClassName.isEmpty()) ensureFcEntity();
        FCEntityStateCodec.applyState(buf, fcEntity);
    }

    // ------------------------------------------------------------------
    // Position / rotation synchronization
    // ------------------------------------------------------------------

    /**
     * Puppet-mode architecture (see git history): FC entity owns the FULL
     * simulation — AI, intent, motion, collision, gravity, position. The
     * MC proxy is a "puppet": every tick we copy FC's authoritative
     * position/rotation onto the MC entity for rendering and network
     * sync, but MC's travel() and aiStep() are no-oped so MC physics
     * cannot touch position.
     *
     * syncToFc copies only NON-position state INTO the FC entity (things
     * MC owns: damage state, fire timer, world reference). Position is
     * NEVER pushed from MC into FC — that would erase the position FC
     * computed last tick.
     */
    private void syncToFc() {
        if (fcEntity == null) return;
        fcEntity.entityId = getId();
        fcEntity.ticksExisted = tickCount;
        // S8: on the server FC's own Entity.handleWaterMovement (vanilla/server Entity.java:1079-1123) owns
        // inWater; its !inWater -> inWater edge plays the 1.5.2 'liquid.splash' (:1092), and seeding it from
        // MC's looser water check pre-empted that edge. The client FC entity does not simulate, so MC's
        // state stands in there.
        if (level().isClientSide) fcEntity.inWater = isInWater();
        // Two-way fire sync (1.5.2 Entity.onEntityUpdate is the decrementer):
        // only RAISE fire on external MC ignition. Blind assignment wiped any
        // setFire() done inside FC code (frozen EntitySkeleton/EntityZombie
        // daylight setFire(8)) one tick later, so undead never burned in
        // daylight. FC's value flows back to MC in tick() after onUpdate.
        int mcFire = getRemainingFireTicks();
        if (mcFire > fcEntity.fire) {
            fcEntity.fire = mcFire;
        }
        // Knockback transfer is handled exclusively in tick() via the
        // pendingKnockback flag. Do NOT touch deltaMovement here —
        // syncToFc() is called from getFcEntity() which external code
        // (hurt pipeline, etc.) can invoke at any time, and blindly
        // adding deltaMovement would accumulate spurious velocity.
    }

    /**
     * Copies FC's authoritative position, rotation, and animation state
     * onto the MC entity so the renderer and network tracker see what FC
     * computed. This is the ONLY direction position flows.
     */
    private void syncFromFc() {
        if (fcEntity == null) return;
        // Position — single source of truth.
        setPos(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
        // Rotation — also single source of truth (FC AI's lookHelper sets these).
        setYRot(fcEntity.rotationYaw);
        setXRot(fcEntity.rotationPitch);
        this.yBodyRot = fcEntity.renderYawOffset;
        this.yHeadRot = fcEntity.rotationYawHead;
        this.yBodyRotO = fcEntity.prevRenderYawOffset;
        this.yHeadRotO = fcEntity.prevRotationYawHead;
        // Fall distance for vanilla MC's fall-damage hooks (FC.moveEntity
        // already applied damage via fall(), this just keeps MC in sync).
        this.fallDistance = fcEntity.fallDistance;
        this.setOnGround(fcEntity.onGround);
        // Zero MC delta so MC's friction loop is inert.
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (fcEntity.isDead) {
            discard();
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle overrides
    // ------------------------------------------------------------------

    /** No-op: FC AI runs in fcEntity.onUpdate(). MC AI is bypassed. */
    @Override
    protected void customServerAiStep() {}

    /** No-op: FC physics owns motion. MC physics is bypassed. */
    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {}

    /**
     * FC-driven puppets are never "stuck in a wall" from MC's perspective. FC owns
     * this entity's collision and applies its own suffocation damage inside
     * fcEntity.onUpdate(); MC's LivingEntity.baseTick() also checks isInWall() and
     * deals 1 suffocation damage/tick, but it runs on the puppet's MC position/eye
     * box (which does not match FC's), so it fired spuriously on essentially every
     * FC mob — the "all mobs red / randomly dying" bug. Returning false disables the
     * redundant MC check without affecting FC's authoritative simulation.
     */
    @Override
    public boolean isInWall() { return false; }

    /** No-op: FC owns body/head rotation. Prevents MC's LivingEntity.aiStep
     *  from overwriting yBodyRot/yHeadRot that we set from FC's values. */
    @Override
    protected float tickHeadTurn(float yRot, float animStep) { return animStep; }

    @Override
    public void tick() {
        if (fcEntity == null) ensureFcEntity();

        // CLIENT SIDE: FC physics only run on the server. The client
        // receives position updates via network packets. If we ran
        // fcEntity.onUpdate() on the client with the dummy world, the
        // client FC entity would stay at its spawn position (no real
        // WorldBridge) and overwrite the server-synced position every tick.
        if (level().isClientSide) {
            // R8: MC interpolates the position first (super.tick -> aiStep lerp); then the
            // render-only FC animation tick advances prev*/limbs/body/head/swing/hurt/death
            // timers from it, once per tick, without running FC physics.
            super.tick();
            if (fcEntity != null) {
                FCPuppetSupport.clientAnimTick(this, fcEntity, clientAnim);
            }
            return;
        }

        // Puppet mode: FC entity is the source of truth for position,
        // motion, AI, and physics. MC proxy entity is a "puppet" that
        // mirrors FC's state every tick for rendering and network sync.
        // Knockback (set on MC.deltaMovement by hurt() pipeline) flows
        // INTO FC; everything else flows FROM FC.
        if (fcEntity != null) {
            // 1. One-way state IN: knockback velocity from MC -> FC,
            //    plus a few non-position fields MC owns (fire, water).
            // Transfer knockback only — see ProxyAnimal for rationale.
            if (pendingKnockback) {
                pendingKnockback = false;
                net.minecraft.world.phys.Vec3 d = getDeltaMovement();
                if (d.lengthSqr() > 1.0E-6) {
                    fcEntity.motionX += d.x;
                    fcEntity.motionY += d.y;
                    fcEntity.motionZ += d.z;
                }
            }
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            fcEntity.entityId = getId();
            fcEntity.ticksExisted = tickCount;
            // S8: inWater keeps FC's own previous-tick value (handleWaterMovement runs first in
            // onEntityUpdate, vanilla/server Entity.java:461, before any AI reads it), so FC sees the
            // water-entry edge and plays its splash; see syncToFc. FC's air logic reads
            // isInsideOfMaterial, not this flag, so D1 is unaffected.
            // Two-way fire sync — only raise on external MC ignition; FC's
            // onEntityUpdate decrements. See syncToFc for full rationale.
            int mcFire = getRemainingFireTicks();
            if (mcFire > fcEntity.fire) {
                fcEntity.fire = mcFire;
            }

            // 2. FC simulation: AI -> moveEntityWithHeading -> vanilla
            //    1.5.2 moveEntity (collision, gravity, friction, fall
            //    damage). At runtime, moveEntity is FC's actual code
            //    (we exclude Modern-Common's stub from the classpath).
            //
            // Unconditionally sanitize ALL entity state at tick start.
            // Something inside FC's onUpdate produces NaN posX/posZ every
            // tick via an unknown code path. Instead of tracing the exact
            // source, ensure the entity ALWAYS starts with valid state.
            if (Double.isFinite(fcEntity.posX) && Double.isFinite(fcEntity.posY) && Double.isFinite(fcEntity.posZ)) {
                lastGoodPosX = fcEntity.posX;
                lastGoodPosY = fcEntity.posY;
                lastGoodPosZ = fcEntity.posZ;
            } else {
                fcEntity.posX = lastGoodPosX;
                fcEntity.posY = lastGoodPosY;
                fcEntity.posZ = lastGoodPosZ;
            }
            if (!Double.isFinite(fcEntity.motionX)) fcEntity.motionX = 0;
            if (!Double.isFinite(fcEntity.motionY)) fcEntity.motionY = 0;
            if (!Double.isFinite(fcEntity.motionZ)) fcEntity.motionZ = 0;
            if (!Float.isFinite(fcEntity.rotationYaw)) fcEntity.rotationYaw = 0;
            if (!Float.isFinite(fcEntity.rotationPitch)) fcEntity.rotationPitch = 0;
            // ALWAYS rebuild BB and prevPos from the (now-sanitized) position.
            // This is the nuclear option: whatever corrupted BB/prevPos last
            // tick gets wiped before FC code can read it.
            fcEntity.setPosition(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
            fcEntity.prevPosX = fcEntity.posX;
            fcEntity.prevPosY = fcEntity.posY;
            fcEntity.prevPosZ = fcEntity.posZ;
            fcEntity.lastTickPosX = fcEntity.posX;
            fcEntity.lastTickPosY = fcEntity.posY;
            fcEntity.lastTickPosZ = fcEntity.posZ;

            // Snapshot pos/rotation/motion BEFORE the FC tick so the NaN
            // guard below can revert if FC produces non-finite output.
            double snapPosX = fcEntity.posX, snapPosY = fcEntity.posY, snapPosZ = fcEntity.posZ;
            float snapYaw = fcEntity.rotationYaw, snapPitch = fcEntity.rotationPitch;
            double snapMotX = fcEntity.motionX, snapMotY = fcEntity.motionY, snapMotZ = fcEntity.motionZ;

            reconcileHealth();
            FCPuppetSupport.dropProtectedPlayerTargets(fcEntity);
            try {
                FCEnvHurtGuard.runOnUpdate(fcEntity, this);
                PossessionDiagnostics.poll(this, fcEntity);
            } catch (Throwable e) {
                // HAI-10: one stack trace per distinct throw site, not a 1-in-100-ticks sample.
                FCPuppetSupport.logUpdateFailureOnce(LOGGER, fcEntity, e);
            }
            // D6: FC's own damage this update (fall, drowning, fire/lava, cactus) -> MC health;
            // an FC-side death becomes an MC death (event 3 to clients).
            reconcileHealth();

            // Compute onGround directly: FC's moveEntity misses ground detection when
            // entity-collision pushes produce large horizontal motion, and MC's onGround
            // is also false because travel() is a no-op. SW4: only a block with a real
            // collision shape under the feet counts (never water, lava or plants).
            try {
                if (FCPuppetSupport.hasGroundBelow(level(), fcEntity)) {
                    fcEntity.onGround = true;
                }
            } catch (Throwable t) {
                FCPuppetSupport.logFailureOnce(LOGGER, "Ground check", fcEntity, t);
            }

            // Defensive NaN/Infinity guard — fix ONLY the fields that are
            // actually non-finite. The old approach reverted ALL state when
            // ANY field was NaN, which destroyed gravity accumulation when
            // only rotationYaw was NaN (common: atan2 in FC's lookHelper).
            if (!Double.isFinite(fcEntity.posX)) fcEntity.posX = snapPosX;
            if (!Double.isFinite(fcEntity.posY)) fcEntity.posY = snapPosY;
            if (!Double.isFinite(fcEntity.posZ)) fcEntity.posZ = snapPosZ;
            if (!Double.isFinite(fcEntity.motionX)) fcEntity.motionX = 0.0;
            if (!Double.isFinite(fcEntity.motionY)) fcEntity.motionY = 0.0;
            if (!Double.isFinite(fcEntity.motionZ)) fcEntity.motionZ = 0.0;
            if (!Float.isFinite(fcEntity.rotationYaw)) fcEntity.rotationYaw = snapYaw;
            if (!Float.isFinite(fcEntity.rotationPitch)) fcEntity.rotationPitch = snapPitch;

            // Recalculate the bounding box from sanitized position. During
            // onUpdate, NaN rotationYaw → NaN motionX/Z → moveEntity offsets
            // the bounding box by NaN, corrupting it permanently. The field
            // fixes above restore position/motion but NOT the bounding box.
            // setPosition rebuilds the BB from posX/posY/posZ.
            if (fcEntity.boundingBox != null
                    && (!Double.isFinite(fcEntity.boundingBox.minX)
                        || !Double.isFinite(fcEntity.boundingBox.minZ))) {
                fcEntity.setPosition(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
            }

            // 3. Copy FC's new state OUT to the MC proxy for rendering
            //    and network broadcast. Save old position for interpolation
            //    BEFORE updating, so the renderer can smoothly animate.
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
            setPos(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
            setYRot(fcEntity.rotationYaw);
            setXRot(fcEntity.rotationPitch);
            this.yBodyRot = fcEntity.renderYawOffset;
            this.yHeadRot = fcEntity.rotationYawHead;
            this.yBodyRotO = fcEntity.prevRenderYawOffset;
            this.yHeadRotO = fcEntity.prevRotationYawHead;
            this.fallDistance = fcEntity.fallDistance;
            setOnGround(fcEntity.onGround);
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            // Fire write-back: FC's onEntityUpdate decremented (and applied
            // burn damage); mirror onto MC so the shared on-fire flag is set
            // and clients render flames.
            setRemainingFireTicks(Math.max(0, fcEntity.fire));

            if (fcEntity.isDead) {
                discard();
            }
        }

        // 4. Run MC's tick (chunk tracking, despawn, network sync).
        //    travel() and customServerAiStep are no-ops so MC physics
        //    cannot move us. The chunk tracker compares MC.position vs
        //    its internal xp/yp/zp and broadcasts on change.
        // Save xo/yo/zo BEFORE super.tick() overwrites them, then restore.
        double savedXo = this.xo, savedYo = this.yo, savedZo = this.zo;
        // D1: FC's own air logic (EntityLiving.onEntityUpdate:618-647, EntityWaterMob) is the
        // only one; MC's air stays full so ForgeHooks.onLivingBreathe never drowns a puppet.
        if (getAirSupply() < getMaxAirSupply()) setAirSupply(getMaxAirSupply());
        super.tick();

        // 5. Re-pin position and restore interpolation origin.
        if (fcEntity != null) {
            setPos(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            this.xo = savedXo;
            this.yo = savedYo;
            this.zo = savedZo;
        }

        if (!level().isClientSide && fcEntity != null) {
            if ((tickCount % 2) == 0) {
                BTWNetwork.broadcastFCEntityState(this, fcEntity);
            }
            animSender.send(this, fcEntity);
            // WitherBoss.customServerAiStep: the bar follows health (FC heals during the summon
            // charge reach MC through the health link).
            if (bossEvent != null) {
                bossEvent.setProgress(net.minecraft.util.Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
            }
        }
    }

    /** D6: see {@link FCPuppetSupport.HealthLink}. Never throws (tick path). */
    private void reconcileHealth() {
        if (fcEntity == null) return;
        try {
            healthLink.reconcile(this, fcEntity, amount -> {
                // What MC's own hurt() would have recorded (LivingEntity.hurt invulnerability
                // window), so MC-side lava/fire damage in the same window doesn't stack on top.
                if (invulnerableTime <= 10) {
                    invulnerableTime = 20;
                    lastHurt = amount;
                } else {
                    lastHurt += amount;
                }
            });
        } catch (Throwable t) {
            // HAI-10: one stack trace per distinct throw site (tick path).
            FCPuppetSupport.logFailureOnce(LOGGER, "Health reconcile", fcEntity, t);
        }
    }

    /**
     * FC's onDeathUpdate (vanilla/client EntityLiving.java:720-760) owns a puppet's death: it
     * spawns the XP orbs at deathTime 20 and setDead()s, which tick() turns into discard(). MC's
     * own counter starts whenever MC learns of the death and could remove(KILLED) the puppet a
     * tick before FC's orb spawn; while the FC entity is still finishing its death, only count
     * (falling back to MC removal if FC stalls).
     */
    @Override
    protected void tickDeath() {
        if (!level().isClientSide && fcEntity != null && !fcEntity.isDead && deathTime < 40) {
            ++deathTime;
            return;
        }
        super.tickDeath();
    }

    /** R8: hand FC-relevant entity events (hurt, death, ...) to the client FC entity. */
    @Override
    public void handleEntityEvent(byte id) {
        if (level().isClientSide && fcEntity != null) {
            FCPuppetSupport.forwardEntityEvent(fcEntity, id);
        }
        super.handleEntityEvent(id);
    }

    /** R8: 1.20.1 announces a hurt with a damage event, 1.5.2 with entity event 2. */
    @Override
    public void handleDamageEvent(DamageSource source) {
        super.handleDamageEvent(source);
        if (level().isClientSide && fcEntity != null) {
            FCPuppetSupport.forwardEntityEvent(fcEntity, (byte) 2);
        }
    }

    /** D-M1: FC plays its own hurt/death sounds; MC's GENERIC_HURT/GENERIC_DEATH are not 1.5.2's. */
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return null; }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return null; }

    /**
     * S8: FC's Entity.handleWaterMovement owns the puppet's whole 1.5.2 water-entry splash (vanilla/server
     * Entity.java:1081-1110): 'liquid.splash' with its volume from FC's motion, and the 1 + width*20 'bubble' and
     * 'splash' particles, which reach clients through WorldBridge.spawnParticle. MC's own splash detects the
     * water first (its check is looser than FC's box shrunk by 0.4) and, with the puppet's zeroed deltaMovement,
     * played an inaudible zero-volume splash. One owner: on the server MC keeps only its SPLASH game event (no
     * 1.5.2 equivalent); the client puppet, whose FC entity does not simulate, draws none of MC's particles, which
     * would double FC's.
     */
    @Override
    protected void doWaterSplashEffect() {
        if (fcEntity == null) {
            super.doWaterSplashEffect();
            return;
        }
        if (!level().isClientSide) gameEvent(net.minecraft.world.level.gameevent.GameEvent.SPLASH);
    }

    /**
     * D1: MC must not drown a puppet. FC's EntityLiving.onEntityUpdate (vanilla/client
     * EntityLiving.java:618-647) and EntityWaterMob run their own air logic and damage, which
     * reaches MC health through the health link; MC's drowning on top was a second air pool
     * (fc_squid drowned in water on the MC side).
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean canBreatheUnderwater() { return true; }

    /**
     * D8: FC's handleLavaMovement -> setOnFireFromLava (vanilla/server Entity.java:489-493, 521-528)
     * is the puppet's only lava damage and ignition, and skips fire-immune FC mobs.
     */
    @Override
    public void lavaHurt() {
        if (fcEntity == null) super.lavaHurt();
    }

    /**
     * Mob.checkDespawn (Mob.java:707-735) also runs on the puppet; FC's own despawnEntity
     * (vanilla/server EntityLiving.java:3583-3611) never removes a persistent or !canDespawn() mob
     * (e.g. one that picked up loot), so MC must not either. Withers keep canDespawn() == true but
     * override despawnEntity to never despawn (vanilla/server EntityWither.java:551-554).
     */
    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        if (isFcBoss()) return false;
        if (fcEntity != null && !FCPuppetSupport.fcCanDespawn(fcEntity)) return false;
        return super.removeWhenFarAway(distanceSq);
    }

    // ------------------------------------------------------------------
    // Wither boss bar
    // ------------------------------------------------------------------

    /**
     * 1.5.2 drew the wither's health bar from the renderer (RenderWither.func_82418_a ->
     * BossStatus.func_82824_a, vanilla/client RenderWither.java:31); 1.20.1 drives it from the server
     * (WitherBoss.bossEvent). Created for FC withers only; no screen darkening (1.5.2 had none).
     */
    private net.minecraft.server.level.ServerBossEvent bossEvent;
    private Boolean fcBoss;

    private boolean isFcBoss() {
        if (fcBoss == null) {
            if (fcEntity != null) {
                fcBoss = fcEntity instanceof btw.modern.EntityWither;
            } else {
                String name = getFcClassName();
                if (name == null || name.isEmpty()) return false;
                try {
                    fcBoss = btw.modern.EntityWither.class.isAssignableFrom(Class.forName(name));
                } catch (Throwable t) {
                    fcBoss = false;
                }
            }
        }
        return fcBoss;
    }

    private net.minecraft.server.level.ServerBossEvent bossEvent() {
        if (bossEvent == null && !level().isClientSide && isFcBoss()) {
            bossEvent = new net.minecraft.server.level.ServerBossEvent(bossName(),
                    net.minecraft.world.BossEvent.BossBarColor.PURPLE,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
            bossEvent.setProgress(net.minecraft.util.Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        }
        return bossEvent;
    }

    /** 1.5.2 EntityWither.getEntityName -> "Wither" unless named. */
    private net.minecraft.network.chat.Component bossName() {
        return hasCustomName() ? getCustomName()
                : net.minecraft.network.chat.Component.translatable("entity.minecraft.wither");
    }

    @Override
    public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        try {
            var bar = bossEvent();
            if (bar != null) bar.addPlayer(player);
        } catch (Throwable t) {
            if (FCPuppetSupport.firstFailure("bossbar|" + t.getClass().getName())) {
                LOGGER.warn("Wither boss bar failed: {}", t.toString());
            }
        }
    }

    @Override
    public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossEvent != null) bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        if (bossEvent != null) bossEvent.setName(bossName());
    }

    /** Delegate to FC's interact() for villager trading, etc. */
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        if (fcEntity != null && hand == net.minecraft.world.InteractionHand.MAIN_HAND
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            btw.forge.PlayerBridge pb = btw.forge.PlayerBridge.getOrCreate(sp);
            try {
                boolean handled = fcEntity.interact(pb);
                // FC edits the held stack in place (feeding --stackSize, bucket swaps); write those
                // slots back to the MC inventory (selective per-slot diff, no-op when unchanged).
                pb.syncInventoryToReal();
                if (handled) {
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            } catch (Throwable t) {
                // Errors from FC interact (e.g. villager trade generation hitting
                // a bridge gap) must not crash the interaction packet handler.
                FCPuppetSupport.logFailureOnce(LOGGER, "FC interact()", fcEntity, t);
                // INV-RESYNC: a failed FC call lands nothing; the next sync dropped its edits, it would now write them
                pb.discardInventoryEdits();
            }
        }
        return super.mobInteract(player, hand);
    }

    /** FC handles its own entity collision; MC's push/cramming must be ignored. */
    @Override
    public void push(double x, double y, double z) {}

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void pushEntities() {} // FC handles via Entity.applyEntityCollision

    /** Real knockback from hurt() — flag it so tick() transfers the delta. */
    @Override
    public void knockback(double strength, double x, double z) {
        super.knockback(strength, x, z);
        pendingKnockback = true;
    }

    private boolean forwardingHurtToFc = false;

    public boolean hurt(DamageSource source, float amount) {
        // D8: FC's own update applies burning, lava and fire-block damage (honouring FC fire
        // immunity) and the health link mirrors it; MC's baseTick copy would be a second application.
        if (fcEntity != null && !level().isClientSide && FCPuppetSupport.isFcSimulatedFireDamage(source)) {
            return false;
        }
        // Let MC's full hurt() pipeline run: hurt sound, red flash,
        // knockback, hit particles, hurt animation, AI revenge target,
        // damage application, death/loot. We do not short-circuit any
        // of MC's "million little subroutines" — they all fire normally
        // on the MC proxy entity.
        boolean result = super.hurt(source, amount);

        // Mirror the result to FC so FC game logic stays in sync:
        // health value, AI revenge target, attacker tracking. FC's
        // custom death drops (CheckForScrollDrop, etc.) are triggered
        // separately via die() — see ProxyMob.die() override.
        if (result && fcEntity != null) {
            // R2: 1.5.2 attackEntityFrom credits every landed hit (recentlyHit/attackingPlayer)
            // even when FC itself never sees it (inside FC's hurt-resistance window).
            FCPuppetSupport.creditAttacker(fcEntity, source);
            // 1.5.2: all damage funnels through EntityLiving.attackEntityFrom,
            // so FC overrides must see MC-side hits too — FCEntityPigZombie
            // group anger (Common FCEntityPigZombie.java:32), FCEntityEnderman
            // teleport dodge (:241), FCEntitySquid hit reactions (:535-569).
            // Reentrancy flag guards against FC-side feedback; the health
            // mirror below keeps MC authoritative for HP (FC's own damage
            // accounting is overwritten by MC's value). Lethal hits are NOT
            // forwarded: super.hurt() already ran die() -> fcEntity.onDeath,
            // and forwarding would zero FC health and fire onDeath (and its
            // drops) a second time.
            if (!forwardingHurtToFc && !isDeadOrDying() && !fcEntity.isDead) {
                forwardingHurtToFc = true;
                try {
                    fcEntity.attackEntityFrom(translateDamageSource(source), (int) amount);
                } catch (Throwable t) {
                    // HAI-10: once per throw site with its stack, not a line per hit.
                    FCPuppetSupport.logFailureOnce(LOGGER, "FC attackEntityFrom()", fcEntity, t);
                } finally {
                    forwardingHurtToFc = false;
                }
            }
            // Don't mirror MC health back onto an already-dead FC entity: with two-way
            // fire sync, FC can die internally (health 0) while MC health is still >0 this
            // tick — re-raising it would resurrect the FC entity and re-run its death path.
            if (fcEntity.health > 0) {
                fcEntity.health = FCPuppetSupport.mcHpToFc(getHealth());
            }
            // Agreed state for the health link. If the forwarded hit killed FC while MC lives,
            // leave it unmarked so the next reconcile turns that FC death into an MC death.
            if (!(fcEntity.health <= 0 && getHealth() > 0)) {
                healthLink.mark(this, fcEntity);
            }
            btw.modern.EntityLiving fcAttacker = wrapAttacker(source);
            if (fcAttacker != null) {
                fcEntity.lastAttackingEntity = fcAttacker;
                fcEntity.setRevengeTarget(fcAttacker);
            }
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        // Server only: on the client the FC death runs through the forwarded entity event 3
        // (1.5.2 handleHealthUpdate(3), see handleEntityEvent).
        if (fcEntity != null && !level().isClientSide) {
            try {
                // Only run FC's death path if FC hasn't already died internally (e.g. from
                // fire damage in its own onUpdate) — otherwise onDeath and its drops fire
                // twice. Suppress vanilla loot either way since FC owns drops.
                if (fcEntity.health > 0) {
                    // 1.5.2 attackEntityFrom order (vanilla/client EntityLiving.java:1090-1159):
                    // health drops, the hit is credited (R2: a one-hit player kill now yields
                    // player XP orbs, not the unowned dragon orb), death sound, then onDeath.
                    FCPuppetSupport.creditAttacker(fcEntity, source);
                    fcEntity.health = 0;
                    FCPuppetSupport.playFcDeathSound(fcEntity);
                    btw.modern.DamageSource fcSource = translateDamageSource(source);
                    fcEntity.onDeath(fcSource);
                    syncFromFc();
                }
                // FC's onDeath already ran dropFewItems + CheckForScrollDrop +
                // dropEquipment. Suppress vanilla's loot table so drops don't
                // double-fire, then let MC handle death animation / XP / removal.
                suppressVanillaLoot = true;
            } catch (Throwable t) {
                // If FC's death path hit a bridge gap, fall back to vanilla loot
                // (suppressVanillaLoot stays false) instead of crashing the server.
                FCPuppetSupport.logFailureOnce(LOGGER, "FC onDeath()", fcEntity, t);
            }
        }
        super.die(source);
        suppressVanillaLoot = false;
        if (fcEntity != null && !level().isClientSide) healthLink.mark(this, fcEntity);
    }

    private boolean suppressVanillaLoot = false;

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        if (suppressVanillaLoot) return;
        super.dropAllDeathLoot(source);
    }

    // ------------------------------------------------------------------
    // Attribute / health helpers
    // ------------------------------------------------------------------

    public static AttributeSupplier.Builder createProxyMobAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FCClassName")) {
            fcClassName = tag.getString("FCClassName");
            if (fcEntity == null && !fcClassName.isEmpty()) {
                // RE7: keep the Health MC just restored (no reset to max).
                loadingSaveData = tag.contains("Health", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC);
                try {
                    ensureFcEntity(); // recreate on server side too
                } finally {
                    loadingSaveData = false;
                }
            }
        }
        replacedCategory = readSpawnCategory(tag, replacedCategory, LOGGER); // W7
        if (fcEntity != null && tag.contains("FCData")) {
            CompoundTag fcCompound = tag.getCompound("FCData");
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.readFromNBT(wrapper);
                fcEntity.readEntityFromNBT(wrapper);
            } catch (Throwable e) {
                // ML-1: frozen Entity.readFromNBT wraps the real error in a ReportedException
                // ("Loading entity NBT", vanilla/server Entity.java:2970-2975).
                FCPuppetSupport.logNbtFailure(LOGGER, "Reading FC entity NBT", fcEntity, e);
            }
        }
        // MC owns puppet HP: FC's saved Health (1.5.2 readEntityFromNBT) yields to MC's.
        if (fcEntity != null && !level().isClientSide) {
            // RE7: MAX_HEALTH from the restored FC state (slime size: EntitySlime.readEntityFromNBT
            // -> setSlimeSize, getMaxHealth = size*size, vanilla/server EntitySlime.java:54-84),
            // then the loaded Health clamped to it (setBaseValue does not clamp).
            FCPuppetSupport.seedHealthFromFc(this, fcEntity, false);
            setHealth(Math.min(getHealth(), getMaxHealth()));
            if (fcEntity.health > 0 && getHealth() > 0) {
                fcEntity.health = FCPuppetSupport.mcHpToFc(getHealth());
            }
            healthLink.mark(this, fcEntity);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("FCClassName", fcClassName);
        if (replacedCategory != null) tag.putString(SPAWN_CATEGORY_TAG, replacedCategory.getName()); // W7
        if (fcEntity != null) {
            CompoundTag fcCompound = new CompoundTag();
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.writeToNBT(wrapper);
                fcEntity.writeEntityToNBT(wrapper);
            } catch (Throwable e) {
                // ML-1: frozen Entity.writeToNBT wraps the real error in a ReportedException
                // ("Saving entity NBT", vanilla/server Entity.java:1468-1473).
                FCPuppetSupport.logNbtFailure(LOGGER, "Writing FC entity NBT", fcEntity, e);
            }
            tag.put("FCData", fcCompound);
        }
    }

    // ------------------------------------------------------------------
    // DamageSource translation
    // ------------------------------------------------------------------

    /**
     * Translates the MC attacker behind a DamageSource into an FC
     * EntityLiving so {@link btw.modern.EntityLiving#setRevengeTarget} can
     * record it. Without this, FC's panic / hurt-by-target AI never fires
     * because {@code entityLivingToAttack} stays null.
     *
     * Returns null if the source has no entity attacker (env damage, etc.).
     */
    static btw.modern.EntityLiving wrapAttacker(DamageSource source) {
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (attacker == null) return null;
        if (attacker instanceof ProxyMob pm && pm.fcEntity != null) return pm.fcEntity;
        if (attacker instanceof ProxyAnimal pa && pa.getFcEntity() != null) return pa.getFcEntity();
        if (attacker instanceof ProxyPathfinderMob pp && pp.getFcEntity() != null) return pp.getFcEntity();
        if (attacker instanceof net.minecraft.world.entity.LivingEntity le) {
            return LivingEntityBridge.wrapLiving(le);
        }
        return null;
    }

    static btw.modern.DamageSource translateDamageSource(DamageSource source) {
        // 1.5.2 DamageSource.causePlayerDamage / causeMobDamage — attach the
        // attacker before falling back to the msgId map. FC's patched
        // EntityLiving.onDeath (vanilla/server EntityLiving.java:3325-3395)
        // reads source.getEntity() for onKillEntity, the player-weapon
        // looting modifier and CheckForHeadDrop, and
        // FCEntityMechPower.attackEntityFrom:136-143 checks it for the
        // creative one-hit break. Without this, every entity-inflicted hit
        // arrived as the entity-less DamageSource.generic.
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        // Indirect damage (arrow/snowball/fireball/thrown potion): the direct entity is the
        // projectile, the causing entity the shooter. 1.5.2 uses an EntityDamageSourceIndirect
        // (isProjectile) here, not a direct melee source, so armor/enchant handling matches.
        if (source.getDirectEntity() != null && source.getDirectEntity() != attacker) {
            btw.modern.EntityLiving fcThrower = wrapAttacker(source);
            return btw.modern.DamageSource.causeThrownDamage(fcThrower, fcThrower);
        }
        if (attacker instanceof net.minecraft.server.level.ServerPlayer sp) {
            return btw.modern.DamageSource.causePlayerDamage(PlayerBridge.getOrCreate(sp));
        }
        btw.modern.EntityLiving fcAttacker = wrapAttacker(source);
        if (fcAttacker != null) {
            return btw.modern.DamageSource.causeMobDamage(fcAttacker);
        }
        String msgId = source.getMsgId();
        // Map common modern damage type message IDs to FC DamageSource types
        switch (msgId) {
            case "inFire":
                return btw.modern.DamageSource.inFire;
            case "onFire":
                return btw.modern.DamageSource.onFire;
            case "lava":
                return btw.modern.DamageSource.lava;
            case "inWall":
                return btw.modern.DamageSource.inWall;
            case "drown":
                return btw.modern.DamageSource.drown;
            case "starve":
                return btw.modern.DamageSource.starve;
            case "cactus":
                return btw.modern.DamageSource.cactus;
            case "fall":
                return btw.modern.DamageSource.fall;
            case "outOfWorld":
                return btw.modern.DamageSource.outOfWorld;
            case "magic":
                return btw.modern.DamageSource.magic;
            case "wither":
                return btw.modern.DamageSource.wither;
            case "anvil":
                return btw.modern.DamageSource.anvil;
            case "fallingBlock":
                return btw.modern.DamageSource.fallingBlock;
            default:
                return btw.modern.DamageSource.generic;
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
