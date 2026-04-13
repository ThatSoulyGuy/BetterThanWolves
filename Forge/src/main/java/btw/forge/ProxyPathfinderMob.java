package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
        }
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
    // Lifecycle no-ops — FC owns AI, physics, and rotation
    // ------------------------------------------------------------------

    @Override protected void customServerAiStep() {}
    @Override public void travel(net.minecraft.world.phys.Vec3 travelVector) {}
    @Override protected float tickHeadTurn(float yRot, float animStep) { return animStep; }
    @Override public void push(double x, double y, double z) {}
    @Override public boolean isPushable() { return false; }
    @Override protected void pushEntities() {}

    @Override
    public void knockback(double strength, double x, double z) {
        super.knockback(strength, x, z);
        pendingKnockback = true;
    }

    // ------------------------------------------------------------------
    // Tick — mirrors ProxyMob.tick exactly
    // ------------------------------------------------------------------

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
            fcEntity.inWater = isInWater();
            fcEntity.fire = getRemainingFireTicks();

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

            try {
                fcEntity.onUpdate();
            } catch (Throwable e) {
                if (tickCount % 100 == 0) {
                    LOGGER.warn("FC entity {} onUpdate() threw:", fcEntity.getClass().getSimpleName(), e);
                }
            }

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
            if (fcEntity.isDead) discard();
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
            if (fcEntity.interact(pb)) {
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    // ------------------------------------------------------------------
    // Damage
    // ------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && fcEntity != null) {
            fcEntity.health = (int) Math.max(0, getHealth());
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        if (fcEntity != null) {
            btw.modern.DamageSource fcSource = ProxyMob.translateDamageSource(source);
            fcEntity.onDeath(fcSource);
            suppressVanillaLoot = true;
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
            if (fcEntity == null && !fcClassName.isEmpty()) ensureFcEntity();
        }
        if (fcEntity != null && tag.contains("FCData")) {
            CompoundTag fcCompound = tag.getCompound("FCData");
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.readFromNBT(wrapper);
                fcEntity.readEntityFromNBT(wrapper);
            } catch (Exception e) {
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
            } catch (Exception e) {
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
