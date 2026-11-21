# DeadSet — Living Progress Tracker

This file holds the **working state** of the improvement plan. Any agent (Claude Code, Cursor, or a person) should read it before starting and update it before stopping.

- **Why each item exists:** [`IMPROVEMENT_PLAN.md`](IMPROVEMENT_PLAN.md). It covers the reasoning, feature ideas, and segue-family research. It changes rarely.
- **What's done and what's next:** this file. It changes every session.

---

## How to use this file

1. **Start of session:** read **Current focus** and the newest **Handoff log** entry.
2. **Pick a step:** take the first unchecked `[ ]` step in the current phase. Mark it `[~]` (in progress) and add your tool name, e.g. `[~] (Cursor)`.
3. **Finish a step:** change it to `[x]`, add the short commit hash, and note anything surprising under the step.
4. **End of session:** add a **Handoff log** entry at the top. Say what changed, what's half-done, and what the next agent should do first.
5. **Plan changes:** edit the steps here. Update `IMPROVEMENT_PLAN.md` only if the *reasoning* changed.
6. **Working rules** (one task per turn, tests before commit, ground rules): see [`../AGENTS.md`](../AGENTS.md). Cursor and Claude Code both read it.

Legend: `[ ]` todo · `[~]` in progress · `[x]` done · `[-]` dropped (say why)

---

## Current focus

**Where the code is (read this first):**
- **`main` is the working line** (through C5): Phases 0–1, 3, 4 (except 4.3), 5.1/5.7, seed reset, A1–A3, **C1–C3 + C5**. DB **v11** (`user_show_data` + `shows_with_user` view). Importer: `--fetch`, `--normalize`, `--build-catalog`.
- Next: **C6** catalog tests + DAO segue query (then C7).

**Next up (in order):**
1. **C6** Show-count / Cornell setlist / no-future-date tests against built DB; DAO next-song segue query.
2. **C7** About / credits; then D1/D2 / Phase 5 / Phase 6. **Blocked on owner:** A5 package IDs; 4.3 Ticketmaster key.
3. **C4** Relisten cross-check remains optional — deferred (not blocking).

