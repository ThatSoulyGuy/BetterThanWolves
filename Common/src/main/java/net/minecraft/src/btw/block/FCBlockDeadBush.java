package net.minecraft.src.btw.block;

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


public class FCBlockDeadBush extends BlockDeadBush
{
    public static final double m_dWidth = 0.8D;
    public static final double m_dHalfWidth = ( m_dWidth / 2D );
    
    public FCBlockDeadBush( int iBlockID )
    {
    	super( iBlockID );
    	
    	setHardness( 0F );
    	
    	SetBuoyant();
    	
        InitBlockBounds( 
        	0.5D - m_dHalfWidth, 0D, 0.5D - m_dHalfWidth, 
        	0.5D + m_dHalfWidth, 0.8D, 0.5D + m_dHalfWidth);
        
    	setStepSound( soundGrassFootstep );
    	
    	setUnlocalizedName("deadbush");    	
    }
    
    @Override
    public boolean CanSpitWebReplaceBlock( World world, int i, int j, int k )
    {
    	return true;
    }
    
    @Override
    public boolean IsReplaceableVegetation( World world, int i, int j, int k )
    {
    	return true;
    }
	
    @Override
    public boolean CanBeGrazedOn( IBlockAccess blockAccess, int i, int j, int k, 
    	EntityAnimal animal )
    {
		return animal.CanGrazeOnRoughVegetation();
    }
    
    @Override
    public boolean CanGrowOnBlock( World world, int i, int j, int k )
    {
    	return world.getBlockId( i, j, k ) == Block.sand.blockID;
    }
    
    //------------- Class Specific Methods ------------//

	//----------- Client Side Functionality -----------//    
}
    
