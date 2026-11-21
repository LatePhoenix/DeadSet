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

Legend: `[ ]` todo · `[~]` in progress · `[x]` done · `[-]` dropped (say why)

---

## Current focus

**Phase 1: Data foundation.** Start with step 1.1, the schema baseline. Every later step depends on it.

**Open decisions for the owner**
- [ ] Demo show `2024-05-18-terrapin-flyer-martyrs` (`data/seed/CuratedShows.kt`) ships pre-marked as **attended**, with an invented review and companions. Choose one: delete it, keep it unattended as a demo, or replace it with a real show you attended.
- [ ] Phase 2 dataset source and license (see step 2.1).
- [ ] Ticketmaster API key / Bandsintown registered app ID (needed for Phase 4).

---

## Environment notes (read before running anything)

| Thing | Detail |
|---|---|
| JDK | Not on PATH. Use Android Studio's: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (bash) or `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` (PowerShell) |
| Run tests | `./gradlew --offline :app:testDebugUnitTest` (Robolectric, about 20 s once warm) |
| Full build | `assembleDebug` fails at `validateSigningDebug` because `debug.keystore` is git-ignored and absent. This predates Phase 0. Build from Android Studio, or generate a local debug keystore. |
| Offline | `--offline` works with cached deps. **New dependencies need network** (e.g. `room-testing`, `navigation-compose`). |
| minSdk 24 | **Don't use `java.time`.** Core library desugaring is off. Use `com.example.data.todayIsoDate()` (`data/DateUtils.kt`) or `SimpleDateFormat`. |
| Python | Not installed. Use the Kotlin/Gradle tools or shell scripts. |
| Do-not-touch | `signingConfigs` blocks in `app/build.gradle.kts` (required by AI Studio). |
| Git | Work on a branch per phase (`phase-N-<slug>`), fast-forward into `main`. `.idea/` is untracked; don't commit it. |

## Conventions

- Package root is `com.example` (scaffold default; renaming it is step 6.6). App ID is `com.aistudio.deadset.kxwmtp`.
- Every user-visible fact (dates, venues, prices, setlists) must come from a real source or from the user. **No invented data in seeds.** Tests enforce "no future-dated seed shows."
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

- [ ] **1.1 Schema baseline.** In `app/build.gradle.kts`, add `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`. Set `exportSchema = true` in `AppDatabase.kt`. Build once at **version 3** so `app/schemas/.../3.json` is generated, then commit it.
  _Done when:_ `3.json` is committed and the DB version is still 3.
- [ ] **1.2 Stop destructive migration.** Remove `fallbackToDestructiveMigration()`. Add the `androidx.room:room-testing` dependency (needs network). Add a `MigrationTestHelper` test scaffold (Robolectric or androidTest).
  _Done when:_ the app opens an existing v3 DB with no data loss and the migration test infrastructure runs.
- [ ] **1.3 Fix seeding foreign-key edge case.** In `ShowRepository.checkAndSeedDatabase()`, the branch where `count > 0` but `perfCount == 0` inserts performances for curated shows that may not exist, which throws `SQLiteConstraintException`. Insert or ignore the curated shows first, or sync only the shows that exist. Add a test.
- [ ] **1.4 `songs` table + name normalization.** Columns: `id`, `canonicalName`, `aliases` (JSON or a child table), `writers`, `debutDate`. Seed canonical names for every song in `CuratedShows`, with aliases such as St. Stephen/Saint Stephen, Lovelight/Turn On Your Love Light, Slipknot!/Slipknot. Add `normalizeSongName()` and route all setlist writes through it.
  _Migration 3→4._
- [ ] **1.5 `TransitionType`.** Add an enum (`NONE`, `SEGUE "->"`, `TRANSITION ">"`, `JAM_INTO "~>"`, `TEASE`). Replace `SongEntry.isSegue` / `SongPerformanceEntity.isSegue` with `transitionOut`. The migration maps `isSegue=1 → SEGUE`. Update `SegueIndicatorPill` to cycle through the types and `ShowDetailScreen` to show them.
  _Migration 4→5._
- [ ] **1.6 Single source of truth.** Make `song_performances` the canonical setlist store: search, stats, and the trivia generator all read from it. Drop `ShowEntity.setlistJson` (SQLite needs a table rebuild in the migration). Keep `position`, which `toSongEntry()` currently drops.
  _Migration 5→6. Highest-risk step: write the migration test first._
- [ ] **1.7 Exact segue query.** Add a DAO query that matches adjacent rows: `a.songId = :from AND b.songId = :to AND b.trackOrder = a.trackOrder + 1 [AND a.transitionOut = 'SEGUE']`. Replace the word-matching segue filter in `DeadSetViewModel.applyFilters`. Add a test showing that "Playing in the Band ➔ Uncle John's Band" no longer matches unrelated shows.
- [ ] **1.8 Upcoming is computed, not stored.** Replace reads of `ShowEntity.isUpcoming` with `date > todayIsoDate()`. Drop the column in the 1.6 migration.
- [ ] **1.9 Move stats off the main thread.** Run `computeOverallStats` on `Dispatchers.Default` (or as SQL aggregates once 1.6 lands).

## Phase 2: Full catalog

