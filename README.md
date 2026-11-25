# 戏神（Xishen）— Minecraft 1.21.1 / Fabric 模组工程

工程位置：`D:\code\MC mod`（VS Code 用 **`D:\code`** 作为根目录打开，这个工程是里面的子文件夹）。

| 项目 | 值 |
| --- | --- |
| 模组名称 | 戏神 |
| modid | `xishen` |
| 包名 | `com.xishen` |
| Minecraft | 1.21.1 |
| Fabric | Loader 0.20.1 + Fabric API 0.116.18+1.21.1 |
| 构建 | Loom 1.19 + Gradle 9.8.2，模组字节码 Java 21 |
| 版本号 | 1.0.0（在 `gradle.properties` 的 `version` 改） |

> 当前进度：**灰界维度**（禁忌之海区域 + 常规灰雾带 + 边界灰雾带）、**灰蚀 / 灰蚀：边界** 两种环境伤害、**4 个灰界方块**（灰土/灰草/灰石/灰砾石，全灰阶）。没有生物、植被、矿物、结构、传送门——等你的指令再加。

---

## 一、环境清单

| 组件 | 位置 / 版本 | 说明 |
| --- | --- | --- |
| JDK 21（编译用） | `C:\Program Files\BellSoft\LibericaJDK-21` | 模组代码按 Java 21 编译（MC 1.21.1 的要求） |
| JDK 25（跑 Gradle 用） | `C:\Users\hp\.jdks\jdk-25.0.5` | Loom 1.19 本身要求 Java 25 才能运行 |
| Gradle 9.8.2 | `%USERPROFILE%\.gradle\wrapper\dists\gradle-9.8.2-bin` | 已手动放好，wrapper 不会再卡在下载 |
| VS Code 扩展 | `vscjava.vscode-java-pack`、`vscjava.vscode-gradle` | Java 语言支持、调试、Gradle 集成 |

工作区配置（根目录 `D:\code`）：

- `.vscode\launch.json`：F5 启动 `MC mod: 客户端` / `MC mod: 服务端`
- `.vscode\tasks.json`：`MC mod: build / runClient / runServer / genSources / refresh-vscode`
- `.vscode\settings.json`：JDK 21/25 注册、Gradle 用 JDK 25、排除 `FtcRobotController-10.1.1`、文件编码 UTF-8、Java 语言服务器 `-Dfile.encoding=UTF-8`

---

## 二、目录结构与各文件用途

```
MC mod\
├─ src\
│  ├─ main\
│  │  ├─ java\com\xishen\
│  │  │  ├─ XishenMod.java                ← 主入口：注册方块/效果/生成器，启动环境驱动
│  │  │  ├─ block\XishenBlocks.java       ← 灰土 / 灰草 / 灰石 / 灰砾石
│  │  │  ├─ effect\GrayErosionEffect.java ← 灰蚀
│  │  │  ├─ effect\GrayErosionEdgeEffect.java ← 灰蚀：边界（无视豁免）
│  │  │  ├─ world\GrayRealmRegions.java   ← 区域规格 + 地图半径等常量
│  │  │  ├─ world\GrayRealmLayout.java    ← 按世界种子生成区域布局/形状
│  │  │  ├─ world\GrayRealmBiomeSource.java ← 按坐标分配 区域/灰雾带/边界 群系
│  │  │  ├─ world\GrayRealmChunkGenerator.java ← 地形后处理（海域保水、雾带抽干铺灰草灰土）
│  │  │  ├─ world\GrayRealmTicker.java    ← 每秒挂灰蚀/失明/黑暗、免疫判定
│  │  │  └─ mixin\XishenMixin.java
│  │  └─ resources\
│  │     ├─ fabric.mod.json
│  │     ├─ xishen.mixins.json
│  │     ├─ assets\xishen\            ← 贴图/模型/语言/效果图标
│  │     └─ data\xishen\              ← 维度/群系/地形/伤害类型/战利品表
│  └─ client\java\com\xishen\client\XishenClient.java
└─ build\libs\xishen-1.0.0.jar        ← 打包产物
```

要加内容时的固定位置（路径写错游戏里就会紫黑方块或显示键名）：

