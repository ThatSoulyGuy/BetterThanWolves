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

public class FCBlockDragonEgg extends BlockDragonEgg
{
    public FCBlockDragonEgg( int iBlockID )
    {
        super( iBlockID );
        
        InitBlockBounds( 0.0625F, 0.0F, 0.0625F, 0.9375F, 1.0F, 0.9375F );
        
        setCreativeTab( CreativeTabs.tabDecorations );
    }
    
	@Override
    public void updateTick( World world, int i, int j, int k, Random rand )
    {
        CheckForFall( world, i, j, k );
    }

	@Override
    public void OnBlockDestroyedLandingFromFall( World world, int i, int j, int k, int iMetadata )
    {
		dropBlockAsItem( world, i, j, k, iMetadata, 0 );
    }
    
	//------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
    
    @Override
    public boolean RenderBlock( RenderBlocks renderer, int i, int j, int k )
    {
        renderer.setRenderBounds( GetBlockBoundsFromPoolBasedOnState( 
        	renderer.blockAccess, i, j, k ) );
        
    	return renderer.renderBlockDragonEgg( this, i, j, k );
    }    
}
