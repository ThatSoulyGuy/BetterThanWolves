package net.minecraft.src.btw.entity;

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


public class FCEntityBat extends EntityBat
{
    public FCEntityBat( World world )
    {
        super( world );
    }
    
    @Override
    public void CheckForScrollDrop()
    {    	
    	if ( rand.nextInt( 250 ) == 0 )
    	{
            ItemStack itemstack = new ItemStack( FCBetterThanWolves.fcItemArcaneScroll, 1, 
            	Enchantment.featherFalling.effectId );
            
            entityDropItem( itemstack, 0F );
    	}
    }
    
    @Override
    public void dropFewItems( boolean bPlayerKilled, int iFortuneLevel )
    {
    	int iNumDrop = 1;
    	
    	if ( rand.nextInt( 4 ) - iFortuneLevel <= 0 )
    	{
    		iNumDrop = 2;
    	}
    	
        for ( int iTempCount = 0; iTempCount < iNumDrop; iTempCount++ )
        {
            dropItem( FCBetterThanWolves.fcItemBatWing.itemID, 1 );
        }    	
    }
    
    @Override
    public boolean AttractsLightning()
    {
    	return false;
    }
}