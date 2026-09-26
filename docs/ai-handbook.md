# McDonald's Mod 总说明书（AI / 新开发者上手用）

> 读法：**先读本文**，需要做机器 / 电网相关的东西再读 [`docs/machine-guide.md`](machine-guide.md)。
> 目标：看完就知道"东西在哪、怎么加、怎么验证、哪些坑已经踩过"，不用把 80 多个 Java 文件翻一遍。
> 最后更新：2026-09-26（机器框架重构之后）。

---

## 0. 一分钟摘要

| 项目 | 值 |
|---|---|
| 游戏 / 加载器 | Minecraft **1.21.11** + Fabric Loader 0.18.4 |
| 映射 / Java | Yarn `1.21.11+build.4` / Java **21** |
| mod id / 包名 | `mcdonalds-mod` / `com.myachi.mcdonaldsmod` |
| 构建 | `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"; .\gradlew.bat build --console=plain` |
| 版本控制 | GitHub `Myachion/McDonalds-Mod`，分支 `master` |

**铁律（用户明确要求过的）**

1. **不要自己 commit / push** —— 用户说"commit+push"才做。
2. **所有数值参数（电压、电流、容量、耗电、掉落数量、生成密度…）由用户拍板**：
   先给方案 → 用户确认/修改 → 再落地。
3. **改完必须实测**（服务端 `data get`、客户端截图），不接受"理论上可以"。
4. **临时自检代码 / 临时世界 / `.verify-run` 用完必须清掉**（`rg SelfCheck src/` 必须是 0 处）。
5. 用户要的是**中文回复**，结论先行，附**实测数字和截图路径**。

---

## 1. 按任务找文件

| 我要做的事 | 去哪 |
|---|---|
| 加方块 / 物品 | `ModBlocks.java` / `ModItems.java`（+ `assets/.../items/<id>.json` 等资源） |
| 加方块实体 | `ModBlockEntities.java` |
| 加界面 | `ModScreenHandlers.java`（服务端容器）+ `client/` 下的 Screen + `McDonaldsModClient` 里 `HandledScreens.register` |
| 加**机器 / 用电设备** | 先读 [`machine-guide.md`](machine-guide.md)；基类在 `machine/` |
| 加矿石世界生成 | `worldgen/ModOreGeneration.java` + `data/mcdonalds-mod/worldgen/` + `tags/worldgen/biome/` |
| 加一种金属/材料（锭/板/粉/块/矿） | [`material-guide.md`](material-guide.md)：在 `material/ModMaterials.java` 写一条 + `runDatagen` |
| 加作物 | `crop/`（一次性作物继承 `SimpleCropBlock`）+ `ModBlocks` + 掉落表/模型/贴图 |
| 加电缆 | `ModBlocks`（`new CableBlock(电压V, 粗细, 电流A, 每格电阻mΩ, settings)`）+ `blockstates/*_cable.json` multipart 模型 |
| 电网逻辑（网络结算、损耗、烧毁） | `energy/`（`EnergyNetwork` = 结算，`EnergyNetworkManager` = 拓扑/烧毁） |
| 界面数值格式化 / 图标 | `machine/MachineNumbers.java` / `client/EnergyBoltIcon.java` / `client/MachineScreenStyle.java` |
| 材质（用户提供的参考图） | 仓库外 `resource/texture/...`（**不在 git 里**，只是素材暂存区） |

---

## 2. 现有内容速查

### 2.1 矿石

世界生成参数在 `worldgen/ModOreGeneration.java`，矿脉形态在 `worldgen/configured_feature/`，
密度在 `worldgen/placed_feature/`，生物群系分配在 `tags/worldgen/biome/`。

| 矿石 | size | 每区块次数 | 高度 | 深层变种 | 掉落 | 经验 |
|---|---|---|---|---|---|---|
| 盐 `salt_ore` | 9 | 分三档：密 16 / 中 10 / 疏 4 | 见标签 | 有 | 粗盐 3~5（时运加成，精准采集掉方块） | 0~1 |
| 锡 `tin_ore` | 9 | 12 | 0~112 均匀 | 无 | 粗锡 | 0~2 |
| 铅 `lead_ore` | 8 | 8 | -32~64 均匀 | 有 | 粗铅 | 0~2 |
| 铝 `aluminum_ore` | 9 | 6 | 0~96 均匀 | 无 | 粗铝 | 0~2 |
| 铀 `uranium_ore` | 4 | **2** | -64~16 梯形 | 有 | 粗铀 | 0~4 |
| 银 `silver_ore` | 7 | **3** | -48~48 梯形 | 有 | 粗银 | 0~3 |
| 下界铝 `nether_aluminum_ore` | 8 | 8 | 10~(顶-10) | — | 粗铝粒 4~7 | 0~2 |
| 下界银 `nether_silver_ore` | 6 | 4 | 10~(顶-10) | — | 粗银粒 4~7 | 0~3 |

