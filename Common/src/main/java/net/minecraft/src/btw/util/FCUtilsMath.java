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


/**
 * Similar to MatherHelper, fills in a few additional functions
 */
public class FCUtilsMath
{
    public static double ClampDouble( double dValue, double dBottom, double dTop )
    {
    	if ( dValue < dBottom )
    	{
    		return dBottom;
    	}
    	else if ( dValue > dTop )
    	{
    		return dTop;
    	}
    	
    	return dValue;
    }
    
    public static double ClampDoubleTop( double dValue, double dTop )
    {
    	if ( dValue > dTop )
    	{
    		return dTop;
    	}
    	
    	return dValue;
    }
    
    public static double ClampDoubleBottom( double dValue, double dBottom )
    {
    	if ( dValue < dBottom )
    	{
    		return dBottom;
    	}
    	
    	return dValue;
    }
    
    public static double AbsDouble( double dValue )
    {
    	if ( dValue >= 0D )
    	{
    		return dValue;
    	}
    	
    	return -dValue;
    }
}
