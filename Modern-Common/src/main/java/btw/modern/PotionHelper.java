package btw.modern;

public class PotionHelper {
    public static final String field_77924_a = null;
    public static final String sugarEffect = null;
    public static final String ghastTearEffect = null;
    public static final String spiderEyeEffect = null;
    public static final String fermentedSpiderEyeEffect = null;
    public static final String speckledMelonEffect = null;
    public static final String blazePowderEffect = null;
    public static final String magmaCreamEffect = null;
    public static final String redstoneEffect = null;
    public static final String glowstoneEffect = null;
    public static final String gunpowderEffect = null;
    public static final String goldenCarrotEffect = null;

    public static int applyIngredient(int existingDamage, String ingredientEffect) { return 0; }
    public static boolean checkFlag(int damage, int flag) { return false; }
    public static int func_77909_a(int damage) { return 0; }
    public static String func_77905_c(int damage) { return null; }

    // 1.5.2 PotionHelper.calcPotionLiquidColor — EntityLiving.updatePotionEffects
    // computes this whenever a mob has any active potion effect. Effects whose
    // Potion isn't registered in the shim's potionTypes are skipped instead of
    // NPEing (bridging deviation; vanilla assumes a fully populated table).
    public static int calcPotionLiquidColor(java.util.Collection effects) {
        int defaultColor = 3694022;
        if (effects == null || effects.isEmpty()) {
            return defaultColor;
        }
        float r = 0.0F, g = 0.0F, b = 0.0F, weight = 0.0F;
        for (Object o : effects) {
            PotionEffect effect = (PotionEffect) o;
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;
            int color = potion.getLiquidColor();
            for (int i = 0; i <= effect.getAmplifier(); i++) {
                r += (float) (color >> 16 & 255) / 255.0F;
                g += (float) (color >> 8 & 255) / 255.0F;
                b += (float) (color & 255) / 255.0F;
                weight++;
            }
        }
        if (weight == 0.0F) {
            return defaultColor;
        }
        r = r / weight * 255.0F;
        g = g / weight * 255.0F;
        b = b / weight * 255.0F;
        return (int) r << 16 | (int) g << 8 | (int) b;
    }

    // 1.5.2 PotionHelper.func_82817_b — true iff every active effect is ambient
    public static boolean func_82817_b(java.util.Collection effects) {
        for (Object o : effects) {
            if (!((PotionEffect) o).getIsAmbient()) {
                return false;
            }
        }
        return true;
    }
}