盐矿的三档生物群系标签必须**互斥**，否则同一群系会叠密度。

### 2.2 材料

- 锭：精炼铁、锡、铅、铝、铀、青铜、银（钢还没加）。
- 粗矿：粗锡 / 粗铅 / 粗铝 / 粗铀 / 粗银；粗铝粒 / 粗银粒（9 粒 ↔ 1 粗矿，双向配方）。
- 板：精炼铁、铁、铜、青铜、金、青金石、铅、黑曜石、红石、银、锡、铝、钻石、铱
  （青金石 / 黑曜石 / 红石 / 钻石这四种**故意没有配方**）。
- 粉：24 种（铝、灰烬、青铜、碳、粘土、湿润碳、铜、钻石、绿宝石、末地石、金、铁、青金石、
  铅、锂、地狱岩、黑曜石、磷、下界石英、红色合金、二氧化硅、银、石、锡、煤炭）。
- 材料块：铝 / 青铜 / 木炭 / 铅 / 银 / 钢 / 锡 / 铀（9 锭 ↔ 1 块；钢块暂时没有配方；木炭块可当燃料）。

### 2.3 作物

| 作物 | 结构 | 收获 |
|---|---|---|
| 番茄 / 洋葱 | 一次性，agr 0~3，种在耕地上 | 成熟收获 2~3 个果实（时运影响）；未成熟破坏掉种子；成熟不掉种子 |
| 玉米 | 一次性，两格高（age≥2 起），生长快 30% | 成熟收获 1~2 个玉米，不掉种子 |
| 蓝莓丛 | 甜浆果式，种在泥土类方块上，可反复右键采摘 | 右键 2~3 个；空手破坏按阶段；剪刀 / 精准采集整丛 |

### 2.4 电缆（`new CableBlock(电压, 粗细, 电流A, 每格电阻mΩ, settings)`）

| 电缆 | 电压 | 电流 | 每格电阻 |
|---|---|---|---|
| 锡 | 32 V | 16 A | 0.05 Ω |
| 铜 | 128 V | 32 A | 0.02 Ω |
| 2x 铜（加粗，6px） | 128 V | 128 A | 0.01 Ω |
| 精炼铁 | 512 V | 32 A | 0.05 Ω |
| 金 | 512 V | 64 A | 0.02 Ω |
| 2x 金（加粗，6px） | 512 V | 256 A | 0.01 Ω |
| 铁 | 2048 V | 128 A | 0.04 Ω |
| 玻璃纤维 | 8192 V | 256 A | 0.005 Ω |

铝缆、超导缆还没做。电缆的物品提示（电压/电流/电阻）在 `ModTooltips`。
"x2"= 同一材质的加粗版（`CableBlock.THICK`，6 像素，和铁导线一样粗），载流量更大、电阻更低。

### 2.5 机器

见 [`machine-guide.md`](machine-guide.md) 第 4 节。现有三台：
电炉 `electric_furnace`、测试发电机 `test_generator`、测试电池盒 `test_battery_box`
（三台都已迁移到机器基类）。

---

## 3. 全局约定

### 3.1 单位（整个电网 / 机器体系）

- 电压 **V**、电流 **mA**、能量 **mJ**、功率 **mW**（= V × mA），全部用 `int` / `long`，禁止浮点存数值。
- 1 tick = 1/20 秒 → **每 tick 的能量 = 功率 ÷ 20**，用 `AbstractMachineBlockEntity.milliJoulesPerTick(mW)` 换算。
- 界面显示才做美化：`MachineNumbers.energy/current/power/ohms`。
- 超过 16 位的数（电量、实测电流）用 `LongPropertyCodec` 拆 3 个 15 位字段同步。

### 3.2 注册

- 方块：`ModBlocks.register(...)`（自动同时注册 BlockItem）；只想要方块不要物品用 `registerBlockOnly(...)`（作物用）。
- 物品：`ModItems.register(...)`；方块物品的键是 `block.mcdonalds-mod.<id>`。
- 创造栏顺序在 `ModItemGroups`（新东西记得加进去，否则游戏里找不到）。
- 电缆接线登记：继承 `MachineBlock` / `AbstractMachineBlock` 会自动登记"六面可接"，
  其它方块要在 `ModBlocks.initializeModBlocks()` 里 `CableConnections.always/only/custom`，
  或者写进数据包标签 `mcdonalds-mod:cable_connectable`。

### 3.3 资源

- **1.21.4+ 每个方块物品都必须在 `assets/<ns>/items/<id>.json` 里有条目**，否则紫黑块。
- 模型 / 掉落表 / 配方 / 世界生成**目前全是手写 JSON**（datagen provider 基本是空的，别指望 `runDatagen`）。
- 透明贴图（作物、细杆电缆）要 `BlockRenderLayerMap.putBlocks(BlockRenderLayer.CUTOUT, ...)`，
  否则透明像素会被填成实心面板（这条踩过两次）。
