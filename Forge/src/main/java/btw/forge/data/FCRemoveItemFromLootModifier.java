package btw.forge.data;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * Global Loot Modifier that strips a single item from generated loot.
 *
 * Used under INTEGRATIONS.md Pattern C (rebalance) for loot tables that
 * drop items trivializing FC progression — bastion ancient debris,
 * ancient-city diamond gear, pillager-outpost enchanted books, etc.
 *
 * Applied as a JSON file under
 * {@code data/betterthanwolves/loot_modifiers/<name>.json} with a
 * {@code LootTableIdCondition} selecting the target loot table.
 */
public class FCRemoveItemFromLootModifier extends LootModifier {

    public static final Supplier<Codec<FCRemoveItemFromLootModifier>> CODEC =
            Suppliers.memoize(() -> RecordCodecBuilder.create(inst ->
                    LootModifier.codecStart(inst).and(
                            ForgeRegistries.ITEMS.getCodec().fieldOf("item")
                                    .forGetter(m -> m.item)
                    ).apply(inst, FCRemoveItemFromLootModifier::new)));

    private final Item item;

    protected FCRemoveItemFromLootModifier(LootItemCondition[] conditionsIn, Item item) {
        super(conditionsIn);
        this.item = item;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                 LootContext context) {
        generatedLoot.removeIf(stack -> stack.getItem() == this.item);
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
