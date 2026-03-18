package net.minecraft.src.btw.model;

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


public class FCModelBlockFence extends FCModelBlock
{
    public static final double m_dPostWidth = ( 4D / 16D );
    public static final double m_dPostHalfWidth = ( m_dPostWidth / 2D );
    
    public static final double m_dItemPostsWidth = ( 4D / 16D );
    public static final double m_dItemPostsHalfWidth = ( m_dItemPostsWidth / 2D );
    public static final double m_dItemPostsBorderGap = ( 2D / 16D );
    
    public static final double m_dStrutWidth = ( 2D / 16D );
    public static final double m_dStrutHalfWidth = ( m_dStrutWidth / 2D );
    public static final double m_dStrutHeight = ( 3D / 16D );
    public static final double m_dStrutBottomVerticalOffset = ( 6D / 16D );
    public static final double m_dStrutTopVerticalOffset = ( 12D / 16D );
    
	public FCModelBlock m_modelStruts;
	public FCModelBlock m_modelItem;
    
	public AxisAlignedBB m_boxCollisionCenter;
	public AxisAlignedBB m_boxCollisionStruts;	
	
	public AxisAlignedBB m_boxBoundsCenter;
	
	@Override
    public void InitModel()
    {
		// center
		
		AddBox( 0.5D - m_dPostHalfWidth, 0D, 0.5D - m_dPostHalfWidth,
			0.5D + m_dPostHalfWidth, 1D, 0.5D + m_dPostHalfWidth );
		
		// support (-k aligned)
		
		m_modelStruts = new FCModelBlock();
		
		m_modelStruts.AddBox( 0.5D - m_dStrutHalfWidth, m_dStrutBottomVerticalOffset, 0D,
			0.5D + m_dStrutHalfWidth, m_dStrutBottomVerticalOffset + m_dStrutHeight, 0.5D );
		
		m_modelStruts.AddBox( 0.5D - m_dStrutHalfWidth, m_dStrutTopVerticalOffset, 0D,
			0.5D + m_dStrutHalfWidth, m_dStrutTopVerticalOffset + m_dStrutHeight, 0.5D );
		
		// item posts
		
		m_modelItem = new FCModelBlock();
		
		m_modelItem.AddBox( 0.5D - m_dItemPostsHalfWidth, 0D, 0D,
			0.5D + m_dItemPostsHalfWidth, 1D, m_dItemPostsWidth );
		
		m_modelItem.AddBox( 0.5D - m_dItemPostsHalfWidth, 0D, 1D - m_dItemPostsWidth,
			0.5D + m_dItemPostsHalfWidth, 1D, 1D );
		
		// item struts
		// these are a bit weird in that they extend beyond the block boundaries, 
		// which is copying how the vanilla renderer does it
		
		m_modelItem.AddBox( 0.5D - m_dStrutHalfWidth, m_dStrutBottomVerticalOffset, -m_dItemPostsBorderGap,
			0.5D + m_dStrutHalfWidth, m_dStrutBottomVerticalOffset + m_dStrutHeight, 1D + m_dItemPostsBorderGap );
		
		m_modelItem.AddBox( 0.5D - m_dStrutHalfWidth, m_dStrutTopVerticalOffset, -m_dItemPostsBorderGap,
			0.5D + m_dStrutHalfWidth, m_dStrutTopVerticalOffset + m_dStrutHeight, 1D + m_dItemPostsBorderGap );
		
		// collision volumes 
		
		m_boxCollisionCenter = new AxisAlignedBB( 
			0.5D - m_dPostHalfWidth, 0D, 0.5D - m_dPostHalfWidth,
			0.5D + m_dPostHalfWidth, 1.5D, 0.5D + m_dPostHalfWidth );
		
		m_boxCollisionStruts = new AxisAlignedBB( 
			0.5D - m_dPostHalfWidth, 0D, 0D,
			0.5D + m_dPostHalfWidth, 1.5D, 0.5D );
		
		// selection volume
		
		m_boxBoundsCenter = new AxisAlignedBB( 
			0.5D - m_dPostHalfWidth, 0D, 0.5D - m_dPostHalfWidth,
			0.5D + m_dPostHalfWidth, 1D, 0.5D + m_dPostHalfWidth );
    }
	
	//----------- Client Side Functionality -----------//

    @Override
    public void RenderAsItemBlock( RenderBlocks renderBlocks, Block block, int iItemDamage )
    {
    	m_modelItem.RenderAsItemBlock( renderBlocks, block, iItemDamage );
    }
}
