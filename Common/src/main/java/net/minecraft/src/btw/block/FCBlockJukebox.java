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


public class FCBlockJukebox extends BlockJukeBox
{
    public FCBlockJukebox( int iBlockID )
    {
    	super( iBlockID );

    	setHardness( 1.5F );
    	setResistance( 10F );
    	SetAxesEffectiveOn();
    	
    	SetBuoyant();
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.WOOD_BASED_BLOCK );    	
    	
    	setStepSound( soundStoneFootstep );
    	
    	setUnlocalizedName( "jukebox" );
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
