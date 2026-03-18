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


public class FCItemBone extends Item
{
    public FCItemBone( int iItemID )
    {
        super( iItemID );
        
        maxStackSize = 16;
        
        SetBuoyant();
        SetIncineratedInCrucible();
        SetFilterableProperties( m_iFilterable_Narrow );

        setFull3D();
        
        setUnlocalizedName( "bone" );
        
        setCreativeTab( CreativeTabs.tabMisc );
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
    	return FCBetterThanWolves.fcAestheticOpaque.blockID;
    }
    
    @Override
    public int GetResultingBlockMetadataOnPistonPack( ItemStack stack )
    {
    	return FCBlockAestheticOpaque.m_iSubtypeBone;
    }
}
