package btw.forge;

import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * FCMOD-INTEGRATION(1.20.1): hardcore_respawn (INTEGRATIONS #054; RESPAWN design 7(d)(1), b2a/F2)
 *
 * <p><b>Feature:</b> 1.20.1 lights a Nether portal in BaseFireBlock.onPlace (BaseFireBlock.java:123-139): a fire
 * placed in the Overworld or the Nether looks for an empty obsidian frame (PortalShape.findEmptyPortalShape), posts
 * Forge's cancellable BlockEvent.PortalSpawnEvent (ForgeEventFactory.onTrySpawnPortal, ForgeEventFactory.java:705-708)
 * and, unless a listener cancels it, fills the frame (PortalShape.createPortalBlocks, PortalShape.java:168-173). Flint
 * and steel, fire charges, spreading fire and lightning all end there, FC's fire too: every fire the port places is a
 * 1.20.1 fire.</p>
 *
 * <p><b>Conflict with FC:</b> 4.B sets the "Nether accessed" progress flag in two places: the portal's random tick
 * (Common FCBlockPortal.java:41-54, which TickPolicy runs for id 90) and the moment a portal is lit,
 * FCBlockPortal.tryToCreatePortal (:127), called from BlockFire.onBlockAdded (vanilla/server BlockFire.java:381-394)
 * and FCBlockCampfire.SetOnFireDirectly (Server FCBlockCampfire.java:237). The 1.20.1 lighting never reaches
 * tryToCreatePortal (BaseFireBlock.onPlace does not call BlockBehaviour.onPlace, where FC's onBlockAdded hook sits;
 * Modern-Common BlockFire.java:102-108), so the flag waited for the first random tick of a portal block in a
 * random-ticking chunk: about 11 seconds on average for a 2x3 portal at randomTickSpeed 3 (4096 / (3 x 6) ticks), and
 * never while no player stood within 128 blocks of it (ServerChunkCache.java:355-362). FC's hardcore respawn reads the
 * flag (Server FCUtilsHardcoreSpawn.java:99 and :145: the x1.5 radius, no shared early-game respawn), so a death in
 * that window was placed as if the Nether were still closed.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): 1.20.1's portal shapes and lighting stay; FC's own setter runs at
 * 4.B's moment.</p>
 *
 * <p><b>Rebalance:</b> none. The flag is set when a portal is lit, as in 4.B; the random-tick setter is unchanged. FC's
 * campfire lighting comes here too: FCBlockPortal.tryToCreatePortal goes through 1.20.1's PortalShape and posts the
 * same event (NetherPortalShapes, INTEGRATIONS #058), so FC's own setter call there (:127) no longer runs.</p>
 *
 * <p><b>Injection strategy:</b> no mixin. A LOWEST-priority listener of PortalSpawnEvent that does not receive
 * cancelled events, so only a portal that is about to be filled counts. It calls FC's setter,
 * FCUtilsWorld.GameProgressSetNetherBeenAccessedServerOnly (Server FCUtilsWorld.java:555-561), through a MethodHandle
 * resolved on first use (Forge compiles against Modern-Common, not the FC classes; the ModernWoods precedent). The
 * event comes only from the server (LevelChunk.setBlockState runs onPlace server-side only, and NetherPortalShapes
 * posts it only for a server WorldBridge); a client level is skipped anyway. Never throws: a failure leaves the
 * random-tick setter in charge and is logged once.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> routing BaseFireBlock.onPlace through FC's BlockFire.onBlockAdded (FC's
 * fixed 2x3 frame test would replace 1.20.1's portal shapes, and two portal attempts would run, Modern-Common
 * BlockFire.java:102-108); a mixin at PortalShape.createPortalBlocks (the same moment, where Forge already posts a
 * public event); setting the flag for every portal block placed (4.B set nothing when Teleporter.makePortal built the
 * arrival portal or a command placed portal blocks; their random ticks set it, as here).</p>
 */
// FCMOD-INTEGRATION(1.20.1): hardcore_respawn: lighting a Nether portal sets FC's nether-accessed progress flag at once, as 4.B's FCBlockPortal.tryToCreatePortal did
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class NetherPortalProgress {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Progress");

    private static final String FC_UTILS_WORLD = "net.minecraft.src.btw.util.FCUtilsWorld";

    /** FCUtilsWorld.GameProgressSetNetherBeenAccessedServerOnly(), type ()V; null until first resolved. */
    private static volatile MethodHandle setNetherAccessed;

    private static volatile boolean failureLogged;

    private NetherPortalProgress() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent e) {
        if (e.getLevel().isClientSide() || !BTWLifecycle.isFcReady()) return;
        try {
            MethodHandle setter = setNetherAccessed;
            if (setter == null) {
                Method m = Class.forName(FC_UTILS_WORLD).getMethod("GameProgressSetNetherBeenAccessedServerOnly");
                setter = MethodHandles.lookup().unreflect(m);
                setNetherAccessed = setter;
            }
            setter.invokeExact();
        } catch (Throwable t) {
            if (!failureLogged) {
                failureLogged = true;
                LOGGER.warn("[hardcore_respawn] lighting a portal could not set FC's nether-accessed flag (logged once); "
                        + "the portal's random tick still sets it", t);
            }
        }
    }
}
