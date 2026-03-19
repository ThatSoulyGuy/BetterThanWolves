package btw.forge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple registry mapping legacy int block/item IDs to their Forge proxy
 * counterparts.  Populated during {@link BTWRegistration#registerAllBTWContent()}
 * after FC initialization fills {@code btw.modern.Block.blocksList[]}.
 *
 * This replaces all former IDMappingService look-ups inside Forge code.
 */
public class ProxyRegistry {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyRegistry");

    private static final Map<Integer, ProxyBlock> blocksByLegacyId = new HashMap<>();
    private static final Map<Integer, net.minecraft.world.item.Item> itemsByLegacyId = new HashMap<>();

    // ---- blocks ----

    public static void registerProxy(int legacyId, ProxyBlock proxy) {
        blocksByLegacyId.put(legacyId, proxy);
    }

    public static ProxyBlock getProxy(int legacyId) {
        return blocksByLegacyId.get(legacyId);
    }

    /**
     * Returns the Forge {@link net.minecraft.world.level.block.Block} for a
     * legacy block ID, or {@code null} if no proxy has been registered.
     */
    public static net.minecraft.world.level.block.Block getModernBlock(int legacyId) {
        return blocksByLegacyId.get(legacyId);
    }

    // ---- items ----

    public static void registerItem(int legacyId, net.minecraft.world.item.Item item) {
        itemsByLegacyId.put(legacyId, item);
    }

    public static net.minecraft.world.item.Item getModernItem(int legacyId) {
        return itemsByLegacyId.get(legacyId);
    }
}
