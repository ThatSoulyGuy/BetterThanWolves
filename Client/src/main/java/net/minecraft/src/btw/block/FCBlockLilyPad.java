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


public class FCBlockLilyPad extends BlockLilyPad
{
    public FCBlockLilyPad( int iBlockID )
    {
        super(iBlockID);
        
        SetBuoyant();
        
        InitBlockBounds( 0D, 0D, 0D, 1D, 0.015625D, 1D );
    }
    
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool( World world, int i, int j, int k )
    {
    	return GetBlockBoundsFromPoolBasedOnState( world, i, j, k ).offset( i, j, k );
    }
    
    @Override
    public boolean CanGrowOnBlock( World world, int i, int j, int k )
    {
    	return world.getBlockId( i, j, k ) == Block.waterStill.blockID;
    }
    
    @Override
    public boolean CanGroundCoverRestOnBlock( World world, int i, int j, int k )
    {
    	return false;
    }
    
	//------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
    
    @Override
    public boolean RenderBlock( RenderBlocks renderer, int i, int j, int k )
    {
        renderer.setRenderBounds( GetBlockBoundsFromPoolBasedOnState( 
        	renderer.blockAccess, i, j, k ) );
        
    	return renderer.renderBlockLilyPad( this, i, j, k );
    }    
}
