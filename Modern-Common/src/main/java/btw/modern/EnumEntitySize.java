package btw.modern;

/**
 * Vanilla 1.5.2 enum used by {@code Entity.myEntitySize} to pick a
 * rounding mode for server-side entity position quantisation (used for
 * the serverPosX/Y/Z fields sent over the network). Referenced directly
 * by vanilla Entity's field {@code public EnumEntitySize myEntitySize}.
 *
 * Ported from vanilla 1.5.2 since the logic is pure math with no
 * external dependencies.
 */
public enum EnumEntitySize {
    SIZE_1,
    SIZE_2,
    SIZE_3,
    SIZE_4,
    SIZE_5,
    SIZE_6;

    public int multiplyBy32AndRound(double value) {
        double frac = value - ((double) MathHelper.floor_double(value) + 0.5D);
        switch (this) {
            case SIZE_1:
                if (frac < 0.0D) {
                    if (frac < -0.3125D) return MathHelper.ceiling_double_int(value * 32.0D);
                } else if (frac < 0.3125D) {
                    return MathHelper.ceiling_double_int(value * 32.0D);
                }
                return MathHelper.floor_double(value * 32.0D);

            case SIZE_2:
                if (frac < 0.0D) {
                    if (frac < -0.3125D) return MathHelper.floor_double(value * 32.0D);
                } else if (frac < 0.3125D) {
                    return MathHelper.floor_double(value * 32.0D);
                }
                return MathHelper.ceiling_double_int(value * 32.0D);

            case SIZE_3:
                if (frac > 0.0D) return MathHelper.floor_double(value * 32.0D);
                return MathHelper.ceiling_double_int(value * 32.0D);

            case SIZE_4:
                if (frac < 0.0D) {
                    if (frac < -0.1875D) return MathHelper.ceiling_double_int(value * 32.0D);
                } else if (frac < 0.1875D) {
                    return MathHelper.ceiling_double_int(value * 32.0D);
                }
                return MathHelper.floor_double(value * 32.0D);

            case SIZE_5:
                if (frac < 0.0D) {
                    if (frac < -0.1875D) return MathHelper.floor_double(value * 32.0D);
                } else if (frac < 0.1875D) {
                    return MathHelper.floor_double(value * 32.0D);
                }
                return MathHelper.ceiling_double_int(value * 32.0D);

            case SIZE_6:
            default:
                if (frac > 0.0D) return MathHelper.ceiling_double_int(value * 32.0D);
                return MathHelper.floor_double(value * 32.0D);
        }
    }
}