| 内容 | 放哪里 |
| --- | --- |
| 物品/方块/事件逻辑 | `src\main\java\com\xishen\` 下新建类，在 `XishenMod.onInitialize()` 里注册 |
| 只给客户端的逻辑 | `src\client\java\com\xishen\client\` |
| 物品贴图 | `src\main\resources\assets\xishen\textures\item\<名字>.png`（16×16 PNG，透明背景） |
| 方块贴图 | `src\main\resources\assets\xishen\textures\block\<名字>.png` |
| 物品模型 | `src\main\resources\assets\xishen\models\item\<名字>.json`（普通物品 `"parent": "item/generated"`） |
| 方块模型/方块状态 | `assets\xishen\models\block\`、`assets\xishen\blockstates\` |
| 显示名（多语言） | `assets\xishen\lang\zh_cn.json`、`en_us.json`，键形如 `item.xishen.<名字>` |
| 合成表/进度 | `src\main\resources\data\xishen\recipe\`、`...\advancement\` |
| 音效/Ogg | `assets\xishen\sounds\` + `sounds.json` |

### 现在工程的代码地图（想知道某个功能在哪写的就看这张表）

| 文件 | 负责什么 |
| --- | --- |
| `src\main\java\com\xishen\XishenMod.java` | 主入口：注册方块 / 状态效果 / 生成器，启动灰界环境驱动 |
| `src\main\java\com\xishen\block\XishenBlocks.java` | 灰界方块注册（灰土/灰草/灰石/灰砾石） |
| `src\main\java\com\xishen\effect\GrayErosionEffect.java` | 「灰蚀」效果本体：每 2 秒按等级扣血 |
| `src\main\java\com\xishen\effect\GrayErosionEdgeEffect.java` | 「灰蚀：边界」：伤害翻倍、无视豁免 |
| `src\main\java\com\xishen\world\GrayRealmRegions.java` | 区域规格表（半径区间、是否固定在原点）+ 地图半径等常量 |
| `src\main\java\com\xishen\world\GrayRealmLayout.java` | 按世界种子生成布局：区域位置/大小/不规则形状、灰雾带与边界判定、灰蚀等级公式 |
| `src\main\java\com\xishen\world\GrayRealmChunkGenerator.java` | 地形生成器：按区域后处理地形——禁忌之海保水，雾带/边界抽干成灰草+灰土丘陵 |
| `src\main\java\com\xishen\world\GrayRealmTicker.java` | 每秒扫描灰界里的活物，按坐标挂灰蚀/失明/黑暗；免疫判定也在这里 |
| `src\client\java\com\xishen\client\XishenClient.java` | 客户端入口（以后做按键、HUD、渲染） |
| `src\main\resources\data\xishen\...` | 纯数据：维度、群系、地形、伤害类型、标签（改 JSON 就能调数值/范围，不用写 Java） |
| `src\main\resources\assets\xishen\...` | 美术资源：贴图、模型、语言、效果图标 |

### 在 VS Code 里读代码的常用操作

- **打开文件**：左边资源管理器按路径展开，或 `Ctrl+P` 输入文件名（如 `GrayRealmTicker`）直接跳。
- **跳到定义**：`Ctrl+点击` 类名/方法名，或把光标放上去按 `F12`。
- **找引用**：右键 → `Find All References`（`Shift+F12`），看某个方法被谁调用了。
- **看 Minecraft 原版源码**：先跑一次任务 `MC mod: genSources`，之后 `Ctrl+点` `Item`、`Level` 这类类就能进原版源码（官方映射名）。
- **看某个效果/维度到底注册了什么**：`Ctrl+Shift+F` 全局搜索 `xishen:` 或 `gray_erosion`。
- **改完怎么生效**：改 Java 要重新 build（`Ctrl+Shift+B`）；只改 `data/` 或 `assets/` 里的 JSON，重新 build 一次即可，不用改代码。

---

## 三、灰界维度：禁忌之海（当前进度）

**灰界**已经做成一个真正的维度，目前有**四个区域**（风暴孤岛按你的要求暂不做）：
禁忌之海（海）、鬼嘲深渊（碗形深渊）、苦肉浊林（石柱林）、叹息旷野（平原）、虚妄山脉（高山）。

| 内容 | 注册名 | 文件 |
| --- | --- | --- |
| 维度 | `xishen:gray_realm` | `data/xishen/dimension/gray_realm.json` |
| 维度类型 | `xishen:gray_realm` | `data/xishen/dimension_type/gray_realm.json` |
| 群系·区域1 | `xishen:forbidden_sea`（禁忌之海） | `data/xishen/worldgen/biome/forbidden_sea.json` |
| 群系·区域2 | `xishen:ghost_abyss`（鬼嘲深渊） | `data/xishen/worldgen/biome/ghost_abyss.json` |
| 群系·区域3 | `xishen:rot_forest`（苦肉浊林） | `data/xishen/worldgen/biome/rot_forest.json` |
| 群系·区域4 | `xishen:sigh_plain`（叹息旷野） | `data/xishen/worldgen/biome/sigh_plain.json` |
| 群系·区域5 | `xishen:vain_range`（虚妄山脉） | `data/xishen/worldgen/biome/vain_range.json` |
| 群系·常规灰雾带 | `xishen:gray_fog`（灰雾带） | `data/xishen/worldgen/biome/gray_fog.json` |
| 群系·边界灰雾带 | `xishen:gray_fog_edge`（边界灰雾带） | `data/xishen/worldgen/biome/gray_fog_edge.json` |
| 群系分配器 | `xishen:gray_realm`（自定义 BiomeSource） | `com.xishen.world.GrayRealmBiomeSource` |
| 地形噪声 | `xishen:gray_realm` | `data/xishen/worldgen/noise_settings/gray_realm.json` |

设计要点（按你的要求：**区域位置每个世界随机、但挤成一团不碎开；区域内地形随机**）：

- **整片灰界是一个维度**，区域之间靠坐标分隔（不是每个区域一个维度），以后加「鬼嘲深渊 / 苦肉浊林 / 叹息旷野 / 虚妄山脉 / 风暴孤岛」只是往同一个维度里加区域。
- **每个区域 = 一个群系**，位置**每个世界随机**（不是固定地图，也不是同心圆）：`GrayRealmLayout` 按世界种子生成布局——**每个区域自身是一整块**（变形圆盘，不会碎成几片），大小在 `GrayRealmRegions.SPECS` 给的区间里随机，边界用低频噪声扰动成不规则形状；**区域之间留出空隙**，空隙就是灰雾带。
- **禁忌之海永远以 (0,0) 为中心**（`Spec.pinnedAtOrigin = true`）；其它区域以后按环状随机摆放（`RING_MIN`~`RING_MAX`）。
- 区域之外、灰界范围之内（离地图中心 3000 格内）全部是**常规灰雾带** `xishen:gray_fog`——也就是"实体区域以外的不规则空地"；超出 3000 格是**边界灰雾带** `xishen:gray_fog_edge`（雾最浓、伤害最高）。
- 加新区域：`GrayRealmRegions.SPECS` 加一条规格 + `dimension/gray_realm.json` 的 `regions` 表里配群系；原书特别点明的形状（如鬼嘲深渊是巨大峡谷）以后在 `GrayRealmLayout` 里按 id 特判拉伸。
- **地形用噪声随机生成**（抄的原版 overworld 噪声路由器），所以每次开新世界，海床起伏、岛屿形状都不一样。
- 海平面抬到 **96**，绝大部分地形被水淹没，只剩零星岛屿；海床是**灰砾石**、地层是**灰石**、岛屿表层是**灰土**（全灰阶，见下一节）。
- **地形按区域区分**（`GrayRealmChunkGenerator`）：
  - 禁忌之海：灰海，**海床固定在海平面下 55 格左右**（海底铺灰砾石，下面全是灰石）
    —— 这样水不会一路深到岩浆层，也不会在水/岩浆交界生成黑曜石、看到岩浆
  - 鬼嘲深渊：一条贯穿区域的**大峡谷**——走向每个区域随机固定，中心线来回摆动、宽 24~42 格，
    最深约 96 格（谷底 y≈0），两端收浅，谷壁 4 格一台阶成陡崖，谷底铺灰土 / 零星灰砾石
  - 苦肉浊林：**血肉丛林**。地面是灰草灰土，树由**原版 tree 特征**生成（5 种：参天大树 / 塔形高树 /
    平顶伞状 / 中等橡树 / 小树），高矮、树冠形状、分枝都不同，按区块随机放置，所以是自然生成的森林而不是复制粘贴
  - 叹息旷野：0~3 格缓起伏 + 散布着**浅坑洼地**（风蚀地貌，96 格一格、半径 10~24、深 1~5），地表灰草
  - 虚妄山脉：**脊状噪声堆山**，山高 10~60 格；表层按坡度决定——陡壁（相邻高差 ≥6）露灰石，缓坡和盆地一律铺灰草，
    所以再低的盆地也有土层，只有悬崖才露岩石
  - 灰雾带 / 边界灰雾带：抽干海水，铺成灰草 + 灰土 + 灰石的地表，**纯平**（0 格起伏）；两者地形完全一样，**区别只在伤害**
- 时间固定在**午夜**（`fixed_time: 18000`）、环境光偏暗、床会爆炸（`bed_works: false`）、没有袭击。
- **没有任何生物、植被、矿物、结构、岩浆**（`spawners` 全空、`features` 全空、`disable_mob_generation: true`；
  噪声路由里的 `lava` 密度设成常数 0，所以地底不会再生成岩浆湖/岩浆含水层）——按你的要求，生物和传送门等你发话再加。

### 灰界方块与颜色（全部黑白灰）

原著设定：**灰界没有任何颜色**。所以灰界里的方块和液体都换成了无彩色的版本：

| 方块 | id | 用在哪 |
| --- | --- | --- |
| 灰草方块 | `xishen:gray_grass_block` | 灰雾带 / 边界地表顶层 |
| 灰土 | `xishen:gray_dirt` | 灰草下面 3 层 |
| 灰石 | `xishen:gray_stone` | 整个地层（默认方块） |
| 灰砾石 | `xishen:gray_gravel` | 水下海床 |
| 血肉原木 | `xishen:flesh_log` | 苦肉浊林的血肉树树干 |
| 血肉簇 | `xishen:flesh_leaves` | 血肉树的树冠 |

- 三个群系的 **雾色/天空色/水色/水下雾色全部改成了灰阶**（比如水色 `#2A2A2A`），所以连海水都是灰的，没有原版的蓝。
- **例外：鬼嘲深渊**。原著里它是"猩红主宰"的地盘，所以这个区域给了一层**极暗的红**（雾 `#1A0808`、天 `#2A0A0A`、水 `#3A0A0A`），方块本身仍是灰阶——只有氛围是暗红。
- **例外：血肉树**。苦肉浊林里的**血肉原木 / 血肉簇**是暗红色（`#6C1012` 一带）——原著写的是"血肉丛生的林间"，红色是它本身的质感；其余方块仍保持灰阶。
- 液体用的是原版水，但通过群系颜色去掉了蓝色调；没有额外写流体。
- 贴图在 `assets/xishen/textures/block/`，都是纯灰（R=G=B），没有色相。

