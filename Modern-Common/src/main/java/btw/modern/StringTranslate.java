package btw.modern;

import java.io.*;
import java.util.Properties;

public class StringTranslate {
    private Properties translateTable = new Properties();
    private static StringTranslate instance = new StringTranslate("en_US");

    public StringTranslate(String locale) {}

    public static StringTranslate getInstance() { return instance; }

    public String translateKey(String key) {
        return translateTable.getProperty(key, key);
    }

    public String translateKeyFormat(String key, Object... args) {
        String pattern = translateTable.getProperty(key, key);
        try {
            return String.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    public String translateNamedKey(String key) {
        return translateTable.getProperty(key, key);
    }

    public boolean containsTranslateKey(String key) {
        return translateTable.containsKey(key);
    }

    public Properties GetTranslateTable() { return translateTable; }

    public void LoadAddonLanguageExtension(String path) {
        loadLangFile(path);
    }

    /**
     * Loads a .lang file (key=value format, # comments) into the translate table.
     */
    public void loadLangFile(String path) {
        try (InputStream is = new FileInputStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    translateTable.setProperty(line.substring(0, eq), line.substring(eq + 1));
                }
            }
        } catch (Exception e) {
            // File not found or read error — silent
        }
    }

    /**
     * Loads a .lang file from an InputStream.
     */
    public void loadLangStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    translateTable.setProperty(line.substring(0, eq), line.substring(eq + 1));
                }
            }
        } catch (Exception e) {
            // Read error — silent
        }
    }
}
