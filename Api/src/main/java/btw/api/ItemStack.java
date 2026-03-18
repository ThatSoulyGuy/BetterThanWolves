package btw.api;

/**
 * Concrete representation of an item stack.
 * Mirrors net.minecraft.src.ItemStack with identical field/method names.
 * This is a CONCRETE class because FC code does `new ItemStack(...)`.
 */
public class ItemStack {

    // --- Instance fields ---
    public int stackSize;
    public int animationsToGo;
    public int itemID;
    public NBTTagCompound stackTagCompound;

    private int itemDamage;

    // --- Constructors ---

    public ItemStack(Block block) {
        this(block, 1);
    }

    public ItemStack(Block block, int stackSize) {
        this(block.blockID, stackSize, 0);
    }

    public ItemStack(Block block, int stackSize, int damage) {
        this(block.blockID, stackSize, damage);
    }

    public ItemStack(Item item) {
        this(item.itemID, 1, 0);
    }

    public ItemStack(Item item, int stackSize) {
        this(item.itemID, stackSize, 0);
    }

    public ItemStack(Item item, int stackSize, int damage) {
        this(item.itemID, stackSize, damage);
    }

    public ItemStack(int itemID, int stackSize, int damage) {
        this.stackSize = 0;
        this.itemID = itemID;
        this.stackSize = stackSize;
        this.itemDamage = damage;

        if (this.itemDamage < 0) {
            this.itemDamage = 0;
        }
    }

    // package-private for loadItemStackFromNBT
    ItemStack() {
        this.stackSize = 0;
    }

    // --- Core methods ---

    public Item getItem() {
        if (this.itemID >= 0 && this.itemID < Item.itemsList.length) {
            return Item.itemsList[this.itemID];
        }
        return null;
    }

    public ItemStack splitStack(int amount) {
        ItemStack result = new ItemStack(this.itemID, amount, this.itemDamage);
        if (this.stackTagCompound != null) {
            result.stackTagCompound = this.stackTagCompound;
        }
        this.stackSize -= amount;
        return result;
    }

    public ItemStack copy() {
        ItemStack result = new ItemStack(this.itemID, this.stackSize, this.itemDamage);
        if (this.stackTagCompound != null) {
            // Tag compound copying deferred to backend
            result.stackTagCompound = this.stackTagCompound;
        }
        return result;
    }

    // --- Damage ---

    public int getItemDamage() {
        return this.itemDamage;
    }

    public void setItemDamage(int damage) {
        this.itemDamage = damage;
    }

    public int getItemDamageForDisplay() {
        return this.itemDamage;
    }

    public int getMaxDamage() {
        Item item = getItem();
        return item != null ? item.getMaxDamage() : 0;
    }

    public boolean isItemDamaged() {
        return this.itemDamage > 0;
    }

    public boolean isItemStackDamageable() {
        Item item = getItem();
        return item != null && item.getMaxDamage() > 0 && !this.getHasSubtypes();
    }

    public void damageItem(int amount, EntityLiving livingEntity) {}

    // --- Stack properties ---

    public int getMaxStackSize() {
        Item item = getItem();
        return item != null ? item.getItemStackLimit() : 64;
    }

    public boolean isStackable() {
        return this.getMaxStackSize() > 1 && (!this.isItemStackDamageable() || !this.isItemDamaged());
    }

    public boolean getHasSubtypes() {
        Item item = getItem();
        return item != null && item.getHasSubtypes();
    }

    // --- NBT tag compound ---

    public boolean hasTagCompound() {
        return this.stackTagCompound != null;
    }

    public NBTTagCompound getTagCompound() {
        return this.stackTagCompound;
    }

    public void setTagCompound(NBTTagCompound tagCompound) {
        this.stackTagCompound = tagCompound;
    }

    public NBTTagList getEnchantmentTagList() {
        if (this.stackTagCompound != null && this.stackTagCompound.hasKey("ench")) {
            return this.stackTagCompound.getTagList("ench");
        }
        return null;
    }

    // --- Display ---

    public String getDisplayName() {
        return "";
    }

    public void setItemName(String name) {}

