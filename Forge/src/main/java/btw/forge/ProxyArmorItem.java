package btw.forge;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A Forge {@link ArmorItem} that proxies an FC armor item. Unlike {@link ProxyItem}
 * this extends ArmorItem so vanilla 1.20.1 handles equipping (right-click / armor
 * slots), the protection attribute, and worn-on-body rendering automatically —
 * which is exactly what FC armor needs since BTW uses the vanilla armor formula.
 *
 * Defense/durability come from the {@link FCArmorMaterial} (read off the FC item),
 * so {@code use()} is intentionally NOT overridden — vanilla equip-on-use must run.
 * Only display/tooltip/tick are delegated to FC, mirroring {@link ProxyItem}.
 */
public class ProxyArmorItem extends ArmorItem implements LegacyProxyItem {

    private final int legacyId;
    private final String displayName;

    public ProxyArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties props,
                          int legacyId, String displayName) {
        super(material, type, props);
        this.legacyId = legacyId;
        this.displayName = displayName;
    }

    @Override
    public int getLegacyId() {
        return legacyId;
    }

    private btw.modern.Item fc() {
        return btw.modern.Item.itemsList[legacyId];
    }

    @Override
    public Component getName(ItemStack stack) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null) {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                String name = fcItem.getItemDisplayName(fcStack);
                if (name != null && !name.isEmpty()) {
                    return Component.literal(name);
                }
            }
        }
        return Component.literal(displayName);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null) {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                return fcItem.hasEffect(fcStack);
            }
        }
        return super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip, TooltipFlag flag) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null) {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                java.util.List<String> fcTooltip = new java.util.ArrayList<>();
                fcItem.addInformation(fcStack, null, fcTooltip, flag.isAdvanced());
                for (String line : fcTooltip) {
                    if (line != null && !line.isEmpty()) {
                        tooltip.add(Component.literal(line));
                    }
                }
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        btw.modern.Item fcItem = fc();
        if (fcItem == null) return;
        if (level instanceof net.minecraft.server.level.ServerLevel sl
                && entity instanceof net.minecraft.world.entity.player.Player player) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                fcItem.onUpdate(fcStack, WorldBridge.getOrCreate(sl), pb, slot, selected);
            }
        }
    }
}
