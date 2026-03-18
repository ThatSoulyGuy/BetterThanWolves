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


import java.util.HashMap;
import java.util.Map;

public class FCCraftingManagerCampfire
{
	public static FCCraftingManagerCampfire instance = new FCCraftingManagerCampfire();
	
    private Map m_recipeMap = new HashMap();

    private FCCraftingManagerCampfire()
    {
    }

    public ItemStack GetRecipeResult( int iInputItemID )
    {
        return (ItemStack)m_recipeMap.get( iInputItemID );
    }

    public void AddRecipe( int iInputItemID, ItemStack outputStack )
    {
    	m_recipeMap.put( iInputItemID, outputStack );
    }
}
