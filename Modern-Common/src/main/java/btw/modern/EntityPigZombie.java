package btw.modern;

public class EntityPigZombie extends EntityZombie {

    /**
     * Compile-only: the frozen EntityPigZombie ships (Forge/build.gradle shadow excludes) with these public fields, the
     * anger and angry-grunt delay becomeAngryAt sets (vanilla/client EntityPigZombie.java:144-149). btw.forge.FcTargetRule
     * zeroes them with a creative or spectator target.
     */
    public int angerLevel;
    public int randomSoundDelay;

    public EntityPigZombie(World world) {
        super(world);
    }

    public void entityInit() {}
    public void becomeAngryAt(Entity entity) {}
}
