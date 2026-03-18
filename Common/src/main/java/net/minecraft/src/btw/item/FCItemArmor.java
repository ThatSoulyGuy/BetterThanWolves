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


public class FCItemArmor extends ItemArmor
{
	private final int m_iArmorWeight;
	
    public FCItemArmor( int iItemID, EnumArmorMaterial armorMaterial, int iRenderIndex, int iArmorType, int iWeight )
    {
    	super( iItemID, armorMaterial, iRenderIndex, iArmorType );
    	
    	m_iArmorWeight = iWeight;
    }
    
    @Override
    public int GetWeightWhenWorn()
    {
    	return m_iArmorWeight;
    }
    
    @Override
    public void onCreated( ItemStack stack, World world, EntityPlayer player ) 
    {
    	super.onCreated( stack, world, player );
    	
		if ( player.m_iTimesCraftedThisTick == 0 && world.isRemote )
		{
			if ( getArmorMaterial() == EnumArmorMaterial.CLOTH )
			{
				player.playSound( "step.cloth", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F );
			}
			else
			{
				player.playSound( "random.anvil_use", 0.5F, world.rand.nextFloat() * 0.25F + 1.25F );
			}
		}		
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
