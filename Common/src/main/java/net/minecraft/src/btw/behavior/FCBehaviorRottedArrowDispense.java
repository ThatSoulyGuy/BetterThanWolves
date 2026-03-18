package net.minecraft.src.btw.behavior;

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



public class FCBehaviorRottedArrowDispense extends BehaviorProjectileDispense
{
    public FCBehaviorRottedArrowDispense()
    {
    }

    /**
     * Return the projectile entity spawned by this dispense behavior.
     */
    public IProjectile getProjectileEntity( World world, IPosition position)
    {
    	FCEntityRottenArrow arrow = new FCEntityRottenArrow( world, position.getX(), position.getY(), position.getZ() );
        
        arrow.canBePickedUp = 2;
        
        return arrow;        
    }
}
