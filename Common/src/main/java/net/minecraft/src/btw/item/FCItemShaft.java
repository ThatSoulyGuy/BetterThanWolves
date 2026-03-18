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


public class FCItemShaft extends ItemReed
{
    public FCItemShaft( int iItemID )
    {
    	// the shaft supplies its own blockID through method override to avoid initialization order
    	// problems
    	
    	super( iItemID, 0 );
    	
    	setFull3D();
    	
    	SetBuoyant();
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.SHAFT );
    	SetIncineratedInCrucible();
    	SetFilterableProperties( m_iFilterable_Narrow );
    	
    	setUnlocalizedName( "stick" );
    	
    	setCreativeTab( CreativeTabs.tabMaterials );    	
    }

    @Override
    public int getBlockID()
    {
        return FCBetterThanWolves.fcBlockShaft.blockID;
    }

    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
