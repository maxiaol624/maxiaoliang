# 词迹 WordTrail

《Android 移动终端应用开发技术》与《移动互联网应用开发实战》课程设计。

选题为单词记忆，伴生模块为桌面小组件。主要围绕新词学习、间隔复习和学习进度记录展开，计划支持通过词表文件管理自己的单词本。

## 开发进度

从首页进入单词本，可以新建词本、添加单词并查看释义。词本和词条保存在本地 Room 数据库中。

在单词本里选择「开始新词学习」，可以逐张查看单词卡片。显示释义后选择掌握程度，应用保存反馈和学习时间，然后切换到下一张。

新词学完后，可选择「再练一遍」翻看本词本的已学单词。巩固练习保留原有的学习记录。

在词本中选择「到期复习」，按到期时间复习已学单词。首次学习的三个反馈对应 10 分钟、1 天和 3 天。复习时会根据新反馈调整间隔，暂无到期词时会显示下次复习时间。

首页的「查看学习统计」展示今日新学词数、复习次数、连续学习天数、待复习词数和近 7 天趋势。还可以查看各词本的已学比例、待复习数量及当前复习阶段分布，从进度卡片直接打开词本。统计读取实际学习记录，巩固练习不重复计数。

词本中的「单词测试」采用四选一，每轮从当前词本随机抽取最多 10 个单词，至少需要 4 种不同释义。答题后显示正确释义，中途退出可继续。完成整轮后保存成绩，可以从最近成绩重看错题。学习统计展示完成轮数和累计测试正确率。

后续开发内容：

- CSV 词表导入与导出。
- 每日复习提醒及通知权限处理。
- 桌面小组件，显示今日任务并提供学习入口。

## 技术方案

界面使用 Kotlin、XML、AppCompat 和 ViewBinding。词本页面通过 ViewModel 读取 Room 数据，数据库变动后列表会自动更新。提醒和小组件按后续计划接入。

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
- [学习统计的计算规则](docs/statistics.md)
- [单词测试的流程与保存](docs/quiz.md)
