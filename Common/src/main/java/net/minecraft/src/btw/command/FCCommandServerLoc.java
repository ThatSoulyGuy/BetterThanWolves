package net.minecraft.src.btw.command;

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


public class FCCommandServerLoc extends CommandBase
{
    public FCCommandServerLoc()
    {
    }

    @Override
    public String getCommandName()
    {
        return "loc";
    }

    @Override
    public String getCommandUsage(ICommandSender par1ICommandSender)
    {
        return "/loc";
    }

    @Override
    public void processCommand(ICommandSender par1ICommandSender, String par2ArrayOfStr[])    
    {   
    	if ( par1ICommandSender instanceof EntityPlayer )
    	{
    		EntityPlayer player = (EntityPlayer)par1ICommandSender;
    		
	    	par1ICommandSender.sendChatToPlayer( ( new StringBuilder()).
	    		append( "\247e"). // yellow text
	    		append( "Current Location: " ).
				append( MathHelper.floor_double( player.posX ) ).append( ", " ).
				append( MathHelper.floor_double( player.posY ) ).append( ", " ).
				append( MathHelper.floor_double( player.posZ ) ).
				toString() );
    	}
    }
}
