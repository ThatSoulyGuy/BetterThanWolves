package btw.forge.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.VillageSiege;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): village_sieges
 *
 * <p><b>Feature:</b> the zombie siege (MC 1.0; 1.20.1 {@code VillageSiege}, a CustomSpawner of the overworld only,
 * MinecraftServer.java:335). ServerLevel.tickCustomSpawners calls its {@code tick} every server tick while
 * {@code doMobSpawning} is true (ServerChunkCache.java:368-369, ServerLevel.java:379-384). At midnight, one night in
 * ten, a player standing in a village draws 20 zombies into it (VillageSiege.java:31-69, :85).</p>
 *
 * <p><b>Conflict with FC:</b> 4.B has no zombie siege. FC removed the siege object and its tick
 * (vanilla/server World.java:105-107 and WorldServer.java:183-185, "FCMOD: Removed"; vanilla/client World.java:103-105
 * and WorldServer.java:184-186), and no FC class replaces it. 1.20.1's siege besieges a POI village
 * (VillageSiege.java:75, :122; ServerLevel.java:1228-1246): a village left from play before the port, or one whose job
 * sites, beds or bells another mod's Minecraft-style villagers occupy. A BTW village (1.5.2's doors and villagers) is
 * never one by itself: only a vanilla villager's brain takes a POI ticket (AcquirePoi, VillagerMakeLove), and every
 * villager in the port is a BTW villager.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance by subtraction), by ruling (2026-10-23, "zombie sieges: none, as 4.B"),
 * which replaces the 2026-10-22 ruling's 10-door, 20-villager gate. Part of INTEGRATIONS #048 (Villages are zombified,
 * as in 4.B).</p>
 *
 * <p><b>Disables (justification):</b> the zombie siege, in every village and every game mode, as 4.B removed it. The
 * rest of the village stays: BTW's village (Modern-Common VillageCollection) still ticks in
 * WorldBridge.ModSpecificTick, as 4.B kept {@code villageCollectionObj.tick()} (WorldServer.java:182), and the other
 * custom spawners (phantoms, patrols, cats, the wandering trader) are untouched.</p>
 *
 * <p><b>Injection strategy:</b> one cancellable {@code @Inject} at the HEAD of
 * {@code VillageSiege.tick(ServerLevel, boolean, boolean)} (SRG {@code m_7995_}, the CustomSpawner method) that returns
 * 0, what FC did to 1.5.2: the object stays, its tick does nothing. The state stays {@code SIEGE_DONE}, so the midnight
 * roll (:33-36), the village test (:75), the spawn search, Forge's VillageSiegeEvent (:83) and the zombies (:98-112)
 * never run, for the overworld's siege and for any other instance. ServerLevel discards the return value
 * (tickCustomSpawners, bytecode {@code pop} after the call). Server thread.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> cancelling Forge's VillageSiegeEvent (Protocol #4's first choice): it
 * is posted only after the village test and a successful spawn search (VillageSiege.java:75-83), and a cancel makes
 * {@code tryToSetupSiege} return false, so the setup runs again on every tick of a siege night (:41-47): the ring
 * search, its draws from the level's random source and the event, until day. Dropping the siege from the overworld's
 * spawner list (MinecraftServer.java:335): the literal form of FC's removal, but it targets an
 * {@code ImmutableList.of} call inside {@code createLevels}, which Forge patches, and it misses other instances. The
 * 2026-10-22 gate (two redirects of {@code isVillage} to BTW's village and a log inject): machinery for a feature that
 * no longer runs.</p>
 */
@Mixin(VillageSiege.class)
public abstract class VillageSiegeMixin {

    // FCMOD-INTEGRATION(1.20.1): village_sieges: no zombie siege, as in 4.B (FC removed the siege's tick, vanilla/server WorldServer.java:183-185)
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;ZZ)I", at = @At("HEAD"), cancellable = true)
    private void btw$noSiege(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies,
                             CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
