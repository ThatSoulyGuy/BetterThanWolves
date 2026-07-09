package btw.forge.mixin;

import btw.forge.BTWNetwork;
import btw.forge.PlayerBridge;
import btw.modern.ChunkCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
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
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /**
     * End-of-tick hook: sync state, run FC per-tick updates, sync back.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void btw$tick(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
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

        // Debug: log penalty levels occasionally
        if (self.tickCount % 100 == 0) {
            org.apache.logging.log4j.LogManager.getLogger("BTW-Penalties").info(
                    "food={} sat={:.1f} health={} penalties: hunger={} fat={} health={} gloom={} miningMod={:.2f}",
                    pb.foodStats.getFoodLevel(), pb.foodStats.getSaturationLevel(),
                    pb.health,
                    pb.GetHungerPenaltyLevel(), pb.GetFatPenaltyLevel(),
                    pb.GetHealthPenaltyLevel(), pb.GetGloomLevel(),
                    pb.GetMiningSpeedModifier());
        }

        // Send penalty levels to the client for HUD rendering.
        // This replaces FC's DataWatcher-based sync (IDs 22-31) with a
        // dedicated network packet. Runs every tick after penalty levels
        // are recalculated by UpdateModStatusVariables().
        BTWNetwork.sendPenaltySync(self, pb);

        // Sync FC state -> real MC
        pb.syncToReal();
    }

    /**
     * Load FC custom data from NBT on player load.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void btw$readNBT(CompoundTag tag, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PlayerBridge pb = PlayerBridge.getOrCreate(self);

        btw.forge.ForgeNBTCompound fcTag = new btw.forge.ForgeNBTCompound(tag);
        pb.ReadModDataFromNBT(fcTag);

        // Also load FC food data
        if (tag.contains("foodLevel")) {
            pb.foodStats.readNBT(fcTag);
        }
    }

    /**
     * Save FC custom data to NBT on player save.
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void btw$writeNBT(CompoundTag tag, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PlayerBridge pb = PlayerBridge.getOrCreate(self);

        btw.forge.ForgeNBTCompound fcTag = new btw.forge.ForgeNBTCompound(tag);
        pb.WriteModDataToNBT(fcTag);

        // Also save FC food data
        pb.foodStats.writeNBT(fcTag);
    }

    /**
     * Clone FC data when player respawns or transfers dimensions.
     */
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void btw$restoreFrom(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PlayerBridge oldBridge = PlayerBridge.getOrCreate(oldPlayer);
        PlayerBridge newBridge = PlayerBridge.getOrCreate(self);

        // Always copy persistent spawn data (survives death)
        newBridge.m_HardcoreSpawnChunk = oldBridge.m_HardcoreSpawnChunk;
        newBridge.m_iSpawnDimension = oldBridge.m_iSpawnDimension;
        newBridge.m_lTimeOfLastSpawnAssignment = oldBridge.m_lTimeOfLastSpawnAssignment;
        newBridge.m_lTimeOfLastDimensionSwitch = oldBridge.m_lTimeOfLastDimensionSwitch;
        newBridge.m_lRespawnAssignmentCooldownTimer = oldBridge.m_lRespawnAssignmentCooldownTimer;

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
    }

    // =========================================================================
    // FC Spawn System Hook
    // =========================================================================

    /**
     * Intercepts {@link ServerPlayer#getRespawnPosition()} to return the FC
     * hardcore spawn chunk coordinates instead of the vanilla spawn point,
     * when the FC spawn system has assigned one.
     *
     * <p>FC replaces vanilla bed-based spawning with a beacon-based system.
     * The spawn coordinates are stored in {@link PlayerBridge#m_HardcoreSpawnChunk}
     * and persisted in NBT (fcHCSpawnX/Y/Z). When set, this hook returns
     * those coordinates as the player's respawn position, overriding the
     * vanilla bed/respawn anchor position.</p>
     *
     * <p>If no FC spawn is set, vanilla behavior is preserved (returns the
     * original respawn position, which may be null for world spawn).</p>
     */
    @Inject(method = "getRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void btw$getRespawnPosition(CallbackInfoReturnable<BlockPos> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PlayerBridge pb = PlayerBridge.getOrCreate(self);

        ChunkCoordinates fcSpawn = pb.m_HardcoreSpawnChunk;
        if (fcSpawn != null) {
            cir.setReturnValue(new BlockPos(fcSpawn.posX, fcSpawn.posY, fcSpawn.posZ));
        }
        // Otherwise, fall through to vanilla behavior
    }

    /**
     * Intercepts {@link ServerPlayer#getRespawnDimension()} to return the
     * FC spawn dimension when an FC hardcore spawn is set.
     *
     * <p>FC tracks the spawn dimension separately in
     * {@link PlayerBridge#m_iSpawnDimension}. The legacy dimension IDs
     * (0 = overworld, -1 = nether, 1 = end) are mapped to MC 1.20.1
     * {@link ResourceKey ResourceKey&lt;Level&gt;} values.</p>
     */
    @Inject(method = "getRespawnDimension", at = @At("HEAD"), cancellable = true)
    private void btw$getRespawnDimension(CallbackInfoReturnable<ResourceKey<Level>> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PlayerBridge pb = PlayerBridge.getOrCreate(self);

        if (pb.m_HardcoreSpawnChunk != null) {
            ResourceKey<Level> dim = legacyDimToResourceKey(pb.m_iSpawnDimension);
            cir.setReturnValue(dim);
        }
        // Otherwise, fall through to vanilla behavior
    }

    /**
     * Intercepts {@link ServerPlayer#isRespawnForced()} to return true
     * when an FC hardcore spawn is set. FC spawn points are always "forced"
     * (they do not require an intact bed/anchor at the spawn position;
     * the beacon validation is handled separately by FC code).
     */
    @Inject(method = "isRespawnForced", at = @At("HEAD"), cancellable = true)
    private void btw$isRespawnForced(CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PlayerBridge pb = PlayerBridge.getOrCreate(self);

        if (pb.m_HardcoreSpawnChunk != null) {
            // FC spawn points are always forced (beacon validation is separate)
            cir.setReturnValue(true);
        }
    }

    // =========================================================================
    // Helper: legacy dimension ID -> MC 1.20.1 ResourceKey<Level>
    // =========================================================================

    /**
     * Maps a legacy numeric dimension ID to an MC 1.20.1 dimension ResourceKey.
     *
     * @param legacyDim 0 = overworld, -1 = nether, 1 = end
     * @return the corresponding {@link ResourceKey}, defaulting to overworld
     */
    private static ResourceKey<Level> legacyDimToResourceKey(int legacyDim) {
        return switch (legacyDim) {
            case -1 -> Level.NETHER;
            case 1 -> Level.END;
            default -> Level.OVERWORLD;
        };
    }
}
