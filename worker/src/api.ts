export interface Env {
  DB: D1Database;
  PUBLIC_BASE_URL: string;
}

interface CreateShareItem {
  id?: string;
  name: string;
  categoryName?: string | null;
  categoryColor?: string | null;
  categoryIcon?: string | null;
  quantityCurrent?: number | null;
  quantityUnit?: string;
  quantityLowThreshold?: number | null;
  expireAt?: number | null;
  note?: string;
  sortOrder?: number;
  rules?: Array<{
    kind: string;
    enabled?: boolean;
    daysBefore?: number | null;
    remindAt?: number | null;
  }>;
}

interface CreateShareCategory {
  id?: string;
  name: string;
  color?: string | null;
  icon?: string | null;
  sortOrder?: number;
}

interface CreateShareRequest {
  title: string;
  items: CreateShareItem[];
  categories: CreateShareCategory[];
}

const MAX_TITLE = 60;
const MAX_ITEMS = 500;
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

function json(body: unknown, init: ResponseInit = {}): Response {
  const headers = {
    "Content-Type": "application/json; charset=utf-8",
    ...CORS_HEADERS,
    ...(init.headers || {}),
  };
  return new Response(JSON.stringify(body), { ...init, headers });
}

function badRequest(reason: string): Response {
  return json({ error: reason }, { status: 400 });
}

function notFound(): Response {
  return json({ error: "not_found" }, { status: 404 });
}

function tooManyRequests(): Response {
  return json({ error: "rate_limited" }, { status: 429 });
}

const CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

function generateShareId(): string {
  let id = "";
  while (id.length < 8) {
    const bytes = new Uint8Array(8);
    crypto.getRandomValues(bytes);
    for (let i = 0; i < bytes.length && id.length < 8; i++) {
      id += CROCKFORD[bytes[i] % 32];
    }
  }
  return id.slice(0, 8);
}

function isValidShareId(id: string): boolean {
  return /^[0-9A-HJKMNP-TV-Z]{8}$/.test(id);
}

async function checkRateLimit(env: Env, ip: string, now: number): Promise<boolean> {
  const windowStart = now - 3600;
  const { count } = await env.DB.prepare(
    "SELECT COUNT(*) as count FROM share_rate WHERE ip = ? AND ts > ?"
  ).bind(ip, windowStart).first<{ count: number }>();
  if ((count ?? 0) >= 20) return false;
  await env.DB.prepare(
    "INSERT INTO share_rate (ip, ts) VALUES (?, ?)"
  ).bind(ip, now).run();
  return true;
}

