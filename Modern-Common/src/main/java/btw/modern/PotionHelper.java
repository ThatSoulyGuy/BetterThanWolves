package btw.modern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 1.5.2 PotionHelper — full port of the damage-bits parser and ingredient
 * effect strings (they are data, not logic). Live references: FC init sets
 * witch wart / spider eye brewing effects (FCBetterThanWolves.java:1526,1561),
 * and ItemPotion.getEffects drives all potion consumption.
 *
 * NOTE: the static tables below read Potion.* statics, so this class must not
 * be loaded before Potion.initializeVanillaPotions() (BTWLifecycle Step 2c runs
 * it before any FC code).
 */
public class PotionHelper {
    public static final String field_77924_a = null;
    public static final String sugarEffect;
    public static final String ghastTearEffect = "+0-1-2-3&4-4+13";
    public static final String spiderEyeEffect;
    public static final String fermentedSpiderEyeEffect;
    public static final String speckledMelonEffect;
    public static final String blazePowderEffect;
    public static final String magmaCreamEffect;
    public static final String redstoneEffect;
    public static final String glowstoneEffect;
    public static final String gunpowderEffect;
    public static final String goldenCarrotEffect;
    private static final HashMap potionRequirements = new HashMap();

    /** Potion effect amplifier map */
    private static final HashMap potionAmplifiers = new HashMap();
    private static final HashMap field_77925_n;

    /** An array of possible potion prefix names, as translation IDs. */
    private static final String[] potionPrefixes;

    // 1.5.2 PotionHelper.checkFlag — is the bit set?
    public static boolean checkFlag(int damage, int bit) {
        return (damage & 1 << bit) != 0;
    }

    // 1.5.2 PotionHelper.isFlagSet
    private static int isFlagSet(int damage, int bit) {
        return checkFlag(damage, bit) ? 1 : 0;
    }

    // 1.5.2 PotionHelper.isFlagUnset
    private static int isFlagUnset(int damage, int bit) {
        return checkFlag(damage, bit) ? 0 : 1;
    }

    // 1.5.2 PotionHelper.func_77909_a — prefix index from the damage bits.
    public static int func_77909_a(int damage) {
        return func_77908_a(damage, 5, 4, 3, 2, 1);
    }

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

    // 1.5.2 PotionHelper.func_77915_a — cached liquid color for a damage value;
    // ItemPotion.getColorFromDamage renders through this.
    public static int func_77915_a(int damage, boolean includeUsable) {
        if (!includeUsable) {
            if (field_77925_n.containsKey(Integer.valueOf(damage))) {
                return ((Integer) field_77925_n.get(Integer.valueOf(damage))).intValue();
            } else {
                int color = calcPotionLiquidColor(getPotionEffects(damage, false));
                field_77925_n.put(Integer.valueOf(damage), Integer.valueOf(color));
                return color;
            }
        } else {
            return calcPotionLiquidColor(getPotionEffects(damage, includeUsable));
        }
    }

    // 1.5.2 PotionHelper.func_77905_c — translated potion prefix for a damage
    // value; ItemPotion.getItemDisplayName uses it for effect-less potions.
    public static String func_77905_c(int damage) {
        int prefixIndex = func_77909_a(damage);
        return potionPrefixes[prefixIndex];
    }

    // 1.5.2 PotionHelper.func_77904_a — evaluates one term of a requirement string.
    private static int func_77904_a(boolean negated, boolean multiplied, boolean inverted, int comparison, int bit, int multiplier, int damage) {
        int result = 0;

        if (negated) {
            result = isFlagUnset(damage, bit);
        } else if (comparison != -1) {
            if (comparison == 0 && countSetFlags(damage) == bit) {
                result = 1;
            } else if (comparison == 1 && countSetFlags(damage) > bit) {
                result = 1;
            } else if (comparison == 2 && countSetFlags(damage) < bit) {
                result = 1;
            }
        } else {
            result = isFlagSet(damage, bit);
        }

        if (multiplied) {
            result *= multiplier;
        }

        if (inverted) {
            result *= -1;
        }

        return result;
    }

    // 1.5.2 PotionHelper.countSetFlags — number of ON bits.
    private static int countSetFlags(int value) {
        int count;

        for (count = 0; value > 0; ++count) {
            value &= value - 1;
        }

        return count;
    }

