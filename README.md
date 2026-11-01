# 词迹 WordTrail

《Android 移动终端应用开发技术》与《移动互联网应用开发实战》课程设计。

选题为单词记忆，伴生模块为桌面小组件。主要围绕新词学习、间隔复习和学习进度记录展开，计划支持通过词表文件管理自己的单词本。

## 开发进度

目前完成了 Android 工程搭建和启动首页，可以编译生成 APK，已在 Pixel 10 模拟器上验证安装与启动。

后续开发内容：

- 单词本管理、单词列表与详情。
- 新词学习、掌握程度反馈和到期复习队列。
- 根据学习记录调整复习间隔。
- 单词测试、成绩记录和错题回顾。
- 学习量统计、连续学习天数和词本进度。
- CSV 词表导入与导出。
- 每日复习提醒及通知权限处理。
- 桌面小组件，显示今日任务并提供学习入口。

## 技术方案

基础界面使用 Kotlin、XML、AppCompat 和 ViewBinding。数据库、后台任务等部分将在对应功能开发时接入。

| 部分 | 选用技术 |
| --- | --- |
| 页面状态与异步处理 | ViewModel、协程、Flow |
| 单词与学习记录 | Room |
| 学习目标及提醒设置 | DataStore |
| 后台检查与提醒 | WorkManager、系统通知 |
| 桌面小组件 | AppWidgetProvider、RemoteViews |

## 运行方法

1. 使用 Android Studio 打开仓库根目录。
2. 在 SDK Manager 中安装 Android API 37 平台及所需构建工具。
3. Gradle JDK 选择 Android Studio 自带的 JBR 25，等待项目同步完成。
4. 选择 Android 8.0 及以上版本的模拟器或手机，运行 `app`。

也可以在项目目录执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

命令行构建时，`JAVA_HOME` 需指向兼容的 JDK，不能使用 Java 8。生成的安装包位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 目录结构

```text
WordTrail/
├── app/                  应用代码、布局与资源
├── docs/                 需求、开发计划及问题记录
├── gradle/               Gradle Wrapper 与 JVM 配置
├── build.gradle.kts      项目构建配置
└── settings.gradle.kts   模块及依赖仓库配置
```

## 项目文档

- [需求与验收清单](docs/requirements.md)
- [开发计划](docs/development-plan.md)
- [开发记录](docs/development-log.md)
- [构建环境与验证记录](docs/build-environment.md)
