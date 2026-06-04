import type { Env } from "./api";

export async function cleanupExpired(env: Env): Promise<void> {
  const now = Math.floor(Date.now() / 1000);
  await env.DB.batch([
    env.DB.prepare("DELETE FROM shared_items WHERE share_id IN (SELECT share_id FROM shared_lists WHERE expires_at < ?)").bind(now),
    env.DB.prepare("DELETE FROM shared_categories WHERE share_id IN (SELECT share_id FROM shared_lists WHERE expires_at < ?)").bind(now),
    env.DB.prepare("DELETE FROM shared_lists WHERE expires_at < ?").bind(now),
  ]);
  // 顺便清理过期的 rate-limit 记录
  await env.DB.prepare("DELETE FROM share_rate WHERE ts < ?").bind(now - 7 * 24 * 3600).run();
}