    public boolean hasDisplayName() {
        return false;
    }

    public String getItemName() {
        return "";
    }

    // --- Enchantment ---

    public boolean isItemEnchantable() {
        return false;
    }

    public boolean isItemEnchanted() {
        return false;
    }

    public void addEnchantment(Enchantment enchantment, int level) {}

    public void setTagInfo(String key, NBTBase nbtBase) {}

    // --- Comparison ---

    public static boolean areItemStackTagsEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null && stack2 == null) return true;
        if (stack1 == null || stack2 == null) return false;
        if (stack1.stackTagCompound == null && stack2.stackTagCompound == null) return true;
        if (stack1.stackTagCompound == null || stack2.stackTagCompound == null) return false;
        return stack1.stackTagCompound.equals(stack2.stackTagCompound);
    }

    public static boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null && stack2 == null) return true;
        if (stack1 == null || stack2 == null) return false;
        return stack1.isItemEqual(stack2) && stack1.stackSize == stack2.stackSize;
    }

    public boolean isItemEqual(ItemStack other) {
        return this.itemID == other.itemID && this.itemDamage == other.itemDamage;
    }

    public static ItemStack copyItemStack(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    // --- Use actions ---

    public int getMaxItemUseDuration() {
        return 0;
    }

    public EnumAction getItemUseAction() {
        return EnumAction.none;
    }

    // --- World interaction ---

    public boolean tryPlaceItemIntoWorld(EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return false;
    }

    public ItemStack useItemRightClick(World world, EntityPlayer player) {
        Item item = getItem();
        if (item != null) {
            return item.onItemRightClick(this, world, player);
        }
        return this;
    }

    public ItemStack onFoodEaten(World world, EntityPlayer player) {
        Item item = getItem();
        if (item != null) {
            return item.onEaten(this, world, player);
        }
        return this;
    }

    public void onBlockDestroyed(World world, int blockID, int x, int y, int z, EntityPlayer player) {}

    public void hitEntity(EntityLiving target, EntityPlayer player) {}

    public boolean interactWith(EntityLiving livingEntity) {
        return false;
    }

    public int getDamageVsEntity(Entity entity) {
        return 0;
    }

    public boolean canHarvestBlock(Block block) {
        return false;
    }

    public void updateAnimation(World world, EntityPlayer player, int inventorySlot, boolean isHeld) {}

    public void onCrafting(World world, EntityPlayer player, int amount) {}

    public void onPlayerStoppedUsing(World world, EntityPlayer player, int ticksRemaining) {}

    public float getStrVsBlock(Block block) {
        return 1.0F;
    }

    public float getStrVsBlock(World world, Block block, int i, int j, int k) {
        return 1.0F;
    }

    // --- NBT serialization ---

    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        return tagCompound;
    }

    public void readFromNBT(NBTTagCompound tagCompound) {}

    public static ItemStack loadItemStackFromNBT(NBTTagCompound tagCompound) {
        return null;
    }

    // --- Client-side rendering methods ---

    public int getItemSpriteNumber() {
        return 1;
    }

    // --- BTW-added methods ---

    public long GetTimeOfLastUse() {
        if (hasTagCompound() && this.stackTagCompound.hasKey("fcLastUse")) {
            return stackTagCompound.getLong("fcLastUse");
        }
        return -1;
    }

    public void SetTimeOfLastUse(long time) {
        if (!hasTagCompound()) {
            stackTagCompound = new NBTTagCompound();
        }
        stackTagCompound.setLong("fcLastUse", time);
    }

    public float GetAccumulatedChance(float fDefault) {
        if (hasTagCompound() && this.stackTagCompound.hasKey("fcChance")) {
            return stackTagCompound.getFloat("fcChance");
        }
        return fDefault;
    }

    public void SetAccumulatedChance(float fChance) {
        if (!hasTagCompound()) {
            stackTagCompound = new NBTTagCompound();
        }
        stackTagCompound.setFloat("fcChance", fChance);
    }

    @Override
    public String toString() {
        return this.stackSize + "x" + this.itemID + "@" + this.itemDamage;
    }
}
