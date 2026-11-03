package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The beacon half of fc_beacon: FC's tick, the level 1.20.1 renders, its sounds, and 4.B's looting on 1.20.1 mobs. */
// FCMOD-INTEGRATION(1.20.1): fc_beacon: FC's beacon tick, the level 1.20.1 renders, and 4.B's looting on 1.20.1 mobs
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class FcBeacons {
    private FcBeacons() {}

    private static final Logger LOGGER = LogManager.getLogger("BTW-Beacon");
    static final int FC_BEACON_ID = 138;

    // ------------------------------------------------------------------
    // Mixin entry points (BeaconBlockEntityMixin). Public: merged into BeaconBlockEntity, outside this package.
    // ------------------------------------------------------------------

    /** FC's updateEntity, both sides (1.5.2 ticked every tile entity on both); the 1.20.1 body then runs its beam scan. */
    public static void tick(Level level, BlockPos pos, BeaconBlockEntity be) {
        if (!(be instanceof FcBackedBlockEntity backed)) return;
        btw.modern.TileEntity te = backed.btw$fcTile();
        if (!(te instanceof btw.modern.TileEntityBeacon fb)) return;
        long before = signature(fb);
        try {
            te.xCoord = pos.getX();
            te.yCoord = pos.getY();
            te.zCoord = pos.getZ();                         // as ProxyBlockEntity.tick
            te.updateEntity();
        } catch (Throwable t) {
            FCPuppetSupport.logFailureOnce(LOGGER, "FC beacon tick", te, pos, t);   // one WARN per site, both sides
        }
        // the chunk must save FC's new power: RESPAWN validates a bound beacon from the loaded state
        if (!level.isClientSide && signature(fb) != before) be.setChanged();
    }

    /** FC's level for 1.20.1's updateBase, or -1 to keep 1.20.1's (not an FC beacon, or no FC tile entity on this
     *  side). FC pairs IsOn with levels > 0 in every SetPowerState (Common FCTileEntityBeacon.java:259, :227, :263), but
     *  IsOn is never saved (1.5.2 TileEntityBeacon.java:225-242): keyed on the saved level, the beam is back at the first
     *  1.20.1 cadence after a chunk load or a join, as for a 1.20.1 beacon. */
    public static int mirroredLevels(Level level, int x, int y, int z) {
        BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
        if (!(be instanceof FcBackedBlockEntity backed) || !FcBackedBlockEntities.isBacked(be)) return -1;
        if (!(backed.btw$fcTile() instanceof btw.modern.TileEntityBeacon fb)) return -1;
        return fb.getLevels() > 0 ? Math.min(fb.getLevels(), 4) : 0;
    }

    public static boolean isFcBeacon(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && FcBackedBlockEntities.isBacked(be);
    }

    /** 1.20.1's activate and deactivate sounds mark a power edge FC already sounds (the wither spawn and death, Common
     *  FCTileEntityBeacon.java:283-310; Server FCBlockBeacon.java:49-57), so they are dropped for an FC beacon; the
     *  ambient hum belongs to the 1.20.1 beam this port keeps, and plays. */
    public static boolean silences(BlockEntity be, SoundEvent sound) {
        return (sound == SoundEvents.BEACON_ACTIVATE || sound == SoundEvents.BEACON_DEACTIVATE)
                && FcBackedBlockEntities.isBacked(be);
    }

    // ------------------------------------------------------------------

    private static long signature(btw.modern.TileEntityBeacon b) {
        return ((long) b.getPrimaryEffect() << 32) ^ ((long) b.getLevels() << 1) ^ (b.IsOn() ? 1L : 0L);
    }

    /** 4.B EntityLiving.onDeath (vanilla/server EntityLiving.java:3347-3357): max(ambient looting at the mob, the killer
     *  player's looting incl. the Looting potion), for mobs whose drops 1.20.1's loot tables roll. */
    @SubscribeEvent
    public static void onLootingLevel(LootingLevelEvent e) {
        try {
            LivingEntity target = e.getEntity();
            if (!(target.level() instanceof ServerLevel sl) || !BTWLifecycle.isFcReady()) return;
            int ambient = WorldBridge.getOrCreate(sl).GetAmbientLootingEffectAtLocation(
                    Mth.floor(target.getX()), Mth.floor(target.getY()), Mth.floor(target.getZ()));
            int potion = 0;
            if (e.getDamageSource() != null && e.getDamageSource().getEntity() instanceof Player p) {
                MobEffect looting = BTWMobEffects.LOOTING.isPresent() ? BTWMobEffects.LOOTING.get() : null;
                MobEffectInstance inst = looting == null ? null : p.getEffect(looting);
                if (inst != null) potion = inst.getAmplifier() + 1;
            }
            int fc = Math.max(ambient, potion);
            if (fc > e.getLootingLevel()) e.setLootingLevel(fc);
        } catch (Throwable t) {
            FCPuppetSupport.logFailureOnce(LOGGER, "beacon looting", e.getEntity(), t);   // the 1.20.1 level stands
        }
    }
}
