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


public class FCItemChiselStone extends FCItemChisel
{	
    public FCItemChiselStone( int iItemID )
    {
        super( iItemID, EnumToolMaterial.STONE, 8 );
        
        SetFilterableProperties( Item.m_iFilterable_Small );
        
        efficiencyOnProperMaterial /= 2;
        
        setUnlocalizedName( "fcItemChiselStone" );        
    }
    
    @Override
    public float getStrVsBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	float fStrength = super.getStrVsBlock( stack, world, block, i, j, k );
    	
    	if ( block.blockID == Block.web.blockID || block.blockID == FCBetterThanWolves.fcBlockWeb.blockID )
    	{
    		// boost stone chisels against webs so that it reads a little better
    		
    		fStrength *= 2;
    	}
    	
    	return fStrength;
    }
}
