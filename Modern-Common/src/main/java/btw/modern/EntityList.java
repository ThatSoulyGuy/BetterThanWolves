package btw.modern;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.5.2 EntityList (vanilla/server/net/minecraft/src/EntityList.java) — the
 * name/id/class lookup tables. Populated live by
 * FCBetterThanWolves.CreateModEntityMappings via AddMapping /
 * ReplaceExistingMapping; read live by the frozen EntityAgeable.interact
 * (spawn-egg baby spawning calls getClassFromID).
 */
public class EntityList {
    /** Provides a mapping between entity classes and a string */
    public static Map stringToClassMapping = new HashMap();

    /** Provides a mapping between a string and an entity classes */
    public static Map classToStringMapping = new HashMap();

    /** provides a mapping between an entityID and an Entity Class */
    public static Map IDtoClassMapping = new HashMap();

    /** provides a mapping between an Entity Class and an entity ID */
    public static Map classToIDMapping = new HashMap();

    /** Maps entity names to their numeric identifiers */
    public static Map stringToIDMapping = new HashMap();

    /**
     * This is a HashMap of the Creative Entity Eggs/Spawners. Egg-info values
     * are not populated here (EntityEggInfo has no shim; the modern engine
     * owns creative spawn eggs).
     */
    public static HashMap entityEggs = new LinkedHashMap();

    // 1.5.2 EntityList.addMapping (public via FCMOD change) — populates the
    // lookup maps; called live by FC's CreateModEntityMappings.
    public static void addMapping(Class clazz, String name, int id) {
        stringToClassMapping.put(name, clazz);
        classToStringMapping.put(clazz, name);
        IDtoClassMapping.put(Integer.valueOf(id), clazz);
        classToIDMapping.put(clazz, Integer.valueOf(id));
        stringToIDMapping.put(name, Integer.valueOf(id));
    }

