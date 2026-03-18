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


public class FCBlockDetectorLogicGlowing extends FCBlockDetectorLogic
{
    private final static float m_fLitFaceThickness = 0.01F;
	
    public FCBlockDetectorLogicGlowing( int iBlockID )
    {
        super( iBlockID );  
        
        setLightValue( 1F );     
        
        setUnlocalizedName( "fcBlockDetectorLogicGlowing" );        
        
        setCreativeTab( CreativeTabs.tabRedstone );
    }
    
    //------------- FCBlockDetectorLogic ------------//

	@Override
	public void RemoveSelf( World world, int i, int j, int k )
	{
		// this function exists as the regular block shouldn't notify the client when it is removed, but the glowing variety should 
		
    	world.setBlock( i, j, k, 0, 0, 2 );        	
	}
    
    //------------- Class Specific Methods ------------//

	//----------- Client Side Functionality -----------//
}