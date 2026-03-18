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


public class FCBlockLegacyLadder extends FCBlockLadderBase
{
    public FCBlockLegacyLadder( int iBlockID )
    {
        super( iBlockID );
        
        setUnlocalizedName( "ladder" );
        
        setCreativeTab( null );
    }
    
    @Override
	public boolean GetPreventsFluidFlow( World world, int i, int j, int k, Block fluidBlock )
	{
    	return true;
	}
    
    @Override
    public int GetFacing( int iMetadata )
    {
    	return iMetadata;
    }
    
    @Override
    public int SetFacing( int iMetadata, int iFacing )
    {
    	return MathHelper.clamp_int( iFacing, 2, 5 );
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
