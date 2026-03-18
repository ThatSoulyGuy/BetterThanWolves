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


public class FCEntityMagmaCube extends EntityMagmaCube
{
    public FCEntityMagmaCube( World world )
    {
        super( world );
        
        landMovementFactor = 0.5F; // unifying client and server values in parent
    }
    
    @Override
    public boolean canDamagePlayer()
    {
    	return isEntityAlive() && attackTime <= 0;
    }

    @Override
    public void CheckForScrollDrop()
    {    	
        if ( getSlimeSize() == 1 && rand.nextInt( 250 ) == 0 )
        {
            ItemStack itemstack = new ItemStack( FCBetterThanWolves.fcItemArcaneScroll, 1, 
            	Enchantment.fireAspect.effectId );
            
            entityDropItem( itemstack, 0F );
        }
    }
    
    @Override
    public EntitySlime createInstance()
    {
        return new FCEntityMagmaCube( worldObj );
    }

    //------------- Class Specific Methods ------------//
}
