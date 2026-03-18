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


public class FCItemMattock extends FCItemRefinedPickAxe
{
    public FCItemMattock( int i )
    {
        super( i );
        
        setUnlocalizedName( "fcItemMattock" );
    }
    
    @Override
    public float getStrVsBlock( ItemStack itemStack, World world, Block block, int i, int j, int k ) 
    {
    	float pickStr = super.getStrVsBlock( itemStack, world, block, i, j, k );
    	float shovelStr = ((FCItemRefinedShovel)(FCBetterThanWolves.fcItemRefinedShovel)).getStrVsBlock( itemStack, world, block, i, j, k );
    	
    	if ( shovelStr > pickStr )
    	{
    		return shovelStr;
    	}
    	else
    	{
    		return pickStr;
    	}
    }

    @Override
    public boolean canHarvestBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	return super.canHarvestBlock( stack, world, block, i, j, k ) || 
    		((FCItemRefinedShovel)(FCBetterThanWolves.fcItemRefinedShovel)).canHarvestBlock( stack, world, block, i, j, k );
    }
    
    @Override
    public boolean IsEfficientVsBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	return super.IsEfficientVsBlock( stack, world, block, i, j, k ) || 
    		((FCItemRefinedShovel)(FCBetterThanWolves.fcItemRefinedShovel)).IsEfficientVsBlock( stack, world, block, i, j, k );
    }    
}