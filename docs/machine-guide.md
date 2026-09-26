# 机器（用电方块）开发指南

> 给"下一次要加机器的人 / AI"看的说明书：看完这一页就能直接动手，不用把全部代码翻一遍。
> 现在已按这套体系落地的机器：**电炉**（`ElectricFurnace*`）。

---

## 0. 一分钟摘要（先看这里）

- 版本：Minecraft **1.21.11** + Fabric，Yarn 映射，Java 21；mod id `mcdonalds-mod`，包 `com.myachi.mcdonaldsmod`。
- 单位全部用整数：**V / mA / mJ / mW**，功率 = V × mA；1 tick = 1/20 秒，
  所以 **每 tick 的能量（mJ）= 功率（mW） ÷ 20**
  （用 `AbstractMachineBlockEntity.milliJoulesPerTick(96000)` 直接换算 96 W → 4800 mJ/tick）。
- 一台机器 = **方块** + **方块实体** + **界面（可选）**，通用部分已经在基类里：

| 类 | 作用 |
|---|---|
| `machine/MachineBlock` | 朝向 `facing`、工作状态 `lit`（激活贴图）、六面接电缆、右键开界面、每 tick 转发 |
| `machine/AbstractMachineBlockEntity` | 缓冲区、额定输入/输出、实测读数、电网登记、存档 |
| `machine/MachineInventory` | 通用物品栏（固定格数，改动自动 `markDirty`） |
| `client/EnergyBoltIcon` | 界面里的缓冲区"闪电"图标 + 悬停提示 |

- **最省事的做法**：整台电炉抄一遍改名 ——
  `machine/ElectricFurnaceBlock` / `machine/ElectricFurnaceBlockEntity` /
  `machine/ElectricFurnaceScreenHandler` / `client/ElectricFurnaceScreen`。

---

## 1. 文件地图

```
src/main/java/com/myachi/mcdonaldsmod/
├── ModBlocks.java                 方块注册（含电缆参数表）
├── ModBlockEntities.java          方块实体类型注册
├── ModScreenHandlers.java         界面类型注册（每个界面一个常量 + 标题语言键）
├── ModItemGroups.java             创造模式物品栏顺序
├── machine/
│   ├── MachineBlock.java                ★机器方块基类
│   ├── AbstractMachineBlockEntity.java  ★机器方块实体基类（缓冲区/额定值/电网）
│   ├── MachineInventory.java            ★通用物品栏
│   ├── ElectricFurnaceBlock.java        参考实现：方块
│   ├── ElectricFurnaceBlockEntity.java  参考实现：方块实体（烧制逻辑）
│   ├── ElectricFurnaceScreenHandler.java 参考实现：界面容器
│   ├── MachineNumbers.java              界面数字格式化（kJ / A / W …）
│   ├── EnergyLevels.java                电压电流档位表（电池盒/发电机调参用）
│   ├── LongPropertyCodec.java           超 16 位的数值拆字段同步用
│   ├── TestGeneratorBlockEntity.java    早期原型：发电机（未迁到基类）
│   └── TestBatteryBoxBlockEntity.java   早期原型：电池盒（未迁到基类）
├── energy/
│   ├── EnergyStorage.java          机器与电网之间的接口（额定值、收发、端口方向）
│   ├── EnergyNetwork.java          单张电网的结算（电压、按功率分电、逐段电流、损耗）
│   ├── EnergyNetworkManager.java   连接关系重建 / 过载烧毁计时
│   ├── CableBlock.java             电缆方块（额定电压、电流、电阻）
│   └── CableConnections.java       机器接入电缆的登记表
└── client/
    ├── ElectricFurnaceScreen.java  参考实现：复用原版熔炉布局 + 换图标
    ├── EnergyBoltIcon.java         ★缓冲区闪电图标 + 悬停提示
    ├── MachineScreen.java          自定义界面的 +/- 按钮基类
    └── TestGeneratorScreen.java    自定义界面的参考（按钮 + 能量条）

src/main/resources/
├── assets/mcdonalds-mod/{blockstates,models/block,models/item,items,textures,lang}
└── data/mcdonalds-mod/{loot_table/blocks,recipe/{crafting,smelting,blasting},tags}
```

