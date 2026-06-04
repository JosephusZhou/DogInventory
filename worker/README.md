# dog-inventory-share

DogInventory 应用的分享链接后端，基于 Cloudflare Workers + D1（SQLite）免费层。

## 路由

| 路径 | 方法 | 说明 |
| --- | --- | --- |
| `POST /api/shares` | 创建分享，返回 `{shareId, url, expiresAt}`；`shareId` 为 8 字符 Crockford base32（~32⁸ ≈ 1.1 万亿组合） |
| `GET  /api/shares/{id}` | 读取分享内容；过期或不存在返回 404 |
| `GET  /s/{id}` | 落地页 HTML，移动端加载时自动尝试唤起 App |

`POST` 同时有基于客户端 IP 的限流（`share_rate` 表）：每 IP 每小时最多 20 次创建。

## 配置（不进入 git）

`worker/wrangler.toml` **不提交到仓库**——它包含真实的 `database_id` 与自定义域名。

仓库里提交的是 `wrangler.toml.example`（占位符 + 注释）。首次部署或换机时：

```bash
cd worker
cp wrangler.toml.example wrangler.toml
# 编辑 wrangler.toml 填入：
#   database_id    — npx wrangler d1 create dog_inventory_share 的返回
#   PUBLIC_BASE_URL — 实际部署的自定义域名，例如 https://share.example.com
```

`wrangler.toml` 已在仓库根 `.gitignore` 里，无需手动维护。

`PUBLIC_BASE_URL` 必须与 Android 端两个地方保持一致：

- `app/build.gradle.kts` 的 `buildConfigField("String", "SHARE_BASE_URL", ...)`
- `app/src/main/AndroidManifest.xml` 的 `<data android:host="...">`

## 首次部署

1. 安装依赖：`npm install`
2. 创建 D1 数据库：`npx wrangler d1 create dog_inventory_share`
3. `cp wrangler.toml.example wrangler.toml` 并填入 `database_id` 和 `PUBLIC_BASE_URL`
4. 应用 schema：
   - 本地：`npm run db:init:local`
   - 远程：`npm run db:init`
5. 部署：`npm run deploy`

## 本地开发

```bash
cd worker
npm install
npm run db:init:local
npm run dev    # 默认 http://localhost:8787
```

可用 curl 测试：

```bash
# 创建分享
curl -X POST http://localhost:8787/api/shares \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "test",
    "items": [
      {"name": "维生素 C", "categoryName": "药品",
       "quantityCurrent": 30, "quantityUnit": "片",
       "expireAt": 1735689600000, "note": "饭后服用"}
    ],
    "categories": [
      {"name": "药品", "color": "#FF9B71", "icon": "💊"}
    ]
  }'
# → {"shareId":"XXXXXXXX","url":"http://localhost:8787/s/XXXXXXXX","expiresAt":...}

# 读取
curl http://localhost:8787/api/shares/XXXXXXXX

# 落地页
curl -I http://localhost:8787/s/XXXXXXXX
```

## 数据模型（D1）

```sql
shared_lists        (id, share_id UNIQUE, title, created_at, expires_at)
shared_categories   (id, share_id, name, color, icon, sort_order)
shared_items        (id, share_id, name, category_name, category_color, category_icon,
                     quantity_current, quantity_unit, quantity_low_threshold,
                     expire_at, note, sort_order, rules_json)
share_rate          (ip, ts)  -- 限流计数
```

- `shared_items.category_*` 是冗余字段——便于没有 `categories` 数组时也能渲染卡片
- `shared_items.rules_json` 是提醒规则 JSON 数组；接收方可按开关决定是否导入
- 单条 share 30 天后过期；cron 触发器 `0 3 * * *`（UTC 03:00）每天清理过期行

## 自动清理

`wrangler.toml` 中 `triggers.crons = ["0 3 * * *"]`（UTC 03:00），每天清理：
- 过期 share（`expires_at < now`）关联的所有行
- 一周前的限流记录

## 与 App 端的协议约定

落地页唤起 App 用自定义协议 `doginv://share.com/s/{id}`，原因：

- `intent://s/{id}#Intent;scheme=https;...` 这种写法生成的 Intent data 实际是 `https://s/{id}`（host 变 `s`），与 App 端 `host="share.example.com"` 不匹配，会被系统回退到 Play Store
- 自定义协议简单可靠，不依赖 App Links（无需 worker 托管 `assetlinks.json`，也无需稳定 release 签名）

App 端 Manifest 注册了 `doginv` 协议（host=`share.com`，pathPrefix=`/s/`）。落地页 JS 检测移动端后用隐藏 iframe 自动触发，桌面端不触发（避免污染地址栏）。按钮点击是 iframe 失败时的兜底。

## 安全提示

- API 无鉴权；8 字符 Crockford base32 ID 足以防偶然枚举
- 限流：每 IP 每小时 20 次创建
- CORS `Access-Control-Allow-Origin: *`（免费层 + ID 难以猜测前提下风险可控）
- 若未来加用户系统，应同时收紧 CORS 并改用签名 URL

## 目录结构

```
worker/
├── wrangler.toml.example  # 提交到 git 的模板
├── wrangler.toml          # gitignore；从模板复制
├── package.json           # 仅 devDep: wrangler, typescript, workers-types
├── tsconfig.json
├── schema.sql             # D1 DDL
└── src/
    ├── index.ts           # fetch + scheduled handler 入口
    ├── api.ts             # POST/GET handlers + share_id 生成 + 限流
    ├── shareHtml.ts       # /s/{id} 落地页
    └── cleanup.ts         # cron 触发的过期清理
```

Worker 与 Android `app/` 模块相互独立，不进 Gradle。仓库根目录 `npx wrangler deploy`（在 `worker/` 下执行）。
