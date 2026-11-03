package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Forge item of an FC block (registered by BTWRegistration as {@code betterthanwolves:block_<id>} for every
 * ProxyBlock, and for the FC blocks registered as 1.20.1 block classes: BTW's detector rails, FcDetectorRailBlock). The FC metadata subtype of a stack rides its {@code Damage} tag (ItemStackHelper.toMcStack), so one
 * item carries every subtype, as 1.5.2's one ItemBlock id did.
 *
 * <ul>
 *   <li>Name: FC's own, localized and metadata-aware, as 1.5.2 named every stack
 *       ({@code Item.itemsList[id].getItemDisplayName(stack)}; ProxyItem does the same for standalone items).</li>
 *   <li>Placement through the 1.20.1 BlockItem path (blocks with no legacy id clicked, and the client's prediction):
 *       the block FC's ItemBlock names for the stack, {@code GetBlockIDToPlace(damage, side, hit)}, as
 *       FCItemPlacesAsBlock.onItemUse (Common FCItemPlacesAsBlock.java:63) chose it -- e.g. FCItemBlockAestheticOpaque
 *       places soulforged steel as fcSoulforgedSteelBlock, the wood siding stubs place their species block. That
 *       block's getStateForPlacement turns the held stack into FC's metadata. Stack consumption, the creative
 *       keep, setPlacedBy and the place sound stay vanilla BlockItem.place, which matches 1.5.2 (stackSize-- outside
 *       creative, (volume + 1) / 2 and pitch * 0.8).</li>
 * </ul>
 *
 * Not a {@link LegacyProxyItem}: ItemStackMixin keeps running the FC item hooks for block items.
 */
public class ProxyBlockItem extends BlockItem {

    private final int legacyId;
    /** Last-resort name (BTWRegistration.formatFcName of the block's name) when FC has no translation. */
    private final String displayName;

    // FCMOD-INTEGRATION(1.20.1): fc_redstone: also the item of an FC block registered as a 1.20.1 class (FcDetectorRailBlock)
    public ProxyBlockItem(Block block, Properties props, int legacyId, String displayName) {
        super(block, props);
        this.legacyId = legacyId;
        this.displayName = displayName;
    }

    public int getLegacyId() {
        return legacyId;
    }

    private btw.modern.Item fc() {
        return legacyId > 0 && legacyId < btw.modern.Item.itemsList.length ? btw.modern.Item.itemsList[legacyId] : null;
    }

    // ================================================================
    // Name -> FC getItemDisplayName
    // ================================================================

    /**
     * 1.5.2 shows a stack under {@code Item.itemsList[id].getItemDisplayName(stack)}: for a block item the translation
     * of "tile." + the block's name plus FC's subtype suffix (Modern-Common ItemBlock.getUnlocalizedName,
     * FCItemBlockAestheticOpaque.getUnlocalizedName ".soap" -> "Block of Soap"). The old constant camel-case split of
     * the raw block name gave every subtype the base name ("Aesthetic Opaque") and legacy blocks "Fc ..." names.
     */
    @Override
    public Component getName(ItemStack stack) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null) {
            try {
                btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
                if (fcStack != null) {
                    String name = fcItem.getItemDisplayName(fcStack);
                    if (isTranslated(name, fcItem, fcStack)) {
                        return Component.literal(name);
                    }
                }
            } catch (Throwable t) {
                ProxyItem.logFcFailureOnce("getItemDisplayName", legacyId, t);
            }
        }
        return Component.literal(displayName);
    }

    /**
     * Modern-Common Item.getItemDisplayName hands back the raw unlocalized key when neither the subtype key nor the
     * base key has a translation; that is no name to show ("tile.fcBlockWeeds").
     */
    private static boolean isTranslated(String name, btw.modern.Item fcItem, btw.modern.ItemStack fcStack) {
        if (name == null) return false;
        String trimmed = name.trim();
        return !trimmed.isEmpty()
                && !trimmed.endsWith(".name")
                && !trimmed.equals(fcItem.getUnlocalizedName(fcStack))
                && !trimmed.equals(fcItem.getUnlocalizedName());
    }

    // ================================================================
    // Placement -> FC GetBlockIDToPlace
    // ================================================================

    @Override
    protected BlockState getPlacementState(BlockPlaceContext ctx) {
        Block target = blockToPlace(ctx);
        if (target == this.getBlock()) {
            return super.getPlacementState(ctx);
        }
        // BlockItem.getPlacementState (1.20.1 BlockItem.java:118-121) for the block FC names instead of this one.
        // BlockItem.place then sets that state, runs its setPlacedBy and plays its place sound.
        BlockState state = target.getStateForPlacement(ctx);
        return state != null && this.canPlace(ctx, state) ? state : null;
    }

    /**
     * The block FC's ItemBlock places for the held stack: {@code GetBlockIDToPlace(itemDamage, iFacing, fClickX, fClickY,
     * fClickZ)} as FCItemPlacesAsBlock.onItemUse calls it (Common FCItemPlacesAsBlock.java:63), with the click offsets
     * relative to the CLICKED block (1.5.2 ItemInWorldManager / PlayerControllerMP pass hitVec minus the clicked block's
     * coordinates), not to the position the block goes to. This item's own block when FC names no other, names an id
     * without a 1.20.1 block, or fails.
     */
    private Block blockToPlace(BlockPlaceContext ctx) {
        if (!(fc() instanceof btw.modern.ItemBlock fcItemBlock)) return this.getBlock();
        try {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(ctx.getItemInHand());
            int damage = fcStack != null ? fcStack.getItemDamage() : 0;
            BlockPos clicked = ctx.replacingClickedOnBlock()
                    ? ctx.getClickedPos() : ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite());
            Vec3 hit = ctx.getClickLocation();
            int blockId = fcItemBlock.GetBlockIDToPlace(damage, ctx.getClickedFace().get3DDataValue(),
                    (float) (hit.x - clicked.getX()), (float) (hit.y - clicked.getY()), (float) (hit.z - clicked.getZ()));
            if (blockId == legacyId) return this.getBlock();
            Block block = ProxyRegistry.getModernBlock(blockId);
            if (block != null && block != Blocks.AIR) return block;
        } catch (Throwable t) {
            ProxyItem.logFcFailureOnce("GetBlockIDToPlace", legacyId, t);
        }
        return this.getBlock();
    }
}
