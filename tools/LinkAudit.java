import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Member-level link audit for the BTW Forge port.
 *
 * Rebuilds the EFFECTIVE runtime class set exactly as the dist jar does
 * (Modern-Common first minus the exclude list, then the relocated fc
 * classes, plus Forge main), then verifies that every method/field/ctor
 * reference in that set's bytecode resolves against the class that
 * actually wins the shadow-merge. Unresolved refs = latent
 * NoSuchMethodError / NoSuchFieldError / NoClassDefFoundError.
 */
public class LinkAudit {
    static final String ROOT = "D:/IntelliJ/IDEA/Projects/BetterThanWolves";
    static final String FC_DIR = ROOT + "/Forge/build/classes/java/fc";
    static final String MC_DIR = ROOT + "/Modern-Common/build/classes/java/main";
    static final String FORGE_DIR = ROOT + "/Forge/build/classes/java/main";
    static final String API_DIR = ROOT + "/Api/build/classes/java/main";
    static final String GRADLE = ROOT + "/Forge/build.gradle";

    static final Set<String> OBJECT_METHODS = new HashSet<>(Arrays.asList(
        "equals(Ljava/lang/Object;)Z", "hashCode()I", "toString()Ljava/lang/String;",
        "getClass()Ljava/lang/Class;", "clone()Ljava/lang/Object;",
        "wait()V", "wait(J)V", "wait(JI)V", "notify()V", "notifyAll()V",
        "finalize()V", "<init>()V"));

    static class ClassInfo {
        String name, superName, source;
        List<String> interfaces = new ArrayList<>();
        Set<String> methods = new HashSet<>();  // name+desc
        Set<String> fields = new HashSet<>();   // name+desc
        List<String[]> refs = new ArrayList<>(); // [kind, owner, name, desc]
    }

    static Map<String, ClassInfo> runtime = new TreeMap<>();
    static Map<String, ClassInfo> api = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        // ---- parse shadow lists from build.gradle ----
        String gradle = new String(Files.readAllBytes(Paths.get(GRADLE)), StandardCharsets.UTF_8);
        Set<String> jarExcludes = new TreeSet<>();
        Matcher m = Pattern.compile("exclude 'btw/modern/([A-Za-z0-9_$]+)\\.class'").matcher(gradle);
        while (m.find()) jarExcludes.add(m.group(1));

        Set<String> devDeletes = new TreeSet<>();
        int taskIdx = gradle.indexOf("task removeModernCommonShadowedClasses");
        int endIdx = gradle.indexOf("].each", taskIdx);
        Matcher d = Pattern.compile("'([A-Za-z0-9_$]+)\\.class'").matcher(gradle.substring(taskIdx, endIdx));
        while (d.find()) devDeletes.add(d.group(1));

        // ---- dev/jar shadow-list drift ----
        Set<String> devOnly = new TreeSet<>(devDeletes); devOnly.removeAll(jarExcludes);
        Set<String> jarOnly = new TreeSet<>(jarExcludes); jarOnly.removeAll(devDeletes);
        System.out.println("== Shadow-list drift ==");
        System.out.println("dev-delete only (stub still ships in PROD jar): " + devOnly);
        System.out.println("jar-exclude only (stub still shadows FC in DEV): " + jarOnly);

        // ---- build effective runtime set, mirroring the jar task ----
        loadDir(FC_DIR, "fc", runtime, null);
        // Modern-Common wins unless excluded (jar puts MC first; EXCLUDE strategy)
        Map<String, ClassInfo> mc = new TreeMap<>();
        loadDir(MC_DIR, "modern-common", mc, null);
        List<String> orphanExcludes = new ArrayList<>();
        for (ClassInfo ci : mc.values()) {
            if (ci.name.startsWith("btw/api/")) continue;
            String simple = ci.name.startsWith("btw/modern/") ? ci.name.substring("btw/modern/".length()) : null;
            if (simple != null && jarExcludes.contains(simple)) {
                if (!runtime.containsKey(ci.name)) orphanExcludes.add(ci.name);
                continue; // fc version wins
            }
            runtime.put(ci.name, ci);
        }
        loadDir(FORGE_DIR, "forge", runtime, null);
        loadDir(API_DIR, "api(dev-only)", api, null);
        System.out.println("\nruntime classes: " + runtime.size() + " (api dev-only: " + api.size() + ")");
        if (!orphanExcludes.isEmpty())
            System.out.println("!! excluded from jar but NO fc replacement (NoClassDefFoundError): " + orphanExcludes);

