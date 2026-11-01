# 开发记录

## 2026-11-01

### 需求与工程

- 选题确定为 B4 单词记忆，伴生模块为 P1 桌面小组件。
- 个性化功能包括智能间隔复习、学习统计与进度分析、词本导入导出。
- 采用 Kotlin + XML，创建启动首页，接入 AppCompat 与 ViewBinding。
- 包名为 `com.maxiaol624.wordtrail`，最低支持 Android 8.0。

### 构建问题

命令行默认 Java 为 8，与当前构建工具不匹配，改用 Android Studio 自带的 JBR 25。

首次构建在目录检查阶段失败。检查发现，Java 的 `Files.isWritable` 返回 false，但实际创建和删除临时文件成功。使用正常进程权限重新构建后通过，没有修改目录安全设置。

Android Studio 生成 `gradle-daemon-jvm.properties`，将 Gradle JVM 固定为 Java 25。

### 验证结果

| 检查 | 结果 |
| --- | --- |
| XML 语法 | 6 个文件解析通过 |
| Gradle Wrapper | 返回 Gradle 9.6.0 |
| assembleDebug | BUILD SUCCESSFUL，37 个任务，33 秒 |
| APK 安装 | Pixel 10 模拟器，返回 Success |
| 首页启动 | 冷启动成功，Status: ok，2423 ms |
| 页面检查 | 中文与空状态正常，系统栏未遮挡内容 |
| 启动错误日志 | 本次 AndroidRuntime 错误日志无输出 |

以上测试仅覆盖启动首页。数据库、学习、复习和小组件尚未实现。

### 仓库同步问题

仓库保留原始初始化提交，采用普通合并接入工程。

首次推送遇到凭据组件问题。完成 Android Studio 的 GitHub 登录后，又发现 IDE 自动检测的 `git.exe` 不存在，补充实际 Git 路径后，提交记录恢复显示。

随后上传出现 `Recv failure: Connection was reset`。对本仓库的 GitHub 地址配置已有本地代理后上传成功，核对远程与本地 HEAD 一致。

### 文档整理

README 改为项目介绍、开发进度、技术方案和运行步骤。其余文档统一整理为需求、排期、环境和问题记录，补充已解决问题的最终结果。

### 后续事项

- 完成页面导航与数据模型设计。
- 接入 Room 并验证数据重启保存。
- 保持阶段记录与实际完成内容一致。
