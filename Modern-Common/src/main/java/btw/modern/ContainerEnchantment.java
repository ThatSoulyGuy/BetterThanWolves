package btw.modern;

import java.util.List;
import java.util.Random;

/**
 * 1.5.2 ContainerEnchantment (with FCMOD additions) — opened by
 * BlockEnchantmentTable.onBlockActivated via PlayerBridge.displayGUIEnchantment
 * (PlayerBridge.java:709). enchantLevels[0..2] sync to the client through
 * ICrafting.sendProgressBarUpdate -> FCContainerMenu data slots, and the
 * enchant buttons arrive via FCContainerMenu.clickMenuButton -> enchantItem.
 */
public class ContainerEnchantment extends Container {

    /** SlotEnchantmentTable inventory holding the ItemStack to be enchanted */
    public IInventory tableInventory = new SlotEnchantmentTable(this, "Enchant", true, 1);

    /** current world (for bookshelf counting) */
    private World worldPointer;
    private int posX;
    private int posY;
    private int posZ;
    private Random rand = new Random();

    /** used as seed for EnchantmentNameParts (see GuiEnchantment) */
    public long nameSeed;

    /** 3-member array storing the enchantment levels of each slot */
    public int[] enchantLevels = new int[3];

    public ContainerEnchantment(InventoryPlayer playerInv, World world, int x, int y, int z) {
        this.worldPointer = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.addSlotToContainer(new SlotEnchantment(this, this.tableInventory, 0, 25, 47));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public void addCraftingToCrafters(ICrafting crafting) {
        super.addCraftingToCrafters(crafting);
        crafting.sendProgressBarUpdate(this, 0, this.enchantLevels[0]);
        crafting.sendProgressBarUpdate(this, 1, this.enchantLevels[1]);
        crafting.sendProgressBarUpdate(this, 2, this.enchantLevels[2]);
    }

    /**
     * Looks for changes made in the container, sends them to every listener.
     */
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (int i = 0; i < this.crafters.size(); i++) {
            ICrafting crafting = (ICrafting) this.crafters.get(i);
            crafting.sendProgressBarUpdate(this, 0, this.enchantLevels[0]);
            crafting.sendProgressBarUpdate(this, 1, this.enchantLevels[1]);
            crafting.sendProgressBarUpdate(this, 2, this.enchantLevels[2]);
        }
    }

    public void updateProgressBar(int id, int value) {
        if (id >= 0 && id <= 2) {
            this.enchantLevels[id] = value;
        } else {
            super.updateProgressBar(id, value);
        }
    }

