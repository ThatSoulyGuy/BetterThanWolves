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
}