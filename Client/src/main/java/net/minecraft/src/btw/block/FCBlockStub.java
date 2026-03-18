package net.minecraft.src.btw.block;

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


public class FCBlockStub extends Block
{
    private final IBehaviorDispenseItem dropperDefaultBehaviour = new BehaviorDefaultDispenseItem();

    public FCBlockStub(int par1)
    {
        super( par1, Material.rock );
    }
    
	//----------- Client Side Functionality -----------//
    
    public void registerIcons( IconRegister register )
    {
        blockIcon = register.registerIcon("fcBlockStub");
    }
}
