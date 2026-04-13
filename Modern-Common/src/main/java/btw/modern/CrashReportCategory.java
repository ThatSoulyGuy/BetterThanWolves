package btw.modern;

import java.util.concurrent.Callable;

public class CrashReportCategory {
    public void addCrashSection(String name, Object value) {}
    public void addCrashSectionThrowable(String name, Throwable throwable) {}
    /** Vanilla 1.5.2 method called by Entity.addEntityCrashInfo. */
    public void addCrashSectionCallable(String name, Callable<?> callable) {}
    /** Vanilla 1.5.2 helper used by Entity.writeToNBT to build a block-location string for crash reports. */
    public static String getLocationInfo(int x, int y, int z) {
        return String.format("World: (%d,%d,%d)", x, y, z);
    }
}
