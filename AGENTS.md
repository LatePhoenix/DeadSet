# Agent instructions — DeadSet

DeadSet is a Grateful Dead setlist, segue, trivia, and tribute-show companion for Android (Kotlin, Jetpack Compose, Room). These rules apply to every agent: Cursor, Claude Code, or anyone else.

## Before you do anything
1. Read **[docs/PROGRESS.md](docs/PROGRESS.md)**: "Where the code is", "Next up", open decisions, environment notes, and the newest Handoff log entry.
2. Background and reasoning for each step are in [docs/IMPROVEMENT_PLAN.md](docs/IMPROVEMENT_PLAN.md) (section 5 covers the external-review items).
3. Check out the branch PROGRESS.md names as the working line before you change anything.

## Workflow (one task per turn)
1. Take **one** step from PROGRESS.md (first unchecked one in "Next up" order). Mark it `[~] (Cursor)` or `[~] (Claude Code)`.
2. Work on a `phase-N-<slug>` branch cut from the working line.
3. Make the change, then run the tests (below). Fix any failure before moving on.
4. Summarize what changed and what you checked, then **stop and wait for the owner to say "next"**.
5. **Don't commit anything the owner hasn't reviewed.** Once they approve: commit, push, open a PR into `main`, wait for CodeRabbit (see PROGRESS.md "Code review"), fix its findings, then merge.

## Build and test
- Set JAVA_HOME first (no JDK on PATH):
  - PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`
  - bash: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`
- Tests: `./gradlew --offline :app:testDebugUnitTest` (drop `--offline` if a new dependency must download). Must pass before every commit.
- Build: `./gradlew :app:assembleDebug` (needs a local `debug.keystore`; see README).

## Ground rules
- **Never invent user-visible facts** (dates, venues, setlists, segues, prices, reviews, ratings). Everything comes from a real source or from the user. If sources disagree, mark the item uncertain; don't pick one.
- **Data sources:** gdshowsdb (MIT) is the catalog source; credit it and Deadlists. Never scrape gratefulstats.com (terms forbid it), jerrybase.com or gdsets.com (no license). setlist.fm data may be fetched on demand with attribution, never bundled.
- **Lean dependencies:** use the standard library, OkHttp and org.json before adding a library. No YAML library for the importer.
- **Cross-platform tooling:** the owner has no Python. Build tools must be Kotlin/JVM run through Gradle and work on Windows and Linux.
- **Credentials** come from `.env` / env vars / `local.properties`, never committed. Never commit `.env`, `debug.keystore`, or `.idea/`.
- **Token economy:** never paste or read whole data files (gdshowsdb YAML, generated DBs, Room schema JSON) into the chat. Use one small sample, `grep`, and summarized tool output.
- Don't use `java.time` (minSdk 24, no desugaring); use `com.example.data.todayIsoDate()`.
- Don't modify the `signingConfigs` blocks in `app/build.gradle.kts`.
- Don't rename the package or `applicationId` until the owner gives the new IDs (step A5).
- Every schema change gets a Room migration plus a `MigrationTest` case; migrations read JSON raw (org.json), never through model classes.

## Before you stop
- Tick finished steps with the commit hash, and add notes under the step if anything surprised you.
- Add a dated **Handoff log** entry at the top of the log in PROGRESS.md: what changed, what's half-done, and the next action.
