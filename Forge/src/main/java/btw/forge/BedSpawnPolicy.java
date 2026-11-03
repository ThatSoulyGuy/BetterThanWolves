package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FCMOD-INTEGRATION(1.20.1): beds_sleep_no_spawn (ruling 3, 2026-10-19)
 *
 * <p><b>Feature:</b> 1.20.1 beds. BedBlock.use (BedBlock.java:72-110) explodes in the Nether and the End, reports an
 * occupied bed, and otherwise calls ServerPlayer.startSleepInBed, which sets the respawn point (ServerPlayer.java:853,
 * before the time check at :854, so a daytime click sets it too), then sleeps; the night skip follows
 * (ServerLevel.java:278-289).</p>
 *
 * <p><b>Conflict with FC:</b> 4.B removed sleeping and bed spawns: FCBlockBed.onBlockActivated only prints a line
 * (Client FCBlockBed.java:32-40), and 4.B respawns at a bound beacon or else at a random hardcore spawn point
 * ({@link HardcoreRespawn}, INTEGRATIONS #054). The port's former veto, PlayerMixin.btw$disableBeds, declared the
 * wrong return type on startSleepInBed (Player.java:1306) and threw a ClassCastException at night, after every bed
 * click had already set the spawn.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance, keep the feature), by ruling: beds keep 1.20.1 sleep (night skip,
 * action-bar messages, the Nether/End explosion), but a bed never sets the respawn point. HookPolicy rules bed USE
 * VANILLA (RULED(3)), so FCBlockBed's line never runs.</p>
 *
 * <p><b>Rebalance:</b> no spawn from any bed click, day or night; a respawn point set on a bed before this version is
 * ignored at the next respawn (HardcoreRespawn.begin, with {@link #isBed}) and dropped. {@code /spawnpoint} is forced
 * (SetSpawnCommand.java:36); respawn anchors (RespawnAnchorBlock.java:75) and the FC beacon bind (Server
 * FCBlockBeacon.java:93, not forced; validated by HardcoreRespawn) are not beds. All three are untouched. FC hostiles
 * keep a player awake (ServerPlayerMixin.btw$fcHostilesPreventRest).</p>
 *
 * <p><b>Injection strategy:</b> no mixin. Forge's PlayerSetSpawnEvent (cancellable) fires at the head of
 * ServerPlayer.setRespawnPosition (ServerPlayer.java:1479), before the field writes and the "Respawn point set"
 * message (:1481-1489), for every caller. A non-forced spawn whose block is a bed ({@code #minecraft:beds}, or a
 * modded block reporting {@code isBed}, IForgeBlockState.isBed) is cancelled. The block is read only where the chunk
 * is loaded (a bed click always is); this listener never loads a chunk. Never throws: a failure lets the spawn stand,
 * logged once.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> a redirect or wrap of the :853 call (vanilla beds only, and an
 * exclusive redirect); PlayerSleepInBedEvent or SleepingTimeCheckEvent (they can only refuse sleep, and the spawn
 * write comes before the time event); SleepingLocationCheckEvent (it decides whether a player stays asleep); clearing
 * old bed spawns at login (a chunk read per player).</p>
 */
// FCMOD-INTEGRATION(1.20.1): beds_sleep_no_spawn: a bed never sets the respawn point; 1.20.1 sleep, the night skip and the Nether/End explosion stay
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class BedSpawnPolicy {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Beds");

    private static volatile boolean failureLogged;

    private BedSpawnPolicy() {}

    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent e) {
        try {
            BlockPos pos = e.getNewSpawn();
            if (pos == null || e.isForced()) return;            // reset; /spawnpoint (forced)
            if (!(e.getEntity() instanceof ServerPlayer sp) || sp.getServer() == null) return;
            ServerLevel level = sp.getServer().getLevel(e.getSpawnLevel());
            if (level == null || !level.isLoaded(pos)) return;  // a bed click is loaded; never load a chunk here
            if (isBed(level.getBlockState(pos), level, pos, sp)) e.setCanceled(true);
        } catch (Throwable t) {
            if (!failureLogged) {
                failureLogged = true;
                LOGGER.warn("[beds_sleep_no_spawn] the bed spawn check failed (logged once); this spawn stands", t);
            }
        }
    }

    /** "A bed" for the beds ruling: {@code #minecraft:beds}, or a modded block reporting IForgeBlockState.isBed. Shared with
     *  HardcoreRespawn.begin, which ignores a non-forced respawn point on a bed saved before this policy existed. */
    static boolean isBed(BlockState s, ServerLevel level, BlockPos pos, ServerPlayer sp) {
        return s.is(BlockTags.BEDS) || s.isBed(level, pos, sp);
    }
}