### 方块家族（按原版合成规则延伸出来的方块）

原版里"一个基础方块 → 一整套建筑方块"是有固定规则的，我们的方块也照这个规则做了延伸：

| 家族 | 基础方块 | 延伸出来的方块 | 合成规则（和原版一致） |
| --- | --- | --- | --- |
| 灰石 | `gray_stone` | 灰石台阶 / 楼梯 / 墙 / 压力板 / 按钮、灰石砖（+ 砖台阶 / 楼梯 / 墙） | 3 个一排 → 6 台阶；阶梯形 → 4 楼梯；2×3 → 6 墙；2 个一排 → 压力板；1 个 → 按钮；2×2 → 4 砖；切石机也能切 |
| 血肉 | `flesh_log` | 血肉木板 → 楼梯 / 台阶 / 栅栏 / 栅栏门 / 门 / 活板门 / 按钮 / 压力板 | 原木 → 4 木板；之后和原版橡木那套完全相同（栅栏 4 板+2 棍→3，门 2×3→3，活板门 2×3→2……） |
| 灰土 | `gray_dirt` | 粗灰土 | 2 灰土 + 2 灰砾石（无序）→ 4 粗灰土，和原版"粗土"一样 |

### 血肉树（用原版 tree 特征生成）

不再手写"棒棒糖"，改成 5 个 `minecraft:tree` 配置（`data/xishen/worldgen/configured_feature/flesh_tree_*.json`），
方块换成血肉原木/血肉簇，树干和树冠算法直接用原版的：

