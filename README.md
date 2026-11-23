# DeadSet

An Android companion for Grateful Dead fans: browse setlists and segues, learn common song transitions (Segue School with spaced repetition), play trivia, track the shows you've been to, and follow tribute bands like Terrapin Flyer, with alerts when they announce dates near you.

Built with Kotlin, Jetpack Compose and Room. minSdk 24.

## Status

The plan and its progress are in [docs/PROGRESS.md](docs/PROGRESS.md); the reasoning is in [docs/IMPROVEMENT_PLAN.md](docs/IMPROVEMENT_PLAN.md). Agents (Cursor, Claude Code) follow [AGENTS.md](AGENTS.md).

## Build

1. Point `JAVA_HOME` at a JDK 17+ (Android Studio's bundled JBR works):
   - Windows PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`
   - Windows bash / Git Bash: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`
   - Linux: `export JAVA_HOME="/opt/android-studio/jbr"` (or wherever your Studio/JDK install lives)
2. Create a local debug keystore once (it's git-ignored; the build's `signingConfigs` expect it in the project root). Invoke `keytool` from `$JAVA_HOME`:

   ```bash
   "$JAVA_HOME/bin/keytool" -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```

   PowerShell:

   ```powershell
   & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```

3. Run the tests: `./gradlew :app:testDebugUnitTest`
4. Build and install: `./gradlew :app:installDebug`

## Configuration

Copy `.env.example` to `.env` (never commit it). Both values are optional; without them the Tickets tab shows an honest empty state.

| Variable | What it's for |
|---|---|
| `TICKETMASTER_PROXY_URL` | Cloudflare Worker URL for Ticketmaster (preferred; see `tools/proxy/ticketmaster/`) |
| `TICKETMASTER_API_KEY` | Debug-only direct Discovery key (ignored in release without a proxy) |
| `DEADSET_CLIENT_TOKEN` | Optional shared token with Worker secret; sent as `X-DeadSet-Client` on proxy calls when set |
| `BANDSINTOWN_APP_ID` | Registered Bandsintown app ID (`YOUR_BANDSINTOWN_APP_ID` / blank skips requests) |

## Data sources and credits

- Curated show data in the app comes from public sources; nothing user-visible is invented.
- Full catalog (planned, Phase 2): [gdshowsdb](https://github.com/jefmsmit/gdshowsdb) by Jeff Smith, MIT license. Its setlists appear to derive from Deadlists, which is credited too.
- Upcoming concerts: Ticketmaster and Bandsintown, when keys are configured.
- "Listen" links open the [Internet Archive](https://archive.org/details/GratefulDead) Grateful Dead collection.
