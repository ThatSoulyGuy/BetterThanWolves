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


public class FCItemShovel extends FCItemTool
{
    public FCItemShovel( int iItemID, EnumToolMaterial material )
    {
        super( iItemID, 1, material );
    }

    public FCItemShovel( int iItemID, EnumToolMaterial material, int iMaxUses )
    {
        super( iItemID, 1, material );
        
        setMaxDamage( iMaxUses );
    }

    @Override
    public boolean canHarvestBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
        return IsToolTypeEfficientVsBlockType( block );
    }
    
    @Override
    public boolean IsToolTypeEfficientVsBlockType( Block block )
    {
    	return block.AreShovelsEffectiveOn();
    }
    
    @Override
    public float GetVisualVerticalOffsetAsBlock()
    {
    	return 0.70F;
    }    
    
    @Override
    public float GetVisualRollOffsetAsBlock()
    {
    	return -15F;
    }
}