    // 1.5.2 EntityList.createEntityByName — reflective construction from the
    // string mapping.
    public static Entity createEntityByName(String name, World world) {
        Entity entity = null;

        try {
            Class clazz = (Class) stringToClassMapping.get(name);

            if (clazz != null) {
                entity = (Entity) clazz.getConstructor(new Class[] {World.class}).newInstance(new Object[] {world});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return entity;
    }

    // 1.5.2 EntityList.createEntityFromNBT — includes the legacy "Minecart"
    // id remap; the getWorldLogAgent "Skipping Entity" warning is dropped
    // (no log-agent shim).
    public static Entity createEntityFromNBT(NBTTagCompound tag, World world) {
        Entity entity = null;

        if ("Minecart".equals(tag.getString("id"))) {
            switch (tag.getInteger("Type")) {
                case 0:
                    tag.setString("id", "MinecartRideable");
                    break;

                case 1:
                    tag.setString("id", "MinecartChest");
                    break;

                case 2:
                    tag.setString("id", "MinecartFurnace");
            }

            tag.removeTag("Type");
        }

        try {
            Class clazz = (Class) stringToClassMapping.get(tag.getString("id"));

            if (clazz != null) {
                entity = (Entity) clazz.getConstructor(new Class[] {World.class}).newInstance(new Object[] {world});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (entity != null) {
            entity.readFromNBT(tag);
        }

        return entity;
    }

    // 1.5.2 EntityList.createEntityByID.
    public static Entity createEntityByID(int id, World world) {
        Entity entity = null;

        try {
            Class clazz = getClassFromID(id);

            if (clazz != null) {
                entity = (Entity) clazz.getConstructor(new Class[] {World.class}).newInstance(new Object[] {world});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return entity;
    }

    // 1.5.2 EntityList.getEntityID.
    public static int getEntityID(Entity entity) {
        Class clazz = entity.getClass();
        return classToIDMapping.containsKey(clazz) ? ((Integer) classToIDMapping.get(clazz)).intValue() : 0;
    }

    // 1.5.2 EntityList.getClassFromID — live caller: frozen
    // EntityAgeable.interact (vanilla/server EntityAgeable.java:51,
    // spawn-egg right-click baby spawning).
    public static Class getClassFromID(int id) {
        return (Class) IDtoClassMapping.get(Integer.valueOf(id));
    }

    // 1.5.2 EntityList.getEntityString.
    public static String getEntityString(Entity entity) {
        return (String) classToStringMapping.get(entity.getClass());
    }

    // 1.5.2 EntityList.getStringFromID.
    public static String getStringFromID(int id) {
        Class clazz = getClassFromID(id);
        return clazz != null ? (String) classToStringMapping.get(clazz) : null;
    }

    /**
     * Registers a vanilla mapping by frozen class name. The vanilla static
     * block references vanilla entity classes directly, but not all of them
     * are mirrored as Modern-Common sources — at runtime every entry resolves
     * against the frozen 1.5.2 classes (btw.modern.*) on the game classpath.
     * When Modern-Common is compiled/run standalone, absent classes are
     * simply skipped.
     */
    private static void addMapping(String simpleName, String name, int id) {
        try {
            addMapping(Class.forName("btw.modern." + simpleName), name, id);
        } catch (Throwable e) {
            // Catch Throwable, not just ClassNotFoundException: a class that is PRESENT but
            // fails to link (e.g. it implements/references a missing type -> NoClassDefFoundError)
            // must SKIP its mapping, never abort EntityList.<clinit> — a failed <clinit> makes
            // EntityList permanently uninitializable and breaks the entire entity-id registry
            // (spawn eggs, entity-string lookups, every downstream FC entity mapping).
        }
    }

    // 1.5.2 EntityList static init — the vanilla name/id table. Required so
    // FC's ReplaceExistingMapping("Pig", "Cow", ...) calls can find the
    // vanilla ids to replace; egg color entries are omitted (see entityEggs).
    static {
        addMapping("EntityItem", "Item", 1);
        addMapping("EntityXPOrb", "XPOrb", 2);
        addMapping("EntityPainting", "Painting", 9);
        addMapping("EntityArrow", "Arrow", 10);
        addMapping("EntitySnowball", "Snowball", 11);
        addMapping("EntityLargeFireball", "Fireball", 12);
        addMapping("EntitySmallFireball", "SmallFireball", 13);
        addMapping("EntityEnderPearl", "ThrownEnderpearl", 14);
        addMapping("EntityEnderEye", "EyeOfEnderSignal", 15);
        addMapping("EntityPotion", "ThrownPotion", 16);
        addMapping("EntityExpBottle", "ThrownExpBottle", 17);
        addMapping("EntityItemFrame", "ItemFrame", 18);
        addMapping("EntityWitherSkull", "WitherSkull", 19);
        addMapping("EntityTNTPrimed", "PrimedTnt", 20);
        addMapping("EntityFallingSand", "FallingSand", 21);
        addMapping("EntityFireworkRocket", "FireworksRocketEntity", 22);
        addMapping("EntityBoat", "Boat", 41);
        addMapping("EntityMinecartEmpty", "MinecartRideable", 42);
        addMapping("EntityMinecartChest", "MinecartChest", 43);
        addMapping("EntityMinecartFurnace", "MinecartFurnace", 44);
        addMapping("EntityMinecartTNT", "MinecartTNT", 45);
        addMapping("EntityMinecartHopper", "MinecartHopper", 46);
        addMapping("EntityMinecartMobSpawner", "MinecartSpawner", 47);
        addMapping("EntityLiving", "Mob", 48);
        addMapping("EntityMob", "Monster", 49);
        addMapping("EntityCreeper", "Creeper", 50);
        addMapping("EntitySkeleton", "Skeleton", 51);
        addMapping("EntitySpider", "Spider", 52);
        addMapping("EntityGiantZombie", "Giant", 53);
        addMapping("EntityZombie", "Zombie", 54);
        addMapping("EntitySlime", "Slime", 55);
        addMapping("EntityGhast", "Ghast", 56);
        addMapping("EntityPigZombie", "PigZombie", 57);
        addMapping("EntityEnderman", "Enderman", 58);
        addMapping("EntityCaveSpider", "CaveSpider", 59);
        addMapping("EntitySilverfish", "Silverfish", 60);
        addMapping("EntityBlaze", "Blaze", 61);
        addMapping("EntityMagmaCube", "LavaSlime", 62);
        addMapping("EntityDragon", "EnderDragon", 63);
        addMapping("EntityWither", "WitherBoss", 64);
        addMapping("EntityBat", "Bat", 65);
        addMapping("EntityWitch", "Witch", 66);
        addMapping("EntityPig", "Pig", 90);
        addMapping("EntitySheep", "Sheep", 91);
        addMapping("EntityCow", "Cow", 92);
        addMapping("EntityChicken", "Chicken", 93);
        addMapping("EntitySquid", "Squid", 94);
        addMapping("EntityWolf", "Wolf", 95);
        addMapping("EntityMooshroom", "MushroomCow", 96);
        addMapping("EntitySnowman", "SnowMan", 97);
        addMapping("EntityOcelot", "Ozelot", 98);
        addMapping("EntityIronGolem", "VillagerGolem", 99);
        addMapping("EntityVillager", "Villager", 120);
        addMapping("EntityEnderCrystal", "EnderCrystal", 200);
    }

    // FCMOD: Added
    public static void AddMapping(Class entityClass, String sName, int iID) {
        addMapping(entityClass, sName, iID);
    }

    public static boolean RemoveMapping(String sName, boolean bRemoveEgg) {
        Integer iID = (Integer) stringToIDMapping.get(sName);

        if (iID != null) {
            Class mappedClass = (Class) IDtoClassMapping.get(Integer.valueOf(iID));

            if (mappedClass != null) {
                stringToClassMapping.remove(sName);
                classToStringMapping.remove(mappedClass);
                IDtoClassMapping.remove(iID);
                classToIDMapping.remove(mappedClass);
                stringToIDMapping.remove(sName);

                if (bRemoveEgg) {
                    // may or may not have an egg, but this is a safe operation

                    entityEggs.remove(iID);
                }

                return true;
            }
        }

        return false;
    }

    public static boolean ReplaceExistingMapping(Class newClass, String sName) {
        Integer iID = (Integer) stringToIDMapping.get(sName);

        if (iID != null) {
            if (RemoveMapping(sName, false)) // egg mapping intentionally left in place
            {
                addMapping(newClass, sName, iID);
            }
        }

        return false;
    }
    // END FCMOD
}
