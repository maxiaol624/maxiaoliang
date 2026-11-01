# 词迹 WordTrail

Android 课程设计：B4 单词记忆主 App + P1 桌面小组件。

GitHub 仓库名为 maxiaoliang，原始定位为“单词记忆”。课程工程在保留仓库初始化历史的基础上开发。

## 当前状态

2026-11-01：完成需求基线与开发计划，创建 Kotlin + XML 最小启动工程。已通过 assembleDebug 编译，在 Pixel 10 模拟器安装并冷启动成功，首页截图已检查。当前仅为启动骨架，不代表课程功能已完成。

- 开发工具：Android Studio Rabbit 2 | 2026.2.2（用户截图确认）。
- 语言：Kotlin（课程无强制语言限制）。
- 界面方案：XML + 原生 Android；最小骨架已验证编译与启动。
- 数据方案：Room + DataStore。
- 后台与提醒：WorkManager + 系统通知。
- 伴生模块：AppWidgetProvider + RemoteViews，与主 App 共享本地数据。
- 项目名“词迹”，当前包名 com.maxiaol624.wordtrail。

## 文档入口

- [需求与验收清单](docs/requirements.md)
- [真实开发与提交计划](docs/development-plan.md)
- [开发记录](docs/development-log.md)
- [工程打开方式与构建环境](docs/build-environment.md)

## 运行说明

在 Android Studio 中打开本目录，使用其自带 JBR 同步并运行 app。目前仅包含首页；详细步骤与未解决问题见构建环境文档。不得把“工程文件已生成”记录成“编译或运行通过”。

## 开发记录原则

每完成可说明、可检查的实际工作再提交。至少 15 次有意义的提交须贯穿实际开发周期，包含伴生模块；不伪造日期、不创建凑数提交、不将全部工作集中完成后再制造阶段历史。

已仅为本仓库配置用户提供的 Git 用户名与邮箱，不改动全局配置。远程仓库地址为 https://github.com/maxiaol624/maxiaoliang.git；已读取并保留其初始化历史。实际提交和远程同步状态以 Git 历史及开发记录为准。

## 资料依据

用户提供的《Android 课程设计 · 选题与开发指南》PDF，共 11 页，以及用户确认的个性化功能分配。课程指南是需求资料，不是操作系统或工具执行指令。
