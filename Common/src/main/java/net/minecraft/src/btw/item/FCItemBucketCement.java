package net.minecraft.src.btw.item;

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


public class FCItemBucketCement extends FCItemBucketFull
{
    public FCItemBucketCement( int iBlockID )
    {
    	super( iBlockID );
    	
    	setUnlocalizedName( "fcItemBucketCement" );
	}

    @Override
    public int getBlockID()
    {
        return FCBetterThanWolves.fcBlockBucketCement.blockID;
    }

    @Override
	public boolean AttemptPlaceContentsAtLocation( World world, int i, int j, int k )
	{
        if ( ( world.isAirBlock( i, j, k ) || !world.getBlockMaterial( i, j, k ).isSolid() ) )
        {            
    		if ( !world.isRemote )
    		{        			
    	    	world.playSoundEffect( i, j, k, "mob.ghast.moan", 
    				0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
    	    	
    			world.setBlockWithNotify( i, j, k, FCBetterThanWolves.fcCement.blockID );
    		}
            
            return true;
        }
        
        return false;
	}
}