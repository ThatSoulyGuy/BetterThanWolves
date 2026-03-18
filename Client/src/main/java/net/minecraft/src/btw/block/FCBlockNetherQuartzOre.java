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


import java.util.Random;

public class FCBlockNetherQuartzOre extends BlockOre
{
    public FCBlockNetherQuartzOre( int iBlockID )
    {
        super( iBlockID );
        
    	SetBlockMaterial( FCBetterThanWolves.fcMaterialNetherRock );
    	
    	setHardness( 1F );
    	setResistance( 5F );
    	
    	setStepSound( soundStoneFootstep );
    	
    	setUnlocalizedName("netherquartz");
    }
    
	//----------- Client Side Functionality -----------//
    
	@Override
    public void registerIcons( IconRegister register )
    {
		blockIcon = register.registerIcon( "fcBlockNetherQuartz" );
    }
	
    @Override
    public boolean RenderBlock( RenderBlocks renderer, int i, int j, int k )
    {
    	return renderer.RenderStandardFullBlock( this, i, j, k );
    }
    
    @Override
    public boolean DoesItemRenderAsBlock( int iItemDamage )
    {
    	return true;
    }    
    
    @Override
    public void RenderBlockMovedByPiston( RenderBlocks renderBlocks, int i, int j, int k )
    {
    	renderBlocks.RenderStandardFullBlockMovedByPiston( this, i, j, k );
    }    
}