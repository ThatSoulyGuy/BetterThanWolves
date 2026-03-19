package btw.modern;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry with a default value. Mirrors net.minecraft.src.RegistryDefaulted.
 */
public class RegistryDefaulted implements IRegistry {

    private Map map = new HashMap();
    private Object defaultObject;

    public RegistryDefaulted(Object defaultObj) {
        this.defaultObject = defaultObj;
    }

    public void putObject(Object key, Object value) {
        map.put(key, value);
    }

    public Object getObject(Object key) {
        Object result = map.get(key);
        return result != null ? result : defaultObject;
    }

    public Object func_82594_a(Object key) {
        return getObject(key);
    }
}
