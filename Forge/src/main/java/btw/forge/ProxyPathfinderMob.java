package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge-side proxy for FC entities rooted at EntityCreature that are not
 * hostile mobs or animals — e.g. EntityVillager, EntityGolem subclasses,
 * EntityWaterMob subclasses, FCEntityWolfDire.
 *
 * Architecture mirrors ProxyMob — see that class for full rationale.
 */
public class ProxyPathfinderMob extends PathfinderMob
        implements net.minecraftforge.entity.IEntityAdditionalSpawnData {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyPathfinderMob");

    private btw.modern.EntityLiving fcEntity;
    private String fcClassName = "";
    private boolean pendingKnockback = false;
    private double lastGoodPosX, lastGoodPosY, lastGoodPosZ;
    /** D6: MC/FC health agreement (server). */
    private final FCPuppetSupport.HealthLink healthLink = new FCPuppetSupport.HealthLink();
    /** R8: client animation targets fed by BTWNetwork.FCAnimSync. */
    private final FCPuppetSupport.ClientAnim clientAnim = new FCPuppetSupport.ClientAnim();
    private final FCPuppetSupport.AnimSender animSender = new FCPuppetSupport.AnimSender();
    /** RE7: set while readAdditionalSaveData runs, so ensureFcEntity keeps the loaded Health. */
    private boolean loadingSaveData = false;
    /** W7: category of the replaced vanilla mob, or null — see ProxyMob.replacedCategory. */
    private MobCategory replacedCategory;

    public ProxyPathfinderMob(EntityType<? extends PathfinderMob> type, Level level) {
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
            // FC-spawned: FC's max/current health are authoritative — see ProxyMob.setFcEntity.
            FCPuppetSupport.seedHealthFromFc(this, fc, true);
            healthLink.mark(this, fc);
        }
    }

    FCPuppetSupport.ClientAnim getClientAnim() {
        return clientAnim;
    }

    public btw.modern.EntityLiving getFcEntity() {
        if (fcEntity == null) ensureFcEntity();
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
     * W7: natural-spawn caps count a replacement in the replaced mob's category — a glow squid made into
     * fc_squid (registered WATER_CREATURE) stays UNDERGROUND_WATER_CREATURE. See ProxyMob.getClassification.
     */
    @Override
    public MobCategory getClassification(boolean forSpawnCount) {
        if (forSpawnCount && replacedCategory != null) return replacedCategory;
        return super.getClassification(forSpawnCount);
    }

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
                fcEntity.setLocationAndAngles(getX(), getY(), getZ(), getYRot(), getXRot());
                lastGoodPosX = getX();
                lastGoodPosY = getY();
                lastGoodPosZ = getZ();
                fcEntity.entityId = getId();
                int fcMaxHp = fcEntity.getMaxHealth();
                if (fcMaxHp > 0) {
                    // RE7: keep the Attributes and Health restored by an NBT load; MAX_HEALTH is
                    // re-seeded after FC's NBT is read — see ProxyMob.ensureFcEntity.
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
    // Lifecycle no-ops — FC owns AI, physics, and rotation
    // ------------------------------------------------------------------

    @Override protected void customServerAiStep() {}
    @Override public void travel(net.minecraft.world.phys.Vec3 travelVector) {}
    // FC owns collision/suffocation; MC's isInWall() check is spurious here (see ProxyMob.isInWall).
    @Override public boolean isInWall() { return false; }
    @Override protected float tickHeadTurn(float yRot, float animStep) { return animStep; }
    @Override public void push(double x, double y, double z) {}
    @Override public boolean isPushable() { return false; }
    @Override protected void pushEntities() {}

    @Override
    public void knockback(double strength, double x, double z) {
        super.knockback(strength, x, z);
        pendingKnockback = true;
    }

    /**
     * D1: MC must not drown a puppet — fc_squid (FC EntityWaterMob, vanilla/server
     * EntityWaterMob.java:23-26 canBreatheUnderwater) drowned in water on the MC side because
     * PathfinderMob is not a WaterAnimal. FC's own air logic (EntityWaterMob :63-86 for squids,
     * EntityLiving.onEntityUpdate:618-647 for the rest) stays the only one; its damage reaches
     * MC health through the health link. See ProxyMob.canBreatheUnderwater.
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean canBreatheUnderwater() { return true; }

    /** D8: FC's setOnFireFromLava is the only lava damage/ignition — see ProxyMob.lavaHurt. */
    @Override
    public void lavaHurt() {
        if (fcEntity == null) super.lavaHurt();
    }

    /**
     * FC villagers, golems (incl. fc_iron_golem), snowmen and water mobs: MC's Mob.checkDespawn
     * (MISC/WATER_CREATURE despawn distance) must not discard what FC's despawnEntity keeps
     * (1.5.2 EntityVillager/EntityGolem.canDespawn() == false) — see ProxyMob.removeWhenFarAway.
     */
    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        if (fcEntity != null && !FCPuppetSupport.fcCanDespawn(fcEntity)) return false;
        return super.removeWhenFarAway(distanceSq);
    }

    /** D-M1: FC plays its own hurt/death sounds (FCEntitySquid's are null) — see ProxyMob. */
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return null; }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return null; }

    /** S8: FC's handleWaterMovement owns the water-entry splash sound and particles — see ProxyMob.doWaterSplashEffect. */
    @Override
    protected void doWaterSplashEffect() {
        if (fcEntity == null) {
            super.doWaterSplashEffect();
            return;
        }
        if (!level().isClientSide) gameEvent(net.minecraft.world.level.gameevent.GameEvent.SPLASH);
    }

    /** FC's onDeathUpdate owns the death sequence and XP — see ProxyMob.tickDeath. */
    @Override
    protected void tickDeath() {
        if (!level().isClientSide && fcEntity != null && !fcEntity.isDead && deathTime < 40) {
            ++deathTime;
            return;
        }
        super.tickDeath();
    }

    /** R8: hand FC-relevant entity events to the client FC entity — see ProxyMob. */
    @Override
    public void handleEntityEvent(byte id) {
        if (level().isClientSide && fcEntity != null) {
            FCPuppetSupport.forwardEntityEvent(fcEntity, id);
        }
        super.handleEntityEvent(id);
    }

    /** R8: 1.20.1's damage event is 1.5.2's entity event 2. */
    @Override
    public void handleDamageEvent(DamageSource source) {
        super.handleDamageEvent(source);
        if (level().isClientSide && fcEntity != null) {
            FCPuppetSupport.forwardEntityEvent(fcEntity, (byte) 2);
        }
    }

    /** D6: see {@link FCPuppetSupport.HealthLink} and ProxyMob.reconcileHealth. Never throws. */
    private void reconcileHealth() {
        if (fcEntity == null) return;
        try {
            healthLink.reconcile(this, fcEntity, amount -> {
                // MC hurt() invulnerability bookkeeping — see ProxyMob.reconcileHealth.
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

    // ------------------------------------------------------------------
    // Tick — mirrors ProxyMob.tick exactly
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        if (fcEntity == null) ensureFcEntity();

        if (level().isClientSide) {
            // R8: render-only FC animation tick after MC's interpolation — see ProxyMob.tick.
            super.tick();
            if (fcEntity != null) {
                FCPuppetSupport.clientAnimTick(this, fcEntity, clientAnim);
            }
            return;
        }

        if (fcEntity != null) {
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
            // S8: inWater keeps FC's own previous-tick value so FC's splash fires — see ProxyMob.tick.
            // (Squid swimming is unaffected: EntitySquid.isInWater re-checks the world itself,
            // vanilla/server EntitySquid.java:107-110.)
            // Two-way fire sync — only raise on external MC ignition; FC's
            // onEntityUpdate decrements. See ProxyMob.syncToFc for rationale.
            int mcFire = getRemainingFireTicks();
            if (mcFire > fcEntity.fire) {
                fcEntity.fire = mcFire;
            }

            // Pre-tick sanitization — see ProxyMob for rationale
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

            reconcileHealth();
            FCPuppetSupport.dropProtectedPlayerTargets(fcEntity);
            try {
                FCEnvHurtGuard.runOnUpdate(fcEntity, this);
                PossessionDiagnostics.poll(this, fcEntity);
            } catch (Throwable e) {
                // HAI-10: one stack trace per distinct throw site, not a 1-in-100-ticks sample.
                FCPuppetSupport.logUpdateFailureOnce(LOGGER, fcEntity, e);
            }
            reconcileHealth();

            // Post-tick NaN guard
            if (!Double.isFinite(fcEntity.posX)) fcEntity.posX = snapPosX;
            if (!Double.isFinite(fcEntity.posY)) fcEntity.posY = snapPosY;
            if (!Double.isFinite(fcEntity.posZ)) fcEntity.posZ = snapPosZ;
            if (!Double.isFinite(fcEntity.motionX)) fcEntity.motionX = 0.0;
            if (!Double.isFinite(fcEntity.motionY)) fcEntity.motionY = 0.0;
            if (!Double.isFinite(fcEntity.motionZ)) fcEntity.motionZ = 0.0;
            if (!Float.isFinite(fcEntity.rotationYaw)) fcEntity.rotationYaw = snapYaw;
            if (!Float.isFinite(fcEntity.rotationPitch)) fcEntity.rotationPitch = snapPitch;
            if (fcEntity.boundingBox != null
                    && (!Double.isFinite(fcEntity.boundingBox.minX)
                        || !Double.isFinite(fcEntity.boundingBox.minZ))) {
                fcEntity.setPosition(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
            }

            // Sync FC → MC
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
            if (fcEntity.isDead) discard();
        }

        double savedXo = this.xo, savedYo = this.yo, savedZo = this.zo;
        // D1: FC's air logic is the only one — see ProxyMob.tick.
        if (getAirSupply() < getMaxAirSupply()) setAirSupply(getMaxAirSupply());
        super.tick();

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
        }
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        if (fcEntity != null && hand == net.minecraft.world.InteractionHand.MAIN_HAND
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            try {
                boolean handled = fcEntity.interact(pb);
                // FC edits the held stack in place (villager milk / trades): write the changed slots
                // back to MC — see ProxyMob.mobInteract.
                pb.syncInventoryToReal();
                if (handled) {
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            } catch (Throwable t) {
                // A bridge gap in FC interact (villager trading, ...) must not crash the packet handler.
                FCPuppetSupport.logFailureOnce(LOGGER, "FC interact()", fcEntity, t);
                // INV-RESYNC: a failed FC call lands nothing; the next sync dropped its edits, it would now write them
                pb.discardInventoryEdits();
            }
        }
        return super.mobInteract(player, hand);
    }

    // ------------------------------------------------------------------
    // Damage
    // ------------------------------------------------------------------

    private boolean forwardingHurtToFc = false;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // D8: FC applies burning/lava/fire-block damage itself and the health link mirrors it — see ProxyMob.hurt.
        if (fcEntity != null && !level().isClientSide && FCPuppetSupport.isFcSimulatedFireDamage(source)) {
            return false;
        }
        boolean result = super.hurt(source, amount);
        if (result && fcEntity != null) {
            // R2: credit every landed hit as 1.5.2 attackEntityFrom does — see ProxyMob.hurt.
            FCPuppetSupport.creditAttacker(fcEntity, source);
            // 1.5.2: all damage funnels through EntityLiving.attackEntityFrom,
            // so FC overrides (FCEntitySquid hit reactions, Common
            // FCEntitySquid.java:535-569) must see MC-side hits too — see
            // ProxyMob.hurt for the reentrancy/lethal-hit rationale.
            if (!forwardingHurtToFc && !isDeadOrDying() && !fcEntity.isDead) {
                forwardingHurtToFc = true;
                try {
                    fcEntity.attackEntityFrom(ProxyMob.translateDamageSource(source), (int) amount);
                } catch (Throwable t) {
                    // HAI-10: once per throw site with its stack, not a line per hit.
                    FCPuppetSupport.logFailureOnce(LOGGER, "FC attackEntityFrom()", fcEntity, t);
                } finally {
                    forwardingHurtToFc = false;
                }
            }
            // Don't resurrect an FC entity that already died internally (see ProxyMob.hurt).
            if (fcEntity.health > 0) {
                fcEntity.health = FCPuppetSupport.mcHpToFc(getHealth());
            }
            // Health-link sync point; an FC-only death is left for reconcile (see ProxyMob.hurt).
            if (!(fcEntity.health <= 0 && getHealth() > 0)) {
                healthLink.mark(this, fcEntity);
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
        // Server only; the client FC death runs via forwarded event 3 — see ProxyMob.die.
        if (fcEntity != null && !level().isClientSide) {
            try {
                // Skip if FC already died internally — avoids double onDeath/drops.
                if (fcEntity.health > 0) {
                    // 1.5.2 order: credit (R2), health 0, death sound, onDeath — see ProxyMob.die.
                    FCPuppetSupport.creditAttacker(fcEntity, source);
                    fcEntity.health = 0;
                    FCPuppetSupport.playFcDeathSound(fcEntity);
                    btw.modern.DamageSource fcSource = ProxyMob.translateDamageSource(source);
                    fcEntity.onDeath(fcSource);
                }
                suppressVanillaLoot = true;
            } catch (Throwable t) {
                // A bridge gap in FC's death path must not crash the server; vanilla loot is the fallback.
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
    // Attributes / NBT
    // ------------------------------------------------------------------

    public static AttributeSupplier.Builder createProxyPathfinderMobAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
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
                    ensureFcEntity();
                } finally {
                    loadingSaveData = false;
                }
            }
        }
        replacedCategory = ProxyMob.readSpawnCategory(tag, replacedCategory, LOGGER); // W7
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
        // MC owns puppet HP: FC's saved Health yields to MC's — see ProxyMob.
        if (fcEntity != null && !level().isClientSide) {
            // RE7: MAX_HEALTH from the restored FC state, loaded Health clamped to it — see ProxyMob.
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
        if (replacedCategory != null) tag.putString(ProxyMob.SPAWN_CATEGORY_TAG, replacedCategory.getName()); // W7
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

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
