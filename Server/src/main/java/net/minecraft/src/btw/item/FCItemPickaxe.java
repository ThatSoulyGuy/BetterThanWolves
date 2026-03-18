package net.minecraft.src.btw.item;

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


public class FCItemPickaxe extends FCItemTool
{
    public FCItemPickaxe( int iItemID, EnumToolMaterial material )
    {
        super( iItemID, 2, material );
    }

    public FCItemPickaxe( int iItemID, EnumToolMaterial material, int iMaxUses )
    {
        super( iItemID, 2, material );
        
        setMaxDamage( iMaxUses );
    }

    @Override
    public boolean canHarvestBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	int iToolLevel = toolMaterial.getHarvestLevel();
    	int iBlockToolLevel = block.GetHarvestToolLevel( world, i, j, k ); 
    	
    	if ( iBlockToolLevel > iToolLevel )
    	{
        	return false;
    	}
    	
    	// FCTODO: Move the following to block classes like above
    	if ( block == Block.obsidian )
    	{
    		return toolMaterial.getHarvestLevel() >= 3;
    	}
    	else if ( block == Block.blockDiamond || block == Block.blockEmerald || block == Block.blockGold )
    	{
    		return toolMaterial.getHarvestLevel() >= 2;
    	}
    	else if ( block == Block.blockIron || block == Block.blockLapis )
    	{
    		return toolMaterial.getHarvestLevel() >= 1;
    	}
    	
    	return block.blockMaterial == Material.rock || 
    		block.blockMaterial == Material.iron || 
    		block.blockMaterial == Material.anvil ||
    		block.blockMaterial == FCBetterThanWolves.fcMaterialNetherRock;
    }

    @Override
    public boolean IsToolTypeEfficientVsBlockType( Block block )
    {
    	return block.ArePicksEffectiveOn();
    }
    
    @Override
    public float getStrVsBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	int iToolLevel = toolMaterial.getHarvestLevel();
    	int iBlockToolLevel = block.GetEfficientToolLevel( world, i, j, k ); 
    	
    	if ( iBlockToolLevel > iToolLevel )
    	{
        	return 1.0F;
    	}
    	
		Material material = block.blockMaterial;
		
		if ( material == Material.iron || material == Material.rock || 
			block.blockMaterial == Material.anvil ||
			material == FCBetterThanWolves.fcMaterialNetherRock )
		{
			return efficiencyOnProperMaterial;
		}    		
    	
    	return super.getStrVsBlock( stack, world, block, i, j, k );
    }
    
    @Override
    public boolean IsEfficientVsBlock( ItemStack stack, World world, Block block, int i, int j, int k )
    {
    	int iToolLevel = toolMaterial.getHarvestLevel();
    	int iBlockToolLevel = block.GetEfficientToolLevel( world, i, j, k ); 
    	
    	if ( iBlockToolLevel > iToolLevel )
    	{
        	return false;
    	}    	
    	
    	return super.IsEfficientVsBlock( stack, world, block, i, j, k );
    }
    
    @Override
    public float GetVisualVerticalOffsetAsBlock()
    {
    	return 0.72F;
    }

    @Override
    public float GetVisualHorizontalOffsetAsBlock()
    {
    	return 0.35F;
    }
    
    @Override
    public float GetVisualRollOffsetAsBlock()
    {
    	return 20F;
    }
    
    @Override
    public float GetBlockBoundingBoxHeight()
    {
    	return 0.65F;
    }
    
    @Override
    public float GetBlockBoundingBoxWidth()
    {
    	return 1F;
    }
    
    @Override
    public void PlayPlacementSound( ItemStack stack, Block blockStuckIn, World world, int i, int j, int k )
    {
    	world.playSoundEffect( (float)i + 0.5F, (float)j + 0.5F, (float)k + 0.5F,
    		"random.anvil_land", 0.5F, world.rand.nextFloat() * 0.25F + 1.75F );
    }    
    
    @Override
    public boolean CanToolStickInBlock( ItemStack stack, Block block, World world, int i, int j, int k )
    {
		if ( block.blockMaterial == Material.glass )
		{
			return false;
		}
    	
		return super.CanToolStickInBlock( stack, block, world, i, j, k );
    }
}
