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

**灰界**已经做成一个真正的维度，目前有**两个区域**：禁忌之海（海）、鬼嘲深渊（巨型深渊）。

| 内容 | 注册名 | 文件 |
| --- | --- | --- |
| 维度 | `xishen:gray_realm` | `data/xishen/dimension/gray_realm.json` |
| 维度类型 | `xishen:gray_realm` | `data/xishen/dimension_type/gray_realm.json` |
| 群系·区域1 | `xishen:forbidden_sea`（禁忌之海） | `data/xishen/worldgen/biome/forbidden_sea.json` |
| 群系·区域2 | `xishen:ghost_abyss`（鬼嘲深渊） | `data/xishen/worldgen/biome/ghost_abyss.json` |
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
  - 禁忌之海：保留海水（一片灰海）
  - 鬼嘲深渊：在区域里挖一个**碗形深渊**——越靠中心越深（最深约 60 格，中心底部 y≈36），按 3 格一台阶形成阶梯状陡壁，坑底铺灰土、坑壁是灰石，上方全部抽成空气（没有水、没有岛）
  - 灰雾带 / 边界灰雾带：抽干海水，铺成灰草 + 灰土 + 灰石的地表，带 0~5 格平缓起伏；两者地形完全一样，**区别只在伤害**
- 时间固定在**午夜**（`fixed_time: 18000`）、环境光偏暗、床会爆炸（`bed_works: false`）、没有袭击。
- **没有任何生物、植被、矿物、结构**（`spawners` 全空、`features` 全空、`disable_mob_generation: true`）——按你的要求，生物和传送门等你发话再加。

### 灰界方块与颜色（全部黑白灰）

原著设定：**灰界没有任何颜色**。所以灰界里的方块和液体都换成了无彩色的版本：

| 方块 | id | 用在哪 |
| --- | --- | --- |
| 灰草方块 | `xishen:gray_grass_block` | 灰雾带 / 边界地表顶层 |
| 灰土 | `xishen:gray_dirt` | 灰草下面 3 层 |
| 灰石 | `xishen:gray_stone` | 整个地层（默认方块） |
| 灰砾石 | `xishen:gray_gravel` | 水下海床 |

- 三个群系的 **雾色/天空色/水色/水下雾色全部改成了灰阶**（比如水色 `#2A2A2A`），所以连海水都是灰的，没有原版的蓝。
- **例外：鬼嘲深渊**。原著里它是"猩红主宰"的地盘，所以这个区域给了一层**极暗的红**（雾 `#1A0808`、天 `#2A0A0A`、水 `#3A0A0A`），方块本身仍是灰阶——只有氛围是暗红。
- 液体用的是原版水，但通过群系颜色去掉了蓝色调；没有额外写流体。
- 贴图在 `assets/xishen/textures/block/`，都是纯灰（R=G=B），没有色相。

怎么进去（当前只能用命令，因为你还没让做传送门）：

```
/execute in xishen:gray_realm run tp @s 0 120 0
```

落地后如果掉进水里，先 `/gamemode creative` 再飞起来看看地形。

### 灰蚀（灰界的环境伤害）

灰界里**所有原版生物都会持续掉血**，用的是自定义状态效果「**灰蚀**」（`xishen:gray_erosion`），
不是原版凋零/中毒，伤害也走**自定义伤害类型**，死亡提示是「xxx 在灰界中被侵蚀殆尽」。

| 位置 | 判定（离「实体区域」多远） | 灰蚀等级 | 每次伤害（每 2 秒一次） | 附加效果 |
| --- | --- | --- | --- | --- |
| 区域（如禁忌之海） | 在某个区域圆内 | 0 | 1（半颗心） | — |
| 常规灰雾带 | 区域外、灰界半径 3000 内 | 1 起，离区域越远越高（最高 3） | 2 ~ 4 | **失明** |
| 边界灰雾带 | 超出灰界半径 3000 后 | **「灰蚀：边界」**（独立效果），0 级起每 200 格 +1，最高 4 级 | 2 ~ 10 | **失明 + 黑暗**，并**无视一切豁免**（灰界生物、神道领域也会被侵蚀） |

布局算法在 `com.xishen.world.GrayRealmLayout`（按世界种子生成位置/大小/不规则形状），区域规格在 `GrayRealmRegions.SPECS`（含半径区间、是否固定在原点；现有 `forbidden_sea` 固定原点、`ghost_abyss` 随机摆放），`WORLD_RADIUS = 3000` 是灰界地图半径。

**谁免疫**（你要的「灰界生物 / 神道领域不受伤」）现在有两个钩子：

1. **实体类型标签** `xishen:gray_realm_immune` —— 文件在 `data/xishen/tags/entity_type/gray_realm_immune.json`，
   以后加灰界生物时把它们的实体 id 填进 `values` 即可，它们就完全不吃灰蚀。
2. **scoreboard tag** `xishen_gray_immune` —— 给玩家/生物打上这个标签就免疫，例如
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

**7. 物品紫黑方块 / 名字显示成键名** → 模型文件名或 `textures` 路径不对、贴图不是 16×16；或 `lang` 里缺键。

---

## 七、git 说明

仓库已经连上 GitHub 并推送过：<https://github.com/yzbgx/xishen>（公开仓库，默认分支 `main`）。

- 提交身份：`LGXXXXXX <862210701@qq.com>`（本地 `git config`，只作用于这个仓库）
- 远程：`origin = https://github.com/yzbgx/xishen.git`
- 代理：**这个仓库本地配了** `http.proxy=http://127.0.0.1:7890`。直连 GitHub 会 25 秒起步、经常挂死，所以必须走 Clash；哪天不开 Clash 了，执行 `git config --local --unset http.proxy` 即可去掉。
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
