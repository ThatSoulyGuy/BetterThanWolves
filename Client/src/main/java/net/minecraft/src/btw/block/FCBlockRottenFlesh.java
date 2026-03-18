package net.minecraft.src.btw.block;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.client.*;
import net.minecraft.src.btw.crafting.*;
import net.minecraft.src.btw.api.*;
import net.minecraft.src.btw.util.*;
import net.minecraft.src.btw.world.*;
import net.minecraft.src.btw.behavior.*;
import net.minecraft.src.btw.properties.*;
import net.minecraft.src.btw.model.*;
import net.minecraft.src.btw.command.*;

// FCMOD


public class FCBlockRottenFlesh extends Block
{
    public final static float m_fHardness = 0.6F;
    
    public FCBlockRottenFlesh( int iBlockID )
    {
        super( iBlockID, Material.ground );
        
        setHardness( m_fHardness );
        SetBuoyancy( 1F );
        
        SetShovelsEffectiveOn( true );
        
        setStepSound( FCBetterThanWolves.fcStepSoundSquish );
        
        setCreativeTab( CreativeTabs.tabBlock );
        
        setUnlocalizedName( "fcBlockRottenFlesh" );
    }
    
	@Override
    public boolean DoesBlockBreakSaw( World world, int i, int j, int k )
    {
		return false;
    }
    
	@Override
    public int GetItemIDDroppedOnSaw( World world, int i, int j, int k )
    {
		return Item.rottenFlesh.itemID;
    }
	
	@Override
    public int GetItemCountDroppedOnSaw( World world, int i, int j, int k )
    {
		return 5; // 9 in full block
    }
	
	@Override
    public boolean CanBePistonShoveled( World world, int i, int j, int k )
    {
    	return true;
    }
	
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
    
	@Override
    public void registerIcons( IconRegister register )
    {
        blockIcon = register.registerIcon( "fcBlockRottenFlesh" );
    }	
}