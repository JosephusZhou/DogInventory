# DogInventory

DogInventory 是一个原生 Android 家庭存货管理应用，聚焦三件事：记录家中物品、管理待买清单、在临期前给出提醒。项目使用 Kotlin 与 Jetpack Compose 构建，当前已具备本地持久化、提醒、备份恢复和 WebDAV 同步能力，可作为独立 Android 项目持续维护和演进。

## 项目概览

这个 App 主要面向日常家庭物资管理场景，例如食品、饮料、药品、日用品、优惠券等。它的目标不是做复杂 ERP，而是帮助用户用较低的操作成本完成以下事情：

- 记录当前家里有什么。
- 追踪哪些物品快到期、已过期。
- 为有截止日期的物品设置提醒规则。
- 维护待买清单，买完即可勾选归档。
- 在本地导出备份，或通过 WebDAV 做跨设备同步与恢复。
- 把存货列表通过短链接分享给家人或朋友，对方在浏览器打开链接即可唤起 App 导入。

## 核心功能

### 1. 存货管理

存货模块是 App 的核心能力，支持完整的新增、编辑、查看、删除与分类管理流程。

主要能力包括：

- 新增存货条目。
- 编辑已有存货条目。
- 查看存货详情。
- 删除存货及其关联提醒规则。
- 按状态查看全部、即将到期、已过期物品。
- 通过统计卡片快速查看临期和过期数量。
- 给存货分配分类、颜色和图标。
- 管理预设分类和自定义分类。

当前存货数据模型包含以下关键信息：

- 名称
- 分类
- 当前数量
- 单位
- 低库存阈值
- 到期时间
- 备注
- 状态
- 创建时间与更新时间

说明：

- 当前首页的筛选和展示重点放在到期状态上。
- 数量相关字段已经保存在数据模型中，为后续更完整的库存能力预留了结构基础。

### 2. 提醒规则

围绕存货到期时间，App 支持为单个物品配置多条提醒规则。

支持的提醒方式包括：

- 按到期日前若干天提醒，例如提前 1、3、7、14、30 天。
- 指定某个绝对时间点提醒。
- 单条提醒可启用、禁用或删除。

提醒的设计特点：

- 提醒规则与存货条目绑定。
- 更新物品时会先取消旧提醒，再重新同步新的提醒集合。
- 删除物品时会同步清理相关提醒。
- 备份恢复和远端恢复完成后，会重新调度所有提醒，避免本地调度状态丢失。

系统权限方面，提醒功能依赖：

- Android 13 及以上的通知权限。
- 精确闹钟权限，用于更稳定地触发定时提醒。

### 3. 待买清单

待买模块用于记录临时采购事项，定位比存货模块更轻量。

主要能力包括：

- 新增待买项。
- 编辑待买项。
- 记录备注。
- 勾选完成或取消完成。
- 区分未完成与已完成分组。
- 折叠或展开已完成分组。
- 删除单条待买项。
- 一键清空所有已完成项。

待买列表当前采用简单直接的数据结构，字段包括：

- 名称
- 备注
- 是否完成
- 完成时间
- 创建时间与更新时间

### 4. 分类管理

App 内置一组默认分类，首次启动时会自动初始化，包括常见家庭物资类别。当前代码中已预置：

- 食品
- 饮料
- 药品
- 日用品
- 优惠券
- 其他

分类管理支持：

- 查看预设分类。
- 新增自定义分类。
- 编辑自定义分类。
- 删除自定义分类。
- 调整分类顺序。
- 通过颜色与图标增强视觉识别。

分类与存货条目通过外键关联。若分类被删除，存货条目的分类会被置空，而不会直接删除物品。

### 5. 外观与主题

App 提供基础主题配置能力，支持三种模式：

- 跟随系统
- 浅色模式
- 深色模式

主题设置会持久化保存，并纳入备份与同步范围。应用界面使用 Material 3 主题体系，但页面层面做了较多显式样式控制，以保证存货卡片、状态色、背景层次和提醒语义更加稳定。

### 6. 备份与恢复

App 内置本地备份能力，适合换机、重装或手工归档数据。

支持的能力包括：

- 手动导出备份为 zip 文件。
- 从本地 zip 文件恢复备份。
- 自动定期备份到公开下载目录。
- 恢复完成后自动重启应用。

备份内容包含：

- Room 数据库文件
- 偏好设置数据
- 描述备份格式与数据库版本的 manifest 文件

恢复流程具备一定的安全保护：

- 先做当前数据快照作为回滚点。
- 校验备份包结构与版本兼容性。
- 恢复失败时回滚数据库与偏好设置。
- 回滚后重新同步提醒，尽量恢复到失败前状态。

### 7. WebDAV 同步

为了支持跨设备数据同步，项目提供 WebDAV 远端备份与恢复能力。

