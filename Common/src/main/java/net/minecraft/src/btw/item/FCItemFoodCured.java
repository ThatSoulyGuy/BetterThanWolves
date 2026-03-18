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


public class FCItemFoodCured extends FCItemFood
{
    public FCItemFoodCured( int iItemID, int iHungerHealed, float fSaturationModifier, String sItemName )
    {
        super( iItemID, iHungerHealed, fSaturationModifier, false, sItemName );
        
        maxStackSize = 32;
        
        setUnlocalizedName( sItemName );    
    }
    
    @Override
    public void onCreated( ItemStack stack, World world, EntityPlayer player ) 
    {
		if ( player.m_iTimesCraftedThisTick == 0 && world.isRemote )
		{
			player.playSound( "random.fizz",  0.25F, 2.5F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F );
		}
		
    	super.onCreated( stack, world, player );
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}