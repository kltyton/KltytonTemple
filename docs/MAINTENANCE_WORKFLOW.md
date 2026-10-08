# 开发与迁移

1. 在现有项目内先运行 `python <模板目录>/temple.py inspect <项目目录> --json`，并读取实际 Gradle、Loader 元数据、Mixin、AT/AW、资源与注册入口。inspect 只返回声明和布局线索，不证明兼容。
2. 用 init 建立新的独立目录，选择已确认的目标。保留原工程和用户改动，不原地批量搬迁。
3. 先接入一个版本与 Loader 的完整入口和资源；只把证实可共用的代码放 common，把确切 API 差异放版本、Loader 或 target 层。
4. 在用户指定的生产阶段完成全部必要源码、资源与元数据。检查是否需要正式生成资源；没有 datagen 或构建授权时不自动执行这些任务。
5. 按既定合同统一构建受影响目标；使用 doctor 检查静态输入，使用 verify 检查真实发行 JAR。静态、构建、客户端启动、玩法、视觉、专用服务器与发布结果分别记录。
6. 原项目迁移与删除、提交、推送、上传仍需对应明确授权。Skill、模板与 CLI 不扩大当前任务权限。

## 目标配置

目标目录名与 `loader`、`minecraft_version` 完全一致。每个目标的 gradle.properties 提供 java_version（字节码）与 gradle_java_version（运行 Wrapper 的 JDK）、Loader 版本与范围、共享源层、固定构建插件版本与 ci_enabled。没有另一份手工 target map 或独立 ci.properties。

`python temple.py list --json` 是完整目标清单；matrix 仅筛选 CI 启用目标，关闭 CI 不删除或隐藏目标本身。蓝图命令 add-target 拒绝已有目录；Fabric 新版本要求显式匹配的 fabric_api_version。跨混淆边界或 MDK 世代时还需改变构建脚本，而不是只修改版本字符串。

## IDEA 与共享源

select 选择一个完整目标并同步根 Wrapper distribution。重新导入 Gradle 后，common、所选版本与 Loader 源均在该目标类路径中解析。IDE 切换不会改变任何其他目标的依赖版本。也可直接打开 targets 下目标目录。

根 `gradlew build` 构建当前所选目标；未选择时构建全部目标。根 Wrapper 需要匹配的 JDK；跨不同 Gradle 世代的完整批次建议直接使用 `python temple.py build`，它不受根 Wrapper 的运行版本限制。
