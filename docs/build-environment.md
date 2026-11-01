# 工程构建与环境记录

## 当前工具链

以下版本依据本机已安装组件与已有缓存配置，尚未完成应用编译验证：

- Android Studio Rabbit 2 | 2026.2.2。
- Android Studio 自带 JBR：E:/android-stuido/jbr，Java 25.0.4。
- Gradle：9.6.0。
- Android Gradle Plugin：9.5.0，使用其内置 Kotlin 支持，未重复应用 Kotlin Android 插件。
- compileSdk / targetSdk：37；minSdk：26（Android 8.0）。
- SDK：C:/Users/llqiang4/AppData/Local/Android/Sdk。
- AppCompat：1.6.1；暂用本机缓存版本完成最小骨架，后续按实际兼容性升级。

以上本机绝对路径不写入可提交构建配置。local.properties 已被忽略。包名暂定 com.maxiaol624.wordtrail。

## 在 Android Studio 打开

1. Open 选择 WordTrail 文件夹，不选外层聊天目录或 app 子目录。
2. 在 Gradle 设置中选择 Android Studio 自带的 JBR，避免使用系统 PATH 中的 Java 8。
3. 等待同步，若提示 SDK 位置，选择本机 SDK。
4. 运行 app 到 Android 8.0 以上模拟器或真机。

目前只有启动首页和空词本提示，不包含数据库、导航、复习或小组件；不能作为完整课设提交。

## 已做验证

- Wrapper JAR 从本机 Gradle 9.6.0 分发包内提取，启动脚本随工程提供。
- 已通过工程自带 gradlew.bat --version 验证启动脚本，返回 Gradle 9.6.0 与 JBR 25.0.4。
- 6 个 XML 文件通过语法解析；Wrapper JAR 入口存在。这些检查不能代替 Android 构建与运行。
- 最小工程文件、资源、启动 Activity 和 ViewBinding 已创建。
- 构建命令已实际尝试，但失败发生在工程目录验证阶段，尚未进入 Kotlin 或资源编译。
- Java 检查：目录存在，是目录；Files.isWritable 返回 false；同一进程在该目录实际创建并删除临时文件成功。
- 明确授予项目目录写入权限后重试，Gradle 仍报 projectDirectory 不可写。尚未在用户直接启动的 Android Studio 环境验证。
- 不修改目录安全设置，不把本次失败标记为构建成功。下一步优先在 Android Studio 同步与构建，取得真实报错后继续处理。

## 命令行（Windows）

在项目目录的 PowerShell 中运行：

```powershell
$env:JAVA_HOME = 'E:\android-stuido\jbr'
.\gradlew.bat :app:assembleDebug
```

成功后安装包预计位于 app/build/outputs/apk/debug/app-debug.apk。当前未生成并验证该安装包。
