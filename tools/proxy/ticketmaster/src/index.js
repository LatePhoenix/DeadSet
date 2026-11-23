/**
 * DeadSet Ticketmaster Discovery proxy.
 *
 * Exposes only GET /discovery/v2/events.json with a fixed music query shape.
 * The Ticketmaster API key stays in a Worker secret (TICKETMASTER_API_KEY).
 * Per-IP limit via Workers Rate Limiting binding (~30 req/min).
 * Optional DEADSET_CLIENT_TOKEN secret: when set, require header X-DeadSet-Client.
 */

const UPSTREAM = "https://app.ticketmaster.com/discovery/v2/events.json";
const ALLOWED_PATH = "/discovery/v2/events.json";
const MAX_PER_MINUTE = 30;

const ALLOWED_PARAMS = new Set([
  "keyword",
  "classificationName",
  "sort",
  "size",
]);

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }
    if (request.method !== "GET") {
      return json({ error: "method_not_allowed" }, 405);
    }

    const url = new URL(request.url);
    if (url.pathname !== ALLOWED_PATH && url.pathname !== `${ALLOWED_PATH}/`) {
      return json({ error: "not_found" }, 404);
    }

    if (!env.TICKETMASTER_API_KEY) {
      return json({ error: "proxy_misconfigured" }, 503);
    }

    if (url.searchParams.has("apikey")) {
      return json({ error: "apikey_not_allowed" }, 400);
    }

    const clientGate = checkClientToken(request, env);
    if (clientGate) return clientGate;

    const limited = await isRateLimited(request, env);
    if (limited) {
      return json({ error: "rate_limited" }, 429);
    }

    const keyword = (url.searchParams.get("keyword") || "").trim();
    if (!keyword) {
      return json({ error: "keyword_required" }, 400);
    }

    const upstream = new URL(UPSTREAM);
    upstream.searchParams.set("apikey", env.TICKETMASTER_API_KEY);
    upstream.searchParams.set("keyword", keyword);
    upstream.searchParams.set(
      "classificationName",
      url.searchParams.get("classificationName") || "music"
    );
    upstream.searchParams.set("sort", url.searchParams.get("sort") || "date,asc");
    const size = clampSize(url.searchParams.get("size"));
    upstream.searchParams.set("size", String(size));

    // Drop any other client params; only the allowlist is forwarded above.
    for (const key of url.searchParams.keys()) {
      if (!ALLOWED_PARAMS.has(key)) {
        // already ignored
      }
    }

    const upstreamRes = await fetch(upstream.toString(), {
      method: "GET",
      headers: { Accept: "application/json" },
    });

    const body = await upstreamRes.arrayBuffer();
    const headers = new Headers(corsHeaders());
    headers.set(
      "Content-Type",
      upstreamRes.headers.get("Content-Type") || "application/json"
    );
    headers.set("Cache-Control", "public, max-age=60");
    return new Response(body, { status: upstreamRes.status, headers });
  },
};

function clampSize(raw) {
  const n = Number.parseInt(raw || "25", 10);
  if (!Number.isFinite(n) || n < 1) return 25;
  return Math.min(n, 50);
}

/**
 * When DEADSET_CLIENT_TOKEN is set, require matching X-DeadSet-Client header.
 * Unset token = open (local/dev); set it in production so random scrapers cannot burn quota.
 */
function checkClientToken(request, env) {
  const expected = (env.DEADSET_CLIENT_TOKEN || "").trim();
  if (!expected) return null;
  const got = (request.headers.get("X-DeadSet-Client") || "").trim();
  if (got !== expected) {
    return json({ error: "client_token_required" }, 401);
  }
  return null;
}

/**
 * Prefer Workers Rate Limiting binding (TM_RATE_LIMITER). Fall back to Cache API only when
 * the binding is absent (older local wrangler), knowing Cache API is ineffective on workers.dev.
 */
async function isRateLimited(request, env) {
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  if (env.TM_RATE_LIMITER && typeof env.TM_RATE_LIMITER.limit === "function") {
    const { success } = await env.TM_RATE_LIMITER.limit({ key: ip });
    return !success;
  }
  // Legacy fallback — not reliable on *.workers.dev.
  const bucket = Math.floor(Date.now() / 60_000);
  const key = new Request(`https://deadset-tm-rate.invalid/${ip}/${bucket}`);
  const cache = caches.default;
  const prior = await cache.match(key);
  const count = prior ? Number.parseInt(await prior.text(), 10) || 0 : 0;
  if (count >= MAX_PER_MINUTE) return true;
  await cache.put(
    key,
    new Response(String(count + 1), {
      headers: { "Cache-Control": "max-age=120" },
    })
  );
  return false;
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, X-DeadSet-Client",
  };
}

function json(obj, status) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { ...corsHeaders(), "Content-Type": "application/json" },
  });
}
