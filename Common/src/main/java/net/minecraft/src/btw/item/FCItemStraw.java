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


public class FCItemStraw extends Item
{
    public FCItemStraw( int iItemID )
    {
    	super( iItemID );
    	
    	SetBuoyant();
        SetIncineratedInCrucible();
		SetBellowsBlowDistance( 2 );
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.KINDLING );
    	SetFilterableProperties( m_iFilterable_Narrow );
        
    	SetHerbivoreFoodValue( EntityAnimal.m_iBaseGrazeFoodValue );
    	
    	setUnlocalizedName( "fcItemStraw" );
    	
    	setCreativeTab( CreativeTabs.tabMaterials );    	
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
