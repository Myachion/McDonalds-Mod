# 材料族（MaterialFamily）使用说明

> 给"下一次要加一种金属/材料的人 / AI"看：加一种材料只要在
> `material/ModMaterials.java` 里写一条定义 + 跑一次数据生成，Java 注册、模型、
> 方块状态、物品定义、掉落表、配方都会自动出来。
> 最后更新：2026-09-26。

---

## 0. 一分钟摘要

```java
// material/ModMaterials.java
public static final MaterialFamily NICKEL = MaterialFamily.builder("nickel")
        .mapColor(MapColor.LIGHT_GRAY)
        .name("镍", "Nickel")            // 只用于 datagen 的"缺语言条目"提示
        .ingot().plate().dust().block().raw().ore(0, 2)
        .build();
```

然后：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat runDatagen --console=plain
.\gradlew.bat build --console=plain
```

一条定义会自动得到：`nickel_ingot` / `nickel_plate` / `nickel_dust` / `nickel_block` /
`raw_nickel` / `nickel_ore` 六个物品（方块自动带 BlockItem），以及

| 自动生成的资源 | 路径 |
|---|---|
| 方块状态 | `src/main/generated/assets/mcdonalds-mod/blockstates/<id>.json` |
| 方块模型（六面同贴图） | `src/main/generated/assets/mcdonalds-mod/models/block/<id>.json` |
| 物品模型 / 物品定义 | `.../models/item/<id>.json` + `.../items/<id>.json` |
| 掉落表 | `src/main/generated/data/mcdonalds-mod/loot_table/blocks/<id>.json` |
| 配方 | `src/main/generated/data/mcdonalds-mod/recipe/{crafting,smelting,blasting}/…` |

**贴图和语言不生成**：

- 贴图仍然要自己放：`textures/item/<id>.png`、`textures/block/<id>.png`（矿石/材料块）；
- 语言条目由 datagen 检查并在日志里列出缺哪条（例如 `item.mcdonalds-mod.nickel_ingot`），
  照着往 `lang/zh_cn.json` + `lang/en_us.json` 补（故意不自动写，避免覆盖手写语言文件）。

矿石的**世界生成**（密度、高度、生物群系）也不生成：那是用户拍板的数值，
仍然写在 `worldgen/ModOreGeneration.java` + `data/mcdonalds-mod/worldgen/`。

---

## 1. 部件与自动配方

| 部件 | 写法 | 生成的 id | 默认配方 |
|---|---|---|---|
| 锭 | `.ingot()` | `<材料>_ingot` | — |
| 粒 | `.nugget()` | `<材料>_nugget` | 1 锭 ↔ 9 粒 |
| 板 | `.plate()` | `<材料>_plate` | 精炼铁锤 + 1 锭 → 2 板 |
| 粉 | `.dust()` | `<材料>_dust` | 研钵标签 + 1 锭 → 1 粉 |
| 粗矿 | `.raw()` | `raw_<材料>` | 熔炉/高炉 → 1 锭（0.7 经验） |
| 粗矿粒 | `.rawNugget()` | `raw_<材料>_nugget` | 1 粗矿 ↔ 9 粗矿粒 |
| 材料块 | `.block()` | `<材料>_block` | 9 锭 ↔ 1 块 |
| 石头矿 | `.ore(xpMin, xpMax)` | `<材料>_ore` | 掉落表：精准采集掉方块，否则 1 粗矿 + 时运 |
| 深层矿 | `.deepslateOre(...)` | `deepslate_<材料>_ore` | 同上 |
| 下界矿 | `.netherOre(...)` | `nether_<材料>_ore` | 掉落表：精准采集掉方块，否则 4~7 粗矿粒 + 时运 |

矿石方块是 `ExperienceDroppingBlock`，挖掉会掉经验，范围就是括号里那两个数。

---

## 2. 反例（不标准的材料）怎么写

| 情况 | 写法 | 例子 |
|---|---|---|
| 没有某个部件 | 不调用对应的 `.xxx()` | 铀没有板：`ModMaterials.URANIUM` |
| 有部件但没有配方 | `.blockRecipe(false)` / `.plateRecipe(false)` / `.dustRecipe(false)` | 钢块：`STEEL` 只有 `.block().blockRecipe(false)` |
| 锭不是自己的（用原版锭） | `.ingotFrom(Items.COPPER_INGOT)` | 铜/金/铁板那种（还没迁，见下） |
| 粉的来源不是自己的锭 | `.dustFrom(Items.LAPIS_LAZULI)` | 青金石粉那种 |
| 粉能烧回锭 | `.dustSmelting(true)` | **青铜**（目前唯一有这条配方的） |
| 粗矿不能烧 | `.rawSmelting(false)` | 暂时没有例子 |
| 某个部件的资源还是手写的 | `.handWritten(Part.ORE, Part.DEEPSLATE_ORE)` | 迁移一半的族 |
| 整族资源都还是手写的 | `.noGeneratedAssets()` | 铅/铝/银/青铜/铀/钢块（见下） |

`.noGeneratedAssets()` 的作用：datagen 跳过这个族，**绝不覆盖手写文件**；
所以"迁移中"的族可以先把 Java 注册统一进来，资源以后再迁。

---

## 3. 迁移一族资源（把 `noGeneratedAssets()` 去掉）

1. 删掉（或先备份）这一族在 `src/main/resources` 里的手写文件，例如锡族要删：

   ```
   assets/mcdonalds-mod/blockstates/tin_block.json, tin_ore.json
   assets/mcdonalds-mod/models/block/tin_block.json, tin_ore.json
   assets/mcdonalds-mod/models/item/tin_ingot.json, tin_plate.json, tin_dust.json, raw_tin.json
   assets/mcdonalds-mod/items/tin_*.json, raw_tin.json
   data/mcdonalds-mod/loot_table/blocks/tin_block.json, tin_ore.json
   data/mcdonalds-mod/recipe/{crafting,smelting,blasting}/tin_*.json
   ```

   > ⚠️ **同一个路径不能同时存在于 `src/main/resources` 和 `src/main/generated`**，
   > Gradle 9 的 `processResources` 会直接报
   > `Entry ... is a duplicate but no duplicate handling strategy has been set` 并构建失败。

2. 把定义里的 `.noGeneratedAssets()` 去掉。
3. `.\gradlew.bat runDatagen`，然后**对比生成结果**再提交：

   ```python
   # 语义比对（忽略 key 顺序和 1 vs 1.0）：stdout 里 equal 的数量应该等于文件数
   import json, os
   backup, gen = r"<备份目录>", r"src\main\generated"
   for root, _, files in os.walk(backup):
       for f in files:
           rel = os.path.relpath(os.path.join(root, f), backup)
           a = json.load(open(os.path.join(root, f), encoding="utf-8"))
           b = json.load(open(os.path.join(gen, rel), encoding="utf-8"))
           print("OK " if a == b else "DIFF", rel)
   ```

4. 已知的可接受差异（都是语义等价的写法差异）：
   - 方块模型 `"parent": "block/cube_all"` ↔ `"minecraft:block/cube_all"`；
   - 熔炼配方结果多一个默认的 `"count": 1`；
   - 生成文件的 key 顺序是排序过的，手写文件是随意的。

5. 语言条目：datagen 日志会列出缺的键；当前锡族的条目已经齐了。

---

## 4. 当前迁移状态

| 材料族 | Java 注册 | 资源 |
|---|---|---|
| 锡 `tin` | ✅ 材料族 | ✅ datagen 生成（模型/掉落/配方） |
| 铅 `lead` | ✅ 材料族 | ⏳ 手写（`noGeneratedAssets()`） |
| 铝 `aluminum` | ✅ 材料族 | ⏳ 手写 |
| 银 `silver` | ✅ 材料族 | ⏳ 手写 |
| 青铜 `bronze` | ✅ 材料族 | ⏳ 手写 |
| 铀 `uranium` | ✅ 材料族 | ⏳ 手写 |
| 钢 `steel`（只有块） | ✅ 材料族 | ⏳ 手写 |

还没进材料族的（属于"和材料族形状不同"的那批，需要时再抽象）：
精炼铁（带锤子/剪子工具）、原版锭派生的铜/金/铁板与粉、
青金石/黑曜石/红石/钻石板（没有配方）、各种非金属粉、盐。

---

## 5. 代码地图

| 文件 | 作用 |
|---|---|
| `material/MaterialFamily.java` | 材料族本体：构建器、部件枚举、配方计划、注册逻辑 |
| `material/ModMaterials.java` | **所有材料族的定义表**（加材料只改这里） |
| `datagen/MaterialAssetsProvider.java` | 资源生成器（模型/方块状态/物品定义/掉落/配方 + 语言检查） |
| `ModBlocks.metalBlockSettings/oreSettings/deepslateOreSettings/netherOreSettings` | 材料块 / 各类矿石的方块属性（材料族复用，保证和原版手感一致） |
