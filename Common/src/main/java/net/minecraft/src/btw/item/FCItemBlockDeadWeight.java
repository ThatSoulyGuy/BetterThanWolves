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


public class FCItemBlockDeadWeight extends ItemBlock
{
    public FCItemBlockDeadWeight( int iItemID )
    {
    	super( iItemID );
    }
    
    @Override
    public void OnUsedInCrafting( EntityPlayer player, ItemStack outputStack )
    {
		// note: the playSound function for player both plays the sound locally on the client, and plays it remotely on the server without it being sent again to the same player
    	
		if ( player.m_iTimesCraftedThisTick == 0 )
		{
			player.playSound( "random.anvil_land", 0.3F, player.worldObj.rand.nextFloat() * 0.1F + 0.9F );
		}
    }
    
    @Override
    public void PlayPlaceSound( World world, int i, int j, int k, Block block )
    {
        world.playSoundEffect( (double)i + 0.5D, (double)j + 0.5D, (double)k + 0.5D, "random.anvil_use", 
        	0.5F, world.rand.nextFloat() * 0.05F + 0.7F );
    }
    
    @Override
    public void onCreated( ItemStack stack, World world, EntityPlayer player ) 
    {
		if ( player.m_iTimesCraftedThisTick == 0 && world.isRemote )
		{
			player.playSound( "random.anvil_use", 0.5F, world.rand.nextFloat() * 0.25F + 0.75F );
		}
		
    	super.onCreated( stack, world, player );
    }    
}
