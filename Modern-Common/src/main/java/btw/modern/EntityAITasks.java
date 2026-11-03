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

    /**
     * Resets every executing task that {@code drop} accepts (resetTask, then out of the executing list), as the full pass
     * does for a task whose continueExecuting failed; the next full pass may select it again. Returns how many were reset.
     * Port addition: btw.forge.FcTargetRule drops the tasks that still hold a player it removed as a target.
     */
    public int resetExecutingTasks(java.util.function.Predicate<EntityAIBase> drop) {
        int n = 0;
        Iterator<EntityAITaskEntry> it = executingTasks.iterator();
        while (it.hasNext()) {
            EntityAITaskEntry entry = it.next();
            boolean match;
            try {
                match = drop.test(entry.action);
            } catch (Throwable t) {
                match = false;
            }
            if (!match) continue;
            try {
                entry.action.resetTask();
            } catch (Throwable e) {
                logTaskFailureOnce("resetTask", entry.action, e);
            }
            it.remove();
            n++;
        }
        return n;
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

    /**
     * Ported 1:1 from vanilla/client EntityAITasks.java:69-140; only the per-task try/catch is added.
     *
     * The order matters: a newly selected task is started (goalStart) BEFORE any running lower-priority task
     * it conflicts with is reset, and that task is only dropped on a later full pass, when its own
     * canUse fails. Both started and running tasks then get updateTask in the same tick (goalTick).
     * FCMOD's EntityAIAttackOnCollide.resetTask clears the attack target
     * (vanilla/client EntityAIAttackOnCollide.java:95-101), and EntityAICreeperSwell.startExecuting
     * captures getAttackTarget() (EntityAICreeperSwell.java:33-37). Resetting conflicts first meant the
     * swell task always captured null and the creeper fuse never lit.
     */
    public void onUpdateTasks() {
        List<EntityAITaskEntry> started = new ArrayList<>();

        if (tickCount++ % TICK_RATE == 0) {
            // Full pass (EntityAITasks.java:75-101): re-validate the running tasks and select new ones.
            for (EntityAITaskEntry entry : taskEntries) {
                if (executingTasks.contains(entry)) {
                    boolean keep;
                    try {
                        keep = canUse(entry) && entry.action.continueExecuting();
                    } catch (Throwable e) {
                        logTaskFailureOnce("continueExecuting", entry.action, e);
                        keep = false;
                    }
                    if (keep) continue;
                    try {
                        entry.action.resetTask();
                    } catch (Throwable e) {
                        logTaskFailureOnce("resetTask", entry.action, e);
                    }
                    executingTasks.remove(entry);
                }
                // 1.5.2 falls through here, so a task dropped above may be re-selected in the same pass.
                try {
                    if (canUse(entry) && entry.action.shouldExecute()) {
                        started.add(entry);
                        executingTasks.add(entry);
                    }
                } catch (Throwable e) {
                    logTaskFailureOnce("shouldExecute", entry.action, e);
                }
            }
        } else {
            // In-between ticks (EntityAITasks.java:102-116): only continueExecuting is checked.
            Iterator<EntityAITaskEntry> it = executingTasks.iterator();
            while (it.hasNext()) {
                EntityAITaskEntry entry = it.next();
                boolean keep;
                try {
                    keep = entry.action.continueExecuting();
                } catch (Throwable e) {
                    logTaskFailureOnce("continueExecuting", entry.action, e);
                    keep = false;
                }
                if (!keep) {
                    try {
                        entry.action.resetTask();
                    } catch (Throwable e) {
                        logTaskFailureOnce("resetTask", entry.action, e);
                    }
                    it.remove();
                }
            }
        }

        // goalStart (EntityAITasks.java:118-129). A task whose start throws is dropped without a reset,
        // as before.
        for (EntityAITaskEntry entry : started) {
            try {
                entry.action.startExecuting();
            } catch (Throwable e) {
                logTaskFailureOnce("startExecuting", entry.action, e);
                executingTasks.remove(entry);
            }
        }

        // goalTick (EntityAITasks.java:130-137). Every running task, including those started this tick.
        Iterator<EntityAITaskEntry> it = executingTasks.iterator();
        while (it.hasNext()) {
            EntityAITaskEntry entry = it.next();
            try {
                entry.action.updateTask();
            } catch (Throwable e) {
                logTaskFailureOnce("updateTask", entry.action, e);
                try { entry.action.resetTask(); } catch (Throwable ignored) {}
                it.remove();
            }
        }
    }

    /**
     * vanilla/client EntityAITasks.java:157-186. A task can run if every running task of equal or higher
     * priority (lower or equal number) is mutex-compatible with it, and every running lower-priority task is
     * interruptible. Lower-priority conflicting tasks are not reset here: they fail this same check on the
     * next full pass, because the new task is now running ahead of them.
     */
    private boolean canUse(EntityAITaskEntry candidate) {
        for (EntityAITaskEntry other : taskEntries) {
            if (other == candidate) continue;
            if (candidate.priority >= other.priority) {
                if (executingTasks.contains(other) && !areTasksCompatible(candidate, other)) return false;
            } else if (executingTasks.contains(other) && !other.action.isInterruptible()) {
                return false;
            }
        }
        return true;
    }

    /** vanilla/client EntityAITasks.java:191-194 */
    private static boolean areTasksCompatible(EntityAITaskEntry a, EntityAITaskEntry b) {
        return (a.action.getMutexBits() & b.action.getMutexBits()) == 0;
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
