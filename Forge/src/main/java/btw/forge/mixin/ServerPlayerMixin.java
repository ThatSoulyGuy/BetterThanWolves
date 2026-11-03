package btw.forge.mixin;

import btw.forge.BTWNetwork;
import btw.forge.HardcoreRespawn;
import btw.forge.PlayerBridge;
import btw.forge.ProxyMob;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into {@link ServerPlayer} to drive FC player systems each tick.
 *
 * The mixin is a HOOK, not logic. It calls into {@link PlayerBridge}
 * which holds the real FC state and methods (penalty levels, gloom,
 * mining speed modifiers, etc.).
 *
 * <p>No hook here may throw: tick, NBT read/write and restoreFrom all sit on vanilla paths
 * with no BTW-aware error handling (a throw from the FC player NBT read makes the singleplayer
 * owner's world crash on every open; one from the write can abort the stop save before
 * session.lock is released -- triage reentry M2/RE1).</p>
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Unique
    private static final org.apache.logging.log4j.Logger BTW$LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("BTW-PlayerHooks");

    /** "hook:player" keys already logged -- each failure is reported once per player per hook. */
    @Unique
    private static final java.util.Set<String> BTW$FAILURES_LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * FC tick failures seen so far, keyed by exception + throw site (triage HAI-10): the first
     * failure at a site logs one ERROR with its stack trace; repeats are only counted, with a
     * one-line ERROR at 10, 100, 1000, ... occurrences. Replaces a 10 s time limiter that let one
     * per-tick failure hide every other site and reported a stack every 10 s.
     */
    @Unique
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> BTW$TICK_FAILURES =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Unique
    private static String btw$nameOf(ServerPlayer player) {
        try {
            return player.getGameProfile().getName();
        } catch (Throwable ignored) {
            return "<unknown>";
        }
    }

    @Unique
    private static void btw$logTickFailure(ServerPlayer player, Throwable t) {
        // First stack frame outside the JDK, so FC failures surfacing through e.g. ArrayList get distinct sites.
        String site = "<no stack>";
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            if (!c.startsWith("java.") && !c.startsWith("jdk.") && !c.startsWith("sun.")) {
                site = c + "." + e.getMethodName() + ":" + e.getLineNumber();
                break;
            }
        }
        String key = t.getClass().getName() + '|' + site;
        java.util.concurrent.atomic.AtomicLong count = BTW$TICK_FAILURES.get(key);
        if (count == null) {
            java.util.concurrent.atomic.AtomicLong fresh = new java.util.concurrent.atomic.AtomicLong();
            count = BTW$TICK_FAILURES.putIfAbsent(key, fresh);
            if (count == null) {
                count = fresh;
            }
        }
        long n = count.incrementAndGet();
        long p = 10;
        while (p < n) {
            p *= 10;
        }
        if (n == 1) {
            BTW$LOGGER.error("FC player tick failed for {} at {} (stack trace logged once per throw site): {}",
                    btw$nameOf(player), site, t.toString(), t);
        } else if (p == n) {
            BTW$LOGGER.error("FC player tick failed at {} {} times so far (last for {}: {})",
                    site, n, btw$nameOf(player), t.toString());
        }
    }

    @Unique
    private static void btw$logFailureOnce(String hook, ServerPlayer player, Throwable t) {
        String name = btw$nameOf(player);
        if (BTW$FAILURES_LOGGED.add(hook + ":" + name)) {
            BTW$LOGGER.error("FC player {} failed for {} (logged once per player; the vanilla {} continues without FC data): {}",
                    hook, name, hook, t.toString(), t);
        }
    }

    /**
     * End-of-tick hook: sync state, run FC per-tick updates, sync back.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void btw$tick(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        try {
            btw$tickFc(self);
        } catch (Throwable t) {
            btw$logTickFailure(self, t);
        }
    }

    @Unique
    private static void btw$tickFc(ServerPlayer self) {
        PlayerBridge pb = PlayerBridge.getOrCreate(self);

        // Sync real MC state -> FC layer
        pb.syncFromReal();

        // FC per-tick updates
        pb.m_iTimesCraftedThisTick = 0;
        pb.m_iTicksSinceEmoteSound++;

        // 1.5.2 EntityPlayer.onUpdate (vanilla/server EntityPlayer.java:243-246) —
        // XP-orb absorption cooldown; the frozen EntityXPOrb.onCollideWithPlayer
        // sets it to 2 and skips absorption while it is non-zero.
        if (pb.xpCooldown > 0) {
            --pb.xpCooldown;
        }

        pb.UpdateModStatusVariables();

        // 1.5.2 EntityPlayerMP.ModSpecificOnUpdate (vanilla/server EntityPlayerMP.java:1081-1085) —
        // passive hunger drain + starvation/low-health status effects; runs after
        // UpdateModStatusVariables, matching vanilla's end-of-onUpdate ordering.
        pb.UpdateExhaustionWithTime();
        pb.UpdateHealthAndHungerEffects();

        // Send penalty levels to the client for HUD rendering.
        // This replaces FC's DataWatcher-based sync (IDs 22-31) with a
        // dedicated network packet. Runs every tick after penalty levels
        // are recalculated by UpdateModStatusVariables().
        BTWNetwork.sendPenaltySync(self, pb);

        // Sync FC state -> real MC
        pb.syncToReal();
    }

    /**
     * Load FC custom data from NBT on player load. Never throws: for the singleplayer owner this
     * runs inside PlayerList.load -> Entity.load with no try (PlayerList.java:315-328), so a throw
     * would crash the world on every open and the bad tag would never be rewritten.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void btw$readNBT(CompoundTag tag, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        try {
            PlayerBridge pb = PlayerBridge.getOrCreate(self);

            btw.forge.ForgeNBTCompound fcTag = new btw.forge.ForgeNBTCompound(tag);
            pb.ReadModDataFromNBT(fcTag);
            // FCMOD-INTEGRATION(1.20.1): hardcore_respawn: FC's beacon bind marker beside 1.20.1's own respawn point
            HardcoreRespawn.readBind(tag, pb);

            // Also load FC food data
            if (tag.contains("foodLevel")) {
                pb.foodStats.readNBT(fcTag);
            }
        } catch (Throwable t) {
            btw$logFailureOnce("NBT read", self, t);
        }
    }

    /**
     * Save FC custom data to NBT on player save. Never throws: IntegratedPlayerList.saveWithoutId
     * runs outside any try (IntegratedPlayerList.java:26), and a throw during the stop save aborts
     * MinecraftServer.stopServer before session.lock is released (MinecraftServer.java:557, :600-604).
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void btw$writeNBT(CompoundTag tag, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        try {
            PlayerBridge pb = PlayerBridge.getOrCreate(self);

            btw.forge.ForgeNBTCompound fcTag = new btw.forge.ForgeNBTCompound(tag);
            pb.WriteModDataToNBT(fcTag);
            HardcoreRespawn.writeBind(tag, pb);

            // Also save FC food data
            pb.foodStats.writeNBT(fcTag);
        } catch (Throwable t) {
            btw$logFailureOnce("NBT write", self, t);
        }
    }

    /**
     * Clone FC data when player respawns or transfers dimensions.
     *
     * <p>1.20.1 PlayerList.respawn (PlayerList.java:440-443) builds a NEW ServerPlayer, calls this,
     * then setId(old id). Bridges are bound to the player OBJECT (PlayerBridge.Holder), so
     * {@code self} gets its own bridge here and keeps it after setId -- the old id-keyed cache
     * returned the dead player's bridge for the rest of the session (triage HAI-1 / reentry M1,
     * incl. the End-exit credits respawn(player, true) variant).</p>
     *
     * <p>The copies below are the behaviour for a restoreFrom outside PlayerList.respawn (a mod's own respawn). Inside
     * PlayerList.respawn, HardcoreRespawn.onRestore then gives the new player 4.B's fresh-player state (no hardcore home,
     * time 0, bind cooldown 0, dimension-switch time 0; EntityPlayer.clonePlayer copies none of them) and places it
     * (INTEGRATIONS #054).</p>
     */
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void btw$restoreFrom(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        try {
            PlayerBridge oldBridge = PlayerBridge.getOrCreate(oldPlayer);
            PlayerBridge newBridge = PlayerBridge.getOrCreate(self);

            // Always copy persistent spawn data (survives death)
            newBridge.m_HardcoreSpawnChunk = oldBridge.m_HardcoreSpawnChunk;
            newBridge.m_iSpawnDimension = oldBridge.m_iSpawnDimension;
            newBridge.m_lTimeOfLastSpawnAssignment = oldBridge.m_lTimeOfLastSpawnAssignment;
            newBridge.m_lTimeOfLastDimensionSwitch = oldBridge.m_lTimeOfLastDimensionSwitch;
            newBridge.m_lRespawnAssignmentCooldownTimer = oldBridge.m_lRespawnAssignmentCooldownTimer;
            newBridge.fcBindPos = oldBridge.fcBindPos;
            newBridge.fcBindDim = oldBridge.fcBindDim;

            if (keepEverything) {
                // Dimension transfer: preserve ALL state
                newBridge.m_iHungerPenaltyLevel = oldBridge.m_iHungerPenaltyLevel;
                newBridge.m_iFatPenaltyLevel = oldBridge.m_iFatPenaltyLevel;
                newBridge.m_iHealthPenaltyLevel = oldBridge.m_iHealthPenaltyLevel;
                newBridge.m_iGloomLevel = oldBridge.m_iGloomLevel;
                newBridge.m_iInGloomCounter = oldBridge.m_iInGloomCounter;
                newBridge.m_fCurrentMiningSpeedModifier = oldBridge.m_fCurrentMiningSpeedModifier;
                newBridge.foodStats.setFoodLevel(oldBridge.foodStats.getFoodLevel());
                newBridge.foodStats.setFoodSaturationLevel(oldBridge.foodStats.getSaturationLevel());
            }
            // Death respawn: penalty levels stay at defaults (0),
            // food stats stay at defaults (60/0), mining speed stays at 1.0.
            // UpdateModStatusVariables() on the next tick will recompute
            // everything from the fresh health/food state.

            // The old player was removed by PlayerList.respawn (removePlayerImmediately). 1.5.2 marks
            // the replaced EntityPlayerMP dead (vanilla/server ServerConfigurationManager.java:1095
            // removePlayerEntityDangerously -> setDead, World.java:1422-1424), so FC AI holding it
            // as a target drops it at once (EntityLiving.isEntityAlive) instead of chasing a removed
            // player frozen at its last position (End exit keeps health > 0). Field only:
            // EntityPlayer.setDead() would also close the old FC containers. Normally already set by
            // PlayerMixin.btw$markBridgeDead on respawn's DISCARDED removal; kept as a fallback.
            if (oldPlayer.isRemoved()) {
                oldBridge.isDead = true;
            }
        } catch (Throwable t) {
            btw$logFailureOnce("restoreFrom", self, t);
        }
        // FCMOD-INTEGRATION(1.20.1): hardcore_respawn: 4.B's fresh player and placement, before the respawn packet (PlayerList.java:476-477)
        try {
            HardcoreRespawn.onRestore(self, oldPlayer, keepEverything);
        } catch (Throwable t) {
            btw$logFailureOnce("hardcore respawn", self, t);
        }
    }

    // =========================================================================
    // Beds: FC hostiles prevent rest (beds_sleep_no_spawn)
    // =========================================================================

    /**
     * 1.20.1's NOT_SAFE test counts {@code Monster} entities within +/-8, +/-5, +/-8 of the bed (ServerPlayer.java:857-867),
     * but every FC hostile is a {@link ProxyMob}, which extends Mob, not Monster (ProxyMob.java:24), so a player could
     * sleep beside an FC zombie. 1.5.2 refused sleep for any EntityMob in the same box (vanilla/server
     * EntityPlayer.java:1410-1414); 4.B removed sleeping, so the gap shows only now that beds sleep (ruling 3). The
     * class rule is the one Modern-Common keeps: zombie, creeper, spider, silverfish and pig zombie are EntityMob and
     * count; slime and magma cube (EntityLiving) and ghast (EntityFlying) do not.
     *
     * <p>Injected just before the super call (:869), after the time check (:854) and after 1.20.1's own Monster check,
     * so the refusal messages keep vanilla's order; creative players are exempt, as at :857. A PlayerSleepInBedEvent
     * listener was rejected: it fires at :842, before the range, obstruction and time checks, so a daytime click would
     * say "not_safe". Never throws (class rule above): a failure is logged once and the player sleeps as in 1.20.1.
     * ProxyMob.getFcEntity syncs the FC mob, on a sleep attempt only.</p>
     */
    // FCMOD-INTEGRATION(1.20.1): beds_sleep_no_spawn: FC hostiles are ProxyMob (Mob), never Monster (ServerPlayer.java:857-867); 1.5.2 counted EntityMob in the same box (vanilla/server EntityPlayer.java:1410-1414)
    @Inject(method = "startSleepInBed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;startSleepInBed(Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;"),
            cancellable = true, require = 1)
    private void btw$fcHostilesPreventRest(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.isCreative()) return;                                             // as :857
        try {
            Vec3 c = Vec3.atBottomCenterOf(pos);                                    // :860
            AABB box = new AABB(c.x - 8.0D, c.y - 5.0D, c.z - 8.0D, c.x + 8.0D, c.y + 5.0D, c.z + 8.0D); // :861
            if (!self.level().getEntitiesOfClass(ProxyMob.class, box,
                    m -> m.isAlive() && m.getFcEntity() instanceof btw.modern.EntityMob).isEmpty()) {  // ProxyMob.java:109
                cir.setReturnValue(Either.left(Player.BedSleepingProblem.NOT_SAFE));
            }
        } catch (Throwable t) {
            btw$logFailureOnce("sleep rest check", self, t);
        }
    }
}
