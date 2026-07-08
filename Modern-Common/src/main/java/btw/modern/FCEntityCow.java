package btw.modern;

/**
 * Bridge stub for the flat 1.5.2 FCEntityCow. Vanilla 1.5.2 Entity references
 * this class as a method parameter (OnKickedByCow) and, crucially, the frozen
 * EntityMooshroom extends it: FC cows grazing mycelium convert via
 * ConvertToMooshroom() into btw.modern.EntityMooshroom, whose milking path
 * (EntityMooshroom.interact) calls GotMilk()/SetGotMilk(boolean) — resolved
 * against THIS class, since it shadows the frozen flat FCEntityCow (which
 * cannot be un-shadowed: it references btw.modern.FCBetterThanWolves, a
 * pre-restructure name that no longer exists).
 *
 * GotMilk state mirrors net.minecraft.src.btw.entity.FCEntityCow (the live
 * restructured cow): a byte in DataWatcher slot 26. The frozen EntityMooshroom
 * does not override entityInit, so slot 26 is free on its watcher.
 *
 * The actual FC cow entity is at {@code net.minecraft.src.btw.entity.FCEntityCow}
 * (kept at its original FC package by the {@code net.minecraft.src.btw.**}
 * exclude in {@code remapFcCode}).
 */
public class FCEntityCow extends EntityCow {

    public static final int m_iGotMilkDataWatcherID = 26;

    public FCEntityCow(World world) { super(world); }

    @Override
    public void entityInit() {
        super.entityInit();
        dataWatcher.addObject(m_iGotMilkDataWatcherID, new Byte((byte) 0));
    }

    public boolean GotMilk() {
        return dataWatcher.getWatchableObjectByte(m_iGotMilkDataWatcherID) != 0;
    }

    public void SetGotMilk(boolean gotMilk) {
        dataWatcher.updateObject(m_iGotMilkDataWatcherID, new Byte((byte) (gotMilk ? 1 : 0)));
    }
}
