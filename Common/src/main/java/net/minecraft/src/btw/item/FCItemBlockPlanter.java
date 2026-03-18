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

//FCMOD 


public class FCItemBlockPlanter extends ItemBlock
{
    public FCItemBlockPlanter( int i )
    {
        super( i );
        
        setMaxDamage( 0 );
        setHasSubtypes( true );
        setUnlocalizedName( "fcBlockPlanter" );
    }

    @Override
    public int getMetadata( int i )
    {
    	return i;
    }
    
    @Override
    public String getItemDisplayName( ItemStack stack )
    {
    	String name = super.getItemDisplayName( stack );
    	
    	int iDamage = stack.getItemDamage();
    	
    	if ( iDamage == FCBlockPlanter.m_iTypeSoil || 
    		iDamage == FCBlockPlanter.m_iTypeSoilFertilized )
    	{
    		// deprecated subtypes
    		
    		return "Old " + name;
    	}
    	
    	return name;
    }    
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
