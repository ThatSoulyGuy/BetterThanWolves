package btw.modern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Container {

    public List<ItemStack> inventoryItemStacks = new ArrayList<>();
    public List<Slot> inventorySlots = new ArrayList<>();
    public int windowId;
    public List<ICrafting> crafters = new ArrayList<>();

    private short transactionID;
    private int dragMode = -1;
    private int dragEvent;
    private final Set<Slot> dragSlots = new HashSet<>();

    protected Slot addSlotToContainer(Slot slot) {
        slot.slotNumber = this.inventorySlots.size();
        this.inventorySlots.add(slot);
        this.inventoryItemStacks.add(null);
        return slot;
    }

    public Slot getSlot(int slotIndex) {
        return this.inventorySlots.get(slotIndex);
    }

    // 1.5.2 Container.calcRedstoneFromInventory — comparator fill level;
    // referenced by the frozen EntityMinecartContainer.
    public static int calcRedstoneFromInventory(IInventory inventory) {
        if (inventory == null) {
            return 0;
        }
        int itemsFound = 0;
        float fill = 0.0F;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null) {
                fill += (float) stack.stackSize
                        / (float) Math.min(inventory.getInventoryStackLimit(), stack.getMaxStackSize());
                itemsFound++;
            }
        }
        fill /= (float) inventory.getSizeInventory();
        return MathHelper.floor_float(fill * 14.0F) + (itemsFound > 0 ? 1 : 0);
    }

    public void detectAndSendChanges() {
        for (int i = 0; i < this.inventorySlots.size(); i++) {
            ItemStack currentStack = this.inventorySlots.get(i).getStack();
            ItemStack previousStack = this.inventoryItemStacks.get(i);

            if (!ItemStack.areItemStacksEqual(currentStack, previousStack)) {
                previousStack = currentStack == null ? null : currentStack.copy();
                this.inventoryItemStacks.set(i, previousStack);

                for (ICrafting crafter : this.crafters) {
                    crafter.sendSlotContents(this, i, previousStack);
                }
            }
        }
    }

    public void addCraftingToCrafters(ICrafting crafting) {
        if (this.crafters.contains(crafting)) {
            throw new IllegalArgumentException("Listener already listening");
        }
        this.crafters.add(crafting);
        crafting.sendContainerAndContentsToPlayer(this, this.getInventory());
        this.detectAndSendChanges();
    }

    public void onCraftGuiOpened(ICrafting crafting) {
        addCraftingToCrafters(crafting);
    }

    public List<ItemStack> getInventory() {
        List<ItemStack> list = new ArrayList<>();
        for (Slot slot : this.inventorySlots) {
            list.add(slot.getStack());
        }
        return list;
    }

    public abstract boolean canInteractWith(EntityPlayer player);

    /**
     * Handles slot clicks. This is a simplified but functional implementation
     * matching vanilla b1.5/1.5.2 Container behavior.
     *
     * mode 0 = normal click (button 0=left, 1=right)
     * mode 1 = shift-click (button 0=left, 1=right)
     * mode 2 = hotbar swap (button = hotbar slot index 0-8)
     * mode 3 = middle/clone click (creative only)
     * mode 4 = drop (button 0=drop 1, button 1=drop stack; slotId=-999 = click outside)
     * mode 5 = drag (button encodes drag phase and mouse button)
     * mode 6 = double-click to collect
     */
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        ItemStack returnStack = null;
        InventoryPlayer inventoryPlayer = player.inventory;

        if (mode == 5) {
            // Drag mode: button encodes phase and mouse button
            // Phase 0 = start, Phase 1 = add slot, Phase 2 = end
            // Button bits: low 2 = mouse (0=left, 1=right, 2=middle), high bits = phase
            int phase = (clickedButton >> 2) & 3;
            int mouseButton = clickedButton & 3;

            if (phase == 0) {
                // Start drag
                dragMode = mouseButton;
                dragEvent = 0;
                dragSlots.clear();
            } else if (phase == 1 && dragEvent >= 0) {
                // Add slot to drag set
                if (slotId >= 0 && slotId < inventorySlots.size()) {
                    Slot slot = inventorySlots.get(slotId);
                    ItemStack held = inventoryPlayer.getItemStack();
                    if (held != null && slot.isItemValid(held)) {
                        dragSlots.add(slot);
                    }
                }
                dragEvent = 1;
            } else if (phase == 2 && dragEvent >= 0) {
                // End drag — distribute held stack across collected slots
                ItemStack held = inventoryPlayer.getItemStack();
                if (held != null && !dragSlots.isEmpty()) {
                    int totalAmount = held.stackSize;
                    int perSlot = dragMode == 0 ? totalAmount / dragSlots.size() : 1; // left=even split, right=1 each

                    for (Slot slot : dragSlots) {
                        if (held.stackSize <= 0) break;
                        ItemStack existing = slot.getStack();
                        int maxSize = Math.min(slot.getSlotStackLimit(), held.getMaxStackSize());
                        int placeCount = Math.min(perSlot, maxSize);

                        if (existing == null) {
                            placeCount = Math.min(placeCount, held.stackSize);
                            slot.putStack(held.splitStack(placeCount));
                        } else if (existing.itemID == held.itemID
                                && existing.getItemDamage() == held.getItemDamage()
                                && ItemStack.areItemStackTagsEqual(existing, held)) {
                            int space = maxSize - existing.stackSize;
                            placeCount = Math.min(placeCount, Math.min(space, held.stackSize));
                            if (placeCount > 0) {
                                existing.stackSize += placeCount;
                                held.stackSize -= placeCount;
                                slot.onSlotChanged();
                            }
                        }
                    }

                    if (held.stackSize <= 0) {
                        inventoryPlayer.setItemStack(null);
                    }
                }
                dragSlots.clear();
                dragEvent = -1;
            } else {
                // Invalid state — reset
                dragSlots.clear();
                dragEvent = -1;
            }
            return null;
        }

        if (mode == 0 || mode == 1) {
            if (slotId == -999) {
                // Clicked outside inventory
                if (inventoryPlayer.getItemStack() != null) {
                    if (clickedButton == 0) {
                        // Drop entire held stack
                        player.func_71012_a(inventoryPlayer.getItemStack());
                        inventoryPlayer.setItemStack(null);
                    } else if (clickedButton == 1) {
                        // Drop one item from held stack
                        player.func_71012_a(inventoryPlayer.getItemStack().splitStack(1));
                        if (inventoryPlayer.getItemStack().stackSize == 0) {
                            inventoryPlayer.setItemStack(null);
                        }
                    }
                }
            } else if (mode == 1) {
                // Shift-click
                if (slotId < 0 || slotId >= this.inventorySlots.size()) {
                    return null;
                }
                Slot slot = this.inventorySlots.get(slotId);
                if (slot != null && slot.canTakeStack(player)) {
                    ItemStack transferred = this.transferStackInSlot(player, slotId);
                    if (transferred != null) {
                        returnStack = transferred.copy();
                    }
                }
            } else {
                // Normal click
                if (slotId < 0 || slotId >= this.inventorySlots.size()) {
                    return null;
                }
                Slot slot = this.inventorySlots.get(slotId);
                if (slot != null) {
                    ItemStack slotStack = slot.getStack();
                    ItemStack heldStack = inventoryPlayer.getItemStack();

                    if (slotStack != null) {
                        returnStack = slotStack.copy();
                    }

                    if (slotStack == null) {
                        // Empty slot - place held stack
                        if (heldStack != null && slot.isItemValid(heldStack)) {
                            int placeCount = clickedButton == 0 ? heldStack.stackSize : 1;
                            if (placeCount > slot.getSlotStackLimit()) {
                                placeCount = slot.getSlotStackLimit();
                            }

                            if (heldStack.stackSize == placeCount) {
                                slot.putStack(heldStack);
                                inventoryPlayer.setItemStack(null);
                            } else {
                                slot.putStack(heldStack.splitStack(placeCount));
                                if (heldStack.stackSize == 0) {
                                    inventoryPlayer.setItemStack(null);
                                }
                            }
                        }
                    } else if (slot.canTakeStack(player)) {
                        if (heldStack == null) {
                            // Pick up from slot
                            int pickupCount = clickedButton == 0 ? slotStack.stackSize : (slotStack.stackSize + 1) / 2;
                            ItemStack picked = slot.decrStackSize(pickupCount);
                            inventoryPlayer.setItemStack(picked);

                            if (slotStack.stackSize == 0) {
                                slot.putStack(null);
                            }
                            slot.onPickupFromSlot(player, picked);
                        } else if (slot.isItemValid(heldStack)) {
                            // Both slot and cursor have items
                            if (slotStack.itemID == heldStack.itemID
                                    && slotStack.getItemDamage() == heldStack.getItemDamage()
                                    && ItemStack.areItemStackTagsEqual(slotStack, heldStack)) {
                                // Same item type - try to merge
                                int placeCount = clickedButton == 0 ? heldStack.stackSize : 1;
                                int maxSize = Math.min(slot.getSlotStackLimit(), heldStack.getMaxStackSize());
                                int space = maxSize - slotStack.stackSize;

                                if (placeCount > space) {
                                    placeCount = space;
                                }

                                if (placeCount > 0) {
                                    heldStack.stackSize -= placeCount;
                                    slotStack.stackSize += placeCount;
                                    slot.onSlotChanged();
                                }

                                if (heldStack.stackSize == 0) {
                                    inventoryPlayer.setItemStack(null);
                                }
                            } else if (heldStack.stackSize <= slot.getSlotStackLimit()) {
                                // Different items - swap
                                slot.putStack(heldStack);
                                inventoryPlayer.setItemStack(slotStack);
                            }
                        } else if (slotStack.itemID == heldStack.itemID
                                && heldStack.getMaxStackSize() > 1
                                && slotStack.getItemDamage() == heldStack.getItemDamage()
                                && ItemStack.areItemStackTagsEqual(slotStack, heldStack)) {
                            // Slot doesn't accept held item, but they're the same type -
                            // try to pull items from slot into cursor stack
                            int pullCount = slotStack.stackSize;
                            int space = heldStack.getMaxStackSize() - heldStack.stackSize;
                            if (pullCount > space) {
                                pullCount = space;
                            }
                            if (pullCount > 0) {
                                heldStack.stackSize += pullCount;
                                slotStack = slot.decrStackSize(pullCount);
                                if (slotStack.stackSize == 0) {
                                    slot.putStack(null);
                                }
                                // Must call onPickupFromSlot so merchant/crafting
                                // slots can consume inputs on trade/craft completion.
                                slot.onPickupFromSlot(player, slotStack);
                            }
                        }
                    }

                    slot.onSlotChanged();
                }
            }
        } else if (mode == 2 && clickedButton >= 0 && clickedButton < 9) {
            // Hotbar swap
            if (slotId >= 0 && slotId < this.inventorySlots.size()) {
                Slot slot = this.inventorySlots.get(slotId);
                ItemStack hotbarStack = inventoryPlayer.getStackInSlot(clickedButton);
                ItemStack slotStack = slot.getStack();

                if (hotbarStack != null || slotStack != null) {
                    if (hotbarStack == null) {
                        if (slot.canTakeStack(player)) {
                            inventoryPlayer.setInventorySlotContents(clickedButton, slotStack);
                            slot.putStack(null);
                            slot.onPickupFromSlot(player, slotStack);
                        }
                    } else if (slotStack == null) {
                        if (slot.isItemValid(hotbarStack)) {
                            int maxSize = slot.getSlotStackLimit();
                            if (hotbarStack.stackSize > maxSize) {
                                slot.putStack(hotbarStack.splitStack(maxSize));
                            } else {
                                slot.putStack(hotbarStack);
                                inventoryPlayer.setInventorySlotContents(clickedButton, null);
                            }
                        }
                    } else {
                        if (slot.canTakeStack(player) && slot.isItemValid(hotbarStack)) {
                            int maxSize = slot.getSlotStackLimit();
                            if (hotbarStack.stackSize <= maxSize) {
                                slot.putStack(hotbarStack);
                                inventoryPlayer.setInventorySlotContents(clickedButton, slotStack);
                                slot.onPickupFromSlot(player, slotStack);
                            }
                        }
                    }
                }
            }
        } else if (mode == 3 && player.isInCreativeMode() && inventoryPlayer.getItemStack() == null && slotId >= 0) {
            // 1.5.2 Container.slotClick par3==3 — creative middle-click stack clone;
            // reached via FCContainerMenu.clicked ClickType.CLONE→3. Creative check is
            // bridged through EntityPlayer.isInCreativeMode (PlayerBridge overrides it).
            if (slotId < this.inventorySlots.size()) {
                Slot slot = this.inventorySlots.get(slotId);

                if (slot != null && slot.getHasStack()) {
                    ItemStack cloned = slot.getStack().copy();
                    cloned.stackSize = cloned.getMaxStackSize();
                    inventoryPlayer.setItemStack(cloned);
                }
            }
        } else if (mode == 4) {
            // Drop mode
            if (slotId >= 0 && slotId < this.inventorySlots.size()) {
                Slot slot = this.inventorySlots.get(slotId);
                if (slot.getStack() != null && slot.canTakeStack(player)) {
                    ItemStack dropped = slot.decrStackSize(clickedButton == 0 ? 1 : slot.getStack().stackSize);
                    slot.onPickupFromSlot(player, dropped);
                    player.func_71012_a(dropped);
                }
            }
        } else if (mode == 6) {
            // Double-click to collect
            if (slotId >= 0 && slotId < this.inventorySlots.size()) {
                ItemStack heldStack = inventoryPlayer.getItemStack();
                if (heldStack != null) {
                    int maxStackSize = heldStack.getMaxStackSize();
                    for (int i = 0; i < this.inventorySlots.size() && heldStack.stackSize < maxStackSize; i++) {
                        Slot scanSlot = this.inventorySlots.get(i);
                        ItemStack scanStack = scanSlot.getStack();
                        if (scanStack != null
                                && scanStack.itemID == heldStack.itemID
                                && scanStack.getItemDamage() == heldStack.getItemDamage()
                                && ItemStack.areItemStackTagsEqual(scanStack, heldStack)
                                && scanSlot.canTakeStack(player)) {
                            int take = Math.min(scanStack.stackSize, maxStackSize - heldStack.stackSize);
                            heldStack.stackSize += take;
                            scanSlot.decrStackSize(take);
                            if (scanSlot.getStack() != null && scanSlot.getStack().stackSize == 0) {
                                scanSlot.putStack(null);
                            }
                        }
                    }
                }
            }
        }

        return returnStack;
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        return null;
    }

    public void onContainerClosed(EntityPlayer player) {}

    public void onCraftMatrixChanged(IInventory inventory) {}

    public void putStackInSlot(int slotIndex, ItemStack stack) {
        this.getSlot(slotIndex).putStack(stack);
    }

    public boolean enchantItem(EntityPlayer player, int enchantmentIndex) {
        return false;
    }

    public void updateProgressBar(int id, int data) {}

    public short getNextTransactionID(InventoryPlayer playerInventory) {
        ++this.transactionID;
        return this.transactionID;
    }

    public boolean isPlayerNotUsingContainer(EntityPlayer player) {
        return !this.canInteractWith(player);
    }

    public void retrySlotClick(int slotId, int clickedButton, boolean holding, EntityPlayer player) {}

    public void onCraftGuiClosed(EntityPlayer player) {
        this.onContainerClosed(player);
    }

    // 1.5.2 FCMOD Container.mergeItemStack(stack, first, cap, bFavorHotbar) — the 4-arg entry
    // every FC container's transferStackInSlot calls (FCContainerWorkbench:64, FCContainerPlayer:43,
    // FCContainerWithInventory:98, FCContainerHopper:102, FCContainerPulley:96, ...). The 4th arg is
    // NOT vanilla's reverseDirection: it requests hotbar-first (left-to-right) placement.
    // FCMOD: Added
    public boolean mergeItemStack(ItemStack stackSource, int iSlotDestFirst, int iSlotDestCap, boolean bFavorHotbar) {
        // test of specific player inv size shouldn't be necessary, but serves as a sanity check

        if (bFavorHotbar && iSlotDestCap - iSlotDestFirst == 36) {
            // favor the hotbar from left to right, then the main inventory, whereas vanilla
            // just reverses the order from last slot of hotbar to first of inv.

            return MergeItemStackFavoringHotbar(stackSource, iSlotDestFirst, iSlotDestCap);
        } else {
            return mergeItemStack(stackSource, iSlotDestFirst, iSlotDestCap);
        }
    }

    // 1.5.2 FCMOD Container.mergeItemStack(stack, first, cap) — forward 3-arg merge.
    protected boolean mergeItemStack(ItemStack stackSource, int iSlotDestFirst, int iSlotDestCap) {
        boolean bMerged = false;

        if (stackSource.isStackable()) {
            // look for destination stacks already containing the same item type

            for (int iTempSlot = iSlotDestFirst;
                iTempSlot < iSlotDestCap && stackSource.stackSize > 0; iTempSlot++) {
                bMerged |= AttemptToMergeWithSlot(stackSource, iTempSlot);
            }
        }

        if (stackSource.stackSize > 0) {
            // look for empty destination stacks

            for (int iTempSlot = iSlotDestFirst;
                iTempSlot < iSlotDestCap && stackSource.stackSize > 0; iTempSlot++) {
                bMerged |= AttemptToMergeWithSlotIfEmpty(stackSource, iTempSlot);
            }
        }

        return bMerged;
    }

    // 1.5.2 FCMOD Container.MergeItemStackFavoringHotbar — hotbar segment [cap-9, cap) first,
    // then main inventory [first, cap-9).
    protected boolean MergeItemStackFavoringHotbar(ItemStack stackSource, int iSlotDestFirst, int iSlotDestCap) {
        boolean bMerged = false;

        if (stackSource.isStackable()) {
            // look for destination stacks already containing the same item type

            for (int iTempSlot = iSlotDestCap - 9;
                iTempSlot < iSlotDestCap && stackSource.stackSize > 0; iTempSlot++) {
                bMerged |= AttemptToMergeWithSlot(stackSource, iTempSlot);
            }

            for (int iTempSlot = iSlotDestFirst;
                iTempSlot < iSlotDestCap - 9 && stackSource.stackSize > 0; iTempSlot++) {
                bMerged |= AttemptToMergeWithSlot(stackSource, iTempSlot);
            }
        }

        if (stackSource.stackSize > 0) {
            // look for empty destination stacks

            for (int iTempSlot = iSlotDestCap - 9;
                iTempSlot < iSlotDestCap && stackSource.stackSize > 0; iTempSlot++) {
                bMerged |= AttemptToMergeWithSlotIfEmpty(stackSource, iTempSlot);
            }

            for (int iTempSlot = iSlotDestFirst;
                iTempSlot < iSlotDestCap - 9 && stackSource.stackSize > 0; iTempSlot++) {
                bMerged |= AttemptToMergeWithSlotIfEmpty(stackSource, iTempSlot);
            }
        }

        return bMerged;
    }

    // 1.5.2 FCMOD Container.AttemptToMergeWithSlot — note the !getHasSubtypes() guard on damage compare.
    public boolean AttemptToMergeWithSlot(ItemStack stackSource, int iTempSlot) {
        Slot tempDestSlot = this.inventorySlots.get(iTempSlot);
        ItemStack tempDestStack = tempDestSlot.getStack();

        if (tempDestStack != null && tempDestStack.itemID == stackSource.itemID
                && (!stackSource.getHasSubtypes()
                || stackSource.getItemDamage() == tempDestStack.getItemDamage())
                && ItemStack.areItemStackTagsEqual(stackSource, tempDestStack)) {
            int iDestStackSize = tempDestStack.stackSize + stackSource.stackSize;
            int iMaxStackSize = stackSource.getMaxStackSize();

            if (tempDestSlot.getSlotStackLimit() < iMaxStackSize) {
                iMaxStackSize = tempDestSlot.getSlotStackLimit();
            }

            if (tempDestStack.stackSize < iMaxStackSize) {
                if (iDestStackSize <= iMaxStackSize) {
                    stackSource.stackSize = 0;
                    tempDestStack.stackSize = iDestStackSize;
                } else {
                    stackSource.stackSize -= iMaxStackSize - tempDestStack.stackSize;
                    tempDestStack.stackSize = iMaxStackSize;
                }

                tempDestSlot.onSlotChanged();

                return true;
            }
        }

        return false;
    }

    // 1.5.2 FCMOD Container.AttemptToMergeWithSlotIfEmpty — no isItemValid gate in FCMOD.
    public boolean AttemptToMergeWithSlotIfEmpty(ItemStack stackSource, int iTempSlot) {
        Slot tempDestSlot = this.inventorySlots.get(iTempSlot);
        ItemStack tempDestStack = tempDestSlot.getStack();

        if (tempDestStack == null) {
            int iMaxStackSize = stackSource.getMaxStackSize();

            if (tempDestSlot.getSlotStackLimit() < iMaxStackSize) {
                iMaxStackSize = tempDestSlot.getSlotStackLimit();
            }

            if (stackSource.stackSize <= iMaxStackSize) {
                tempDestSlot.putStack(stackSource.copy());
                stackSource.stackSize = 0;
            } else {
                tempDestSlot.putStack(stackSource.copy());
                stackSource.stackSize -= iMaxStackSize;
                tempDestSlot.getStack().stackSize = iMaxStackSize;
            }

            tempDestSlot.onSlotChanged();

            return true;
        }

        return false;
    }
    // END FCMOD
}
