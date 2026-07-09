import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Build-time static audit for the BTW Forge port — catches the "compiles fine, links fine,
 * but silently wrong at runtime" bug classes that LinkAudit (member/class resolution) can't:
 *
 *   1. SOUND COVERAGE   — FC code plays a sound name that SoundMapping doesn't map -> silent.
 *   2. AUX-FX COVERAGE  — FC code fires a custom aux-FX id (>=2222) that WorldBridge's
 *                         playFcAuxSFX switch doesn't handle -> silent (e.g. the ghast
 *                         scream/moan 2225/2228).
 *   3. TEXTURE EXISTENCE— an entity renderer's loadTexture() path, or an FC item's icon
 *                         texture, resolves (via the FCEntityRenderer rules) to a resource
 *                         file that isn't shipped -> placeholder/invisible (e.g. the soul urn).
 *
 * Pure static: scans source + resources, no game load. Run: javac -d out tools/BridgeAudit.java
 * && java -cp out BridgeAudit. Exit code 0 = clean, 1 = high-severity findings.
 *
 * NOTE: the texture resolution here mirrors FCEntityRenderer.resolveEntityTexture — keep the
 * two in sync if that method changes.
 */
public class BridgeAudit {
    static final String ROOT = "D:/IntelliJ/IDEA/Projects/BetterThanWolves";
    static final String[] FC_SRC = {ROOT + "/Common/src", ROOT + "/Client/src", ROOT + "/Server/src"};
    static final String MODERN = ROOT + "/Modern-Common/src/main/java/btw/modern";
    static final String CLIENT = ROOT + "/Client/src/main/java/net/minecraft/src/btw/client";
    static final String TEX_BASE = ROOT + "/Forge/src/main/resources/assets/betterthanwolves/textures";
    static final String SOUND_MAPPING = ROOT + "/Forge/src/main/java/btw/forge/SoundMapping.java";
    static final String WORLD_BRIDGE = ROOT + "/Forge/src/main/java/btw/forge/WorldBridge.java";
    static final String FC_CORE = ROOT + "/Client/src/main/java/net/minecraft/src/btw/core/FCBetterThanWolves.java";

