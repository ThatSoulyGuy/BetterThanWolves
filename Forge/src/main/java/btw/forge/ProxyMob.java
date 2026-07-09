package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
     * Catches every removal path so we can diagnose disappearing entities.
     */
    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        String key = (fcEntity != null ? fcEntity.getClass().getSimpleName() : "null") + "|" + reason;
        if (LOGGED_REMOVALS.add(key)) {
            LOGGER.warn("ProxyMob.remove({}) — fc={}, id={}, pos={},{},{}",
                    reason,
                    fcEntity != null ? fcEntity.getClass().getSimpleName() : "null",
                    getId(), getX(), getY(), getZ());
            StackTraceElement[] st = new Throwable().getStackTrace();
            for (int i = 1; i < Math.min(12, st.length); i++) {
                LOGGER.warn("    at {}", st[i]);
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
        }
        syncToFc();
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
                    var attr = getAttribute(Attributes.MAX_HEALTH);
                    if (attr != null) attr.setBaseValue(fcMaxHp);
                    setHealth(fcMaxHp);
                    fcEntity.health = fcMaxHp;
                }
                ProxyMobTaskDumper.dumpFcTasks(fcEntity);
            }
        } catch (Throwable e) {
            LOGGER.info("Could not create FC entity {}: {}", className, e.getMessage(), e);
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
        fcEntity.inWater = isInWater();
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
            // Animation/rotation data arrives via FCEntityStateSync packet
            // (bridged from server FC entity's actual computed values).
            // Keep FC entity position in sync for renderer origin.
            if (fcEntity != null) {
                fcEntity.prevPosX = fcEntity.posX;
                fcEntity.prevPosY = fcEntity.posY;
                fcEntity.prevPosZ = fcEntity.posZ;
                fcEntity.posX = getX();
                fcEntity.posY = getY();
                fcEntity.posZ = getZ();
            }
            super.tick();
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
            fcEntity.inWater = isInWater();
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

            double preMotY = fcEntity.motionY;
            try {
                fcEntity.onUpdate();
                PossessionDiagnostics.poll(this, fcEntity);
            } catch (Throwable e) {
                if (tickCount % 100 == 0) {
                    LOGGER.warn("FC entity {} onUpdate() threw {}: {}",
                        fcEntity.getClass().getSimpleName(),
                        e.getClass().getName(), e.getMessage());
                }
            }

            // Compute onGround directly: check if the block below the entity's
            // feet is solid. FC's moveEntity misses ground detection when
            // entity-collision pushes produce large horizontal motion, and
            // MC's onGround is also false because travel() is a no-op.
            {
                int bx = net.minecraft.util.Mth.floor(fcEntity.posX);
                int by = net.minecraft.util.Mth.floor(fcEntity.boundingBox.minY - 0.01);
                int bz = net.minecraft.util.Mth.floor(fcEntity.posZ);
                net.minecraft.core.BlockPos belowPos = new net.minecraft.core.BlockPos(bx, by, bz);
                boolean solidBelow = !level().getBlockState(belowPos).isAir();
                if (solidBelow) {
                    fcEntity.onGround = true;
                }
            }

            // --- Debug: periodic AI/movement diagnostics (every 60 ticks / 3s) ---
            if (!level().isClientSide && (tickCount % 60) == 0) {
                btw.modern.PathNavigate nav = fcEntity.getNavigator();
                boolean navNoPath = nav.noPath();
                btw.modern.PathEntity navPath = nav.getPath();
                int pathLen = (navPath != null) ? navPath.getCurrentPathLength() : 0;
                int pathIdx = (navPath != null) ? navPath.getCurrentPathIndex() : -1;

                btw.modern.EntityLiving atkTarget = fcEntity.getAttackTarget();
                btw.modern.EntityLiving aiTarget = fcEntity.getAITarget();

                LOGGER.info("[AI-DBG] {} | onGround={} mF={} aiSpd={} mot=({},{},{}) | nav: noPath={} pathLen={} pathIdx={} | atkTarget={} aiTarget={} | pathfindCalls={}",
                    fcEntity.getClass().getSimpleName(),
                    fcEntity.onGround,
                    String.format("%.3f", fcEntity.moveForward),
                    String.format("%.3f", fcEntity.getAIMoveSpeed()),
                    String.format("%.4f", fcEntity.motionX),
                    String.format("%.4f", fcEntity.motionY),
                    String.format("%.4f", fcEntity.motionZ),
                    navNoPath, pathLen, pathIdx,
                    atkTarget != null ? atkTarget.getClass().getSimpleName() : "none",
                    aiTarget != null ? aiTarget.getClass().getSimpleName() : "none",
                    btw.modern.World.pathfindCallCount.get());
            }

            // --- Debug: flame particle at next path waypoint (every 20 ticks) ---
            if (!level().isClientSide && (tickCount % 20) == 0
                    && level() instanceof ServerLevel sl) {
                btw.modern.PathNavigate nav2 = fcEntity.getNavigator();
                if (!nav2.noPath()) {
                    btw.modern.PathEntity path = nav2.getPath();
                    if (path != null && path.getCurrentPathIndex() < path.getCurrentPathLength()) {
                        btw.modern.PathPoint wp = path.getPathPointFromIndex(path.getCurrentPathIndex());
                        sl.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.FLAME,
                            wp.xCoord + 0.5, wp.yCoord + 0.5, wp.zCoord + 0.5,
                            1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
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
        super.tick();

        // 5. Re-pin position and restore interpolation origin.
        if (fcEntity != null) {
            setPos(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            this.xo = savedXo;
            this.yo = savedYo;
            this.zo = savedZo;
        }

        if (!level().isClientSide && fcEntity != null && (tickCount % 2) == 0) {
            BTWNetwork.broadcastFCEntityState(this, fcEntity);
        }
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
                if (fcEntity.interact(pb)) {
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            } catch (Throwable t) {
                // Errors from FC interact (e.g. villager trade generation hitting
                // a bridge gap) must not crash the interaction packet handler.
                LOGGER.warn("FC entity interact() threw: {}: {}",
                        t.getClass().getSimpleName(), t.getMessage());
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
        // [DIAG] capture what is damaging puppet mobs (red-flash + random-death bug)
        LOGGER.warn("[MOB-DMG] fc={} src={} amt={} hpBefore={} y={} fire={} inWall={}",
                fcClassName, source.type().msgId(), amount, getHealth(), getY(),
                fcEntity != null ? fcEntity.fire : -999, isInWall());
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
                    LOGGER.warn("FC entity attackEntityFrom() threw: {}: {}",
                            t.getClass().getSimpleName(), t.getMessage());
                } finally {
                    forwardingHurtToFc = false;
                }
            }
            // Don't mirror MC health back onto an already-dead FC entity: with two-way
            // fire sync, FC can die internally (health 0) while MC health is still >0 this
            // tick — re-raising it would resurrect the FC entity and re-run its death path.
            if (fcEntity.health > 0) {
                fcEntity.health = (int) Math.max(0, getHealth());
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
        if (fcEntity != null) {
            try {
                // Only run FC's death path if FC hasn't already died internally (e.g. from
                // fire damage in its own onUpdate) — otherwise onDeath and its drops fire
                // twice. Suppress vanilla loot either way since FC owns drops.
                if (fcEntity.health > 0) {
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
                LOGGER.warn("FC entity onDeath() threw: {}: {}",
                        t.getClass().getSimpleName(), t.getMessage());
            }
        }
        super.die(source);
        suppressVanillaLoot = false;
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
                ensureFcEntity(); // recreate on server side too
            }
        }
        if (fcEntity != null && tag.contains("FCData")) {
            CompoundTag fcCompound = tag.getCompound("FCData");
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.readFromNBT(wrapper);
                fcEntity.readEntityFromNBT(wrapper);
            } catch (Throwable e) {
                // Vanilla 1.5.2 Entity.readFromNBT wraps everything in a
                // ReportedException("Loading entity NBT"); the actual error
                // lives in the cause chain. Walk it so the log is useful.
                Throwable root = e;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                LOGGER.warn("Failed to read FC entity NBT data for {}: {}: {}",
                        fcClassName, root.getClass().getSimpleName(), root.getMessage());
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("FCClassName", fcClassName);
        if (fcEntity != null) {
            CompoundTag fcCompound = new CompoundTag();
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.writeToNBT(wrapper);
                fcEntity.writeEntityToNBT(wrapper);
            } catch (Throwable e) {
                LOGGER.warn("Failed to write FC entity NBT data: {}", e.getMessage());
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
