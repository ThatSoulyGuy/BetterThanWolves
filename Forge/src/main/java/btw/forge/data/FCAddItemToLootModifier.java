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
 * Global Loot Modifier that adds a single item to generated loot with a given chance.
 *
 * Used to reintroduce FC's net-new structure loot without replacing entire vanilla loot
 * tables (INTEGRATIONS.md #003 — prefer data over code). The headline case is the FC
 * Lightning Rod in jungle temples — the only genuinely new item FC adds to any vanilla
 * structure chest. {@code chance} approximates FC's weighted-pool probability.
 *
 * Applied as {@code data/betterthanwolves/loot_modifiers/<name>.json} with a
 * {@code forge:loot_table_id} condition selecting the target loot table.
 */
public class FCAddItemToLootModifier extends LootModifier {

    public static final Supplier<Codec<FCAddItemToLootModifier>> CODEC =
            Suppliers.memoize(() -> RecordCodecBuilder.create(inst ->
                    LootModifier.codecStart(inst)
                            .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
                            .and(Codec.INT.optionalFieldOf("min_count", 1).forGetter(m -> m.minCount))
                            .and(Codec.INT.optionalFieldOf("max_count", 1).forGetter(m -> m.maxCount))
                            .and(Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(m -> m.chance))
                            .apply(inst, FCAddItemToLootModifier::new)));

    private final Item item;
    private final int minCount;
    private final int maxCount;
    private final float chance;

    protected FCAddItemToLootModifier(LootItemCondition[] conditionsIn, Item item,
                                      int minCount, int maxCount, float chance) {
        super(conditionsIn);
        this.item = item;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                 LootContext context) {
        if (context.getRandom().nextFloat() < this.chance) {
            int span = this.maxCount - this.minCount;
            int count = span <= 0 ? this.minCount : this.minCount + context.getRandom().nextInt(span + 1);
            if (count > 0) {
                generatedLoot.add(new ItemStack(this.item, count));
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
