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


public class FCItemBucketLava extends Item
{
	// deprecated item.  Only legacy support for any lingering items.
	
    public FCItemBucketLava( int iItemID )
    {
    	super( iItemID );
    	
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.LAVA_BUCKET );
    	
    	setUnlocalizedName( "bucketLava" );
	}
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
