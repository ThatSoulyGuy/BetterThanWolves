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


public class FCItemBlockMouldingAndDecorative extends FCItemBlockMoulding
{	
    public FCItemBlockMouldingAndDecorative( int iItemID )
    {
        super( iItemID );        
    }
    
    @Override
    public int getMetadata( int iItemDamage )
    {
		return iItemDamage;    	
    }
    
    @Override
    public String getUnlocalizedName( ItemStack itemStack )
    {
    	switch( itemStack.getItemDamage() )
    	{
    		case FCBlockMouldingAndDecorative.m_iSubtypeColumn:
    			
                return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("column").toString();
    			
    		case FCBlockMouldingAndDecorative.m_iSubtypePedestalUp:
    		case FCBlockMouldingAndDecorative.m_iSubtypePedestalDown:
    			
                return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("pedestal").toString();
    			
    		case FCBlockMouldingAndDecorative.m_iSubtypeTable:
    			
                return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("table").toString();
    			
			default:
				
				return super.getUnlocalizedName( itemStack );
    	}
    }    
}
