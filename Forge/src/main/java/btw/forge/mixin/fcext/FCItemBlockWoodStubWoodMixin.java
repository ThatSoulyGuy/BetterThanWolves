package btw.forge.mixin.fcext;

import btw.forge.ModernWoodSubBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks (W1, the wood stubs)
 *
 * <p><b>Feature:</b> FC's five wood stub items (siding 196, moulding 197, corner 198, moulding decorative 199, siding
 * decorative 200) of wood types 5-8 place and name the newer woods' own sub-blocks (ModernWoodSubBlocks, 1093-1100).</p>
 *
 * <p><b>Conflict with FC:</b> each stub turns its wood type into a block and a name through a switch over 0-4 whose
 * default is blood wood (Common FCItemBlockWoodSidingStub.java:31-86, FCItemBlockWoodMouldingStub.java:31-86,
 * FCItemBlockWoodCornerStub.java:31-86; Client FCItemBlockWoodMouldingDecorativeStub.java:36-129,
 * FCItemBlockWoodSidingDecorativeStub.java:35-120), so wood types 5-8 placed blood wood blocks and read ".blood"; the
 * siding decorative stub also names its type {@code damage >> 2}, so a 5-8 bench read "fence".</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): FC's stubs, with four more woods. Wood types 0-4 keep FC's own path and
 * 4.B's names (the blood bench is still named "fence", as in 4.B).</p>
 *
 * <p><b>Rebalance:</b> none.</p>
 *
 * <p><b>Injection strategy:</b> one load-time mixin over the five FC classes ({@code @Pseudo}, {@code remap = false};
 * the same two descriptors on all five, javap of Forge/build/classes/java/fc): {@code GetBlockIDToPlace(IIFFF)I} HEAD,
 * cancellable, answers the wood's block (ProxyBlockItem.blockToPlace and FC's own onItemUse both ask it); the placed
 * metadata stays FC's {@code getMetadata(damage)}. {@code getUnlocalizedName(ItemStack)} HEAD, cancellable, answers
 * {@code <ItemBlock name>.<wood>[.<type>]} by FC's GetBlockType, named in /lang/BTW_modern_en_US.lang.</p>
 *
 * <p><b>Alternative considered and rejected:</b> five mixins of one method each (the same two injects on five
 * classes); new stub items for the four woods (the stubs are wood-agnostic by damage, FC's recipes and the saw already
 * produce them).</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: FC's wood stubs place and name wood types 5-8 as their own woods
@Pseudo
@Mixin(targets = {
        "net.minecraft.src.btw.item.FCItemBlockWoodSidingStub",
        "net.minecraft.src.btw.item.FCItemBlockWoodMouldingStub",
        "net.minecraft.src.btw.item.FCItemBlockWoodCornerStub",
        "net.minecraft.src.btw.item.FCItemBlockWoodMouldingDecorativeStub",
        "net.minecraft.src.btw.item.FCItemBlockWoodSidingDecorativeStub"}, remap = false)
public abstract class FCItemBlockWoodStubWoodMixin {

    /** GetBlockIDToPlace: a stub of wood type 5-8 places its wood's sub-block, where FC answers blood wood's. */
    @Inject(method = "GetBlockIDToPlace(IIFFF)I", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void btw$modernWoodBlock(int damage, int facing, float clickX, float clickY, float clickZ,
                                     CallbackInfoReturnable<Integer> cir) {
        int id = ModernWoodSubBlocks.stubBlockToPlace(((btw.modern.Item) (Object) this).itemID, damage);
        if (id > 0) {
            cir.setReturnValue(id);
        }
    }

    /** getUnlocalizedName(ItemStack): {@code <name>.<wood>[.<type>]} for 5-8, where FC answers ".blood". */
    @Inject(method = "getUnlocalizedName(Lbtw/modern/ItemStack;)Ljava/lang/String;", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void btw$modernWoodName(btw.modern.ItemStack stack, CallbackInfoReturnable<String> cir) {
        if (stack == null) return;
        String suffix = ModernWoodSubBlocks.stubNameSuffix(((btw.modern.Item) (Object) this).itemID,
                stack.getItemDamage());
        if (suffix != null) {
            cir.setReturnValue(((btw.modern.Item) (Object) this).getUnlocalizedName() + suffix);
        }
    }
}
