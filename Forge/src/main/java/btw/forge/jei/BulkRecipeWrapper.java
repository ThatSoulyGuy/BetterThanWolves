package btw.forge.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A single FC bulk-recipe (cauldron / crucible / mill stone) translated
 * into MC ItemStacks, ready to hand to JEI.
 *
 * @param inputs  one stack per input ingredient slot
 * @param outputs one stack per output slot
 */
public record BulkRecipeWrapper(List<ItemStack> inputs, List<ItemStack> outputs) {
}
