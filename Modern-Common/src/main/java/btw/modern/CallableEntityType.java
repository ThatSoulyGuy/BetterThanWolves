package btw.modern;

import java.util.concurrent.Callable;

/**
 * Vanilla 1.5.2 helper used by CrashReport to include entity type info
 * in crash reports. Referenced by Entity.addEntityCrashInfo.
 * Package-private in vanilla, made public here for simplicity.
 */
public class CallableEntityType implements Callable {
    final Entity theEntity;

    public CallableEntityType(Entity entity) {
        this.theEntity = entity;
    }

    public String callEntityType() {
        return this.theEntity == null ? "unknown"
                : this.theEntity.getClass().getCanonicalName();
    }

    @Override
    public Object call() {
        return this.callEntityType();
    }
}
