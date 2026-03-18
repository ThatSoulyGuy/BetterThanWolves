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


import java.util.List;
import java.util.Random;

public class FCBlockWoodSlab extends BlockHalfSlab
{
    public static final String[] m_sWoodType = new String[] { "oak", "spruce", "birch", "jungle", "blood" };

    public FCBlockWoodSlab( int iBlockID, boolean bDoubleSlab )
    {
        super( iBlockID, bDoubleSlab, FCBetterThanWolves.fcMaterialPlanks );
        
        setHardness( 1F );
        setResistance( 5F );
        SetAxesEffectiveOn();
        
        SetBuoyant();
        
		SetFireProperties( FCEnumFlammability.PLANKS );
		
        setStepSound( soundWoodFootstep );
        
        setUnlocalizedName( "woodSlab" );
        
        setCreativeTab( CreativeTabs.tabBlock );
    }

    @Override
    public int idDropped( int iMetadata, Random rand, int iFortuneModifier )
    {
        return Block.woodSingleSlab.blockID;
    }
    
    @Override
    public String getFullSlabName( int iMetadata )
    {
        return super.getUnlocalizedName() + "." + m_sWoodType[iMetadata];
    }    
    
    @Override
    public ItemStack createStackedBlock( int iMetadata )
    {
        return new ItemStack( Block.woodSingleSlab.blockID, 2, iMetadata & 7 );
    }

    @Override
    public int GetHarvestToolLevel( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 2; // iron or better
    }
    
    @Override
    public void OnBlockDestroyedWithImproperTool( World world, EntityPlayer player, int i, int j, int k, int iMetadata )
    {
    	int iNumItems = isDoubleSlab ? 2 : 1;
    	
    	for ( int iTempCount = 0; iTempCount < iNumItems; iTempCount++ )
    	{
			dropBlockAsItem_do( world, i, j, k, new ItemStack( FCBetterThanWolves.fcItemSawDust ) );
    	}
    }
		
    @Override
    public int GetFurnaceBurnTime( int iItemDamage )
    {
    	int iBurnTime = FCBlockPlanks.GetFurnaceBurnTimeByWoodType( iItemDamage );
    	
    	if ( !isDoubleSlab )
    	{
    		iBurnTime >>= 1;
    	}
    	
    	return iBurnTime;
    }    
		
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
