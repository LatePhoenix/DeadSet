# DeadSet Ticketmaster proxy

Cloudflare Worker that holds the Ticketmaster Discovery API key as a **secret** and exposes only the music events query the app needs.

Deploying requires the owner's Cloudflare account. The Android app talks to this Worker when `TICKETMASTER_PROXY_URL` is set; debug builds may still call Ticketmaster directly with a local `TICKETMASTER_API_KEY`.

## Endpoints

`GET /discovery/v2/events.json?keyword=…`  
Optional: `classificationName` (default `music`), `sort` (default `date,asc`), `size` (1–50, default 25).  
`apikey` from the client is rejected. Soft limit ~30 requests / IP / minute.

## Deploy

```bash
cd tools/proxy/ticketmaster
npm install -D wrangler
npx wrangler login
npx wrangler secret put TICKETMASTER_API_KEY
npx wrangler deploy
```

Put the Worker URL (no trailing path) into the app `.env` as `TICKETMASTER_PROXY_URL`, e.g. `https://deadset-ticketmaster-proxy.<account>.workers.dev`.

## Local

```bash
npx wrangler dev
# In another shell: curl "http://127.0.0.1:8787/discovery/v2/events.json?keyword=Terrapin%20Flyer"
```

Use a `.dev.vars` file (gitignored) with `TICKETMASTER_API_KEY=…` for local secrets — never commit it.
