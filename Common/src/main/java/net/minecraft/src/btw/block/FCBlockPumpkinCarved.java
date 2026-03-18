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

public class FCBlockPumpkinCarved extends BlockPumpkin
{
    public FCBlockPumpkinCarved( int iBlockID )
    {
    	super( iBlockID, false );
    	
    	setHardness( 1F );
    	SetAxesEffectiveOn( true );
    	
    	SetBuoyant();
    	
    	setStepSound( soundWoodFootstep );
    	
    	setUnlocalizedName( "pumpkin" );    	
    }
    
    @Override
    public void onBlockAdded( World world, int i, int j, int k ) 
    {
    	// override to prevent vanilla golem creation
    }
    
    @Override
    public boolean canPlaceBlockAt( World world, int i, int j, int k )
    {
        int iBlockID = world.getBlockId(i, j, k);
        
        return iBlockID == 0 || blocksList[iBlockID].blockMaterial.isReplaceable();
    }
    
    @Override
    public int idDropped( int iMetadata, Random rand, int iFortuneModifier )
    {
        return 0;
    }
    
    @Override
    public void breakBlock( World world, int i, int j, int k, int iBlockID, int iMetadata )
    {
    	super.breakBlock( world, i, j, k, iBlockID, iMetadata );
    	
    	if ( !world.isRemote )
    	{
			world.playAuxSFX( FCBetterThanWolves.m_iMelonImpactSoundAuxFXID, i, j, k, 0 );
    	}
    }
    
    @Override
	public int RotateMetadataAroundJAxis( int iMetadata, boolean bReverse )
	{
		int iDirection = iMetadata & 3;
		
		if ( bReverse )
		{
			iDirection++;
			
			if ( iDirection > 3 )
			{
				iDirection = 0;
			}
		}
		else
		{
			iDirection--;
			
			if ( iDirection < 0 )
			{
				iDirection = 3;
			}
		}		
		
		return ( iMetadata & (~3) ) | iDirection;
	}
    
    @Override
    public boolean CanBeGrazedOn( IBlockAccess blockAccess, int i, int j, int k, 
    	EntityAnimal animal )
    {
		return animal.CanGrazeOnRoughVegetation();
    }
}