主要能力包括：

- 配置服务器地址、用户名、密码和远端路径。
- 测试 WebDAV 连接。
- 将当前本地数据同步到服务器。
- 从服务器恢复数据到本机。
- 检查本地与远端数据一致性。
- 记录最近一次自动同步时间。

同步实现不是按业务记录逐条合并，而是基于备份快照进行整体上传与恢复。这样做的好处是实现相对稳定、易于校验，也更适合当前项目的规模与复杂度。

需要注意：

- WebDAV 配置通过独立凭据存储管理。
- 修改数据、分类、待买项或主题设置后，可触发自动同步请求。
- 远端恢复完成后同样会重新调度提醒并要求重启应用。

### 8. 存货分享

App 支持把存货列表以短链接形式分享给其他人，接收方在浏览器打开链接即可唤起 DogInventory 导入。

分享方：

- 在存货页右上角点击分享按钮。
- 选择分享范围：默认按当前筛选视图（如「已过期」），可勾选「包含所有存货」切换到全量。
- 选择是否包含提醒规则（默认包含）。
- 写一个分享标题（默认「我的存货」），点击「创建分享链接」即可生成短链接。
- 通过系统分享面板把链接发给朋友，或手动复制粘贴。

接收方：

- 在普通浏览器打开链接，看到落地页（品牌色与 App 一致）。
- 移动端页面加载时自动尝试唤起 App（隐藏 iframe 唤起，桌面端不触发）。
- 唤起失败时手动点击「在 App 中打开」按钮即可。
- 已安装但版本不匹配或未装 App 时，按钮下方的「未安装 App？前往下载」可跳到 Play Store。
- App 唤起后自动切到存货 tab，弹导入对话框：可全选/反选、决定是否导入提醒规则，确认后写入本机。

数据流：

- 分享内容走 Cloudflare Workers + D1（SQLite）后端，**不依赖任何第三方 SaaS**。
- 同一 App 内每次分享触发 1 次 WebDAV 同步（如已开启），导入时同理。
- 链接默认 30 天后过期；过期链接的访问会直接显示「分享链接无效或已过期」。
- 分类名匹配规则：本地已有同名的分类直接复用，缺失的分类自动新建。

实现细节：

- 落地页唤起协议是 `doginv://share.com/s/{id}`（自定义 scheme），App 端 Manifest 已注册。
- 之所以不用 `intent://s/{id}#Intent;scheme=https;...`：Android 解析出来的实际 Intent data 是 `https://s/{id}`（host 变成 `s`），与 App 端 `host="<实际域名>"` 不匹配，会被系统回退到 Play Store。`doginv://` 没有这个问题。
- 后端部署和 Android 端三处配置必须保持域名一致：`app/build.gradle.kts` 的 `SHARE_BASE_URL`、AndroidManifest 的 `<data android:host>`、`worker/wrangler.toml` 的 `PUBLIC_BASE_URL`。

## 技术架构

项目整体采用适合中小型 Android 应用的分层结构，核心可以概括为：

- UI 层：Jetpack Compose 页面与组件。
- 状态层：ViewModel 负责组织页面状态与用户操作。
- 数据层：Repository 统一封装 Room、提醒同步和自动同步触发。
- 持久化层：Room 数据库与 SharedPreferences/安全存储。
- 系统能力层：提醒调度、备份归档、存储访问、WebDAV 通信、分享服务、权限协调。
- 后端子系统：`worker/` 目录下的 Cloudflare Workers + D1，分享链接的 HTTP API 与落地页。

### 架构分层

#### 1. UI 层

UI 层基于 Jetpack Compose，按功能模块拆分在 `ui/` 下：

- `ui/inventory/`：存货首页、编辑页、详情页、分类页。
- `ui/shopping/`：待买列表与编辑页。
- `ui/settings/`：设置首页、备份页、WebDAV 页。
- `ui/components/`：通用页面背景、卡片、顶部栏等复用组件。
- `ui/theme/`：主题、颜色、语义色、系统栏样式等。

页面层职责主要是：

- 展示状态。
- 采集用户输入。
- 调用 ViewModel 暴露的行为。
- 处理基础导航。

业务逻辑没有直接塞进 Composable，而是尽量放在 ViewModel 或更下层服务中。

#### 2. 状态层

项目使用多个 ViewModel 分别维护各页面状态，例如：

- `InventoryViewModel`
- `InventoryEditorViewModel`
- `InventoryDetailViewModel`
- `InventoryCategoriesViewModel`
- `ShoppingViewModel`
- `ShoppingEditorViewModel`
- `SettingsBackupViewModel`
- `SettingsWebdavSyncViewModel`

这些 ViewModel 通过 `ViewModelFactory` 统一创建。当前工程未引入 DI 框架，依赖由 `MainActivity` 和 `ViewModelFactory` 手工装配。

