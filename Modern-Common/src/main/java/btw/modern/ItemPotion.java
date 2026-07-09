package btw.modern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ItemPotion extends Item {

    /** 1.5.2 ItemPotion.effectCache — maps potion damage values to effect lists. */
    private HashMap effectCache = new HashMap();

    public ItemPotion(int id) {
        super(id);
    }

    public static boolean isSplash(int metadata) {
        return (metadata & 16384) != 0;
    }

    /**
     * 1.5.2 ItemPotion.getEffects(ItemStack) — CustomPotionEffects NBT branch
     * plus the cached PotionHelper damage-bits parse. Caller:
     * FCItemPotion.onEaten (drinking any potion) and EntityPotion.onImpact.
     */
    public List getEffects(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("CustomPotionEffects")) {
            ArrayList customEffects = new ArrayList();
            NBTTagList effectList = stack.getTagCompound().getTagList("CustomPotionEffects");

            for (int i = 0; i < effectList.tagCount(); i++) {
                NBTTagCompound tag = (NBTTagCompound) effectList.tagAt(i);
                customEffects.add(PotionEffect.readCustomPotionEffectFromNBT(tag));
            }

            return customEffects;
        } else {
            List effects = (List) this.effectCache.get(Integer.valueOf(stack.getItemDamage()));

            if (effects == null) {
                effects = PotionHelper.getPotionEffects(stack.getItemDamage(), false);
                this.effectCache.put(Integer.valueOf(stack.getItemDamage()), effects);
            }

            return effects;
        }
    }

    /**
     * 1.5.2 ItemPotion.getEffects(int) — effect list for a damage value.
     */
    public List getEffects(int damage) {
        List effects = (List) this.effectCache.get(Integer.valueOf(damage));

        if (effects == null) {
            effects = PotionHelper.getPotionEffects(damage, false);
            this.effectCache.put(Integer.valueOf(damage), effects);
        }

        return effects;
    }

    // 1.5.2 ItemPotion.getColorFromDamage — potion liquid tint via PotionHelper.
    public int getColorFromDamage(int damage) {
        return PotionHelper.func_77915_a(damage, false);
    }

    // 1.5.2 ItemPotion.isEffectInstant — true if any effect is instant
    // (heal/harm); used by render/name code.
    public boolean isEffectInstant(int damage) {
        List effects = this.getEffects(damage);

        if (effects != null && !effects.isEmpty()) {
            Iterator it = effects.iterator();
            PotionEffect effect;

            do {
                if (!it.hasNext()) {
                    return false;
                }

                effect = (PotionEffect) it.next();
            }
            while (!Potion.potionTypes[effect.getPotionID()].isInstant());

            return true;
        } else {
            return false;
        }
    }

    // 1.5.2 ItemPotion.hasEffect — glint when the potion has any effect.
    public boolean hasEffect(ItemStack stack) {
        List effects = this.getEffects(stack);
        return effects != null && !effects.isEmpty();
    }
}
