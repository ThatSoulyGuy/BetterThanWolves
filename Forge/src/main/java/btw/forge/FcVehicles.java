package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_entity_views (INTEGRATIONS #057)
 *
 * <p>The spawn half of {@link VanillaEntityViews}: an FC object of a frozen 1.5.2 minecart or boat class is spawned as
 * that 1.20.1 entity. FC builds them in the block dispenser (Common FCItemMinecart.OnItemUsedByBlockDispenser :29-44,
 * FCItemBoat.OnItemUsedByBlockDispenser :37-54: EntityMinecart.createMinecart, new EntityBoat), and no BTW entity type
 * exists for those classes, so EntityProxyFactory made the generic {@code betterthanwolves:fc_entity} puppet, which no
 * client renderer draws and nothing can ride. Only classes without a BTW entity type are taken, so FC's own registered
 * entities stay FC's.</p>
 *
 * <p>Such a puppet saved by an earlier jar converts once its chunk has loaded: it joins as before, and at the end of the
 * server tick, while its chunk is still loaded, the 1.20.1 cart or boat takes its place (a chest cart takes the FC
 * cart's contents) and the puppet is discarded. A puppet whose chunk unloaded first stays, and converts at its next
 * load.</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_entity_views: FC's frozen minecarts and boats spawn as 1.20.1 minecarts and boats
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class FcVehicles {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Vehicles");

    private FcVehicles() {}

    /** Saved generic puppets of a frozen cart or boat class waiting for their conversion (server thread). */
    private static final Queue<ProxyEntity> PENDING = new ConcurrentLinkedQueue<>();

    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * True for an FC object EntityProxyFactory hands to {@link #create}: a frozen minecart or boat with no BTW entity
     * type of its own ({@code registeredType} null).
     */
    static boolean isVehicle(btw.modern.Entity fc, @Nullable EntityType<?> registeredType) {
        return registeredType == null
                && (fc instanceof btw.modern.EntityMinecart || fc instanceof btw.modern.EntityBoat);
    }

    /**
     * The 1.20.1 minecart or boat for an FC cart or boat: at FC's position, its feet at {@code posY - yOffset} (1.5.2
     * EntityMinecart / EntityBoat(World, x, y, z) put the centre yOffset above the y they were given), with FC's motion
     * and yaw. Null when {@code fc} is neither.
     */
    static @Nullable Entity create(btw.modern.Entity fc, Level level) {
        double feet = fc.posY - (double) fc.yOffset;
        Entity made;
        if (fc instanceof btw.modern.EntityMinecart cart) {
            made = AbstractMinecart.createMinecart(level, fc.posX, feet, fc.posZ, typeOf(cart.getMinecartType()));
        } else if (fc instanceof btw.modern.EntityBoat) {
            made = new Boat(level, fc.posX, feet, fc.posZ);
        } else {
            return null;
        }
        made.setYRot(fc.rotationYaw);
        made.setDeltaMovement(fc.motionX, fc.motionY, fc.motionZ);
        return made;
    }

    /** 1.20.1's cart type for a 1.5.2 EntityMinecart type (vanilla/server EntityMinecart.java:189-210). */
    private static AbstractMinecart.Type typeOf(int minecartType) {
        return switch (minecartType) {
            case 1 -> AbstractMinecart.Type.CHEST;
            case 2 -> AbstractMinecart.Type.FURNACE;
            case 3 -> AbstractMinecart.Type.TNT;
            case 4 -> AbstractMinecart.Type.SPAWNER;
            case 5 -> AbstractMinecart.Type.HOPPER;
            default -> AbstractMinecart.Type.RIDEABLE;
        };
    }

    // ================================================================
    // Saved puppets of an earlier jar
    // ================================================================

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ProxyEntity pe)) return;
        if (pe.getType() != BTWEntityRegistration.getGenericPlainType()) return;
        String cls = pe.getFcClassName();
        if (cls == null || !(cls.startsWith("btw.modern.EntityMinecart") || cls.equals("btw.modern.EntityBoat"))) return;
        PENDING.add(pe);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        List<ProxyEntity> batch = new ArrayList<>();
        for (ProxyEntity p; (p = PENDING.poll()) != null; ) batch.add(p);
        for (ProxyEntity pe : batch) {
            try {
                convert(pe);
            } catch (Throwable t) {
                if (LOGGED.add("convert|" + t.getClass().getName())) {
                    LOGGER.warn("Could not convert a saved BTW minecart or boat at {}: {}", pe.position(), t.toString(), t);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    private static void convert(ProxyEntity pe) {
        if (pe.isRemoved() || !(pe.level() instanceof ServerLevel level) || !level.isLoaded(BlockPos.containing(pe.position()))) {
            return;
        }
        btw.modern.Entity fc = pe.getFcEntity();
        if (fc == null) return;
        Entity made;
        if (fc instanceof btw.modern.EntityMinecart cart) {
            made = AbstractMinecart.createMinecart(level, pe.getX(), pe.getY(), pe.getZ(), typeOf(cart.getMinecartType()));
        } else if (fc instanceof btw.modern.EntityBoat) {
            made = new Boat(level, pe.getX(), pe.getY(), pe.getZ());
        } else {
            return;
        }
        made.setYRot(pe.getYRot());
        made.setDeltaMovement(pe.getDeltaMovement());
        if (!level.addFreshEntity(made)) return;
        if (fc instanceof btw.modern.IInventory inv && made instanceof net.minecraft.world.Container container) {
            int n = Math.min(inv.getSizeInventory(), container.getContainerSize());
            for (int i = 0; i < n; i++) {
                btw.modern.ItemStack stack = inv.getStackInSlot(i);
                if (stack != null && stack.stackSize > 0) container.setItem(i, ItemStackHelper.toMcStack(stack));
            }
        }
        pe.discard();
        if (LOGGED.add("converted|" + fc.getClass().getName())) {
            LOGGER.info("Converted a saved BTW {} puppet into a 1.20.1 {} (logged once per class)",
                    fc.getClass().getSimpleName(), EntityType.getKey(made.getType()));
        }
    }
}
