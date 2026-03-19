package btw.modern;

import java.util.Properties;

public class StringTranslate {
    private Properties translateTable = new Properties();
    private static StringTranslate instance = new StringTranslate("en_US");

    public StringTranslate(String locale) {}

    public static StringTranslate getInstance() { return instance; }

    public String translateKey(String key) { return key; }
    public String translateKeyFormat(String key, Object... args) { return key; }
    public String translateNamedKey(String key) { return key; }
    public boolean containsTranslateKey(String key) { return false; }

    public Properties GetTranslateTable() { return translateTable; }
    public void LoadAddonLanguageExtension(String path) {}
}
