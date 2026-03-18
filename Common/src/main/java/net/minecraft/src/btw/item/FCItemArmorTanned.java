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


public class FCItemArmorTanned extends FCItemArmorMod
{
	static final int m_iRenderIndex = 1;
	static final int m_iWornWeight = 0;
	
    public FCItemArmorTanned( int iItemID, int iArmorType )
    {
        super( iItemID, EnumArmorMaterial.CLOTH, m_iRenderIndex, iArmorType, m_iWornWeight ); // we're overriding the material
     
        setMaxDamage( getMaxDamage() << 1 ); // 2X durability as normal leather
        
		SetInfernalMaxEnchantmentCost( 10 );
		SetInfernalMaxNumEnchants( 2 );
        
        SetBuoyant();
        SetIncineratedInCrucible();
    }
    
    @Override
	public String GetWornTexturePrefix()
    {
    	return "fcTanned";
    }    
}
