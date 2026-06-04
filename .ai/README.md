# DogInventory AGENTS 指南

本文件定义 AI 代理与协作者在本仓库中的统一工作规范。DogInventory 现阶段按独立 Android 项目维护，不再以任何外部项目作为功能、交互或视觉基准。发生冲突时，以用户最新指令、当前仓库代码和本文件为准。

## 1. 项目定位

- 项目类型：原生 Android 应用。
- 技术栈：Kotlin、Jetpack Compose、Navigation Compose、Room、KSP、Material 3。
- 最小假设：当前仓库代码就是实现真相，不依赖历史迁移背景。
- 默认目标：在不破坏现有行为的前提下，持续提升功能完整性、可维护性、一致性与稳定性。

## 2. 构建环境

- JDK 版本：JDK 21.0.2（或 JDK 17 及以上版本）。
- Gradle 版本：Gradle 8.14（通过 Gradle Wrapper 管理）。
- 编译命令：`./gradlew :app:assembleDebug`。
- 确保 `JAVA_HOME` 环境变量指向正确的 JDK 安装路径。

## 3. 核心原则

- 先读后改：修改前先阅读相关代码、状态流、数据流和入口。
- 小步精改：优先做最小且正确的改动，避免无边界重构。
- 全链路一致：数据模型变更必须同步更新 UI、持久化、业务逻辑、提醒/备份/同步等相关链路。
- 不猜需求：用户未明确要求的新功能、新页面、新字段，不主动发明。
- 保持风格：遵循现有命名、目录结构、状态管理方式和 UI 模式。
- 明确差异：如果保留了已知限制、技术债或有意行为差异，需在最终说明中写清楚。

## 4. 代码事实来源

处理任务时按以下优先级建立事实：

1. 用户当前指令。
2. 当前仓库源码与测试。
3. 本文件约束。
4. 构建、运行、静态检查结果。

禁止将外部项目、通用模板或习惯性 Material 默认行为当作本项目事实来源。

## 5. 目录与关键入口

高价值入口文件：

- 应用入口：`app/src/main/java/com/doginventory/MainActivity.kt`
- 启动页（含深度链接解析）：`app/src/main/java/com/doginventory/SplashActivity.kt`
- 根导航：`app/src/main/java/com/doginventory/ui/MainScreen.kt`
- ViewModel 工厂：`app/src/main/java/com/doginventory/ui/ViewModelFactory.kt`
- 主题颜色：`app/src/main/java/com/doginventory/ui/theme/Color.kt`
- 主题定义：`app/src/main/java/com/doginventory/ui/theme/Theme.kt`
- 主题常量：`app/src/main/java/com/doginventory/ui/theme/AppDefaults.kt`
- 数据库：`app/src/main/java/com/doginventory/data/AppDatabase.kt`
- DAO：`app/src/main/java/com/doginventory/data/dao/InventoryDao.kt`
- 仓库层：`app/src/main/java/com/doginventory/data/repository/InventoryRepository.kt`
- AndroidManifest：`app/src/main/AndroidManifest.xml`

主要功能目录：

- 存货：`app/src/main/java/com/doginventory/ui/inventory/`
- 待买：`app/src/main/java/com/doginventory/ui/shopping/`
- 设置：`app/src/main/java/com/doginventory/ui/settings/`
- 提醒：`app/src/main/java/com/doginventory/reminder/`
- 备份：`app/src/main/java/com/doginventory/backup/`
- WebDAV：`app/src/main/java/com/doginventory/webdav/`
- 权限：`app/src/main/java/com/doginventory/permission/`
- 设置存储：`app/src/main/java/com/doginventory/settings/`
- 分享客户端（OkHttp + `org.json`）：`app/src/main/java/com/doginventory/share/`

后端 / 工具链：

- `worker/`：Cloudflare Workers + D1 的分享链接后端，**与 `app/` 平级且不进入 Gradle**。通过 `wrangler.toml.example` + gitignored `wrangler.toml` 管理真实配置。
- 计划文档：`.claude/plans/`

## 5.1 分享功能的关键文件

Android 端：

