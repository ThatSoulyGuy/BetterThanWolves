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


public class FCItemRefinedShovel extends FCItemShovel
{
    public FCItemRefinedShovel( int iItemID )
    {
        super( iItemID, EnumToolMaterial.SOULFORGED_STEEL );
        
        setUnlocalizedName( "fcItemShovelRefined" );        
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
    public boolean GetCanBePlacedAsBlock()
    {
    	return false;
    }    

    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}