        // ---- resolve every reference ----
        // key: kind|owner|name|desc  -> referencing classes
        Map<String, Set<String>> missing = new TreeMap<>();
        Map<String, Set<String>> inconclusive = new TreeMap<>();
        Map<String, Set<String>> apiRefs = new TreeMap<>();
        Set<String> missingClasses = new TreeSet<>();

        for (ClassInfo ci : runtime.values()) {
            for (String[] r : ci.refs) {
                String kind = r[0], owner = r[1], name = r[2], desc = r[3];
                if (owner.startsWith("[")) continue;
                boolean ours = owner.startsWith("btw/modern/") || owner.startsWith("net/minecraft/src/")
                        || owner.startsWith("btw/forge/") || owner.startsWith("btw/api/");
                if (!ours) continue;
                if (owner.startsWith("btw/api/")) {
                    apiRefs.computeIfAbsent(owner + " " + name + desc, k -> new TreeSet<>()).add(ci.name);
                    continue;
                }
                if (!runtime.containsKey(owner)) {
                    missingClasses.add(owner + "  (referenced by " + ci.name + ")");
                    continue;
                }
                int res = (kind.equals("F")) ? resolveField(owner, name, desc)
                        : name.equals("<init>") ? resolveCtor(owner, name, desc)
                        : resolveMethod(owner, name, desc);
                String key = kindLabel(kind, name) + " " + owner + "." + name + " " + desc
                        + "   [winner: " + runtime.get(owner).source + "]";
                if (res == MISSING) missing.computeIfAbsent(key, k -> new TreeSet<>()).add(ci.name);
                else if (res == INCONCLUSIVE) inconclusive.computeIfAbsent(key, k -> new TreeSet<>()).add(ci.name);
            }
        }

