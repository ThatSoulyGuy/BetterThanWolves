package btw.forge;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

/** FC's beacon potions as 1.20.1 MobEffects (Client FCBetterThanWolves.java:770-772, Server :771-773). */
// FCMOD-INTEGRATION(1.20.1): fc_beacon: FC's beacon potions (Fortune 31, Looting 30, True Sight 29) are 1.20.1 MobEffects; FC's own readers find them through PotionMapping
public final class BTWMobEffects {
    private BTWMobEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, BTWForgeMod.MOD_ID);
    // new Potion(31, false, 14270531) "potion.fcFortune" icon (2,0) = haste's; new Potion(30, false, 9643043)
    // "potion.fcLooting" icon (4,0) = strength's; new Potion(29, false, 14270531) "potion.fcTrueSight" icon (4,1) = night vision's
    public static final RegistryObject<MobEffect> FORTUNE = EFFECTS.register("fortune", () -> new Marker(0xD9C043));
    public static final RegistryObject<MobEffect> LOOTING = EFFECTS.register("looting", () -> new Marker(0x932423));
    public static final RegistryObject<MobEffect> TRUE_SIGHT = EFFECTS.register("true_sight", () -> new Marker(0xD9C043));

    /** No tick behaviour, no attributes: FC's code reads the effect (EnchantmentHelper, 4.B GuiIngame's True Sight). */
    static final class Marker extends MobEffect {
        Marker(int color) { super(MobEffectCategory.BENEFICIAL, color); }
    }

    public static void register(IEventBus modBus) { EFFECTS.register(modBus); }

    @Nullable
    public static MobEffect byLegacyId(int id) {
        RegistryObject<MobEffect> r = switch (id) {
            case 31 -> FORTUNE;
            case 30 -> LOOTING;
            case 29 -> TRUE_SIGHT;
            default -> null;
        };
        return r != null && r.isPresent() ? r.get() : null;
    }
}