| 特征 | 原型 | 外形 |
| --- | --- | --- |
| `flesh_tree_big` | 原版大橡树（fancy_oak） | 粗干分叉、冠大而不规则（参天大树） |
| `flesh_tree_tall` | 原版云杉（spruce） | 高瘦、分层圆锥冠 |
| `flesh_tree_flat` | 原版金合欢（acacia） | 斜干、平顶伞状 |
| `flesh_tree_medium` | 原版橡树（oak） | 中等大小、圆冠 |
| `flesh_tree_small` | 原版白桦（birch） | 细干小冠 |

- 放在群系的 `features[9]`（植被装饰阶段），每个区块按 `count` + `in_square` + `heightmap` 随机落点
- 灰土/灰草已加进 `#minecraft:dirt`，原版树、甘蔗、耕地这些逻辑都认我们的方块

### 血肉船（`xishen:flesh_boat`）

原版船是"按木材种类各做一个实体"，所以光加标签是没用的，这里做了完整的一条：

| 部分 | 文件 |
| --- | --- |
| 实体（继承原版 `Boat`，划水/上船/破坏掉落全部沿用原版） | `src\main\java\com\xishen\entity\FleshBoat.java` |
| 实体类型注册 | `com\xishen\entity\XishenEntities.java` |
| 物品（照原版 `BoatItem` 的放置逻辑） | `com\xishen\item\FleshBoatItem.java` |
| 客户端渲染（复用原版船模型 `BoatModel`，只换血肉贴图） | `src\client\java\com\xishen\client\FleshBoatRenderer.java` |
| 贴图 | `assets\xishen\textures\entity\boat\flesh_boat.png`（船体）、`assets\xishen\textures\item\flesh_boat.png`（物品图标） |
| 合成 | 血肉木板 `# #` / `###` → 1 艘（和原版船形状一样） |

