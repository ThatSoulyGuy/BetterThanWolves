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


public class FCMaterialSoulforgedSteel extends Material
{
    public FCMaterialSoulforgedSteel( MapColor mapColor )
    {
        super( mapColor );
        setImmovableMobility(); // same as obsidian
        setRequiresTool(); // can only be harvested if the tool in question is specifically coded for it
    }
}
