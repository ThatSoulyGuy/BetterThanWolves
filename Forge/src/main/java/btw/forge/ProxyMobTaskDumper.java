package btw.forge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * One-time-per-class dumper for FC entity AI task lists. Used by all
 * proxy entity types to log which AI tasks each FC entity registered
 * during construction. Output is logged once per FC class to avoid spam.
 */
final class ProxyMobTaskDumper {

    private static final Logger LOGGER = LogManager.getLogger("BTW-AI-DUMP");
    private static final java.util.Set<String> DUMPED = new java.util.HashSet<>();

    private ProxyMobTaskDumper() {}

    static void dumpFcTasks(btw.modern.EntityLiving fc) {
        if (fc == null) return;
        String key = fc.getClass().getName();
        if (!DUMPED.add(key)) return;
        StringBuilder sb = new StringBuilder("[AI-DUMP] ").append(key);
        try {
            sb.append(" tasks=[");
            appendTaskList(sb, fc.tasks);
            sb.append("] targetTasks=[");
            appendTaskList(sb, fc.targetTasks);
            sb.append("]");
            LOGGER.info(sb.toString());
        } catch (Throwable t) {
            LOGGER.warn("[AI-DUMP] failed to dump tasks for {}: {}", key, t.getMessage());
        }
    }

    private static void appendTaskList(StringBuilder sb, btw.modern.EntityAITasks tl) {
        if (tl == null) {
            sb.append("NULL");
            return;
        }
        java.util.List<btw.modern.EntityAITasks.EntityAITaskEntry> entries = tl.taskEntries();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(",");
            var e = entries.get(i);
            sb.append("p").append(e.priority).append(":").append(e.action.getClass().getSimpleName());
        }
    }
}
