package net.minecraft.src.btw.util;

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


public class FCUtilsPrimitiveAABBWithBenefits extends AxisAlignedBB
{
	private int m_iAssemblyID = -1; // used to identify objects within a parent block model
	private boolean m_bForceRenderWithColorMultiplier = false;
	
    public FCUtilsPrimitiveAABBWithBenefits( double dXMin, double dYMin, double dZMin, 
    	double dXMax, double dYMax, double dZMax )
    {
    	super( dXMin, dYMin, dZMin, dXMax, dYMax, dZMax  );
    }
    
    public FCUtilsPrimitiveAABBWithBenefits( double dXMin, double dYMin, double dZMin, 
    	double dXMax, double dYMax, double dZMax, int iAssemblyID )
    {
    	super( dXMin, dYMin, dZMin, dXMax, dYMax, dZMax );
    	
    	m_iAssemblyID = iAssemblyID;
    }
    
    @Override
    public FCUtilsPrimitiveAABBWithBenefits MakeTemporaryCopy()
    {
    	FCUtilsPrimitiveAABBWithBenefits tempCopy = 
    		new FCUtilsPrimitiveAABBWithBenefits( minX, minY, minZ, maxX, maxY, maxZ, m_iAssemblyID );

    	tempCopy.m_bForceRenderWithColorMultiplier = m_bForceRenderWithColorMultiplier;
    	
    	return tempCopy; 
    }
    
    @Override
    public int GetAssemblyID()
    {
    	return m_iAssemblyID;
    }
    
	//------------- Class Specific Methods ------------//

    public void SetForceRenderWithColorMultiplier( boolean bForce )
    {
    	m_bForceRenderWithColorMultiplier = bForce;
    }
	
	//----------- Client Side Functionality -----------//

    @Override
	public boolean RenderAsBlock( RenderBlocks renderBlocks, Block block, int i, int j, int k )
	{	
    	if ( !m_bForceRenderWithColorMultiplier )
    	{
    		return super.RenderAsBlock( renderBlocks, block, i, j, k );
    	}
    	else
    	{
    		return RenderAsBlockWithColorMultiplier( renderBlocks, block, i, j, k );
    	}
    }    
}
