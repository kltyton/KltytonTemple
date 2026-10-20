# KltytonTemple

[English](README.en.md) · [Wiki](https://github.com/kltyton/KltytonTemple/wiki) · [迁移指南](docs/wiki/Migration.zh-CN.md)

一个仓库维护多个 Minecraft 版本和加载器。每个目标保留自己的 Wrapper、运行 JDK、编译 JDK、映射和依赖，共享源码在各目标的 Minecraft 类路径中重新编译。

模板基于 [SighsTemple](https://github.com/Tower-of-Sighs/SighsTemple)，工程组织参考 MultiLoader、Stonecutter 和 Architectury。共享模块和依赖范围采用 Gradle 原生模型；版本适配使用普通 Java 源码，不使用条件注释预处理。

## 创建与迁移

Minecraft Development 集成提供 Mod 信息和版本表单，支持简体中文与 English 切换。Minecraft 和 Loader 读取 Mojang、Fabric、Forge、NeoForge 的目录；Fabric API 按 Minecraft 版本查询 Fabric 项目在 Modrinth 发布的版本。Java 要求来自 Minecraft 官方元数据。选定版本写入目标配置，刷新目录不会升级已有项目。

“Gradle 脚本语言”可选择 Groovy（`.gradle`）或 Kotlin DSL（`.gradle.kts`）。两种方式使用同一套目标配置、源层和 Gradle 任务。KTS 工程的根工程、目标工程、settings、buildSrc 构建脚本及公共构建约定都生成 KTS；`addTarget` 沿用工程的选择。模组源码语言不随这个选项改变。

- [安装扩展与创建工程](docs/wiki/Minecraft-Development.zh-CN.md)
- [现有工程迁移](docs/wiki/Migration.zh-CN.md)
- [可复制的迁移提示词](integrations/prompts/migrate.zh-CN.md)
- [迁移 Skill](integrations/skills/kltyton-temple/SKILL.md)

## Gradle 任务

命令从工程根目录执行。Windows 使用 `.\gradlew.bat`，其他系统使用 `./gradlew`。

| 任务 | 功能 |
| --- | --- |
| `listTargets` | 列出目标、Minecraft / Loader、编译及运行 Java 和 CI 开关 |
| `createProject` | 打开新工程表单，拒绝覆盖已有目录 |
| `addTarget` | 打开目标选择表单，拒绝替换已有目标 |
| `setupProject` | 初始化目标 Wrapper，不构建游戏 |
| `select_<target>` | 设置默认目标和共享源码的编辑环境 |
| `build` | 构建默认目标 |
| `build_<target>` | 构建指定目标 |
| `buildAllTargets` | 构建全部目标 |
| `runClient_<target>` | 启动开发客户端 |
| `runServer_<target>` | 启动开发服务器 |
| `runDatagen_<target>` | 执行目标的资源生成任务 |
| `verify_<target>` | 检查已有发行 JAR，不追加构建 |
| `verifyAllDistributions` | 检查全部已有发行 JAR |
| `publish_modrinth_<target>` | 打开 Modrinth 发布表单 |
| `publish_curseforge_<target>` | 打开 CurseForge 发布表单 |
| `publish_maven_<target>` | 打开 Maven 发布表单 |
| `exportMinecraftTemplates` | 生成 Minecraft Development 向导资产 |
| `writeCiMatrix` | 输出启用目标的 CI 矩阵 |

目标后缀中的短横线和点替换为下划线，例如 `fabric-1.21.1` 对应 `fabric_1_21_1`：

```sh
./gradlew build_fabric_1_21_1
./gradlew verify_fabric_1_21_1
./gradlew runClient_fabric_1_21_1
./gradlew select_neoforge_26_1_2
```

`runClient`、`runServer`、`runDatagen` 是默认目标的简写。选择目标同时确定共享源码的编辑环境；重新加载 Gradle 模型后，补全使用该目标的 Minecraft、Loader 和 Java。全部目标仍独立导入。

创建工程时，“默认编辑目标”下拉框列出已添加的 Minecraft / Loader 组合。选择后，首次导入的 `common` 环境和根任务默认值都使用这个目标。删除当前默认目标后，下拉框改选剩余列表中的第一个目标，可以继续手动选择。

安装 1.5.0 扩展并同步全部 Gradle 项目后，其他 `compat` 目录也按实际构建模型登记到匹配的 common 源集模块，使用所属目标的 SDK。共享 main 保留一个编辑环境；切换默认目标不需要复制源码，也不会把其他版本的适配目录合到同一个 SDK 下。

`-PtemplePlan=true` 输出目标构建/运行计划，并跳过对应子构建：

```sh
./gradlew buildAllTargets -PtemplePlan=true
```

计划输出不代表构建已经通过。

## 配置参数

根 `gradle.properties` 描述模组身份；各目标文件描述构建环境。

| 参数 | 位置 | 含义 |
| --- | --- | --- |
| `mod_id` | 根 | 注册与资源命名空间 |
| `mod_name` | 根 | 显示名称 |
| `mod_group_id` | 根 | Maven group，默认也作为 Java 包名 |
| `mod_java_package` | 根，可选 | 已有项目的 Java 包名，可与 Maven group 分开维护 |
| `mod_entry_class` | 根，可选 | 发行检查使用的已有公共入口类 |
| `mod_version` | 根 | 模组版本 |
| `mod_authors` | 根 | 作者 |
| `mod_license` | 根 | 许可证 |
| `mod_description` | 根 | 描述 |
| `temple_default_target` | 根 | 创建表单选定的默认编辑目标；没有本地选择记录时使用 |
| `temple_idea_project` | 根 | 启用 IDEA 扩展的独立目标关联 |
| `temple_build_dsl` | 根 | `groovy` 或 `kotlin`；新建表单写入，后续新增目标沿用对应脚本语言 |
| `loader`、`minecraft_version` | 目标 | 加载器和 Minecraft 版本 |
| `loader_version` | 目标 | Loader 版本 |
| `fabric_api_version` | Fabric 目标 | Fabric API 版本 |
| `java_version` | 目标 | 游戏代码的编译 Java 版本 |
| `gradle_java_version` | 目标 | 运行目标 Gradle 的 Java 版本 |
| `ci_enabled` | 目标 | 是否纳入 CI 矩阵 |
| `datagen_task` | 目标，可选 | 已实际配置的资源生成任务；未配置时不注册 datagen 别名 |

Loader 和 Minecraft 的版本范围保留在目标元数据参数中。Wrapper 版本由目标的 `gradle/wrapper/gradle-wrapper.properties` 决定。

## 源码与资源

开发时主要维护两个目录：

```text
common/
├── build.gradle(.kts)      共享模块、依赖与源码配置
└── src/
    ├── main/              按功能组织的共享 Java 与资源
    └── compat/            有实际差异时才添加的适配源码
targets/
└── <loader>-<minecraft>/  独立构建、Wrapper 与 Loader 元数据
```

共享依赖在 `common/build.gradle(.kts)` 中使用 `api`、`implementation`、`compileOnly`、`runtimeOnly` 等标准配置声明。每个 target 包含真实的 `:common` 模块，使用自己的 Minecraft/Loader 类路径编译；公共类合入该目标的发行 JAR。

`common/src/main/java` 中的类按注册、网络、配置、渲染等实际功能分包。只把查证过的 API 差异放入 `common/src/compat`，通过普通 Java 的小范围接口接入公共逻辑。没有差异就不建目录，不复制完整业务类，不写版本条件注释。适配目录的精确规则见[架构](docs/dev/architecture.md)。

手写资源保持原维护方式，datagen 输出仍在各目标的 `src/generated/resources`。Forge/NeoForge 的现有资源输入包含 common 与当前目标；Fabric 使用相同的主资源集。

迁移采用与新建工程相同的结构。游戏实现归入 common，targets 保留独立构建和必要元数据；旧工程壳和失效路径在消费者全部迁移后清退。构建控制、IDEA 扩展和向导资产不进入模组 JAR。

## 基线目标

| Minecraft | Loader |
| --- | --- |
| 1.18.2、1.19.2 | Forge |
| 1.20.1 | Fabric、Forge |
| 1.21.1 | Fabric、NeoForge |
| 26.1.2 | Fabric、NeoForge |
| 26.2 | NeoForge |

九个样例不限制版本下拉列表。目录从 Minecraft 1.17 起提供当前构建管线覆盖的正式版本；Loader 和 Fabric API 保留可用历史版本，“推荐”只是标记。预发布 Loader/API 可通过表单筛选开启。新组合仍需匹配 API 和实际构建验收，已有模组不会因为选了新版本就自动适配。

[架构](docs/dev/architecture.md) · [CI](docs/CI_TARGET_DISCOVERY.md) · [发布](docs/PUBLISHING.md)

## 许可证

模板采用 MIT，保留 SighsTemple 的版权声明。生成工程的游戏代码、素材和第三方依赖仍按各自许可证维护。IDEA 扩展依赖本机 Minecraft Development，不分发其二进制。
