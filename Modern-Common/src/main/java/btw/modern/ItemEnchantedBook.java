package btw.modern;

import java.util.List;

public class ItemEnchantedBook extends Item {

    public ItemEnchantedBook(int id) {
        super(id);
        this.maxStackSize = 1;
    }

    // 1.5.2 ItemEnchantedBook.hasEffect — books with stored enchants glint.
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    // 1.5.2 ItemEnchantedBook.isItemTool
    public boolean isItemTool(ItemStack stack) {
        return false;
    }

    // 1.5.2 ItemEnchantedBook.func_92110_g — reads the StoredEnchantments list;
    // EnchantmentHelper.getEnchantments branches through here for books.
    public NBTTagList func_92110_g(ItemStack stack) {
        return stack.stackTagCompound != null && stack.stackTagCompound.hasKey("StoredEnchantments")
                ? (NBTTagList) stack.stackTagCompound.getTag("StoredEnchantments")
                : new NBTTagList();
    }

    // 1.5.2 ItemEnchantedBook.addInformation — tooltip lists the stored enchants.
    public void addInformation(ItemStack stack, EntityPlayer player, List infoList, boolean advancedToolTips) {
        super.addInformation(stack, player, infoList, advancedToolTips);
        NBTTagList storedEnchants = this.func_92110_g(stack);

        if (storedEnchants != null) {
            for (int i = 0; i < storedEnchants.tagCount(); i++) {
                short id = ((NBTTagCompound) storedEnchants.tagAt(i)).getShort("id");
                short lvl = ((NBTTagCompound) storedEnchants.tagAt(i)).getShort("lvl");

                if (Enchantment.enchantmentsList[id] != null) {
                    infoList.add(Enchantment.enchantmentsList[id].getTranslatedName(lvl));
                }
            }
        }
    }

    // 1.5.2 ItemEnchantedBook.func_92115_a — stores an EnchantmentData in the
    // StoredEnchantments NBT list, upgrading the level of an existing entry.
    // Callers: EnchantmentHelper.addRandomEnchantment/setEnchantments and
    // ContainerEnchantment.enchantItem (book branch).
    public void func_92115_a(ItemStack stack, EnchantmentData data) {
        NBTTagList storedEnchants = this.func_92110_g(stack);
        boolean isNewEnchant = true;

        for (int i = 0; i < storedEnchants.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) storedEnchants.tagAt(i);

            if (tag.getShort("id") == data.enchantmentobj.effectId) {
                if (tag.getShort("lvl") < data.enchantmentLevel) {
                    tag.setShort("lvl", (short) data.enchantmentLevel);
                }

                isNewEnchant = false;
                break;
            }
        }

        if (isNewEnchant) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort("id", (short) data.enchantmentobj.effectId);
            tag.setShort("lvl", (short) data.enchantmentLevel);
            storedEnchants.appendTag(tag);
        }

        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        stack.getTagCompound().setTag("StoredEnchantments", storedEnchants);
    }

    // 1.5.2 ItemEnchantedBook.func_92111_a — creates a book with one stored enchant.
    public ItemStack func_92111_a(EnchantmentData data) {
        ItemStack stack = new ItemStack(this.itemID, 1, 0);
        this.func_92115_a(stack, data);
        return stack;
    }

    // 1.5.2 ItemEnchantedBook.func_92113_a — one book per level of the enchantment.
    public void func_92113_a(Enchantment enchantment, List list) {
        for (int level = enchantment.getMinLevel(); level <= enchantment.getMaxLevel(); level++) {
            list.add(this.func_92111_a(new EnchantmentData(enchantment, level)));
        }
    }
}
