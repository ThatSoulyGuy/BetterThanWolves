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


public class FCItemSoulUrn extends FCItemThrowable
{
    public FCItemSoulUrn( int iItemID )
    {
    	super( iItemID );
    	
        maxStackSize = 16;
        
        SetBuoyant();
        
    	setUnlocalizedName( "fcItemUrnSoul" );
    	
    	setCreativeTab( CreativeTabs.tabMaterials );
    }
    
    @Override
    public boolean onItemUse( ItemStack itemStack, EntityPlayer player, World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ )    
    {
		if ( world.provider.dimensionId == 1 )
		{
			return false;
		}

		boolean bUrnUsed = false;
		boolean bPlayShatterEffect = true;
		
        int iTargetBlockID = world.getBlockId( i, j, k );
        
        if ( iTargetBlockID == FCBetterThanWolves.fcAestheticVegetation.blockID )
        {
        	int iTargetSubType = world.getBlockMetadata( i, j, k );
        	
        	if ( iTargetSubType == FCBlockAestheticVegetation.m_iSubtypeBloodWoodSapling )
        	{
                if ( !world.isRemote )
                {
                    ((FCBlockAestheticVegetation)FCBetterThanWolves.fcAestheticVegetation).AttemptToGrowBloodwoodSapling( world, i, j, k, world.rand );	                    
                }
            
                bUrnUsed = true;
        	}
            
        }
        else if ( iTargetBlockID == Block.netherStalk.blockID )
        {
            int iTargetMetadata = world.getBlockMetadata( i, j, k );

            if ( iTargetMetadata < 3 )
            {
                if ( !world.isRemote  )
                {
                	world.setBlockMetadataWithNotify( i, j, k, 3 );
                	
                	world.markBlockRangeForRenderUpdate( i, j, k, i, j, k );	                	
                }
                
                bUrnUsed = true;
            }
        }
        else if ( iTargetBlockID == FCBetterThanWolves.fcBlockBloodMoss.blockID )
        {
        	FCBlockNetherGrowth bloodMoss = (FCBlockNetherGrowth)FCBetterThanWolves.fcBlockBloodMoss;
        	int iHeightLevel = bloodMoss.GetHeightLevel( world, i, j, k );
        	
        	if ( iHeightLevel < 7 )
        	{
				bloodMoss.SetHeightLevel( world, i, j, k, 7 );
				
		        world.markBlockRangeForRenderUpdate( i, j, k, i, j, k );    		        
        	}
        	
            bUrnUsed = true;
        }
        else if ( !world.isRemote && FCEntityUrn.AttemptToCreateGolemOrWither( world, i, j, k ) )
    	{
			bUrnUsed = true;
			
    		bPlayShatterEffect = false;            		
    	}
        
        if ( bUrnUsed )
        {
            if ( !world.isRemote )
            {
                if ( !player.capabilities.isCreativeMode )
                {
                	itemStack.stackSize--;
                }
                
                if ( bPlayShatterEffect )
                {
                	world.playAuxSFX( FCBetterThanWolves.m_iSoulUrnShatterAuxFXID, i, j, k, 0 );
                }
            }
            
            return true;
        }
    	
		return false;
    }
    
    @Override
    public void SpawnThrownEntity( ItemStack stack, World world, 
    	EntityPlayer player )
    {
        world.spawnEntityInWorld( new FCEntityUrn( world, player, itemID ) );
    }
    
    @Override
    public EntityThrowable GetEntityFiredByByBlockDispenser( World world, 
    	double dXPos, double dYPos, double dZPos )
    {
    	return new FCEntityUrn( world, dXPos, dYPos, dZPos, itemID );
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//

    @Override
    public boolean hasEffect( ItemStack itemStack )
    {
		return true;
    }
}
