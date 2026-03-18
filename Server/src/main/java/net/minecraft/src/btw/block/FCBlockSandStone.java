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


public class FCBlockSandStone extends BlockSandStone
{
    public FCBlockSandStone( int iBlockID )
    {
        super( iBlockID );
        
        SetPicksEffectiveOn();
        
        setHardness( 1.5F );
        
        setStepSound( soundStoneFootstep );
        
        setUnlocalizedName( "sandStone" );        
    }
    
    @Override
    public int GetHarvestToolLevel( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 3; // diamond or better
    }
    
	@Override
	public boolean DropComponentItemsOnBadBreak( World world, int i, int j, int k, int iMetadata, float fChanceOfDrop )
	{
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemPileSand.itemID, 16, 0, fChanceOfDrop );
		
		return true;
	}
	
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
