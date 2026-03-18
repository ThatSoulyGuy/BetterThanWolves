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


public class AaaFCItemTemplate extends Item
{
    public AaaFCItemTemplate( int iItemID )
    {
    	super( iItemID );
    	
    	SetNonBuoyant();
		SetBellowsBlowDistance( 0 );		
    	SetNotIncineratedInCrucible();
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.NONE );
    	SetFilterableProperties( m_iFilterable_NoProperties );
    	
    	setUnlocalizedName( "fcItemTemplate" );
    	
    	setCreativeTab( CreativeTabs.tabMisc );    	
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
