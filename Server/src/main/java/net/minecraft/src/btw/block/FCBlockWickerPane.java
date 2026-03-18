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


public class FCBlockWickerPane extends FCBlockPane
{
    public FCBlockWickerPane( int iBlockID )
    {
        super( iBlockID, "fcBlockWicker", "fcBlockWicker", 
        	FCBetterThanWolves.fcMaterialWicker, false );
        
        setHardness( 0.5F );        
        SetAxesEffectiveOn();
		
        SetBuoyant();
        
		SetFireProperties( FCEnumFlammability.WICKER );
		
        setLightOpacity( 4 );
        Block.useNeighborBrightness[iBlockID] = true;
        
        setStepSound( soundGrassFootstep );        
        
        setUnlocalizedName( "fcBlockWickerPane" );
    }
    
	@Override
    public boolean DoesBlockBreakSaw( World world, int i, int j, int k )
    {
		return false;
    }
    
	@Override
	public boolean DropComponentItemsOnBadBreak( World world, int i, int j, int k, 
		int iMetadata, float fChanceOfDrop )
	{
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemWickerPiece.itemID, 
			1, 0, fChanceOfDrop );
		
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemSawDust.itemID, 
			2, 0, fChanceOfDrop );
		
		return true;
	}
	
    @Override
    public boolean CanItemPassIfFilter( ItemStack filteredItem )
    {
    	int iFilterableProperties = filteredItem.getItem().GetFilterableProperties( filteredItem ); 
    		
    	return ( iFilterableProperties & Item.m_iFilterable_Fine ) != 0;
    }
    
    @Override
    public boolean CanTransformItemIfFilter( ItemStack filteredItem )
    {
    	return filteredItem.itemID == Block.gravel.blockID;
    }
    
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
