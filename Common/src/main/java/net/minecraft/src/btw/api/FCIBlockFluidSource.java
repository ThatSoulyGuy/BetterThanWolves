package net.minecraft.src.btw.api;

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


public interface FCIBlockFluidSource
{
	/*
	 * Returns the height level of the source (0 to 8) if a valid source for the fluid block, -1 otherwise
	 */
	public int IsSourceToFluidBlockAtFacing( World world, int i, int j, int k, int iFacing  );
}