这种方式的特点是：

- 结构直接，适合当前体量。
- 依赖关系清晰可追踪。
- 后续如果项目继续扩大，再考虑引入 Hilt 等依赖注入框架也较容易迁移。

#### 3. 数据层

数据访问统一收口在 `InventoryRepository`。

Repository 当前负责：

- 暴露分类、存货、待买项和提醒规则的读取接口。
- 对 Room DAO 的增删改查进行封装。
- 在存货变更时同步处理提醒调度。
- 在业务数据变化后触发 WebDAV 自动同步请求。
- 提供重新同步全部提醒、清空全部提醒等跨模块能力。

这意味着它不仅是 DAO 包装层，还承担了少量跨模块编排职责，是当前应用的数据与副作用中心之一。

#### 4. 持久化层

项目主要有两套本地持久化机制：

- Room：用于结构化业务数据。
- SharedPreferences 与安全存储：用于主题、备份时间、同步时间、WebDAV 配置等。

Room 数据库定义在 `AppDatabase` 中，当前版本为 `4`，包含以下实体：

- `InventoryCategoryEntity`
- `InventoryItemEntity`
- `InventoryReminderRuleEntity`
- `ShoppingItemEntity`

数据库已包含从旧版本升级到当前版本的迁移逻辑。

偏好设置由 `PreferencesService` 管理，负责：

- 主题模式读写。
- 最近自动备份时间记录。
- 最近 WebDAV 自动同步时间记录。
- WebDAV 配置读写。
- 备份恢复时的偏好快照与还原。

### 关键数据流

以“新增一个带提醒的存货”为例，项目中的典型数据流如下：

1. 用户在 `InventoryEditorScreen` 输入名称、分类、到期时间和提醒规则。
2. `InventoryEditorViewModel` 组织表单状态并调用保存逻辑。
3. `InventoryRepository` 写入 `inventory_items` 与 `inventory_reminder_rules`。
4. Repository 调用 `InventoryReminderScheduler` 同步本地提醒。
5. Repository 触发 WebDAV 自动同步请求。
6. `InventoryHomeScreen` 通过 Flow 感知 Room 数据变化并自动刷新列表。

“恢复备份”或“从 WebDAV 拉取远端数据”的链路则会额外经过：

- 关闭数据库实例。
- 替换数据库文件。
- 恢复偏好设置。
- 重新创建数据库实例。
- 重新同步全部提醒。
- 重启应用，使运行态和持久化状态重新对齐。

## 导航结构

应用导航由 `MainScreen` 管理，整体分为两层：

- 外层导航：用于进入编辑页、详情页、分类页、备份页、WebDAV 页等二级页面。
- 内层导航：用于底部三个主标签页切换。

当前底部标签包括：

- 存货
- 待买
- 设置

二级页面包括：

- 存货编辑页
- 存货详情页
- 分类管理页
- 分类编辑页
- 待买编辑页
- 备份页
- WebDAV 同步页

这种结构的优点是主标签切换和二级页面跳转解耦，返回栈更清晰，也方便单独维护各个功能模块。

## 启动流程

应用从 `SplashActivity` 启动，默认展示约 2 秒启动页，然后进入 `MainActivity`。

`MainActivity` 启动时会完成以下初始化工作：

- 读取主题模式。
- 初始化 Room 数据库。
- 创建提醒调度器与权限协调器。
- 创建 Repository、偏好服务、备份协调器与 WebDAV 同步服务。
- 如果分类表为空，则写入一组默认分类。
- 判断是否需要执行自动备份。
- 挂载 Compose 根界面 `MainScreen`。

这使得 App 在进入主界面前，核心依赖基本已准备完毕。

## 目录结构

仓库当前最重要的目录如下：

```text
app/src/main/java/com/doginventory/
├── backup/        # 备份归档、恢复协调、存储桥接
├── data/          # Room 数据库、DAO、实体
├── permission/    # 通知、精确闹钟、存储权限协调
├── reminder/      # 本地提醒调度与接收
├── settings/      # 偏好设置服务
├── share/         # 分享功能：HTTP 客户端、Service、DTO、ViewModel、Dialog
├── ui/            # Compose 页面、导航、ViewModel、主题
├── webdav/        # WebDAV 客户端、配置、同步服务
├── MainActivity.kt
└── SplashActivity.kt
```

```text
worker/             # Cloudflare Workers + D1 分享后端（独立于 Gradle）
├── wrangler.toml.example   # 提交到 git 的配置模板
├── wrangler.toml          # gitignored；从模板复制
├── package.json
├── schema.sql
└── src/
    ├── index.ts
    ├── api.ts
    ├── shareHtml.ts
    └── cleanup.ts
```

