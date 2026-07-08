package btw.forge.data;

import btw.forge.BTWForgeMod;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;

/**
 * Datapack provider for BTW Global Loot Modifier JSON instances.
 *
 * Each call to {@link #add} writes one
 * {@code data/betterthanwolves/loot_modifiers/<name>.json} file and
 * registers it in {@code global_loot_modifiers.json}.
 *
 * Corresponds to INTEGRATIONS.md {@code #004 — Loot overrides}.
 *
 * Pattern C (rebalance): vanilla loot tables stay intact; we subtract
 * items that trivialize FC's progression curve:
 *   - Ancient debris from bastion chests (bypasses mining netherite —
 *     though per docs/audit/tiers.md netherite is re-tiered to emerald,
 *     chest drops still bypass the tier ladder entirely).
 *   - Diamond gear from ancient-city chests (shortcuts FC's armor chain).
 *   - Enchanted books from pillager outpost (shortcuts enchant chain).
 *   - Totem of Undying drops remain (handled by #026 soul-flux reagent
 *     gate in docs/audit/combat.md §6, not by removing the drop).
 */
public class FCLootOverrides extends GlobalLootModifierProvider {

    public FCLootOverrides(PackOutput output) {
        super(output, BTWForgeMod.MOD_ID);
    }

    @Override
    protected void start() {
        // Bastion chest tiers — strip ancient debris. All four bastion
        // chest tables roll ancient debris at various weights.
        removeFromTable("no_ancient_debris_bastion_treasure",
                new ResourceLocation("minecraft", "chests/bastion_treasure"),
                Items.ANCIENT_DEBRIS);
        removeFromTable("no_ancient_debris_bastion_other",
                new ResourceLocation("minecraft", "chests/bastion_other"),
                Items.ANCIENT_DEBRIS);
        removeFromTable("no_ancient_debris_bastion_hoglin_stable",
                new ResourceLocation("minecraft", "chests/bastion_hoglin_stable"),
                Items.ANCIENT_DEBRIS);
        removeFromTable("no_ancient_debris_bastion_bridge",
                new ResourceLocation("minecraft", "chests/bastion_bridge"),
                Items.ANCIENT_DEBRIS);

        // Ancient city — strip diamond gear (leggings, chestplate,
        // enchanted variants). Echo shards stay — they're reagent for
        // the recovery compass (#062) which gates behind arcane scroll.
        removeFromTable("no_diamond_leggings_ancient_city",
                new ResourceLocation("minecraft", "chests/ancient_city"),
                Items.DIAMOND_LEGGINGS);
        removeFromTable("no_diamond_horse_armor_ancient_city",
                new ResourceLocation("minecraft", "chests/ancient_city"),
                Items.DIAMOND_HORSE_ARMOR);

        // Pillager outpost — strip iron pickaxe and enchanted books.
        removeFromTable("no_iron_pickaxe_pillager_outpost",
                new ResourceLocation("minecraft", "chests/pillager_outpost"),
                Items.IRON_PICKAXE);
        removeFromTable("no_enchanted_book_pillager_outpost",
                new ResourceLocation("minecraft", "chests/pillager_outpost"),
                Items.ENCHANTED_BOOK);

        // Woodland mansion — strip diamond gear.
        removeFromTable("no_diamond_chestplate_woodland_mansion",
                new ResourceLocation("minecraft", "chests/woodland_mansion"),
                Items.DIAMOND_CHESTPLATE);

        // Trail ruins (rare brushing) — strip music discs. Not a progression
        // issue but they're free loot in the FC-unfriendly archaeology loop.
        removeFromTable("no_music_disc_trail_ruins",
                new ResourceLocation("minecraft", "archaeology/trail_ruins_rare"),
                Items.MUSIC_DISC_RELIC);
    }

    private void removeFromTable(String name, ResourceLocation tableId,
                                 net.minecraft.world.item.Item item) {
        LootItemCondition[] conditions = {
                LootTableIdCondition.builder(tableId).build()
        };
        this.add(name, new FCRemoveItemFromLootModifier(conditions, item));
    }
}
