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


public class FCItemSlatsLegacy extends FCItemPlacesAsBlock
{
    public FCItemSlatsLegacy( int iItemID )
    {
    	super( iItemID, FCBetterThanWolves.fcAestheticNonOpaque.blockID, 
    		FCBlockAestheticNonOpaque.m_iSubtypeSlats, "fcItemSlats" );
    	
    	SetBuoyant();
    	SetIncineratedInCrucible();    	
    }
	
    @Override
    public boolean CanItemPassIfFilter( ItemStack filteredItem )
    {
    	int iFilterableProperties = filteredItem.getItem().GetFilterableProperties( filteredItem ); 
		
    	return ( iFilterableProperties & ( Item.m_iFilterable_Thin | 
    		Item.m_iFilterable_Fine ) ) != 0;
    }
    
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
