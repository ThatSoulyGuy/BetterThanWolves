package net.minecraft.src.btw.item;

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


public class FCItemBlockLegacyCorner extends ItemBlock
{
    public FCItemBlockLegacyCorner( int iItemID )
    {
        super( iItemID );
        
        setMaxDamage( 0 );
        setHasSubtypes(true);
        
        setUnlocalizedName( "fcCorner" );
    }

    @Override
    public int getMetadata( int iDamage )
    {
		return iDamage;
    }
    
    @Override    
    public float GetBuoyancy( int iItemDamage )
    {
    	if ( iItemDamage > 0 ) // stone corner
    	{
    		return -1.0F;
    	}
    	else
    	{
    		return super.GetBuoyancy( iItemDamage );
    	}
    }
    
	//----------- Client Side Functionality -----------//

    @Override
    public Icon getIconFromDamage( int iDamage ) 
    {
    	if ( iDamage > 0 )
    	{
    		return FCBetterThanWolves.fcBlockLegacySmoothstoneAndOakCorner.blockIcon;
    	}
    	else
    	{
    		return ((FCBlockLegacyCorner)FCBetterThanWolves.fcBlockLegacySmoothstoneAndOakCorner).m_IconWood;
    	}
    }
}