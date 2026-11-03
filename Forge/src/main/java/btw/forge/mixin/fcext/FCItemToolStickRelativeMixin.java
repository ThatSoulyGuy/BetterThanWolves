package btw.forge.mixin.fcext;

import btw.forge.FcAttachSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * FCMOD-INTEGRATION(1.20.1): support_relatives (placed tools, ruling 2026-11-02)
 *
 * <p><b>Feature:</b> a tool used on a block sticks into it when the block has a centre hard point toward the clicked
 * face and the tool may stick into the block (Server FCItemTool.onItemUse, FCItemTool.java:151-219). Whether a tool may
 * stick into a block is a support question (maintainer ruling 2026-11-02), so a modern fence, wall or stained glass
 * pane that FcAttachSupport.SUPPORT_RELATIVE relates to a BTW block answers it as that relative: every axe sticks into
 * the top or bottom of a spruce fence as into the oak fence (85), a pickaxe does not; the modern walls answer as the
 * cobblestone wall (139), the stained panes as the glass pane (102).</p>
 *
 * <p><b>Conflict with FC:</b> onItemUse takes the stuck-in block from the cell's FC identity,
 * {@code Block.blocksList[world.getBlockId(...)]} (:172), which for a related modern support is its stand-in
 * (ModernBlockStandIn), and asks the tool {@code CanToolStickInBlock(stack, blockStuckIn, world, i, j, k)} (:176): the
 * tool's efficiency against the block (:232-235), refined in FCItemAxe (planks and logs take every axe, Common
 * FCItemAxe.java:163-174), FCItemPickaxe (never glass, Server FCItemPickaxe.java:147-155), FCItemHoe (also a block
 * shovels are effective on, Common FCItemHoe.java:54-58) and FCItemChiselIron (rock takes the chisel, Server
 * FCItemChiselIron.java:91-102). SUPPORT-REL gave the centre hard point (:169) to the relative; the tool test stayed
 * the stand-in's material. The eight flammable wooden fences' stand-in is FLAMMABLE_ROCK, which no FC tool is
 * efficient against, so nothing stuck in them; a crimson or warped fence, and any waterlogged wooden
 * fence (1.20.1's fire odds are 0 for a waterlogged block, so it reads as the plain rock stand-in), took every pickaxe
 * and the iron chisel and no axe: the reverse of 4.B's oak fence (fcMaterialPlanks, axes effective). Walls and panes
 * already answered as their relatives.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend), a read only: the tool test is asked of
 * {@link FcAttachSupport#supportBlock}, the relative's FC block for a stand-in standing in for a related support on a
 * WorldBridge, else the block FC read. Nothing else in onItemUse changes: blockStuckIn stays the stand-in, so its
 * CanToolsStickInBlock (Block's true for the stand-in and the three relatives alike) and the placement sound
 * (PlayPlacementSound reads its step sound and, for the chisel, its material) are the stand-in's; mining speed,
 * harvest, drops, fire and the stand-in's material for every other caller are untouched.</p>
 *
 * <p><b>Rebalance:</b> none (4.B's answer for the relative).</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap of
 * Forge/build/classes/java/fc): {@code onItemUse(ItemStack, EntityPlayer, World, int, int, int, int, float, float,
 * float)} has one {@code invokevirtual FCItemTool.CanToolStickInBlock(ItemStack, Block, World, int, int, int)} (offset
 * 213, the class's only call of it); {@code @ModifyArg} index 1 replaces that call's block. Every FC tool inherits
 * onItemUse (FCItemAxe, FCItemPickaxe, FCItemShovel, FCItemHoe, FCItemChiselIron, FCItemRefinedHoe; the others are not
 * placeable), so their CanToolStickInBlock overrides receive the relative. onItemUse runs on the server only
 * (ProxyBlock.tryPlaceHeldItem); the client predicts no placed tool (its use of a fence, wall or pane passes and the
 * item's useOn does nothing there) and the server resends the held slot after a placement (INV-RESYNC).</p>
 *
 * <p><b>Alternative considered and rejected:</b> a {@code @ModifyVariable} of blockStuckIn (it would also move the
 * placement sound and CanToolsStickInBlock); the relative's material or tool flags on the stand-in (material is
 * identity: mining speed, harvest, fire and lava would change, which the ruling excludes); one mixin per
 * CanToolStickInBlock (FCItemTool and its four overrides; a new FC tool would be missed).</p>
 */
// FCMOD-INTEGRATION(1.20.1): support_relatives: which placed tool sticks into a modern fence, wall or stained glass pane is asked of its BTW relative (oak fence, cobblestone wall, glass pane), as its centre hard point already is
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.item.FCItemTool", remap = false)
public abstract class FCItemToolStickRelativeMixin {

    /** onItemUse's tool test (:174-176): the block the support question is asked of at the stuck-in cell. */
    @ModifyArg(method = "onItemUse(Lbtw/modern/ItemStack;Lbtw/modern/EntityPlayer;Lbtw/modern/World;IIIIFFF)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/src/btw/item/FCItemTool;CanToolStickInBlock(Lbtw/modern/ItemStack;Lbtw/modern/Block;Lbtw/modern/World;III)Z",
                    remap = false),
            index = 1, require = 1, allow = 1, remap = false)
    private btw.modern.Block btw$stickTestBlock(btw.modern.ItemStack stack, btw.modern.Block block,
                                                btw.modern.World world, int i, int j, int k) {
        return FcAttachSupport.supportBlock(block, world, i, j, k);
    }
}
