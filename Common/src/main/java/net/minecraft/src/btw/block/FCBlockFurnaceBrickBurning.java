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


public class FCBlockFurnaceBrickBurning extends FCBlockFurnaceBrick
{
    public FCBlockFurnaceBrickBurning( int iBlockID )
    {
        super( iBlockID, true );
        
    	setLightValue( 0.5F ); // 0.875 on standard furnace
    	
    	// this is necessary so changes to nearby lights will propagate past this one
    	
    	setLightOpacity( 8 ); 
    }
    
	@Override
    public boolean isOpaqueCube()
    {
    	// this is necessary so changes to nearby lights will propagate past this one
    	
        return false;
    }
}
