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


public abstract class FCBlockMortarReceiverStairs extends FCBlockStairsFalling
{
    public FCBlockMortarReceiverStairs( int iBlockID, Block referenceBlock, int iReferenceBlockMetadata )
    {
        super( iBlockID, referenceBlock, iReferenceBlockMetadata );
    }
    
    @Override
    public void OnBlockDestroyedWithImproperTool( World world, EntityPlayer player, int i, int j, int k, int iMetadata )
    {
        dropBlockAsItem( world, i, j, k, iMetadata, 0 );
    }
    
    @Override
    public void onBlockAdded( World world, int i, int j, int k ) 
    {
    	if ( HasNeighborWithMortarInContact( world, i, j, k ) )
    	{
	        world.playAuxSFX( FCBetterThanWolves.m_iLooseBlockOnMortarAuxFXID, i, j, k, 0 ); 

            world.scheduleBlockUpdate( i, j, k, blockID, FCBlockFalling.m_iTackyFallingBlockTickRate );
    	}
    	else
    	{
    		ScheduleCheckForFall( world, i, j, k );
    	}
    }
    
    @Override
    public int ValidateMetadataForLocation( World world, int i, int j, int k, int iMetadata )
    {
    	// force stairs to be right side up with there's no mortar supporting them
    	
    	if ( GetIsUpsideDown( iMetadata ) )
    	{
    		int iFacing = ConvertDirectionToFacing( GetDirection( iMetadata ) );
    		
    		if ( !HasNeighborWithMortarInContact( world, i, j, k, iFacing, true ) )
    		{
    			iMetadata = SetIsUpsideDown( iMetadata, false );
    		}
    	}
    	
    	return iMetadata;
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
