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


public class FCItemBlockLegacySubstitution extends ItemBlock
{
	public int m_iSubstituteBlockID;
	
    public FCItemBlockLegacySubstitution( int iItemID, int iSubstituteBlockID )
    {
    	super( iItemID );
    	
    	m_iSubstituteBlockID = iSubstituteBlockID;
    }
    
    @Override
    public int GetBlockIDToPlace( int iItemDamage, int iFacing, float fClickX, float fClickY, float fClickZ )
    {
    	return m_iSubstituteBlockID;
    }
    
    @Override
    public String getItemDisplayName( ItemStack stack )
    {
        return "Old " + super.getItemDisplayName( stack );
    }    
}
