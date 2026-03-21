package btw.forge;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers MC 1.20.1 {@link MenuType} instances for the BTW container bridge.
 *
 * <p>A single {@code MenuType<FCContainerMenu>} is used for all FC containers
 * (hopper, soulforge, pulley, etc.). The FC container type is determined at
 * runtime by the block that opens the GUI, not by the MenuType.</p>
 *
 * <p>Uses Forge's {@link IForgeMenuType#regular} with an {@code IContainerFactory}
 * so that extra data (slot count, slot positions) can be transmitted to the
 * client in the opening packet.</p>
 */
public class BTWMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BTWForgeMod.MOD_ID);

    /**
     * The single MenuType for all FC container GUIs.
     * The IContainerFactory receives a FriendlyByteBuf with the slot layout
     * so the client can create a matching FCContainerMenu.
     */
    public static final RegistryObject<MenuType<FCContainerMenu>> FC_CONTAINER =
            MENU_TYPES.register("fc_container",
                    () -> IForgeMenuType.create(FCContainerMenu::new));
}
