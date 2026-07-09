package btw.modern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EnchantmentHelper {

    private static final Random enchantmentRand = new Random();

    public static int getEnchantmentLevel(int enchId, ItemStack stack) {
        if (stack == null) return 0;
        NBTTagList enchList = stack.getEnchantmentTagList();
        if (enchList == null) return 0;

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) enchList.tagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            if (id == enchId) {
                return lvl;
            }
        }
        return 0;
    }

    // 1.5.2 EnchantmentHelper.getEnchantments — enchanted books store their
    // enchants in StoredEnchantments (via ItemEnchantedBook.func_92110_g)
    // instead of the "ench" list.
    public static Map getEnchantments(ItemStack stack) {
        Map<Integer, Integer> map = new java.util.LinkedHashMap<>();
        if (stack == null) return map;
        NBTTagList enchList = stack.itemID == Item.enchantedBook.itemID
                ? Item.enchantedBook.func_92110_g(stack)
                : stack.getEnchantmentTagList();
        if (enchList == null) return map;

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) enchList.tagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            map.put((int) id, (int) lvl);
        }
        return map;
    }

    // 1.5.2 EnchantmentHelper.setEnchantments — vanilla ContainerRepair
    // (anvil enchant-combining) is the caller once that container is ported.
    public static void setEnchantments(Map enchMap, ItemStack stack) {
        NBTTagList enchList = new NBTTagList();

        for (Object key : enchMap.keySet()) {
            int enchId = ((Integer) key).intValue();
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort("id", (short) enchId);
            tag.setShort("lvl", (short) ((Integer) enchMap.get(Integer.valueOf(enchId))).intValue());
            enchList.appendTag(tag);

            if (stack.itemID == Item.enchantedBook.itemID) {
                Item.enchantedBook.func_92115_a(stack, new EnchantmentData(enchId, ((Integer) enchMap.get(Integer.valueOf(enchId))).intValue()));
            }
        }

        if (enchList.tagCount() > 0) {
            if (stack.itemID != Item.enchantedBook.itemID) {
                stack.setTagInfo("ench", enchList);
            }
        } else if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("ench");
        }
    }

    /**
     * Walks every stack in {@code stacks} and returns the highest level of
     * enchantment {@code enchId} found across all of them. Mirrors vanilla
     * 1.5.2 EnchantmentHelper.getMaxEnchantmentLevel.
     */
    public static int getMaxEnchantmentLevel(int enchId, ItemStack[] stacks) {
        if (stacks == null) return 0;
        int max = 0;
        for (ItemStack stack : stacks) {
            int lvl = getEnchantmentLevel(enchId, stack);
            if (lvl > max) max = lvl;
        }
        return max;
    }

    /**
     * Returns the modifier of protection enchantments on armors.
     * Reimplements vanilla 1.5.2 EnchantmentHelper + EnchantmentProtection logic.
     */
    public static int getEnchantmentModifierDamage(ItemStack[] stacks, DamageSource source) {
        if (stacks == null) return 0;

        int totalModifier = 0;

        for (ItemStack stack : stacks) {
            if (stack == null) continue;
            NBTTagList enchList = stack.getEnchantmentTagList();
            if (enchList == null) continue;

            for (int i = 0; i < enchList.tagCount(); i++) {
                NBTTagCompound tag = (NBTTagCompound) enchList.tagAt(i);
                short id = tag.getShort("id");
                short lvl = tag.getShort("lvl");
                totalModifier += calcProtectionModifier(id, lvl, source);
            }
        }

        if (totalModifier > 25) {
            totalModifier = 25;
        }

        return (totalModifier + 1 >> 1) + enchantmentRand.nextInt((totalModifier >> 1) + 1);
    }

    /**
     * Calculates protection modifier for a single enchantment.
     * Mirrors EnchantmentProtection.calcModifierDamage from vanilla 1.5.2.
     */
    private static int calcProtectionModifier(int enchId, int level, DamageSource source) {
        if (source.canHarmInCreative()) return 0;

        // Determine protection type from enchantment ID
        // 0 = protection (all), 1 = fire protection, 2 = feather falling, 3 = blast protection, 4 = projectile protection
        int protectionType;
        if (enchId == 0) protectionType = 0;      // protection
        else if (enchId == 1) protectionType = 1;  // fire protection
        else if (enchId == 2) protectionType = 2;  // feather falling
        else if (enchId == 3) protectionType = 3;  // blast protection
        else if (enchId == 4) protectionType = 4;  // projectile protection
        else return 0; // not a protection enchantment

        float base = (float)(6 + level * level) / 3.0F;
        if (protectionType == 0) return MathHelper.floor_float(base * 0.75F);
        if (protectionType == 1 && source.isFireDamage()) return MathHelper.floor_float(base * 1.25F);
        if (protectionType == 2 && source == DamageSource.fall) return MathHelper.floor_float(base * 2.5F);
        if (protectionType == 3 && source.isExplosion()) return MathHelper.floor_float(base * 1.5F);
        if (protectionType == 4 && source.isProjectile()) return MathHelper.floor_float(base * 1.5F);
        return 0;
    }

    /**
     * Returns the (magic) extra damage of enchantments on the attacker's held item.
     * Reimplements vanilla 1.5.2 EnchantmentHelper + EnchantmentDamage logic.
     */
    public static int getEnchantmentModifierLiving(EntityLiving attacker, EntityLiving target) {
        ItemStack held = attacker.getHeldItem();
        if (held == null) return 0;

        NBTTagList enchList = held.getEnchantmentTagList();
        if (enchList == null) return 0;

        int totalModifier = 0;

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) enchList.tagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            totalModifier += calcDamageModifier(id, lvl, target);
        }

        return totalModifier > 0 ? 1 + enchantmentRand.nextInt(totalModifier) : 0;
    }

    /**
     * Calculates damage modifier for a single enchantment.
     * Mirrors EnchantmentDamage.calcModifierLiving from vanilla 1.5.2.
     */
    private static int calcDamageModifier(int enchId, int level, EntityLiving target) {
        // 16 = sharpness (all), 17 = smite (undead), 18 = bane of arthropods
        if (enchId == 16) return MathHelper.floor_float((float) level * 2.75F);
        if (enchId == 17 && target.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD)
            return MathHelper.floor_float((float) level * 4.5F);
        if (enchId == 18 && target.getCreatureAttribute() == EnumCreatureAttribute.ARTHROPOD)
            return MathHelper.floor_float((float) level * 4.5F);
        return 0;
    }

    // The methods below mirror vanilla 1.5.2 EnchantmentHelper.
    // Earlier versions delegated to per-entity helper methods like
    // entity.getKnockbackEnchantLevel(), but those only existed on
    // Modern-Common's stub EntityLiving — at runtime the real vanilla
    // 1.5.2 EntityLiving is loaded, and it doesn't expose them, so the
    // delegating versions threw NoSuchMethodError every tick.
    // Instead we walk the entity's items directly via getHeldItem() /
    // getLastActiveItems(), which DO exist on real EntityLiving.

    public static int getKnockbackModifier(EntityLiving attacker, EntityLiving target) {
        return getEnchantmentLevel(Enchantment.knockback.effectId, attacker.getHeldItem());
    }

    public static int getFireAspectModifier(EntityLiving entity) {
        return getEnchantmentLevel(Enchantment.fireAspect.effectId, entity.getHeldItem());
    }

    public static int getRespiration(EntityLiving entity) {
        return getMaxEnchantmentLevel(Enchantment.respiration.effectId, entity.getLastActiveItems());
    }

    public static int getEfficiencyModifier(EntityLiving entity) {
        return getEnchantmentLevel(Enchantment.efficiency.effectId, entity.getHeldItem());
    }

    public static int getUnbreakingModifier(EntityLiving entity) {
        return getEnchantmentLevel(Enchantment.unbreaking.effectId, entity.getHeldItem());
    }

    // 1.5.2 EnchantmentHelper.getFortuneModifier (FCMOD: Code change) — takes
    // the max of the held-item enchant and FC's Fortune potion (amplifier+1).
    // FCBetterThanWolves.potionFortune registers at fixed Potion id 31;
    // Modern-Common can't reference the FC class, so look it up by id.
    public static int getFortuneModifier(EntityLiving entity) {
        int enchantmentLevel = getEnchantmentLevel(Enchantment.fortune.effectId, entity.getHeldItem());

        Potion potionFortune = Potion.potionTypes[31];
        if (potionFortune != null && entity.isPotionActive(potionFortune)) {
            int potionLevel = entity.getActivePotionEffect(potionFortune).getAmplifier() + 1;
            if (potionLevel > enchantmentLevel) {
                enchantmentLevel = potionLevel;
            }
        }

        return enchantmentLevel;
    }

    // 1.5.2 EnchantmentHelper.getLootingModifier (FCMOD: Code change) — same
    // max() with FC's Looting potion (FCBetterThanWolves.potionLooting, id 30).
    public static int getLootingModifier(EntityLiving entity) {
        int enchantmentLevel = getEnchantmentLevel(Enchantment.looting.effectId, entity.getHeldItem());

        Potion potionLooting = Potion.potionTypes[30];
        if (potionLooting != null && entity.isPotionActive(potionLooting)) {
            int potionLevel = entity.getActivePotionEffect(potionLooting).getAmplifier() + 1;
            if (potionLevel > enchantmentLevel) {
                enchantmentLevel = potionLevel;
            }
        }

        return enchantmentLevel;
    }

    public static boolean getAquaAffinityModifier(EntityLiving entity) {
        return getMaxEnchantmentLevel(Enchantment.aquaAffinity.effectId, entity.getLastActiveItems()) > 0;
    }

    public static boolean getSilkTouchModifier(EntityLiving entity) {
        // getSilkTouchEnchant is a bridge method and must live on the
        // NON-shadowed EntityPlayer (the runtime EntityLiving is FC's real
        // 1.5.2 class, which doesn't have it — calling it there is a
        // NoSuchMethodError). PlayerBridge overrides it with the modern
        // player's held item. Non-player mobs take the real 1.5.2 path.
        if (entity instanceof EntityPlayer) {
            return ((EntityPlayer) entity).getSilkTouchEnchant();
        }
        return getEnchantmentLevel(Enchantment.silkTouch.effectId, entity.getHeldItem()) > 0;
    }

    // 1.5.2 EnchantmentHelper.func_92098_i — fc EnchantmentThorns.func_92096_a
    // (called from fc EntityMob.attackEntityAsMob, EntityLiving, EntityArrow)
    // reads the highest thorns level across the target's worn items.
    public static int func_92098_i(EntityLiving entity) {
        return getMaxEnchantmentLevel(Enchantment.thorns.effectId, entity.getLastActiveItems());
    }

    // 1.5.2 EnchantmentHelper.func_92099_a — fc EnchantmentThorns.func_92096_a
    // uses this to pick the thorns armor piece that takes durability damage.
    public static ItemStack func_92099_a(Enchantment ench, EntityLiving entity) {
        ItemStack[] items = entity.getLastActiveItems();
        if (items == null) return null;

        for (ItemStack stack : items) {
            if (stack != null && getEnchantmentLevel(ench.effectId, stack) > 0) {
                return stack;
            }
        }

        return null;
    }

    // 1.5.2 EnchantmentHelper.addRandomEnchantment — FCEntitySkeleton spawn gear
    // and FCEntityVillager trade generation. Books convert in place to
    // enchanted books and store enchants via func_92115_a.
    public static ItemStack addRandomEnchantment(Random rand, ItemStack stack, int level) {
        List enchList = buildEnchantmentList(rand, stack, level);
        boolean isBook = stack.itemID == Item.book.itemID;

        if (isBook) {
            stack.itemID = Item.enchantedBook.itemID;
        }

        if (enchList != null) {
            for (Object o : enchList) {
                EnchantmentData data = (EnchantmentData) o;

                if (isBook) {
                    Item.enchantedBook.func_92115_a(stack, data);
                } else {
                    stack.addEnchantment(data.enchantmentobj, data.enchantmentLevel);
                }
            }
        }

        return stack;
    }

    // 1.5.2 EnchantmentHelper.buildEnchantmentList — enchantability fuzzing,
    // weighted pick via WeightedRandom.getRandomItem, then the halving-level
    // extra-enchant loop filtered by canApplyTogether. Also called by
    // ContainerEnchantment.enchantItem.
    public static List buildEnchantmentList(Random rand, ItemStack stack, int level) {
        Item item = stack.getItem();
        int enchantability = item.getItemEnchantability();

        if (enchantability <= 0) {
            return null;
        } else {
            enchantability /= 2;
            enchantability = 1 + rand.nextInt((enchantability >> 1) + 1) + rand.nextInt((enchantability >> 1) + 1);
            int baseLevel = enchantability + level;
            float fuzz = (rand.nextFloat() + rand.nextFloat() - 1.0F) * 0.15F;
            int modifiedLevel = (int)((float)baseLevel * (1.0F + fuzz) + 0.5F);

            if (modifiedLevel < 1) {
                modifiedLevel = 1;
            }

            java.util.ArrayList result = null;
            Map possibleEnchants = mapEnchantmentData(modifiedLevel, stack);

            if (possibleEnchants != null && !possibleEnchants.isEmpty()) {
                EnchantmentData picked = (EnchantmentData) WeightedRandom.getRandomItem(rand, possibleEnchants.values());

                if (picked != null) {
                    result = new java.util.ArrayList();
                    result.add(picked);

                    for (int remainingLevel = modifiedLevel; rand.nextInt(50) <= remainingLevel; remainingLevel >>= 1) {
                        java.util.Iterator idIterator = possibleEnchants.keySet().iterator();

                        while (idIterator.hasNext()) {
                            Integer enchId = (Integer) idIterator.next();
                            boolean compatible = true;
                            java.util.Iterator pickedIterator = result.iterator();

                            while (true) {
                                if (pickedIterator.hasNext()) {
                                    EnchantmentData existing = (EnchantmentData) pickedIterator.next();

                                    if (existing.enchantmentobj.canApplyTogether(Enchantment.enchantmentsList[enchId.intValue()])) {
                                        continue;
                                    }

                                    compatible = false;
                                }

                                if (!compatible) {
                                    idIterator.remove();
                                }

                                break;
                            }
                        }

                        if (!possibleEnchants.isEmpty()) {
                            EnchantmentData extra = (EnchantmentData) WeightedRandom.getRandomItem(rand, possibleEnchants.values());
                            result.add(extra);
                        }
                    }
                }
            }

            return result;
        }
    }

    // 1.5.2 EnchantmentHelper.mapEnchantmentData — builds the map of possible
    // EnchantmentData for the enchantability level, honoring the item's
    // IsEnchantmentApplicable (FCMOD: Changed) and skipping enchants FC bans
    // from the vanilla enchanter (FCMOD: Added).
    public static Map mapEnchantmentData(int level, ItemStack stack) {
        Item item = stack.getItem();
        HashMap map = null;
        boolean isBook = stack.itemID == Item.book.itemID;

        for (Enchantment ench : Enchantment.enchantmentsList) {
            // FCMOD: Changed — vanilla used ench.type.canEnchantItem(item)
            if (ench != null && (item.IsEnchantmentApplicable(ench) || isBook)) {
            // END FCMOD
                // FCMOD: Added to prevent certain enchants from vanilla enchanter
                if (!ench.CanBeAppliedByVanillaEnchanter()) {
                    continue;
                }
                // END FCMOD

                for (int enchLevel = ench.getMinLevel(); enchLevel <= ench.getMaxLevel(); ++enchLevel) {
                    if (level >= ench.getMinEnchantability(enchLevel) && level <= ench.getMaxEnchantability(enchLevel)) {
                        if (map == null) {
                            map = new HashMap();
                        }

                        map.put(Integer.valueOf(ench.effectId), new EnchantmentData(ench, enchLevel));
                    }
                }
            }
        }

        return map;
    }

    // 1.5.2 EnchantmentHelper.calcItemStackEnchantability (FCMOD: Code added —
    // this FC replacement is the ground truth, NOT the commented-out vanilla
    // formula): doubles the bookshelf requirement, caps enchant level at 15,
    // and makes the no-shelf enchant level 1. Caller:
    // ContainerEnchantment.onCraftMatrixChanged.
    public static int calcItemStackEnchantability(Random rand, int tableSlotNum, int numBookShelves, ItemStack stack) {
        Item item = stack.getItem();
        int itemEnchantability = item.getItemEnchantability();

        if (itemEnchantability <= 0) {
            return 0;
        } else {
            int enchantmentLevel = 1;

            if (tableSlotNum != 0) {
                int maxEnchantmentLevel = numBookShelves >> 1;

                if (maxEnchantmentLevel <= 0) {
                    maxEnchantmentLevel = 1;
                } else if (maxEnchantmentLevel > 15) {
                    maxEnchantmentLevel = 15;
                }

                if (tableSlotNum == 1) {
                    if (maxEnchantmentLevel > 1) {
                        enchantmentLevel = 2;

                        if (maxEnchantmentLevel > 3) {
                            enchantmentLevel += rand.nextInt(maxEnchantmentLevel - 2);
                        }
                    }
                } else {
                    enchantmentLevel = maxEnchantmentLevel;
                }
            }

            return enchantmentLevel;
        }
    }
}