- 语言文件 `zh_cn.json` / `en_us.json` 两份都要加。

### 3.4 存档

- 机器基类用的键名固定：`stored_energy` / `rated_input_voltage` / `rated_input_current`（**mA**）/
  `rated_output_voltage` / `rated_output_current`（**mA**）。
- 改键名 = 老存档机器数值清零，慎改。电炉自己的键：`cook_ticks`、`pending_experience_milli`；
  测试发电机自己的键：`generation_voltage`、`generation_current`。
- `TestBatteryBoxBlockEntity.readData` 里有一段"旧存档电流单位是 A"的兼容代码，删之前先确认没有老存档。

---

## 4. 常见任务怎么做

### 4.1 加一个"方块 + 物品"

1. `ModBlocks` 加字段（选好 `MapColor` / 硬度 / 音效，材料块用 `metalBlockSettings(...)`）。
2. `ModItemGroups` 加一行。
3. 资源：`blockstates/<id>.json`、`models/block/<id>.json`、`models/item/<id>.json`、
   `items/<id>.json`、`textures/block/<id>.png`、`data/.../loot_table/blocks/<id>.json`、
   两份 lang。

### 4.2 加一个材料块

照抄 `tin_block`：方块 + `loot_table`（掉自己）+ `recipe/crafting/<id>_block.json`（9 锭 → 1 块）
与 `<id>_ingot_from_block.json`（1 块 → 9 锭）+ `models/block`、`models/item`、`items`、贴图、lang。

### 4.3 加一种矿石（清单最长，按顺序来）

1. **方块**：`ModBlocks` 里注册（石头用 `oreSettings()`，深板岩用 `deepslateOreSettings()`，
   下界用 `netherOreSettings()`；经验用 `new ExperienceDroppingBlock(UniformIntProvider.create(min,max), settings)`）。
2. **物品**：粗矿 / 粒 / 锭（`ModItems` + `models/item` + `items` + 贴图 + lang）。
3. **掉落表**：`data/mcdonalds-mod/loot_table/blocks/<ore>.json`，参考 `tin_ore.json`
   （精准采集掉方块分支 + `set_count` + `apply_bonus` 时运 + `explosion_decay`）。
4. **配方**：熔炉 / 高炉烧粗矿 → 锭；粒 ↔ 粗矿双向。
5. **模型**：`blockstates`、`models/block`、`items`、`textures/block`。
6. **世界生成**：`worldgen/configured_feature/<ore>.json`（长什么样）+ `placed_feature/<ore>.json`
   （多密、多高、什么形状）+ `ModOreGeneration` 里加 `RegistryKey` 并 `BiomeModifications.addFeature(...)`。
7. **生物群系**（可选）：需要分档就加 `tags/worldgen/biome/<ore>_*.json`（**各档必须互斥**）。
8. `ModItemGroups` + 两份 lang。

### 4.4 加一种作物

- **一次性、4 阶段、种耕地**：继承 `crop/SimpleCropBlock`（公共逻辑都在里面），
  子类只写 `CODEC` + `getSeedsItem()`，参考 `TomatoCropBlock` / `OnionCropBlock`。
  然后：`ModBlocks` 用 `registerBlockOnly(...)`、种子物品在 `ModItems` 用 `BlockItem`、
  模型 4 个阶段、掉落表（未成熟掉种子 / 成熟掉果实，参考 `tomato_crop.json`）、
  `BlockRenderLayerMap.putBlocks(CUTOUT, ...)`。
- **两格高**：参考 `CornCropBlock`（照原版瓶子草的路子，`TallPlantBlock` + 自己控制上半格）。
- **丛生反复收获 / 种在泥土上**：参考 `BlueberryBushBlock`（照原版甜浆果）。

### 4.5 加一种电缆

1. `ModBlocks`：`register("<id>_cable", settings -> new CableBlock(电压, CableBlock.THIN/THICK, 电流A, 每格电阻mΩ, settings), cableSettings(MapColor.X))`。
   **同时必须把新方块加进 `data/mcdonalds-mod/tags/block/cable.json`** —— 电网靠这个标签判断"这是不是电缆"，
   漏了的话方块能放、能显示，但完全不入网（表现为放下去"没电"）。
2. 资源：`blockstates/<id>_cable.json`（multipart：core + 六个臂）、`models/block/cable/<id>_cable_core.json`、
   `models/block/cable/<id>_cable_arm.json`、`models/item`、`items`、`textures/block`（需要 CUTOUT）、
   `data/.../loot_table/blocks/<id>_cable.json`。
