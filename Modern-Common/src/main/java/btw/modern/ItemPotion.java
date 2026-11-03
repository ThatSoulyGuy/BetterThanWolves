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

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemPotion's icon members (vanilla/client ItemPotion.java:29-31,
    // :182-193, :391-401): the drinkable or splash bottle by damage, the contents overlay of the first pass, and the
    // static lookup RenderSnowball reads for a thrown potion. FCItemPotion (373) inherits them; a held splash potion
    // drew Item's default, the unlocalized "potion", the drinkable bottle.
    private Icon splashIcon;
    private Icon drinkableIcon;
    private Icon contentsIcon;

    // 1.5.2 ItemPotion.getIconFromDamage (vanilla/client ItemPotion.java:182-185); Item's icon until registerIcons
    // runs (client only)
    @Override
    public Icon getIconFromDamage(int damage) {
        if (this.splashIcon == null || this.drinkableIcon == null) {
            return super.getIconFromDamage(damage);
        }
        return isSplash(damage) ? this.splashIcon : this.drinkableIcon;
    }

    // 1.5.2 ItemPotion.getIconFromDamageForRenderPass (vanilla/client ItemPotion.java:190-193); Item's answer while the
    // contents icon is not registered (client only)
    @Override
    public Icon getIconFromDamageForRenderPass(int damage, int renderPass) {
        return renderPass == 0 && this.contentsIcon != null ? this.contentsIcon
                : super.getIconFromDamageForRenderPass(damage, renderPass);
    }

    // 1.5.2 ItemPotion.registerIcons (vanilla/client ItemPotion.java:391-396)
    @Override
    public void registerIcons(IconRegister register) {
        this.drinkableIcon = register.registerIcon("potion");
        this.splashIcon = register.registerIcon("potion_splash");
        this.contentsIcon = register.registerIcon("potion_contents");
    }

    // 1.5.2 ItemPotion.func_94589_d (vanilla/client ItemPotion.java:398-401): Item.potion's icons by name (compared by
    // value; 1.5.2's callers pass these literals)
    public static Icon func_94589_d(String name) {
        ItemPotion potion = Item.potion;
        if (potion == null) {
            return null;
        }
        return "potion".equals(name) ? potion.drinkableIcon
                : ("potion_splash".equals(name) ? potion.splashIcon
                : ("potion_contents".equals(name) ? potion.contentsIcon : null));
    }
}
