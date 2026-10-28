package btw.forge.mixin.client;

import btw.forge.ProxyItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void btw$skipClientPrediction(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide() && context.getHand() == InteractionHand.MAIN_HAND
                && ProxyItem.fcItemFor(context.getItemInHand().getItem()) != null) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
