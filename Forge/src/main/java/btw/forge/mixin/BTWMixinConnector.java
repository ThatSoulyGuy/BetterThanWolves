package btw.forge.mixin;

import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Mixin connector that registers the BTW mixin config early in the loading
 * process. Declared as a service in META-INF/services.
 */
public class BTWMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("betterthanwolves.mixins.json");
    }
}
