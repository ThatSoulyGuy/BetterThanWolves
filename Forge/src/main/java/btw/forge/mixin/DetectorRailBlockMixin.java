package btw.forge.mixin;

import btw.forge.FcDetectorRailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DetectorRailBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_redstone (INTEGRATIONS #056)
 *
 * <p><b>Feature:</b> 1.20.1's detector rail presses for every minecart over it: DetectorRailBlock.checkPressed
 * (DetectorRailBlock.java:77-109) asks getInteractingMinecartOfType for any AbstractMinecart in the search box, from
 * entityInside, its scheduled re-check (tick) and onPlace.</p>
 *
 * <p><b>Conflict with FC:</b> 4.B's detector rails press by BTW's rule (Client FCBlockDetectorRail.java:138-174): the
 * vanilla detector rail 28 for a cart that is not empty (a storage or furnace cart, or a ridden one), BTW's wooden rail
 * 235 for every cart, the soulforged steel rail 236 for a cart carrying a player. FC's own contact body, which asks that
 * rule, never runs for a 1.20.1 detector rail (DetectorRailBlock.entityInside does not call the base body).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend). 1.20.1's check stays, with FC's rule as its cart filter
 * ({@link FcDetectorRailBlock#cartTriggers}: FCBlockDetectorRail.ShouldPlateActivateBasedOnMinecart with the cart's
 * 1.5.2 type and rider from its FC view). Rulings Q-RS1 (a) (BTW's detector rails are 1.20.1 detector rails) and Q-RS2
 * (a) (28 follows 4.B: an empty cart does not press it).</p>
 *
 * <p><b>Rebalance:</b> none (4.B parity). The comparator reading (getAnalogOutputSignal) is unchanged: it reads a
 * pressed rail's carts, as 1.5.2 BlockDetectorRail.getComparatorInputOverride did (vanilla/client
 * BlockDetectorRail.java:158-170).</p>
 *
 * <p><b>Injection strategy:</b> {@link ModifyArg} on the one getInteractingMinecartOfType call in checkPressed (SRG
 * m_52432_, the call m_52436_; javap of Forge 47.3.0's mapped jar: checkPressed offset 38, the two other calls are in
 * getAnalogOutputSignal), index 3, the entity predicate: 1.20.1's predicate and FC's rule. The handler takes the call's
 * arguments, so the rail's level and position come with it. Applies to every DetectorRailBlock: 28 and
 * FcDetectorRailBlock; another mod's detector rail has no FC rule and keeps 1.20.1's.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> a redirect of the whole query (it would rebuild the private search
 * box); FC's contact body as an FC-owned INSIDE hook (it writes the power through the metadata, a second press path
 * beside 1.20.1's own, with its own tick and comparator timing).</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_redstone: 1.20.1's detector rail counts only the carts FC's detector rail rule accepts
@Mixin(DetectorRailBlock.class)
public abstract class DetectorRailBlockMixin {

    @ModifyArg(method = "checkPressed(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/DetectorRailBlock;getInteractingMinecartOfType(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/lang/Class;Ljava/util/function/Predicate;)Ljava/util/List;"),
            index = 3)
    private Predicate<Entity> btw$fcCartRule(Level level, BlockPos pos, Class<?> cartClass, Predicate<Entity> filter) {
        return cart -> filter.test(cart) && FcDetectorRailBlock.cartTriggers(level, pos, cart);
    }
}