    static int highSeverity = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("=== BTW Bridge Audit ===\n");
        auditSounds();
        auditAuxFx();
        auditTextures();
        System.out.println("\n=== Summary: " + highSeverity + " high-severity finding(s) ===");
        System.exit(highSeverity > 0 ? 1 : 0);
    }

    // ---- helpers ----

    static String read(String path) {
        try { return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8); }
        catch (IOException e) { return ""; }
    }

    static List<String> javaFiles(String dir) {
        List<String> out = new ArrayList<>();
        Path base = Paths.get(dir);
        if (!Files.isDirectory(base)) return out;
        try (Stream<Path> s = Files.walk(base)) {
            s.filter(p -> p.toString().endsWith(".java")).forEach(p -> out.add(p.toString()));
        } catch (IOException ignored) {}
        return out;
    }

    // ============================================================ 1. SOUNDS
    static void auditSounds() {
        Set<String> mapped = new HashSet<>();
        Matcher m = Pattern.compile("SOUND_MAP\\.put\\(\\s*\"([^\"]+)\"").matcher(read(SOUND_MAPPING));
        while (m.find()) mapped.add(m.group(1));

        // FC sound names look like "<category>.<...>" with a known category prefix.
        Pattern soundLit = Pattern.compile(
            "\"((?:random|dig|step|mob|fire|liquid|portal|ambient|note|damage|tile)\\.[^\"]*|fcwhitesmoke)\"");
        // Map name -> where first seen (for actionable output)
        Map<String, String> used = new TreeMap<>();
        for (String dir : FC_SRC) {
            for (String f : javaFiles(dir)) {
                String src = read(f);
                Matcher sm = soundLit.matcher(src);
                while (sm.find()) {
                    String name = sm.group(1);
                    // Skip "note."/"tile." etc. — a trailing dot means it's a concatenation
                    // prefix ("note." + instrument), not a literal sound name.
                    if (name.endsWith(".")) continue;
                    used.putIfAbsent(name, shortPath(f));
                }
            }
        }

        List<String> unmapped = new ArrayList<>();
        for (Map.Entry<String, String> e : used.entrySet()) {
            if (!mapped.contains(e.getKey())) unmapped.add(e.getKey() + "   (first used: " + e.getValue() + ")");
        }
        System.out.println("[1] SOUND COVERAGE: " + used.size() + " sound names used, "
            + mapped.size() + " mapped, " + unmapped.size() + " UNMAPPED (would be silent):");
        for (String u : unmapped) System.out.println("      " + u);
        // Unmapped direct sounds are medium severity (some are intentionally-rare FX).
        if (unmapped.isEmpty()) System.out.println("      (all direct sounds mapped)");
        System.out.println();
    }

    // ============================================================ 2. AUX-FX
    static void auditAuxFx() {
        // constant name -> numeric id
        Map<String, Integer> idOf = new HashMap<>();
        Matcher cm = Pattern.compile("(m_i[A-Za-z0-9]+AuxFXID)\\s*=\\s*(\\d+)").matcher(read(FC_CORE));
        while (cm.find()) idOf.put(cm.group(1), Integer.parseInt(cm.group(2)));

        // handled cases in WorldBridge.playFcAuxSFX (numeric case labels >= 2222)
        Set<Integer> handled = new HashSet<>();
        Matcher hm = Pattern.compile("case\\s+(\\d+)\\s*:").matcher(read(WORLD_BRIDGE));
        while (hm.find()) { int v = Integer.parseInt(hm.group(1)); if (v >= 2222) handled.add(v); }

        // which aux-FX constants are actually fired via playAuxSFX(... m_iXxxAuxFXID ...)
        Map<Integer, String> firedButUnhandled = new TreeMap<>();
        Pattern call = Pattern.compile("playAuxSFX[A-Za-z]*\\([^;]*?(m_i[A-Za-z0-9]+AuxFXID)");
        for (String dir : FC_SRC) {
            for (String f : javaFiles(dir)) {
                Matcher call_m = call.matcher(read(f));
                while (call_m.find()) {
                    Integer id = idOf.get(call_m.group(1));
                    if (id != null && id >= 2222 && !handled.contains(id)) {
                        firedButUnhandled.putIfAbsent(id, call_m.group(1) + "   (e.g. " + shortPath(f) + ")");
                    }
                }
            }
        }
        System.out.println("[2] AUX-FX COVERAGE: " + idOf.size() + " aux-FX ids defined, "
            + handled.size() + " handled in playFcAuxSFX, " + firedButUnhandled.size()
            + " FIRED-but-UNHANDLED (would be silent):");
        for (Map.Entry<Integer, String> e : firedButUnhandled.entrySet())
            System.out.println("      " + e.getKey() + " = " + e.getValue());
        if (firedButUnhandled.isEmpty()) System.out.println("      (all fired aux-FX ids handled)");
        System.out.println();
    }

    // ============================================================ 3. TEXTURES
    static void auditTextures() {
        // (a) renderer loadTexture() paths — HIGH severity (whole entity mis-textured)
        Set<String> loadPaths = new TreeSet<>();
        Map<String, String> loadWhere = new HashMap<>();
        Pattern lt = Pattern.compile("loadTexture\\(\\s*\"([^\"]+)\"");
        for (String f : javaFiles(MODERN)) collectLoad(f, lt, loadPaths, loadWhere);
        for (String f : javaFiles(CLIENT)) collectLoad(f, lt, loadPaths, loadWhere);

        List<String> missingRenderer = new ArrayList<>();
        for (String p : loadPaths) {
            // /gui/items.png and /terrain.png are item/block ATLAS paths — the renderer
            // overrides them per-quad with the icon's own texture name, so skip them here.
            if (p.equals("/gui/items.png") || p.equals("/terrain.png")) continue;
            String res = resolveTexture(p);
            if (res != null && !Files.exists(Paths.get(TEX_BASE, res))) {
                missingRenderer.add(p + " -> textures/" + res + "   (" + loadWhere.get(p) + ")");
            }
        }
        System.out.println("[3a] RENDERER TEXTURES: " + loadPaths.size() + " loadTexture paths, "
            + missingRenderer.size() + " MISSING (entity renders with placeholder):");
        for (String s : missingRenderer) { System.out.println("      " + s); highSeverity++; }
        if (missingRenderer.isEmpty()) System.out.println("      (all renderer textures resolve)");

        // (b) FC item icon textures — MEDIUM (item shows a placeholder icon). Heuristic:
        //     iconString defaults to setUnlocalizedName(); check textures/item/<lower>.png.
        Pattern nameP = Pattern.compile("setUnlocalizedName\\(\\s*\"(fcItem[A-Za-z0-9]+)\"");
        Pattern texOverride = Pattern.compile("(?:func_111206_d|setTextureName)\\(\\s*\"([^\"]+)\"");
        Map<String, String> missingItem = new TreeMap<>();
        String itemDir = ROOT + "/Common/src/main/java/net/minecraft/src/btw/item";
        for (String f : javaFiles(itemDir)) {
            String src = read(f);
            String icon = null;
            Matcher om = texOverride.matcher(src);
            if (om.find()) icon = om.group(1);
            else { Matcher nm = nameP.matcher(src); if (nm.find()) icon = nm.group(1); }
            if (icon == null) continue;
            String name = icon.toLowerCase(Locale.ROOT);
            if (!Files.exists(Paths.get(TEX_BASE, "item", name + ".png")))
                missingItem.putIfAbsent(name, shortPath(f));
        }
        System.out.println("\n[3b] ITEM ICON TEXTURES: " + missingItem.size()
            + " FC item icon(s) with no textures/item/<name>.png (placeholder inventory icon;"
            + " some may set a non-default icon name):");
        int shown = 0;
        for (Map.Entry<String, String> e : missingItem.entrySet()) {
            if (shown++ < 25) System.out.println("      " + e.getKey() + ".png   (" + e.getValue() + ")");
        }
        if (missingItem.size() > 25) System.out.println("      ... +" + (missingItem.size() - 25) + " more");
        if (missingItem.isEmpty()) System.out.println("      (all item icon textures present)");
        System.out.println();
    }

    static void collectLoad(String f, Pattern lt, Set<String> paths, Map<String, String> where) {
        Matcher m = lt.matcher(read(f));
        while (m.find()) { paths.add(m.group(1)); where.putIfAbsent(m.group(1), shortPath(f)); }
    }

    /** Mirrors FCEntityRenderer.resolveEntityTexture. Returns the path under textures/ (no leading slash). */
    static String resolveTexture(String fcPath) {
        String clean = fcPath.startsWith("/") ? fcPath.substring(1) : fcPath;
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith("btwmodtex/")) return "entity/" + lower.substring("btwmodtex/".length());
        if (!lower.contains("/") && !lower.endsWith(".png")) {
            String subdir = lower.startsWith("fcblock") ? "block/" : "item/";
            return subdir + lower + ".png";
        }
        return lower;
    }

    static String shortPath(String f) {
        int i = f.replace('\\', '/').indexOf("/src/");
        String rel = i >= 0 ? f.replace('\\', '/').substring(f.replace('\\', '/').lastIndexOf('/', i - 1) + 1) : f;
        return rel;
    }
}
