package btw.modern;

public class EntityWither extends EntityMob {

    public EntityWither(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 300;
    }

    public int getWatchedTargetId(int head) {
        return 0;
    }

    /**
     * Compile-only: the frozen EntityWither ships (Forge/build.gradle shadow excludes); it sets head {@code head}'s watched
     * target id (vanilla/client EntityWither.java:335-342). btw.forge.FcTargetRule clears heads 1-2 with it.
     */
    public void func_82211_c(int head, int entityId) {}

    public int getInvulTime() {
        return 0;
    }

    public void setInvulTime(int time) {}

    public void entityInit() {}
}
