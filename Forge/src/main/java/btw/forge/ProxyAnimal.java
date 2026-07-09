package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge-side proxy that extends {@link Animal} and forwards all lifecycle
 * callbacks to an FC {@link btw.modern.EntityLiving} instance.
 *
 * Used for FC entities whose legacy hierarchy roots at
 * {@link btw.modern.EntityAnimal} or {@link btw.modern.EntityTameable}
 * (pig, sheep, cow, chicken, wolf, ocelot, etc.).
 */
public class ProxyAnimal extends Animal
        implements net.minecraftforge.entity.IEntityAdditionalSpawnData {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyAnimal");

    /**
     * Tracks (entity class name + exception class name) pairs we've already
     * logged so we don't spam the log with the same NPE every tick. Each
     * unique (entity, exception-type) is reported once with a real stack
     * trace; subsequent occurrences are suppressed.
     */
    private static final java.util.Set<String> LOGGED_TICK_FAILURES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private btw.modern.EntityLiving fcEntity;
    private String fcClassName = "";
    private boolean pendingKnockback = false;
    private double lastGoodPosX, lastGoodPosY, lastGoodPosZ;

    public ProxyAnimal(EntityType<? extends Animal> type, Level level) {
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

    /**
     * Creates the FC entity if missing — works on both client and server.
     * On server: uses a real WorldBridge so FC AI/logic functions.
     * On client: uses a dummy world for rendering only.
     */
    private void ensureFcEntity() {
        String className = getFcClassName();
        if (className == null || className.isEmpty()) return;
        btw.modern.World world;
        if (level().isClientSide) {
            world = ProxyEntity.createDummyClientWorld(level());
        } else if (level() instanceof ServerLevel sl) {
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
                // CRITICAL: seed FC position from MC spawn coords — see ProxyMob.
                fcEntity.setLocationAndAngles(getX(), getY(), getZ(), getYRot(), getXRot());
                lastGoodPosX = getX();
                lastGoodPosY = getY();
                lastGoodPosZ = getZ();
                fcEntity.entityId = getId();
                int fcMaxHp = fcEntity.getMaxHealth();
                if (fcMaxHp > 0) {
                    var attr = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
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

    /** Puppet mode: see ProxyMob for the architectural rationale. */
    private void syncToFc() {
        if (fcEntity == null) return;
        fcEntity.entityId = getId();
        fcEntity.ticksExisted = tickCount;
        fcEntity.inWater = isInWater();
        // Two-way fire sync — only raise on external MC ignition; FC's
        // onEntityUpdate decrements. See ProxyMob.syncToFc for rationale.
        int mcFire = getRemainingFireTicks();
        if (mcFire > fcEntity.fire) {
            fcEntity.fire = mcFire;
        }
        // Knockback transfer is handled exclusively in tick() via the
        // pendingKnockback flag — see ProxyMob.syncToFc for rationale.
    }

    private void syncFromFc() {
        if (fcEntity == null) return;
        setPos(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
        setYRot(fcEntity.rotationYaw);
        setXRot(fcEntity.rotationPitch);
        this.yBodyRot = fcEntity.renderYawOffset;
        this.yHeadRot = fcEntity.rotationYawHead;
        this.yBodyRotO = fcEntity.prevRenderYawOffset;
        this.yHeadRotO = fcEntity.prevRotationYawHead;
        this.fallDistance = fcEntity.fallDistance;
        this.setOnGround(fcEntity.onGround);
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (fcEntity.isDead) {
            discard();
        }
    }

    /**
     * Catches ALL removal paths — our explicit discard(), MC's despawn,
     * MC's chunk-unload eviction, command /kill, etc. — so we can see
     * exactly which code path is killing freshly-spawned entities.
     */
    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        String key = (fcEntity != null ? fcEntity.getClass().getSimpleName() : "null") + "|" + reason;
        if (LOGGED_TICK_FAILURES.add(key)) {
            LOGGER.warn("ProxyAnimal.remove({}) — fc={}, id={}, pos={},{},{}",
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

    // ------------------------------------------------------------------
    // Lifecycle overrides
    // ------------------------------------------------------------------

    /** No-op: FC AI runs in fcEntity.onUpdate(). */
    @Override
    protected void customServerAiStep() {}

    /** No-op: FC physics owns motion. */
    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {}

    /** FC owns collision/suffocation; MC's isInWall() suffocation check is spurious
     *  on a position-driven puppet (see ProxyMob.isInWall). */
    @Override
    public boolean isInWall() { return false; }

    /** No-op: FC owns body/head rotation — see ProxyMob. */
    @Override
    protected float tickHeadTurn(float yRot, float animStep) { return animStep; }

    @Override
    public void tick() {
        if (fcEntity == null) ensureFcEntity();
        if (level().isClientSide) {
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
        // Puppet mode — see ProxyMob.tick for the architecture rationale.
        if (fcEntity != null) {
            // Transfer knockback delta from MC to FC. We use a flag set by
            // our knockback() override to distinguish real hits from routine
            // entity pushes (both set hasImpulse, so that flag is useless).
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
            // onEntityUpdate decrements. See ProxyMob.syncToFc for rationale.
            int mcFire = getRemainingFireTicks();
            if (mcFire > fcEntity.fire) {
                fcEntity.fire = mcFire;
            }

            // Snapshot the entity's position/rotation BEFORE FC physics
            // runs. If onUpdate corrupts any field with NaN/Infinity we
            // can revert to this so the corruption doesn't escape into
            // MC's entity.
            // Unconditionally sanitize state at tick start — see ProxyMob.
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
            fcEntity.setPosition(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
            fcEntity.prevPosX = fcEntity.posX;
            fcEntity.prevPosY = fcEntity.posY;
            fcEntity.prevPosZ = fcEntity.posZ;
            fcEntity.lastTickPosX = fcEntity.posX;
            fcEntity.lastTickPosY = fcEntity.posY;
            fcEntity.lastTickPosZ = fcEntity.posZ;

            double snapPosX = fcEntity.posX, snapPosY = fcEntity.posY, snapPosZ = fcEntity.posZ;
            float snapYaw = fcEntity.rotationYaw, snapPitch = fcEntity.rotationPitch;
            double snapMotX = fcEntity.motionX, snapMotY = fcEntity.motionY, snapMotZ = fcEntity.motionZ;

            try {
                FCEnvHurtGuard.runOnUpdate(fcEntity, this);
                PossessionDiagnostics.poll(this, fcEntity);
            } catch (Throwable e) {
                String key = fcEntity.getClass().getSimpleName() + "|" + e.getClass().getName();
                if (LOGGED_TICK_FAILURES.add(key)) {
                    LOGGER.warn("FC entity {} onUpdate() threw {}: {}",
                            fcEntity.getClass().getSimpleName(),
                            e.getClass().getSimpleName(),
                            e.getMessage(), e);
                    StackTraceElement[] frames = e.getStackTrace();
                    if (frames.length == 0) {
                        LOGGER.warn("    (stack trace was elided by JIT — re-run with -XX:-OmitStackTraceInFastThrow)");
                    }
                }
            }

            // Compute onGround directly — see ProxyMob for rationale.
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

            // Defensive NaN/Infinity guard — fix ONLY the fields that are
            // actually non-finite. See ProxyMob for full rationale.
            if (!Double.isFinite(fcEntity.posX)) fcEntity.posX = snapPosX;
            if (!Double.isFinite(fcEntity.posY)) fcEntity.posY = snapPosY;
            if (!Double.isFinite(fcEntity.posZ)) fcEntity.posZ = snapPosZ;
            if (!Double.isFinite(fcEntity.motionX)) fcEntity.motionX = 0.0;
            if (!Double.isFinite(fcEntity.motionY)) fcEntity.motionY = 0.0;
            if (!Double.isFinite(fcEntity.motionZ)) fcEntity.motionZ = 0.0;
            if (!Float.isFinite(fcEntity.rotationYaw)) fcEntity.rotationYaw = snapYaw;
            if (!Float.isFinite(fcEntity.rotationPitch)) fcEntity.rotationPitch = snapPitch;

            // Recalculate bounding box if NaN-corrupted — see ProxyMob for rationale.
            if (fcEntity.boundingBox != null
                    && (!Double.isFinite(fcEntity.boundingBox.minX)
                        || !Double.isFinite(fcEntity.boundingBox.minZ))) {
                fcEntity.setPosition(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
            }

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
            // Fire write-back: mirror FC's decremented counter onto MC so
            // clients render flames — see ProxyMob.tick.
            setRemainingFireTicks(Math.max(0, fcEntity.fire));
            if (fcEntity.isDead) {
                discard();
            }
        }
        double savedXo = this.xo, savedYo = this.yo, savedZo = this.zo;
        super.tick();
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

    /**
     * Returns true iff every position/rotation/motion field on the FC
     * entity is a finite (non-NaN, non-Infinity) double/float.
     */
    private static boolean isFiniteState(btw.modern.EntityLiving fc) {
        return Double.isFinite(fc.posX) && Double.isFinite(fc.posY) && Double.isFinite(fc.posZ)
            && Double.isFinite(fc.motionX) && Double.isFinite(fc.motionY) && Double.isFinite(fc.motionZ)
            && Float.isFinite(fc.rotationYaw) && Float.isFinite(fc.rotationPitch);
    }

    /** Delegate to FC's interact() for feeding, breeding, milking, etc. */
    @Override
    public net.minecraft.world.InteractionResult mobInteract(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        if (fcEntity != null && hand == net.minecraft.world.InteractionHand.MAIN_HAND
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            btw.forge.PlayerBridge pb = btw.forge.PlayerBridge.getOrCreate(sp);
            if (fcEntity.interact(pb)) {
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    /** FC handles its own entity collision; MC's push/cramming must be ignored. */
    @Override
    public void push(double x, double y, double z) {
        // No-op: FC's Entity.applyEntityCollision handles entity pushing.
        // MC's push() accumulates deltaMovement that pollutes FC's motion.
    }

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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Run MC's full hurt pipeline (sound, knockback, animation, etc.).
        boolean result = super.hurt(source, amount);
        // Mirror to FC so FC game state stays in sync, and record the
        // attacker so EntityAIPanic / EntityAIHurtByTarget actually fires.
        if (result && fcEntity != null) {
            // 1.5.2: all damage funnels through EntityLiving.attackEntityFrom,
            // so FC overrides (e.g. FCEntityWolf reactions) must see MC-side
            // hits too — see ProxyMob.hurt for the reentrancy/lethal-hit
            // rationale. MC stays authoritative for HP via the mirror below.
            if (!forwardingHurtToFc && !isDeadOrDying() && !fcEntity.isDead) {
                forwardingHurtToFc = true;
                try {
                    fcEntity.attackEntityFrom(ProxyMob.translateDamageSource(source), (int) amount);
                } catch (Throwable t) {
                    LOGGER.warn("FC entity attackEntityFrom() threw: {}: {}",
                            t.getClass().getSimpleName(), t.getMessage());
                } finally {
                    forwardingHurtToFc = false;
                }
            }
            // Don't resurrect an FC entity that already died internally (see ProxyMob.hurt).
            if (fcEntity.health > 0) {
                fcEntity.health = (int) Math.max(0, getHealth());
            }
            btw.modern.EntityLiving fcAttacker = ProxyMob.wrapAttacker(source);
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
                // Skip if FC already died internally — avoids double onDeath/drops.
                if (fcEntity.health > 0) {
                    btw.modern.DamageSource fcSource = ProxyMob.translateDamageSource(source);
                    fcEntity.onDeath(fcSource);
                    syncFromFc();
                }
                suppressVanillaLoot = true;
            } catch (Throwable t) {
                // Bridge-gap Errors (e.g. frozen EntityAnimal.onDeath reading FC
                // statics) must not crash the server; vanilla loot is the fallback.
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
    // Animal-specific overrides
    // ------------------------------------------------------------------

    /**
     * Animal requires this for breeding. We delegate to the FC entity's
     * isBreedingItem() if the FC entity exposes it.
     */
    @Override
    public boolean isFood(ItemStack stack) {
        if (fcEntity instanceof btw.modern.EntityAnimal ea && stack != null && !stack.isEmpty()) {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack == null) return false;
            return ea.isBreedingItem(fcStack);
        }
        return false;
    }

    /**
     * Required abstract from AgeableMob. We return null since FC handles
     * child creation through its own breeding system.
     */
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        // FC breeding is handled by the FC entity's own logic.
        // Returning null prevents the Forge engine from creating a baby on its own.
        return null;
    }

    // ------------------------------------------------------------------
    // Attribute helpers
    // ------------------------------------------------------------------

    public static AttributeSupplier.Builder createProxyAnimalAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    // ------------------------------------------------------------------
    // NBT
    // ------------------------------------------------------------------

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FCClassName")) {
            fcClassName = tag.getString("FCClassName");
            if (fcEntity == null && !fcClassName.isEmpty()) {
                ensureFcEntity();
            }
        }
        if (fcEntity != null && tag.contains("FCData")) {
            CompoundTag fcCompound = tag.getCompound("FCData");
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.readFromNBT(wrapper);
                fcEntity.readEntityFromNBT(wrapper);
            } catch (Throwable e) {
                LOGGER.warn("Failed to read FC entity NBT data: {}", e.getMessage());
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

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
