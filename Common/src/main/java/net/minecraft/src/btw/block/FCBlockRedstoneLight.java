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

public class FCBlockRedstoneLight extends BlockRedstoneLight
{
	// copy of parent variable to get around private declaration
	
    private final boolean powered;

    public FCBlockRedstoneLight( int iBlockID, boolean bIsLit )
    {
    	super( iBlockID, bIsLit );
    	
    	SetPicksEffectiveOn( true );
    	
    	powered = bIsLit;
    }

    @Override
    public boolean canSilkHarvest()
    {
        return false;
    }
    
    @Override
    public void onBlockAdded( World world, int i, int j, int k)
    {
    	// copy of parent function in order to provide block notifications on setBlock()
    	
        if ( !world.isRemote )
        {
            if ( powered && !world.isBlockIndirectlyGettingPowered( i, j, k ) )
            {
                world.scheduleBlockUpdate( i, j, k, blockID, 4 );
            }
            else if ( !powered && world.isBlockIndirectlyGettingPowered( i, j, k ) )
            {
                world.setBlock( i, j, k, Block.redstoneLampActive.blockID );
            }
        }
    }
    
    @Override
    public void onNeighborBlockChange( World world, int i, int j, int k, int iNeighborBlockID )
    {
    	// copy of parent function in order to provide block notifications on setBlock()
    	// and to check for already pending updates.
    	
        if ( powered )
        {
        	if ( !world.isBlockIndirectlyGettingPowered( i, j, k ) &&
        		!world.IsUpdatePendingThisTickForBlock( i, j, k, blockID ) )
        	{
        		world.scheduleBlockUpdate( i, j, k, blockID, 4 );
        	}
        }
        else 
        {
        	if ( world.isBlockIndirectlyGettingPowered( i, j, k ) )
        	{
        		world.setBlockWithNotify( i, j, k, Block.redstoneLampActive.blockID );
        	}
        }
    }
    
    @Override
    public void updateTick( World world, int i, int j, int k, Random rand )
    {
    	// copy of parent function in order to provide block notifications on setBlock()
    	
        if ( !world.isRemote && powered && !world.isBlockIndirectlyGettingPowered( i, j, k ) )
        {
            world.setBlock( i, j, k, Block.redstoneLampIdle.blockID );
        }
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
