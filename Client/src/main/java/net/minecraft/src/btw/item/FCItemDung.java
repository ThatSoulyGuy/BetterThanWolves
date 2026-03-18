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


public class FCItemDung extends Item
{
    public FCItemDung( int iItemID )
    {
    	super( iItemID );
    	
    	SetBuoyant();
    	SetIncineratedInCrucible();
		SetFilterableProperties( m_iFilterable_Small );
    	
    	setUnlocalizedName( "fcItemDung" );
    	
    	setCreativeTab( CreativeTabs.tabMaterials );
    }
    
    @Override
    public boolean itemInteractionForEntity(ItemStack itemstack, EntityLiving entity )
    //public boolean useItemOnEntity( ItemStack itemStack, EntityLiving entity )
    {
        if ( entity instanceof FCEntitySheep )
        {
            entity.attackEntityFrom( DamageSource.generic, 0 );
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean IsPistonPackable( ItemStack stack )
    {
    	return true;
    }
    
    @Override
    public int GetRequiredItemCountToPistonPack( ItemStack stack )
    {
    	return 9;
    }
    
    @Override
    public int GetResultingBlockIDOnPistonPack( ItemStack stack )
    {
    	return FCBetterThanWolves.fcBlockAestheticOpaqueEarth.blockID;
    }
    
    @Override
    public int GetResultingBlockMetadataOnPistonPack( ItemStack stack )
    {
    	return FCBlockAestheticOpaqueEarth.m_iSubtypeDung;
    }
}