import { handleCreateShare, handleGetShare, isSharePath } from "./api";
import { renderLandingPage } from "./shareHtml";
import { cleanupExpired } from "./cleanup";

export interface Env {
  DB: D1Database;
  PUBLIC_BASE_URL: string;
}

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

function corsPreflight(): Response {
  return new Response(null, { status: 204, headers: CORS_HEADERS });
}

function getClientIp(req: Request): string {
  return req.headers.get("cf-connecting-ip")
    || req.headers.get("x-forwarded-for")
    || "unknown";
}

function notFoundHtml(): Response {
  return new Response("Not Found", { status: 404, headers: { "Content-Type": "text/plain" } });
}

export default {
  async fetch(req: Request, env: Env, _ctx: ExecutionContext): Promise<Response> {
    if (req.method === "OPTIONS") return corsPreflight();

    const url = new URL(req.url);
    const path = url.pathname;
    const info = isSharePath(path);

    // /s/{id} 落地页
    if (info.kind === "landing" && info.id) {
      const html = renderLandingPage(info.id, env.PUBLIC_BASE_URL);
      return new Response(html, {
        status: 200,
        headers: {
          "Content-Type": "text/html; charset=utf-8",
          "Content-Security-Policy": "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'",
          "Cache-Control": "public, max-age=300",
        },
      });
    }

    // /api/shares
    if (path === "/api/shares" || path === "/api/shares/") {
      if (req.method !== "POST") {
        return new Response(JSON.stringify({ error: "method_not_allowed" }), {
          status: 405,
          headers: { "Content-Type": "application/json", ...CORS_HEADERS, Allow: "POST" },
        });
      }
      return handleCreateShare(req, env, getClientIp(req));
    }

    const apiMatch = path.match(/^\/api\/shares\/([A-Za-z0-9]+)\/?$/);
    if (apiMatch) {
      if (req.method !== "GET") {
        return new Response(JSON.stringify({ error: "method_not_allowed" }), {
          status: 405,
          headers: { "Content-Type": "application/json", ...CORS_HEADERS, Allow: "GET" },
        });
      }
      return handleGetShare(apiMatch[1], env);
    }

    return notFoundHtml();
  },

  async scheduled(_event: ScheduledEvent, env: Env, ctx: ExecutionContext): Promise<void> {
    ctx.waitUntil(cleanupExpired(env));
  },
};
