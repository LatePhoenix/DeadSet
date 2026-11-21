# DeadSet — Audit & Improvement Plan

_Audited 2026-11-21 against `main` @ `a0f13b5`. Android / Kotlin / Jetpack Compose / Room, ~13.9k lines across 31 Kotlin files._

---

## 1. Audit findings

### 1.1 What's there today

| Area | Files | State |
|---|---|---|
| Show catalog | `data/seed/CuratedShows.kt`, `data/local/*` | 15 hand-seeded Grateful Dead shows + 3 Terrapin Flyer shows in Room |
| Segues | `TransitionsScreen.kt`, `Models.kt` (`SongEntry.isSegue`, `LegendarySegue`) | 6 curated "legendary" segues + top-10 counted from the catalog |
| Trivia | `DeadTriviaSeed.kt` (688 lines), `TriviaScreen.kt` (1,619 lines) | ~57 curated questions + generated "vault" questions, 4 game modes, badges |
| Live concerts | `LiveConcertApiService.kt`, `LiveConcertsScreen.kt` | Bandsintown + Ticketmaster queries merged with a **hard-coded** schedule for DSO / JRAD / Terrapin Flyer |
| Spotify | `SpotifyApiService.kt`, `SpotifySetlistPlayer.kt`, `ShowDetailScreen.kt` | Client-credentials search + an offline fallback |
| Attendance, reviews, stats, profile | `AttendanceScreen`, `StatsScreen`, `UserProfileScreen` | Working, stats computed in memory |

### 1.2 Problems, by priority

**Critical — risks losing user data or shows false information**

1. **Room wipes data on upgrade.** `AppDatabase` uses `fallbackToDestructiveMigration()` with `exportSchema = false`. The next schema bump deletes every attended show, rating, and review the user entered.
2. **Trivia progress and profile don't persist.** `TriviaUserProgress` and `UserProfileData` live only in `UiState`, so they reset whenever the app process is killed. (DataStore is already in the version catalog but commented out.)
3. **Made-up tour dates are shown as "verified."** `getCuratedUpcomingTourSchedule()` hard-codes 17 DSO / JRAD / Terrapin Flyer dates with prices and venues, and the code comments call them "verified." They're placeholder data, and some (e.g. TF at Martyrs' on 11/14) are already in the past. They're merged into real API results with nothing to tell them apart.
4. **Fake Spotify tracks.** `SpotifyApiClient.synthesizeDeterministicTrackId()` makes up `spotify:track:` IDs when there are no credentials, and labels the result "Spotify URI Archive • Ready." Also, the client secret is compiled into the APK through `BuildConfig`, so anyone can pull it out.

**High — correctness**