- 已加进 `minecraft:boats` 标签
- 船只在水面上能用（和原版一样，放陆地上会碎成物品）
- 船体贴图是把原版橡木船贴图重新染成暗红，所以模型形状、划桨动画和原版一致

### 工具（对应原版的石制 / 木制两套）

| 材质 | 剑 | 镐 | 斧 | 锹 | 锄 |
| --- | --- | --- | --- | --- | --- |
| 灰石（= 原版石级，`Tiers.STONE`） | `gray_stone_sword` | `gray_stone_pickaxe` | `gray_stone_axe` | `gray_stone_shovel` | `gray_stone_hoe` |
| 血肉（= 原版木级，`Tiers.WOOD`） | `flesh_sword` | `flesh_pickaxe` | `flesh_axe` | `flesh_shovel` | `flesh_hoe` |

- 耐久 / 挖掘速度 / 攻击力**直接复用原版 Tier**，所以数值和原版石制、木制工具完全一致
  （比如灰石斧 7 伤害、-3.2 速度；血肉锄 -1 伤害、-2.0 速度）
- 合成表照原版形状：剑 `X / X / #`、镐 `XXX / ·#· / ·#·`、斧 `XX / X# / ·#`、锹 `X / # / #`、锄 `XX / ·# / ·#`，
  其中 `X` = 灰石 / 血肉木板，`#` = 原版木棍
- 修复材料：灰石工具用灰石（已加进 `minecraft:stone_tool_materials`），血肉工具用血肉木板（在 `minecraft:planks` 里）
- 已加进 `minecraft:swords` / `pickaxes` / `axes` / `shovels` / `hoes` 和 `enchantable/*` 标签，
  所以附魔台、铁砧、抢夺/时运/耐久等机制都把它们当正常工具
- 图标是把原版石制/木制工具的图形重新上色得到的（灰石=灰度、血肉=暗红）

### 因为补齐了标签，这些原版配方也能直接用我们的方块

| 原版配方 | 现在可以用 |
| --- | --- |
| 木棍（2 木板 → 4 棍） | 血肉木板 ✔ |
| 工作台、箱子、梯子、书架、船 | 血肉木板 ✔（`#minecraft:planks`） |
| 烧木炭 | 血肉原木 ✔（`#minecraft:logs_that_burn`） |
| 熔炉（8 圆石 → 熔炉） | 灰石 ✔（`#minecraft:stone_crafting_materials`） |
| 原版木制工具 | 血肉木板 ✔（`#minecraft:wooden_tool_materials`） |
| 原版石制工具 | 灰石 ✔（`#minecraft:stone_tool_materials`） |

补充说明：

- 血肉原木 / 血肉木板 分别加进了 `minecraft:logs_that_burn` 和 `minecraft:planks` 标签，
  所以**原版的木棍、箱子、工作台、烧木炭**这些配方可以直接用血肉木板 / 原木。
- 灰石系列加进了 `minecraft:needs_stone_tool`，和原版石头一样需要石镐以上才能挖下来。
- 台阶/楼梯/墙/栅栏/门等的方块状态、模型、掉落物、音效都照原版对应方块配置（比如台阶是双台阶合并、门有两格高）。
- 全部新方块的贴图都在 `assets/xishen/textures/block/`，灰石砖、血肉木板、血肉门、血肉活板门是这次新画的。

怎么进去（当前只能用命令，因为你还没让做传送门）：

```
/execute in xishen:gray_realm run tp @s 0 120 0
```

落地后如果掉进水里，先 `/gamemode creative` 再飞起来看看地形。

### 指令（在游戏里用）

| 指令 | 作用 |
| --- | --- |
| `/xishen regions` | 列出当前世界所有区域：id、中心坐标、半径 |
| `/xishen tp <区域id>` | 传送到该区域的**正中心**（自动落到地表高度，支持 Tab 补全区块 id） |

> 区域中心是 `GrayRealmLayout` 生成的那份真实布局，所以 `/xishen tp forbidden_sea` 一定落在圆心（0,0）；
> 传送到鬼嘲深渊会直接落在深渊底部。
> 控制台执行会报 "A player is required"，因为传送需要玩家实体。

### 灰蚀（灰界的环境伤害）

