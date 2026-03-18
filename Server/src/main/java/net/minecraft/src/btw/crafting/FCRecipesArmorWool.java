package net.minecraft.src.btw.crafting;

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


public class FCRecipesArmorWool extends ShapedRecipes
{
    public FCRecipesArmorWool( int par1, int par2, ItemStack[] par3ArrayOfItemStack, ItemStack par4ItemStack )
    {
    	super( par1, par2, par3ArrayOfItemStack, par4ItemStack );
    }
    
    @Override
    public ItemStack getCraftingResult( InventoryCrafting inventory )
    {
    	ItemStack resultStack = super.getCraftingResult( inventory );
    	
    	if ( resultStack != null && resultStack.getItem() instanceof FCItemArmorWool )
    	{
    		int iAverageColor = FCItemWool.AverageWoolColorsInGrid( inventory );
    		
    		FCItemArmorWool woolArmorItem = (FCItemArmorWool)resultStack.getItem();
    		
            woolArmorItem.func_82813_b( resultStack, iAverageColor );
    	}
    	
    	return resultStack;
    }
}
