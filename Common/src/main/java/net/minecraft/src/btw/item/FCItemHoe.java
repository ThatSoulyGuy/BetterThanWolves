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


public class FCItemHoe extends FCItemTool
{
    public EnumToolMaterial theToolMaterial;

    public FCItemHoe( int iItemID, EnumToolMaterial material )
    {
        super( iItemID, 1, material );        
        
        if ( material.getHarvestLevel() <= 2 ) // iron or worse
        {
        	efficiencyOnProperMaterial /= 8;
        }
        else
        {
        	efficiencyOnProperMaterial /= 4;
        }
    }

    @Override
    public boolean canHarvestBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	// hoes don't harvest, they only convert
    	
    	return false;
    }
    
    @Override
    public boolean IsToolTypeEfficientVsBlockType( Block block )
    {
    	return block.AreHoesEffectiveOn();
    }

    @Override
    public boolean CanToolStickInBlock( ItemStack stack, Block block, World world, int i, int j, int k )
    {
		return super.CanToolStickInBlock( stack, block, world, i, j, k ) ||
			block.AreShovelsEffectiveOn();			
    }
    
    @Override
    public float GetVisualVerticalOffsetAsBlock()
    {
    	return 0.80F;
    }    
    
    @Override
    public float GetBlockBoundingBoxHeight()
    {
    	return 0.79F;
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
