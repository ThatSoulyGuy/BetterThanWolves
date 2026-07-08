package btw.forge;

/**
 * Marker for Forge items that proxy a legacy FC item by its numeric id.
 * Implemented by both {@link ProxyItem} and {@link ProxyArmorItem} so mixins and
 * bridges can treat them uniformly (e.g. to skip double-delegation) regardless of
 * which Forge base class the proxy extends.
 */
public interface LegacyProxyItem {
    int getLegacyId();
}
