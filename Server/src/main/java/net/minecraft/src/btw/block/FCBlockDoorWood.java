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

public class FCBlockDoorWood extends FCBlockDoor
{
    public FCBlockDoorWood( int iBlockID )
    {
        super( iBlockID, Material.wood );
        
        setHardness( 1.5F );
        SetBuoyant();
        
        setStepSound( soundWoodFootstep );
        
        setUnlocalizedName( "doorWood" );
        
        disableStats();        
    }
    
    @Override
    public boolean CanPathThroughBlock( IBlockAccess blockAccess, int i, int j, int k, Entity entity, PathFinder pathFinder )
    {
    	if ( !pathFinder.CanPathThroughClosedWoodDoor() )
    	{
    		// note: getBlocksMovement() is misnamed and returns if the block *doesn't* block movement
    		
	    	if ( !pathFinder.CanPathThroughOpenWoodDoor() || !getBlocksMovement( blockAccess, i, j, k ) )
	    	{
	    		return false;
	    	}
    	}
    	
    	return true;
    }
    
    @Override
    public boolean IsBreakableBarricade( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return true;
    }

    @Override
    public boolean IsBreakableBarricadeOpen( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return isDoorOpen( blockAccess, i, j, k );
    }
    
    @Override
    public void onPoweredBlockChange(World par1World, int par2, int par3, int par4, boolean par5)
    {
    	// override to remove the ability of wooden doors to react to redstone signal
    }
    
    @Override
    public void OnAIOpenDoor( World world, int i, int j, int k, boolean bOpen )
    {
    	super.onPoweredBlockChange( world, i, j, k, bOpen );    	
    }
}
