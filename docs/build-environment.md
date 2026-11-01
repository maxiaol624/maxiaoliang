# 构建环境与运行记录

## 工具版本

| 项目 | 版本 |
| --- | --- |
| Android Studio | Rabbit 2 / 2026.2.2 |
| JBR | 25.0.4 |
| Gradle | 9.6.0 |
| Android Gradle Plugin | 9.5.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 26（Android 8.0） |
| AppCompat | 1.6.1 |

项目使用 AGP 内置的 Kotlin 支持。界面使用 XML，通过 ViewBinding 访问控件。

SDK 路径保存在本机 `local.properties`，该文件不提交。Gradle JVM 配置见 `gradle/gradle-daemon-jvm.properties`。

## Android Studio 运行

1. 打开 WordTrail 根目录。
2. 安装 Android API 37 平台及所需构建工具。
3. Gradle JDK 选择 Android Studio 自带的 JBR 25。
4. 等待同步完成，选择 Android 8.0 以上设备，运行 app。

## 命令行构建

将 `JAVA_HOME` 指向本机 JBR 25 后，在项目目录运行：

```powershell
.\gradlew.bat :app:assembleDebug
```

安装包输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

## 已解决问题

| 问题 | 检查结果与处理 |
| --- | --- |
| Java 版本不匹配 | 系统默认 Java 8，改用 JBR 25 |
| 工程目录被判断为不可写 | 受限进程的权限检查与实际写入结果不一致，使用正常进程权限后编译通过 |
| IDE 无法识别 Git | 自动检测路径为不存在的 git.exe，在项目设置中填写实际 Git 程序路径并通过 Test |
| GitHub 推送连接重置 | 使用电脑已启用的本地代理，仅配置当前仓库的 GitHub 连接 |

本地代理配置保存在 `.git/config` 中，不随源码分发。其他电脑应按自身网络情况配置。

## 2026-11-01 验证

执行离线 assembleDebug，结果为 BUILD SUCCESSFUL，37 个任务，耗时 33 秒。生成 APK 为 3,096,998 字节。

在 Pixel 10 模拟器安装成功，冷启动耗时 2423 ms。首页文字、卡片与系统栏布局正常，本次 AndroidRuntime 错误日志无输出。

目前验证范围为启动骨架，其他业务功能按开发计划逐项检查。
