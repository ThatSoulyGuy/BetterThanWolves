package net.minecraft.src.btw.item;

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


public class FCItemBlockDamageToMetadata extends ItemBlock
{
	public FCItemBlockDamageToMetadata(  int iItemID  )
	{
        super( iItemID );
        
        setMaxDamage( 0 );
        setHasSubtypes(true);
	}
	
    @Override
    public int getMetadata( int iItemDamage )
    {
		return iItemDamage;    	
    }
}
