package net.minecraft.src.btw.block;

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


import java.util.Random;

public class FCBlockGravel extends FCBlockFallingFullBlock
{
    public FCBlockGravel( int iBlockID )
    {
        super( iBlockID, Material.sand );
        
        setHardness( 0.6F );
        SetShovelsEffectiveOn();
		SetFilterableProperties( Item.m_iFilterable_Fine );
        
        setStepSound( soundGravelFootstep );
        
        setUnlocalizedName( "gravel" );
        
        setCreativeTab( CreativeTabs.tabBlock );        
    }

    public int idDropped( int iMetadata, Random rand, int  iFortuneModifier )
    {
    	// only drop gravel with fortune enchant...no flint
    	
        if ( iFortuneModifier > 0 || rand.nextInt( 10 ) != 0 )
        {
             return blockID;
        }

        return Item.flint.itemID;
    }
    
    @Override
    public void OnBlockDestroyedWithImproperTool( World world, EntityPlayer player, int i, int j, int k, int iMetadata )
    {
    	if ( world.rand.nextInt( 10 ) == 0 )
    	{
    		DropItemsIndividualy( world, i, j, k, Item.flint.itemID, 1, 0, 1F );
    	}
    	else
    	{
    		super.OnBlockDestroyedWithImproperTool( world, player, i, j, k, iMetadata );
    	}
    }
    
	@Override
	public boolean DropComponentItemsOnBadBreak( World world, int i, int j, int k, int iMetadata, float fChanceOfDrop )
	{
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemPileGravel.itemID, 6, 0, fChanceOfDrop );
		
		return true;
	}
    
	@Override
    public boolean CanBePistonShoveled( World world, int i, int j, int k )
    {
    	return true;
    }
	
    //------------- Class Specific Methods ------------//    
    
	//----------- Client Side Functionality -----------//
}