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


public abstract class FCItemBucketDrinkable extends FCItemBucket
{
	private int m_iHungerHealed;
	private float m_fSaturationModifier;
	
    public FCItemBucketDrinkable( int iItemID, int iHungerHealed, float fSaturationModifier )
    {
        super( iItemID );
        
    	m_iHungerHealed = iHungerHealed;
    	m_fSaturationModifier = fSaturationModifier;
    }
    
    @Override
    public int getMaxItemUseDuration( ItemStack stack )
    {
        return 32;
    }
    
    @Override
    public EnumAction getItemUseAction( ItemStack stack )
    {
        return EnumAction.drink;
    }
    
    @Override
    public ItemStack onItemRightClick( ItemStack stack, World world, EntityPlayer player )
    {
    	if ( player.CanDrink() )
    	{
    		player.setItemInUse( stack, getMaxItemUseDuration( stack ) );
    	}
    	else
    	{
    		player.OnCantConsume();
    	}
        
        return stack;
    }
    
    @Override
    public ItemStack onEaten( ItemStack itemStack, World world, EntityPlayer player )
    {
    	// override to add nutritional value to drinking milk buckets
    	
        if ( !player.capabilities.isCreativeMode )
        {
            --itemStack.stackSize;
        }

        if ( !world.isRemote )
        {
            player.getFoodStats().addStats( m_iHungerHealed, m_fSaturationModifier ); // food value adjusted for increased hunger meter resolution
        }

        return itemStack.stackSize <= 0 ? new ItemStack( Item.bucketEmpty ) : itemStack;
    }
    
	//------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
