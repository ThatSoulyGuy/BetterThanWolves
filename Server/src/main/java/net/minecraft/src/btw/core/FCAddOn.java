package net.minecraft.src.btw.core;

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

//import net.minecraft.client.Minecraft;

public abstract class FCAddOn
{
	public FCAddOn()
	{
		FCAddOnHandler.AddMod( this );
	}
	
	public void PreInitialize()
	{
	}
	
	abstract public void Initialize();
	
	public void PostInitialize()
	{
	}
	
	public void OnLanguageLoaded( StringTranslate translator )
	{
	}	
	
	/**
	 * Prefix for custom addon-specific .lang files
	 * Returns null if addon doesn't support such files
	 */
	public String GetLanguageFilePrefix()
	{
		return null;
	}
	
    /*
     * Returns true if the packet has been processed, false otherwise
     */    
    public boolean ServerCustomPacketReceived( NetServerHandler handler, Packet250CustomPayload packet )
    {
    	return false;
    }
    
	//----------- Client Side Functionality -----------//
}