- [ ] **2.1 Choose dataset.** It needs about 2,300 GD shows with segue markers and a license that allows bundling. Candidates: setlist.fm API export (check its terms), published community datasets. Record the choice and license here.
- [ ] **2.2 Importer.** Write a Gradle task or JVM script that converts the dataset into the Phase 1 schema, mapping `->`/`>` to `TransitionType` and normalizing names through the `songs` table.
- [ ] **2.3 Prebuilt asset DB.** Ship it with `createFromAsset`. Keep user columns (attended, review, rating) in a separate table or merge them on upgrade so they're never overwritten.
- [ ] **2.4 Perf check.** Make sure Shows, Stats, and Segues stay smooth with the full catalog.

## Phase 3: Transitions

- [ ] **3.1 Segue Families.** Replace `CuratedShows.legendaryTransitions` with a data-driven list (see the table in IMPROVEMENT_PLAN §2.2b). Compute era span and frequency from the catalog; hand-write only the notes. Fact-check the notes.
- [ ] **3.2 Song "Ins & Outs" panel.** Show the top incoming and outgoing transitions with percentages, filterable by era.
- [ ] **3.3 Set-structure flow view** on Show Detail (Set I | pre-Drums | Drums/Space | post-Space | Encore).
- [ ] **3.4 Quiz modes.** "What comes next?" (distractors = real exits from song A), "Complete the suite," "Fill the sandwich," "Name that era," "Build Set Two."
- [ ] **3.5 Spaced repetition.** A Leitner box per transition pair, stored in Room.
- [ ] **3.6 Transition graph** (chord or force-directed); tapping an edge filters shows.

## Phase 4: Tribute band tracking

- [ ] **4.1 `bands` table.** Replace the `TributeBandFilter` enum. Seed Terrapin Flyer (starred default), DSO, JRAD, Melvin Seals & JGB, Bob Weir & Wolf Bros, Dead & Company; let the user add bands.
- [ ] **4.2 `events` table + cache.** Store each event's source and last-checked time and show them on the card. Refresh with WorkManager about every 12 hours (new dependency).
- [ ] **4.3 Credentials.** Register a Bandsintown app ID or drop that source; add a Ticketmaster key. Add setlist.fm for past tribute setlists.
- [ ] **4.4 Manual add / paste-link import** for local shows (Terrapin Flyer).
- [ ] **4.5 Near me.** Sort by distance from the profile home city, with a radius slider, replacing the hard-coded Midwest state set.
- [ ] **4.6 Notifications.** New-date alerts for followed bands; day-of reminder.
- [ ] **4.7 Song chase list** ("heard 112/180 live") and cross-band stats.
- [ ] **4.8 DSO recreation link.** Match a tribute setlist to the original GD date and show them side by side.
- [ ] **4.9 Live setlist capture + next-song predictor** at shows (uses the Phase 3 transition probabilities).

## Phase 5: Almanac (trivia + articles)

- [ ] **5.1 Per-question answer history.** Stop repeating questions the user has mastered; show accuracy by category.
- [ ] **5.2 Fact-check pass** on `DeadTriviaSeed` and `historicTrivia`. Add a `sourceUrl` to `TriviaQuestion`.
- [ ] **5.3 New question types.** True/false, order-the-dates, who-sang-it, venue→city, lyric→song (paraphrase only, never quoted lyrics).
- [ ] **5.4 Article format.** Markdown with front matter in `assets/almanac/`, rendered by a small Compose Markdown renderer.
- [ ] **5.5 Article sets.** Song Stories (data-driven + notes), Show Spotlights, Band & Era Guides, Tribute Band Profiles (Terrapin Flyer first).
- [ ] **5.6 Cross-linking.** Setlist song → Song Story; quiz explanation → "Read more"; Today in Dead History → Spotlight.
- [ ] **5.7 Firebase AI decision.** Either use it for drafts that a person fact-checks, or remove `firebase-ai` / App Check dependencies.

## Phase 6: Polish & architecture

- [ ] **6.1 Navigation-Compose + 5 tabs:** Shows · Live · Segues · Almanac · Me.
- [ ] **6.2 Split `DeadSetViewModel`** into per-feature ViewModels; add a DI container.
- [ ] **6.3 Break up large screens** (`TriviaScreen`, `UserProfileScreen`, `StatsScreen`).
- [ ] **6.4 Backup/export** of user data (JSON via the system file picker).
- [ ] **6.5 Share cards, home-screen widget, accessibility pass.**
- [ ] **6.6 Rename namespace and applicationId**; add `.idea/` to `.gitignore`.
- [ ] **6.7 Test coverage:** ViewModel and filter tests, MockWebServer for the APIs, Compose UI tests for the main flows.

---

## Handoff log

_Newest first. Keep entries short: what changed, what's half-done, next action._

### 2026-11-21 · Claude Code
- Wrote `IMPROVEMENT_PLAN.md` and this tracker. Completed all of Phase 0 (`4762086`, fast-forwarded into `main`). 12/12 unit tests pass. Not yet run on a device.
- Found while testing: the seeding FK edge case (now step 1.3). Retired seed rows had filler `userReview` text, so the cleanup keys on `isAttended`/`attendedWith` instead.
- **Next:** step 1.1, schema baseline.
