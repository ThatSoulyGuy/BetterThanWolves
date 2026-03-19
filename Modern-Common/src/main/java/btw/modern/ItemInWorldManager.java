package btw.modern;

public class ItemInWorldManager {
    public World theWorld;
    public EntityPlayer thisPlayerMP;
    public EnumGameType gameType;

    public ItemInWorldManager(World world) {
        this.theWorld = world;
    }

    public void setGameType(EnumGameType type) { this.gameType = type; }
    public EnumGameType getGameType() { return this.gameType; }
    public boolean isCreative() { return false; }
    public void initBlockRemoving() {}

    /**
     * FC's complete block harvest pipeline from the patched ItemInWorldManager.
     * Handles: conversion (log→damaged log), proper tool harvest, improper tool component drops.
     *
     * Called by the mixin when a player finishes breaking a block.
     * ALL gameplay logic is FC code — the mixin just calls this method.
     */
    public boolean tryHarvestBlock(int i, int j, int k, int iFromSide) {
        if (theWorld == null || thisPlayerMP == null) {
            org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] tryHarvestBlock: world or player null");
            return false;
        }

        int iBlockID = theWorld.getBlockId(i, j, k);
        org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] tryHarvestBlock at " + i + "," + j + "," + k + " blockID=" + iBlockID);
        if (iBlockID <= 0) return false;

        Block block = Block.blocksList[iBlockID];
        if (block == null) {
            org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] blocksList[" + iBlockID + "] is null");
            return false;
        }
        org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] block class: " + block.getClass().getSimpleName());

        int iMetadata = theWorld.getBlockMetadata(i, j, k);
        // NOTE: break particles/sound are handled by the vanilla client when
        // the block state changes. We do NOT call playAuxSFX(2001,...) here
        // because the data format differs between FC (legacy ID) and MC 1.20.1
        // (modern registry ID), which causes wrong particle textures.

        ItemStack currentStack = thisPlayerMP.getCurrentEquippedItem();
        org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] currentStack: " + (currentStack != null ? "id=" + currentStack.itemID : "null"));

        boolean bRemovingBlock = true;
        boolean bConvertingBlock = false;

        boolean bHarvestingBlock = thisPlayerMP.canHarvestBlock(block, i, j, k);
        org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] canHarvest=" + bHarvestingBlock);

        if (!bHarvestingBlock) {
            bConvertingBlock = block.CanConvertBlock(currentStack, theWorld, i, j, k);
            org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] canConvert=" + bConvertingBlock);

            if (bConvertingBlock) {
                boolean converted = block.ConvertBlock(currentStack, theWorld, i, j, k, iFromSide);
                bRemovingBlock = !converted;
                org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] ConvertBlock returned " + converted + ", removing=" + bRemovingBlock);
            }
        }

        if (bRemovingBlock) {
            boolean bRemoved = theWorld.setBlockToAir(i, j, k);
            org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] setBlockToAir=" + bRemoved + " converting=" + bConvertingBlock);

            if (bRemoved && !bConvertingBlock) {
                if (bHarvestingBlock) {
                    org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] calling harvestBlock");
                    block.harvestBlock(theWorld, thisPlayerMP, i, j, k, iMetadata);
                } else {
                    org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] calling OnBlockDestroyedWithImproperTool");
                    block.OnBlockDestroyedWithImproperTool(theWorld, thisPlayerMP, i, j, k, iMetadata);
                }
            }
        } else {
            org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info("[BTW-DEBUG] block NOT removed (converted in place)");
        }

        // Damage the held tool
        if (currentStack != null) {
            currentStack.onBlockDestroyed(theWorld, iBlockID, i, j, k, thisPlayerMP);

            if (currentStack.stackSize <= 0) {
                thisPlayerMP.destroyCurrentEquippedItem();
            }
        }

        return true;
    }
}