    // 1.5.2 PotionHelper.parsePotionEffects — recursive-descent evaluation of a
    // requirement/amplifier string against the damage bits.
    private static int parsePotionEffects(String str, int start, int end, int damage) {
        if (start < str.length() && end >= 0 && start < end) {
            int orIndex = str.indexOf(124, start);
            int leftResult;
            int rightResult;

            if (orIndex >= 0 && orIndex < end) {
                leftResult = parsePotionEffects(str, start, orIndex - 1, damage);

                if (leftResult > 0) {
                    return leftResult;
                } else {
                    rightResult = parsePotionEffects(str, orIndex + 1, end, damage);
                    return rightResult > 0 ? rightResult : 0;
                }
            } else {
                leftResult = str.indexOf(38, start);

                if (leftResult >= 0 && leftResult < end) {
                    rightResult = parsePotionEffects(str, start, leftResult - 1, damage);

                    if (rightResult <= 0) {
                        return 0;
                    } else {
                        int andRight = parsePotionEffects(str, leftResult + 1, end, damage);
                        return andRight <= 0 ? 0 : (rightResult > andRight ? rightResult : andRight);
                    }
                } else {
                    boolean multiplied = false;
                    boolean hasMultiplier = false;
                    boolean hasNumber = false;
                    boolean negated = false;
                    boolean inverted = false;
                    byte comparison = -1;
                    int number = 0;
                    int multiplier = 0;
                    int total = 0;

                    for (int i = start; i < end; i++) {
                        char c = str.charAt(i);

                        if (c >= 48 && c <= 57) {
                            if (multiplied) {
                                multiplier = c - 48;
                                hasMultiplier = true;
                            } else {
                                number *= 10;
                                number += c - 48;
                                hasNumber = true;
                            }
                        } else if (c == 42) {
                            multiplied = true;
                        } else if (c == 33) {
                            if (hasNumber) {
                                total += func_77904_a(negated, hasMultiplier, inverted, comparison, number, multiplier, damage);
                                negated = false;
                                inverted = false;
                                multiplied = false;
                                hasMultiplier = false;
                                hasNumber = false;
                                multiplier = 0;
                                number = 0;
                                comparison = -1;
                            }

                            negated = true;
                        } else if (c == 45) {
                            if (hasNumber) {
                                total += func_77904_a(negated, hasMultiplier, inverted, comparison, number, multiplier, damage);
                                negated = false;
                                inverted = false;
                                multiplied = false;
                                hasMultiplier = false;
                                hasNumber = false;
                                multiplier = 0;
                                number = 0;
                                comparison = -1;
                            }

                            inverted = true;
                        } else if (c != 61 && c != 60 && c != 62) {
                            if (c == 43 && hasNumber) {
                                total += func_77904_a(negated, hasMultiplier, inverted, comparison, number, multiplier, damage);
                                negated = false;
                                inverted = false;
                                multiplied = false;
                                hasMultiplier = false;
                                hasNumber = false;
                                multiplier = 0;
                                number = 0;
                                comparison = -1;
                            }
                        } else {
                            if (hasNumber) {
                                total += func_77904_a(negated, hasMultiplier, inverted, comparison, number, multiplier, damage);
                                negated = false;
                                inverted = false;
                                multiplied = false;
                                hasMultiplier = false;
                                hasNumber = false;
                                multiplier = 0;
                                number = 0;
                                comparison = -1;
                            }

                            if (c == 61) {
                                comparison = 0;
                            } else if (c == 60) {
                                comparison = 2;
                            } else if (c == 62) {
                                comparison = 1;
                            }
                        }
                    }

                    if (hasNumber) {
                        total += func_77904_a(negated, hasMultiplier, inverted, comparison, number, multiplier, damage);
                    }

                    return total;
                }
            }
        } else {
            return 0;
        }
    }

