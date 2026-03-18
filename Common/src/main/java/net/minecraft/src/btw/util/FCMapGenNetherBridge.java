package net.minecraft.src.btw.util;

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


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class FCMapGenNetherBridge extends MapGenNetherBridge
{
    public List m_mobSpawnList = new ArrayList();

    public FCMapGenNetherBridge()
    {
    	super();
    	
    	m_mobSpawnList.add( new SpawnListEntry( FCEntityBlaze.class, 10, 2, 3 ) );
    	m_mobSpawnList.add( new SpawnListEntry( FCEntityPigZombie.class, 5, 4, 4 ) );
    	m_mobSpawnList.add( new SpawnListEntry( FCEntitySkeleton.class, 10, 4, 4 ) );
    	m_mobSpawnList.add( new SpawnListEntry( FCEntityMagmaCube.class, 3, 4, 4 ) );
    }
    
    @Override
    public List getSpawnList()
    {
        return m_mobSpawnList;
    }
    
    //------------- Class Specific Methods ------------//
    
    public boolean HasStructureAtLoose( int i, int j, int k )
    {
        Iterator structureIterator = structureMap.values().iterator();

        while ( structureIterator.hasNext() )
        {
            StructureStart tempStructure = (StructureStart)structureIterator.next();
            
            StructureBoundingBox box = tempStructure.getBoundingBox();
            
            if ( tempStructure.isSizeableStructure() && box.intersectsWith( i, k, i, k ) )
            {
            	// test j value as intersection test does not
            	
            	if ( j >= box.minY && j <= box.maxY )
            	{            		
            		return true;
            	}
            }
        }

        return false;
    }
    
    public StructureStart GetClosestStructureWithinRangeSq( double xPos, double zPos, double dRangeSq )
    {
    	StructureStart closestStructure = null;
    	double dClosestDistSq = dRangeSq;
    	
        Iterator structureIterator = structureMap.values().iterator();

        while ( structureIterator.hasNext() )
        {
            StructureStart tempStructure = (StructureStart)structureIterator.next();
            
            StructureBoundingBox box = tempStructure.getBoundingBox();

            double dStructXPos = box.getCenterX();
            double dStructZPos = box.getCenterZ();
            
            double dDeltaX = xPos - dStructXPos;
            double dDeltaZ = zPos - dStructZPos;
            
            double dTempDistSq = ( dDeltaX * dDeltaX ) + ( dDeltaZ * dDeltaZ );
            
            if ( dTempDistSq < dClosestDistSq )
            {
            	closestStructure = tempStructure;
            	dClosestDistSq = dTempDistSq;
            }
        }
        
        return closestStructure;
    }
    
	//----------- Client Side Functionality -----------//
}
