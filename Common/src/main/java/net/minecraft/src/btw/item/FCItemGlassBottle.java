package net.minecraft.src.btw.item;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.crafting.*;
import net.minecraft.src.btw.api.*;
import net.minecraft.src.btw.util.*;
import net.minecraft.src.btw.world.*;
import net.minecraft.src.btw.behavior.*;
import net.minecraft.src.btw.properties.*;
import net.minecraft.src.btw.model.*;
import net.minecraft.src.btw.command.*;

// FCMOD


public class FCItemGlassBottle extends ItemGlassBottle
{
    public FCItemGlassBottle(int par1)
    {
        super(par1);
    }

    /**
     * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
     */
    public ItemStack onItemRightClick( ItemStack itemStack, World world, EntityPlayer player )
    {
    	// this one line is the reason for the reflected base class, so bottles can be filled from non-source water blocks
    	MovingObjectPosition movingobjectposition = FCUtilsMisc.GetMovingObjectPositionFromPlayerHitWaterAndLava( world, player, true );    	

        if (movingobjectposition == null)
        {
            return itemStack;
        }

        if (movingobjectposition.typeOfHit == EnumMovingObjectType.TILE)
        {
            int i = movingobjectposition.blockX;
            int j = movingobjectposition.blockY;
            int k = movingobjectposition.blockZ;

            if (!world.canMineBlock(player, i, j, k))
            {
                return itemStack;
            }

            if ( !player.canPlayerEdit( i, j, k, movingobjectposition.sideHit, itemStack ) )
            {
                return itemStack;
            }

            if (world.getBlockMaterial(i, j, k) == Material.water)
            {
                itemStack.stackSize--;

                if (itemStack.stackSize <= 0)
                {
                    return new ItemStack(Item.potion);
                }

                if (!player.inventory.addItemStackToInventory(new ItemStack(Item.potion)))
                {
                    player.dropPlayerItem(new ItemStack(Item.potion.itemID, 1, 0));
                }
            }
        }

        return itemStack;
    }
}