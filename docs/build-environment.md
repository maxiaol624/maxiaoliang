# 工程构建与环境记录

## 当前工具链

以下版本依据本机已安装组件与已有缓存配置，最小工程已完成编译与模拟器启动验证：

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
- 初次构建失败发生在工程目录验证阶段，尚未进入 Kotlin 或资源编译（历史问题，后续验证见下）。
- Java 检查：目录存在，是目录；Files.isWritable 返回 false；同一进程在该目录实际创建并删除临时文件成功。
- 初次明确授予项目目录写入权限后重试，Gradle 仍报 projectDirectory 不可写。此项记录保留，不代表当前构建结论。

## 2026-11-01：构建与启动验证通过

- 用户在 Android Studio 打开并识别工程，IDE 生成 Gradle Daemon JVM 配置，固定 Java 25。
- 使用正常进程权限与已有缓存运行 gradlew.bat --offline --console=plain :app:assembleDebug。
- 结果：BUILD SUCCESSFUL，37 个任务执行，耗时 33 秒。无需修改目录安全设置。
- 生成 app/build/outputs/apk/debug/app-debug.apk，大小 3,096,998 字节。
- 后台启动现有 Pixel_10 AVD，adb install -r 返回 Success。
- am start -W 返回 Status: ok、LaunchState: COLD、TotalTime: 2423ms。
- 检查实际首页截图：中文、卡片、空状态及系统栏未见遮挡；本次 AndroidRuntime 错误日志无输出。
- 这只验证当前启动骨架，不等于学习、数据库或小组件已完成，也不代表所有机型均已覆盖。

## 命令行（Windows）

在项目目录的 PowerShell 中运行：

```powershell
$env:JAVA_HOME = 'E:\android-stuido\jbr'
.\gradlew.bat :app:assembleDebug
```

成功后安装包位于 app/build/outputs/apk/debug/app-debug.apk。本次已生成，并在 Pixel 10 模拟器中验证安装与启动。
