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

public class FCItemWoolKnit extends Item
{
    public FCItemWoolKnit( int iItemID )
    {
    	super( iItemID );
    	
        setMaxDamage( 0 );
        setHasSubtypes( true );
        
        SetBuoyant();
        SetBellowsBlowDistance( 1 );
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.WOOL_KNIT );
    	SetFilterableProperties( Item.m_iFilterable_Thin );
        
    	setUnlocalizedName( "fcItemWoolKnit" );
    	
        setCreativeTab( CreativeTabs.tabMaterials );
    }
    
    @Override
    public String getItemDisplayName( ItemStack stack )
    {
        int iColor = MathHelper.clamp_int( stack.getItemDamage(), 0, 15 );
    	
        return ( "" + FCItemWool.m_sWoolColorNames[iColor] + " " + StringTranslate.getInstance().translateNamedKey( getLocalizedName( stack ) ) ).trim();
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
    
    @Override
    public void getSubItems( int iItemID, CreativeTabs creativeTabs, List list )
    {
    	for ( int iColor = 0; iColor < 16; iColor++ )
    	{
    		list.add( new ItemStack( iItemID, 1, iColor ) );
    	}
    }
    
    @Override    
    public int getColorFromItemStack( ItemStack stack, int iRenderPass )
    {
    	return FCItemWool.m_iWoolColors[stack.getItemDamage()];
    }
}
