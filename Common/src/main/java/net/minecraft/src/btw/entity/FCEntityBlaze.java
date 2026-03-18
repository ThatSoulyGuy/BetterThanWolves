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


public class FCEntityBlaze extends EntityBlaze
{
    public FCEntityBlaze( World world )
    {
        super( world );
    }
    
    @Override
    public void dropFewItems( boolean bKilledByPlayer, int iLootingModifier )
    {
    	// treat as always killed by player to override vanilla behavior of only dropping rods
    	// when killed by player
    	
    	super.dropFewItems( true, iLootingModifier );
    }
    
    @Override
    public void CheckForScrollDrop()
    {    	
    	if ( rand.nextInt( 500 ) == 0 )
    	{
            ItemStack itemstack = new ItemStack( FCBetterThanWolves.fcItemArcaneScroll, 1, 
            	Enchantment.flame.effectId );
            
            entityDropItem( itemstack, 0F );
    	}
    }
}