    /**
     * 1.5.2 ContainerEnchantment.onCraftMatrixChanged — bookshelf scan +
     * enchantLevels via the FC-patched calcItemStackEnchantability.
     */
    public void onCraftMatrixChanged(IInventory inventory) {
        if (inventory == this.tableInventory) {
            ItemStack stack = inventory.getStackInSlot(0);
            int shelfCount;

            if (stack != null && stack.isItemEnchantable()) {
                this.nameSeed = this.rand.nextLong();

                if (!this.worldPointer.isRemote) {
                    shelfCount = 0;
                    int dz;

                    for (dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if ((dz != 0 || dx != 0)
                                    && this.worldPointer.isAirBlock(this.posX + dx, this.posY, this.posZ + dz)
                                    && this.worldPointer.isAirBlock(this.posX + dx, this.posY + 1, this.posZ + dz)) {
                                if (this.worldPointer.getBlockId(this.posX + dx * 2, this.posY, this.posZ + dz * 2) == Block.bookShelf.blockID) {
                                    shelfCount++;
                                }

                                if (this.worldPointer.getBlockId(this.posX + dx * 2, this.posY + 1, this.posZ + dz * 2) == Block.bookShelf.blockID) {
                                    shelfCount++;
                                }

                                if (dx != 0 && dz != 0) {
                                    if (this.worldPointer.getBlockId(this.posX + dx * 2, this.posY, this.posZ + dz) == Block.bookShelf.blockID) {
                                        shelfCount++;
                                    }

                                    if (this.worldPointer.getBlockId(this.posX + dx * 2, this.posY + 1, this.posZ + dz) == Block.bookShelf.blockID) {
                                        shelfCount++;
                                    }

                                    if (this.worldPointer.getBlockId(this.posX + dx, this.posY, this.posZ + dz * 2) == Block.bookShelf.blockID) {
                                        shelfCount++;
                                    }

                                    if (this.worldPointer.getBlockId(this.posX + dx, this.posY + 1, this.posZ + dz * 2) == Block.bookShelf.blockID) {
                                        shelfCount++;
                                    }
                                }
                            }
                        }
                    }

                    for (dz = 0; dz < 3; dz++) {
                        this.enchantLevels[dz] = EnchantmentHelper.calcItemStackEnchantability(this.rand, dz, shelfCount, stack);
                    }

                    this.detectAndSendChanges();
                }
            } else {
                for (shelfCount = 0; shelfCount < 3; shelfCount++) {
                    this.enchantLevels[shelfCount] = 0;
                }
            }
        }
    }

    /**
     * 1.5.2 ContainerEnchantment.enchantItem (with FCMOD level-up sound) —
     * enchants the item on the table using the specified slot; deducts XP.
     */
    public boolean enchantItem(EntityPlayer player, int slotNum) {
        ItemStack stack = this.tableInventory.getStackInSlot(0);

        if (this.enchantLevels[slotNum] > 0 && stack != null
                && (player.experienceLevel >= this.enchantLevels[slotNum] || player.capabilities.isCreativeMode)) {
            if (!this.worldPointer.isRemote) {
                List enchList = EnchantmentHelper.buildEnchantmentList(this.rand, stack, this.enchantLevels[slotNum]);
                boolean isBook = stack.itemID == Item.book.itemID;

                if (enchList != null) {
                    player.addExperienceLevel(-this.enchantLevels[slotNum]);

                    if (isBook) {
                        stack.itemID = Item.enchantedBook.itemID;
                    }

                    int bookEnchantIndex = isBook ? this.rand.nextInt(enchList.size()) : -1;

                    for (int i = 0; i < enchList.size(); i++) {
                        EnchantmentData data = (EnchantmentData) enchList.get(i);

                        if (!isBook || i == bookEnchantIndex) {
                            if (isBook) {
                                Item.enchantedBook.func_92115_a(stack, data);
                            } else {
                                stack.addEnchantment(data.enchantmentobj, data.enchantmentLevel);
                            }
                        }
                    }

                    this.onCraftMatrixChanged(this.tableInventory);

                    // FCMOD: Code added
                    worldPointer.playSoundAtEntity(player, "random.levelup", 0.25F, worldPointer.rand.nextFloat() * 0.1F + 0.5F);
                    // END FCMOD
                }
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * 1.5.2 ContainerEnchantment.onCraftGuiClosed — returns the table item.
     */
    public void onCraftGuiClosed(EntityPlayer player) {
        super.onCraftGuiClosed(player);

        if (!this.worldPointer.isRemote) {
            ItemStack stack = this.tableInventory.getStackInSlotOnClosing(0);

            if (stack != null) {
                player.dropPlayerItem(stack);
            }
        }
    }

    public boolean canInteractWith(EntityPlayer player) {
        return this.worldPointer.getBlockId(this.posX, this.posY, this.posZ) != Block.enchantmentTable.blockID
                ? false
                : player.getDistanceSq((double) this.posX + 0.5D, (double) this.posY + 0.5D, (double) this.posZ + 0.5D) <= 64.0D;
    }

    /**
     * 1.5.2 ContainerEnchantment.transferStackInSlot — shift-click handling.
     */
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack result = null;
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();

            if (slotIndex == 0) {
                if (!this.mergeItemStack(slotStack, 1, 37, true)) {
                    return null;
                }
            } else {
                if (((Slot) this.inventorySlots.get(0)).getHasStack()
                        || !((Slot) this.inventorySlots.get(0)).isItemValid(slotStack)) {
                    return null;
                }

                if (slotStack.hasTagCompound() && slotStack.stackSize == 1) {
                    ((Slot) this.inventorySlots.get(0)).putStack(slotStack.copy());
                    slotStack.stackSize = 0;
                } else if (slotStack.stackSize >= 1) {
                    ((Slot) this.inventorySlots.get(0)).putStack(new ItemStack(slotStack.itemID, 1, slotStack.getItemDamage()));
                    --slotStack.stackSize;
                }
            }

            if (slotStack.stackSize == 0) {
                slot.putStack((ItemStack) null);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.stackSize == result.stackSize) {
                return null;
            }

            slot.onPickupFromSlot(player, slotStack);
        }

        return result;
    }

    // ================================================================
    // 1.5.2 SlotEnchantmentTable / SlotEnchantment — nested so they don't
    // shadow the fc-compiled classes of the same name.
    // ================================================================

    // 1.5.2 SlotEnchantmentTable — the 1-slot table inventory; item changes
    // trigger onCraftMatrixChanged (bookshelf scan).
    private static class SlotEnchantmentTable extends InventoryBasic {
        final ContainerEnchantment container;

        SlotEnchantmentTable(ContainerEnchantment container, String title, boolean localized, int slots) {
            super(title, localized, slots);
            this.container = container;
        }

        public int getInventoryStackLimit() {
            return 1;
        }

        public void onInventoryChanged() {
            super.onInventoryChanged();
            this.container.onCraftMatrixChanged(this);
        }

        // Vanilla 1.5.2 InventoryBasic.setInventorySlotContents calls
        // onInventoryChanged; the shim InventoryBasic dropped that call, so
        // restore it here — Slot.putStack must retrigger the bookshelf scan.
        public void setInventorySlotContents(int slot, ItemStack stack) {
            super.setInventorySlotContents(slot, stack);
            this.onInventoryChanged();
        }
    }

    // 1.5.2 SlotEnchantment — the visible table slot; accepts any item.
    private static class SlotEnchantment extends Slot {
        final ContainerEnchantment container;

        SlotEnchantment(ContainerEnchantment container, IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.container = container;
        }

        public boolean isItemValid(ItemStack stack) {
            return true;
        }
    }
}
