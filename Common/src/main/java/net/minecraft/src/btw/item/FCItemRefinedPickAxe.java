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


public class FCItemRefinedPickAxe extends FCItemPickaxe
{
    public FCItemRefinedPickAxe( int i )
    {
        super( i, EnumToolMaterial.SOULFORGED_STEEL );
        
        setUnlocalizedName( "fcItemPickAxeRefined" );
    }

    @Override
    public EnumAction getItemUseAction(ItemStack itemstack)
    {
        return EnumAction.block;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack itemstack)
    {
        return 0x11940;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
    {
        entityplayer.setItemInUse(itemstack, getMaxItemUseDuration(itemstack));
        
        return itemstack;
    }

    @Override
    public boolean canHarvestBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	if ( block != null && block.blockMaterial == FCBetterThanWolves.fcMaterialSoulforgedSteel )
    	{
    		return true;
    	}
    	
    	return super.canHarvestBlock( stack, world, block, i, j, k );
    }
    
    @Override
    public float getStrVsBlock( ItemStack toolItemStack, World world, Block block, int i, int j, int k ) 
    {
    	if ( block != null && block.blockMaterial == FCBetterThanWolves.fcMaterialSoulforgedSteel )
    	{
            return efficiencyOnProperMaterial;
    	}
    	
    	return super.getStrVsBlock( toolItemStack, world, block, i, j, k );
    }
    
    @Override
    public boolean GetCanBePlacedAsBlock()
    {
    	return false;
    }    
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}