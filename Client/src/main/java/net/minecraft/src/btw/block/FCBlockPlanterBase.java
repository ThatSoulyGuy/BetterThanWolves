package net.minecraft.src.btw.block;

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


public class FCBlockPlanterBase extends Block
{
	public static final double m_dPlanterWidth = ( 1D - ( 4D / 16D ) );
	public static final double m_dPlanterHalfWidth = ( m_dPlanterWidth / 2D );
	
	public static final double m_dPlanterBandHeight = ( 5D / 16D );
	public static final double m_dPlanterBandHalfHeight = ( m_dPlanterBandHeight / 2D );
	
    public FCBlockPlanterBase( int iBlockID )
    {
    	super( iBlockID, Material.glass );
    	
        setHardness( 0.6F );
        SetPicksEffectiveOn( true );
        
        setTickRandomly( true );        
        
        setStepSound( soundGlassFootstep );        
        
        setUnlocalizedName( "fcBlockPlanterSoil" );     
        
        setCreativeTab( CreativeTabs.tabDecorations );
    }
    
	@Override
    public boolean isOpaqueCube()
    {
        return false;
    }

	@Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }
    
	@Override
    public void onEntityCollidedWithBlock( World world, int i, int j, int k, Entity entity )
    {
		if ( !world.isRemote && entity.isEntityAlive() && entity instanceof EntityItem )
		{
			EntityItem entityItem = (EntityItem)entity;
			ItemStack stack = entityItem.getEntityItem();			
			
			if ( stack.getItem().itemID == Item.dyePowder.itemID && 
				stack.getItemDamage() == 15 ) // bone meal
			{
				if ( AttemptToApplyFertilizerTo( world, i, j, k ) )
				{
					stack.stackSize--;
					
					if ( stack.stackSize <= 0 )
					{
						entityItem.setDead();
					}

		            world.playSoundEffect( i + 0.5D, j + 0.5D, k + 0.5D, "random.pop", 0.25F, 
	            		( ( world.rand.nextFloat() - world.rand.nextFloat() ) * 
            			0.7F + 1F ) * 2F );
				}
			}
		}
    }
	
	@Override
	public float GetPlantGrowthOnMultiplier( World world, int i, int j, int k, Block plantBlock )
	{
		if ( GetIsFertilizedForPlantGrowth( world, i, j, k ) )
		{
			return 2F;
		}

		return 1F;
	}
	
    @Override
    public MovingObjectPosition collisionRayTrace( World world, int i, int j, int k, Vec3 startRay, Vec3 endRay )
    {
    	FCUtilsRayTraceVsComplexBlock rayTrace = new FCUtilsRayTraceVsComplexBlock( world, i, j, k, startRay, endRay );
    	
    	// bottom
    	
		rayTrace.AddBoxWithLocalCoordsToIntersectionList( 0.5F - m_dPlanterHalfWidth, 0F, 0.5F - m_dPlanterHalfWidth, 
    		0.5F + m_dPlanterHalfWidth, 1F - m_dPlanterBandHeight, 0.5F + m_dPlanterHalfWidth );
    
    	// top
    	
		rayTrace.AddBoxWithLocalCoordsToIntersectionList( 0F, 1F - m_dPlanterBandHeight, 0F, 1F, 1F, 1F );
        
        return rayTrace.GetFirstIntersection();    	
    }
    
    @Override
    public float GetMovementModifier( World world, int i, int j, int k )
    {
    	return 1F;
    }
    
    @Override
	public boolean HasCenterHardPointToFacing( IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency )
	{
    	return iFacing == 0 || super.HasCenterHardPointToFacing( blockAccess, i, j, k, iFacing, bIgnoreTransparency );
	}
    
    @Override
	public boolean HasLargeCenterHardPointToFacing( IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency )
	{
		return iFacing == 1;		
	}
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
    
    public Icon m_iconTopSoilWet;
    public Icon m_iconTopSoilWetFertilized;
    
	@Override
    public void registerIcons( IconRegister register )
    {
		blockIcon = register.registerIcon( "fcBlockPlanter" );
		
		m_iconTopSoilWet = register.registerIcon( "fcBlockPlanter_top_wet" );
		m_iconTopSoilWetFertilized = register.registerIcon( "fcBlockPlanter_top_wet_fertilized" );
    }
	
    @Override
    public boolean shouldSideBeRendered( IBlockAccess blockAccess, 
    	int iNeighborI, int iNeighborJ, int iNeighborK, int iSide )
    {
		return m_currentBlockRenderer.ShouldSideBeRenderedBasedOnCurrentBounds( 
			iNeighborI, iNeighborJ, iNeighborK, iSide );
    }
    
    public void RenderFilledPlanterInvBlock( RenderBlocks renderer, Block block, int iItemDamage )
	{
    	// render bottom
    	
        renderer.setRenderBounds( 0.5F - m_dPlanterHalfWidth, 0F, 0.5F - m_dPlanterHalfWidth, 
    		0.5F + m_dPlanterHalfWidth, 1F - m_dPlanterBandHeight, 0.5F + m_dPlanterHalfWidth );
    
        FCClientUtilsRender.RenderInvBlockWithMetadata( renderer, block, -0.5F, -0.5F, -0.5F, iItemDamage );    
        
    	// render top
    	
        renderer.setRenderBounds( 0F, 1F - m_dPlanterBandHeight, 0F, 
    		1F, 1F, 1F );    
    
        FCClientUtilsRender.RenderInvBlockWithMetadata( renderer, block, -0.5F, -0.5F, -0.5F, iItemDamage );
    }
    
    public boolean RenderFilledPlanterBlock( RenderBlocks renderer, int i, int j, int k )
    {
    	// render bottom
    	
        renderer.setRenderBounds( 0.5F - m_dPlanterHalfWidth, 0F, 0.5F - m_dPlanterHalfWidth, 
    		0.5F + m_dPlanterHalfWidth, 1F - m_dPlanterBandHeight, 0.5F + m_dPlanterHalfWidth );
    
        renderer.renderStandardBlock( this, i, j, k );
    
    	// render top
    	
        renderer.setRenderBounds( 0F, 1F - m_dPlanterBandHeight, 0F, 
    		1F, 1F, 1F );    
    
        renderer.renderStandardBlock( this, i, j, k );
    
    	return true;
    }
    
    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool( World world, int i, int j, int k )
    {
    	// only display box around top portion
    	
        return AxisAlignedBB.getAABBPool().getAABB( 0F, 1F - m_dPlanterBandHeight, 0F, 
    		1F, 1F, 1F ).offset( i, j, k );
    }
}
