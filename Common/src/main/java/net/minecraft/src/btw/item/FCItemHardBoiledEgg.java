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


public class FCItemHardBoiledEgg extends ItemFood
{
	static private final int iHardBoiledEggHealthHealed = 3;
	static private final float iHardBoiledEggSaturationModifier = 0.25F;

    public FCItemHardBoiledEgg( int iItemID )
    {
        super( iItemID, iHardBoiledEggHealthHealed, iHardBoiledEggSaturationModifier, false );
        
        SetNeutralBuoyant();
		SetFilterableProperties( m_iFilterable_Small );
        
        setUnlocalizedName( "fcItemEggPoached" );    
    }    
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
