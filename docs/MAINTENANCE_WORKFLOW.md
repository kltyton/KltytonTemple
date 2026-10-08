# 开发与迁移

目标操作使用原生 Gradle 任务；命令与参数见 README。IDEA 安装及向导步骤见 docs/wiki/Minecraft-Development.zh-CN.md。

1. 读取原工程 settings/build/gradle.properties、元数据、源层、Mixin/AT/AW 与依赖。
2. 保留版本/Loader/Java/Wrapper pins 和游戏行为，把共享源码与资源迁入 common；查询源码确认差异后，通过 SPI 分离必要的版本和 Loader 实现，继续把共用算法归并回 common。目标事实以 targets/<id>/gradle.properties 为准。
3. 启用 IDEA 扩展的独立目标关联；默认目标只影响根任务，不限制导入矩阵。
4. 先完成授权生产批次。已有验收合同未授权时，不点构建、datagen、客户端或服务器任务。
5. 构建成功、真实 JAR、客户端/服务器、玩法/视觉和远端发布分别报告。

createProject 拒绝已有目录并过滤本机配置、Git/IDE/运行缓存、私人记录和本地 libs。addTarget 从官方目录选择依赖。涉及不同 MDK 世代时，保留相应构建管线，新增组合仍需实际构建验收。

通用功能在 common 修改一次，所有目标使用同一实现。差异层只处理实际 API 差异，不复制完整业务类。接入根任务、更新源码与资源引用后，检查剩余重复逻辑和旧工程消费者，再清理失效文件。保留人工/编辑器资源的维护方式；提交、推送和平台上传仍需对应用户授权。