        System.out.println("\n== MISSING CLASSES (NoClassDefFoundError) ==");
        missingClasses.forEach(s -> System.out.println("  " + s));
        System.out.println("\n== MISSING MEMBERS (NoSuchMethodError / NoSuchFieldError) == count=" + missing.size());
        dump(missing);
        System.out.println("\n== REFS TO btw.api (absent from PROD jar; dev-only classpath) == count=" + apiRefs.size());
        dump(apiRefs);
        System.out.println("\n== INCONCLUSIVE (hierarchy leaves audited set) == count=" + inconclusive.size());
        dump(inconclusive);
    }

    static void dump(Map<String, Set<String>> map) {
        for (Map.Entry<String, Set<String>> e : map.entrySet()) {
            System.out.println("  " + e.getKey());
            int shown = 0;
            for (String c : e.getValue()) {
                if (shown++ == 5) { System.out.println("      ... +" + (e.getValue().size() - 5) + " more"); break; }
                System.out.println("      <- " + c);
            }
        }
    }

    static String kindLabel(String kind, String name) {
        return kind.equals("F") ? "FIELD " : name.equals("<init>") ? "CTOR  " : "METHOD";
    }

    static final int OK = 0, MISSING = 1, INCONCLUSIVE = 2;

    static int resolveCtor(String owner, String name, String desc) {
        ClassInfo ci = runtime.get(owner);
        return ci.methods.contains(name + desc) ? OK : MISSING;
    }

    static int resolveMethod(String owner, String name, String desc) { return walk(owner, name + desc, true); }
    static int resolveField(String owner, String name, String desc)  { return walk(owner, name + desc, false); }

    static int walk(String owner, String sig, boolean method) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        queue.add(owner);
        boolean external = false;
        while (!queue.isEmpty()) {
            String c = queue.poll();
            if (c == null || !seen.add(c)) continue;
            ClassInfo ci = runtime.get(c);
            if (ci == null) {
                if (c.equals("java/lang/Object")) { if (method && OBJECT_METHODS.contains(sig)) return OK; }
                else external = true;
                continue;
            }
            if (method ? ci.methods.contains(sig) : ci.fields.contains(sig)) return OK;
            if (ci.superName != null) queue.add(ci.superName);
            queue.addAll(ci.interfaces);
        }
        return external ? INCONCLUSIVE : MISSING;
    }

    // ---------------- class file parsing ----------------
    static void loadDir(String dir, String source, Map<String, ClassInfo> into, String prefixFilter) throws IOException {
        Path base = Paths.get(dir);
        if (!Files.isDirectory(base)) { System.out.println("!! missing dir: " + dir); return; }
        Files.walk(base).filter(p -> p.toString().endsWith(".class")).forEach(p -> {
            try {
                ClassInfo ci = parse(Files.readAllBytes(p));
                ci.source = source;
                into.put(ci.name, ci);
            } catch (Exception e) {
                System.out.println("!! parse error " + p + ": " + e);
            }
        });
    }

    static int u2(byte[] b, int o) { return ((b[o] & 0xff) << 8) | (b[o + 1] & 0xff); }
    static long u4(byte[] b, int o) { return ((long) u2(b, o) << 16) | u2(b, o + 2); }

    static ClassInfo parse(byte[] b) {
        int off = 8;
        int cpCount = u2(b, off); off += 2;
        Object[] cp = new Object[cpCount];
        for (int i = 1; i < cpCount; i++) {
            int tag = b[off++] & 0xff;
            switch (tag) {
                case 1: { int len = u2(b, off); off += 2;
                          cp[i] = new String(b, off, len, StandardCharsets.UTF_8); off += len; break; }
                case 7: case 8: case 16: case 19: case 20: cp[i] = (tag == 7) ? new int[]{u2(b, off)} : null; off += 2; break;
                case 15: off += 3; break;
                case 3: case 4: off += 4; break;
                case 5: case 6: off += 8; i++; break;
                case 9: case 10: case 11: cp[i] = new int[]{tag, u2(b, off), u2(b, off + 2)}; off += 4; break;
                case 12: cp[i] = new int[]{-1, u2(b, off), u2(b, off + 2)}; off += 4; break;
                case 17: case 18: off += 4; break;
                default: throw new RuntimeException("cp tag " + tag);
            }
        }
        ClassInfo ci = new ClassInfo();
        off += 2; // access
        ci.name = className(cp, u2(b, off)); off += 2;
        int sup = u2(b, off); off += 2;
        ci.superName = sup == 0 ? null : className(cp, sup);
        int nIf = u2(b, off); off += 2;
        for (int i = 0; i < nIf; i++) { ci.interfaces.add(className(cp, u2(b, off))); off += 2; }
        for (int pass = 0; pass < 2; pass++) { // fields then methods
            int n = u2(b, off); off += 2;
            for (int i = 0; i < n; i++) {
                String name = (String) cp[u2(b, off + 2)];
                String desc = (String) cp[u2(b, off + 4)];
                off += 6;
                int attrs = u2(b, off); off += 2;
                for (int a = 0; a < attrs; a++) { off += 2; long len = u4(b, off); off += 4 + (int) len; }
                if (pass == 0) ci.fields.add(name + desc); else ci.methods.add(name + desc);
            }
        }
        // collect refs from constant pool
        for (int i = 1; i < cpCount; i++) {
            if (cp[i] instanceof int[]) {
                int[] e = (int[]) cp[i];
                if (e.length == 3 && e[0] >= 9 && e[0] <= 11) {
                    String owner = className(cp, e[1]);
                    int[] nat = (int[]) cp[e[2]];
                    String name = (String) cp[nat[1]];
                    String desc = (String) cp[nat[2]];
                    ci.refs.add(new String[]{e[0] == 9 ? "F" : "M", owner, name, desc});
                }
            }
        }
        return ci;
    }

    static String className(Object[] cp, int idx) {
        int[] c = (int[]) cp[idx];
        return (String) cp[c[0]];
    }
}
