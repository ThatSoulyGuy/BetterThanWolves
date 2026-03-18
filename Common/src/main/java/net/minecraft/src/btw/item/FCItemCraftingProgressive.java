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


public class FCItemCraftingProgressive extends Item
{
	static public final int m_iProgressTimeInterval = 4;
	static public final int m_iDefaultMaxDamage = ( 120 * 20 / m_iProgressTimeInterval );
	
    public FCItemCraftingProgressive( int iItemID )
    {
    	super( iItemID );
    	
        maxStackSize = 1;        
        
        setMaxDamage( GetProgressiveCraftingMaxDamage() );
    }
    
    @Override
    public ItemStack onItemRightClick( ItemStack stack, World world, EntityPlayer player )
    {
    	player.setItemInUse( stack, getMaxItemUseDuration( stack ) );

        return stack;
    }
    
    @Override
    public EnumAction getItemUseAction(ItemStack par1ItemStack)
    {
        return EnumAction.miscUse;
    }
    
    @Override
    public int getMaxItemUseDuration(ItemStack par1ItemStack)
    {
    	// stupid large so it's never actually hit in practice
    	
        return 72000;
    }    
    
    @Override
    public void UpdateUsingItem( ItemStack stack, World world, EntityPlayer player )
    {
    	int iUseCount = player.getItemInUseCount();
    	
        if ( getMaxItemUseDuration( stack ) - iUseCount > GetItemUseWarmupDuration() )
        {
        	if ( iUseCount % 4 == 0 )
        	{
        		PlayCraftingFX( stack, world, player );
        	}
            
            if ( !world.isRemote && iUseCount % m_iProgressTimeInterval == 0 )
            {
            	int iDamage = stack.getItemDamage();
            	
            	iDamage -= 1;
            	
            	if ( iDamage > 0 )
            	{            	
            		stack.setItemDamage( iDamage );
            	}
            	else
            	{
            		// set item usage to immediately complete
            		
            		player.SetItemInUseCount( 1 );
            	}
            }
        }
    }
    
    @Override
    public boolean IgnoreDamageWhenComparingDuringUse()
    {
    	return true;
    }

    //------------- Class Specific Methods ------------//

    public void PlayCraftingFX( ItemStack stack, World world, EntityPlayer player )
    {
    }
    
    public int GetProgressiveCraftingMaxDamage()
    {
    	return m_iDefaultMaxDamage;
    }
    
	//------------ Client Side Functionality ----------//
}
