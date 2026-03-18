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


public class FCItemBlockLog extends ItemMultiTextureTile
{
    public FCItemBlockLog( int iBlockID, Block block, String[] sTypeNames )
    {
        super( iBlockID, block, sTypeNames );
    }
    
    @Override
    public void OnUsedInCrafting( int iItemDamage, EntityPlayer player, ItemStack outputStack )
    {
    	if ( !player.worldObj.isRemote )
    	{
    		if ( outputStack.itemID == Block.planks.blockID )
    		{
    			FCUtilsItem.EjectStackWithRandomVelocity( player.worldObj, player.posX, player.posY, player.posZ, 
    				new ItemStack( FCBetterThanWolves.fcItemSawDust, 2, 0 ) );
    			
    			FCUtilsItem.EjectStackWithRandomVelocity( player.worldObj, player.posX, player.posY, player.posZ, 
    				new ItemStack( FCBetterThanWolves.fcItemBark, 1, outputStack.getItemDamage() ) );
    		}
    		else if ( outputStack.itemID == Item.stick.itemID )
    		{
    			FCUtilsItem.EjectStackWithRandomVelocity( player.worldObj, player.posX, player.posY, player.posZ, 
    				new ItemStack( FCBetterThanWolves.fcItemSawDust, 4, 0 ) );
    			
    			FCUtilsItem.EjectStackWithRandomVelocity( player.worldObj, player.posX, player.posY, player.posZ, 
    				new ItemStack( FCBetterThanWolves.fcItemBark, 1, iItemDamage ) );
    		}
    	}
    }
    
    @Override
    public int GetCampfireBurnTime( int iItemDamage )
    {
    	// logs can't be burned directly in a campfire without being split first
    	
    	return 0;
    }    
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
