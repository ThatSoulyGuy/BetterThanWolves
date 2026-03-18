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


public abstract class FCBlockMortarReceiver extends FCBlockFallingFullBlock
{
    public FCBlockMortarReceiver( int iBlockID, Material material )
    {
    	super( iBlockID, material );
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

            world.scheduleBlockUpdate( i, j, k, blockID, m_iTackyFallingBlockTickRate );
    	}
    	else
    	{
    		ScheduleCheckForFall( world, i, j, k );
    	}
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//    
}
