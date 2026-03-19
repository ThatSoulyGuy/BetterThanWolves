package btw.modern;

public enum FCEnumFurnaceBurnTime {
    NONE(0), DAMP_VEGETATION(10), KINDLING(25), SMALL_FUEL(50),
    WOOD_TOOLS(SMALL_FUEL.m_iBurnTime * 4),
    PLANKS_OAK(400),
    PLANKS_SPRUCE(PLANKS_OAK.m_iBurnTime * 3 / 4),
    PLANKS_BIRCH(PLANKS_OAK.m_iBurnTime * 5 / 4),
    PLANKS_JUNGLE(PLANKS_OAK.m_iBurnTime / 2),
    PLANKS_BLOOD(PLANKS_OAK.m_iBurnTime / 2),
    WOOD_BASED_BLOCK(PLANKS_OAK.m_iBurnTime / 2),
    COAL(1600), BLAZE_ROD(12800), LAVA_BUCKET(20000);

    public static final FCEnumFurnaceBurnTime
        WOOL = KINDLING, WOOL_KNIT = SMALL_FUEL, SHAFT = SMALL_FUEL,
        WICKER_PIECE = SMALL_FUEL;

    public final int m_iBurnTime;

    FCEnumFurnaceBurnTime(int iBurnTime) {
        m_iBurnTime = iBurnTime;
    }
}
