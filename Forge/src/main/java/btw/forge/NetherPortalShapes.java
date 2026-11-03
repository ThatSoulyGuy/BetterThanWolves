package btw.forge;

import btw.forge.compat.FcRefs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraftforge.event.ForgeEventFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Optional;

/**
 * FCMOD-INTEGRATION(1.20.1): nether_portal (INTEGRATIONS #058)
 *
 * <p><b>Feature:</b> 1.20.1's Nether portal frames. PortalShape finds an empty obsidian frame from a cell inside it, of
 * any size from 2x3 to 21x21, along X or Z (findEmptyPortalShape tries the axis it is given, then the other one;
 * PortalShape.java:44-58), and createPortalBlocks fills it with nether_portal cells whose AXIS is the frame's
 * (:168-173). Forge posts the cancellable BlockEvent.PortalSpawnEvent first (ForgeEventFactory.onTrySpawnPortal,
 * ForgeEventFactory.java:705-709). 1.20.1 fire lights portals this way (BaseFireBlock.onPlace,
 * BaseFireBlock.java:123-139), and NetherPortalBlock.updateShape keeps a cell only while PortalShape still finds its
 * frame complete (NetherPortalBlock.java:64-69).</p>
 *
 * <p><b>Conflict with FC:</b> FC lights a portal itself when a campfire standing in a frame catches fire:
 * FCBlockCampfire.SetOnFireDirectly (Server FCBlockCampfire.java:237, Client :238) calls
 * Block.portal.tryToCreatePortal, FC's own 1.5.2 frame test (Common FCBlockPortal.java:56-131). That test (1) wrote
 * every cell as {@code setBlock(90, 0)} (:123), which WorldBridge resolves to {@code nether_portal[axis=x]}
 * (applyFcMetadataToVanillaBlock has no portal axis), so a north-south frame got cells turned the wrong way: they drew
 * across the frame, the first neighbour update from its east or west side failed PortalShape for axis X and emptied
 * that column, and a traveller arrived through an exit portal built along X; (2) lit only 2x3 frames, where 1.20.1 fire
 * lights up to 21x21; (3) counted FC's four campfire blocks as open interior (:106-111), which 1.20.1's
 * PortalShape.isEmpty (air, fire, portal; PortalShape.java:160-162) refuses, so a 1.20.1 fire never lit a frame with a
 * campfire in it, which 4.B's fire did.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): 1.20.1's PortalShape creates, sizes and orients every portal; FC keeps
 * its lighting entry, its interior rule (the campfires count as open interior for every lighting, fire's too) and its
 * random tick (Common FCBlockPortal.java:41-54: possession and the Nether flag, unchanged).</p>
 *
 * <p><b>Rebalance:</b> a campfire now opens every frame 1.20.1 fire opens (2x3 to 21x21), not only 2x3 ones; in 4.B the
 * campfire and fire always lit the same frames, and the port's fire already lights the larger ones. The campfire path
 * keeps 4.B's lack of a dimension gate (fire keeps 1.20.1's Overworld and Nether, as 4.B's {@code dimensionId <= 0}).
 * The Nether flag that FC's lighting set (:127) now comes from Forge's PortalSpawnEvent through NetherPortalProgress
 * (INTEGRATIONS #054), as for every 1.20.1 portal; FC's possession tick stays per portal cell, so a larger portal
 * possesses as often as the same number of cells in 2x3 portals.</p>
 *
 * <p><b>Injection strategy:</b> btw.forge.mixin.fcext.FCBlockPortalShapeMixin answers FCBlockPortal.tryToCreatePortal
 * at HEAD with {@link #tryToCreatePortal}: on a server WorldBridge, PortalShape.findEmptyPortalShape from FC's cell
 * (axis X first, as BaseFireBlock), then ForgeEventFactory.onTrySpawnPortal, then createPortalBlocks; any other FC
 * world (the client's read-only ClientFcWorld) answers false and writes nothing. btw.forge.mixin.PortalShapeMixin
 * widens PortalShape.isEmpty with {@link #isFcPortalInterior} on both sides, so the client's updateShape agrees with
 * the server's. Without that widening FC's own lighting could never succeed: the lit campfire (FC's small fire) stands
 * in the frame when SetOnFireDirectly calls tryToCreatePortal (Client FCBlockCampfire.java:222-249).</p>
 *
 * <p><b>Alternatives considered and rejected:</b> an axis branch in WorldBridge's metadata mapping (it fixes the axis
 * only, keeps FC's 2x3-only test beside 1.20.1's frame rule, and is dead code under this route); porting 1.5.2's
 * BlockPortal.onNeighborBlockChange (vanilla/server BlockPortal.java:176-228: it would strip every larger portal at its
 * first neighbour update); capping portals at 2x3; routing 1.20.1 fire through FC's BlockFire.onBlockAdded (two portal
 * attempts per fire, Modern-Common BlockFire.java:102-108).</p>
 */
