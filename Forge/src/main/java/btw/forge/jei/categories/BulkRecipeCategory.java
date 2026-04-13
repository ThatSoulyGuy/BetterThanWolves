package btw.forge.jei.categories;

import btw.forge.jei.BulkRecipeWrapper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Shared JEI category for FC bulk-style recipes (cauldron / crucible /
 * mill stone). All three recipe managers share the same input/output
 * layout: N ingredient stacks on the left, M output stacks on the right.
 *
 * <p>Slots are laid out in a 3×3 grid on each side, big enough for the
 * worst case. Unused slot positions simply stay empty.</p>
 */
public class BulkRecipeCategory implements IRecipeCategory<BulkRecipeWrapper> {

    /** 3x3 grid on each side + arrow in the middle = 120 wide, 58 tall. */
    private static final int WIDTH = 120;
    private static final int HEIGHT = 58;

    private static final int GRID_COLS = 3;
    private static final int GRID_ROWS = 3;
    private static final int SLOT_SIZE = 18;

    // Left-grid top-left corner
    private static final int INPUTS_X = 0;
    private static final int INPUTS_Y = 2;
    // Right-grid top-left corner
    private static final int OUTPUTS_X = WIDTH - GRID_COLS * SLOT_SIZE;
    private static final int OUTPUTS_Y = 2;

    private final RecipeType<BulkRecipeWrapper> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    public BulkRecipeCategory(IGuiHelper guiHelper,
                              ResourceLocation id,
                              String titleKey,
                              ItemStack iconStack) {
        this.recipeType = RecipeType.create(id.getNamespace(), id.getPath(), BulkRecipeWrapper.class);
        this.title = Component.translatable(titleKey);
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(iconStack);
    }

    @Override
    public RecipeType<BulkRecipeWrapper> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BulkRecipeWrapper recipe, IFocusGroup focuses) {
        placeStacks(builder, RecipeIngredientRole.INPUT, recipe.inputs(), INPUTS_X, INPUTS_Y);
        placeStacks(builder, RecipeIngredientRole.OUTPUT, recipe.outputs(), OUTPUTS_X, OUTPUTS_Y);
    }

    private static void placeStacks(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                                    List<ItemStack> stacks, int originX, int originY) {
        int maxSlots = GRID_COLS * GRID_ROWS;
        int n = Math.min(stacks.size(), maxSlots);
        for (int i = 0; i < n; i++) {
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int x = originX + col * SLOT_SIZE;
            int y = originY + row * SLOT_SIZE;
            builder.addSlot(role, x + 1, y + 1)
                    .addIngredient(VanillaTypes.ITEM_STACK, stacks.get(i));
        }
    }
}
