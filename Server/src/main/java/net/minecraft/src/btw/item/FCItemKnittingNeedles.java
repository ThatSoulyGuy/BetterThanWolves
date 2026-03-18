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


public class FCItemKnittingNeedles extends Item
{
    public FCItemKnittingNeedles( int iItemID )
    {
    	super( iItemID );
    	
    	SetBuoyant();
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.SHAFT );    	
    	SetFilterableProperties( Item.m_iFilterable_Narrow );
    	
    	setUnlocalizedName( "fcItemKnittingNeedles" );
    	
    	setCreativeTab( CreativeTabs.tabTools );
    }
    
    @Override
    public boolean GetCanBeFedDirectlyIntoCampfire( int iItemDamage )
    {
    	return false;
    }
    
    @Override
    public boolean GetCanBeFedDirectlyIntoBrickOven( int iItemDamage )
    {
    	return false;
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
