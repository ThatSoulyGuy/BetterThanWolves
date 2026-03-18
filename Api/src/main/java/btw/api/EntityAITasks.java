package btw.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages a set of AI tasks for an entity.
 * Mirrors net.minecraft.src.EntityAITasks.
 */
public class EntityAITasks {

    private List taskEntries = new ArrayList();

    public void addTask(int priority, EntityAIBase task) {
    }

    public void removeTask(EntityAIBase task) {
    }

    public void RemoveAllTasks() {
        taskEntries.clear();
    }

    public void RemoveAllTasksOfClass(Class clazz) {
    }

    public void onUpdateTasks() {
    }

    public boolean areTasksExecuting() {
        return false;
    }
}
