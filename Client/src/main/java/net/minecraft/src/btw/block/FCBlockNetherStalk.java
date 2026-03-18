package net.minecraft.src.btw.block;

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


import java.util.Random;

public class FCBlockNetherStalk extends BlockNetherStalk
{
    public FCBlockNetherStalk( int iBlockID )
    {
    	super( iBlockID );
    	
        InitBlockBounds( 0D, 0D, 0D, 1D, 0.25F, 1D );
        
        setStepSound( soundGrassFootstep );
    }
    
    @Override
    public boolean canBlockStay( World world, int i, int j, int k )
    {
        return CanGrowOnBlock( world, i, j - 1, k );    	
    }
    
    @Override
    public void updateTick( World world, int i, int j, int k, Random rand )
    {
        // prevent growth outside of the nether
        
        if ( world.provider.dimensionId != -1 )
        {
        	// replicate update functionality two levels above us in the hierarchy
        	
            checkFlowerChange( world, i, j, k );
        }
        else
        {        
        	super.updateTick( world, i, j, k, rand );
        }
    }
    
    @Override
    public boolean CanGrowOnBlock( World world, int i, int j, int k )
    {
    	Block blockOn = Block.blocksList[world.getBlockId( i, j, k )];
    	
    	return blockOn != null && blockOn.CanNetherWartGrowOnBlock( world, i, j, k );
    }
    
    //------------- Class Specific Methods ------------//

	//----------- Client Side Functionality -----------//
    
    @Override
    public boolean RenderBlock( RenderBlocks renderer, int i, int j, int k )
    {
        renderer.setRenderBounds( GetBlockBoundsFromPoolBasedOnState( 
        	renderer.blockAccess, i, j, k ) );
        
    	return renderer.renderBlockCrops( this, i, j, k );
    }    
}