灰界里**所有原版生物都会持续掉血**，用的是自定义状态效果「**灰蚀**」（`xishen:gray_erosion`），
不是原版凋零/中毒，伤害也走**自定义伤害类型**，死亡提示是「xxx 在灰界中被侵蚀殆尽」。

| 位置 | 判定（离「实体区域」多远） | 灰蚀等级 | 每次伤害（每 2 秒一次） | 附加效果 |
| --- | --- | --- | --- | --- |
| 区域（如禁忌之海） | 在某个区域圆内 | 0 | 1（半颗心） | — |
| 常规灰雾带 | 区域外、灰界半径 3000 内 | 1 起，离区域越远越高（最高 3） | 2 ~ 4 | **失明** |
| 边界灰雾带 | 超出灰界半径 3000 后 | **「灰蚀：边界」**（独立效果），0 级起每 200 格 +1，最高 4 级 | 2 ~ 10 | **失明 + 黑暗**，并**无视一切豁免**（灰界生物、神道领域也会被侵蚀） |

布局算法在 `com.xishen.world.GrayRealmLayout`（按世界种子生成位置/大小/不规则形状），区域规格在 `GrayRealmRegions.SPECS`（含半径区间、是否固定在原点；现有 `forbidden_sea` 固定原点，其余 3 个随机摆在半径 1900~2250 的环上，彼此至少留 210 格空隙），`WORLD_RADIUS = 3000` 是灰界地图半径。

**谁免疫**（你要的「灰界生物 / 神道领域不受伤」）现在有三个钩子：

1. **实体类型标签** `xishen:gray_realm_immune` —— 文件在 `data/xishen/tags/entity_type/gray_realm_immune.json`，
   以后加灰界生物时把它们的实体 id 填进 `values` 即可，它们就完全不吃灰蚀。
2. **创造 / 旁观模式**：玩家处于创造或旁观模式时**完全不吃**灰界效果（边界灰蚀也不吃），方便调试和建设。
3. **scoreboard tag** `xishen_gray_immune` —— 给玩家/生物打上这个标签就免疫，例如
   `/tag @s add xishen_gray_immune`。这是给"拥有神道领域的玩家"预留的接口，等做神道系统时由你的代码自动打标签。

### ⚠️ 加灰界生物时的检查清单（我以后会提醒你）

每加一个"灰界原生的生物"，记得同时做这三件事，否则它会被自己的世界侵蚀死：

- [ ] 在 `data/xishen/tags/entity_type/gray_realm_immune.json` 的 `values` 里加上它的实体 id
- [ ] 在 `assets/xishen/lang/zh_cn.json`、`en_us.json` 里加 `entity.xishen.<名字>` 显示名
- [ ] 染色/刷怪蛋/生成规则（`spawners`）如果要做，也要一起加

**这套流程我会记着：以后你说"加一个灰界生物"时，我会主动提醒你补免疫标签。**

### 怎么看生成的贴图