世界生成（矿石）在 `data/mcdonalds-mod/worldgen/` + `worldgen/ModOreGeneration.java`，机器用不到。

---

## 2. 电网约定（用户拍板的规则，写代码前先认这些）

1. **网络电压** = 网内所有电源里最高的额定输出电压。
2. **负载实际输入电压** = `min(网络电压, 自己的额定输入电压)`；高于额定暂不烧机（以后可能加）。
3. 可用功率不够时，按各负载的**额定功率比例**分配。
4. 电源侧按各自可用功率比例扣缓冲区（并额外承担线路损耗）。
5. **逐段电流**用多源 BFS 生成树把负载电流回推到电源：每根电缆的电流 = 它下游所有负载之和。
6. 过流**或**过压持续 3 秒 → 电缆烧毁（无掉落）；线损 = I²R。两者各有开关，见 `energy/EnergyConfig`。
7. **端口方向**由 `EnergyStorage#canProvideEnergyFrom(side)` / `#canReceiveEnergyOn(side)` 决定：
   普通用电器六面都能充；发电机只有输出面；电池盒只有上下能充、侧面只输出。
8. **缓冲区模式**：外界只往缓冲区灌电，机器自己按耗电从缓冲区取；缓冲区不够就暂停工作（别让机器"反复断电重启"）。

---

## 3. 加一台新机器：7 步

### 步骤 1 — 方块类（继承 `MachineBlock`）

```java
public class MyMachineBlock extends MachineBlock {
    public static final MapCodec<MyMachineBlock> CODEC = createCodec(MyMachineBlock::new);
    public MyMachineBlock(AbstractBlock.Settings settings) { super(settings); }
    @Override public MapCodec<MyMachineBlock> getCodec() { return CODEC; }
    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new MyMachineBlockEntity(pos, state); }
}
```

`MachineBlock` 已经处理：朝向放置者、`lit` 属性、六面接电缆（构造时自动登记）、
右键开界面（方块实体实现 `NamedScreenHandlerFactory` 即可）、每 tick 转发给方块实体。

### 步骤 2 — 方块实体（继承 `AbstractMachineBlockEntity`）

```java
public class MyMachineBlockEntity extends AbstractMachineBlockEntity {
    /** 额定 128 V / 2 A，缓冲区 8 kJ；干活时耗电 100 W。 */
    private static final long CAPACITY = 8_000_000L;
    private static final long DRAW_PER_TICK = milliJoulesPerTick(100_000L);

    private final MachineInventory inventory = new MachineInventory(this, 2);

    public MyMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_MACHINE, pos, state, CAPACITY, 128, 2_000); // 电压 V、电流 mA
    }

    @Override
    protected void tickServer(ServerWorld world) {
        boolean working = false;
        if (/* 有活可干 */ true && consumeEnergy(DRAW_PER_TICK)) {
            working = true;
            // …每 tick 的加工逻辑
        }
        setActive(working);   // 正面换激活贴图
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);                       // 缓冲区 + 额定值（基类管）
        Inventories.readData(view, this.inventory.getStacks());
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory.getStacks());
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MyMachineScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() { return Text.translatable("block.mcdonalds-mod.my_machine"); }
}
```

要点：

- 缓冲区和额定值不用自己写：`consumeEnergy(mJ)` / `insertEnergy` / `extractEnergy` / `getStoredEnergyMilliJoules` 都在基类；
- 要给界面显示"实测读数"时用 `getMeasuredInputVoltage()` 之类的 getter（基类已接好 `setMeasuredInput/Output`）；
- 额定值可调的机器（像电池盒）用 `setRatedInput(v, mA)` / `setRatedOutput(v, mA)`。

### 步骤 3 — 注册

1. `ModBlocks`：加一个字段，设置参照现有机器（`.requiresTool().strength(3.0F, 3.0F).sounds(BlockSoundGroup.IRON)`）；
   电缆连接不用写，`MachineBlock` 构造器里自动登记（要限制端口才需要 `CableConnections.custom(...)`）。
