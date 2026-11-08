# 词本导入与导出

打开词本，点「导入与导出」，选择导入或导出。系统文件选择器负责选取文件或保存位置。导入写入当前词本，导出包含当前词本的词条与扩展字段。导入后的词条可参与新词学习与测试。参见 [Android 文档选择器说明](https://developer.android.com/training/data-storage/shared/documents-files)。

文件使用 UTF-8，支持 BOM。首行需要列名 `word` 和 `meaning`，可加 `phonetic`、`example` 和 `exampleTranslation`。列的顺序可调整，列名不区分大小写，每条记录的列数应与表头一致。字段里有英文逗号、双引号或换行时，用双引号包住整个字段，字段内的双引号写成两个。参见 [CSV 格式说明](https://www.rfc-editor.org/rfc/rfc4180)。

```csv
word,meaning,phonetic,example,exampleTranslation
apple,苹果,/ˈæpəl/,I ate an apple.,我吃了一个苹果。
say,"说, 表达",/seɪ/,"She said ""hello"".",她说了“你好”。
```

导入时去除字段首尾空白，按词本跳过重复拼写，英文大小写视为同一词。空白行会跳过，缺少必填值、列数不符或字段超长的记录计为无效。表头或引号结构有误时，需要修正文件再导入。单次导入最多 20,000 条记录和 4,194,304 个字符。

导出使用带 BOM 的 UTF-8，包含全部五列。也可以导出空词本获得模板。文件范围为词条，学习反馈、测试成绩和复习安排保存在应用数据库中。

数据库版本 5 增加音标、例句和翻译三个字段，旧词条升级后默认留空。应用先解析整份文件，再用 Room 批量写入有效词条，并利用原有的唯一索引跳过重复记录。
