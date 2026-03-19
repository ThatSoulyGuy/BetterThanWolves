package btw.modern;

import java.util.Map;

public class EntityList {
    public static Map stringToClassMapping;
    public static Map classToStringMapping;
    public static Map IDtoClassMapping;

    public static void addMapping(Class clazz, String name, int id) {}
    public static Entity createEntityByName(String name, World world) { return null; }
    public static Entity createEntityByID(int id, World world) { return null; }
    public static Entity createEntityFromNBT(NBTTagCompound tag, World world) { return null; }
    public static int getEntityID(Entity entity) { return 0; }
    public static String getEntityString(Entity entity) { return null; }
    public static String getStringFromID(int id) { return null; }

    public static void AddMapping(Class clazz, String name, int id) {
        addMapping(clazz, name, id);
    }

    public static boolean ReplaceExistingMapping(Class clazz, String name) { return false; }
}
