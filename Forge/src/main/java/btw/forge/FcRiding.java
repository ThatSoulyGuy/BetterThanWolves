package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Triage CF2-MX3, STUB_CATALOGUE (bl): 1.5.2 World.updateEntities / updateEntityWithOptionalForce (vanilla/server
 * World.java:1721-1730, :1910-1918, :1972-1982) for a puppet. A rider stays on only while its mount is live and still
 * carries it, and runs updateRidden(), so its mount places it (vanilla/client Entity.java:1786-1868); a mount forgets a
 * rider that died, left, or rides something else. The proxies used to call onUpdate() for everything, so no FC rider
 * followed its mount (a squid head crab hung where it latched and kept biting; an FC spider jockey's skeleton walked
 * away), and an animal stayed "head-crabbed" after its squid died. FC mounts are FC-only: nothing is mirrored as a
 * 1.20.1 passenger. Riders tick in proxy order, so a rider can lag its mount by one tick (1.5.2 updated it right after).
 */
final class FcRiding {

    private FcRiding() {}

    /** The puppet's FC update, inside FCEnvHurtGuard.runOnUpdate's UPDATING window. Server thread. */
    static void update(btw.modern.EntityLiving fc, net.minecraft.world.entity.Entity proxy) {
        Level level = proxy.level();
        btw.modern.Entity mount = fc.ridingEntity;
        if (mount != null && (mount.riddenByEntity != fc || !isLive(mount, level))) {
            if (mount.riddenByEntity == fc) mount.riddenByEntity = null;
            fc.ridingEntity = null;
            mount = null;
        }
        if (mount != null) {
            if (mount instanceof PlayerBridge pb) leaveVehicleUnderHeadCrab(pb);
            fc.updateRidden();
        } else {
            fc.onUpdate();
        }
        btw.modern.Entity rider = fc.riddenByEntity;
        if (rider != null && (rider.ridingEntity != fc || !isLive(rider, level))) {
            if (rider.ridingEntity == fc) rider.ridingEntity = null;
            fc.riddenByEntity = null;
        }
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): head_crab: 4.B FCEntitySquid.UpdateHeadCrab (Common FCEntitySquid.java:954-964) knocks the
     * player it rides out of "boats, minecarts, or whatever else he might be riding" through the player's ridingEntity,
     * which the bridge never sets (the vehicle is 1.20.1's; PlayerBridge.canPlayerEdit). The same rule on the real
     * vehicle, under 4.B's condition: a squid on the player's head (Entity.HasHeadCrabbedSquid).
     */
    private static void leaveVehicleUnderHeadCrab(PlayerBridge pb) {
        ServerPlayer sp = pb.getServerPlayer();
        if (sp != null && sp.isPassenger() && pb.HasHeadCrabbedSquid()) {
            sp.stopRiding();
        }
    }

    /** True while {@code e} is in {@code level}: a player's bridge whose player is, or an FC entity whose puppet is. */
    static boolean isLive(btw.modern.Entity e, Level level) {
        if (e == null || e.isDead) return false;
        if (e instanceof PlayerBridge pb) {
            ServerPlayer sp = pb.getServerPlayer();
            return sp != null && !sp.isRemoved() && sp.level() == level;
        }
        return level instanceof ServerLevel sl && WorldBridge.getOrCreate(sl).livePuppetOf(e) != null;
    }
}
