package btw.forge;

import net.minecraft.world.entity.player.Abilities;

/**
 * Bridges {@link Abilities} to {@link btw.modern.PlayerCapabilities}.
 * FC code reads capability flags (creative, flying, damage immunity)
 * through btw.modern.PlayerCapabilities fields.
 */
public class CapabilitiesBridge extends btw.modern.PlayerCapabilities {
    private final Abilities abilities;

    public CapabilitiesBridge(Abilities abilities) {
        this.abilities = abilities;
        sync();
    }

    /** Copy current abilities state to FC fields */
    public void sync() {
        this.disableDamage = abilities.invulnerable;
        this.isCreativeMode = abilities.instabuild;
        this.allowFlying = abilities.mayfly;
        this.isFlying = abilities.flying;
        this.allowEdit = abilities.mayBuild;
        this.flySpeed = abilities.getFlyingSpeed();
        this.walkSpeed = abilities.getWalkingSpeed();
    }

    @Override
    public float getFlySpeed() { return abilities.getFlyingSpeed(); }

    @Override
    public float getWalkSpeed() { return abilities.getWalkingSpeed(); }
}
