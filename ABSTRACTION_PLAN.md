# Plan: Abstract BTW from MCP — Interface Layer

## Goal

Replace every direct MCP (vanilla Minecraft) reference in BTW code with a reference to
an abstraction interface. The MCP classes then *implement* those interfaces, allowing BTW
to be compiled and potentially run against any Minecraft implementation (MCP, Mojmap,
Fabric intermediary, etc.) that provides the same interface contracts.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  BTW Mod Code (FC classes)                      │
│  Only references: btw-api interfaces            │
├─────────────────────────────────────────────────┤
│  btw-api module (NEW)                           │
│  Interfaces: IBlock, IItem, IWorld, IEntity...  │
│  No implementation — pure contracts             │
├─────────────────────────────────────────────────┤
│  btw-mcp-adapter module (NEW)                   │
│  MCP classes implement btw-api interfaces       │
│  Block implements IBlock, Item implements IItem  │
│  Thin wrappers / mixins / extends               │
└─────────────────────────────────────────────────┘
```

**Dependency direction:**
- `btw-api` depends on nothing
- `btw-mod` (Common/Client/Server) depends on `btw-api` only
- `btw-mcp-adapter` depends on `btw-api` + MCP vanilla classes
- At runtime: `btw-mcp-adapter` + `btw-mod` + vanilla MC

---

## Phase 1: Define the Interface Layer (`btw-api`)

### 1.1 Core Type Interfaces

These replace the most-referenced vanilla classes. Every FC class that currently
`extends Block` would instead `extends IBlock`, and the MCP `Block` class would
`implements IBlock`.

Based on the API surface catalog, these interfaces are needed:

#### Tier 1 — Highest usage (referenced by 100+ FC classes)

| Interface    | Replaces MCP class | Key members to abstract |
|---|---|---|
| `IBlock`     | `Block`            | blocksList[], setHardness, setResistance, setStepSound, setLightValue, blockIcon, blockHardness, blockResistance, getItemDropped, damageDropped, onBlockActivated, updateTick, breakBlock, getRenderType, getCollisionBoundingBoxFromPool, isBlockSolidOnSide, onEntityCollidedWithBlock, getExplosionResistance, shouldSideBeRendered, dropBlockAsItem_do, CanFallIntoBlockAtPos, OnFallingUpdate, ScheduleCheckForFall, SetBuoyant, SetPicksEffectiveOn, SetFireProperties, SetFurnaceBurnTime, etc. |
| `IItem`      | `Item`             | itemsList[], Item(int), setPotionEffect, itemIcon, SetBuoyant, SetBellowsBlowDistance, SetIncineratedInCrucible, SetFilterableProperties, m_iFilterable_*, etc. |
| `IWorld`     | `World`            | worldInfo, getBlockId, setBlock, setBlockMetadataWithNotify, getBlockMetadata, playSound, isBlockNormalCube, getBlockMaterial, notifyBlockChange, scheduleBlockUpdate, etc. |
| `IEntity`    | `Entity`           | rand, inWater, posX/Y/Z, motionX/Y/Z, boundingBox, dealFireDamage, setBeenAttacked, onUpdate, readFromNBT, writeToNBT, setSize |
| `IEntityLiving` | `EntityLiving`  | health, moveSpeed, isLivingDead, jump, getMaxHealth, onDeath, applyEntityCollision |
| `IItemStack` | `ItemStack`        | itemID, stackSize, getItemDamage, getItem, getMaxStackSize |

#### Tier 2 — Medium usage (20-100 references)

| Interface | Replaces | Key members |
|---|---|---|
| `IEntityPlayer` | `EntityPlayer` | foodStats, inventory, IsWearingSoulforgedBoots, capabilities |
| `IEntityCreature` | `EntityCreature` | entityToAttack, IsPossessed, IsFullyPossessed |
| `IEntityAnimal` | `EntityAnimal` | m_iGrazeProgressCounter, GetTicksForChildToGrow |
| `IMaterial` | `Material` | setRequiresTool, setBurning, setNoPushMobility, setTranslucent |
| `ITileEntity` | `TileEntity` | worldObj, readFromNBT, writeToNBT, updateEntity |
| `IContainer` | `Container` | slotClick, detectAndSendChanges |
| `IAxisAlignedBB` | `AxisAlignedBB` | minX/Y/Z, maxX/Y/Z, setBounds, RotateAroundJToFacing, TiltToFacingAlongJ |
| `IDamageSource` | `DamageSource` | setDamageBypassesArmor |
| `IPotion` | `Potion` | Potion(int,boolean,int), setIconIndex |
| `ICraftingManager` | `CraftingManager` | addRecipe, AddShapelessRecipe, AddShapedRecipeWithCustomClass |
| `IBiomeGenBase` | `BiomeGenBase` | temperature, rainfall, decorator |
| `IVec3` | `Vec3` | xCoord/yCoord/zCoord, setComponents |

#### Tier 3 — Lower usage but still needed

| Interface | Replaces |
|---|---|
| `IBlockContainer` | `BlockContainer` |
| `IEntityItem` | `EntityItem` |
| `IEntityFallingSand` | `EntityFallingSand` |
| `IStepSound` | `StepSound` |
| `ICreativeTabs` | `CreativeTabs` |
| `IEnchantment` | `Enchantment` |
| `IFurnaceRecipes` | `FurnaceRecipes` |
| `IRenderBlocks` | `RenderBlocks` |
| `IPacket` | `Packet` |
| `IWorldGenerator` | `WorldGenerator` |
| `IEntityAIBase` | `EntityAIBase` |
| `INBTTagCompound` | `NBTTagCompound` |
| `IInventory` | (already vanilla interface — keep as-is) |
| `IRecipe` | (already vanilla interface — keep as-is) |

### 1.2 Enum/Constant Interfaces

| Interface | Replaces |
|---|---|
| `IEnumAction` | `EnumAction` (eat, drink, bow, block, miscUse) |
| `IEnumCreatureType` | `EnumCreatureType` |
| `IEnumToolMaterial` | `EnumToolMaterial` |
| `IEnumArmorMaterial` | `EnumArmorMaterial` |

### 1.3 Registry Interfaces

| Interface | Purpose |
|---|---|
| `IBlockRegistry` | Replaces `Block.blocksList[]` static array |
| `IItemRegistry` | Replaces `Item.itemsList[]` static array |
| `IEntityRegistry` | Replaces `EntityList.AddMapping()` |
| `IDispenserRegistry` | Replaces `BlockDispenser.dispenseBehaviorRegistry` |
| `IRecipeRegistry` | Replaces `CraftingManager` singleton |
| `IPotionRegistry` | Replaces `Potion` static instances |

### 1.4 Client-Only Interfaces (Client module only)

| Interface | Replaces |
|---|---|
| `IMinecraft` | `Minecraft` (thePlayer, theWorld, renderEngine, etc.) |
| `IGuiContainer` | `GuiContainer` |
| `IRender` | `Render` |
| `IRenderLiving` | `RenderLiving` |
| `IEntityFX` | `EntityFX` (particleScale) |
| `IModelBase` | `ModelBase` |
| `IModelRenderer` | `ModelRenderer` |
| `ITileEntityRenderer` | `TileEntitySpecialRenderer` |
| `ITextureStitched` | `TextureStitched` |
| `ISoundManager` | `SoundManager` |

### 1.5 Networking Interfaces

| Interface | Replaces |
|---|---|
| `INetServerHandler` | `NetServerHandler` |
| `IPacket250CustomPayload` | `Packet250CustomPayload` |

---

## Phase 2: Create the MCP Adapter (`btw-mcp-adapter`)

### 2.1 Strategy: Extend-and-Implement

For each vanilla class, the adapter creates a subclass (or modifies via patch) that
implements the corresponding interface:

```java
// In btw-mcp-adapter
public class MCPBlock extends Block implements IBlock {
    // Block already has all the methods — IBlock just declares them
    // No new code needed if IBlock mirrors Block's API exactly
}
```

For classes that BTW patches (adds methods to), the adapter includes those additions:

```java
public class MCPBlock extends Block implements IBlock {
    // BTW-added methods (currently in patch.txt)
    public Block SetBuoyant() { return SetBuoyancy(1F); }
    public Block SetPicksEffectiveOn() { ... }
    public Block SetFireProperties(FCEnumFlammability f) { ... }
    // etc.
}
```

### 2.2 Strategy: Wrapper for Final/Static classes

Some vanilla classes can't be subclassed (final, static utility, etc.). For these,
the adapter wraps them:

```java
public class MCPWorldAdapter implements IWorld {
    private final World world;
    public MCPWorldAdapter(World world) { this.world = world; }
    public int getBlockId(int x, int y, int z) { return world.getBlockId(x, y, z); }
    // delegate all methods
}
```

### 2.3 Registry Adapters

```java
public class MCPBlockRegistry implements IBlockRegistry {
    public IBlock getBlock(int id) { return (IBlock) Block.blocksList[id]; }
    public void register(int id, IBlock block) { Block.blocksList[id] = (Block) block; }
}
```

### 2.4 Patched Vanilla Classes

The 276 patched vanilla files currently contain inline BTW modifications. These
modifications move into the adapter module:

- **New methods** added to vanilla classes → implemented in adapter subclasses
- **Changed method signatures** → adapter provides the new signatures
- **Changed field access** → adapter exposes fields via interface getters/setters
- **Changed class hierarchies** (e.g., `AxisAlignedBB extends FCUtilsPrimitiveGeometric`)
  → adapter handles via composition or interface

---

## Phase 3: Migrate BTW Code to Use Interfaces

### 3.1 Change `extends` to use interface-aware base classes

| Current | New |
|---|---|
| `class FCBlockAnvil extends Block` | `class FCBlockAnvil extends IBlock` |
| `class FCItemSword extends ItemSword` | `class FCItemSword extends IItemSword` |
| `class FCEntityWolf extends EntityWolf` | `class FCEntityWolf extends IEntityWolf` |

**Challenge:** Java interfaces can't have constructors or fields. Two solutions:

**Option A: Abstract base classes in btw-api**
```java
// btw-api
public abstract class AbstractBlock implements IBlock {
    // Common field storage, constructor logic
    // Delegates to IBlock methods
}
```

**Option B: Keep concrete base classes but reference via interface types**
```java
// FC code uses IBlock for parameter/return types
// but still extends the concrete adapter class for inheritance
class FCBlockAnvil extends MCPBlock { // MCPBlock implements IBlock
    void doSomething(IWorld world, IBlock block) { // interface types in signatures
        ...
    }
}
```

**Recommendation: Option B** — least disruptive. FC classes extend adapter classes
(which implement the interfaces), and all parameter/return types change to interface
types. This preserves Java inheritance while abstracting the API.

### 3.2 Change field/method references

| Current BTW code | After abstraction |
|---|---|
| `Block.blocksList[id]` | `blockRegistry.getBlock(id)` |
| `Block.stone` | `Blocks.STONE` (interface constant holder) |
| `new ItemStack(Block.planks, 1, 0)` | `ItemStacks.of(Blocks.PLANKS, 1, 0)` |
| `world.getBlockId(x, y, z)` | `world.getBlockId(x, y, z)` (same if IWorld mirrors World) |
| `entity.rand` | `entity.getRandom()` |
| `block.blockHardness` | `block.getHardness()` |
| `CreativeTabs.tabBlock` | `CreativeTabs.BLOCK` |

### 3.3 Change import statements

All `import net.minecraft.src.*` → `import btw.api.*`

---

## Phase 4: Execution Order

### Step 1: Create `btw-api` module
- Define all interfaces from Phase 1
- No implementation code
- ~50 interface files, ~2000 method signatures

### Step 2: Create `btw-mcp-adapter` module
- For each interface, create MCP implementation
- Move patched vanilla code (from patch.txt) into adapter classes
- Verify adapter compiles against vanilla + btw-api

### Step 3: Migrate Common module
- Change all 373 FC files to use interface types
- Update imports
- Verify Common compiles against btw-api only (not vanilla directly)

### Step 4: Migrate Client module
- Change all 378 FC files
- Add client-specific interfaces (rendering, GUI)
- Verify Client compiles against btw-api + Common

### Step 5: Migrate Server module
- Change all 319 FC files
- Verify Server compiles against btw-api + Common

### Step 6: Integration test
- Assemble: btw-mcp-adapter + btw-api + btw-mod (Common+Client+Server) + vanilla MC
- Verify runtime behavior matches pre-migration

---

## Scope Estimate

| Component | Files | Estimated effort |
|---|---|---|
| btw-api interfaces | ~50 new files | Define ~2000 method signatures |
| btw-mcp-adapter | ~100 new files | Wrap/extend 100 vanilla classes, absorb 276 patches |
| Common migration | 373 files | Change imports + type references |
| Client migration | 378 files | Change imports + type references + rendering interfaces |
| Server migration | 319 files | Change imports + type references |
| **Total** | **~1220 files touched** | |

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Java can't extend interfaces — FC classes need concrete parents | High | Use abstract base classes in btw-api, or extend adapter classes (Option B) |
| Static field access (Block.blocksList, Item.itemsList) can't be interfaced | High | Registry objects injected via DI or static accessor class |
| BTW patches change vanilla method signatures (new parameters) | High | Adapter provides both old and new signatures; interface uses the BTW signature |
| 276 patched vanilla files contain complex logic changes | High | Patches move into adapter module as method overrides |
| `instanceof` checks against vanilla classes | Medium | Change to interface checks: `instanceof IBlock` |
| Reflection usage (CodecMus) | Low | Adapter handles reflection internally |
| Inner classes / anonymous classes referencing vanilla types | Medium | Migrate inner class types too |

---

## What This Enables

Once complete, BTW's mod code references **zero** MCP-specific classes directly.
This means:

1. **Portability** — BTW could theoretically run on any MC 1.5.2 implementation
   (or modern versions) that provides the same interfaces
2. **Testability** — FC classes can be unit tested with mock implementations
3. **Clean separation** — Mod logic is clearly separated from MC internals
4. **Future-proofing** — If Mojang mappings or intermediary mappings change,
   only the adapter module needs updating