- `app/src/main/java/com/doginventory/share/ShareApiClient.kt` — OkHttp 封装，对照 `webdav/WebDavClient.kt` 模式
- `app/src/main/java/com/doginventory/share/ShareService.kt` — `suspend` + `withContext(Dispatchers.IO)`
- `app/src/main/java/com/doginventory/share/ShareModels.kt` — DTO + 手写 `org.json` 序列化，对照 `backup/BackupModels.kt` 模式
- `app/src/main/java/com/doginventory/share/InventoryShareViewModel.kt` + `InventoryShareDialog.kt` — 分享方 UI
- `app/src/main/java/com/doginventory/share/SharedInventoryImportViewModel.kt` + `SharedInventoryImportDialog.kt` — 接收方 UI
- `app/src/main/java/com/doginventory/data/repository/InventoryRepository.kt` 的 `insertItemsFromShare(...)` — 导入逻辑，套 `withBatchedAutoSync("share_import")`

深度链接与唤起协议：

- AndroidManifest 的 `SplashActivity` 注册 **两个** intent-filter：
  - `https` + `host="<实际域名>"` + `pathPrefix="/s/"`（用于直接从浏览器地址栏打开 https 链接）
  - `doginv` + `host="share.com"` + `pathPrefix="/s/"`（用于落地页 iframe 自动唤起，绕过 Chrome 对 `intent://` 的拦截）
- 分享 UI 始终按「当前筛选视图」预填，可选「包含所有存货」「包含提醒规则」开关

后端：

- `worker/src/index.ts`、`api.ts`、`shareHtml.ts`、`cleanup.ts`
- 落地页用 `doginv://share.com/s/{id}` 唤起（**不要改回 `intent://`**，会因 host 不匹配被回退到 Play Store）
- D1 表：`shared_lists` / `shared_categories` / `shared_items` / `share_rate`
- `share_id` 必须是 8 字符 Crockford base32（`generateShareId`），不要改回 5 字符版本（`isValidShareId` 会拦截，GET 直接 404）

配置三处必须保持一致（域名一致、scheme 协议对应）：

1. `app/build.gradle.kts` 的 `buildConfigField("String", "SHARE_BASE_URL", ...)`
2. `app/src/main/AndroidManifest.xml` 的 `<data android:host="...">`
3. `worker/wrangler.toml`（gitignored）的 `[vars] PUBLIC_BASE_URL`

## 6. 功能边界规则

- 只实现用户明确要求的功能范围。
- 修改已有功能时，默认保持现有页面结构、导航关系和交互路径稳定。
- 删除字段或简化功能时，必须一并移除对应的：
  - 编辑 UI
  - 展示 UI
  - 数据库存储
  - 查询与映射逻辑
  - 提醒调度与取消逻辑
  - 备份/恢复字段
  - 同步载荷与兼容处理
- 未经用户明确要求，不新增“占位式能力”，例如空入口、假按钮、未接通的数据流。

## 7. UI 与交互规范

- 优先复用项目内已有的 Compose 组件模式、间距体系、颜色语义和交互反馈。
- 不要仅因“Material 默认更方便”就替换已有显式样式。
- 页面内容默认必须避开系统状态栏和导航栏，不允许与系统栏重叠；实现时需显式检查 `WindowInsets`、顶部栏和底部内容区的安全区处理。
- 背景图只允许用于启动页和主页面的子 Tab 页面；所有二级页面默认使用纯色页面背景，不使用背景图，除非用户明确提出。
- 修改 UI 时至少核对以下内容：
  - 页面背景与容器背景
  - 文本层级、次级文字和禁用态颜色
  - 卡片圆角、边距、阴影或分隔策略
  - 输入框的提示文案、边框、背景与错误态
  - 按钮、Chip、图标按钮的可点击态、选中态、禁用态
  - 空态、加载态、错误态是否与页面语义一致
- 未经要求，不要把已有页面重做成另一套视觉语言。
- 文案变更应保持术语一致，避免同一概念出现多个叫法。

## 8. 状态管理与 Compose 约束

- 延续当前 ViewModel + UI state 的组织方式，不随意混入新的状态架构。
- 状态应单向流动，避免在多个层级重复持有同一业务状态。
- Compose 修改需重点检查：
  - 是否引入了不必要的重组热点
  - 是否把业务逻辑塞进 Composable
  - 是否在错误的层级持有 `Context`、导航或副作用
  - 是否破坏了可预期的状态恢复与返回行为
- 未经充分理由，不要为了“代码更现代”引入大量抽象层、包装组件或工具函数。

## 9. 数据与持久化规则

