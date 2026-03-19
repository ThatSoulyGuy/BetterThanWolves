package btw.modern;

public class StatCollector {

    public static String translateToLocal(String key) {
        return StringTranslate.getInstance().translateKey(key);
    }

    public static String translateToLocalFormatted(String key, Object... args) {
        return StringTranslate.getInstance().translateKeyFormat(key, args);
    }

    public static boolean canTranslate(String key) {
        return StringTranslate.getInstance().containsTranslateKey(key);
    }

    public static boolean func_94522_b(String key) {
        return StringTranslate.getInstance().containsTranslateKey(key);
    }
}
