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

> 目前是**空壳工程**：只有入口类、Mixin 骨架和模组元数据，没有任何物品/方块/贴图。等你下一步指令再往里加内容。

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
- `.vscode\settings.json`：JDK 21/25 注册、Gradle 用 JDK 25、排除 `FtcRobotController-10.1.1`

---

## 二、目录结构与各文件用途

```
MC mod\
├─ src\
│  ├─ main\                                       ← 通用代码 + 全部资源
│  │  ├─ java\com\xishen\
│  │  │  ├─ XishenMod.java                        ← 主入口（MOD_ID、注册东西的地方）
│  │  │  └─ mixin\XishenMixin.java                ← 通用 Mixin 骨架
│  │  └─ resources\
│  │     ├─ fabric.mod.json                       ← 模组元数据（id/名称/入口/依赖）
│  │     ├─ xishen.mixins.json                    ← 通用 Mixin 配置
│  │     └─ assets\xishen\                        ← 美术资源，路径固定 assets\<modid>\
│  │        └─ icon.png                           ← 模组图标（列表里显示，可替换成自己的）
│  └─ client\                                     ← 仅客户端代码（渲染/按键/HUD）
│     ├─ java\com\xishen\client\XishenClient.java
│     └─ resources\xishen.client.mixins.json
└─ build\libs\xishen-1.0.0.jar                    ← 打包产物
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

---

## 三、常用操作

**编译 / 打包**

```powershell
cd "D:\code\MC mod"
.\gradlew.bat build
```

产物：**`build\libs\xishen-1.0.0.jar`** —— 这个才是模组文件，丢进 `%APPDATA%\.minecraft\mods`（对方需装 Fabric Loader + Fabric API）。`*-sources.jar` 不用装。

VS Code 里也可以 `Ctrl+Shift+P` → `Tasks: Run Task` → `MC mod: build（打包 jar）`。

**运行 / 调试**

按 **F5** → 选 `MC mod: 客户端`（或 `MC mod: 服务端`）。日志里会看到 `戏神（xishen）已加载`。

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

## 四、不同 MC 版本 / 加载器的差异

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

## 五、常见问题

**1. `Dependency requires at least JVM runtime version 25`** → Gradle 用错 JDK，检查 `gradle.properties` 的 `org.gradle.java.home` 是否为 JDK 25。

**2. 卡在下载 `gradle-9.x-bin.zip`** → 从镜像手动下载后放进 wrapper 目录（腾讯镜像 `https://mirrors.cloud.tencent.com/gradle/`），并建 `.ok` 标记文件。

**3. F5 报找不到 `launch.cfg` 或出现 `@@0020`** → 项目路径含空格时 Loom 生成的 `launch.json` 会把空格写成 `@@0020`。已在 `build.gradle` 末尾加了修正（自动改成相对路径 `../.gradle/loom-cache/launch.cfg`），**别手改回去**；重新生成用任务 `MC mod: refresh-vscode`。

**4. `settings.gradle` 报 `Unexpected character: '\ufeff'`** → 文件带 BOM 了，用 VS Code 右下角改成 UTF-8 另存。

**5. 物品紫黑方块 / 名字显示成键名** → 模型文件名或 `textures` 路径不对、贴图不是 16×16；或 `lang` 里缺键。

---

## 六、git 说明

工程初始化过 git，**没有连任何 GitHub 远程仓库，也没有 push 过**。提交作者是占位身份：

```
mcmod <mcmod@localhost>
```

换成自己的：

```powershell
cd "D:\code\MC mod"
git config user.name "你的名字"
git config user.email "你的邮箱"
git commit --amend --reset-author --no-edit
```

要推 GitHub：建仓库 → `git remote add origin <地址>` → `git push -u origin main`。

## 许可

模板本身 CC0；你的模组代码随意。