- 涉及 `Room Entity`、DAO、Repository、ViewModel 的改动必须成套检查。
- 数据字段新增、删除、重命名时，必须明确评估：
  - 数据库版本与迁移策略
  - 查询结果是否仍覆盖现有页面
  - 排序、筛选、统计、详情展示是否受影响
  - 备份与恢复是否兼容
  - 同步导入导出是否受影响
- 不保留“代码里存在但产品已不用”的隐性字段，除非有明确兼容理由。
- 若存在兼容保留字段，需在最终说明中明确其用途和未删除原因。

## 10. 提醒、备份、同步相关规则

只要改动涉及存货、待买、设置或数据模型，都应主动评估以下链路是否需要同步更新：

- 提醒调度：`app/src/main/java/com/doginventory/reminder/`
- 本地备份与恢复：`app/src/main/java/com/doginventory/backup/`
- WebDAV 同步：`app/src/main/java/com/doginventory/webdav/`
- 偏好设置：`app/src/main/java/com/doginventory/settings/`
- 权限申请与回调：`app/src/main/java/com/doginventory/permission/`

特别要求：

- 删除提醒相关字段时，必须同步移除调度、取消、接收与恢复逻辑。
- 备份结构变更时，必须检查导出、导入、恢复后的页面可用性。
- 同步配置或凭据相关改动时，必须避免破坏现有安全存储和自动同步行为。

## 11. 禁止事项

除非用户明确要求，否则不要：

- 引入与当前架构不一致的大规模重构。
- 擅自更换状态管理模式、导航模式或数据层分层方式。
- 为了“代码好看”重命名大量稳定 API、文件或类型。
- 保留半删除功能：UI 已删但数据逻辑还在，或数据已删但提醒/备份还在。
- 添加未验证的兜底逻辑掩盖真实问题。
- 用硬编码临时值替代已有配置、语义颜色或真实数据来源。
- 忽略构建失败、明显警告或可见回归风险。

## 12. 实施流程

执行非琐碎任务时，建议遵循以下顺序：

1. 阅读相关入口、页面、ViewModel、Repository 与数据模型。
2. 明确本次改动边界及潜在影响链路。
3. 以最小改动完成实现。
4. 自查 UI、状态、数据、提醒、备份、同步是否一致。
5. 运行必要验证。
6. 在最终说明中写清修改点、验证结果和剩余风险。

## 13. 验证要求

完成有意义的改动后，默认至少执行或评估以下验证：

1. 构建应用：`./gradlew :app:assembleDebug`
2. 若涉及数据库：检查编译、实体、DAO、迁移、列表与详情链路。
3. 若涉及导航：检查入口、返回栈、参数传递、底部导航可见性。
4. 若涉及 UI：检查加载态、空态、错误态、选中态、禁用态。
5. 若涉及提醒：检查创建、更新、删除、重启恢复、权限相关路径。
6. 若涉及备份或同步：检查导出、导入、凭据、自动触发和失败提示。

每次修改代码后，必须运行一次编译（`./gradlew :app:assembleDebug`）以确保没有语法错误或编译问题。

如果因环境限制无法完成验证，必须明确说明未验证项及潜在风险。

## 14. 最终交付要求

完成任务后的说明应尽量简洁，但必须包含：

- 改了什么。
- 为什么这样改。
- 运行了哪些验证，结果如何。
- 是否存在未解决风险、未覆盖验证或有意保留的差异。

若任务是代码审查型请求，优先输出问题清单，按严重程度排序，并附文件路径与定位信息。

## 15. 面向 AI 代理的执行要求

- 把自己当作该仓库的长期维护者，而不是一次性脚本执行者。
- 优先追求可维护性、边界清晰和低回归风险。
- 发现脏工作区时，不回滚不是自己产生的改动。
- 与当前任务无关的异常可以记录，但不要顺手扩散修改范围。
- 若遇到真实冲突或需求不明确，只提一个最小必要问题；否则直接完成实现。

## 16. 简要 Do / Don't

Do：

- 先理解入口和调用链再修改。
- 优先做小而完整的改动。
- 数据变更时同步检查提醒、备份、同步和设置链路。
- 保持命名、交互和视觉语义一致。
- 在最终说明中明确验证与剩余风险。

Don't：

- 不要依赖外部项目作为参考真相。
- 不要发明新需求、新字段或新入口。
- 不要留下半迁移、半删除、半接通的代码路径。
- 不要用未经验证的默认样式或兜底逻辑掩盖问题。
- 不要在没有明确收益时扩大改动面。
