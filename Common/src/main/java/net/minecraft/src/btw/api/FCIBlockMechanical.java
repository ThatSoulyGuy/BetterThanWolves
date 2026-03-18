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


public interface FCIBlockMechanical
{
	public boolean CanOutputMechanicalPower();
	
	public boolean CanInputMechanicalPower();
	
	public boolean IsInputtingMechanicalPower( World world, int i, int j, int k );
	
	public boolean IsOutputtingMechanicalPower( World world, int i, int j, int k );
	
	public boolean CanInputAxlePowerToFacing( World world, int i, int j, int k, int iFacing );
	
	public void Overpower( World world, int i, int j, int k );
}