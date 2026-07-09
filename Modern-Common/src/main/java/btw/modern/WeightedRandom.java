package btw.modern;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

/**
 * 1.5.2 WeightedRandom (vanilla/server WeightedRandom.java) — weighted selection
 * used by EnchantmentHelper.buildEnchantmentList / mapEnchantmentData and the
 * enchanting table. Was a dead null-stub; ported verbatim.
 */
public class WeightedRandom {

    public static int getTotalWeight(Collection collection) {
        int total = 0;
        for (Iterator it = collection.iterator(); it.hasNext(); ) {
            total += ((WeightedRandomItem) it.next()).itemWeight;
        }
        return total;
    }

    public static WeightedRandomItem getRandomItem(Random rand, Collection collection, int totalWeight) {
        if (totalWeight <= 0) {
            throw new IllegalArgumentException();
        }
        int roll = rand.nextInt(totalWeight);
        Iterator it = collection.iterator();
        WeightedRandomItem item;
        do {
            if (!it.hasNext()) {
                return null;
            }
            item = (WeightedRandomItem) it.next();
            roll -= item.itemWeight;
        } while (roll >= 0);
        return item;
    }

    public static WeightedRandomItem getRandomItem(Random rand, Collection collection) {
        return getRandomItem(rand, collection, getTotalWeight(collection));
    }

    public static int getTotalWeight(WeightedRandomItem[] items) {
        int total = 0;
        for (WeightedRandomItem item : items) {
            total += item.itemWeight;
        }
        return total;
    }

    public static WeightedRandomItem getRandomItem(Random rand, WeightedRandomItem[] items, int totalWeight) {
        if (totalWeight <= 0) {
            throw new IllegalArgumentException();
        }
        int roll = rand.nextInt(totalWeight);
        for (WeightedRandomItem item : items) {
            roll -= item.itemWeight;
            if (roll < 0) {
                return item;
            }
        }
        return null;
    }

    public static WeightedRandomItem getRandomItem(Random rand, WeightedRandomItem[] items) {
        return getRandomItem(rand, items, getTotalWeight(items));
    }
}
