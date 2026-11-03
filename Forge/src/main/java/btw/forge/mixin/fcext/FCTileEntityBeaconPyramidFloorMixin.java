package btw.forge.mixin.fcext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * FC's pyramid check bakes in 1.5.2's y floor (Common FCTileEntityBeacon.java:195-198, {@code if (iTempJ < 0) return
 * false}): a pyramid reaching below y=0 never powered. The floor becomes the level's build floor
 * (World.getFcMinBuildHeight: 0 in Modern-Common, the level's in WorldBridge and ClientFcWorld).
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; descriptor from
 * javap of Forge/build/classes/java/fc). {@code IsPyramidLevelValid(III)Z} has one zero comparison, {@code iload 4; ifge}
 * at offsets 8-10; Mixin 0.8.5 rewrites it into {@code iconst_0; <handler>; if_icmpge}. The {@code @Constant} names no
 * intValue: with intValue = 0 it would also match the method's two {@code iconst_0} returns (offsets 13 and 96).</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_beacon: a beacon pyramid may reach below y=0; FC's floor is the level's build floor (World.getFcMinBuildHeight)
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.tileentity.FCTileEntityBeacon", remap = false)
public abstract class FCTileEntityBeaconPyramidFloorMixin {
    @ModifyConstant(method = "IsPyramidLevelValid(III)Z",
            constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO),
            require = 1, allow = 1, remap = false)
    private int btw$pyramidFloor(int zero) {
        btw.modern.World w = ((btw.modern.TileEntity) (Object) this).worldObj;
        return w != null ? w.getFcMinBuildHeight() : zero;
    }
}
