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


import java.util.List;

public class FCItemMap extends ItemMap
{
    public FCItemMap( int iItemID )
    {
    	super( iItemID );
    	
    	SetBuoyant();
		SetBellowsBlowDistance( 3 );
		SetFilterableProperties( m_iFilterable_Thin );
		
		setUnlocalizedName( "map" );
    }
    
    @Override
    public void updateMapData( World world, Entity entity, MapData mapData )
    {
        if ( world.provider.dimensionId == mapData.dimension && entity instanceof EntityPlayer )
        {
        	// Note: This function only controls adding new data to the map.  It has nothing to do with existing data, and thus is not called if
        	// the map is on a frame.
        	
            if ( mapData.IsEntityLocationVisibleOnMap( entity ) )
            {
            	super.updateMapData( world, entity, mapData );
            }
        }
    }

	//----------- Client Side Functionality -----------//

    @Override
    public void addInformation( ItemStack stack, EntityPlayer player, List infoList, boolean bAdvancedTips )
    {
        MapData var5 = this.getMapData(stack, player.worldObj);

        if (var5 != null)
        {
            infoList.add( "Scale: x" + ( 1 << var5.scale ) );
        }
    }
}