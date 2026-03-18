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


public class FCModelBlockFurnaceBrick extends FCModelBlock
{
	public static final float m_fBaseHeight = ( 6F / 16F );
	
	public static final float m_fSideWidth = ( 4F / 16F );
	public static final float m_fHalfSideWidth = ( m_fSideWidth / 2F );
	
	public static final float m_fTopThickness = ( 4F / 16F );
	public static final float m_fHalfTopThickness = ( m_fTopThickness / 2F );
	
	public static final float m_fMindTheGap = 0.001F;
	
	@Override
    public void InitModel()
    {
    	// interior
    	
		/*
    	AddBox( m_fSideWidth - m_fMindTheGap, m_fBaseHeight - m_fMindTheGap, 0F,
    		1F - ( m_fSideWidth - m_fMindTheGap ), 1F - ( m_fTopThickness - m_fMindTheGap), 1F - m_fSideWidth );
		*/
		
		// inverted interior
		
    	AddBox( 1F - ( m_fSideWidth - m_fMindTheGap ), 1F - ( m_fTopThickness - m_fMindTheGap), 1F - m_fSideWidth, 
    		m_fSideWidth - m_fMindTheGap, m_fBaseHeight - m_fMindTheGap, 0F );
    }    
}
