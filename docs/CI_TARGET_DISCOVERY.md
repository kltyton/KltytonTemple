# CI 目标发现

`python temple.py matrix --json` 直接读取全部目标 gradle.properties，输出 `{"include":[...]}`。每项包括 target、loader、minecraft、java、gradle_java、gradle、path、shared_sources、ci。CI 使用 target、java 与 gradle_java；路径字段是当前运行机器的事实，不用作另一台机器的路径。

工作流同时覆盖 Windows 和 Linux；安装目标运行 JDK 与编译 JDK，调用 CLI，再核对实际发行 JAR。所有生产源层、目标构建脚本、元数据、工具与 Wrapper 改动会进入构建批次。没有空 common test job，没有盲目 clean、没有失败自动重试，也不会把不存在的 JAR 当作成功。

该工作流不会启动游戏或上传到模组平台。Actions 是否实际通过须查看真实运行结果，不能由本地构建推断。
