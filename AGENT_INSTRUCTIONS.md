# AGENT TASK: Decouple FC code from MCP

## YOUR ASSIGNMENT
You are assigned a specific package (given in your prompt). Transform every `.java` file in that package so it compiles against `Api/` ONLY — zero references to `net.minecraft.src.*`.

## ARCHITECTURE
```
Api/        — interfaces, zero dependencies
MCP/        — implements Api, contains vanilla MC (YOU DO NOT TOUCH THIS)
Common/     — depends on Api ONLY
Client/     — depends on Api + Common ONLY
Server/     — depends on Api + Common ONLY
```

## RULES — READ EVERY ONE

1. **NEVER modify files outside your assigned package**
2. **NEVER modify MCP/, Adapter/, or vanilla/ files**
3. **NEVER add `net.minecraft.src` imports to FC files** — if you need a type, create an interface in Api/
4. **You MAY create or expand interfaces in `Api/src/main/java/btw/api/`**
5. **You MAY modify FC files in your assigned package**
6. **Every interface method parameter and return type must be another interface type, a primitive, String, or java.util type — NEVER a concrete MC class**

## WHAT TO DO FOR EACH FC FILE

### Step 1: Read the file
Note every reference to a `net.minecraft.src` class.

### Step 2: For each MC type reference, ensure an interface exists
- `World` → `IWorld` in `btw/api/world/`
- `Block` → `IBlock` in `btw/api/block/`
- `Entity` → `IEntity` in `btw/api/entity/`
- `ItemStack` → `IItemStack` in `btw/api/item/`
- etc.
- If the interface doesn't exist, CREATE IT in Api/
- If it exists but is missing a method the FC code calls, ADD the method

### Step 3: Ensure every method the FC code calls is declared on the interface
Example: FC code calls `world.getBlockId(i, j, k)`
- Check `IWorld.java` for `int getBlockId(int x, int y, int z);`
- If missing, add it
- If the method has MC-typed params like `Entity`, use `IEntity` instead
- If the method returns an MC type like `TileEntity`, return `ITileEntity`

### Step 4: Change the FC file
- Replace `import net.minecraft.src.*;` with specific Api imports
- Replace type declarations: `World world` → `IWorld world`
- Replace return types: `Block getBlock()` → `IBlock getBlock()`
- Replace field types: `private World myWorld;` → `private IWorld myWorld;`
- Replace instanceof: `instanceof World` → `instanceof IWorld`
- Replace casts: `(World) obj` → `(IWorld) obj`
- **DO NOT change `extends Abstract*`** — those stay as-is
- **DO NOT change `super.*` calls** — those stay as-is

### Step 5: Handle method bodies that call vanilla methods
When FC code inside a `_btw` method calls a method on an interface-typed variable:
```java
// world is IWorld, getBlockId is on IWorld — works
int id = world.getBlockId(i, j, k);
```
If the method is NOT on the interface, ADD IT to the interface.

### Step 6: Handle `this.field` and `super.method()`
These access inherited members from Abstract* classes (which extend vanilla).
- `this.blockID` → stays as-is (inherited from AbstractBlock → Block)
- `super.updateTick(...)` → stays as-is
- These work because the FC class extends Abstract* which extends vanilla

## INTERFACE DESIGN RULES

When creating or expanding an interface:

```java
package btw.api.world;

public interface IWorld {
    // Parameters: use interface types, primitives, String, java.util only
    // Return types: same rule

    int getBlockId(int x, int y, int z);           // OK: primitives
    IBlock getBlock(int x, int y, int z);           // OK: interface return
    void spawnEntityInWorld(IEntity entity);         // OK: interface param
    boolean setBlock(int x, int y, int z, int id);  // OK: primitives

    // WRONG — never use concrete MC types:
    // Block getBlock(int x, int y, int z);          // NO
    // void spawnEntityInWorld(Entity entity);        // NO
    // TileEntity getTileEntity(int x, int y, int z); // NO
}
```

If a vanilla method returns `List<Entity>`, use `java.util.List` (raw type) and return `java.util.List`.

If a vanilla method takes `IBlockAccess`, create `IBlockAccess` interface in Api if it doesn't exist.

## WHAT SUCCESS LOOKS LIKE

After you finish, every FC file in your package:
1. Has ZERO imports from `net.minecraft.src`
2. Uses ONLY `btw.api.*` and `btw.adapter.*` types (plus java.* stdlib)
3. Compiles when `net.minecraft.src` is NOT on the classpath

## VERIFICATION

After modifying files, mentally verify:
- No `net.minecraft.src` imports remain in your FC files
- Every type reference is to an interface (I-prefixed) or Abstract* class
- Every interface you created/modified has zero MC dependencies
- Method signatures in interfaces use only interfaces, primitives, String, java.util types
