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


public class FCClosestEntitySelectionCriteria
{
	public static FCClosestEntitySelectionCriteria m_secondarySquidTarget = new FCClosestEntitySelectionCriteriaSquidSecondaryTarget();
	
    public void ProcessEntity( FCClosestEntityInfo closestEntityInfo, Entity entity ) 
    {
    	double dDeltaX = entity.posX - closestEntityInfo.m_dSourcePosX;
    	double dDeltaY = entity.posY - closestEntityInfo.m_dSourcePosY;
    	double dDeltaZ = entity.posZ - closestEntityInfo.m_dSourcePosZ;
    	
    	double dDistSq = ( dDeltaX * dDeltaX ) + ( dDeltaY * dDeltaY ) + ( dDeltaZ * dDeltaZ );
    	
    	if ( dDistSq < closestEntityInfo.m_dClosestDistanceSq )
    	{
    		closestEntityInfo.m_closestEntity = entity;
    		closestEntityInfo.m_dClosestDistanceSq = dDistSq;
    	}
	}
}
