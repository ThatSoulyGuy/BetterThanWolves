package btw.modern;

/**
 * Vanilla 1.5.2 thread-local holder for {@link AABBPool}. FC's vanilla
 * {@link AxisAlignedBB}'s static initializer creates one of these and
 * uses it to grab a per-thread pool.
 *
 * Package-private final in vanilla; we make it public so the
 * {@code btw.modern.*} relocation doesn't change visibility semantics.
 */
public final class AABBLocalPool extends ThreadLocal<AABBPool> {
    @Override
    protected AABBPool initialValue() {
        return new AABBPool(300, 2000);
    }
}
