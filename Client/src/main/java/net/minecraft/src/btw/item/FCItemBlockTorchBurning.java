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


public class FCItemBlockTorchBurning extends ItemBlock
{	
    public FCItemBlockTorchBurning( int iItemID )
    {
        super( iItemID );
    }
    
    @Override
    public boolean onItemUse( ItemStack stack, EntityPlayer player, World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ )
    {
        if ( player.canPlayerEdit( i, j, k, iFacing, stack ) )
        {
			if ( AttemptToLightBlock( stack, world, i, j, k, iFacing ) )
			{
	            return true;
			}
        }
        
        return super.onItemUse(  stack, player, world, i, j, k, iFacing, fClickX, fClickY, fClickZ );
    }
    
    @Override
    public boolean GetCanItemStartFireOnUse( int iItemDamage )
    {
    	return true;
    }    
    
    @Override
    public void onUpdate( ItemStack stack, World world, EntityPlayer entity, int iInventorySlot, boolean bIsHandHeldItem )
    {
    	if ( !world.isRemote && stack.stackSize > 0 )
    	{
        	if ( entity.isInWater() && entity.isInsideOfMaterial(Material.water) ) 
        	{
                int iFXI = MathHelper.floor_double( entity.posX );
                int iFXJ = MathHelper.floor_double( entity.posY ) + 1;
                int iFXK = MathHelper.floor_double( entity.posZ );
                
		        world.playAuxSFX( 1004, iFXI, iFXJ, iFXK, 0 ); // fizz sound fx
		        
        		stack.itemID = FCBetterThanWolves.fcBlockTorchNetherUnlit.blockID;       		        		
        	}
    	}
    }

    //------------- Class Specific Methods ------------//
    
    public boolean AttemptToLightBlock( ItemStack stack, World world, int i, int j, int k, int iFacing )
    {
    	int iTargetBlockID = world.getBlockId( i, j, k );
    	Block targetBlock = Block.blocksList[iTargetBlockID];

    	if ( targetBlock != null && targetBlock.GetCanBeSetOnFireDirectlyByItem( world, i, j, k ) )
		{
    		if ( !world.isRemote )
    		{
    			targetBlock.SetOnFireDirectly( world, i, j, k );
    		}
    		
			return true;
		}
    	
    	return false;
    }
    	
	//----------- Client Side Functionality -----------//	
    
    @Override
    public boolean canPlaceItemBlockOnSide( World world, int i, int j, int k, int iFacing, EntityPlayer player, ItemStack itemStack )
    {
    	int iTargetBlockID = world.getBlockId( i, j, k );
    	Block targetBlock = Block.blocksList[iTargetBlockID];

    	if ( targetBlock != null && targetBlock.GetCanBeSetOnFireDirectlyByItem( world, i, j, k ) )
		{
    		return true;
		}
    	
        return super.canPlaceItemBlockOnSide( world, i, j, k, iFacing, player, itemStack );
    }
}
