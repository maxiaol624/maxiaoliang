# 复习提醒

从首页进入「复习提醒」。开关默认关闭，初始时间为 20:00。可以先调整时间，再开启提醒。Android 13 及以上系统在点击「允许通知」时请求权限；如果拒绝，页面说明提醒暂时无法送达，并提供系统通知设置入口。词本内的「到期复习」仍可使用。

## 后台检查

1. DataStore 保存开关、时间和最近发送日期。时间按照手机本地时区解释。
2. 开启后，WorkManager 使用一个名为 `daily_review_reminder` 的周期任务。设置改变时更新同一个任务。收到系统时间或时区变化广播时，重新计算排程。
3. 工作执行时读取最新设置，检查是否达到设定时间、当天是否已发、系统是否允许通知。有条件查询 Room 中到期时间小于等于当前时间的单词，选择最早到期单词所在的词本，并显示该词本的待复习数量。
4. 通知点击后进入对应词本的到期复习。成功提交通知后记录日期，避免同日重复提醒。没有到期词或通知被禁用时跳过发送，继续安排下一天的检查。
5. 关闭提醒时取消后台任务和已显示的提醒。更新或关闭提醒不会改动学习记录或复习时间。

WorkManager 允许为周期任务指定下一次运行时间，因此下次检查按本地日期计算，不直接累加 24 小时。省电、系统调度和进程状态可能推迟执行。应用从最近任务划掉后，后台任务仍由系统管理；在系统设置中强制停止应用后，需要重新打开应用才能恢复调度。参见 [WorkManager 周期任务说明](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest.Builder#setNextScheduleTimeOverride(long))。

## 关键实现

- `ReminderSchedule` 计算本地检查时间，处理日期、时区与夏令时。
- `ReminderCoordinator` 管理唯一任务、查询到期词和记录发送日期。设置修改和后台检查通过同一个锁协调。
- `ReviewReminderWorker` 执行后台任务，暂时读取失败时可重试。
- `ReviewNotifications` 创建通知渠道并设置点击后的页面路径。通知权限处理参考 [Android 通知权限说明](https://developer.android.com/develop/ui/views/notifications/notification-permission)。
- `ReminderActivity` 展示设置、当前权限状态和手动复习入口，使用 `ReminderViewModel` 异步保存设置。

本次增加到期词本查询，Room 数据库仍为版本 4。单元测试覆盖禁用、提前或延迟执行、同日去重、次日排程和夏令时。模拟器操作结果见开发记录。
