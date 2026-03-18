package net.minecraft.src.btw.item;

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


public class FCItemBlockLeaves extends ItemBlock
{
    public FCItemBlockLeaves( int iItemID )
    {
        super( iItemID );
        
        setMaxDamage( 0 );
        setHasSubtypes( true );
    }

    @Override
    public int getMetadata( int iItemDamage )
    {
        return iItemDamage | 4;
    }

	//----------- Client Side Functionality -----------//

    @Override
    public Icon getIconFromDamage( int iItemDamage )
    {
        return FCBetterThanWolves.fcBlockBloodLeaves.getIcon( 0, iItemDamage );
    }

    @Override
    public int getColorFromItemStack( ItemStack itemStack, int iLayer )
    {
    	return 0xD81F1F;
    }
}
