package btw.modern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Bridge-compatible EntityAITasks with a no-arg constructor.
 *
 * Vanilla MC 1.5.2's EntityAITasks requires a Profiler argument, but
 * our EntityLiving bridge stub calls {@code new EntityAITasks()}.
 * This class provides the same priority-based task scheduling as
 * vanilla but with a compatible constructor signature.
 */
public class EntityAITasks {

    private final List<EntityAITaskEntry> taskEntries = new ArrayList<>();
    private final List<EntityAITaskEntry> executingTasks = new ArrayList<>();
    private int tickCount = 0;
    private static final int TICK_RATE = 3;

    public EntityAITasks() {}

    /**
     * Vanilla 1.5.2's real EntityLiving constructor calls
     * {@code new EntityAITasks(worldProfiler)}. Accept the Profiler for
     * signature compatibility — we don't actually use it.
     */
    public EntityAITasks(Profiler profiler) {}

    public List<EntityAITaskEntry> taskEntries() { return taskEntries; }

    public void addTask(int priority, EntityAIBase task) {
        // FCEntityPig and a few others call addTask with the result of
        // helper accessors like getAIControlledByPlayer(). Those helpers
        // return null in our bridge because we don't yet implement saddle
        // / mount control AI. Accepting null would crash the rest of the
        // task pipeline; silently skip the entry instead so the entity
        // still gets the rest of its tasks.
        if (task == null) return;
        taskEntries.add(new EntityAITaskEntry(priority, task));
    }

    public void removeTask(EntityAIBase task) {
        Iterator<EntityAITaskEntry> it = taskEntries.iterator();
        while (it.hasNext()) {
            EntityAITaskEntry entry = it.next();
            if (entry.action == task) {
                if (executingTasks.contains(entry)) {
                    entry.action.resetTask();
                    executingTasks.remove(entry);
                }
                it.remove();
            }
        }
    }

    public void RemoveAllTasks() {
        for (EntityAITaskEntry entry : executingTasks) {
            entry.action.resetTask();
        }
        executingTasks.clear();
        taskEntries.clear();
    }

    public void RemoveAllTasksOfClass(Class clazz) {
        Iterator<EntityAITaskEntry> it = taskEntries.iterator();
        while (it.hasNext()) {
            EntityAITaskEntry entry = it.next();
            if (clazz.isAssignableFrom(entry.action.getClass())) {
                if (executingTasks.contains(entry)) {
                    entry.action.resetTask();
                    executingTasks.remove(entry);
                }
                it.remove();
            }
        }
    }

    private static final Set<String> LOGGED_TASK_FAILURES = new HashSet<>();
    private static final org.apache.logging.log4j.Logger AI_LOGGER =
        org.apache.logging.log4j.LogManager.getLogger("BTW-EntityAITasks");

    private static void logTaskFailureOnce(String stage, EntityAIBase task, Throwable t) {
        String key = stage + "|" + task.getClass().getName() + "|" + t.getClass().getName() + "|" + t.getMessage();
        if (LOGGED_TASK_FAILURES.add(key)) {
            AI_LOGGER.warn("[AI-TASK-FAIL] {} on {}: {}: {}",
                stage, task.getClass().getName(), t.getClass().getSimpleName(), t.getMessage());
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < Math.min(6, st.length); i++) {
                AI_LOGGER.warn("    at {}", st[i]);
            }
        }
    }

    public void onUpdateTasks() {
        tickCount++;

        Iterator<EntityAITaskEntry> execIt = executingTasks.iterator();
        while (execIt.hasNext()) {
            EntityAITaskEntry entry = execIt.next();
            try {
                if (!entry.action.continueExecuting()) {
                    entry.action.resetTask();
                    execIt.remove();
                } else {
                    entry.action.updateTask();
                }
            } catch (Throwable e) {
                logTaskFailureOnce("updateTask/continue", entry.action, e);
                try { entry.action.resetTask(); } catch (Throwable ignored) {}
                execIt.remove();
            }
        }

        if (tickCount % TICK_RATE != 0) return;

        // Diagnostic: log task evaluation every ~3 seconds (60 ticks / TICK_RATE)
        boolean doLog = (tickCount % 60) == 0;

        for (EntityAITaskEntry entry : taskEntries) {
            if (executingTasks.contains(entry)) continue;
            try {
                boolean should = entry.action.shouldExecute();
                if (doLog) {
                    AI_LOGGER.info("[AI-EVAL] p{} {} shouldExecute={}",
                        entry.priority, entry.action.getClass().getSimpleName(), should);
                }
                if (should) {
                    if (canUse(entry)) {
                        interruptConflicts(entry);
                        try {
                            entry.action.startExecuting();
                            executingTasks.add(entry);
                            AI_LOGGER.info("[AI-START] p{} {} started",
                                entry.priority, entry.action.getClass().getSimpleName());
                        } catch (Throwable e) {
                            logTaskFailureOnce("startExecuting", entry.action, e);
                        }
                    }
                }
            } catch (Throwable e) {
                logTaskFailureOnce("shouldExecute", entry.action, e);
            }
        }
    }

    private boolean canUse(EntityAITaskEntry candidate) {
        for (EntityAITaskEntry running : executingTasks) {
            if (running == candidate) continue;
            if ((candidate.action.getMutexBits() & running.action.getMutexBits()) != 0) {
                if (candidate.priority >= running.priority) return false;
                if (!running.action.isInterruptible()) return false;
            }
        }
        return true;
    }

    private void interruptConflicts(EntityAITaskEntry candidate) {
        Iterator<EntityAITaskEntry> it = executingTasks.iterator();
        while (it.hasNext()) {
            EntityAITaskEntry running = it.next();
            if ((candidate.action.getMutexBits() & running.action.getMutexBits()) != 0) {
                running.action.resetTask();
                it.remove();
            }
        }
    }

    public boolean areTasksExecuting() {
        return !executingTasks.isEmpty();
    }

    public static class EntityAITaskEntry {
        public final int priority;
        public final EntityAIBase action;

        public EntityAITaskEntry(int priority, EntityAIBase action) {
            this.priority = priority;
            this.action = action;
        }
    }
}
