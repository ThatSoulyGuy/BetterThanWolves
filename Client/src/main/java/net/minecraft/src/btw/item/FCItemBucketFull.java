package net.minecraft.src.btw.item;

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


public abstract class FCItemBucketFull extends FCItemBucket
{
    public FCItemBucketFull( int iItemID )
    {
    	super( iItemID );
	}
    
    @Override
    public ItemStack onItemRightClick( ItemStack itemStack, World world, EntityPlayer player )
    {
    	MovingObjectPosition posClicked = getMovingObjectPositionFromPlayer( world, player, false );
        
        if ( posClicked != null && posClicked.typeOfHit == EnumMovingObjectType.TILE &&
        	world.canMineBlock( player, posClicked.blockX, posClicked.blockY, posClicked.blockZ ) )
        {
        	FCUtilsBlockPos targetPos = new FCUtilsBlockPos( posClicked.blockX,
	            posClicked.blockY, posClicked.blockZ, posClicked.sideHit );

        	if ( player.canPlayerEdit( targetPos.i, targetPos.j, targetPos.k, 
        		posClicked.sideHit, itemStack ) && 
            	AttemptPlaceContentsAtLocation( world, targetPos.i, targetPos.j, targetPos.k ) )
        	{
                if ( !player.capabilities.isCreativeMode )
                {
                    return new ItemStack( Item.bucketEmpty );
                }
        	}
        } 
        
        return itemStack;
    }
    
	//------------- Class Specific Methods ------------//

	public abstract boolean AttemptPlaceContentsAtLocation( World world, int i, int j, int k );
	
	//----------- Client Side Functionality -----------//
}
