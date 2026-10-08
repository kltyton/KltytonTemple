# KltytonTemple

基于 [SighsTemple](https://github.com/Tower-of-Sighs/SighsTemple) 的 Minecraft 多版本、多加载器开发模板。保留 MIT 许可与上游版权。模板工具使用 Python 3.11+ 标准库，无需额外 Python 依赖，也没有运行时模板库。

`common` 可以直接使用 Minecraft API：共享源码在每个目标自己的 Minecraft、映射、Loader 和 Java 环境内重新编译。默认提供 Forge 1.18.2 / 1.19.2 / 1.20.1，Fabric 1.20.1 / 1.21.1 / 26.1.2，NeoForge 1.21.1 / 26.1.2 / 26.2.0。实际版本、依赖和 JDK 以目标 `gradle.properties` 与 Wrapper 为准。

## 从模板创建项目

```powershell
python temple.py init G:/projects/MyMod --mod-id my_mod --name "My Mod" --group io.github.yourname.mymod --authors yourname --target forge-1.20.1 --target fabric-1.20.1
```

目标目录必须不存在。初始化同时配置模组 ID、元数据、Java 包路径、Gradle group 与作者；没有指定 `--target` 时包含模板的全部目标。不会复制 Git 历史、IDE/运行/构建缓存、本地配置、私有 libs JAR 或本地代理记录。

## 在 IDEA 中开发

```powershell
python temple.py select --target forge-1.20.1 --json
```

然后在 IDEA 打开项目根目录或刷新 Gradle。根目录只导入所选目标的 composite build，共享源码直接作为该目标的 source roots，因此可编辑并获得该版本的类型解析。切换目标后重新加载 Gradle，并按输出设置 Gradle JVM；`select` 会把根 Wrapper 的 distribution 配置同步到该目标的已固定版本。

也可以直接打开目标目录；`python temple.py ide --target fabric-1.21.1 --json` 返回目录与 JDK，不修改选择。

## 构建与运行

```powershell
python temple.py list --json
python temple.py doctor --json
python temple.py build --target forge-1.20.1
python temple.py build
python temple.py verify --target forge-1.20.1 --json
python temple.py run --target forge-1.20.1 --side client
```

`build` 未指定目标时构建全部目标，各自调用自己的 Wrapper；一个目标失败后仍构建其余目标，最后汇总真实退出码。每次不自动 clean、不自动重试。运行客户端或服务器必须显式指定一个目标。命令的 `--plan` 不执行 Gradle。

Gradle 运行 JDK 与游戏字节码 JDK 分开配置。设置 `JAVA_21_HOME`、`JAVA_25_HOME`，旧版编译还需 JDK 17。也可以在忽略的 `temple.local.properties` 中填写：

```properties
java.17.home=F:/Java/jdk17
java.21.home=F:/Java/jdk21
java.25.home=F:/Java/jdk25
gradle_user_home=E:/.gradle
temp_dir=E:/work-temp/MyMod
project_cache_dir=E:/work-cache/MyMod
```

路径使用正斜杠。未设置临时目录时 CLI 使用项目内 `.temple/tmp`；它只影响本次进程和子进程。JSON 构建请提供 `--log-dir`，使 stdout 保持可解析。

## 代码与资源归属

```text
common/src/                  跨版本和加载器共享的源码、资源
versions/<mc>/src/           同一 MC 版本的共享差异，按需创建
loaders/<loader>/src/        Loader 入口及其共享适配
targets/<loader>-<mc>/       独立 Wrapper、依赖、元数据和交叉差异
gradle/target-conventions/   构建与发布约定
tools/temple/                目标管理、初始化、构建调用与 JAR 检查
integrations/skills/         可分发的接入 Skill
```

Java 源码合并，不进行同名类覆盖；重复路径由 doctor 报错、编译器也会拒绝。资源覆盖顺序是 target > loader > version > common，输出每条路径仅保留一个文件。Loader 元数据留在 target；人工资源仍放 src/main/resources，datagen 结果放目标 src/generated/resources。

## 增加目标

```powershell
python temple.py add-target --from fabric-1.21.1 --minecraft 1.21.4 --loader-version 0.16.14 --java 21 --gradle-java 21 --property fabric_api_version=填入匹配版本
```

显式选择已有目标作为蓝图，提供真实依赖版本；CLI 不猜测“最新版”。新版本仍需核对 Loader、插件、映射、Java 与 API 差异并按项目合同构建。创建目录不等于宣称该版本已兼容。旧版 1.7.10、1.12.2 等不同 MDK 不应通过更改版本号假装迁移成功。

## 接入智能体与发布

Skill 位于 [integrations/skills/kltyton-temple/SKILL.md](integrations/skills/kltyton-temple/SKILL.md)，可由支持 GitHub 路径安装的 Skill 安装器安装，或由用户复制到其技能目录。它调用同一 CLI 的 JSON 接口；无需常驻 MCP。仓库内 Skill 是正式产品，私人会话规则仍留本地。

[架构与选型](docs/dev/architecture.md) · [操作与迁移](docs/MAINTENANCE_WORKFLOW.md) · [Agent 接口](docs/dev/agent-interface.md) · [发布](docs/PUBLISHING.md) · [CI](docs/CI_TARGET_DISCOVERY.md)

上游 [版本差异资料](docs/version-differences/README.md) 保留为参考材料；它不是本模板的目标支持或验收清单。