3. `McDonaldsModClient` 的 CUTOUT 列表 + `ModItemGroups` + lang（物品提示会自动带电压/电流/电阻）。

### 4.6 加一台机器

**看 [`machine-guide.md`](machine-guide.md)** —— 那里有完整的 7 步模板、标准属性表、
按钮式界面基类、验证流程。三句话版本：方块继承 `MachineBlock`（对称机器继承 `AbstractMachineBlock`）、
方块实体继承 `AbstractMachineBlockEntity`、界面继承 `AbstractMachineScreenHandler`（有物品槽就学电炉复用原版容器）。

---

## 5. 开发与验证流程（PowerShell 实测可用）

### 5.1 构建

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat build --console=plain
```

### 5.2 服务端验证（方块逻辑 / 电网 / 配方 / 世界生成）

```powershell
# 1) 建 .verify-run，放 eula.txt(eula=true) 和 server.properties
#    （pause-when-empty-seconds=0、online-mode=false）
# 2) 启动：注意必须用 JDK21 的完整路径；系统 PATH 里的 java 是 8
$cp = (Get-Content "<项目>\build\loom-cache\argFiles\runServer")[1]
& "C:\Program Files\Java\jdk-21\bin\java.exe" `
  "-Dfabric.dli.config=<项目>\.gradle\loom-cache\launch.cfg" `
  "-Dfabric.dli.env=server" `
  "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer" `
  -cp $cp net.fabricmc.devlaunchinjector.Main nogui --port 25599
# 3) 控制台里 forceload + setblock + data get block ...
```

坑：

- `java "@argfile"` 这种写法在本机**不生效**，要像上面那样把 argfile 里的 classpath 读出来传 `-cp`。
- `/setblock` 的机器/电缆**会**触发 `onBlockAdded`（电缆登记、邻居连接都正常）。
- **y=-60 附近有岩浆湖**，测试平台要铺地板或者用 y=100 的空中平台，否则玩家会被烧死。
- 机器可以 `data merge block <pos> {rated_input_voltage:128, rated_input_current:20000}` 直接改参数
  （电流是 **mA**）。
- **机器不是导体**：电缆不能"穿过"机器把两张网连起来，机器是网络的终端。
- 电池盒**只有上下面能充电**，四个侧面是输出口 —— 测试时别把进线和出线接在同一个面。

### 5.3 客户端验证（界面 / 贴图 / 粒子）

1. 把服务端世界目录 robocopy 到 `run/saves/<世界名>`（`/XF session.lock`）。
2. 在 `McDonaldsModClient` 里临时加触发（例如读系统属性 `mcdonalds.selfcheck`），
   写一个临时自检类：`ClientTickEvents.END_CLIENT_TICK` 里按 tick 调度
   `client.getServer().execute(...)` 搭场景 → `ServerPlayerEntity#openHandledScreen` 或模拟右键 →
   `ScreenshotRecorder.saveScreenshot(client.runDirectory, "名字.png", client.getFramebuffer(), 1, cb)`。
3. 启动客户端（`run` 作为工作目录，classpath 同样从 `runClient` argfile 读）：
   `--quickPlaySingleplayer <世界名> --width 854 --height 480`。
4. `view_image` 看 `run/screenshots/名字.png`，并让自检把关键数值 `LOGGER.info` 出来做双重证据。
5. **验证完删掉自检类和触发代码**（`rg SelfCheck src/`），并清理临时世界。

### 5.4 清理

- 本环境**不允许递归删除**（`Remove-Item -Recurse` 会被策略拒绝）。
  临时目录（`.verify-run`、`run/saves/selfcheck_world`）用**移动**到 `%TEMP%` 的方式清理：
  `Move-Item -LiteralPath <临时目录> -Destination "$env:TEMP\<名字>"`。
  `run/` 本身在 `.gitignore` 里；`.verify-run` 不在，所以必须清掉，否则 `git status` 会看到它。

---

## 6. 已知待办（用户提过但还没做的）

- 铝质线缆、超导线缆；锡/精炼铁/铁/玻璃纤维的"x2 加粗版"（铜 x2、金 x2 已做）。
- 电炉的合成配方；青金石 / 黑曜石 / 红石 / 钻石板没有获取途径。
- 大部分粉还没有用途（锂、磷、二氧化硅、红色合金等）。
- 万用表还可以扩展（现在的模式是"整张网 / 单根电缆"）。
- 电阻/电损已有开关和实现，但还没在游戏里细调数值；载流量过载烧毁是 3 秒。
- 资源还是手写 JSON，未来可以考虑迁到 datagen（`datagen/` 里的 provider 基本是空壳）。
- 未来机器（比如更多用电设备）要按"缓冲区模式 + 额定输入输出 + 端口方向"设计，
  参考 `machine-guide.md` 第 2 节。
