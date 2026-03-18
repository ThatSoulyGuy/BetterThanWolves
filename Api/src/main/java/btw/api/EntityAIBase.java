package btw.api;

public abstract class EntityAIBase {

    private int mutexBits;

    public abstract boolean shouldExecute();

    public boolean continueExecuting() {
        return shouldExecute();
    }

    public boolean isInterruptible() {
        return true;
    }

    public void startExecuting() {}

    public void resetTask() {}

    public void updateTask() {}

    public void setMutexBits(int bits) {
        this.mutexBits = bits;
    }

    public int getMutexBits() {
        return this.mutexBits;
    }
}
