package btw.modern;

public class EntityItem extends Entity {

    public int age;
    public int delayBeforeCanPickup;
    public float hoverStart;
    public int health = 5;
    private ItemStack entityItem;

    public EntityItem(World world) {
        super(world);
        this.hoverStart = (float) (Math.random() * Math.PI * 2.0D);
        this.setSize(0.25F, 0.25F);
        this.yOffset = this.height / 2.0F;
    }

    public EntityItem(World world, double x, double y, double z, ItemStack stack) {
        this(world);
        this.setPosition(x, y, z);
        this.rotationYaw = (float) (Math.random() * 360.0D);
        this.motionX = (double) ((float) (Math.random() * 0.2D - 0.1D));
        this.motionY = 0.2D;
        this.motionZ = (double) ((float) (Math.random() * 0.2D - 0.1D));
        this.entityItem = stack;
    }

    public void entityInit() {}

    public void onUpdate() {
        super.onUpdate();

        if (this.delayBeforeCanPickup > 0) {
            --this.delayBeforeCanPickup;
        }

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        // Apply gravity
        this.motionY -= 0.04D;

        // Move the entity
        this.moveEntity(this.motionX, this.motionY, this.motionZ);

        // Apply friction
        float friction = 0.98F;
        if (this.onGround) {
            friction = 0.59F; // 0.6 * 0.98 approximation
        }

        this.motionX *= (double) friction;
        this.motionY *= 0.98D;
        this.motionZ *= (double) friction;

        if (this.onGround) {
            this.motionY *= -0.5D;
        }

        ++this.age;

        if (this.age >= 6000) {
            this.setDead();
        }
    }

    public ItemStack getEntityItem() {
        return this.entityItem;
    }

    public void setEntityItemStack(ItemStack stack) {
        this.entityItem = stack;
    }

    public void combineItems(EntityItem other) {
        if (other == this) {
            return;
        }
        if (!other.isEntityAlive() || !this.isEntityAlive()) {
            return;
        }
        ItemStack otherStack = other.getEntityItem();
        ItemStack thisStack = this.getEntityItem();

        if (otherStack == null || thisStack == null) {
            return;
        }

        if (otherStack.itemID != thisStack.itemID) {
            return;
        }

        if (otherStack.getItemDamage() != thisStack.getItemDamage()) {
            return;
        }

        if (!ItemStack.areItemStackTagsEqual(otherStack, thisStack)) {
            return;
        }

        int combined = thisStack.stackSize + otherStack.stackSize;
        int maxStack = thisStack.getMaxStackSize();

        if (combined <= maxStack) {
            otherStack.stackSize = 0;
            other.setDead();
            thisStack.stackSize = combined;
            this.setEntityItemStack(thisStack);
        } else if (thisStack.stackSize < maxStack) {
            otherStack.stackSize -= (maxStack - thisStack.stackSize);
            thisStack.stackSize = maxStack;
            other.setEntityItemStack(otherStack);
            this.setEntityItemStack(thisStack);
        }
    }

    public void setDefaultPickupDelay() {
        this.delayBeforeCanPickup = 10;
    }

    public void setNoDespawn() {
        this.age = -6000;
    }

    public boolean canBePickedUp() {
        return this.delayBeforeCanPickup <= 0;
    }

    public void onCollideWithPlayer(EntityPlayer player) {
        if (this.delayBeforeCanPickup > 0) {
            return;
        }
        if (this.entityItem == null) {
            return;
        }

        int stackSize = this.entityItem.stackSize;

        if (player.inventory != null && player.inventory.addItemStackToInventory(this.entityItem)) {
            player.playSound("random.pop", 0.2F,
                    ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);

            if (this.entityItem.stackSize <= 0) {
                this.setDead();
            }
        }
    }

    public void writeEntityToNBT(NBTTagCompound tag) {
        tag.setShort("Health", (short) this.health);
        tag.setShort("Age", (short) this.age);

        if (this.entityItem != null) {
            NBTTagCompound itemTag = new NBTTagCompound();
            this.entityItem.writeToNBT(itemTag);
            tag.setCompoundTag("Item", itemTag);
        }
    }

    public void readEntityFromNBT(NBTTagCompound tag) {
        this.health = tag.getShort("Health");
        this.age = tag.getShort("Age");

        NBTTagCompound itemTag = tag.getCompoundTag("Item");
        this.entityItem = ItemStack.loadItemStackFromNBT(itemTag);
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, int amount) {
        this.setBeenAttacked();
        this.health -= amount;
        if (this.health <= 0) {
            this.setDead();
        }
        return false;
    }

    @Override
    public boolean IsItemEntity() {
        return true;
    }

    public static boolean InstallationIntegrityTestEntityItem() { return true; }
}
