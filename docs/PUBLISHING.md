# 发布

构建先生成每个目标的实际 production JAR，`verifyDistribution` 检查共享类与元数据，写入 build/temple/artifact.properties。`python temple.py verify --target <目标> --json` 再检查真实 ZIP、身份、入口、版本范围、字节码和哈希。

CLI 发布默认只返回调用计划：

```powershell
python temple.py publish --target fabric-1.20.1 --platform modrinth --json
```

明确获准上传后才添加 `--execute`；JSON 执行同时提供 `--log-dir`。平台可单选 modrinth、curseforge 或 both，不要求只发布一个平台的用户提供另一个平台凭据。

| 平台 | 环境变量 |
| --- | --- |
| Modrinth | MODRINTH_PROJECT_ID、MODRINTH_TOKEN |
| CurseForge | CURSEFORGE_PROJECT_ID、CURSEFORGE_TOKEN |
| Maven | TEMPLE_MAVEN_URL、MAVEN_USERNAME、MAVEN_PASSWORD |

非敏感平台项目 ID 也可填根 gradle.properties 中 publish_modrinth_project_id 或 publish_curseforge_project_id。token、账号密码和本机发布配置不写入仓库。

没有默认远程 Maven 地址。Fabric Maven publication 与模组上传使用同一个 remapJar，旧 Forge 使用 reobfJar，新 NeoForge 与未混淆 Fabric 使用实际生产 JAR；sources JAR 是额外源码产物。模组 publication 不默认包含开发类路径依赖；需要消费方 Maven 依赖时应在其 POM 中显式声明，Loader mod 依赖仍写在目标元数据。

平台权限、发行包可再分发内容与版本应在发布前确认。模板构建成功不证明远端发布、审核或公网下载成功。