2. `ModBlockEntities`：`FabricBlockEntityTypeBuilder.create(MyMachineBlockEntity::new, ModBlocks.MY_MACHINE).build()`。
3. `ModScreenHandlers`：`new ScreenHandlerType<>(MyMachineScreenHandler::new, FeatureFlags.VANILLA_FEATURES)` + 标题语言键。
4. `ModItemGroups`：把方块加进创造物品栏。

### 步骤 4 — 资源文件

| 文件 | 内容 |
|---|---|
| `assets/mcdonalds-mod/blockstates/my_machine.json` | 4 朝向 × `lit=false/true`，`lit=true` 用 `_on` 模型 |
| `assets/mcdonalds-mod/models/block/my_machine.json` | `minecraft:block/orientable_with_bottom` + front/side/top/bottom 贴图 |
| `assets/mcdonalds-mod/models/block/my_machine_on.json` | 同上，`front` 换激活贴图 |
| `assets/mcdonalds-mod/items/my_machine.json` | 物品模型指向方块模型（**1.21.4+ 必需**，不会自动回退） |
| `data/mcdonalds-mod/loot_table/blocks/my_machine.json` | 掉自己（照抄 electric_furnace.json） |
| `lang/zh_cn.json` + `lang/en_us.json` | `block.mcdonalds-mod.my_machine` |
| `data/minecraft/tags/block/mineable/pickaxe.json`（+ `needs_stone_tool.json`） | 让镐子能挖、并且按等级掉落 |

### 步骤 5 — 界面（可选，两种路线）

- **路线 A（最省事）**：复用原版界面。参考 `ElectricFurnaceScreenHandler`：继承原版 `FurnaceScreenHandler`，
  把 `getType()` 覆盖成自己的类型（**不覆盖的话客户端会按熔炉界面开**），界面类继承对应原版 Screen 只重画要改的部分，
  `HandledScreens.register(自己的类型, MyScreen::new)`。
- **路线 B（完全自定义）**：参考 `TestGeneratorScreen` 那套 —— `ScreenHandler` 自己写 +
  `MachineScreen`（带 +/- 按钮的基类），数值显示用 `MachineNumbers`，
  超过 16 位的数值用 `LongPropertyCodec` 拆字段同步。

缓冲区图标两套路线都能用：`EnergyBoltIcon.draw(context, x, y, ratio)` 画，
悬停 `EnergyBoltIcon.drawTooltip(context, textRenderer, 当前J, 容量J, mouseX, mouseY)`。

### 步骤 6 — 电缆连接

- 默认（继承 `MachineBlock`）：六面都能接，什么都不用做。
- 只想某几面能接：`CableConnections.only(ModBlocks.MY_MACHINE, Direction.UP)`。
- 只让正面接：`CableConnections.custom(ModBlocks.MY_MACHINE, CableConnectable.facing(MyMachineBlock.FACING))`。
- 收电/送电的方向限制写在方块实体的 `canReceiveEnergyOn(side)` / `canProvideEnergyFrom(side)` 里。

### 步骤 7 — 验证（别偷懒，这套流程踩过很多坑）

1. `.\gradlew.bat build --console=plain`；
2. 起服务端（见第 6 节），`setblock` 摆机器 + `data get block` 看缓冲区和进度；
3. 需要界面/贴图的：起客户端自检，截图到 `run/screenshots/`，用 `view_image` 肉眼确认；
4. 电缆相关的：摆"发电机 + 电缆 + 机器"实测电流与充放电速率。

---

## 4. 现有机器参数表

| 机器 | 额定输入 | 缓冲区 | 工作时耗电 | 备注 |
|---|---|---|---|---|
| 电炉 `electric_furnace` | 32 V / 4 A（128 W） | 2.5 kJ | 96 W（4800 mJ/tick） | 速度同高炉（100 tick），可烧熔炉能烧的一切 |
| 测试电池盒 `test_battery_box` | 可调（上下两面充） | 无上限 | — | 侧面只输出；早期原型，未迁基类 |
| 测试发电机 `test_generator` | 只有输出 | 100 kJ | — | 可调发电/输出电压电流；早期原型，未迁基类 |