我生成的贴图都在 `src\main\resources\assets\xishen\textures\` 下（物品 `item\`、方块 `block\`、状态效果 `mob_effect\`）。

- **VS Code 里**：点开 `.png` 文件，编辑器直接显示（右下角可放大/缩小，`Ctrl+滚轮`）。
- **Windows 里**：资源管理器双击即可用照片查看器打开；想看像素细节就用画图放大。
- **记得**：MC 的物品/方块贴图是 **16×16**，状态效果图标是 **18×18**，放大后是马赛克颗粒感，属于正常。

## 三·五、神道系统（框架）

原书十四大道："**书医兵黄青巧弈，戏偶巫力卜盗娼**"，所以一共 14 条神道：

| id | 神道 | id | 神道 |
| --- | --- | --- | --- |
| `shu` | 书 | `xi` | 戏 |
| `yi` | 医 | `ou` | 偶 |
| `bing` | 兵 | `wu` | 巫 |
| `huang` | 黄 | `li` | 力 |
| `qing` | 青 | `bu` | 卜 |
| `qiao` | 巧 | `dao` | 盗 |
| `yix` | 弈 | `chang` | 娼 |

> "弈"和"医"同音，所以弈的 id 用 `yix` 区分。

### 已经做好的部分（框架）

| 部分 | 文件 |
| --- | --- |
| 14 条神道定义（id、中英文名、代表色、满级 9） | `src\main\java\com\xishen\path\PathType.java` |
| 玩家数据（14 条等级 + 可用加点，随玩家存档保存、死亡不掉） | `com\xishen\path\PathData.java` |
| 数据挂载 + 自动同步到客户端 | `com\xishen\path\PathAttachments.java` |
| 加点的网络包（客户端点 + → 服务端校验扣点） | `com\xishen\path\PathNetworking.java` |
| 调试指令 | `com\xishen\path\PathCommands.java` |
| 界面（GUI） | `src\client\java\com\xishen\client\PathScreen.java` |

### 怎么打开界面

- 按 **K**（可在「按键设置 → 戏神」里改）
- 或者**打开背包**，在背包左侧会多一个「神道」按钮（类似饰品栏 mod 的入口）

界面是参考 Reskillable 的技能页做的：两列、每条神道一个色块 + 名字 + 等级 + 进度条 + 「+」按钮。

- 点「+」会**消耗 1 点加点、该神道 +1 级**（服务端校验，数据自动同步回来）
- 点数不够或已满级（9 级）时「+」是灰的
- 加点从哪来（升级给点 / 做任务 / 击杀）还没定，现在只能用指令 `/xishen path points <n>` 给自己发

### 调试指令（给自己加等级/点数，方便以后做内容时测试）

```
/xishen path info                 查看自己 14 条神道的等级和剩余点数
/xishen path points 5             设置剩余点数为 5
/xishen path add shu 3            书神道 +3 级（不写数字就是 +1）
/xishen path reset                全部清零
```

### 接下来要定的规则

1. 加点怎么获得（升级给点？还是做任务/杀怪给？）
2. 每条神道的等级上限、每级效果（现在只是数字，没有任何效果）
3. 是否要「转职/唯一神道」之类的限制（原书里人有专属神道）

## 四、常用操作

**编译 / 打包**

```powershell
cd "D:\code\MC mod"
.\gradlew.bat build
```

产物：**`build\libs\xishen-1.0.0.jar`** —— 这个才是模组文件，丢进 `%APPDATA%\.minecraft\mods`（对方需装 Fabric Loader + Fabric API）。`*-sources.jar` 不用装。

VS Code 里也可以 `Ctrl+Shift+P` → `Tasks: Run Task` → `MC mod: build（打包 jar）`。

**运行 / 调试**

按 **F5** → 选 `MC mod: 客户端`（或 `MC mod: 服务端`）。日志里会看到 `戏神（xishen）已加载`。

> F5 的 launch.json 里用的是 `classPaths`（由 `gradlew printClientClasspath` 从 Gradle 导出的完整 classpath），
> 不依赖 VS Code 的 Java 工程导入——因为 Loom 1.19 要求 Gradle ≥9.8，而 Java 扩展自带的 Gradle 读取器只支持到 9.7.1，工程导入会失败。
> **如果以后加了新依赖**（`build.gradle` 里新增 mod 依赖），先跑一次 `.\gradlew.bat printClientClasspath`，再重新生成 launch.json。

**看 Minecraft 源码**

```powershell
.\gradlew.bat genSources
```

之后 Ctrl+点击 `Item`、`Block` 等类可以跳进源码（本项目用 **Official Mojang Mappings**，类名是官方名，如 `net.minecraft.world.item.Item`）。

**改名字/版本号**

- 版本号：`gradle.properties` 的 `version=1.0.0`
- 模组显示名：`fabric.mod.json` 的 `name`
- modid 一般不要中途改（会牵动资源目录、包名、注册名，老存档里的物品会失效）

---

## 五、不同 MC 版本 / 加载器的差异

| MC 版本 | 需要 Java | 模板 Gradle | 插件 id | 目录结构 |
| --- | --- | --- | --- | --- |
| 1.20.1 | 17 | 8.1.x | `net.fabricmc.fabric-loom` | 单一 `src/main` |
| 1.21.1（本项目） | 21 | 9.8.2 | `net.fabricmc.fabric-loom-remap` | `src/main` + `src/client` |
| 1.21.5 ~ 1.21.11 | 21 | 9.x | 同上 | 同上 |
| 26.1 ~ 26.3（最新正式版） | **25** | 9.8.2 | 同上 | 同上 |

要点：

- **Java 版本跟着 MC 版本走**：1.20.1→17，1.21.x→21，26.x→25。
- **Loom 自身要的 JVM 版本可能更高**：本项目 Loom 1.19 要 Java 25 跑 Gradle，而模组字节码仍是 21。所以机器上并存 21/25，`gradle.properties` 的 `org.gradle.java.home` 决定 Gradle 用哪个。
- **源码映射**：`loom.officialMojangMappings()`（官方名，本项目）vs Yarn 名。换了映射所有类名都变，教程不能照抄。
- **1.21 之后模板默认 `splitEnvironmentSourceSets()`**，老版本只有 `src/main`。

换版本：取对应分支的官方模板 <https://github.com/FabricMC/fabric-example-mod>（分支名就是版本号，如 `1.20.1`、`26.3`），把 `src` 和 `fabric.mod.json` 搬过去，并装对应 JDK。版本号组合查 <https://fabricmc.net/develop>。

加载器：本机实测 `maven.neoforged.net`、`maven.minecraftforge.net` 超时，只有 Fabric 能直连；要做 Forge/NeoForge 得先配镜像（如 `https://bmclapi2.bangbang93.com/maven`）。

