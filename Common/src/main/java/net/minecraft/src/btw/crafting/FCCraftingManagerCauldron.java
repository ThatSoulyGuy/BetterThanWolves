package net.minecraft.src.btw.crafting;

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


public class FCCraftingManagerCauldron  extends FCCraftingManagerBulk
{
    private static final FCCraftingManagerCauldron instance = new FCCraftingManagerCauldron();
    
    public static final FCCraftingManagerCauldron getInstance()
    {
        return instance;
    }

    private FCCraftingManagerCauldron()
    {
    	super();    	
    }
}

