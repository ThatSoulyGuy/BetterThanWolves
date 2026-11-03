package btw.forge;

import btw.forge.recipes.ModernLogChoppingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * FC's saw on 1.20.1 wood blocks FC has no id for (INTEGRATIONS #022, "FC actions visible to other mods"): the stairs
 * and slabs of acacia, dark oak, mangrove and cherry ({@link ModernWoodSubBlocks}), and the logs of a
 * {@code betterthanwolves:modern_log_chopping} recipe (the bamboo blocks, {@link ModernLogChoppingRecipe}). FC's saw
 * reaches them only as a {@link ModernBlockStandIn}, which asks {@link ModernBlockHooks}; this hook answers with
 * FCBlockSaw's own cases for 4.B's wood stairs and slabs (FCBlockSaw.java:640-719) and with the recipe's saw yield.
 *
 * <p>Registered once in the BTWForgeMod constructor right after {@link ModernLogHooks#register}, so an integration's
 * own hook (Dynamic Trees #027) and the log rule are asked first. The stairs and slab case needs the sub-blocks (its
 * output is wood-type 5-8 stubs); the chopping-log case does not. The hook runs on the server thread only
 * (ModernBlockStandIn.askHook); a true answer makes FC's SawBlockToFront emit its saw particles itself.</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: FC's saw cuts the newer woods' 1.20.1 stairs and slabs as it cuts 4.B's, and the bamboo blocks into planks and sawdust
public final class ModernWoodSawHooks implements ModernBlockHooks.Hook {

    /** The one instance. */
    static final ModernWoodSawHooks INSTANCE = new ModernWoodSawHooks();

    private static volatile boolean registered;

    private ModernWoodSawHooks() {}

    /** Registers the saw hook for the newer woods' stairs and slabs and the chopping logs (idempotent). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ModernBlockHooks.register(ModernWoodSawHooks::applies, INSTANCE);
    }

    /** The live states this hook owns: the four woods' stairs and slabs (while built) and the chopping logs. */
    static boolean applies(BlockState state) {
        Block block = state.getBlock();
        return ModernWoodSubBlocks.stairsOrSlabWoodType(block) >= 0 || ModernLogChoppingRecipe.sawRecipeFor(block) != null;
    }

    /**
     * FCBlockSaw cut the block. Stairs: one siding and one moulding of its wood (FCBlockSaw.java:640-679); a double
     * slab: two single slabs (:680-689); a single slab: two mouldings (:690-699). A chopping log: the recipe's saw
     * planks and sawdust, no bark (maintainer ruling 2026-10-21). The cell is then cleared (a waterlogged block leaves
     * its water).
     */
    // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: FC's saw cuts the newer woods' stairs and slabs as FCBlockSaw.java:640-719 cuts 4.B's
    @Override
    public boolean onSawed(ServerLevel level, BlockPos pos, BlockState state, Direction sawFacing) {
        int woodType = ModernWoodSubBlocks.stairsOrSlabWoodType(state.getBlock());
        if (woodType >= 0) {
            if (state.getBlock() instanceof StairBlock) {
                pop(level, pos, ModernWoodSubBlocks.sidingStack(woodType, 1));
                pop(level, pos, ModernWoodSubBlocks.mouldingStack(woodType, 1));
            } else if (state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                pop(level, pos, new ItemStack(state.getBlock(), 2));
            } else {
                pop(level, pos, ModernWoodSubBlocks.mouldingStack(woodType, 2));
            }
            level.removeBlock(pos, false);
            return true;
        }
        // FCMOD-INTEGRATION(1.20.1): bamboo_planks: FC's saw cuts a bamboo block into planks and sawdust, no bark (ruling 2026-10-21)
        ModernLogChoppingRecipe recipe = ModernLogChoppingRecipe.sawRecipeFor(state.getBlock());
        if (recipe == null) return false;
        pop(level, pos, recipe.sawPlanks());
        pop(level, pos, FCLogRules.sawdust(recipe.sawSawdust()));
        level.removeBlock(pos, false);
        return true;
    }

    private static void pop(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) Block.popResource(level, pos, stack);          // as ModernLogHooks.pop
    }

    @Override
    public String toString() {
        return "FC's saw on 1.20.1 wood blocks (the newer woods' stairs and slabs, modern_log_chopping logs)";
    }
}
