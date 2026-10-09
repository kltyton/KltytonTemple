# 迁移现有工程

[English](Migration.en.md) · [Wiki](Home.zh-CN.md)

迁移后，工程采用与从 KltytonTemple 新建相同的组织方式。通用逻辑在 `common` 中只维护一份，所有目标使用同一份源码。版本和 Loader 层只保留必要的 API 实现差异。

## 交给 AI

复制 [迁移提示词](../../../integrations/prompts/migrate.zh-CN.md)，填写原工程、模板、输出位置、允许修改的文件，以及允许执行的验收任务。

[迁移 Skill](../../../integrations/skills/kltyton-temple/SKILL.md) 可由智能体直接读取，不需要额外 MCP。它覆盖官方单加载器模板、MultiLoader、Stonecutter、Architectury 和 SighsTemple。无需把私人会话或全局规则提交进游戏仓库。

## 迁移前确认

列出每个 Minecraft/Loader 组合的 Wrapper、Gradle Java、编译 Java、映射、Loader/API 与构建插件版本。检查 modid、group、Java 包、入口、资源路径、访问配置、Mixin、datagen、运行参数和发布项目 ID。

这些是迁移前后要比对的事实，不能被模板默认值替换。

## 不同模板要保留什么

| 来源 | 迁移时保留的链路 |
| --- | --- |
| 官方 MDK | 对应 Loader 插件、依赖、运行任务、元数据及访问配置 |
| MultiLoader | common 的源码/产物消费者、每个 Loader 的编译环境与资源范围 |
| Stonecutter | 版本节点、常量、依赖替换和预处理到编译的任务依赖，迁入新构建约定 |
| Architectury | common 转换、平台声明、expect/actual、remap 与运行时依赖 |
| SighsTemple | 原目标矩阵、源层优先级、元数据和 Wrapper/Java pins |

Stonecutter 的生成目录不能只加进 sourceSets；生成任务必须仍在编译前执行。Architectury 的转换产物不能用普通源码复制冒充。具体任务名以原工程配置为准。

原模板的功能链路需要保留，原工程壳需要替换。把生产者和消费者接入 `gradle` 构建约定及 `targets` 入口后，清退旧控制工程；不能留两套构建系统继续交叉调用。

## 新入口

先把当前主要开发目标的游戏源码与共享资源迁入 `common/src`，再比较所有目标的实现并查询对应版本源码。确认存在的 API 差异进入版本、Loader 或目标层；抽出小范围 SPI，把差异类里的通用算法与业务逻辑继续归并回 common。不要仅因几处 API 调用不同，就在每个版本复制整个类。

`shared_sources` 指向迁移后的源码层，构建、访问配置、Mixin、生成和发布任务同步更新路径。手写资源不转成 datagen。迁移完成时，应能在 common 修改通用功能而无需重复编辑各版本的同一实现。

`versions` 中每个版本只有一个维护目录，例如 `versions/1.20.1`，不再保留 `versions/ProjectName-1.20.1`。这个版本的不同 Loader 使用同一份版本差异层，各自保留 `targets/<loader>-1.20.1` 构建根。没有差异时不用创建空版本目录。

根任务提供目标构建和默认目标简写。IDEA 将各目标独立关联，各自使用自己的 Wrapper；同步全部工程时不会因为默认目标是 Forge 1.20.1 就忽略其他目标。

## 验收

先完成生产改动，再执行填写的验收合同。只授权源码迁移时不运行 Gradle、测试、datagen 或游戏，但仍须完成源码归并、实际迁移和引用替换。

完成报告分别列出：源码映射、IDE 模块、构建目标、发行包、游戏检查。没有执行的项目明确保留为未验证。对照生成器的文件清单核对源码归属和构建根，并清退被替代的 settings/build、Wrapper、Eclipse 元数据、IDE 关联及旧缓存/生成目录。先核实归属和消费者，不删除用户改动、存档、私有配置或本地依赖；不能把废弃工程改名后留在仓库里。即使旧目录只剩缓存，或所有模块都能显示，结构迁移仍不算完成。