    /**
     * 1.5.2 PotionHelper.getPotionEffects — returns the list of PotionEffects
     * encoded by a potion damage value. Caller: ItemPotion.getEffects (all
     * potion drinking / splash effects).
     */
    public static List getPotionEffects(int damage, boolean includeUsable) {
        ArrayList result = null;

        for (Potion potion : Potion.potionTypes) {
            if (potion != null && (!potion.isUsable() || includeUsable)) {
                String requirements = (String) potionRequirements.get(Integer.valueOf(potion.getId()));

                if (requirements != null) {
                    int strength = parsePotionEffects(requirements, 0, requirements.length(), damage);

                    if (strength > 0) {
                        int amplifier = 0;
                        String amplifierStr = (String) potionAmplifiers.get(Integer.valueOf(potion.getId()));

                        if (amplifierStr != null) {
                            amplifier = parsePotionEffects(amplifierStr, 0, amplifierStr.length(), damage);

                            if (amplifier < 0) {
                                amplifier = 0;
                            }
                        }

                        if (potion.isInstant()) {
                            strength = 1;
                        } else {
                            strength = 1200 * (strength * 3 + (strength - 1) * 2);
                            strength >>= amplifier;
                            strength = (int) Math.round((double) strength * potion.getEffectiveness());

                            if ((damage & 16384) != 0) {
                                strength = (int) Math.round((double) strength * 0.75D + 0.5D);
                            }
                        }

                        if (result == null) {
                            result = new ArrayList();
                        }

                        PotionEffect effect = new PotionEffect(potion.getId(), strength, amplifier);

                        if ((damage & 16384) != 0) {
                            effect.setSplashPotion(true);
                        }

                        result.add(effect);
                    }
                }
            }
        }

        return result;
    }

    // 1.5.2 PotionHelper.brewBitOperations — single bit operation for
    // applyIngredient: remove, toggle, require, or set.
    private static int brewBitOperations(int data, int bit, boolean remove, boolean toggle, boolean requirePresent) {
        if (requirePresent) {
            if (!checkFlag(data, bit)) {
                return 0;
            }
        } else if (remove) {
            data &= ~(1 << bit);
        } else if (toggle) {
            if ((data & 1 << bit) == 0) {
                data |= 1 << bit;
            } else {
                data &= ~(1 << bit);
            }
        } else {
            data |= 1 << bit;
        }

        return data;
    }

    /**
     * 1.5.2 PotionHelper.applyIngredient — generates the new potion damage
     * value from the previous one and an ingredient's effect string. Caller:
     * TileEntityBrewingStand.getPotionResult (once the brewing stand is ported).
     */
    public static int applyIngredient(int damage, String effectStr) {
        byte start = 0;
        int length = effectStr.length();
        boolean hasNumber = false;
        boolean toggle = false;
        boolean remove = false;
        boolean requirePresent = false;
        int number = 0;

        for (int i = start; i < length; i++) {
            char c = effectStr.charAt(i);

            if (c >= 48 && c <= 57) {
                number *= 10;
                number += c - 48;
                hasNumber = true;
            } else if (c == 33) {
                if (hasNumber) {
                    damage = brewBitOperations(damage, number, remove, toggle, requirePresent);
                    requirePresent = false;
                    toggle = false;
                    remove = false;
                    hasNumber = false;
                    number = 0;
                }

                toggle = true;
            } else if (c == 45) {
                if (hasNumber) {
                    damage = brewBitOperations(damage, number, remove, toggle, requirePresent);
                    requirePresent = false;
                    toggle = false;
                    remove = false;
                    hasNumber = false;
                    number = 0;
                }

                remove = true;
            } else if (c == 43) {
                if (hasNumber) {
                    damage = brewBitOperations(damage, number, remove, toggle, requirePresent);
                    requirePresent = false;
                    toggle = false;
                    remove = false;
                    hasNumber = false;
                    number = 0;
                }
            } else if (c == 38) {
                if (hasNumber) {
                    damage = brewBitOperations(damage, number, remove, toggle, requirePresent);
                    requirePresent = false;
                    toggle = false;
                    remove = false;
                    hasNumber = false;
                    number = 0;
                }

                requirePresent = true;
            }
        }

        if (hasNumber) {
            damage = brewBitOperations(damage, number, remove, toggle, requirePresent);
        }

        return damage & 32767;
    }

    // 1.5.2 PotionHelper.func_77908_a — packs five damage bits into an index.
    public static int func_77908_a(int damage, int bit4, int bit3, int bit2, int bit1, int bit0) {
        return (checkFlag(damage, bit4) ? 16 : 0)
             | (checkFlag(damage, bit3) ? 8 : 0)
             | (checkFlag(damage, bit2) ? 4 : 0)
             | (checkFlag(damage, bit1) ? 2 : 0)
             | (checkFlag(damage, bit0) ? 1 : 0);
    }

