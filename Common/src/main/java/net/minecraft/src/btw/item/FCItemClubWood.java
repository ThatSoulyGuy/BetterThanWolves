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


public class FCItemClubWood extends FCItemClub
{
    public static final int m_iWeaponDamageWood = 2;    
    public static final int m_iDurabilityWood = 10;
    
    public FCItemClubWood( int iItemID )
    {
        super( iItemID, m_iDurabilityWood, m_iWeaponDamageWood, "fcItemClub" );
        
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.SHAFT );   	
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
