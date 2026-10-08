# Agent 接口

入口是项目根目录 temple.py；Python 3.11+，标准库，无 daemon。每个命令的 --json 输出可直接解析，错误写 stderr 并退出 2；构建失败返回 1 并提供全部已执行目标的真实退出码。

| 操作 | 命令 |
| --- | --- |
| 列出全部目标 | python temple.py list --json |
| CI 子集 | python temple.py matrix --json |
| 静态边界与本机 JDK | python temple.py doctor --json |
| 读取既有工程声明 | python temple.py inspect <path> --json |
| 查询目标 IDEA 目录 | python temple.py ide --target <id> --json |
| 选择根 IDE 目标 | python temple.py select --target <id> --json |
| 准备构建调用 | python temple.py build --target <id> --plan --json |
| 批量构建 | python temple.py build --log-dir <logs> --json |
| 检查真实 JAR | python temple.py verify --target <id> --json |
| 发布计划 | python temple.py publish --target <id> --platform modrinth --json |

build 可重复 --target。不存在的目标、不安全的共享路径、重复 Java 源、错误 JDK 和已有初始化目录均返回明确错误。list 不需要 JDK，doctor 不执行 Gradle。build、run、publish 会按命令实际操作，必须受当前用户任务合同约束。发布默认只计划；--execute 会发生远端写入。

MCP 不是必须条件：能读取文件和执行命令的代理可直接使用本接口。提供了 [可分发 Skill](../../integrations/skills/kltyton-temple/SKILL.md)；不要求改变主模型、Provider、全局工具、个人规则或已有技能。
