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


public class FCBlockWickerSlab extends FCBlockSlab
{
    public FCBlockWickerSlab( int iBlockID )
    {
        super( iBlockID, FCBetterThanWolves.fcMaterialWicker );
        
        setHardness( 0.5F );        
        SetAxesEffectiveOn();
        
        SetBuoyant();
        
		SetFireProperties( FCEnumFlammability.WICKER );
		
        setStepSound( soundGrassFootstep );        
        
        setUnlocalizedName( "fcBlockWickerSlab" );
        
        setCreativeTab( CreativeTabs.tabBlock );
    }
    
	@Override
    public boolean DoesBlockBreakSaw( World world, int i, int j, int k )
    {
		return false;
    }
    
	@Override
    public int GetItemIDDroppedOnSaw( World world, int i, int j, int k )
    {
		return FCBetterThanWolves.fcBlockWickerPane.blockID;
    }
	
	@Override
    public int GetItemCountDroppedOnSaw( World world, int i, int j, int k )
    {
		return 2;
    }
	
	@Override
	public boolean DropComponentItemsOnBadBreak( World world, int i, int j, int k, 
		int iMetadata, float fChanceOfDrop )
	{
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemWickerPiece.itemID, 
			1, 0, fChanceOfDrop );
		
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemSawDust.itemID, 
			3, 0, fChanceOfDrop );
		
		return true;
	}
	
	@Override
	public int GetCombinedBlockID( int iMetadata )
	{
		return FCBetterThanWolves.fcBlockWicker.blockID;
	}
	
	@Override
    public boolean CanToolsStickInBlock( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return false;
    }
    
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
	
	@Override
    public void registerIcons( IconRegister register )
    {
        blockIcon = register.registerIcon( "fcBlockWicker" );
    }
}
