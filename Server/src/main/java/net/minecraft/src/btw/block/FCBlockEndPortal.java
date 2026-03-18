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

public class FCBlockEndPortal extends BlockEndPortal
{
    public FCBlockEndPortal( int iBlockID, Material material )
    {
        super( iBlockID, material );
        
        InitBlockBounds( 0F, 0F, 0F, 1F, 0.0625F, 1F );        
        
        setTickRandomly( true );
    }
    
    @Override
    public void onBlockAdded( World world, int i, int j, int k )
    {
    	super.onBlockAdded( world, i, j, k );
    	
		FCUtilsWorld.GameProgressSetEndDimensionHasBeenAccessedServerOnly();
    }
    
    @Override
    public void updateTick( World world, int i, int j, int k, Random rand )
    {
    	super.updateTick( world, i, j, k, rand );
    	
		FCUtilsWorld.GameProgressSetEndDimensionHasBeenAccessedServerOnly();
    }
    	
	@Override
    public void setBlockBoundsBasedOnState( IBlockAccess blockAccess, int i, int j, int k )
    {
    	// override to deprecate parent
    }
	
    @Override
    public ItemStack GetStackRetrievedByBlockDispenser( World world, int i, int j, int k )
    {	 
    	return null; // can't be picked up
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
