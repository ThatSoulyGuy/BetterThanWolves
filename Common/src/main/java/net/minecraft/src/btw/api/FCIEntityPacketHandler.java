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


public interface FCIEntityPacketHandler
{
	/*
	 *  NOTE: Don't forget to add the entity into NetClientHandler!!! 
	 */
	
	/* 
	 * hook into EntityTrackerEntry getPacketForThisEntity() to get the packet sent from the server when this entity spawns
	 */ 
    public Packet GetSpawnPacketForThisEntity();
    
    public int GetTrackerViewDistance();
    
    public int GetTrackerUpdateFrequency();

    public boolean GetTrackMotion();

    /*
     * Partially disables server-side visibility tests for interacting with an entity
     */
    public boolean ShouldServerTreatAsOversized();
}