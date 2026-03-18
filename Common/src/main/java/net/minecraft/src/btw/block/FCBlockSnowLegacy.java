package net.minecraft.src.btw.block;

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


public class FCBlockSnowLegacy extends BlockSnowBlock
{
    public FCBlockSnowLegacy( int iBlockID )
    {
    	super( iBlockID );
    	
    	setHardness( 0.2F );
    	SetShovelsEffectiveOn();
    	
    	SetBuoyant();
    	
    	setStepSound( soundSnowFootstep );
    	
    	setUnlocalizedName( "snow" );
    	
        setCreativeTab( null );    	
    }
    
    @Override
    public boolean CanBePistonShoveled( World world, int i, int j, int k )
    {
    	return true;
    }
}
