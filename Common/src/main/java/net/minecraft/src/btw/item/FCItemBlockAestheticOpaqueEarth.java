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


public class FCItemBlockAestheticOpaqueEarth extends ItemBlock
{
    public FCItemBlockAestheticOpaqueEarth( int iItemID )
    {
        super( iItemID );
        
        setMaxDamage( 0 );
        setHasSubtypes(true);
        
        setUnlocalizedName( "fcAestheticOpaqueEarth" );
    }

    @Override
    public int getMetadata( int iItemDamage )
    {
		return iItemDamage;    	
    }
    
    @Override
    public String getUnlocalizedName( ItemStack itemstack )
    {
    	switch( itemstack.getItemDamage() )
    	{
    		case FCBlockAestheticOpaqueEarth.m_iSubtypeBlightLevel0:
    		case FCBlockAestheticOpaqueEarth.m_iSubtypeBlightLevel1:
    		case FCBlockAestheticOpaqueEarth.m_iSubtypeBlightLevel2:
    			
                return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("blight").toString();
    			
    		case FCBlockAestheticOpaqueEarth.m_iSubtypeBlightLevel3:
    			
                return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("blight3").toString();
    			
    		case FCBlockAestheticOpaqueEarth.m_iSubtypePackedEarth:
    			
                return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("packed").toString();
    			
    		case FCBlockAestheticOpaqueEarth.m_iSubtypeDung:
    			
                return (new StringBuilder()).append(super.getUnlocalizedName()).append(".").append("dung").toString();
    			
			default:
				
				return super.getUnlocalizedName();
    	}
    }
}