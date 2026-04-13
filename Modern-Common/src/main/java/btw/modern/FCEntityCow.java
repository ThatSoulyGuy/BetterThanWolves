package btw.modern;

/**
 * Type-only stub. Vanilla 1.5.2 Entity references this class as a method
 * parameter (OnKickedByCow) and via .class literal. The class only needs
 * to exist for JVM linker verification — no behavior is required.
 *
 * The actual FC cow entity is at {@code net.minecraft.src.btw.entity.FCEntityCow}
 * (kept at its original FC package by the {@code net.minecraft.src.btw.**}
 * exclude in {@code remapFcCode}).
 */
public class FCEntityCow extends EntityCow {
    public FCEntityCow(World world) { super(world); }
}