// FCMOD-INTEGRATION(1.20.1): nether_portal: FC's campfire lighting opens portals through 1.20.1's PortalShape (axis, size, PortalSpawnEvent), and PortalShape counts FC's campfires as open interior
public final class NetherPortalShapes {

    private static final Logger LOGGER = LogManager.getLogger("BTW-NetherPortal");

    /** The FCBetterThanWolves fields of the four campfire blocks, unlit and lit (Common FCBlockPortal.java:108-111). */
    private static final String[] CAMPFIRE_FIELDS = {
            "fcBlockCampfireUnlit", "fcBlockCampfireSmall", "fcBlockCampfireMedium", "fcBlockCampfireLarge"};

    private static final int[] NONE = new int[0];

    /** FC block ids of {@link #CAMPFIRE_FIELDS} (-1 for a field that did not resolve); null until FC is ready. */
    private static volatile int[] campfireIds;

    private static volatile boolean failureLogged;

    private NetherPortalShapes() {}

    /**
     * FCBlockPortal.tryToCreatePortal through 1.20.1: finds the empty frame around (x, y, z) on the server level behind
     * {@code world}, posts Forge's PortalSpawnEvent and, unless a listener cancels it, fills the frame. True when the
     * portal was made. Any world that is not a server WorldBridge answers false and writes nothing. Never throws: a
     * failure is logged once and answers false.
     */
    public static boolean tryToCreatePortal(btw.modern.World world, int x, int y, int z) {
        if (!(world instanceof WorldBridge bridge)) return false;
        ServerLevel level = bridge.getServerLevel();
        if (level == null) return false;
        try {
            BlockPos pos = new BlockPos(x, y, z);
            Optional<PortalShape> shape = PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X);
            shape = ForgeEventFactory.onTrySpawnPortal(level, pos, shape);
            if (shape.isEmpty()) return false;
            shape.get().createPortalBlocks();
            return true;
        } catch (RuntimeException e) {
            if (!failureLogged) {
                failureLogged = true;
                LOGGER.warn("[nether_portal] FC's portal lighting at {} {} {} failed (logged once); no portal was made",
                        x, y, z, e);
            }
            return false;
        }
    }

    /**
     * True for FC's four campfire blocks, lit or not, which 4.B's frame test counted as open interior (Common
     * FCBlockPortal.java:106-111). FC-only blocks are ProxyBlocks keyed by their FC block id; the ids come from the BTW
     * ID config through FcRefs once FC is ready. Before that, and for every other state, false.
     */
    public static boolean isFcPortalInterior(BlockState state) {
        if (!(state.getBlock() instanceof ProxyBlock proxy)) return false;
        int id = proxy.getLegacyId();
        for (int campfire : campfireIds()) {
            if (campfire == id) return true;
        }
        return false;
    }

    private static int[] campfireIds() {
        int[] ids = campfireIds;
        if (ids != null) return ids;
        if (!BTWLifecycle.isFcReady()) return NONE;
        synchronized (NetherPortalShapes.class) {
            if (campfireIds == null) {
                int[] found = new int[CAMPFIRE_FIELDS.length];
                boolean all = true;
                for (int i = 0; i < found.length; i++) {
                    found[i] = FcRefs.legacyBlockId(CAMPFIRE_FIELDS[i]);
                    all &= found[i] > 0;
                }
                campfireIds = found;
                if (all) {
                    LOGGER.info("[nether_portal] FC's campfire blocks {} count as open portal interior",
                            Arrays.toString(found));
                } else {
                    LOGGER.warn("[nether_portal] FC's campfire blocks did not all resolve ({} for {}); an unresolved "
                            + "one is no portal interior", Arrays.toString(found), Arrays.toString(CAMPFIRE_FIELDS));
                }
            }
            return campfireIds;
        }
    }
}
