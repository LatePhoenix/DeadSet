# DeadSet Ticketmaster proxy

Cloudflare Worker that holds the Ticketmaster Discovery API key as a **secret** and exposes only the music events query the app needs.

Deploying requires the owner's Cloudflare account. The Android app talks to this Worker when `TICKETMASTER_PROXY_URL` is set; debug builds may still call Ticketmaster directly with a local `TICKETMASTER_API_KEY`.

**Wrangler:** develop/deploy with Wrangler **3.91+** (rate-limit bindings). `compatibility_date` is `2024-11-11` so older Wrangler CLIs accept the config (R7).

## Endpoints

`GET /discovery/v2/events.json?keyword=…`  
Optional: `classificationName` (default `music`), `sort` (default `date,asc`), `size` (1–50, default 25).  
`apikey` from the client is rejected.

### Rate limit (R5)

~30 requests / IP / minute via the Workers **Rate Limiting** binding `TM_RATE_LIMITER` in `wrangler.jsonc` (not `caches.default`, which is a no-op on `*.workers.dev`).

### Optional client token

If you set secret `DEADSET_CLIENT_TOKEN`, every request must send header `X-DeadSet-Client: <token>`. Leave the secret unset for open local/dev use.

## Deploy

```bash
cd tools/proxy/ticketmaster
npm install -D wrangler@3.91.0
npx wrangler login
npx wrangler secret put TICKETMASTER_API_KEY
# optional:
# npx wrangler secret put DEADSET_CLIENT_TOKEN
npx wrangler deploy
```

Put the Worker URL (no trailing path) into the app `.env` as `TICKETMASTER_PROXY_URL`, e.g. `https://deadset-ticketmaster-proxy.<account>.workers.dev`.

## Local

```bash
npx wrangler dev
# In another shell: curl "http://127.0.0.1:8787/discovery/v2/events.json?keyword=Terrapin%20Flyer"
```

Use a `.dev.vars` file (gitignored) with `TICKETMASTER_API_KEY=…` for local secrets — never commit it.
