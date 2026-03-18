package btw.api;

public class MathHelper {

    public static final float sin(float value) {
        return (float) Math.sin(value);
    }

    public static final float cos(float value) {
        return (float) Math.cos(value);
    }

    public static final float sqrt_float(float value) {
        return (float) Math.sqrt(value);
    }

    public static final float sqrt_double(double value) {
        return (float) Math.sqrt(value);
    }

    public static int floor_float(float value) {
        int i = (int) value;
        return value < (float) i ? i - 1 : i;
    }

    public static int floor_double(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }

    public static long floor_double_long(double value) {
        long l = (long) value;
        return value < (double) l ? l - 1L : l;
    }

    public static float abs(float value) {
        return value >= 0.0F ? value : -value;
    }

    public static int abs_int(int value) {
        return value >= 0 ? value : -value;
    }

    public static int ceiling_float_int(float value) {
        int i = (int) value;
        return value > (float) i ? i + 1 : i;
    }

    public static int ceiling_double_int(double value) {
        int i = (int) value;
        return value > (double) i ? i + 1 : i;
    }

    public static int clamp_int(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static float clamp_float(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static double clamp_double(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static double abs_max(double a, double b) {
        if (a < 0.0D) a = -a;
        if (b < 0.0D) b = -b;
        return a > b ? a : b;
    }

    public static int bucketInt(int value, int bucketSize) {
        return value < 0 ? -((-value - 1) / bucketSize) - 1 : value / bucketSize;
    }

    public static int getRandomIntegerInRange(java.util.Random rand, int min, int max) {
        return min >= max ? min : rand.nextInt(max - min + 1) + min;
    }

    public static float wrapAngleTo180_float(float angle) {
        angle = angle % 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    public static double wrapAngleTo180_double(double angle) {
        angle = angle % 360.0D;
        if (angle >= 180.0D) angle -= 360.0D;
        if (angle < -180.0D) angle += 360.0D;
        return angle;
    }
}