5. **The segue filter matches loosely.** `applyFilters` splits "Scarlet Begonias ➔ Fire on the Mountain" into words and checks that each word appears *somewhere* in the setlist JSON. It ignores order, adjacency, and the `isSegue` flag, so common words like "the" and "band" match almost every show.
6. **A segue is only a yes/no flag.** The data can't tell a true `->` segue from a `>` transition with a pause, a jam, a tease, or a "sandwich" return. The transition-training features below need that detail.
7. **Song names aren't normalized.** "Saint Stephen" vs "St. Stephen", "Lovelight" vs "Turn On Your Love Light", "Slipknot!" vs "Slipknot". Counts and filters split across spellings.
8. **Setlists are stored twice.** Each show has both `ShowEntity.setlistJson` and the `song_performances` table, and they can drift apart. Search and stats read the JSON; the DAO song query reads the table. `toSongEntry()` also drops `position`.
9. **"Upcoming" is a saved flag.** `isUpcoming = true` is stored on seeded shows and never re-checked against today's date.
10. **Daily trivia uses `DAY_OF_YEAR` without the year.** `DeadSetViewModel` stores the day of the year, so Nov 21 2026 and Nov 21 2027 look like the same day. (`DeadTriviaSeed` already includes the year; the ViewModel doesn't.)
11. **`clearSegueFilter()` also switches tabs.** It calls `filterByLegendarySegue("")`, which jumps to the Shows tab as a side effect.

**Medium — architecture and UX**

12. **Seven bottom-nav tabs.** Material suggests 3–5, and the labels get cramped on phones.
13. **No navigation library.** Screens are switched by hand through a `ScreenState` sealed class, so there's no deep linking, no saved back stack, and no restore after process death.
14. **Stats are computed on the main thread.** `computeOverallStats` parses every setlist's JSON each time Room emits. That's fine for 18 shows but will stutter with a full catalog of about 2,300 shows.
15. **Very large screen files.** `TriviaScreen` (1.6k lines), `UserProfileScreen` (1.2k), and `StatsScreen` (1.1k) should be split into components and their own ViewModels.
16. **Leftover scaffold code.** The Firebase AI and App Check dependencies are included but nothing calls them. The `com.example` namespace and the `com.aistudio.deadset.kxwmtp` app ID are scaffold defaults. `.idea/` is untracked but not in `.gitignore`.
17. **Thin tests.** The tests cover seed data, and the Spotify test calls the live network, so it's flaky in CI. There are no ViewModel or filter tests.
18. **The catalog is tiny.** With 15 GD shows, stats like "most played transitions" don't mean much. A real setlist dataset would make every feature below better.

---

## 2. Requested features

### 2.1 Remove Spotify integration _(do first — small and self-contained)_

**Delete**
- `data/remote/SpotifyApiService.kt`
- `ui/components/SpotifySetlistPlayer.kt`
- The test `spotify api client resolves ... spotify uris` in `ExampleRobolectricTest.kt`

**Edit**
- `ShowDetailScreen.kt`: remove the imports (lines 32, 39–41), the player state (71–74), the "Play full setlist" pill (≈445–490), the `SpotifySetlistPlayerCard` (≈496–515), and the per-song `SetlistSpotifyPlayButton` (≈626–640). Change the hint text to "Tap ➔ to tag transitions."
- `AndroidManifest.xml`: remove the `com.spotify.music` package query and the `spotify:` scheme intent.
- `.env.example`: remove `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET`.

**Replace with something that actually plays:** a **"Listen on Archive.org"** button. `archiveIdentifier` is already in the schema, so the button opens `https://archive.org/details/<identifier>`. Optionally add a Relisten deep link too. There's nothing to authenticate and no secret to ship. Note that some seeded identifiers (e.g. `gd69-02-27.sbd.16trk.flac`) aren't real archive.org item IDs, so check each one before showing the button, and hide it when the ID is blank.

### 2.2 Live song transitions: data, info, and training

#### a) Fix the data model first

```kotlin
enum class TransitionType(val symbol: String) {
    NONE(""),          // stop, applause
    SEGUE("->"),       // seamless, no break
    TRANSITION(">"),   // brief pause / tuning, but connected
    JAM_INTO("~>"),    // extended jam bridges the two
    TEASE("(t)")       // other song teased inside this one
}
// SongEntry: isSegue -> transitionOut: TransitionType, + teases: List<String>
```

- Add a `songs` table (`canonicalName`, `aliases`, `lyricist`, `debutDate`, `writer`) and point `song_performances.songId` at it. Normalize every name through it.
- Make `song_performances` the only place setlists are stored. Drop `setlistJson` in a **real** Room migration (and make that migration the moment to remove `fallbackToDestructiveMigration`).
- Add a `transitions` view or query: `(fromSongId, toSongId, type, showId, era)`. Every stat, filter, and quiz below reads from it.

#### b) Common groupings to seed as "Segue Families"

Curated, annotated entries that replace the current six. Each shows the era it was active, a "classic version" show, and how often the pairing happens in the catalog.

| Family | Typical flow | Notes for the card |
|---|---|---|
| **China Rider** | China Cat Sunflower -> I Know You Rider | Paired from 1969 on; the transition jam is the highlight |
| **Scarlet Fire** | Scarlet Begonias -> Fire on the Mountain | Paired from spring 1977; Cornell 5/8/77 is the reference |
| **Help > Slip > Franklin's** | Help on the Way -> Slipknot! -> Franklin's Tower | Debuted together in 1975 (Blues for Allah era) |
| **Estimated > Eyes** | Estimated Prophet -> Eyes of the World | Common late '70s–'80s second-set pairing |
| **Weather Report Suite** | Prelude -> Part 1 -> Let It Grow | 1973–74 suite; later Let It Grow alone |
| **Lost Sailor > Saint** | Lost Sailor -> Saint of Circumstance | 1979–81 pair; Saint continued alone after |
| **Lazy Lightning > Supplication** | Lazy Lightning -> Supplication | Late '70s Weir pairing |
| **Playin' Sandwich** | Playing in the Band -> … -> Playing in the Band | Teaches the "sandwich" idea; UJB in the middle is the classic |
| **Other One Sandwich** | (Cryptical ->) The Other One -> … -> The Other One | Primal Dead suite and later sandwiches |
| **NFA > GDTRFB > NFA** | Not Fade Away -> Goin' Down the Road -> Not Fade Away | Early '70s; Bobby/Jerry chant ending |
| **Primal Suite** | Dark Star -> St. Stephen -> The Eleven -> Lovelight | Live/Dead sequence, 1968–69 |
| **Sugar Mag … Sunshine Daydream** | Sugar Magnolia … (split) … Sunshine Daydream | Teaches the *split song* idea |
| **Drums -> Space** | Drums -> Space | Nearly every second set from 1978 on; the main "anchor" |
| **Post-Space exits** | Space -> The Wheel / The Other One / I Need a Miracle / Truckin' / Dear Mr. Fantasy | A "where does Space go?" probability chart |
| **Throwing Stones > NFA** | Throwing Stones -> Not Fade Away | Standard late-'80s set-two closer |
| **Truckin' exits** | Truckin' -> Nobody's Fault Jam / The Other One / Smokestack | Jam vehicle |
| **He's Gone -> …** | He's Gone -> Truckin' / The Other One / Drums | Jam vehicle |

> These are drafts from general knowledge and still need fact-checking. The plan is to **compute** the era and frequency numbers from the catalog dataset (§3.1) and keep only the curated notes hand-written.

#### c) Transition info features

- **Song page "Ins & Outs" panel.** Shows the top songs that come *into* and go *out of* this song, with percentages, for example *Space → The Wheel 18% · The Other One 14% · …*. You can filter it by era.
- **Transition graph.** A force-directed or chord diagram of songs, with edge thickness for frequency. Tap an edge to see every show with that transition.
- **Exact segue search.** Replace the word-matching filter with a query on the `transitions` table: `from = A AND to = B [AND type = SEGUE]`.
- **Set-structure view.** Draw any show as a flow (Set I | Set II pre-Drums | Drums/Space | post-Space | Encore), with segue arrows styled by `TransitionType`.
- **Rarity flags.** Mark transitions that happened 3 times or fewer as a "Bust-out segue," and debut or last-played transitions.

#### d) Training modes (fit into the existing Trivia engine)

1. **"What comes next?"** Show song A and pick B out of four. The wrong options are other real exits from A, ranked by frequency, so the question is hard but fair.
2. **"Complete the suite."** Fill in the missing middle (Help -> ??? -> Franklin's).
3. **"Fill the sandwich."** Playing -> ??? -> Playing.
4. **"Name that era."** Show a 4-song segue chain and guess the era. It teaches how the band's habits changed over time.
5. **"Build Set Two."** Drag shuffled cards into the real order from a real show, scored by how far each card is from its true position.
6. **Spaced repetition.** Use a Leitner box per transition pair so weak pairings come back more often. Store the boxes in Room so progress survives restarts.
7. **Live "Next Song Predictor."** At a tribute show (or a couch tour), tap the song that's playing and the app shows the most likely next songs from the transition probabilities. Log the actual song and it builds your setlist for that show. Afterwards you get a "Prediction score," which also makes a good share card.

### 2.3 Trivia: quizzes plus articles

**Quiz improvements**
- Persist `TriviaUserProgress` in Room or DataStore (finding #2).
- Track answers per question so the app can stop repeating questions you've mastered and show accuracy by category.
- Fix the daily-key bug (#10). Use `LocalDate.now().toString()` as the key.
- Add question types: **true/false**, **order the dates** (put four shows in chronological order), **who sang it** (Jerry / Bobby / Phil / Pigpen / Brent), **lyric → song** using *short paraphrases only*, never copied lyrics, and **venue map** (which city?).
- Add a review pass on the seeded questions and liner notes. They were generated, so each claim needs a source before release. Add a `sourceUrl` field to `TriviaQuestion`.

**New "Almanac" section: articles**

| Article type | Content | Data that drives it |
|---|---|---|
| **Song Stories** | Writers (Hunter/Garcia, Barlow/Weir…), debut, last played, times played, notable versions, common ins/outs (from §2.2c) | `songs` table + catalog stats + hand-written notes |
| **Show Spotlights** | Why the show matters, set structure, standout jams, tape lineage, links to the Archive.org recording | `ShowEntity.historicTrivia` grows into a full write-up |
| **Band & Era Guides** | Member bios, the keyboard lineage (Pigpen → TC → Keith → Brent → Vince → Bruce), Wall of Sound, Owsley & the tapers, hiatus 1974–76 | Hand-written, stored as Markdown in `assets/almanac/` |
| **Tribute Band Profiles** | DSO's "recreated show" concept, JRAD's style, Terrapin Flyer's Chicago roots and lineup | Hand-written, plus links to upcoming shows |

- Store articles as bundled Markdown with front matter (`id`, `type`, `relatedSongs`, `relatedShows`, `sources`), and render them with a small Compose Markdown renderer.
- **Link everything together.** Song names in a setlist open the Song Story. A quiz explanation has "Read more →" that opens the article. An article lists the related shows in your catalog.
- **"Today in Dead History"** (`TodayInDeadHistoryCard` already exists) links to the Show Spotlight for that date.
- **About AI-generated content.** The Firebase AI dependency could draft articles, but any draft needs human fact-checking before it ships, and the app shouldn't generate facts on the fly. Otherwise, remove the dependency.

### 2.4 Tribute band concert tracking (Terrapin Flyer first)

**Replace the fake schedule**
- Delete `getCuratedUpcomingTourSchedule()` and the "verified" wording. Show only events that came back from a real source, and show each event's **source and last-checked time** on it.
- Sources:
  - **Bandsintown.** The app ID `deadset_companion_app` is a placeholder. Bandsintown requires a registered app ID, so register one or remove this source.
  - **Ticketmaster Discovery** (free key) for venue and ticket listings.
  - **setlist.fm API** (free for non-commercial use) for **past** tribute setlists. DSO setlists there often say which original GD show they recreated.
  - A **manual "Add a show"** option is always there. For a local band like Terrapin Flyer, the band's own calendar or socials may be the most complete source. Add a "paste event link" importer instead of scraping.
- Cache results in a Room `events` table so the screen still works offline. Refresh with WorkManager about every 12 hours.

**Features**
- **Followed bands.** Terrapin Flyer is the starred default. Also include Dark Star Orchestra, Joe Russo's Almost Dead, Melvin Seals & JGB, Bob Weir & Wolf Bros, Dead & Company, and any band you add. Turn `TributeBandFilter` from a hard-coded enum into a table you can edit.
- **Near me.** Sort by distance from the profile's home city (Chicago is the default), with a radius slider. That replaces the hard-coded "Midwest" state list.
- **Notifications.** "Terrapin Flyer just announced a date at <venue>" and a reminder the day of the show.
- **Add to calendar** (Calendar intent or `.ics`) and **directions** (`ConcertIntentHelper` already exists).
- **Attended tribute shows** use the same `ShowEntity` with `bandName`. Add a live setlist capture (§2.2d #7), a photo or ticket stub, and who you went with.
- **"Recreation" link.** When a DSO or other band's setlist matches a GD date, link the two and show a side-by-side comparison.
- **Song chase list.** Mark songs you want to hear live. The app tracks which band played each one for you first, and shows progress like "You've heard 112/180 songs live."
- **Cross-band stats.** Shows by band, venues visited, most-heard song, and longest gap between shows.

---

## 3. Other ideas

### 3.1 Data
- **Import a full Grateful Dead setlist dataset** (about 2,300 shows) as a bundled, pre-built Room database (`createFromAsset`). Before choosing one, check the license and that it marks segues. Candidates: setlist.fm exports, published community datasets. This one change gives every stat, quiz, and transition chart real numbers to work with.
- **Backup.** Export and import your attended shows, reviews, and trivia progress as JSON through Android's file picker.

### 3.2 Architecture
- Add **Navigation-Compose** with typed routes, and move to 5 bottom tabs: **Shows · Live · Segues · Almanac (Trivia + Articles) · Me (Attendance + Stats + Profile)**.
- Split `DeadSetViewModel` into one ViewModel per feature. Run stats on `Dispatchers.Default`, or better, compute them with SQL aggregate queries.
- **DI** with Hilt or a simple manual container, so tests can swap in fakes for the database and network.
- Rename the namespace from `com.example` to something like `app.deadset`, and set a real `applicationId` before any Play release.
- Add `.idea/` to `.gitignore`, and remove the unused Firebase dependencies (or put them to use).

### 3.3 UX polish
- **Share cards**: generated images of a setlist, a "My year in Dead" summary, and trivia scores.
- **Setlist entry speed-up**: autocomplete from the `songs` table, a one-tap segue toggle, and paste-a-setlist parsing for text like `Scarlet > Fire`.
- **Home-screen widget** for Today in Dead History or the next Terrapin Flyer show.
- **Accessibility**: content descriptions on the emoji and art, and color contrast checks (the theme has many custom colors).

### 3.4 Testing
- Unit tests for the segue filter, transition probabilities, Room migrations (`MigrationTestHelper`), and trivia scoring.
- Replace network-calling tests with MockWebServer.
- Compose UI tests for the main flows: mark a show attended, tag a segue, finish a quiz.

---

## 4. Suggested order

| Phase | Scope | Why this order |
|---|---|---|
| **0. Stop the bleeding** | Remove Spotify (§2.1) · delete fake tour schedule (#3) · persist trivia/profile (#2) · fix daily-key, clearSegueFilter bugs | Small, low-risk, and they remove the user-visible false information |
| **1. Data foundation** | Real Room migrations + schema export (#1) · `songs` table + name normalization · `TransitionType` · drop `setlistJson` · exact segue query | Everything else depends on this; do it before users have data worth losing |
| **2. Catalog** | Import full GD dataset as asset DB | Gives transitions/trivia/stats real numbers |
| **3. Transitions** | Segue Families · Ins & Outs · transition quiz modes · spaced repetition | The core feature you asked for |
| **4. Tribute tracking** | Events table + caching · followed bands (Terrapin Flyer default) · notifications · calendar · chase list | Needs real API keys; build on the Phase 1 schema |
| **5. Almanac** | Markdown articles · Song Stories generated from data · cross-linking | Content-heavy; fill it in over time |
| **6. Polish** | Nav-Compose + 5 tabs · ViewModel split · share cards · widget · live predictor | Larger refactors once features are stable |

Phase 0 is roughly a day of work and can be done right away.

## 5. External review (2026-11-21): what was kept and why

The owner had another AI review the project. Each suggestion was checked against the code and its sources before going into the plan. Steps live in [PROGRESS.md](PROGRESS.md) under Phase 1b, Phase 2 and Phase 4.

| Item | Verdict | Reasoning |
|---|---|---|
| **A1** trim unused deps | Keep | Firebase AI/App Check were already dropped (5.7). `logging-interceptor` is unused, and with AI Studio out of the loop nothing needs the Firebase BoM or google-services plugin. Smaller build, fewer CVE alerts. |
| **A2** delete `.aistudio/`, `public/`, add README | Keep | ~2 MB of tracked AI Studio scratch files and web scaffolding that the Android app never uses; there was no README. |
| **A3** GitHub Actions CI | Keep | CodeRabbit reviews but nothing runs the tests on a PR. CI must create `debug.keystore` with `keytool` instead of committing one or touching `signingConfigs`. |
| **A4** stop shipping fake attended shows | Already done | The seed reset blanked all 16 seeds and cleans old installs only where data still matches the invented values. |
| **A5** rename package / app ID | Keep, needs owner | Same as 6.6. Changing `applicationId` orphans existing installs, so the owner picks the IDs. The Room schema folder is named after the package and must move with it. |
| **B1** single source of truth + exact segue query | Already done | Phase 1 (1.6/1.7) made `song_performances` the only setlist store and replaced the word-overlap filter. The DAO-level query is still useful for importer tests, so it moved into C6. |
| **C1–C7** gdshowsdb importer | Keep, adjusted | gdshowsdb is MIT-licensed (verified), covers 1965–1995 with a per-song `:segued:` flag. This unblocks Phase 2, which had no licensed dataset. Adjustments: share name normalization through a `:shared` module rather than duplicating it; hand-parse the YAML (no new library); build the asset DB from Room's exported schema so `createFromAsset` validates; split user data out of `shows` first so a catalog refresh can't wipe it. `:segued:` can't tell `>` from `->`, so everything imports as `SEGUE`. |
| **C4** Relisten / Archive.org cross-check | Keep, optional | Free, keyless; mark disagreeing shows uncertain instead of choosing a winner (matches the no-invented-facts rule). Archive.org identifiers also give a better "Listen" link. |
| Scraping gratefulstats / jerrybase / gdsets | Reject | GratefulStats' terms forbid it; the other two have no license. The owner may ask Jerrybase for permission separately. |
| **D1** key proxy | Keep | Keys in `BuildConfig` ship in the APK. A tiny Worker holding the Ticketmaster key behind one endpoint fixes that; it needs the owner's Cloudflare account to deploy. |
| **D2** R8 + drop Retrofit/Moshi | Keep | Two endpoints don't justify Retrofit + Moshi + kotlin-reflect; `ConcertRepository` already works with a plain fetch function. R8 needs Room keep rules and a CI shrink check. |
| **D3** setlist.fm for tribute setlists | Note only | Fits 4.3 later; terms allow on-demand fetching with attribution, not bundling. |
| **D4** offline LLM drafting | Note only | Consistent with 5.2: drafts are fine offline if every fact is checked against a source; the app never calls an LLM. |