    // 1.5.2 PotionHelper static tables, verbatim (requirement/amplifier strings
    // and ingredient effect strings are data). Requires
    // Potion.initializeVanillaPotions() to have run (BTWLifecycle Step 2c).
    static {
        potionRequirements.put(Integer.valueOf(Potion.regeneration.getId()), "0 & !1 & !2 & !3 & 0+6");
        sugarEffect = "-0+1-2-3&4-4+13";
        potionRequirements.put(Integer.valueOf(Potion.moveSpeed.getId()), "!0 & 1 & !2 & !3 & 1+6");
        magmaCreamEffect = "+0+1-2-3&4-4+13";
        potionRequirements.put(Integer.valueOf(Potion.fireResistance.getId()), "0 & 1 & !2 & !3 & 0+6");
        speckledMelonEffect = "+0-1+2-3&4-4+13";
        potionRequirements.put(Integer.valueOf(Potion.heal.getId()), "0 & !1 & 2 & !3");
        spiderEyeEffect = "-0-1+2-3&4-4+13";
        potionRequirements.put(Integer.valueOf(Potion.poison.getId()), "!0 & !1 & 2 & !3 & 2+6");
        fermentedSpiderEyeEffect = "-0+3-4+13";
        potionRequirements.put(Integer.valueOf(Potion.weakness.getId()), "!0 & !1 & !2 & 3 & 3+6");
        potionRequirements.put(Integer.valueOf(Potion.harm.getId()), "!0 & !1 & 2 & 3");
        potionRequirements.put(Integer.valueOf(Potion.moveSlowdown.getId()), "!0 & 1 & !2 & 3 & 3+6");
        blazePowderEffect = "+0-1-2+3&4-4+13";
        potionRequirements.put(Integer.valueOf(Potion.damageBoost.getId()), "0 & !1 & !2 & 3 & 3+6");
        goldenCarrotEffect = "-0+1+2-3+13&4-4";
        potionRequirements.put(Integer.valueOf(Potion.nightVision.getId()), "!0 & 1 & 2 & !3 & 2+6");
        potionRequirements.put(Integer.valueOf(Potion.invisibility.getId()), "!0 & 1 & 2 & 3 & 2+6");
        glowstoneEffect = "+5-6-7";
        potionAmplifiers.put(Integer.valueOf(Potion.moveSpeed.getId()), "5");
        potionAmplifiers.put(Integer.valueOf(Potion.digSpeed.getId()), "5");
        potionAmplifiers.put(Integer.valueOf(Potion.damageBoost.getId()), "5");
        potionAmplifiers.put(Integer.valueOf(Potion.regeneration.getId()), "5");
        potionAmplifiers.put(Integer.valueOf(Potion.harm.getId()), "5");
        potionAmplifiers.put(Integer.valueOf(Potion.heal.getId()), "5");
        potionAmplifiers.put(Integer.valueOf(Potion.resistance.getId()), "5");
        potionAmplifiers.put(Integer.valueOf(Potion.poison.getId()), "5");
        redstoneEffect = "-5+6-7";
        gunpowderEffect = "+14&13-13";
        field_77925_n = new HashMap();
        potionPrefixes = new String[] {"potion.prefix.mundane", "potion.prefix.uninteresting", "potion.prefix.bland", "potion.prefix.clear", "potion.prefix.milky", "potion.prefix.diffuse", "potion.prefix.artless", "potion.prefix.thin", "potion.prefix.awkward", "potion.prefix.flat", "potion.prefix.bulky", "potion.prefix.bungling", "potion.prefix.buttered", "potion.prefix.smooth", "potion.prefix.suave", "potion.prefix.debonair", "potion.prefix.thick", "potion.prefix.elegant", "potion.prefix.fancy", "potion.prefix.charming", "potion.prefix.dashing", "potion.prefix.refined", "potion.prefix.cordial", "potion.prefix.sparkling", "potion.prefix.potent", "potion.prefix.foul", "potion.prefix.odorless", "potion.prefix.rank", "potion.prefix.harsh", "potion.prefix.acrid", "potion.prefix.gross", "potion.prefix.stinky"};
    }
}
