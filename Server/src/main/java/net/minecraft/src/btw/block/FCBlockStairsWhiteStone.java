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


public class FCBlockStairsWhiteStone extends FCBlockStairs
{	
	public FCBlockStairsWhiteStone( int iBlockID )
    {
    	super( iBlockID, Block.stone, 0 );
    	
        setHardness( 1.5F );
        setResistance( 10F );
        SetPicksEffectiveOn();        
        
        setUnlocalizedName( "fcBlockWhiteStoneStairs" );        
	}

	@Override
    public int damageDropped( int iMetadata )
    {
		return iMetadata & 8;
    }
	
	//------------- Class Specific Methods ------------//
	
	public boolean GetIsCobbleFromMetadata( int iMetadata )
	{
		return ( iMetadata & 8 ) > 0;
	}	
}
