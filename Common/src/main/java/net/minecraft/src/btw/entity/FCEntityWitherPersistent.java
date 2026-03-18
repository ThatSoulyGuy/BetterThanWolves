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


public class FCEntityWitherPersistent extends FCEntityWither
{
    public FCEntityWitherPersistent( World world )
    {
        super( world );
    }
    
    //------------- Class Specific Methods ------------//
    
    static public void SummonWitherAtLocation( World world, int i, int j, int k )
    {
    	// FCTEST: Change this to create new FCEntityWithPersistent. Release as is
        FCEntityWither wither = new FCEntityWither( world );
        
        wither.setLocationAndAngles( (double)i + 0.5D, (double)j - 1.45D, (double)k + 0.5D, 
        	0F, 0F );
        	
        wither.func_82206_m();
        
        world.spawnEntityInWorld( wither );

        world.playAuxSFX( FCBetterThanWolves.m_iWitherCreatedAuxFXID, i, j, k, 0 );
        
        FCUtilsWorld.GameProgressSetWitherHasBeenSummonedServerOnly();
    }    
}