export async function handleCreateShare(req: Request, env: Env, ip: string): Promise<Response> {
  const now = Math.floor(Date.now() / 1000);
  if (!(await checkRateLimit(env, ip, now))) {
    return tooManyRequests();
  }

  let body: CreateShareRequest;
  try {
    body = await req.json() as CreateShareRequest;
  } catch {
    return badRequest("invalid_json");
  }

  if (typeof body?.title !== "string" || body.title.length === 0 || body.title.length > MAX_TITLE) {
    return badRequest("invalid_title");
  }
  if (!Array.isArray(body.items) || body.items.length === 0) {
    return badRequest("empty_items");
  }
  if (body.items.length > MAX_ITEMS) {
    return badRequest("too_many_items");
  }
  if (!Array.isArray(body.categories)) {
    body.categories = [];
  }

  for (const item of body.items) {
    if (typeof item?.name !== "string" || item.name.length === 0) {
      return badRequest("invalid_item");
    }
  }

  const shareId = generateShareId();
  const createdAt = now;
  const expiresAt = now + 30 * 24 * 60 * 60;
  const internalId = crypto.randomUUID();

  const statements: D1PreparedStatement[] = [];
  statements.push(env.DB.prepare(
    "INSERT INTO shared_lists (id, share_id, title, created_at, expires_at) VALUES (?, ?, ?, ?, ?)"
  ).bind(internalId, shareId, body.title, createdAt, expiresAt));

  let order = 0;
  for (const cat of body.categories) {
    if (typeof cat?.name !== "string" || cat.name.length === 0) continue;
    statements.push(env.DB.prepare(
      "INSERT INTO shared_categories (id, share_id, name, color, icon, sort_order) VALUES (?, ?, ?, ?, ?, ?)"
    ).bind(
      crypto.randomUUID(),
      shareId,
      cat.name,
      cat.color ?? null,
      cat.icon ?? null,
      cat.sortOrder ?? order++,
    ));
  }

  let itemOrder = 0;
  for (const it of body.items) {
    statements.push(env.DB.prepare(
      `INSERT INTO shared_items
        (id, share_id, name, category_name, category_color, category_icon,
         quantity_current, quantity_unit, quantity_low_threshold, expire_at, note,
         sort_order, rules_json)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      crypto.randomUUID(),
      shareId,
      it.name,
      it.categoryName ?? null,
      it.categoryColor ?? null,
      it.categoryIcon ?? null,
      it.quantityCurrent ?? null,
      it.quantityUnit ?? "",
      it.quantityLowThreshold ?? null,
      it.expireAt ?? null,
      it.note ?? "",
      it.sortOrder ?? itemOrder++,
      JSON.stringify(it.rules ?? []),
    ));
  }

  await env.DB.batch(statements);

  const url = `${env.PUBLIC_BASE_URL.replace(/\/$/, "")}/s/${shareId}`;
  return json({
    shareId,
    url,
    expiresAt,
  }, { status: 201 });
}

export async function handleGetShare(shareId: string, env: Env): Promise<Response> {
  if (!isValidShareId(shareId)) return notFound();
  const now = Math.floor(Date.now() / 1000);

  const list = await env.DB.prepare(
    "SELECT title, created_at, expires_at FROM shared_lists WHERE share_id = ?"
  ).bind(shareId).first<{ title: string; created_at: number; expires_at: number }>();

  if (!list) return notFound();
  if (list.expires_at <= now) return notFound();

  const categoryRows = await env.DB.prepare(
    "SELECT id, name, color, icon, sort_order FROM shared_categories WHERE share_id = ? ORDER BY sort_order ASC"
  ).bind(shareId).all<{ id: string; name: string; color: string | null; icon: string | null; sort_order: number }>();

  const itemRows = await env.DB.prepare(
    `SELECT id, name, category_name, category_color, category_icon,
            quantity_current, quantity_unit, quantity_low_threshold,
            expire_at, note, sort_order, rules_json
       FROM shared_items WHERE share_id = ? ORDER BY sort_order ASC`
  ).bind(shareId).all<{
    id: string;
    name: string;
    category_name: string | null;
    category_color: string | null;
    category_icon: string | null;
    quantity_current: number | null;
    quantity_unit: string;
    quantity_low_threshold: number | null;
    expire_at: number | null;
    note: string;
    sort_order: number;
    rules_json: string;
  }>();

  return json({
    title: list.title,
    createdAt: list.created_at,
    expiresAt: list.expires_at,
    categories: (categoryRows.results || []).map((c) => ({
      id: c.id,
      name: c.name,
      color: c.color,
      icon: c.icon,
      sortOrder: c.sort_order,
    })),
    items: (itemRows.results || []).map((it) => {
      let rules: unknown = [];
      try { rules = JSON.parse(it.rules_json); } catch { /* ignore */ }
      return {
        id: it.id,
        name: it.name,
        categoryName: it.category_name,
        categoryColor: it.category_color,
        categoryIcon: it.category_icon,
        quantityCurrent: it.quantity_current,
        quantityUnit: it.quantity_unit,
        quantityLowThreshold: it.quantity_low_threshold,
        expireAt: it.expire_at,
        note: it.note,
        sortOrder: it.sort_order,
        rules,
      };
    }),
  });
}

export function isSharePath(path: string): { kind: "api" | "landing"; id: string | null } {
  if (path === "/api/shares" || path === "/api/shares/") {
    return { kind: "api", id: null };
  }
  const apiMatch = path.match(/^\/api\/shares\/([A-Za-z0-9]+)\/?$/);
  if (apiMatch) return { kind: "api", id: apiMatch[1] };

  const landingMatch = path.match(/^\/s\/([A-Za-z0-9]+)\/?$/);
  if (landingMatch) return { kind: "landing", id: landingMatch[1] };

  return { kind: "api", id: null };
}
