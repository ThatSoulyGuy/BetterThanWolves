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


public class FCItemArrowBroadhead extends FCItemArrow
{
    public FCItemArrowBroadhead( int iItemID )
    {
    	super( iItemID );
		
    	SetNeutralBuoyant();
    	
    	// neutralize parent properties
		SetBellowsBlowDistance( 0 );		
    	SetNotIncineratedInCrucible();
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.NONE );
    	
    	setUnlocalizedName( "fcItemArrowBroadhead" );
    }

    @Override
    EntityArrow GetFiredArrowEntity( World world, double dXPos, double dYPos, double dZPos )
    {
        EntityArrow entity = new FCEntityBroadheadArrow( world, dXPos, dYPos, dZPos );
        
        entity.canBePickedUp = 1;
        
        return entity;
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
