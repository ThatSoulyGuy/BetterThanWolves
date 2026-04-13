package btw.modern;

/**
 * Stub of FC's util math class. Vanilla 1.5.2 Entity (with FCMOD patches)
 * calls {@code FCUtilsMath.ClampDouble} and {@code FCUtilsMath.ClampDoubleTop}
 * — these are the only methods we need to provide.
 *
 * The actual FC version is at {@code net.minecraft.src.btw.util.FCUtilsMath}
 * (kept at its original FC package by the {@code net.minecraft.src.btw.**}
 * exclude in {@code remapFcCode}). This stub at {@code btw.modern.FCUtilsMath}
 * exists because vanilla 1.5.2 Entity's bytecode references it via the
 * {@code net.minecraft.src.* -> btw.modern.*} relocation.
 */
public class FCUtilsMath {

    /** Clamp value between min and max. */
    public static double ClampDouble(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /** Clamp value to a maximum (no minimum). */
    public static double ClampDoubleTop(double value, double max) {
        return value > max ? max : value;
    }
}
