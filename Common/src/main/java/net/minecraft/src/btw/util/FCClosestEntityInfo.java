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


public class FCClosestEntityInfo
{
	public double m_dSourcePosX;
	public double m_dSourcePosY;
	public double m_dSourcePosZ; 
	
	public double m_dClosestDistanceSq;
	public Entity m_closestEntity;
	
	public FCClosestEntitySelectionCriteria m_criteria;
	
	public int m_iChunkEntityListMinVerticalIndex; 
	public int m_iChunkEntityListMaxVerticalIndex;
	
	public FCClosestEntityInfo	
	( 
		double dSourcePosX, 
		double dSourcePosY, 
		double dSourcePosZ, 
		double dClosestDistanceSq, 
		Entity closestEntity, 
		FCClosestEntitySelectionCriteria criteria, 
		int iChunkEntityListMinVerticalIndex, 
		int iChunkEntityListMaxVerticalIndex
	)
	{
		m_dSourcePosX = dSourcePosX;
		m_dSourcePosY = dSourcePosY;
		m_dSourcePosZ = dSourcePosZ; 
		m_dClosestDistanceSq = dClosestDistanceSq;
		m_closestEntity = closestEntity; 
		m_criteria = criteria;
		m_iChunkEntityListMinVerticalIndex = iChunkEntityListMinVerticalIndex; 
		m_iChunkEntityListMaxVerticalIndex = iChunkEntityListMaxVerticalIndex;
	}
}
