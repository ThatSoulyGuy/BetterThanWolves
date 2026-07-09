package btw.forge.mixin;

import btw.forge.ProxyBlockEntity;
import btw.forge.ProxyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.BonusChestFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FC replaces the vanilla spawn bonus chest with the "bonus basket" gag: instead of free
 * starter tools/food, a single Wicker Basket (fcBlockBasketWicker, legacy id 1031) holding
 * exactly one Golden Dung (fcItemGoldenDung, legacy id 22290). Reproduces FC's
 * WorldServer.createBonusChest -> FCWorldGeneratorBonusBasket.
 *
 * <p>Placed near world spawn like the vanilla feature. The FC basket's tile entity is created
 * eagerly by ProxyBlock.newBlockEntity, so its single storage slot is set immediately via
 * FCTileEntityBasketWicker.SetStorageStack (reflective — the frozen FC class isn't on the
 * bridge's compile classpath). If no valid spot is found, vanilla runs (a normal bonus chest)
 * as a fallback so the player is never left with nothing.</p>
 */
@Mixin(BonusChestFeature.class)
public abstract class BonusChestFeatureMixin {

    private static final int BASKET_ID = 1031;

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void btw$bonusBasket(FeaturePlaceContext<NoneFeatureConfiguration> context,
                                 CallbackInfoReturnable<Boolean> cir) {
        Block basket = ProxyRegistry.getModernBlock(BASKET_ID);
        if (basket == null) {
            return; // basket unavailable → let vanilla place a normal chest
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                BlockPos pos = new BlockPos(x, y, z);
                if (level.isEmptyBlock(pos)
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                    level.setBlock(pos, basket.defaultBlockState(), 2);
                    try {
                        BlockEntity be = level.getBlockEntity(pos);
                        // Resolve golden dung by its FC field, not itemsList[22290]: the FC Item
                        // ctor offsets itemID by 256, so the raw id is the wrong slot.
                        btw.modern.Item dungItem = (btw.modern.Item) Class.forName("btw.modern.FCBetterThanWolves")
                                .getField("fcItemGoldenDung").get(null);
                        if (be instanceof ProxyBlockEntity pbe && pbe.getFcTileEntity() != null
                                && dungItem != null) {
                            btw.modern.TileEntity fcTe = pbe.getFcTileEntity();
                            btw.modern.ItemStack dung = new btw.modern.ItemStack(dungItem, 1);
                            fcTe.getClass().getMethod("SetStorageStack", btw.modern.ItemStack.class)
                                    .invoke(fcTe, dung);
                            pbe.setChanged();
                        }
                    } catch (Throwable ignored) {
                    }
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        // No valid spot found — fall through to the vanilla bonus chest.
    }
}
