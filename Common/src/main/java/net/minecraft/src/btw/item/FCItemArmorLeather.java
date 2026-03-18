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


public class FCItemArmorLeather extends FCItemArmor
{
	private static final int m_iRenderIndex = 0;
	private static final int m_iWornWeight = 0;
	
    public FCItemArmorLeather( int iItemID, int iArmorType )
    {
        super( iItemID, EnumArmorMaterial.CLOTH, m_iRenderIndex, iArmorType, m_iWornWeight );
     
		SetInfernalMaxEnchantmentCost( 10 );
		SetInfernalMaxNumEnchants( 2 );
        
        SetBuoyant();
        SetIncineratedInCrucible();
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
