package btw.modern;

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

    public int getItemID() {
        return this.itemID;
    }

    // 1.5.2 ItemStack.splitStack — Container.slotClick/FCContainerMenu split paths;
    // vanilla deep-copies the NBT (stackTagCompound.copy()) so the halves never alias.
    public ItemStack splitStack(int amount) {
        ItemStack result = new ItemStack(this.itemID, amount, this.itemDamage);
        if (this.stackTagCompound != null) {
            result.stackTagCompound = (NBTTagCompound) this.stackTagCompound.copy();
        }
        this.stackSize -= amount;
        return result;
    }

    public ItemStack copy() {
        ItemStack result = new ItemStack(this.itemID, this.stackSize, this.itemDamage);
        if (this.stackTagCompound != null) {
            result.stackTagCompound = (NBTTagCompound) this.stackTagCompound.copy();
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
        // Check for custom name in NBT display tag
        if (this.stackTagCompound != null && this.stackTagCompound.hasKey("display")) {
            NBTTagCompound display = this.stackTagCompound.getCompoundTag("display");
            if (display.hasKey("Name")) {
                return display.getString("Name");
            }
        }
        Item item = getItem();
        return item != null ? item.getItemDisplayName(this) : "";
    }

    public void setItemName(String name) {
        if (this.stackTagCompound == null) {
            this.stackTagCompound = new NBTTagCompound();
        }
        if (!this.stackTagCompound.hasKey("display")) {
            this.stackTagCompound.setCompoundTag("display", new NBTTagCompound());
        }
        this.stackTagCompound.getCompoundTag("display").setString("Name", name);
    }

    public boolean hasDisplayName() {
        if (this.stackTagCompound != null && this.stackTagCompound.hasKey("display")) {
            NBTTagCompound display = this.stackTagCompound.getCompoundTag("display");
            return display.hasKey("Name");
        }
        return false;
    }

    public String getItemName() {
        Item item = getItem();
        return item != null ? item.getUnlocalizedName(this) : "";
    }

    // --- Enchantment ---

    public boolean isItemEnchantable() {
        if (this.stackSize != 1) return false;
        Item item = getItem();
        return item != null && item.getItemEnchantability() > 0 && !isItemEnchanted();
    }

    public boolean isItemEnchanted() {
        return this.stackTagCompound != null && this.stackTagCompound.hasKey("ench");
    }

    public void addEnchantment(Enchantment enchantment, int level) {
        if (this.stackTagCompound == null) {
            this.stackTagCompound = new NBTTagCompound();
        }
        if (!this.stackTagCompound.hasKey("ench")) {
            this.stackTagCompound.setTag("ench", new NBTTagList());
        }
        NBTTagList enchList = this.stackTagCompound.getTagList("ench");
        NBTTagCompound enchTag = new NBTTagCompound();
        enchTag.setShort("id", (short) enchantment.effectId);
        enchTag.setShort("lvl", (short) level);
        enchList.appendTag(enchTag);
    }

    public void setTagInfo(String key, NBTBase nbtBase) {
        if (this.stackTagCompound == null) {
            this.stackTagCompound = new NBTTagCompound();
        }
        this.stackTagCompound.setTag(key, nbtBase);
    }

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
        Item item = getItem();
        return item != null ? item.getMaxItemUseDuration(this) : 0;
    }

    public EnumAction getItemUseAction() {
        Item item = getItem();
        return item != null ? item.getItemUseAction(this) : EnumAction.none;
    }

    // --- World interaction ---

    public boolean tryPlaceItemIntoWorld(EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Item item = getItem();
        if (item != null) {
            return item.onItemUse(this, player, world, x, y, z, side, hitX, hitY, hitZ);
        }
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

    /**
     * FC's item damage system. Applies damage to the item, accounting for
     * unbreaking enchantment and creative mode bypass.
     * FC changes: items break at damage >= maxDamage (not > maxDamage).
     */
    public void damageItem(int amount, EntityLiving entity) {
        if (entity instanceof EntityPlayer ep) {
            if (ep.capabilities != null && ep.capabilities.isCreativeMode) return;
        }

        if (this.isItemStackDamageable()) {
            // Apply unbreaking enchantment reduction
            int unbreaking = EnchantmentHelper.getUnbreakingModifier(entity);
            if (unbreaking > 0) {
                int reduced = 0;
                java.util.Random rand = new java.util.Random();
                for (int i = 0; i < amount; i++) {
                    // Each point of damage has a chance to be negated
                    if (rand.nextInt(unbreaking + 1) > 0) {
                        reduced++;
                    }
                }
                amount -= reduced;
                if (amount <= 0) return;
            }

            this.itemDamage += amount;

            // FC: items break at damage >= maxDamage (not > maxDamage)
            if (this.itemDamage >= this.getMaxDamage()) {
                --this.stackSize;
                if (this.stackSize < 0) {
                    this.stackSize = 0;
                }
                this.itemDamage = 0;
            }
        }
    }

    /**
     * Attempts to damage the item by the given amount, applying unbreaking
     * enchantment chances via the provided random. Returns true if the item
     * should break (durability exhausted).
     */
    public boolean attemptDamageItem(int amount, java.util.Random random) {
        if (!this.isItemStackDamageable()) {
            return false;
        }
        // Apply unbreaking-style reduction using the provided random
        // (No enchant check here — caller is responsible for passing adjusted amount)
        this.itemDamage += amount;
        return this.itemDamage >= this.getMaxDamage();
    }

    public void onBlockDestroyed(World world, int blockID, int x, int y, int z, EntityPlayer player) {
        if (this.getItem() != null) {
            boolean used = this.getItem().onBlockDestroyed(this, world, blockID, x, y, z, player);
            // Stats tracking not bridged yet — FC calls addStat for tool use statistics
        }
    }

    public void hitEntity(EntityLiving target, EntityPlayer player) {
        Item item = getItem();
        if (item != null) {
            boolean used = item.hitEntity(this, target, player);
        }
    }

    public boolean interactWith(EntityLiving livingEntity) {
        Item item = getItem();
        return item != null && item.useItemOnEntity(this, livingEntity);
    }

    public int getDamageVsEntity(Entity entity) {
        Item item = getItem();
        return item != null ? item.getDamageVsEntity(entity) : 0;
    }

    public boolean canHarvestBlock(Block block) {
        Item item = getItem();
        return item != null && item.canHarvestBlock(block);
    }

    public void updateAnimation(World world, EntityPlayer player, int inventorySlot, boolean isHeld) {
        Item item = getItem();
        if (item != null) {
            item.onUpdate(this, world, player, inventorySlot, isHeld);
        }
    }

    public void onCrafting(World world, EntityPlayer player, int amount) {
        Item item = getItem();
        if (item != null) {
            item.onCreated(this, world, player);
        }
    }

    public void onPlayerStoppedUsing(World world, EntityPlayer player, int ticksRemaining) {
        Item item = getItem();
        if (item != null) {
            item.onPlayerStoppedUsing(this, world, player, ticksRemaining);
        }
    }

    public float getStrVsBlock(Block block) {
        if (this.getItem() != null) {
            return this.getItem().getStrVsBlock(this, block);
        }
        return 1.0F;
    }

    public float getStrVsBlock(World world, Block block, int i, int j, int k) {
        if (this.getItem() != null) {
            return this.getItem().getStrVsBlock(this, world, block, i, j, k);
        }
        return 1.0F;
    }

    // 1.5.2 (FCMOD) ItemStack.canHarvestBlock — EntityPlayer/InventoryPlayer.canHarvestBlock
    // harvest check; must hit the positional overload FC tools override (FCItemPickaxe etc.),
    // not the single-arg base that always returns false.
    public boolean canHarvestBlock(World world, Block block, int i, int j, int k) {
        if (this.getItem() != null) {
            return this.getItem().canHarvestBlock(this, world, block, i, j, k);
        }
        return false;
    }

    // --- NBT serialization ---

    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        tagCompound.setShort("id", (short) this.itemID);
        tagCompound.setByte("Count", (byte) this.stackSize);
        tagCompound.setShort("Damage", (short) this.itemDamage);

        if (this.stackTagCompound != null) {
            tagCompound.setTag("tag", this.stackTagCompound);
        }

        return tagCompound;
    }

    public void readFromNBT(NBTTagCompound tagCompound) {
        this.itemID = tagCompound.getShort("id");
        this.stackSize = tagCompound.getByte("Count");
        this.itemDamage = tagCompound.getShort("Damage");

        if (tagCompound.hasKey("tag")) {
            this.stackTagCompound = tagCompound.getCompoundTag("tag");
        }
    }

    public static ItemStack loadItemStackFromNBT(NBTTagCompound tagCompound) {
        ItemStack stack = new ItemStack();
        stack.readFromNBT(tagCompound);
        if (stack.getItem() != null) {
            return stack;
        }
        return null;
    }

    // --- Client-side rendering methods ---

    public int getItemSpriteNumber() {
        Item item = getItem();
        return item != null ? item.getItemSpriteNumber() : 1;
    }

    // 1.5.2 ItemStack.getIconIndex — RenderItem.doRenderItem flat-item path.
    public Icon getIconIndex() {
        Item item = getItem();
        return item != null ? item.getIconIndex(this) : null;
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
