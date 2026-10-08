# 共享源码与目标隔离

每个目标是独立 Gradle 工程，使用自己的 Wrapper、运行 JDK、编译 JDK、Minecraft 与 Loader。根目录负责管理目标，IDE 一次导入一个所选目标。共享源码作为该目标的 source roots 编译，不是先构建一个 Java 8 common JAR 再塞入全部产物。

`shared_sources` 显式列出从低到高的共享层。默认是 common、versions/<minecraft>、loaders/<loader>，最后加入 target 自身。不含 Loader 的共同算法及 Minecraft 逻辑进入 common；只有同一版本可共用的 API 适配进入 versions；入口与 Loader 协议进入 loaders；同时受版本和 Loader 影响的边界进入 target。版本文件夹按需创建，不生成空包。

共享接口可以含该版本存在的 Minecraft 类型。实现若存在版本差异，给接口和实现分别命名，使每个目标恰好编译一份实现；不要用同名类覆盖、反射或运行时版本猜测替代编译时边界。客户端类仍放客户端边界，共同入口不能加载客户端类。

所有目标从根属性生成 BuildInfo 常量；入口的 @Mod 和元数据由同一个 mod_id 派生。初始化改变 Java 包路径，手工改 group 后也应同步源码包。无需手动逐个修改注解中的字符串。

## 参考方案与取舍

- [MultiLoader 1.20.1 的共享 Java 配置](https://github.com/jaredlll08/MultiLoader-Template/blob/d6b81d85d63566cbe5e67fd2f246f1441cccf686/buildSrc/src/main/groovy/multiloader-loader.gradle) 将 commonJava 源码加入各 Loader 编译任务。这里采用其“在目标环境重新编译共享源”的原则；跨 MC 的目标继续隔离。
- [Architectury Plugin](https://docs.architectury.dev/plugin/introduction/) 提供 common module 与平台转换。需要其 API 的项目可以在对应目标明确引入；本模板不让所有项目被迫增加运行时 Architectury 依赖。
- [Stonecutter 官方源码](https://codeberg.org/stonecutter/stonecutter/src/branch/0.10/README.md) 使用源码注释中的条件、替换和 swap 指令做预处理，并提供活动版本管理。这里采用一个活动目标供 IDE 解析的工作方式，同时用独立版本源层表达较大的 API 差异；没有安装或仿造 Stonecutter。已有注释预处理项目可继续保留其管线。
- [上游 SighsTemple](https://github.com/Tower-of-Sighs/SighsTemple/tree/1183529ad80dfecbaa1a559f9d4c68ce2255007e) 的独立目标、Wrapper 和发布插件保留。其独立 Java 8 common 限制改为目标编译共享源。

不同 Gradle 与 Java 版本不能靠一个同时加载全部插件的根工程兼容。`select` 同步根 Wrapper 配置并只 includeBuild 一个目标，避免 IDE 解析不相干版本；CLI 批量构建则启动各目标的真实 Wrapper。

## 资源与依赖

资源按 target > loader > version > common 覆盖，输出同一文件路径一次。datagen 输出进入 target/src/generated/resources；手工与编辑器导出资源保持原目录，不做无授权迁移。对同一目标的手工与 datagen 重复路径应明确唯一维护方式。

Fabric 旧版本地 mod JAR 使用 modImplementation，新版未混淆环境使用 implementation；本地 JAR 不自动嵌入发行包，也不自动推断传递依赖。需要嵌套依赖、Mixin、AW、AT 或兼容模块时，按当前 Loader 的合法能力单独接入。

Maven、CurseForge、Modrinth 指向同一个 productionTask：优先 remapJar、其次 reobfJar、否则 jar。Maven 模组 publication 是明确的发行文件 publication，不把开发组件或 Minecraft/Loader 的开发类路径作为默认运行依赖发布。
