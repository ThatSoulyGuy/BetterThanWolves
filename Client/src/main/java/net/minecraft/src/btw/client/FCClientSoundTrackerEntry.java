package net.minecraft.src.btw.client;

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


public class FCClientSoundTrackerEntry
{
	public String m_sName;
	public float m_fXPos;
	public float m_fYPos;
	public float m_fZPos;
	public float m_fMaxRangeSq;
	
	public FCClientSoundTrackerEntry( String sName, float fXPos, float fYPos, float fZPos, float fMaxRange )
	{
		m_sName = sName;
		m_fXPos = fXPos;
		m_fYPos = fYPos;
		m_fZPos = fZPos;
		m_fMaxRangeSq = fMaxRange * fMaxRange;
	}
}