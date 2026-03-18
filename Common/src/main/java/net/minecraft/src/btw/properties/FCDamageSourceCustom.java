package net.minecraft.src.btw.properties;

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


public class FCDamageSourceCustom extends DamageSource
{
    public static DamageSource m_DamageSourceSaw = new FCDamageSourceCustom( "fcSaw", "rests in pieces" );	
    public static DamageSource m_DamageSourceChoppingBlock = new FCDamageSourceCustom( "fcChoppingBlock", "was put on the chopping block" );	
    public static DamageSource m_DamageSourceGroth = new FCDamageSourceCustom( "fcGroth", "became one with the Groth" ).setDamageBypassesArmor();	
    public static DamageSource m_DamageSourceGrothSpores = new FCDamageSourceCustom( "fcGrothSpores", "succumbed to the Groth menace" ).setDamageBypassesArmor();	
    public static DamageSource m_DamageSourceDeadWeight = new FCDamageSourceCustom( "fcDeadWeight", "had their hopes crushed by poor design" );	
    public static DamageSource m_DamageSourceMelon = new FCDamageSourceCustom( "fcMelon", "was smothered in melons" );	
    public static DamageSource m_DamageSourcePumpkin = new FCDamageSourceCustom( "fcPumpkin", "took wearing a pumpkin a bit too far" );	
    public static DamageSource m_DamageSourceGloom = new FCDamageSourceCustom( "fcGloom", "was consumed by the darkness" ).setDamageBypassesArmor();	
	
	String m_sDeathMessage;
	
    public FCDamageSourceCustom( String sName, String sDeathMessage )
    {
    	super( sName );
    	
    	m_sDeathMessage = sDeathMessage;
    }
    
    @Override
    public String getDeathMessage( EntityLiving entity )
    {
        return (new StringBuilder( entity.getTranslatedEntityName() ) ).append( " " ).append( m_sDeathMessage ).toString();
    }
}
