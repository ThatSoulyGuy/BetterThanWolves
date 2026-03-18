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


public class FCItemShovelStone extends FCItemShovel
{
    public FCItemShovelStone( int iItemID )
    {
        super( iItemID, EnumToolMaterial.STONE );
        
        efficiencyOnProperMaterial /= 3;
        
        setUnlocalizedName("shovelStone");
    }
    
    @Override
    public boolean canHarvestBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	// special casing clay here to avoid having to set tool levels in every block that can be harvested by shovel.  If
    	// more blocks require stone-shovel harvest, consider doing the level thing
    	
    	if ( block == Block.blockClay )
    	{
    		return true;
    	}
    	
    	// stone shovels always drop piles and disturb neighboring blocks, as if dug by hand
    	
        return false;
    }
}
