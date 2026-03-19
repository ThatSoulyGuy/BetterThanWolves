package btw.modern;

/**
 * Translates between legacy 1.5.2 int IDs and modern MC registries.
 * For the Forge backend, this maps int block/item IDs to ProxyBlock/ProxyItem instances.
 * For now, provides the static arrays that FC code accesses via Block.blocksList[id].
 */
public class IDMappingService {
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        // The static arrays (Block.blocksList, Item.itemsList) are populated
        // by Block/Item constructors when FC code creates block/item instances.
        // No explicit mapping needed — FC code self-registers.
    }
}
