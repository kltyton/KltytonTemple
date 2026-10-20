# 迁移到 KltytonTemple

把下面的提示词交给能读取本地工程的 AI，填写路径、修改范围和验收合同。

```text
请将现有 Minecraft 模组工程完整迁移到 KltytonTemple，在模板结构中继续开发。
通用逻辑只在 common/src/main 中维护一份，真实 API 差异在 common/src/compat 中用普通 Java 实现。
最终工程要与使用相同目标矩阵、从 KltytonTemple 新建的工程具有相同组织方式。
不要在原模板旁边另加一套入口，也不要让新入口继续指向旧工程树。

原工程：<绝对路径>
KltytonTemple：<绝对路径或固定提交链接>
输出位置：<原地迁移或新目录>
允许修改：<具体文件或目录>
验收合同：<目标、任务、次数和禁止项>

先读取工程规则、Git 状态、settings/build/gradle.properties、Wrapper、
Loader 元数据、源码层、访问配置、datagen、运行和发布配置。
确认所有版本、加载器、Java、映射、包名及 modid，不凭目录名推断。
确认原项目的 Gradle DSL，以及输出工程选择 Groovy 还是 Kotlin DSL。
构建入口统一到所选 DSL；同一目录不能同时保留 build.gradle 和 build.gradle.kts，
settings 也只保留一份。脚本语法迁移保留任务、依赖与插件版本，不改变模组源码语言。

识别原工程使用官方 MDK、MultiLoader、Stonecutter、Architectury、
SighsTemple，还是这些方案的组合。
读取 KltytonTemple 的 integrations/skills/kltyton-temple/SKILL.md，
再读取其中匹配原工程的迁移参考。
读取模板的生成器和构建约定，确定新建工程会保留哪些文件、源码层和 Gradle 根。
只需源码迁移时，从生成器配置推导这份清单，不运行生成任务或另建测试工程。
逐项记录旧文件的用途、新位置、消费者和旧位置如何清退。

完成以下迁移：
- 保留每个已有目标的 Wrapper、运行/编译 Java、映射、Loader/API
  和构建插件版本，不顺手升级或添加发布目标。
- 接入原生 Gradle 任务，让全部目标独立关联到 IDEA；
  选择目标同时确定根级任务和 common 的 IDE 编辑环境，重新加载 Gradle 模型后生效。
- 保留游戏源码、资源、注册、网络、Mixin/refmap、AT/AW/ClassTweaker、
  datagen、可选依赖、运行配置及发布链路。保留代码的内容和行为，不保留重复工程。
- 先以当前主要开发目标为基准，将游戏源码和共享资源归入 common/src/main。
  比较所有现有版本和 Loader 的实现，并查询对应版本源码，确认真实 API 差异。
  只有这些差异进入 common/src/compat 的对应适配位置；路径以 common 构建脚本为准。
  按注册、网络、渲染等功能分包，没有差异就不建目录。
- 对存在差异的类继续拆分：算法、业务规则、状态和资源处理回到 common；
  通过小范围、类型明确的 SPI 留下 API 调用、类型和生命周期的具体实现差异。
  不把几处 API 不同的整个大类复制到各版本，也不只按文件哈希判断能否共用。
- 提取 SPI 时列出原调用的输入、输出和消费者，逐项保留数据与副作用。
  渲染适配保留原生材质、颜色、光照、法线及图层标识；存档和网络适配保留字段及往返语义。
  不用默认值掩盖丢失的数据，不因公共算法完成就漏掉平台消费者。
- 保留主要开发目标作为默认编辑目标，不把它无条件改成矩阵第一项。
  common 定义 templeSourceDirectories、templeResourceDirectories 后接入 ide-sources.gradle；
  保留原有的有效适配层计算，让扩展按实际模型分配其他 compat 目录的源码归属和 SDK。
- 让各目标包含真实的 :common 模块，共享依赖在 common/build.gradle(.kts) 声明。
  当前编辑目标的 :common.projectDir 必须映射到物理 common 目录；
  共享 Java 和资源根只登记一个 IDE 所有者，不以显示名称或脚本文件存在代替模块接入。
  所有目标使用同一份源码，common 使用各目标自己的 Minecraft/Loader 编译类路径；
  更新源码、资源、AT/AW/ClassTweaker、Mixin、生成任务和发布任务的路径与依赖。
  将各目标剩余的相同实现继续归并，直到通用功能无需逐版本重复修改。
- 生产源码只在 common 中维护，不保留平行的 versions、loaders 或旧版本工程树。
  同一份版本 API 适配由对应版本的所有 Loader 共用，Loader 适配也不复制公共算法。
  targets 下每个 Loader/Minecraft 组合只有一个构建根，只放构建、元数据和必要目标内容。
- 将 Stonecutter 的版本条件注释、常量替换和分支转换成普通 Java 适配与目标依赖配置。
  不保留条件注释或源码预处理链；逐分支保留原有行为、输入、输出和副作用。
  Architectury 的必要转换、expect/actual 与运行时依赖仍须保留完整消费者语义，
  接入规范的 common/target 构建；清退旧控制工程，不保留第二套工程。
- 保留 modid、group、包名、入口类和版权，不留下示例命名空间、
  模板 modid 或模板平台项目 ID。
- 新目录拒绝覆盖；原地迁移保留用户改动、私有配置、存档和本地依赖。
  不创建未要求的备份。
- 核实文件归属后清退旧工程壳：旧 settings/build、Wrapper、模块描述、
  .project、.classpath、.settings、旧 IDE 关联、旧 build/bin/.gradle 和生成目录。
  只清退被本次迁移替代的文件。现有游戏代码和人工资产先迁入正确位置；
  用户存档和私人配置先确认维护位置，不能随旧工程壳删除。
  不把废弃文件改名为 old、legacy 或 backup 继续塞在工程内。
- 先完成生产迁移，再严格按验收合同执行检查。
  source-only 阶段不运行 Gradle、测试、datagen 或客户端，但仍须完成实际源码迁移与归并。
- 报告目标矩阵、原路径到新入口的映射、改动文件、实际命令和结果。
  分开报告源码完成、IDE 导入、构建、发行包及游戏验收。
  对照新建工程清单检查目录、源码所有者、Gradle 根、IDE 关联及旧路径引用。
  检查 common 的真实调用者，以及版本/Loader 层剩余的差异及理由。
  旧源码已迁移但旧缓存目录仍在、两套版本目录并存、通用类仍整份复制、
  旧控制工程仍被编译任务调用，均表示迁移未完成；模块可见不能代替这些检查。

遇到能力缺口，先指出具体调用链或任务依赖，再做最小适配。
不以删功能、吞异常、禁用依赖或只改版本字符串完成迁移。
提交、推送、PR、外部发布和联系其他智能体仅按本次明确授权执行。
```
