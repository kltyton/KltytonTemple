# KltytonTemple

直接在 IntelliJ IDEA 打开工程目录，导入 Gradle 即可。全部日常操作都在右侧 Gradle 面板，无需 Python、命令行参数或额外 MCP。模板基于 [SighsTemple](https://github.com/Tower-of-Sighs/SighsTemple)，保留 MIT 许可与上游版权。

## 点选操作

展开 Gradle > Tasks：

| 分组 | 双击任务 | 操作 |
| --- | --- | --- |
| temple | createProject | 表单填写 Mod 信息与目录，勾选目标，创建独立工程 |
| temple | addTarget | 表单选择蓝图并填写真实版本，添加目标 |
| temple · Loader MC版本 | select_目标 | 选择 IDEA 使用的源码与依赖模型；随后点刷新 |
| 同一目标分组 | build_目标 | 用该目标自己的 Wrapper/JDK 构建 |
| 同一目标分组 | runClient_目标 / runServer_目标 | 启动对应客户端或服务器 |
| 同一目标分组 | runDatagen_目标 | 执行对应 Loader 的资源生成入口 |
| 同一目标分组 | verify_目标 | 检查已有发行 JAR，不追加构建 |
| 同一目标分组 | publish_平台_目标 | 表单填写当前平台凭据，再确认上传 |
| build | build | 构建当前选定目标 |
| build | buildAllTargets | 构建全部目标 |
| verification | verifyAllDistributions | 检查全部已有发行包 |

第一次导入自动选择可用目标并准备身份源码。目标注册来自各自 gradle.properties，不维护另一份任务清单。切换目标后点 Gradle 刷新；若进入 Java 25 目标，在 IDEA Gradle JVM 下拉框选择 Java 25。编译/子构建工具链由 Gradle 自动发现或下载，不需要 temple.local.properties。

## Minecraft Development 插件

在 IDEA 设置 > Minecraft Development > 项目模板仓库添加 **Local**：
`<工程目录>/integrations/minecraft-development/templates`。

之后“新建项目 > Minecraft > KltytonTemple”直接提供 Mod 信息、目标矩阵和 JDK 的原生表单。复用已安装插件的公开模板入口，不替换原有插件和其他模板。模板更新后，可点 temple > exportMinecraftTemplates 更新资产。也可以直接点 Gradle createProject 新建工程。

## 当前目标

Forge 1.18.2 / 1.19.2 / 1.20.1；Fabric 1.20.1 / 1.21.1 / 26.1.2；NeoForge 1.21.1 / 26.1.2 / 26.2.0。各目标保留自己的 Wrapper、Java、映射与 Loader 依赖。新增目标是构建蓝图，仍需核对该版本 API 与依赖，不能只凭创建成功宣称兼容。

## 源码与资源

`common/src` 放共享代码；`versions/<mc>/src` 放同版本差异；`loaders/<loader>/src` 放 Loader 入口；`targets/<loader>-<mc>` 放交叉差异与元数据。共享源码在目标自己的 Minecraft 类路径中编译，允许使用匹配的 Minecraft API。

Java 同名源文件不覆盖；资源优先级 target > loader > version > common。人工资源保留 src/main/resources，datagen 结果放目标 src/generated/resources。模板控制面在 buildSrc，不能进入模组 JAR。

[架构](docs/dev/architecture.md) · [迁移](docs/MAINTENANCE_WORKFLOW.md) · [发布](docs/PUBLISHING.md) · [CI](docs/CI_TARGET_DISCOVERY.md) · [接入 Skill](integrations/skills/kltyton-temple/SKILL.md)

[上游版本差异资料](docs/version-differences/README.md) 仅作参考，不作为当前目标支持或验收清单。
