package net.minecraft.src.btw.model;

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


public class FCModelBlockBucketFull extends FCModelBlockBucket
{
	public static final double m_dContentsHeight = ( m_dHeight - m_dBaseHeight - ( 1.5D / 16D ) );
	public static final double m_dContentsVerticalOffset = ( m_dBaseHeight + ( 1D / 16D ) );
	public static final double m_dContentsWidth = m_dInteriorWidth;
	public static final double m_dContentsHalfWidth = ( m_dContentsWidth / 2D );
	
	public FCModelBlockBucketFull()
	{
		super();
	}
	
	@Override
    public void InitModel()
    {
		super.InitModel();
		
		FCUtilsPrimitiveAABBWithBenefits tempBox = new FCUtilsPrimitiveAABBWithBenefits( 
			0.5D - m_dContentsHalfWidth, m_dContentsVerticalOffset, 0.5D - m_dContentsHalfWidth, 
			0.5D + m_dContentsHalfWidth, m_dContentsVerticalOffset + m_dContentsHeight, 
			0.5D + m_dContentsHalfWidth, 
			m_iAssemblyIDContents );
		
		AddPrimitive( tempBox );
    }
}