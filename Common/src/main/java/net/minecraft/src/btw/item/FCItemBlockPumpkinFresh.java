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


public class FCItemBlockPumpkinFresh extends ItemBlock
{
    public FCItemBlockPumpkinFresh( int iBlockID )
    {
        super( iBlockID );
        
		setMaxStackSize( 16 );
    }
    
    @Override
    public void OnUsedInCrafting( EntityPlayer player, ItemStack outputStack )
    {
		if ( outputStack.itemID == Block.pumpkin.blockID )
		{
	    	if ( !player.worldObj.isRemote )
	    	{
    			FCUtilsItem.EjectStackWithRandomVelocity( player.worldObj, player.posX, player.posY, player.posZ, 
    				new ItemStack( Item.pumpkinSeeds, 4, 0 ) );
	    	}
	    	
	    	if ( player.m_iTimesCraftedThisTick == 0 )
			{
				// note: the playSound function for player both plays the sound locally on the client, and plays it remotely on the server without it being sent again to the same player
		    	
				player.playSound( "mob.slime.attack", 0.5F, ( player.worldObj.rand.nextFloat() - player.worldObj.rand.nextFloat()) * 0.1F + 0.7F );
			}
		}
    }
}
