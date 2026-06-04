-- D1 schema for dog-inventory-share
-- 应用方法：
--   本地: npx wrangler d1 execute dog_inventory_share --local --file=schema.sql
--   远程: npx wrangler d1 execute dog_inventory_share --file=schema.sql

CREATE TABLE IF NOT EXISTS shared_lists (
  id          TEXT PRIMARY KEY,        -- crypto.randomUUID()
  share_id    TEXT NOT NULL UNIQUE,    -- 8 字符 Crockford base32
  title       TEXT NOT NULL,
  created_at  INTEGER NOT NULL,        -- epoch 秒
  expires_at  INTEGER NOT NULL         -- epoch 秒
);
CREATE INDEX IF NOT EXISTS idx_shared_lists_expires ON shared_lists(expires_at);

CREATE TABLE IF NOT EXISTS shared_categories (
  id          TEXT PRIMARY KEY,
  share_id    TEXT NOT NULL,
  name        TEXT NOT NULL,
  color       TEXT,
  icon        TEXT,
  sort_order  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_shared_categories_share ON shared_categories(share_id);

CREATE TABLE IF NOT EXISTS shared_items (
  id                       TEXT PRIMARY KEY,
  share_id                 TEXT NOT NULL,
  name                     TEXT NOT NULL,
  category_name            TEXT,
  category_color           TEXT,
  category_icon            TEXT,
  quantity_current         REAL,
  quantity_unit            TEXT NOT NULL DEFAULT '',
  quantity_low_threshold   REAL,
  expire_at                INTEGER,
  note                     TEXT NOT NULL DEFAULT '',
  sort_order               INTEGER NOT NULL DEFAULT 0,
  rules_json               TEXT NOT NULL DEFAULT '[]'
);
CREATE INDEX IF NOT EXISTS idx_shared_items_share ON shared_items(share_id);

CREATE TABLE IF NOT EXISTS share_rate (
  ip TEXT NOT NULL,
  ts INTEGER NOT NULL,
  PRIMARY KEY (ip, ts)
);
CREATE INDEX IF NOT EXISTS idx_share_rate_ts ON share_rate(ts);
