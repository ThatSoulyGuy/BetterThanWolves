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


public class FCItemArmorChain extends FCItemArmorMod
{
	static final int m_iRenderIndex = 1;
	
    public FCItemArmorChain( int iItemID, int iArmorType, int iWeight )
    {
        super( iItemID, EnumArmorMaterial.CHAIN, m_iRenderIndex, iArmorType, iWeight );
        
        SetInfernalMaxEnchantmentCost( 30 );
        SetInfernalMaxNumEnchants( 2 );        
    }
    
    @Override
	public String GetWornTexturePrefix()
    {
    	return "chain";
    }    
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}