**Open decisions for the owner**
- [x] **Catalog asset in git.** _Decided (agent):_ `catalog.db` (~10MB) is **gitignored**; regenerate with `./gradlew :tools:importer:run --args=--build-catalog` after `--fetch`. Room `createFromAsset` is used when the file is present.
- [x] **Catalog merge on upgrade.** _Decided (agent):_ fresh installs use `createFromAsset`; existing installs keep `user_show_data` via migrations. Future catalog refresh = upsert catalog tables by PK, never touch `user_show_data`.
- [x] **C4 timing.** _Decided (agent):_ defer optional Relisten `--crosscheck` until after C5 catalog.db; network-heavy and not required to ship the corpus.
- [x] **Importer set-type heuristic (C1).** _Decided (agent, reversible):_ 1–2 sets → `SET_1`/`SET_2`; 3+ sets → last is `ENCORE`, earlier `SET_1`…`SET_3` (capped). gdshowsdb has no set labels. Revisit when C5 maps into Room if real encores are mislabeled.
- [ ] **A5 package/app IDs.** What should the Kotlin package and `applicationId` be (e.g. `app.deadset` / `app.deadset.android`)? Don't change them until the owner answers: changing the app ID orphans existing installs.
- [x] **Seeded attendance and reviews.** _Decided (owner, relayed 2026-11-21): reset the six. Done on branch `phase-5-seed-reset`: seeds ship unattended with no review/rating/companions/favorite; existing installs are cleared only where the data still matches the invented fingerprint (`CuratedShows.legacySeedPersonalData`), so user edits survive. Follow-up (owner, 2026-11-21): the other 10 seeds' invented ratings, reviews and favorite songs are blanked too. Cleanup on existing installs now requires rating, favorite, companions and the full invented review to all match exactly, and resets attendance only for the six where it was invented._ Original note: The seed data marks **6 shows as attended** (5 Grateful Dead incl. Soldier Field '95, plus Terrapin Flyer at Martyrs' 2024), each with an invented first-person review, rating and companions. These show up in My Shows, stats and the chase list as if you went. Options: (a) reset all seed shows to unattended with blank reviews, leaving the shows themselves (recommended; your own edits on a device are kept); (b) keep as demo data; (c) tell me which of these you really attended.
- [x] **Phase 2 dataset** (step 2.1). **Resolved: gdshowsdb** (https://github.com/jefmsmit/gdshowsdb). Verified 2026-11-21: `LICENSE.txt` is **MIT** ("Copyright (c) 2013 Jeff Smith"); data is `data/gdshowsdb/YYYY.yaml` (1965–1995) plus `song_refs.yaml`; each song has `:segued: true|false`. Setlists most likely derive from Deadlists (the repo has `deadlists_parser.rb`, but no explicit statement), so credit both. Rejected: setlist.fm (no copying allowed), gratefulstats.com (terms forbid scraping), jerrybase.com / gdsets.com (no license; owner may ask Jerrybase for permission separately). See Phase 2 below.
- [ ] **4.3 API keys.** Owner has applied for a Ticketmaster key (not received). Bandsintown's placeholder `app_id` is rejected (confirmed on device). Code path for the key lands with D1 (proxy, behind a feature flag); Bandsintown stays config-driven and is skipped when unset.

---

## Environment notes (read before running anything)

| Thing | Detail |
|---|---|
| JDK | Not on PATH. Use Android Studio's: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (bash) or `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` (PowerShell). In Cursor's terminal set it once per shell before any `gradlew` call. |
| Run tests | `./gradlew --offline :app:testDebugUnitTest` (Robolectric, about 20 s once warm). Must pass before every commit. |
| Token economy | Never paste or read whole data files (gdshowsdb YAML, generated DBs, schema JSON) into the chat. Work from small samples, `grep`, and summarized importer output. |
| Full build | `assembleDebug` needs a local `debug.keystore` (git-ignored). One exists on this machine; elsewhere generate it with `keytool -genkeypair -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -validity 10000 -dname "CN=Android Debug,O=Android,C=US"`. |
| Offline | `--offline` works with cached deps. **New dependencies need network** (e.g. `room-testing`, `navigation-compose`). |
| minSdk 24 | **Don't use `java.time`.** Core library desugaring is off. Use `com.example.data.todayIsoDate()` (`data/DateUtils.kt`) or `SimpleDateFormat`. |
| Python | Not installed. Use the Kotlin/Gradle tools or shell scripts. |
| Emulator | AVD `Medium_Phone_API_37.0`. Start headless: `ANDROID_AVD_HOME='C:\Users\mhbro\.android\avd' "$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd Medium_Phone_API_37.0 -no-window -no-audio -gpu swiftshader_indirect`. Then `adb install -r app/build/outputs/apk/debug/app-debug.apk`, `adb shell am start -n com.aistudio.deadset.kxwmtp/com.example.MainActivity`, `adb exec-out screencap -p > shot.png`. A local git-ignored `debug.keystore` was generated so `assembleDebug` works. Create one with `"$JAVA_HOME/bin/keytool"` (see README). |
| Do-not-touch | `signingConfigs` blocks in `app/build.gradle.kts` (originally required by AI Studio; CI must generate `debug.keystore` in a step instead of changing them). |
| Git | Work on a branch per step (`phase-N-step-X.Y-<slug>`), open a PR, merge with a merge commit (keeps the hashes cited here valid). `.idea/` is untracked; don't commit it. |
| GitHub CLI | Not on PATH: `"/c/Program Files/GitHub CLI/gh.exe"` (bash) / `& "C:\Program Files\GitHub CLI\gh.exe"` (PowerShell). Logged in as the repo owner. |
| Code review | **CodeRabbit** reviews each PR automatically (status check "CodeRabbit"). **Don't merge while its status is "Review in progress"**: PRs #1 and #2 merged too early and got no review. Free tier ≈ 1 review/hour; "Review rate limited" means it skipped. Trigger a review manually with a PR comment `@coderabbitai review` once the limit lifts. Reply to each finding on its thread and resolve it. |
| CI | GitHub Actions workflow `.github/workflows/ci.yml` (step A3): unit tests on push/PR to `main`. |

## Conventions

- Package root is `com.example` (scaffold default; renaming it is A5/6.6, needs owner IDs). App ID is `com.aistudio.deadset.kxwmtp`.
- Every user-visible fact (dates, venues, prices, setlists, reviews, segues) must come from a real source or from the user. **No invented data in seeds.** If sources disagree, mark the item uncertain rather than picking one. Tests enforce no future-dated seeds and no invented personal data on seeds.
- **Lean dependencies.** Prefer the standard library, OkHttp and org.json over adding a library.
- **Cross-platform build tooling.** The owner's machine has no Python; any build tool (e.g. the importer) must be Kotlin/JVM via Gradle and work on Windows and Linux.
- **Credentials** come from `.env`/env vars or `local.properties`, never committed. Don't ship secrets in `BuildConfig` for release (see D1).
- **Migrations** read setlist JSON raw (org.json), never through app model classes; parent-table rebuilds must park child rows first (see 1.5/1.6 notes).
- Seed rows that need to be removed from existing installs go in `CuratedShows.retiredSeedShowIds`. They're deleted only if the user never claimed them.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` when Claude authored them.

---

## Phase 0: Stop the bleeding ✅

- [x] Remove Spotify integration; add "Listen on Archive.org" link (`4762086`)
- [x] Delete hard-coded fake tour schedule; Tickets tab shows only API results and drops past dates (`4762086`)
- [x] Remove two invented future-dated Terrapin Flyer seed shows, plus cleanup for existing installs (`4762086`)
- [x] Persist trivia progress and profile (`data/local/UserPrefsStore.kt`, SharedPreferences) (`4762086`)
- [x] Fix Daily Five key (day-of-year → ISO date) and `clearSegueFilter()` tab jump (`4762086`)

---

## Phase 1: Data foundation

Goal: user data survives schema changes; one source of truth for setlists; transitions modelled properly.

- [x] **1.1 Schema baseline.** In `app/build.gradle.kts`, add `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`. Set `exportSchema = true` in `AppDatabase.kt`. Build once at **version 3** so `app/schemas/.../3.json` is generated, then commit it.
  _Done when:_ `3.json` is committed and the DB version is still 3. (_see commit "Export Room schema v3 baseline"_)
  _Note:_ `ShowEntity`/`SongPerformanceEntity` are unchanged since `84e62cb`, so `3.json` (identityHash `2b2e24c2…`, 31 columns, 7 indices) matches the v3 DBs already on devices. Every future entity change must bump `version` and commit the new `N.json`.
- [x] **1.2 Stop destructive migration.** Remove `fallbackToDestructiveMigration()`. Add the `androidx.room:room-testing` dependency (needs network). Add a `MigrationTestHelper` test scaffold (Robolectric or androidTest).
  _Done when:_ the app opens an existing v3 DB with no data loss and the migration test infrastructure runs. (_see commit "Stop destructive migrations"_)
  _How it works now:_
  - `AppDatabase.applyMigrationPolicy()` is the single builder config; the app and the tests both use it.
  - Only versions 1–2 (pre-baseline, schemas unknown) are rebuilt. From v3 on, a missing migration **crashes instead of wiping data**. Checked by temporarily bumping to v4 with no migration: `IllegalStateException`.
  - **Recipe for every future schema change:** bump `AppDatabase.VERSION`, write `Migration(N, N+1)`, add it to `ALL_MIGRATIONS`, run `:app:kspDebugKotlin` to export `N+1.json`, commit it, and add a seeded-data test in `app/src/test/.../data/local/MigrationTest.kt`.
  - Schemas are exposed as **debug** assets (Robolectric can't see `src/test` assets); release APKs don't include them.
- [x] **1.3 Fix seeding foreign-key edge case.** In `ShowRepository.checkAndSeedDatabase()`, the branch where `count > 0` but `perfCount == 0` inserts performances for curated shows that may not exist, which throws `SQLiteConstraintException`. Insert or ignore the curated shows first, or sync only the shows that exist. Add a test. (_see commit "Rebuild song performances from stored shows"_)
  _Also fixed:_ that branch and `restoreCuratedOfflineCatalog()` rebuilt song rows from **seed** setlists, overwriting user segue edits and leaving `setlistJson` and `song_performances` out of sync. Both now rebuild from the stored rows, custom shows included. Tests are in `app/src/test/.../data/repository/SeedingTest.kt`.
- [x] **1.4 `songs` table + name normalization.** Columns: `id`, `canonicalName`, `aliases` (JSON or a child table), `writers`, `debutDate`. Seed canonical names for every song in `CuratedShows`, with aliases such as St. Stephen/Saint Stephen, Lovelight/Turn On Your Love Light, Slipknot!/Slipknot. Add `normalizeSongName()` and route all setlist writes through it.
  _Migration 3→4._ (_see commit "Add songs table and song-name normalization"_)
  _As built:_
  - `data/songs/SongNames.kt`: `songKey()` (case/punctuation/`&`/hyphen/leading-"The" insensitive; doubles as `songs.id`), `SongCatalog` (canonical names + shorthand aliases like GDTRFB, Playin', Lovelight), and `SongResolver`, which the repository **and** the migration share.
  - Tables: `songs(id, canonicalName)`, `song_aliases(aliasKey → songId)`. `song_performances` gains a `songId` FK and index (the table is rebuilt in the migration).
  - Canonical names = the existing seed spellings (e.g. "Saint Stephen"), so nothing visible changed and the old segue filter still works until 1.7. A test enforces that every seed title is already canonical.
  - **Deviation:** `writers`/`debutDate` were left out. They'd ship empty, and those facts need sources (add them with 5.5, in their own migration).
  - Not normalized: free-text fields like `favoriteSong`, and trivia text.
  - Changing a canonical name later needs a migration; `ensureSongCatalog()` uses INSERT IGNORE and won't rename existing rows.
- [x] **1.5 `TransitionType`.** Add an enum (`NONE`, `SEGUE "->"`, `TRANSITION ">"`, `JAM_INTO "~>"`, `TEASE`). Replace `SongEntry.isSegue` / `SongPerformanceEntity.isSegue` with `transitionOut`. The migration maps `isSegue=1 → SEGUE`. Update `SegueIndicatorPill` to cycle through the types and `ShowDetailScreen` to show them.
  _Migration 4→5._ (_see commit "Replace isSegue with TransitionType"_)
  _As built:_
  - `TransitionType { NONE, SEGUE "->", TRANSITION ">", JAM "~>" }` in `Models.kt`, with `next()` (pill cycles Stop → Segue → Transition → Jam) and `connects`. It's stored by enum **name** as TEXT (`song_performances.transitionOut`, JSON `transitionOut`); never rename a constant without a migration.
  - `SongEntry.isConnected` (computed, not serialized) replaces boolean reads in stats, trivia, profile, and the setlist ribbon.
  - **Deviation:** no `TEASE` value. A tease is inside one song, not a junction between two; it gets its own field later. Also `JAM_INTO` is named `JAM`.
  - **Rule learned (applies to every future migration):** migrations must not parse setlists through `Converters`/`SongEntry` (they change over time). Use `rewriteSetlistJson()` (raw org.json) in `Migrations.kt`. `MIGRATION_3_4` was switched to this; the old version would have erased segue tags on v3 installs upgrading to v5. Covered by "a v3 install upgraded all the way keeps its segue tags".
- [x] **1.6 Single source of truth.** Make `song_performances` the canonical setlist store: search, stats, and the trivia generator all read from it. Drop `ShowEntity.setlistJson` (SQLite needs a table rebuild in the migration). Keep `position`, which `toSongEntry()` currently drops.
  _Migration 5→6. Highest-risk step: write the migration test first._
- [x] **1.7 Exact segue query.** Add a DAO query that matches adjacent rows: `a.songId = :from AND b.songId = :to AND b.trackOrder = a.trackOrder + 1 [AND a.transitionOut = 'SEGUE']`. Replace the word-matching segue filter in `DeadSetViewModel.applyFilters`. Add a test showing that "Playing in the Band ➔ Uncle John's Band" no longer matches unrelated shows.
- [x] **1.8 Upcoming is computed, not stored.** Replace reads of `ShowEntity.isUpcoming` with `date > todayIsoDate()`. Drop the column in the 1.6 migration.
- [x] **1.9 Move stats off the main thread.** Run `computeOverallStats` on `Dispatchers.Default` (or as SQL aggregates once 1.6 lands).
  _As built (1.6–1.9 in one PR, all migration 5→6):_
  - **1.6:** `setlistJson` is gone; `song_performances` is the only setlist store. `ShowRepository.saveShow(show, songs)`, `getSetlist(showId)`, `toggleSongTransition(showId, i)`. The ViewModel builds `UiState.songsByShow` from `allShowsWithSongs`; screens get setlists from it (`viewModel.songsFor`). Seeds are `SeedShow(show, songs)`. `Converters` (Moshi JSON) was deleted as dead code. `position` is derived per set in `ShowWithSongs.orderSongs()` (no column). Repair and restore now fill only curated shows that have **no** setlist rows, so edits are never overwritten; a custom show that somehow lost its rows can't be rebuilt (there is no second copy anymore).
  - **Migration gotcha:** rebuilding a parent table (`shows`) means DROP TABLE, which with FKs on would CASCADE-delete child rows. `MIGRATION_5_6` parks `song_performances` in a constraint-free temp table first. Reuse this pattern for any future parent-table rebuild. It also backfills rows from JSON for any show that had JSON but no rows.
  - **1.7:** done in memory (`data/setlist/SegueMatcher.kt`), not as a DAO query: the ViewModel already holds every setlist, and a pure function is easy to test. It matches adjacent songs, in order, with every link connected (any `TransitionType` but NONE), titles resolved through catalog aliases.
  - **1.8:** `ShowEntity.isUpcoming` is an extension property: `date >= today` (a show dated today counts as upcoming). Import `com.example.data.local.isUpcoming`.
  - **1.9:** ViewModel filtering and stats run on `Dispatchers.Default`.

## Phase 1b: Cleanup & safety (from external review, 2026-11-21)

Do these after the working-line PR merges, one per PR (or batch A1–A3).

- [x] **A1 Trim unused dependencies.** (`d9a971a`, PR #6) Removed unused `logging-interceptor`, Firebase BoM, all Firebase library catalog entries, and the `google-services` plugin. Confirmed no `HttpLoggingInterceptor` / Firebase references in `app/src`. `.env.example` uses `YOUR_BANDSINTOWN_APP_ID` sentinel (empty BuildConfig values break javac). `testDebugUnitTest` + `assembleDebug` green.
- [x] **A2 Remove AI Studio leftovers + README.** Deleted tracked `.aistudio/` and `public/` media plus `metadata.json`; added `.aistudio/`, `public/`, `metadata.json` to `.gitignore` (`.idea/` already ignored). README already covered build/JDK/keystore/credits/env vars from the Phase 3–5 docs work; lightly updated Bandsintown env note.
- [x] **A3 GitHub Actions CI.** `.github/workflows/ci.yml`: on push + PR (path-filtered), `concurrency` with cancel-in-progress, JDK 17 Temurin + Gradle cache, generates `debug.keystore` with `keytool` in a step (never committed; `signingConfigs` untouched), runs `:app:testDebugUnitTest`, uploads results with 3-day retention. Release-shrink check deferred to D2.
- [x] **A4 Demo seed data.** Done on the working line: all 16 seeds ship unattended with no invented review/rating/companions/favorite; existing installs are cleaned only where data still matches the invented fingerprint (`CuratedShows.legacySeedPersonalData`).
- [ ] ⏸ **A5 Package + applicationId rename** (= 6.6). **Ask the owner for the IDs first.** Rename `com.example` → owner's package (namespace + directories + Room schema dir name changes with the package!) and `applicationId`. Note: the Room schema folder is named after the DB class's package; move `app/schemas/com.example.data.local.AppDatabase/` accordingly and keep `MigrationTest` green.
- [x] **B1 = 1.6/1.7** (single source of truth + exact segue match). Done in Phase 1. The external review asked for a DAO "song X segued into Y" query; the app uses the in-memory `SegueMatcher` (fine at ~2,400 shows). **Add the DAO query in C6** so importer tests can check segues straight against the built DB.

## Phase 2: Full catalog (gdshowsdb importer)

Source: **gdshowsdb** (MIT) — see Open decisions. Expect ≈2,358 shows, ≈39,774 song performances, ≈16,577 `:segued: true` flags, ≈282 shows with no setlist (figures from the external review; verify with importer output). `:segued:` is yes/no only, so map `true → SEGUE`, `false → NONE` (it can't tell `>` from `->`). Credit gdshowsdb **and** Deadlists in the app.

- [x] **2.1 Choose dataset.** gdshowsdb (see above).
- [x] **C1 / 2.2 `tools/importer` module.** (`d7d1543`) Pure-Kotlin `:shared` holds `songKey` / `SongCatalog` / `SongResolver`; `:app` and `:tools:importer` depend on it. Hand YAML parser (`GdshowsYamlParser`) for gdshowsdb list-of-maps shape (`:sets` → `- :uuid:` → `:songs` → `:name`/`:segued`); skips `---` markers. Cornell fixture + year smoke (1977: 60 shows / 1233 songs / 398 segued). CI paths include `shared/**` and `tools/**`.
  _Note:_ set-type heuristic documented under Open decisions (last of 3+ = ENCORE).
- [x] **C2 Fetch + cache.** (`6756615`) `GdshowsFetcher` (`--fetch` / `--force`) downloads 31 year YAMLs + `song_refs.yaml` into gitignored `tools/importer/cache/`, pins tip SHA via GitHub API, writes committed `tools/importer/provenance.json`. Verified full parse: **2358 shows / 39774 songs / 16577 segued / 282 empty** (matches plan figures). Parser follow-up: same-day keys `YYYY/MM/DD/N:`.
- [x] **C3 Normalize.** (`76ac69a`) `--normalize` parses the cache through `SongResolver`, writes gitignored `out/unmatched.csv` (count, song_key, sample_name). Corpus run: 14746 catalog-matched / 25028 unmatched performances / **399** distinct keys outside the seed-sized `SongCatalog` (expected; C5 will insert corpus songs — aliases still hand-added, never auto-guessed).
- [ ] **C4 Cross-check (optional `--crosscheck` flag).** _Deferred until after C5._ Free Relisten API (no key; ~2,000 calls; cache every response; one request at a time with a delay; honest User-Agent). Compare dates/venues/segues, write `out/discrepancies.csv`, and **mark disagreeing shows uncertain** (a flag column) rather than picking a winner. Archive.org's GratefulDead collection is a second free option; its item identifiers can power a per-show "Listen" link (today's link searches by date).
- [x] **C5 / 2.3 Build `catalog.db` + user-table split.** DB **v11**: `ShowCatalogEntity` / `user_show_data` / `shows_with_user` view; MIGRATION_10_11 + MigrationTest. UI still reads `ShowEntity` (view). `--build-catalog` builds Room-valid SQLite from `schemas/.../11.json` + cache (2358 shows / 438 songs / 39774 perfs; empty `user_show_data`). Asset gitignored (~10MB); `createFromAsset` when present. Merge policy: upsert catalog by id, never touch user tables.
- [ ] **C6 Tests.** Show count ≈2,358 (allow a small range); no future dates; 1977-05-08 setlist matches exactly; add the DAO next-song segue query (B1) and test it against the built DB.
- [ ] **C7 About / credits screen.** gdshowsdb (MIT, with license text), Deadlists, plus Relisten / Archive.org if used.
- [ ] **2.4 Perf check.** Shows, Stats, Segues and the transition map stay smooth with ~2,400 shows (stats already run off the main thread; consider SQL aggregates if needed). Re-enable the 3.3 Drums/Space set-structure split once real data is in.

## Phase 3: Transitions

- [x] **3.1 Segue Families.** Replace `CuratedShows.legendaryTransitions` with a data-driven list (see the table in IMPROVEMENT_PLAN §2.2b). Compute era span and frequency from the catalog; hand-write only the notes. Fact-check the notes.
- [x] **3.2 Song "Ins & Outs" panel.** Show the top incoming and outgoing transitions with percentages, filterable by era.
- [x] **3.3 Set-structure flow view** on Show Detail (Set I | pre-Drums | Drums/Space | post-Space | Encore).
  _As built (3.1–3.3, branch `phase-3-transitions-a`):_
  - `data/setlist/SetlistAnalytics.kt`: `SegueFamilies.all` (10 families), `familyStats()`, `songFlow()`, `notation()`/`notationBySet()`. Pure functions, covered by `SetlistAnalyticsTest`.
  - **Facts policy:** family notes describe only the music (meter, feel, structure). Every date, count and "example show" is computed from the catalog ("3 shows in your catalog · 1977–1980"). The old `legendaryTransitions` and the `LegendarySegue` model, which made historical claims, were deleted.
  - **3.2 deviation:** the Song Flow Explorer is a panel on the Segues tab (chips for the top songs), not a per-song page, because there is no song page yet. It has no era filter yet; add one when a song page exists (5.6 cross-linking).
  - **3.3 deviation:** a "Setlist at a glance" card in standard notation (`Scarlet Begonias -> Fire on the Mountain, Estimated Prophet`), one line per set. The pre-Drums / Drums-Space / post-Space split was skipped: it only works once Drums/Space are reliably present (Phase 2 data).
  - The pill and the ribbon below a song now share one color per transition type (`transitionAccent()`).
  - **Checked on the emulator** (Medium_Phone_API_37.0): Segues tab, flow explorer, family → filtered show list (3 Scarlet > Fire shows), pill cycling on Cornell.
- [x] **3.4 Quiz modes.** "What comes next?" (distractors = real exits from song A), "Complete the suite," "Fill the sandwich," "Name that era," "Build Set Two."
- [x] **3.5 Spaced repetition.** A Leitner box per transition pair, stored in Room.
  _As built (3.4–3.5, branch `phase-3-transitions-b`, DB v6→v7):_
  - New **Segue School** mode (`TriviaGameMode.SEGUE_SCHOOL`, 10 questions) on the Trivia tab. Generators live in `data/setlist/TransitionQuiz.kt`: complete-the-chain (from `SegueFamilies`), most-often-next, fill-the-sandwich, and name-that-era. Each question exists only if it has exactly one defensible answer, and its liner note cites the catalog evidence.
  - **Deviation from plan:** "What comes next?" became "which song does X **most often** flow into", with distractors = songs that **never** followed X. The plan's "distractors = real exits" would have made wrong options also correct. It's skipped when the top exits tie.
  - **Deferred:** "Build Set Two" (drag-to-order) needs a reorder UI; revisit with Phase 6 UI work.
  - **3.5:** `data/review/Leitner.kt` (5 boxes; intervals 0/1/3/7/14 days; a miss goes back to box 1). The `review_cards` table (`MIGRATION_6_7`, additive) is keyed by question id, which is stable per card. The deck orders due cards (weakest first), then new cards, then not-due cards. `DeadSetViewModel.recordCardAnswer()` is called per answer in Segue School only.
  - **Checked on the emulator:** an upgrade from a real v6 DB to v7 opened cleanly; Segue School runs; a wrong answer shows the evidence note. A bug found there and fixed: "Brent Mydland Era era" (now guarded by a test).
- [x] **3.6 Transition graph** (chord or force-directed); tapping an edge filters shows.
  _As built (branch `phase-3-transitions-c`):_ a "Transition map" card on the Segues tab (`ui/components/TransitionGraphCard.kt`), built by `SetlistAnalytics.transitionGraph()`. The 14 most-connected songs sit on a circle, with a curved chord per connected transition (width/opacity = count). **Deviation:** you tap a **song** (not an edge) to highlight its links; tapping thin curves on a phone is unreliable, so the song's transitions are listed below as rows, and tapping a row filters the show list. Labels go beside side nodes and above/below top/bottom nodes. Checked on the emulator.

## Phase 4: Tribute band tracking

- [x] **4.1 `bands` table.** Replace the `TributeBandFilter` enum. Seed Terrapin Flyer (starred default), DSO, JRAD, Melvin Seals & JGB, Bob Weir & Wolf Bros, Dead & Company; let the user add bands.
- [x] **4.2 `events` table + cache.** Store each event's source and last-checked time and show them on the card. Refresh with WorkManager about every 12 hours (new dependency).
- [ ] ⏸ **Needs owner:** **4.3 Credentials.** Owner applied for a Ticketmaster key (pending). Bandsintown app ID optional (skipped when unset). Add setlist.fm for past tribute setlists (D3).
- [x] **4.4 Manual add / paste-link import** for local shows (Terrapin Flyer).
- [x] **4.5 Near me.** Sort by distance from the profile home city, with a radius slider, replacing the hard-coded Midwest state set.
  _As built (4.1, 4.2, 4.4, 4.5, branch `phase-4-tribute-a`, DB v7→v8):_
  - **4.1:** `bands` table (`data/local/ConcertEntities.kt`). It's seeded with 6 real band names; Terrapin Flyer, DSO and JRAD are followed (the bands the app queried before). The Tickets tab has a **Bands** dialog to follow/unfollow and add your own (user-added bands can be deleted; built-ins can only be unfollowed). Terrapin Flyer's chip comes first. `TributeBandFilter`, `searchLiveConcerts`, `filterConcertEvents` and `isMidwest` were removed.
  - **4.2:** `events` cache + `ConcertRepository`. `refresh()` runs when the tab opens or the refresh button is tapped, queries only followed bands, and **replaces the cache only if a source answered**, so a failed refresh never wipes good dates. Past dates are dropped. Each card shows "source · checked N min ago · ~N mi". The empty state says honestly which situation applies (no keys/connection, not following anything, filters too narrow, or no dates listed). Periodic background refresh is left to 4.6 (WorkManager).
  - **4.4:** the show editor has a **Band** field with one-tap chips (Grateful Dead + followed bands). A new show counts as attended only if its date has passed; upcoming ones appear under "Upcoming". **Bug fixed:** editing a show used to rebuild it from scratch, wiping trivia, archive id, tags, companions and favorite song, and turning built-in shows into custom (deletable) ones. `buildEditedShow()` now copies the existing show. Paste-link import is not done (needs a parser per site; low value until 4.3 is decided).
  - **4.5:** venue coordinates come from both APIs. The home city is geocoded on device with Android `Geocoder` (no key). Radius chips: any / ≤100 / ≤250 / ≤500 mi, replacing the Midwest toggle. Events without coordinates show only under "Any distance".
  - **Checked on the emulator:** real v7→v8 upgrade; Chicago geocoded; with no keys the honest empty state appears (Bandsintown rejects the placeholder `app_id`, which confirms 4.3 needs the owner).
- [x] **4.6 Notifications.** New-date alerts for followed bands; day-of reminder.
  _As built (branch `phase-4-tribute-d`):_ an **Alerts** chip on the Tickets tab (off by default; on Android 13+ it asks for notification permission first). When on, `ConcertAlertsWorker` (WorkManager, new dependency `work-runtime-ktx` 2.10.0, every ~12 h, needs network) refreshes followed bands. It notifies about dates that weren't cached before (never on the first sync, so turning alerts on doesn't dump every known date as "new") and sends one "Tonight: …" reminder per day for catalog shows dated today. Rules are pure (`data/alerts/ConcertAlertRules.kt`, tested). Prefs: `UserPrefsStore.alertsEnabled` etc. **Checked on the emulator:** permission prompt shown, the job ran ("Worker result SUCCESS"). A real "new date" notification can't be shown until 4.3 provides a working source.
- [x] **4.7 Song chase list** ("heard 112/180 live") and cross-band stats.
  _As built (branch `phase-4-tribute-b`, DB v8→v9):_ a Song Chase card on the My Shows tab (`ui/components/SongChaseCard.kt`, `data/setlist/ChaseProgress.kt`, `chase_songs` table). Chased songs resolve to canonical songs, so "St Stephen" and "Saint Stephen" count once. Each song shows where you first heard it (any band); unheard songs are listed first. Plus a "Shows by band" card.
  - **Bug fixed:** the "Live Songs Seen" tile used `topSongsAttended.size`, which is capped at 15; it now counts distinct songs heard (80 on the seed data).
  - **Bug found on device and fixed:** "+" used to add any typed text as a new song (e.g. "Mississippi Half"). It's now enabled only for a known title (aliases included); otherwise you pick a suggestion or tap an explicit "Add 'X' as a new song" chip.
- [x] **4.8 DSO recreation link.** Match a tribute setlist to the original GD date and show them side by side.
  _As built (branch `phase-4-tribute-e`, DB v9→v10, additive `shows.recreatesShowId`):_ `RecreationMatcher` aligns a tribute setlist with a GD show (LCS over canonical song keys, so skips and extras don't cascade) and scores the share played in order. Tribute show pages offer "Link as a recreation of <date>" when a catalog show matches at least 60% (needs 5+ songs), and once linked show a side-by-side comparison with a match %. Not yet checked on the emulator.
- [x] **4.9 Live setlist capture + next-song predictor** at shows (uses the Phase 3 transition probabilities).
  _As built (branch `phase-4-tribute-c`):_ a "Track it live" card on Show Detail for shows that haven't happened yet (`ui/components/LiveTrackerCard.kt`). Pick the set; it suggests the 3 likeliest next songs from what followed the last song (same set, any transition) across the catalog, or the set's usual openers when empty (`SetlistAnalytics.predictNext`). Tap a guess or type any song (same known-title/explicit-new rule as the chase list). `ShowRepository.appendSong` inserts at the end of the chosen set. A "Predicted N of M" score is kept for the session. Uses all adjacency, not only connected transitions, because "what's next" includes stops.
  - **Bugs fixed while testing on the emulator:** (1) the Setlists header's **Add Show button was squeezed to zero width** on phones, so you couldn't add a show unless the list was empty. The title column now takes `weight(1f)` and the button is a compact icon button. (2) An **upcoming show couldn't be saved without a setlist**; now only past shows require one. (3) Terrapin Flyer is listed first among the editor's band chips.
- [ ] **D1 Keep the Ticketmaster key out of the APK.** Anything in `BuildConfig` can be extracted. Build a tiny proxy (e.g. a Cloudflare Worker, key held as a Worker secret, exposing only the one Discovery query the app needs, with a per-IP rate limit). App points at the proxy URL behind a feature flag; debug builds may still call Ticketmaster directly with a local key. Commit the Worker source under `tools/proxy/`, never the secret. Deploying it needs the owner's Cloudflare account.
- [ ] **D2 Slim the release build.** Turn on R8 (`isMinifyEnabled = true`) with keep rules for Room entities/DAOs. Replace Retrofit + Moshi + `KotlinJsonAdapterFactory` (pulls in kotlin-reflect for two endpoints) with plain OkHttp + `org.json`, mirroring how `ConcertRepository` already uses an injectable fetch. Add `:app:minifyReleaseWithR8` to CI.
- [ ] **D3 (note, don't build yet) Past tribute setlists from setlist.fm.** Free non-commercial API key; fetch on demand, show attribution, don't bundle or keep long-term copies (its terms). Tours stay with Ticketmaster/Bandsintown (SeatGeek is another option).

## Phase 5: Almanac (trivia + articles)

- [x] **5.1 Per-question answer history.** Stop repeating questions the user has mastered; show accuracy by category.
  _As built (branch `phase-5-almanac-a`):_ every quiz answer, in every mode, now updates the per-question Leitner state in `review_cards` (previously Segue School only). Classic/Blitz/category decks lead with **unseen** questions, then due/weak ones, then mastered-not-due ones (Segue School keeps due-first for review). The Trivia hub shows "Your accuracy by category" (`AnswerHistory.byCategory`). A test enforces that question ids are unique, since they key the history.
- [ ] **5.2 Fact-check pass** on `DeadTriviaSeed` and `historicTrivia`. Add a `sourceUrl` to `TriviaQuestion`.
- [ ] **5.3 New question types.** True/false, order-the-dates, who-sang-it, venue→city, lyric→song (paraphrase only, never quoted lyrics).
- [ ] **5.4 Article format.** _(Drafting rule from D4: if trivia or articles are ever machine-drafted, do it offline, e.g. a local Ollama run, and fact-check every claim against the catalog or a cited source before committing. Never call an LLM API from the app.)_ Markdown with front matter in `assets/almanac/`, rendered by a small Compose Markdown renderer.
- [ ] **5.5 Article sets.** Song Stories (data-driven + notes), Show Spotlights, Band & Era Guides, Tribute Band Profiles (Terrapin Flyer first).
- [ ] **5.6 Cross-linking.** Setlist song → Song Story; quiz explanation → "Read more"; Today in Dead History → Spotlight.
- [x] **5.7 Firebase AI decision.** Either use it for drafts that a person fact-checks, or remove `firebase-ai` / App Check dependencies.
  _Decision:_ removed (commented out in 5.7, then fully dropped in A1) `firebase-ai`, App Check, Firebase BoM and google-services. No code used them. Re-add only if Gemini-assisted drafting is wanted later; any drafted facts would still need a human source check (5.2).

## Phase 6: Polish & architecture

- [ ] **6.1 Navigation-Compose + 5 tabs:** Shows · Live · Segues · Almanac · Me.
- [ ] **6.2 Split `DeadSetViewModel`** into per-feature ViewModels; add a DI container.
- [ ] **6.3 Break up large screens** (`TriviaScreen`, `UserProfileScreen`, `StatsScreen`).
- [ ] **6.4 Backup/export** of user data (JSON via the system file picker).
- [ ] **6.5 Share cards, home-screen widget, accessibility pass.**
- [ ] **6.6 Rename namespace and applicationId.** Same as A5 (owner IDs needed). `.idea/` goes into `.gitignore` in A2.
- [ ] **6.7 Test coverage:** ViewModel and filter tests, MockWebServer for the APIs, Compose UI tests for the main flows.

---

## Handoff log

_Newest first. Keep entries short: what changed, what's half-done, next action._

### 2026-11-21 · Cursor (C5 user_show_data + catalog builder)
- Branch `phase-2-c5-user-show-data`: DB v11 — user fields → `user_show_data`; `shows_with_user` view keeps UI `ShowEntity`. MigrationTest 10→11. Importer `--build-catalog` → local `app/src/main/assets/catalog.db` (gitignored).
- **Decisions:** catalog.db not committed (~10MB); createFromAsset optional; catalog upsert never touches user table.
- **Next:** C6 tests + DAO segue query; then C7 credits. C4 still deferred.

### 2026-11-21 · Cursor (C1–C3 merged; pause before C5)
- Merged PR #9 C1 (`d7d1543`), #10 C2 (`6756615`), #11 C3 (`76ac69a` + CSV formula fix `465ee0b`). CodeRabbit findings on #11 fixed; rate-limited on #9/#10 (skipped, CI green).
- Importer corpus verified: 2358 / 39774 / 16577 / 282 empty. Normalize: 399 unmatched keys outside seed catalog.
- **Decisions:** set-type last-of-3+=ENCORE; defer optional C4 Relisten until after C5; do not prune remote `phase-*` without owner OK.
- **Next agent:** C5 on `phase-2-c5-user-show-data` — first migration 10→11 extracting `user_show_data` (~18 Kotlin files touch `isAttended`/`userRating`/…), then asset `catalog.db` builder from cache + Room schema. Skip C4 for now.

### 2026-11-21 · Cursor (C3 normalize)
- Branch `phase-2-c3-normalize`: `GdshowsNormalizer` + `--normalize` → `out/unmatched.csv`. No alias auto-insert.
- Top unmatched are missing catalog songs (Me And My Uncle, Jack Straw…), not spelling collisions. Alias pass deferred until owner wants seed-catalog expansion.
- **Next:** C5 (user columns out of `shows` + asset DB); C4 deferred.

### 2026-11-21 · Cursor (C2 fetch + cache)
- Branch `phase-2-c2-fetch-cache`: `GdshowsFetcher` + `--fetch`; provenance at `tools/importer/provenance.json` (sha `2ccd86c1…`). Cache gitignored.
- Full-corpus parse matches external-review counts. Fixed same-day show keys (`/N` suffix) in the YAML parser.
- **Next:** C3 normalize → `out/unmatched.csv` (aliases still hand-added).

### 2026-11-21 · Cursor (C1 :shared + importer)
- Branch `phase-2-c1-importer-shared`: moved song normalization into `:shared`; added `tools:importer` with hand YAML parser + Cornell/empty-setlist/document-marker tests; app seed-title check stays in `:app`.
- Tests: `:shared:test`, `:tools:importer:test`, `:app:testDebugUnitTest` green. Smoke-parsed cached `1977.yaml`.
- **Decision (reversible):** 3+ sets → last `ENCORE` (see Open decisions).
- **Next:** C2 fetch+cache on `phase-2-c2-fetch-cache` from updated `main`.

### 2026-11-21 · Cursor (A1–A3 done; pause before C1)
- Merged PR #5 (`4604ad0` + CR fixes `f2812e0`), A1 PR #6 (`d9a971a`), A2 PR #7, A3 PR #8 (`06ebe67`/`d0547b8`, CI green).
- **Decisions made for owner review:** deferred CR heavy lifts (multi-band events, refresh race, cross-source IDs, graph a11y); Bandsintown skipped when unset; exact invented-review fingerprint.
- Local branch `phase-2-c1-importer-shared` was cut but not started. **Next agent:** implement C1 from `main`. Ask owner for A5 IDs / 4.3 keys when needed.
- Remote prune of old stacked phase-* branches still needs owner OK (sandbox blocked bulk delete).

### 2026-11-21 · Cursor (A2 AI Studio cleanup)
- PR #5 merged (`4604ad0`). A1 merged as PR #6 (`e8ef86c` / `d9a971a`). A2 on `phase-1b-a2-ai-studio-cleanup`: removed tracked `.aistudio/` + `public/` + `metadata.json`, gitignored them. README already present.
- Remote delete of old stacked phase branches was blocked by sandbox; owner can prune when ready.
- **Next:** merge A2 (PR #7), then A3 CI.

### 2026-11-21 · Cursor (PR #5 merged; A1)
- Merged PR #5 → `main` (`4604ad0`) after CodeRabbit review + `f2812e0` fixes. Deferred heavy lifts noted on the PR.
- **A1** (`d9a971a`, PR #6): dropped logging-interceptor, Firebase BoM/libs, google-services. Tests + assembleDebug pass.

### 2026-11-21 · Cursor (PR #5 CodeRabbit fixes)
- Addressed clear CodeRabbit findings on PR #5 (`phase-5-coderabbit-fixes`): exact invented-review fingerprint, setlist/review mutexes, chase card when attendance empty, live-tracker set-scoped predictions + SET_3, same-set analytics, chain distractor exclusions, tonight-reminder only when notify posts, home-city stale-lookup guard, ISO date validation, followed-band alert query, Bandsintown skipped when unset (no placeholder), MigrationTest 6→10 seeded cases, README/AGENTS docs.
- **Deferred (heavy lift / later):** multi-band event identity, refresh race coordination, cross-source concert dedupe, graph keyboard a11y (Phase 6). Noted for owner in PR reply.
- Unit tests pass. **Next:** push onto PR #5, wait for CodeRabbit re-check, merge, then A1.

### 2026-11-21 · Claude Code (external review triage + handoff to Cursor)
- Owner had another AI review the project; each suggestion was checked against the code and the sources. **Accepted** into the plan: A1, A2, A3, A5 (Phase 1b), C1–C7 (Phase 2, gdshowsdb), D1, D2 (Phase 4), D3 and D4 as notes. **Already done:** A4 (seed reset on the working line), B1 (Phase 1 steps 1.6/1.7; the DAO segue query is folded into C6). **Adjusted:** C1 shares `songKey`/`SongResolver` through a `:shared` module instead of copying them; C5 must first move user columns out of `shows` and build the asset DB from Room's exported schema so `createFromAsset` validates. **Rejected sources:** gratefulstats.com (terms forbid scraping), jerrybase.com / gdsets.com (no license). gdshowsdb's MIT license was verified directly.
- Merged `main` (PR #4 fixes) into `phase-5-seed-reset` (`aae31a4`); 97/97 tests pass. Docs rewritten for Cursor: `AGENTS.md` (workflow + ground rules), new `README.md`, this file.
- **Not checked on the emulator yet:** 4.8 recreation link, seed reset on an existing install.
- **Next action (Cursor):** PR #5 (`phase-5-seed-reset` → `main`) is open; wait for CodeRabbit, fix findings, merge. Then A1.

### 2026-11-21 · Claude Code (Phases 3–4, autonomous run)
- **Branch stack** (each builds on the last; open PRs in order, each after the previous merges): PR #4 `phase-1-step-1.6-single-setlist` (awaiting CodeRabbit; it was rate-limited) → `phase-3-transitions-a` (3.1–3.3) → `-b` (3.4–3.5) → `-c` (3.6) → `phase-4-tribute-a` (4.1/4.2/4.4/4.5) → `-b` (4.7) → `-c` (4.9) → `-d` (4.6). Plan: one PR for all of Phase 3 (from `-c`) and one for Phase 4 (from `-d`), because CodeRabbit's free tier allows ~1 review/hour.
- DB is **v9**; every step's migration is covered by `MigrationTest` and was run for real on the emulator. 81/81 tests pass.
- Every feature was checked on the emulator (see Environment notes for the commands). Bugs found that way and fixed: unreachable Add Show button; upcoming shows needing a setlist; "Era era" wording; chase "+" creating junk songs; "Live Songs Seen" capped at 15; editing a show wiping hidden fields.
- **Blocked on owner:** Open decisions (seed attendance/reviews, Phase 2 dataset, 4.3 keys).
- **Next unblocked:** 4.8 (DSO recreation link), Phase 5 (5.1, 5.3–5.7), Phase 6.

### 2026-11-21 · Claude Code (steps 1.6–1.9)
- PR #3 (1.5) merged as `44ac50b` after CodeRabbit's review; its 2 findings were fixed in `8efc907`.
- Branch `phase-1-step-1.6-single-setlist`: DB v5→v6, setlist JSON removed, `isUpcoming` computed, exact `SegueMatcher`, stats off the main thread. 42/42 tests pass (new: 5→6 migration test, SegueMatcherTest ×7, 3 more SeedingTest cases).
- Bug found and fixed while testing: the matcher didn't link "St. Stephen" to "Saint Stephen" (aliases need the catalog, not just `songKey`).
- Not run on a device.
- **Next:** PR, CodeRabbit, merge. Then Phase 2 needs the owner (dataset/license).

### 2026-11-21 · Claude Code (step 1.5)
- Step 1.4 merged as PR #2 (`64399d3`). Step 1.5 is on `phase-1-step-1.5-transitions`: DB v4→v5, `isSegue` → `transitionOut` (`TransitionType`), and the pill cycles 4 states with a color per type. `MIGRATION_3_4` decoupled from the model classes (see the rule under 1.5).
- 33/33 tests pass. New: 4→5 test, v3→v5 chain test, `TransitionTypeTest` (4). Mutation check: making 3→4 drop `isSegue` fails both the 3→4 and the chain tests.
- CodeRabbit didn't review PRs #1/#2 (merged too early). Leave the 1.5 PR open until it posts.
- Not run on a device. The pill's new colors/labels haven't been seen on screen.
- **Next:** push 1.5, open PR, wait for CodeRabbit, address findings, merge. Then step 1.6.

### 2026-11-21 · Claude Code (step 1.4)
- DB v3→v4: `songs` + `song_aliases` tables, `song_performances.songId`, titles normalized everywhere setlists are written (save, seed, restore, repair) and in existing data via `MIGRATION_3_4`. `ShowRepository` now takes `(showDao, songDao)`.
- 27/27 tests pass. New: `SongNamesTest` (6); the 3→4 data test in `MigrationTest`; 2 more in `SeedingTest`. Mutation check: disabling the migration's JSON normalization fails the 3→4 test.
- On branch `phase-1-step-1.4-songs`, not merged. Not run on a device.
- **Next:** step 1.5 (`TransitionType`, 4→5).

### 2026-11-21 · Claude Code (step 1.3)
- Seeding repair and "restore catalog" now rebuild `song_performances` from the stored shows (no FK crash, user segue edits kept). Added `ShowDao.getAllShowsOnce()` and `SeedingTest` (3 tests); 18/18 pass. On `phase-1-data-foundation`, not merged.
- **Next:** step 1.4, schema 3→4 (`songs` table). Write the migration test first.

### 2026-11-21 · Claude Code (step 1.2)
- Removed blanket `fallbackToDestructiveMigration()`. Added `room-testing` 2.7.0 (now cached, so `--offline` works again) and `MigrationTest` (3 tests). 15/15 tests pass. On `phase-1-data-foundation`, not merged.
- **Next:** step 1.3. Then 1.4 is the first real migration (3→4); follow the recipe under 1.2.

### 2026-11-21 · Claude Code (step 1.1)
- Room schema export enabled; `app/schemas/com.example.data.local.AppDatabase/3.json` committed on `phase-1-data-foundation` (not yet merged to `main`). 12/12 tests pass.
- Regenerate schemas with `./gradlew --offline :app:kspDebugKotlin`.
- **Next:** step 1.2. Add `room-testing` (online), then remove `fallbackToDestructiveMigration()`.

### 2026-11-21 · Claude Code
- Wrote `IMPROVEMENT_PLAN.md` and this tracker. Completed all of Phase 0 (`4762086`, fast-forwarded into `main`). 12/12 unit tests pass. Not yet run on a device.
- Found while testing: the seeding FK edge case (now step 1.3). Retired seed rows had filler `userReview` text, so the cleanup keys on `isAttended`/`attendedWith` instead.
- **Next:** step 1.1, schema baseline.
