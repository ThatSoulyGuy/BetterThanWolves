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


public class FCItemSeeds extends Item
{
	public final int m_iCropBlockID;
	
    public FCItemSeeds( int iItemID, int iCropBlockID )
    {
    	super( iItemID );
    	
    	m_iCropBlockID = iCropBlockID;
    	
        SetBuoyant();        
        SetBellowsBlowDistance( 2 );        
        SetIncineratedInCrucible();
		SetFilterableProperties( m_iFilterable_Fine );
    	
    	setCreativeTab( CreativeTabs.tabMaterials );    
    }
    
    @Override
    public boolean onItemUse( ItemStack itemStack, EntityPlayer player, World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ )
    {
        if ( iFacing == 1 )
        {
	        if ( player == null || ( player.canPlayerEdit( i, j, k, iFacing, itemStack ) && 
	        	player.canPlayerEdit( i, j + 1, k, iFacing, itemStack ) ) )
	        {
	            Block cropBlock = Block.blocksList[m_iCropBlockID]; 
	        	
	            if ( cropBlock != null && cropBlock.canPlaceBlockAt( world, i, j + 1, k ) )
	            {
	                world.setBlockWithNotify( i, j + 1, k, m_iCropBlockID );
	                
	                world.playSoundEffect( i + 0.5D, j + 0.5D, k + 0.5D, 
	                	Block.soundGrassFootstep.getStepSound(), 
	                	( Block.soundGrassFootstep.getVolume() + 1F ) / 2F, 
	                	Block.soundGrassFootstep.getPitch() * 0.8F );
	                
	                itemStack.stackSize--;
	                
	                return true;
	            }
	        }
        }
        
        return false;
    }
    
    @Override
	public boolean OnItemUsedByBlockDispenser( ItemStack stack, World world, 
		int i, int j, int k, int iFacing )
	{
    	FCUtilsBlockPos targetPos = new FCUtilsBlockPos( i, j, k, iFacing );
        Block cropBlock = Block.blocksList[m_iCropBlockID]; 
    	
        if ( cropBlock != null && cropBlock.canPlaceBlockAt( world, 
        	targetPos.i, targetPos.j, targetPos.k ) )
        {
            world.setBlockWithNotify( targetPos.i, targetPos.j, targetPos.k, m_iCropBlockID );
            
            world.playSoundEffect( targetPos.i + 0.5D, targetPos.j + 0.5D, targetPos.k + 0.5D, 
            	Block.soundGrassFootstep.getStepSound(), 
            	( Block.soundGrassFootstep.getVolume() + 1F ) / 2F, 
            	Block.soundGrassFootstep.getPitch() * 0.8F );
            
            return true;
        }
        
        return false;
	}
    
    //------------- Class Specific Methods ------------//

	//----------- Client Side Functionality -----------//    
}
