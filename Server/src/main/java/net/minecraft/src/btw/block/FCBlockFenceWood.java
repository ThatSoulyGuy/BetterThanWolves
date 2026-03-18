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


public class FCBlockFenceWood extends FCBlockFence
{
    public FCBlockFenceWood( int iBlockID )
    {
        super( iBlockID, "wood", FCBetterThanWolves.fcMaterialPlanks );
        
        setHardness( 1.5F );
        setResistance( 5F );
        
        SetAxesEffectiveOn();
        
        SetBuoyant();
        
		SetFireProperties( FCEnumFlammability.PLANKS );
		
        setStepSound( soundWoodFootstep );
        
        setUnlocalizedName( "fence" );
    }
    
	@Override
    public int GetItemIDDroppedOnSaw( World world, int i, int j, int k )
    {
		return FCBetterThanWolves.fcBlockWoodCornerItemStubID;
    }
	
	@Override
    public int GetItemCountDroppedOnSaw( World world, int i, int j, int k )
    {
		return 2;
    }	
	
    @Override
    public int GetHarvestToolLevel( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 2; // iron or better
    }
    
	@Override
	public boolean DropComponentItemsOnBadBreak( World world, int i, int j, int k, int iMetadata, float fChanceOfDrop )
	{
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemSawDust.itemID, 2, 0, fChanceOfDrop );
		
		return true;
	}	
	
	//----------- Client Side Functionality -----------//
}
