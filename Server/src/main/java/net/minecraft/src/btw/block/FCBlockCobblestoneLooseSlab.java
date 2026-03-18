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


public class FCBlockCobblestoneLooseSlab extends FCBlockMortarReceiverSlab
{
    public FCBlockCobblestoneLooseSlab( int iBlockID )
    {
        super( iBlockID, Material.rock );
        
        setHardness( 1F ); // setHardness( 2F ); regular cobble
        setResistance( 5F ); // setResistance( 10F ); regular cobble
        
        SetPicksEffectiveOn();
        SetChiselsEffectiveOn();
        
        setStepSound( soundStoneFootstep );
        
        setUnlocalizedName( "fcBlockCobblestoneLooseSlab" );
        
		setCreativeTab( CreativeTabs.tabBlock );
    }
    
	@Override
	public int GetCombinedBlockID( int iMetadata )
	{
		return FCBetterThanWolves.fcBlockCobblestoneLoose.blockID;
	}

    @Override
    public boolean OnMortarApplied( World world, int i, int j, int k )
    {
		int iNewMetadata = 3; // metadata 3 is cobble slab
		
		if ( GetIsUpsideDown( world, i, j, k ) )
		{
			iNewMetadata |= 8;
		}
		
		world.setBlockAndMetadataWithNotify( i, j, k, Block.stoneSingleSlab.blockID, iNewMetadata );
		
		return true;
    }
    
    //------------- Class Specific Methods ------------//    
    
	//----------- Client Side Functionality -----------//
}