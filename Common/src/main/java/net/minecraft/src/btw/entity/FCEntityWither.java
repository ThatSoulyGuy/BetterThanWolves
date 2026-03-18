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


public class FCEntityWither extends EntityWither
{
    public FCEntityWither( World world )
    {
        super( world );
        
        tasks.RemoveAllTasksOfClass( EntityAIWander.class );
        
        tasks.addTask( 5, new FCEntityAIWanderSimple( this, moveSpeed ) );
    }
    
    @Override
    public void CheckForScrollDrop()
    {    	
        ItemStack stack = new ItemStack( FCBetterThanWolves.fcItemArcaneScroll, 1, 
        	Enchantment.knockback.effectId );
        
        entityDropItem( stack, 0F );
    }
    
    @Override
    public void ModSpecificOnLivingUpdate()
    {
    	super.ModSpecificOnLivingUpdate();
    	
    	if ( !worldObj.isRemote )
    	{            
            FCUtilsWorld.GameProgressSetWitherHasBeenSummonedServerOnly();
    	}
    }
    
    //------------- Class Specific Methods ------------//
}
