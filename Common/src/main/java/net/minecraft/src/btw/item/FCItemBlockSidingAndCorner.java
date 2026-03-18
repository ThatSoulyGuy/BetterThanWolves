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


public class FCItemBlockSidingAndCorner extends ItemBlock
{
    public FCItemBlockSidingAndCorner( int iItemID )
    {
        super( iItemID );
        
        setMaxDamage( 0 );
        setHasSubtypes(true);
        
        setUnlocalizedName( Block.blocksList[getBlockID()].getUnlocalizedName() );
    }
    
    @Override
    public int getMetadata( int iItemDamage )
    {
        return iItemDamage;
    }
    
    @Override
    public String getUnlocalizedName( ItemStack itemstack )
    {
    	if( itemstack.getItemDamage() == FCBlockSidingAndCornerAndDecorative.m_iSubtypeBench )
    	{
            return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("bench").toString();
    	}
    	else if( itemstack.getItemDamage() == FCBlockSidingAndCornerAndDecorative.m_iSubtypeFence )
    	{
            return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("fence").toString();
    	}
    	else if( itemstack.getItemDamage() == 0 )
    	{
            return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("siding").toString();
    	}
    	else
    	{
            return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("corner").toString();
    	}
    }
}
