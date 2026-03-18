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

//FCMOD


public class FCItemStone extends Item
{
    public FCItemStone( int iItemID )
    {
        super( iItemID );
        
    	setUnlocalizedName( "fcItemStone" );
    	
    	SetFilterableProperties( Item.m_iFilterable_Small );
    	
    	setCreativeTab( CreativeTabs.tabMaterials );	    
    }
    
    @Override
    public boolean IsPistonPackable( ItemStack stack )
    {
    	return true;
    }
    
    @Override
    public int GetRequiredItemCountToPistonPack( ItemStack stack )
    {
    	return 8;
    }
    
    @Override
    public int GetResultingBlockIDOnPistonPack( ItemStack stack )
    {
    	return FCBetterThanWolves.fcBlockCobblestoneLoose.blockID;
    }
    
    //------------- Class Specific Methods ------------//
}
    
