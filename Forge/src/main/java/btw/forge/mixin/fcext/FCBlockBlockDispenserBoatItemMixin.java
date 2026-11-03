package btw.forge.mixin.fcext;

import btw.forge.VanillaEntityViews;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_entity_views (INTEGRATIONS #057)
 *
 * <p><b>Feature:</b> 1.20.1 has a boat and a chest boat per wood type, each dropping its own item
 * (Boat.getDropItem, ChestBoat.getDropItem).</p>
 *
 * <p><b>Conflict with FC:</b> 4.B's block dispenser files every boat it takes as {@code Item.boat}, the one 1.5.2 boat
 * (Client FCBlockBlockDispenser.ConsumeEntityAtTargetLoc, :326-334). Through the boat views of VanillaEntityViews it now
 * takes 1.20.1 boats, and the literal item would turn a spruce boat into an oak one and lose a chest boat's chest.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend). Ruling Q-CARTS-3 (a): the dispenser keeps the boat it took as that
 * boat's own item. FC's take stays as written (the boat's setDead, the item into the dispenser, the click); only the item
 * id changes, to the one VanillaEntityViews' boat view recorded when FC killed it (its 1.20.1 drop item as an FC item
 * id: the oak boat is FC's 333, the others pass through as their own items).</p>
 *
 * <p><b>Rebalance:</b> none beyond the ruling: the oak boat is still FC's boat item.</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap of
 * Forge/build/classes/java/fc: {@code private boolean ConsumeEntityAtTargetLoc(btw.modern.World, int, int, int, int,
 * int, int)}, the boat branch {@code invokevirtual Entity.setDead} at offset 261, {@code getstatic Item.boat} at 266,
 * {@code invokestatic FCUtilsInventory.AddSingleItemToInventory(IInventory, int, int)} at 273). {@link ModifyArg} on
 * that call's item id (index 1), the first AddSingleItemToInventory after the {@code Item.boat} read (a slice from it),
 * allow 1; and an {@link Inject} at HEAD of the same method forgets any boat recorded before this take, so a boat
 * that is no view (an FC puppet) keeps FC's item.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> 4.B's literal boat (answer (b)); a redirect of the {@code Item.boat}
 * read (it cannot see which boat was taken).</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_entity_views: the block dispenser files a 1.20.1 boat it takes under that boat's own item
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.block.FCBlockBlockDispenser", remap = false)
public abstract class FCBlockBlockDispenserBoatItemMixin {

    @Inject(method = "ConsumeEntityAtTargetLoc(Lbtw/modern/World;IIIIII)Z", at = @At("HEAD"), require = 1, remap = false)
    private void btw$forgetEarlierBoat(CallbackInfoReturnable<Boolean> cir) {
        VanillaEntityViews.takeConsumedBoatItem(0);
    }

    @ModifyArg(method = "ConsumeEntityAtTargetLoc(Lbtw/modern/World;IIIIII)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/src/btw/util/FCUtilsInventory;AddSingleItemToInventory(Lbtw/modern/IInventory;II)Z",
                    ordinal = 0, remap = false),
            slice = @Slice(from = @At(value = "FIELD", target = "Lbtw/modern/Item;boat:Lbtw/modern/Item;",
                    opcode = Opcodes.GETSTATIC, remap = false)),
            index = 1, require = 1, allow = 1, remap = false)
    private int btw$boatsOwnItem(int itemId) {
        return VanillaEntityViews.takeConsumedBoatItem(itemId);
    }
}
