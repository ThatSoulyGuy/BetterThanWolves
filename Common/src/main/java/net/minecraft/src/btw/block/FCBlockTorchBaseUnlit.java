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

public abstract class FCBlockTorchBaseUnlit extends FCBlockTorchBase
{
    public FCBlockTorchBaseUnlit( int iBlockID )
    {
    	super( iBlockID );
    	
        setCreativeTab( null );
    }
    
	@Override
    public boolean GetCanBeSetOnFireDirectly( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return true;
    }
    
	@Override
    public boolean CanBeCrushedByFallingEntity( World world, int i, int j, int k, EntityFallingSand entity )
    {
    	return true;
    }
    
	@Override
    public int GetChanceOfFireSpreadingDirectlyTo( IBlockAccess blockAccess, int i, int j, int k )
    {
		return 60; // same chance as leaves and other highly flammable objects
    }
    
	@Override
    public boolean SetOnFireDirectly( World world, int i, int j, int k )
    {
		world.setBlockAndMetadataWithNotify( i, j, k, GetLitBlockID(), world.getBlockMetadata( i, j, k ) );
		
        world.playSoundEffect( (double)i + 0.5D, (double)j + 0.5D, (double)k + 0.5D, 
        	"mob.ghast.fireball", 1.0F, world.rand.nextFloat() * 0.4F + 0.8F );
        
        return true;
    }
	
    @Override
    public boolean CanGroundCoverRestOnBlock( World world, int i, int j, int k )
    {
    	return world.doesBlockHaveSolidTopSurface( i, j - 1, k );
    }
    
    @Override
    public float GroundCoverRestingOnVisualOffset( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return -1F;        
    }
    
    //------------- Class Specific Methods ------------//    

	public abstract int GetLitBlockID();
	
	//----------- Client Side Functionality -----------//
}