电缆（`ModBlocks` 里的 `new CableBlock(电压V, 粗细, 电流A, 每格电阻mΩ)`）：

| 电缆 | 电压 | 电流 | 每格电阻 |
|---|---|---|---|
| 锡 | 32 V | 16 A | 0.1 Ω |
| 铜 | 128 V | 32 A | 0.05 Ω |
| 精炼铁 | 512 V | 32 A | 0.1 Ω |
| 金 | 512 V | 64 A | 0.05 Ω |
| 铁 | 2048 V | 128 A | 0.06 Ω |
| 玻璃纤维 | 8192 V | 128 A | 0.01 Ω |

---

## 5. 踩坑清单（1.21.11，全是实测踩过的）

1. **方块 / 方块实体操作必须在服务端线程**：客户端线程里直接操作经常不通，命令会报"该位置尚未被加载"。
   正确写法：`client.getServer().execute(() -> { ... })`。
2. **物品模型**：1.21.4+ 每个方块物品都要有 `assets/<ns>/items/<id>.json`，否则物品是紫黑块。
3. **配方查询**：`world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(stack), world)`；
   配方结果只能通过 `recipe.craft(input, world.getRegistryManager())` 拿（没有 `getResult()`）。
4. **界面类型**：继承原版 `ScreenHandler` 时它会把类型写死成原版类型，
   必须在自己的容器里覆盖 `getType()` 指回自己的类型，否则客户端永远开原版界面。
5. **`HandledScreen#isPointWithinBounds` 的 x/y 是相对界面左上角的**（内部会减掉 `this.x/this.y`），
   传屏幕绝对坐标会永远判定失败。
6. **透明贴图**：作物、细杆电缆这类要 `BlockRenderLayerMap.putBlocks(BlockRenderLayer.CUTOUT, ...)`，
   否则透明像素被填成实心面板。
7. **语言文件两份都要加**（`zh_cn.json` / `en_us.json`），方块物品的键是 `block.mcdonalds-mod.<id>`。
8. **存档键名**沿用 `stored_energy` / `rated_input_voltage` / `rated_input_current`（基类用的就是这些），
   换键名会让老存档里的机器数值丢失。

---

## 6. 验证流程（照抄即可）

**服务端**（方块逻辑 / 配方 / 世界生成）：

```
# 1) 先建一个 .verify-run 目录，放 eula.txt(eula=true) 和 server.properties
# 2) 启动（注意 -D 参数要用引号包住，工作目录设成 .verify-run）：
java "-Dfabric.dli.config=<项目>\.gradle\loom-cache\launch.cfg" ^
     "-Dfabric.dli.env=server" ^
     "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer" ^
     "@<项目>\build\loom-cache\argFiles\runServer" ^
     net.fabricmc.devlaunchinjector.Main nogui --port 25599
# 3) 控制台里：
#    forceload add 0 0 31 31
#    setblock 0 -60 0 mcdonalds-mod:electric_furnace[facing=north,lit=false]
#    data get block 0 -60 0 stored_energy
```

**客户端**（界面 / 贴图 / 粒子）：

1. 复制一份存档到 `run/saves/selfcheck_world`（`robocopy /XF session.lock`）；
2. 在 `McDonaldsModClient` 里临时加：
   `if (Boolean.getBoolean("mcdonalds.selfcheck") || Files.exists(Path.of("selfcheck.trigger"))) SelfCheckBlocks.register();`
3. 自检类里用 `client.getServer().execute(...)` 搭场景、开界面、截图：
   `ScreenshotRecorder.saveScreenshot(client.runDirectory, "名字.png", client.getFramebuffer(), 1, msg -> {})`；
4. 用 `view_image` 看 `run/screenshots/名字.png`；
5. **验证完必须删掉自检类和注册代码**（搜 `SelfCheck` 确认为 0 处）。

模拟鼠标悬停（截图里要有 tooltip）：窗口坐标 = 缩放坐标 × `window.getWidth() / window.getScaledWidth()`，
然后 `GLFW.glfwFocusWindow(handle)` + `GLFW.glfwSetCursorPos(handle, x, y)`，每 tick 设一次避免漂移。