如果要快速理解项目，建议优先阅读这些文件：

- `app/src/main/java/com/doginventory/MainActivity.kt`
- `app/src/main/java/com/doginventory/ui/MainScreen.kt`
- `app/src/main/java/com/doginventory/ui/ViewModelFactory.kt`
- `app/src/main/java/com/doginventory/data/AppDatabase.kt`
- `app/src/main/java/com/doginventory/data/dao/InventoryDao.kt`
- `app/src/main/java/com/doginventory/data/repository/InventoryRepository.kt`
- `app/src/main/java/com/doginventory/ui/theme/Theme.kt`

## 数据模型说明

### InventoryCategoryEntity

用于定义分类，包含：

- `id`
- `name`
- `color`
- `icon`
- `sortOrder`
- `isPreset`
- `isDeleted`
- `createdAt`
- `updatedAt`

### InventoryItemEntity

用于定义存货项，包含：

- `id`
- `name`
- `categoryId`
- `quantityCurrent`
- `quantityUnit`
- `quantityLowThreshold`
- `expireAt`
- `note`
- `status`
- `createdAt`
- `updatedAt`

### InventoryReminderRuleEntity

用于定义提醒规则，包含：

- `id`
- `itemId`
- `kind`
- `enabled`
- `payloadJson`
- `reminderCalendarId`
- `reminderCalendarEventId`
- `lastTriggeredAt`
- `createdAt`
- `updatedAt`

### ShoppingItemEntity

用于定义待买项，包含：

- `id`
- `name`
- `note`
- `isDone`
- `doneAt`
- `createdAt`
- `updatedAt`

## 权限与系统能力

项目当前使用到的关键系统权限包括：

- `POST_NOTIFICATIONS`：通知提醒。
- `SCHEDULE_EXACT_ALARM`：精确提醒调度。
- `INTERNET`：WebDAV 网络访问。
- `WRITE_EXTERNAL_STORAGE`：旧版本 Android 的备份导出兼容。

这些权限不是一次性静态声明就结束，项目中还通过权限协调器在运行时处理：

- 提醒权限申请。
- 精确闹钟设置跳转。
- 旧版存储权限判断与申请。

## 构建与运行

### 环境要求

- Android Studio 新版稳定版
- JDK 17
- Android SDK 34
- Gradle Wrapper

### 构建命令

在仓库根目录执行：

```bash
./gradlew :app:assembleDebug
```

如果需要安装到设备或模拟器，可继续使用 Android Studio 或对应的 Gradle / adb 流程。

## 适合继续演进的方向

从当前代码结构看，项目后续比较自然的演进方向包括：

- 完善数量与低库存阈值在首页和详情页中的展示与提醒联动。
- 为存货和待买提供搜索、排序和更细的筛选能力。
- 为 WebDAV 自动同步补充更明确的调度与可视化状态。
- 增加单元测试和集成测试，提升备份恢复、数据库迁移和提醒链路的回归保障。
- 当依赖关系继续增大时，再引入 Hilt 等依赖注入方案。
- 分享功能：增加可选分享有效期（默认 30 天）、分享查看次数统计、为分享内容加可选密码。
- 启用 Android App Links 消除落地页（需在 Worker 上托管 `/.well-known/assetlinks.json` 并切换到稳定 release 签名）。

## 项目现状总结

DogInventory 当前已经不是一个 Demo 或仅有界面的原型，而是一个具备完整主流程的 Android 应用：

- 有清晰的主导航结构。
- 有稳定的本地数据存储。
- 有面向日常使用的提醒能力。
- 有本地备份与远端同步方案。
- 有相对明确的模块边界。

对于维护者来说，这个项目最值得优先关注的部分是：数据一致性、提醒副作用、备份恢复安全性，以及 WebDAV 远端状态和本地状态之间的对齐。

## 分享后端（Cloudflare Worker）

分享链接由仓库根的 `worker/` 目录承载，使用 Cloudflare Workers + D1（SQLite）的免费层。

- 仓库提交 `wrangler.toml.example`（占位符 + 注释），本地真实的 `wrangler.toml` 已被仓库根 `.gitignore` 忽略，避免 `database_id` 与自定义域名进 git。
- API：`POST /api/shares`（创建）、`GET /api/shares/{id}`（读取，过期/不存在返回 404）、`GET /s/{id}`（落地页 HTML）。
- 落地页移动端加载时通过隐藏 iframe 触发 `doginv://share.com/s/{id}` 自定义协议唤起 App；按钮点击是兜底；「未安装 App？前往下载」链接常驻显示。
- 每天 UTC 03:00 由 cron 触发器清理过期分享与一周前的限流记录。
- 限流：每 IP 每小时 20 次创建（`share_rate` 表）。
- 完整文档见 `worker/README.md`。
