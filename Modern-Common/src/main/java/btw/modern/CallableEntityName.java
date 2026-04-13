package btw.modern;

import java.util.concurrent.Callable;

/** Vanilla 1.5.2 helper — see {@link CallableEntityType}. */
public class CallableEntityName implements Callable {
    final Entity theEntity;

    public CallableEntityName(Entity entity) {
        this.theEntity = entity;
    }

    public String callEntityName() {
        return this.theEntity == null ? "unknown" : this.theEntity.getEntityName();
    }

    @Override
    public Object call() {
        return this.callEntityName();
    }
}