---

## 六、常见问题

**1. `Dependency requires at least JVM runtime version 25`** → Gradle 用错 JDK，检查 `gradle.properties` 的 `org.gradle.java.home` 是否为 JDK 25。

**2. 卡在下载 `gradle-9.x-bin.zip`** → 从镜像手动下载后放进 wrapper 目录（腾讯镜像 `https://mirrors.cloud.tencent.com/gradle/`），并建 `.ok` 标记文件。

**3. F5 报找不到 `launch.cfg` 或出现 `@@0020`** → 项目路径含空格时 Loom 生成的 `launch.json` 会把空格写成 `@@0020`。已在 `build.gradle` 末尾加了修正（自动改成相对路径 `../.gradle/loom-cache/launch.cfg`），**别手改回去**；重新生成用任务 `MC mod: refresh-vscode`。

**4. `settings.gradle` 报 `Unexpected character: '\ufeff'`** → 文件带 BOM 了，用 VS Code 右下角改成 UTF-8 另存。

**5. 鼠标悬停的 Javadoc/提示里中文是乱码** → Java 语言服务器用自己的 workspace 编码（首次启动时按系统 GBK 建的），光加 `-Dfile.encoding=UTF-8` 不够。`.vscode\settings.json` 里已经加了：
   ```json
   "java.jdt.ls.vmargs": "-Dfile.encoding=UTF-8 -Xmx1G",
   "java.project.encoding": "setDefault"
   ```
   然后执行 `Ctrl+Shift+P` → **`Java: Clean Java Language Server Workspace`** → Restart and delete，让语言服务器丢掉旧的 GBK 缓存重建。
   > 只在必须兼容老中文 Windows 文件时才考虑 GBK；本项目（Java + JSON + GitHub）统一 **UTF-8**。

**6. 换了地形/方块，但老存档里没变化** → 地形和群系颜色是生成时写进存档的，改代码不会重写既有区块；想看到新效果要**新建世界**。

**7. 控制台里的中文是乱码（`[CHAT] 鐠...`）** → JVM 在 Windows 上默认按 GBK 输出。
   `launch.json` 的 `vmArgs` 和 `build.gradle` 的 `loom { runs { ... } }` 里都已经加了
   `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8`。
   注意：游戏日志本身已经是 UTF-8（`run/logs/latest.log` 用 UTF-8 打开是正常中文），
   所以乱码是**终端按 GBK 解码**造成的——`.vscode/tasks.json` 里所有 MC 任务已经加了 `chcp 65001`，
   重开一个终端就正常；如果你自己在别的 cmd 窗口跑，先执行一次 `chcp 65001`。

**8. 不想收 GitHub Actions 的失败邮件** → 两种办法：
   - **让构建别失败**：`.github/workflows/build.yml` 里已经用 `sed` 删掉了本机的 `org.gradle.java.home`，
     用 CI 自己的 JDK 构建（2026-11-23 之后的提交都应该是绿的）。
   - **彻底关通知**：仓库页 `Watch` → `Custom` → 取消勾选 `Actions`；或 GitHub 全局
     `Settings → Notifications → Actions` 关掉。

**9. 物品紫黑方块 / 名字显示成键名** → 模型文件名或 `textures` 路径不对、贴图不是 16×16；或 `lang` 里缺键。

---

## 七、git 说明

仓库已经连上 GitHub 并推送过：<https://github.com/yzbgx/xishen>（公开仓库，默认分支 `main`）。

- 提交身份：`LGXXXXXX <862210701@qq.com>`（本地 `git config`，只作用于这个仓库）
- 远程：`origin = https://github.com/yzbgx/xishen.git`
- 代理：**当前没有配代理**（实测直连 GitHub 正常，1 秒内响应）。如果以后直连变慢/连不上，再临时加：
  `git config --local http.proxy http://127.0.0.1:7890`（前提是 Clash 开着），不需要时 `git config --local --unset http.proxy` 去掉。
- 推送凭据由 Git Credential Manager 保存，不用每次登录。

常用命令：

```powershell
cd "D:\code\MC mod"
git status          # 看改了什么
git add -A
git commit -m "说明"
git push            # 推到 GitHub
```

改显示名/邮箱：

```powershell
git config user.name "LGXXXXXX"
git config user.email "862210701@qq.com"
```

## 许可

模板本身 CC0；你的模组代码随意。
