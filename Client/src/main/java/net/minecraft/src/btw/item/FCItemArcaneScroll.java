package net.minecraft.src.btw.item;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.client.*;
import net.minecraft.src.btw.crafting.*;
import net.minecraft.src.btw.api.*;
import net.minecraft.src.btw.util.*;
import net.minecraft.src.btw.world.*;
import net.minecraft.src.btw.behavior.*;
import net.minecraft.src.btw.properties.*;
import net.minecraft.src.btw.model.*;
import net.minecraft.src.btw.command.*;

// FCMOD


import java.util.List;

public class FCItemArcaneScroll extends Item
{
    public FCItemArcaneScroll( int iItemID )
    {
    	super( iItemID );
    	
        setMaxDamage( 0 );
        setHasSubtypes( true );
        
        SetBuoyant();
		SetBellowsBlowDistance( 3 );
		SetFilterableProperties( m_iFilterable_Small | m_iFilterable_Thin );

    	setUnlocalizedName( "fcItemScrollArcane" );
    	
        setCreativeTab( CreativeTabs.tabBrewing );
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//

    @Override
    public boolean hasEffect( ItemStack itemStack )
    {
		return true;
    }
    
    @Override
    public void addInformation( ItemStack itemStack, EntityPlayer player, List infoList, boolean bAdvamcedToolTips )
    {
    	int iIndex = MathHelper.clamp_int( itemStack.getItemDamage(), 0, Enchantment.enchantmentsList.length - 1 );
    	
    	Enchantment enchantment = Enchantment.enchantmentsList[iIndex];
    	
    	if ( enchantment != null )
    	{
    		infoList.add( (new StringBuilder()).append( StatCollector.translateToLocal( enchantment.getName() ) ).toString() );
    	}
    }
    
    @Override
    public void getSubItems( int iItemID, CreativeTabs creativeTabs, List list )
    {
		for ( int iTempIndex = 0; iTempIndex < Enchantment.enchantmentsList.length; iTempIndex++ )
		{
			if ( Enchantment.enchantmentsList[iTempIndex] != null )
			{
				list.add( new ItemStack( FCBetterThanWolves.fcItemArcaneScroll, 1, 
					Enchantment.enchantmentsList[iTempIndex].effectId ) );
			}
		}
    }
}