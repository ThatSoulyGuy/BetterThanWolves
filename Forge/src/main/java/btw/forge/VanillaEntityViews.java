package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_entity_views (INTEGRATIONS #057)
 *
 * <p><b>Feature:</b> the entities 1.20.1 owns outright: minecarts of every type, boats and chest boats, paintings,
 * experience orbs, armor stands and the other 1.20.1-only mobs, item frames, primed TNT, arrows, falling blocks.</p>
 *
 * <p><b>Conflict with FC:</b> FC finds entities through its World (getEntitiesWithinAABB, the untyped
 * getEntitiesWithinAABBExcludingEntity, getEntityByID) and is handed them in its block contact callbacks
 * (onEntityCollidedWithBlock, onEntityWalking, onFallenUpon). The port answered those with BTW's own entities, players
 * and items only, and handed a generic bridge to the callbacks, so FC never saw a 1.20.1 minecart, boat, painting, orb
 * or 1.20.1-only mob as the 1.5.2 class 4.B gave it: the BTW hopper ejected beside a chest minecart instead of filling it
 * (Common FCTileEntityHopper.java:589-651), the block dispenser took no cart or boat (Client FCBlockBlockDispenser.java:
 * 263-392), the detector block and BTW's detector rails ignored them (Client FCBlockDetectorLogic.java:397-419,
 * FCBlockDetectorRail.java:97-136), the hopper never swallowed an orb (Client FCBlockHopper.java:155-168), and the
 * mining charge, bellows, platforms, falling blocks and FC mobs' pushes passed through them.</p>
 *
 * <p><b>Integration pattern:</b> B (bridge). One rule: every entity FC finds is the entity in the world, seen as the
 * 1.5.2 class 4.B gave it. {@link #of} is the one function every entry point asks, in this order: a BTW puppet's own FC
 * object; a player's {@link PlayerBridge}; an item's {@link ItemEntityBridge}; a living entity's
 * {@link LivingEntityBridge} (rulings Q-CARTS-1 (a): 1.20.1-only living entities are EntityLiving to FC); a class view
 * of a minecart ({@code btw.modern.EntityMinecart}, {@code getMinecartType} 0-6, a container cart also an
 * {@code btw.modern.IInventory} over its 1.20.1 container), a boat ({@code EntityBoat}), a painting
 * ({@code EntityPainting}) or an orb ({@code EntityXPOrb}, {@code xpValue} = value x count); else a generic view (the
 * former FC-projectile target view). Each view is cached per entity and re-synced from the entity each time FC is handed
 * it: id, position (1.5.2's {@code posY = minY + yOffset}), motion, rotation, liveness, size, bounding box and rider (the
 * first passenger's FC object). FC's writes go back to the entity: setDead discards it (a container cart's contents
 * spill, as 1.5.2 EntityMinecartContainer.setDead did), attackEntityFrom is its own hurt with the mapped damage source,
 * addVelocity / setVelocity / setPosition move it, a minecart's or boat's applyEntityCollision pushes it and the FC entity
 * by 1.5.2's paired formula (without 1.5.2's cart pick-up), and field writes (motion, position, an orb's value) are
 * flushed as deltas at the next sync and at the end of the level tick, as ItemEntityBridge does. Entry points: WorldBridge's
 * three queries and the three contact-callback wrappers (ProxyBlock, BlockMixin, BlockBehaviorMixin: {@link #forContact});
 * the spawn half is EntityProxyFactory's ({@link FcVehicles}).</p>
 *
 * <p><b>Rebalance:</b> none (4.B parity, rulings Q-CARTS-1 (a) and Q-CARTS-2 (a)). FC's every-entity effects now reach
 * 1.20.1 entities, as they reached every entity in 4.B: a side effect is that FC's wither may target an armor stand
 * (1.20.1's own wither skips them). FC projectiles keep 1.20.1's projectile filter for entities with no FC object of
 * their own (Projectile.canHitEntity: canBeHitByProjectile), so they hit exactly what they hit before.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> a view per caller (the hopper, the dispenser, the detector rail) keeps
 * one type map each and leaves every other caller blind; FC subclasses of the frozen cart and boat classes spawned as
 * BTW puppets (they would be drawn by nothing and ridden by nothing, as the old generic {@code fc_entity} puppet was).</p>
 *
 * <p>Server thread only. Views hold their entity weakly; the cache has weak keys, so a view dies with its entity.</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_entity_views: every entity FC finds is the entity in the world, seen as the 1.5.2 class 4.B gave it
public final class VanillaEntityViews {

    private static final Logger LOGGER = LogManager.getLogger("BTW-EntityViews");

    private VanillaEntityViews() {}

    /** Class and generic views by entity (weak keys, server thread). */
    private static final Map<Entity, View> VIEWS = new WeakHashMap<>();
    /** Views FC was handed since the last end-of-level-tick flush (server thread). */
    private static final List<View> TOUCHED = new ArrayList<>();
    private static long epoch;

    /**
     * FCMOD-INTEGRATION(1.20.1): fc_entity_views: the item of the last 1.20.1 boat FC killed through a view (ruling
     * Q-CARTS-3 (a)): the block dispenser files the boat it takes under its own item (spruce stays spruce, a chest boat
     * a chest boat), read once by fcext.FCBlockBlockDispenserBoatItemMixin right after FC's setDead.
     */
    private static final ThreadLocal<Integer> CONSUMED_BOAT_ITEM = new ThreadLocal<>();

    /** The parts every view shares; each view class delegates its overrides here. */
    public interface View {
        Entity real();

        Core core();
    }

    // ================================================================
    // Entry points
    // ================================================================

    /**
     * The FC object of a 1.20.1 entity for FC's queries and lookups (WorldBridge's typed, untyped and by-id queries):
     * see the class javadoc for the order. A view or a living bridge is synced first. Null when the entity has no FC
     * object to show: a puppet whose FC entity is not built, a removed or empty item, a client-side entity.
     */
    public static @Nullable btw.modern.Entity of(@Nullable Entity e) {
        if (e == null) return null;
        btw.modern.Entity puppet = puppetFc(e);
        if (puppet != null || isPuppet(e)) return puppet;
        if (e instanceof Player p) return PlayerBridge.getOrCreate(p);
        if (e instanceof ItemEntity ie) return ItemEntityBridge.wrap(ie);
        if (!(e.level() instanceof ServerLevel)) return null;
        if (e instanceof LivingEntity living) {
            LivingEntityBridge lb = LivingEntityBridge.getOrCreate(living);
            lb.syncFromReal();
            return lb;
        }
        try {
            View v = VIEWS.get(e);
            if (v == null) {
                v = create(e);
                VIEWS.put(e, v);
            }
            v.core().refresh();
            return (btw.modern.Entity) v;
        } catch (Throwable t) {
            warnOnce("VIEW", "building the FC view of " + e.getClass().getSimpleName(), t);
            return null;
        }
    }

    /**
     * The FC entity an FC block's contact callback receives for {@code e} (ProxyBlock.entityInside / stepOn / fallOn,
     * BlockMixin, BlockBehaviorMixin): {@link #of}, with a player's bridge synced first. One function for the three
     * wrappers; a puppet hands over its own FC object, as BlockBehaviorMixin always did.
     */
    public static @Nullable btw.modern.Entity forContact(@Nullable Entity e) {
        if (e instanceof Player p) {
            PlayerBridge pb = PlayerBridge.getOrCreate(p);
            pb.syncFromReal();
            return pb;
        }
        return of(e);
    }

    /**
     * True when a query for FC class {@code fcClass} may return {@code e}'s FC object, without building a view: the
     * pre-filter of WorldBridge's typed query.
     */
    public static boolean mayMatch(Entity e, Class<?> fcClass) {
        if (isPuppet(e)) {
            btw.modern.Entity fc = puppetFc(e);
            return fc != null && fcClass.isInstance(fc);
        }
        return fcClass.isAssignableFrom(fcClassOf(e));
    }

    /** True for an entity whose FC object is its own (a puppet, a player, an item), never a view. */
    public static boolean hasOwnFcObject(Entity e) {
        return isPuppet(e) || e instanceof Player || e instanceof ItemEntity;
    }

    /** The 1.20.1 entity behind a view, or null for anything else. */
    public static @Nullable Entity realOf(@Nullable btw.modern.Entity fc) {
        return fc instanceof View v ? v.real() : null;
    }

    /** True for an FC arrow in the world: a puppet whose FC object is a btw.modern.EntityArrow (ButtonBlockMixin). */
    public static boolean isFcArrow(Entity e) {
        return e instanceof ProxyEntity pe && !pe.isRemoved() && pe.getFcEntity() instanceof btw.modern.EntityArrow;
    }

    /**
     * The FC item id of the 1.20.1 boat FC last killed through a view, once (and forgotten), else {@code fallbackItemId}
     * (FCBlockBlockDispenserBoatItemMixin, ruling Q-CARTS-3 (a)).
     */
    public static int takeConsumedBoatItem(int fallbackItemId) {
        Integer id = CONSUMED_BOAT_ITEM.get();
        CONSUMED_BOAT_ITEM.remove();
        return id != null && id > 0 ? id : fallbackItemId;
    }

    /** The FC methods each view class overrides on its frozen 1.5.2 superclass (audited at server start). */
    private static final java.util.Set<String> VIEW_OVERRIDES = java.util.Set.of("setDead", "attackEntityFrom",
            "addVelocity", "setVelocity", "setPosition", "canBePushed", "canBeCollidedWith", "isEntityAlive", "isBurning",
            "setFire", "getEntityName", "onUpdate", "updateRiderPosition", "applyEntityCollision", "getMinecartType",
            "getXpValue", "entityInit", "readEntityFromNBT", "writeEntityToNBT", "isInWater", "handleLavaMovement");

    /**
     * [SELF-AUDIT/views] (BridgeSelfAudit.runServerStart): every override of every view class overrides a method of its
     * superclass as the game runs it (the frozen 1.5.2 classes, which the shadow list puts ahead of Modern-Common's
     * stubs: a signature that differs there would leave FC's call on the frozen body); the minecart class is the
     * frozen one (getMinecartType abstract); 1.20.1's ExperienceOrb.count resolves. One INFO line, a WARN per problem.
     */
    public static void audit() {
        int overrides = 0;
        int warnings = 0;
        for (Class<?> view : new Class<?>[] {GenericView.class, MinecartView.class, ContainerMinecartView.class,
                BoatView.class, PaintingView.class, XpOrbView.class}) {
            for (java.lang.reflect.Method m : view.getDeclaredMethods()) {
                if (m.isSynthetic() || !VIEW_OVERRIDES.contains(m.getName())) continue;
                overrides++;
                Class<?> declaring = null;
                try {
                    declaring = view.getSuperclass().getMethod(m.getName(), m.getParameterTypes()).getDeclaringClass();
                } catch (NoSuchMethodException ignored) {
                    // reported below
                }
                if (declaring == null || !(declaring.getName().startsWith("btw.modern.")
                        || declaring.getName().startsWith("btw.forge.VanillaEntityViews"))) {
                    LOGGER.warn("[SELF-AUDIT/views] {}.{} overrides nothing in the running {} chain: FC's calls reach"
                            + " the 1.5.2 body", view.getSimpleName(), m.getName(), view.getSuperclass().getName());
                    warnings++;
                }
            }
        }
        boolean frozenCart = java.lang.reflect.Modifier.isAbstract(btw.modern.EntityMinecart.class.getModifiers());
        if (!frozenCart) {
            LOGGER.warn("[SELF-AUDIT/views] btw.modern.EntityMinecart is not the frozen 1.5.2 class (getMinecartType is"
                    + " not abstract): FC's minecart callers run against Modern-Common's stub");
            warnings++;
        }
        boolean orbCount = orbCountField() != null;
        if (!orbCount) {
            LOGGER.warn("[SELF-AUDIT/views] ExperienceOrb.count did not resolve: FC's partial orb takes keep the merged count");
            warnings++;
        }
        LOGGER.info("[SELF-AUDIT/views] {} view overrides on the running 1.5.2 classes, frozen minecart class {},"
                + " orb count field {}, {} warnings (0 expected)", overrides, frozenCart, orbCount, warnings);
    }

    /** Drops every cached view once the server has stopped (BTWLifecycle.onServerStopped). */
    static void clearCaches() {
        VIEWS.clear();
        TOUCHED.clear();
        CONSUMED_BOAT_ITEM.remove();
    }

    // ================================================================
    // Classification
    // ================================================================

    private static boolean isPuppet(Entity e) {
        return e instanceof ProxyMob || e instanceof ProxyAnimal || e instanceof ProxyPathfinderMob
                || e instanceof ProxyEntity;
    }

    private static @Nullable btw.modern.Entity puppetFc(Entity e) {
        if (e instanceof ProxyMob pm) return pm.getFcEntity();
        if (e instanceof ProxyAnimal pa) return pa.getFcEntity();
        if (e instanceof ProxyPathfinderMob pp) return pp.getFcEntity();
        if (e instanceof ProxyEntity pe) return pe.getFcEntity();
        return null;
    }

    /** The runtime class of the FC object {@link #of} gives a non-puppet entity. */
    private static Class<?> fcClassOf(Entity e) {
        if (e instanceof Player) return PlayerBridge.class;
        if (e instanceof ItemEntity) return ItemEntityBridge.class;
        if (e instanceof LivingEntity) return LivingEntityBridge.class;
        if (e instanceof AbstractMinecartContainer) return ContainerMinecartView.class;
        if (e instanceof AbstractMinecart) return MinecartView.class;
        if (e instanceof Boat) return BoatView.class;
        if (e instanceof Painting) return PaintingView.class;
        if (e instanceof ExperienceOrb) return XpOrbView.class;
        return GenericView.class;
    }

    private static View create(Entity e) {
        if (e instanceof AbstractMinecartContainer cart) return new ContainerMinecartView(cart);
        if (e instanceof AbstractMinecart cart) return new MinecartView(cart);
        if (e instanceof Boat boat) return new BoatView(boat);
        if (e instanceof Painting painting) return new PaintingView(painting);
        if (e instanceof ExperienceOrb orb) return new XpOrbView(orb);
        return new GenericView(e);
    }

    /** 1.5.2 EntityMinecart types (vanilla/server EntityMinecart.java:189-210); 6 for 1.20.1's command block cart. */
    static int minecartType(AbstractMinecart cart) {
        AbstractMinecart.Type type = cart.getMinecartType();
        if (type == null) return 0;
        return switch (type) {
            case CHEST -> 1;
            case FURNACE -> 2;
            case TNT -> 3;
            case SPAWNER -> 4;
            case HOPPER -> 5;
            case COMMAND_BLOCK -> 6;
            default -> 0;
        };
    }

    // ================================================================
    // Shared view state
    // ================================================================

    /**
     * The entity, the snapshot FC's field writes are measured against, and the shared behaviour of every view. Built by
     * each view after its frozen superclass constructor has run, so every override null-checks it.
     */
    public static final class Core {
        private final btw.modern.Entity fc;
        private final WeakReference<Entity> ref;
        private double snapX, snapY, snapZ, snapMX, snapMY, snapMZ;
        private long touchedEpoch = -1L;
        private boolean syncing;

        Core(btw.modern.Entity fc, Entity real) {
            this.fc = fc;
            this.ref = new WeakReference<>(real);
            if (real.level() instanceof ServerLevel sl) {
                fc.worldObj = WorldBridge.getOrCreate(sl);
            }
        }

        Entity real() {
            return ref.get();
        }

        /** Lands FC's earlier field writes, then re-reads the entity (every hand-off to FC). */
        void refresh() {
            flush();
            sync();
            if (touchedEpoch != epoch) {
                touchedEpoch = epoch;
                TOUCHED.add((View) fc);
            }
        }

        /** Position, box, motion, rotation, liveness, size and rider from the entity. */
        void sync() {
            Entity r = real();
            if (r == null) {
                fc.isDead = true;
                return;
            }
            syncing = true;
            try {
                if (r.level() instanceof ServerLevel sl) {
                    fc.worldObj = WorldBridge.getOrCreate(sl);
                }
                fc.entityId = r.getId();
                AABB bb = r.getBoundingBox();
                fc.width = r.getBbWidth();
                fc.height = r.getBbHeight();
                fc.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
                fc.posX = r.getX();
                fc.posY = bb.minY + fc.yOffset;
                fc.posZ = r.getZ();
                fc.prevPosX = r.xo;
                fc.prevPosY = r.yo + fc.yOffset;
                fc.prevPosZ = r.zo;
                fc.lastTickPosX = r.xOld;
                fc.lastTickPosY = r.yOld + fc.yOffset;
                fc.lastTickPosZ = r.zOld;
                Vec3 v = r.getDeltaMovement();
                fc.motionX = v.x;
                fc.motionY = v.y;
                fc.motionZ = v.z;
                fc.rotationYaw = r.getYRot();
                fc.rotationPitch = r.getXRot();
                fc.prevRotationYaw = r.yRotO;
                fc.prevRotationPitch = r.xRotO;
                fc.onGround = r.onGround();
                fc.isDead = r.isRemoved();
                fc.ticksExisted = r.tickCount;
                fc.fallDistance = r.fallDistance;
                fc.fire = r.getRemainingFireTicks();
                // The rider is read, never paired back: the bridges and puppets keep their own mounts (FcRiding)
                Entity passenger = r.getFirstPassenger();
                fc.riddenByEntity = passenger != null ? of(passenger) : null;
                snapshot();
            } finally {
                syncing = false;
            }
        }

        private void snapshot() {
            snapX = fc.posX;
            snapY = fc.posY;
            snapZ = fc.posZ;
            snapMX = fc.motionX;
            snapMY = fc.motionY;
            snapMZ = fc.motionZ;
        }

        /** FC's field writes since the last sync land on the entity, motion as a delta on its live motion. */
        void flush() {
            Entity r = real();
            if (r == null || r.isRemoved()) return;
            try {
                if (fc.isDead) {
                    r.discard();
                    return;
                }
                if (fc.motionX != snapMX || fc.motionY != snapMY || fc.motionZ != snapMZ) {
                    r.setDeltaMovement(r.getDeltaMovement().add(fc.motionX - snapMX, fc.motionY - snapMY,
                            fc.motionZ - snapMZ));
                    r.hasImpulse = true;
                }
                if (fc.posX != snapX || fc.posY != snapY || fc.posZ != snapZ) {
                    r.setPos(fc.posX, fc.posY - fc.yOffset, fc.posZ);
                    r.hasImpulse = true;
                }
                snapshot();
                if (fc instanceof XpOrbView orb) orb.flushValue();
                if (fc instanceof ContainerMinecartView inv) inv.inventory.foldAll();
            } catch (Throwable t) {
                warnOnce("FLUSH", "landing FC's changes on " + r.getClass().getSimpleName(), t);
            }
        }

        // ---- the shared overrides

        void setDead() {
            Entity r = real();
            if (r != null && !r.isRemoved()) r.discard();
        }

        boolean attackEntityFrom(btw.modern.DamageSource source, int amount) {
            return DamageSourceMapping.hurtFromFc(real(), source, amount);
        }

        void addVelocity(double x, double y, double z) {
            Entity r = real();
            if (r != null) {
                r.push(x, y, z);
                snapMX += x;
                snapMY += y;
                snapMZ += z;
            }
        }

        void setVelocity(double x, double y, double z) {
            Entity r = real();
            if (r != null) {
                r.setDeltaMovement(x, y, z);
                r.hasImpulse = true;
                snapMX = x;
                snapMY = y;
                snapMZ = z;
            }
        }

        void setPosition(double x, double y, double z) {
            if (syncing) return;
            Entity r = real();
            if (r != null) {
                r.setPos(x, y - fc.yOffset, z);
                r.hasImpulse = true;
                snapX = x;
                snapY = y;
                snapZ = z;
            }
        }

        boolean canBePushed() {
            Entity r = real();
            return r != null && r.isPushable();
        }

        boolean canBeCollidedWith() {
            Entity r = real();
            return r != null && r.canBeHitByProjectile();
        }

        boolean isEntityAlive() {
            Entity r = real();
            return r != null && r.isAlive();
        }

        boolean isBurning() {
            Entity r = real();
            return r != null && r.isOnFire();
        }

        boolean isInWater() {
            Entity r = real();
            return r != null && r.isInWater();
        }

        boolean isInLava() {
            Entity r = real();
            return r != null && r.isInLava();
        }

        void setFire(int seconds) {
            Entity r = real();
            if (r != null) r.setSecondsOnFire(seconds);
        }

        @Nullable String name() {
            Entity r = real();
            return r != null ? r.getName().getString() : null;
        }

        /**
         * 1.5.2 Entity.applyEntityCollision (vanilla/server Entity.java:1258-1292): both entities pushed apart by the
         * paired addVelocity, the view's share through the entity, the other's to its FC object (a puppet ignores
         * 1.20.1 pushes). 1.5.2 EntityMinecart's own version also picked up mobs (EntityMinecart.java:1056-1065);
         * that is 1.20.1's to do for its carts.
         */
        void applyEntityCollision(btw.modern.Entity other) {
            if (other == null || other.riddenByEntity == fc || other.ridingEntity == fc) return;
            double dx = other.posX - fc.posX;
            double dz = other.posZ - fc.posZ;
            double d = Math.max(Math.abs(dx), Math.abs(dz));
            if (d >= 0.009999999776482582D) {
                d = Math.sqrt(d);
                dx /= d;
                dz /= d;
                double f = 1.0D / d;
                if (f > 1.0D) f = 1.0D;
                dx *= f;
                dz *= f;
                dx *= 0.05000000074505806D;
                dz *= 0.05000000074505806D;
                dx *= 1.0F - fc.entityCollisionReduction;
                dz *= 1.0F - fc.entityCollisionReduction;
                fc.addVelocity(-dx, 0.0D, -dz);
                other.addVelocity(dx, 0.0D, dz);
            }
        }
    }

    // ================================================================
    // The views (frozen 1.5.2 classes at runtime: only members both they and Modern-Common declare are used)
    // ================================================================

    /** Every other 1.20.1 entity, as a plain 1.5.2 Entity (the former FC-projectile target view). */
    public static final class GenericView extends btw.modern.Entity implements View {
        private final Core core;

        GenericView(Entity real) {
            super(null);
            this.core = new Core(this, real);
        }

        @Override public Entity real() { return core != null ? core.real() : null; }
        @Override public Core core() { return core; }
        @Override public void entityInit() {}
        @Override public void readEntityFromNBT(btw.modern.NBTTagCompound tag) {}
        @Override public void writeEntityToNBT(btw.modern.NBTTagCompound tag) {}
        @Override public void onUpdate() {}
        @Override public void setDead() { super.setDead(); if (core != null) core.setDead(); }
        @Override public boolean attackEntityFrom(btw.modern.DamageSource s, int a) { return core != null && core.attackEntityFrom(s, a); }
        @Override public void addVelocity(double x, double y, double z) { motionX += x; motionY += y; motionZ += z; if (core != null) core.addVelocity(x, y, z); }
        @Override public void setVelocity(double x, double y, double z) { motionX = x; motionY = y; motionZ = z; if (core != null) core.setVelocity(x, y, z); }
        @Override public void setPosition(double x, double y, double z) { super.setPosition(x, y, z); if (core != null) core.setPosition(x, y, z); }
        @Override public boolean canBePushed() { return core != null && core.canBePushed(); }
        @Override public boolean canBeCollidedWith() { return core != null && core.canBeCollidedWith(); }
        @Override public boolean isEntityAlive() { return core != null && core.isEntityAlive(); }
        @Override public boolean isInWater() { return core != null && core.isInWater(); }
        @Override public boolean handleLavaMovement() { return core != null && core.isInLava(); }
        @Override public boolean isBurning() { return core != null && core.isBurning(); }
        @Override public void setFire(int seconds) { if (core != null) core.setFire(seconds); }
        @Override public String getEntityName() { String n = core != null ? core.name() : null; return n != null ? n : super.getEntityName(); }
    }

    /** A 1.20.1 minecart as 1.5.2's EntityMinecart. */
    public static class MinecartView extends btw.modern.EntityMinecart implements View {
        protected final Core core;

        MinecartView(AbstractMinecart real) {
            super(null);
            this.core = new Core(this, real);
        }

        @Override public Entity real() { return core != null ? core.real() : null; }
        @Override public Core core() { return core; }

        /** 1.5.2 EntityMinecart.getMinecartType (abstract in the frozen class), from the 1.20.1 cart's type. */
        @Override
        public int getMinecartType() {
            return core != null && core.real() instanceof AbstractMinecart cart ? minecartType(cart) : 0;
        }

        @Override public void onUpdate() {}
        @Override public void updateRiderPosition() {}
        @Override public void setDead() { super.setDead(); if (core != null) core.setDead(); }
        @Override public boolean attackEntityFrom(btw.modern.DamageSource s, int a) { return core != null && core.attackEntityFrom(s, a); }
        @Override public void addVelocity(double x, double y, double z) { motionX += x; motionY += y; motionZ += z; if (core != null) core.addVelocity(x, y, z); }
        @Override public void setVelocity(double x, double y, double z) { motionX = x; motionY = y; motionZ = z; if (core != null) core.setVelocity(x, y, z); }
        @Override public void setPosition(double x, double y, double z) { super.setPosition(x, y, z); if (core != null) core.setPosition(x, y, z); }
        @Override public void applyEntityCollision(btw.modern.Entity other) { if (core != null) core.applyEntityCollision(other); }
        @Override public boolean canBePushed() { return core != null && core.canBePushed(); }
        @Override public boolean canBeCollidedWith() { return core != null && core.canBeCollidedWith(); }
        @Override public boolean isEntityAlive() { return core != null && core.isEntityAlive(); }
        @Override public boolean isInWater() { return core != null && core.isInWater(); }
        @Override public boolean handleLavaMovement() { return core != null && core.isInLava(); }
        @Override public boolean isBurning() { return core != null && core.isBurning(); }
        @Override public void setFire(int seconds) { if (core != null) core.setFire(seconds); }
        @Override public String getEntityName() { String n = core != null ? core.name() : null; return n != null ? n : "Minecart"; }
    }

    /** A 1.20.1 container minecart (chest, hopper) as 1.5.2's EntityMinecart and IInventory. */
    public static final class ContainerMinecartView extends MinecartView implements btw.modern.IInventory {
        final ContainerInventory inventory;

        ContainerMinecartView(AbstractMinecartContainer real) {
            super(real);
            this.inventory = new ContainerInventory(real);
        }

        @Override public int getSizeInventory() { return inventory.size(); }
        @Override public btw.modern.ItemStack getStackInSlot(int slot) { return inventory.get(slot); }
        @Override public btw.modern.ItemStack decrStackSize(int slot, int amount) { return inventory.decr(slot, amount); }
        @Override public btw.modern.ItemStack getStackInSlotOnClosing(int slot) { return null; }
        @Override public void setInventorySlotContents(int slot, btw.modern.ItemStack stack) { inventory.set(slot, stack); }
        @Override public String getInvName() { return getEntityName(); }
        @Override public boolean isInvNameLocalized() { return true; }
        @Override public int getInventoryStackLimit() { return inventory.stackLimit(); }
        @Override public void onInventoryChanged() { inventory.changed(); }
        @Override public boolean isUseableByPlayer(btw.modern.EntityPlayer player) { return !isDead && player != null && player.getDistanceSqToEntity(this) <= 64.0D; }
        @Override public void openChest() {}
        @Override public void closeChest() {}
        @Override public boolean isItemValidForSlot(int slot, btw.modern.ItemStack stack) { return inventory.canPlace(slot, stack); }
    }

    /** A 1.20.1 boat or chest boat as 1.5.2's EntityBoat (1.5.2 has one boat class). */
    public static final class BoatView extends btw.modern.EntityBoat implements View {
        private final Core core;

        BoatView(Boat real) {
            super(null);
            this.core = new Core(this, real);
        }

        @Override public Entity real() { return core != null ? core.real() : null; }
        @Override public Core core() { return core; }
        @Override public void onUpdate() {}
        @Override public void updateRiderPosition() {}

        /** Kills the boat; its own item is kept for the block dispenser (Q-CARTS-3 (a), {@link #takeConsumedBoatItem}). */
        @Override
        public void setDead() {
            super.setDead();
            if (core != null && core.real() instanceof Boat boat) {
                try {
                    CONSUMED_BOAT_ITEM.set(ProxyRegistry.getOrCreateLegacyId(boat.getDropItem()));
                } catch (Throwable t) {
                    warnOnce("BOAT-ITEM", "naming the item of " + boat, t);
                }
                core.setDead();
            }
        }

        @Override public boolean attackEntityFrom(btw.modern.DamageSource s, int a) { return core != null && core.attackEntityFrom(s, a); }
        @Override public void addVelocity(double x, double y, double z) { motionX += x; motionY += y; motionZ += z; if (core != null) core.addVelocity(x, y, z); }
        @Override public void setVelocity(double x, double y, double z) { motionX = x; motionY = y; motionZ = z; if (core != null) core.setVelocity(x, y, z); }
        @Override public void setPosition(double x, double y, double z) { super.setPosition(x, y, z); if (core != null) core.setPosition(x, y, z); }
        @Override public void applyEntityCollision(btw.modern.Entity other) { if (core != null) core.applyEntityCollision(other); }
        @Override public boolean canBePushed() { return core != null && core.canBePushed(); }
        @Override public boolean canBeCollidedWith() { return core != null && core.canBeCollidedWith(); }
        @Override public boolean isEntityAlive() { return core != null && core.isEntityAlive(); }
        @Override public boolean isInWater() { return core != null && core.isInWater(); }
        @Override public boolean handleLavaMovement() { return core != null && core.isInLava(); }
        @Override public boolean isBurning() { return core != null && core.isBurning(); }
        @Override public void setFire(int seconds) { if (core != null) core.setFire(seconds); }
        @Override public String getEntityName() { String n = core != null ? core.name() : null; return n != null ? n : "Boat"; }
    }

    /** A 1.20.1 painting as 1.5.2's EntityPainting (FCEntityCanvas refuses to overlap one, Common FCEntityCanvas.java:269-279). */
    public static final class PaintingView extends btw.modern.EntityPainting implements View {
        private final Core core;

        PaintingView(Painting real) {
            super(null);
            this.core = new Core(this, real);
        }

        @Override public Entity real() { return core != null ? core.real() : null; }
        @Override public Core core() { return core; }
        @Override public void onUpdate() {}
        @Override public void setDead() { super.setDead(); if (core != null) core.setDead(); }
        @Override public boolean attackEntityFrom(btw.modern.DamageSource s, int a) { return core != null && core.attackEntityFrom(s, a); }
        // 1.5.2 EntityHanging.addVelocity breaks a hanging entity that is pushed (vanilla/server EntityHanging.java:257-264);
        // 1.20.1 HangingEntity.push does the same with its drop
        @Override public void addVelocity(double x, double y, double z) { if (core != null) core.addVelocity(x, y, z); }
        @Override public boolean canBeCollidedWith() { return core != null && core.canBeCollidedWith(); }
        @Override public boolean isEntityAlive() { return core != null && core.isEntityAlive(); }
        @Override public boolean isInWater() { return core != null && core.isInWater(); }
        @Override public boolean handleLavaMovement() { return core != null && core.isInLava(); }
        @Override public boolean isBurning() { return core != null && core.isBurning(); }
        @Override public void setFire(int seconds) { if (core != null) core.setFire(seconds); }
        @Override public String getEntityName() { String n = core != null ? core.name() : null; return n != null ? n : "Painting"; }
    }

    /** A 1.20.1 experience orb as 1.5.2's EntityXPOrb: xpValue is the orb's value times its merged count. */
    public static final class XpOrbView extends btw.modern.EntityXPOrb implements View {
        private final Core core;
        private int snapXp;

        XpOrbView(ExperienceOrb real) {
            super(null);
            this.core = new Core(this, real);
            syncValue();
        }

        @Override public Entity real() { return core != null ? core.real() : null; }
        @Override public Core core() { return core; }

        private void syncValue() {
            if (core != null && core.real() instanceof ExperienceOrb orb) {
                xpValue = orb.getValue() * Math.max(1, orbCount(orb));
                snapXp = xpValue;
            }
        }

        /** FC's xpValue write (FCTileEntityHopper.AttemptToSwallowXPOrb takes part of an orb, Common :872-897). */
        void flushValue() {
            if (xpValue == snapXp || !(core.real() instanceof ExperienceOrb orb) || orb.isRemoved()) {
                syncValue();
                return;
            }
            if (xpValue <= 0) {
                orb.discard();
            } else {
                orb.value = xpValue;
                setOrbCount(orb, 1);
            }
            syncValue();
        }

        @Override public int getXpValue() { return xpValue; }
        @Override public void onUpdate() {}
        @Override public void setDead() { super.setDead(); if (core != null) core.setDead(); }
        @Override public boolean attackEntityFrom(btw.modern.DamageSource s, int a) { return core != null && core.attackEntityFrom(s, a); }
        @Override public void addVelocity(double x, double y, double z) { motionX += x; motionY += y; motionZ += z; if (core != null) core.addVelocity(x, y, z); }
        @Override public void setVelocity(double x, double y, double z) { motionX = x; motionY = y; motionZ = z; if (core != null) core.setVelocity(x, y, z); }
        @Override public void setPosition(double x, double y, double z) { super.setPosition(x, y, z); if (core != null) core.setPosition(x, y, z); }
        @Override public boolean canBeCollidedWith() { return core != null && core.canBeCollidedWith(); }
        @Override public boolean isEntityAlive() { return core != null && core.isEntityAlive(); }
        @Override public boolean isInWater() { return core != null && core.isInWater(); }
        @Override public boolean handleLavaMovement() { return core != null && core.isInLava(); }
        @Override public String getEntityName() { String n = core != null ? core.name() : null; return n != null ? n : "XPOrb"; }

        @Override
        public void readEntityFromNBT(btw.modern.NBTTagCompound tag) {}

        @Override
        public void writeEntityToNBT(btw.modern.NBTTagCompound tag) {}
    }

    // ---- ExperienceOrb.count (private in 1.20.1; SRG f_147072_)

    private static Field orbCountField;
    private static boolean orbCountResolved;

    private static Field orbCountField() {
        if (!orbCountResolved) {
            orbCountResolved = true;
            try {
                orbCountField = ObfuscationReflectionHelper.findField(ExperienceOrb.class, "f_147072_");
            } catch (Throwable t) {
                warnOnce("ORB-COUNT", "resolving ExperienceOrb.count", t);
            }
        }
        return orbCountField;
    }

    private static int orbCount(ExperienceOrb orb) {
        Field f = orbCountField();
        try {
            return f != null ? f.getInt(orb) : 1;
        } catch (Throwable t) {
            return 1;
        }
    }

    private static void setOrbCount(ExperienceOrb orb, int count) {
        Field f = orbCountField();
        try {
            if (f != null) f.setInt(orb, count);
        } catch (Throwable t) {
            warnOnce("ORB-COUNT", "writing ExperienceOrb.count", t);
        }
    }

    // ================================================================
    // A 1.20.1 container as an FC IInventory
    // ================================================================

    /**
     * A container minecart's 1.20.1 container as an FC inventory. FC edits the stacks it gets in place and then calls
     * setInventorySlotContents or onInventoryChanged (FCUtilsInventory.AddItemStackToInventory, DecrStackSize: Common
     * FCUtilsInventory.java:90-120, :444-535), so each slot hands out one FC stack while the 1.20.1 slot is unchanged,
     * and an in-place edit is folded back at the next access, onInventoryChanged and the end of the tick.
     */
    static final class ContainerInventory {
        private final WeakReference<net.minecraft.world.Container> ref;
        private btw.modern.ItemStack[] fc = new btw.modern.ItemStack[0];
        private net.minecraft.world.item.ItemStack[] mcRef = new net.minecraft.world.item.ItemStack[0];
        private int[] snapId = new int[0], snapSize = new int[0], snapDamage = new int[0];
        private int[] snapMcCount = new int[0];

        ContainerInventory(net.minecraft.world.Container container) {
            this.ref = new WeakReference<>(container);
        }

        private net.minecraft.world.Container c() {
            return ref.get();
        }

        int size() {
            net.minecraft.world.Container c = c();
            return c != null ? c.getContainerSize() : 0;
        }

        int stackLimit() {
            net.minecraft.world.Container c = c();
            return c != null ? c.getMaxStackSize() : 64;
        }

        private void ensure(int n) {
            if (fc.length < n) {
                fc = java.util.Arrays.copyOf(fc, n);
                mcRef = java.util.Arrays.copyOf(mcRef, n);
                snapId = java.util.Arrays.copyOf(snapId, n);
                snapSize = java.util.Arrays.copyOf(snapSize, n);
                snapDamage = java.util.Arrays.copyOf(snapDamage, n);
                snapMcCount = java.util.Arrays.copyOf(snapMcCount, n);
            }
        }

        btw.modern.ItemStack get(int slot) {
            net.minecraft.world.Container c = c();
            if (c == null || slot < 0 || slot >= c.getContainerSize()) return null;
            ensure(c.getContainerSize());
            fold(slot);
            net.minecraft.world.item.ItemStack mc = c.getItem(slot);
            if (mc.isEmpty()) {
                record(slot, null, mc);
                return null;
            }
            btw.modern.ItemStack held = fc[slot];
            if (held == null || mc != mcRef[slot] || mc.getCount() != snapMcCount[slot]) {
                held = ItemStackHelper.toFcStack(mc);
            }
            record(slot, held, mc);
            return held;
        }

        void set(int slot, btw.modern.ItemStack stack) {
            net.minecraft.world.Container c = c();
            if (c == null || slot < 0 || slot >= c.getContainerSize()) return;
            ensure(c.getContainerSize());
            net.minecraft.world.item.ItemStack mc = stack == null || stack.stackSize <= 0
                    ? net.minecraft.world.item.ItemStack.EMPTY : ItemStackHelper.toMcStack(stack);
            c.setItem(slot, mc);
            c.setChanged();
            record(slot, mc.isEmpty() ? null : stack, c.getItem(slot));
        }

        btw.modern.ItemStack decr(int slot, int amount) {
            btw.modern.ItemStack held = get(slot);
            if (held == null || amount <= 0) return null;
            if (held.stackSize <= amount) {
                set(slot, null);
                return held;
            }
            btw.modern.ItemStack split = held.splitStack(amount);
            fold(slot);
            return split;
        }

        boolean canPlace(int slot, btw.modern.ItemStack stack) {
            net.minecraft.world.Container c = c();
            if (c == null || stack == null) return c != null;
            return c.canPlaceItem(slot, ItemStackHelper.toMcStack(stack));
        }

        void changed() {
            foldAll();
            net.minecraft.world.Container c = c();
            if (c != null) c.setChanged();
        }

        void foldAll() {
            for (int i = 0; i < fc.length; i++) fold(i);
        }

        private void record(int slot, btw.modern.ItemStack held, net.minecraft.world.item.ItemStack mc) {
            fc[slot] = held;
            mcRef[slot] = mc;
            snapMcCount[slot] = mc.getCount();
            snapId[slot] = held != null ? held.itemID : 0;
            snapSize[slot] = held != null ? held.stackSize : 0;
            snapDamage[slot] = held != null ? held.getItemDamage() : 0;
        }

        /** An FC stack edited in place since it was handed out goes back to its slot. */
        private void fold(int slot) {
            if (slot >= fc.length) return;
            btw.modern.ItemStack held = fc[slot];
            if (held == null) return;
            if (held.itemID == snapId[slot] && held.stackSize == snapSize[slot]
                    && held.getItemDamage() == snapDamage[slot]) {
                return;
            }
            net.minecraft.world.Container c = c();
            if (c == null || slot >= c.getContainerSize()) return;
            net.minecraft.world.item.ItemStack mc = held.stackSize <= 0
                    ? net.minecraft.world.item.ItemStack.EMPTY : ItemStackHelper.toMcStack(held);
            c.setItem(slot, mc);
            c.setChanged();
            record(slot, mc.isEmpty() ? null : held, c.getItem(slot));
        }
    }

    // ================================================================
    // End-of-tick flush and logging
    // ================================================================

    /** Flushes every view FC was handed this level tick (FC's field writes outside a later sync). */
    static void flushTouched() {
        epoch++;
        if (TOUCHED.isEmpty()) return;
        View[] batch = TOUCHED.toArray(new View[0]);
        TOUCHED.clear();
        for (View v : batch) {
            Core c = v.core();
            if (c != null) c.flush();
        }
    }

    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void warnOnce(String stage, String what, Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        String site = stage + '|' + t.getClass().getName() + '@' + (frames.length > 0 ? frames[0] : "?");
        if (LOGGED.add(site)) {
            LOGGER.warn("[{}] {} threw {} (logged once per throw site)", stage, what, t.toString(), t);
        }
    }

    @Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
    public static final class TickFlush {
        private TickFlush() {}

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
                flushTouched();
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            clearCaches();
        }
    }
}
