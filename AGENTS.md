# Agent instructions — DeadSet

DeadSet is a Grateful Dead setlist, segue, trivia, and tribute-show companion for Android (Kotlin, Jetpack Compose, Room).

## Before you do anything
1. Read **[docs/PROGRESS.md](docs/PROGRESS.md)**: current focus, environment notes, next unchecked step, latest handoff.
2. Background and reasoning for each step are in [docs/IMPROVEMENT_PLAN.md](docs/IMPROVEMENT_PLAN.md).

## While working
- Take one step at a time from PROGRESS.md and mark it `[~]` with your tool name.
- Work on a `phase-N-<slug>` branch; fast-forward into `main` when the phase or step is done.
- Run `./gradlew --offline :app:testDebugUnitTest` before committing. Set JAVA_HOME first; see Environment notes in PROGRESS.md.
- Never invent user-visible facts (dates, venues, setlists, prices) in seed data.
- Don't use `java.time` (minSdk 24, no desugaring). Don't modify the `signingConfigs` blocks.

## Before you stop
- Tick finished steps with the commit hash.
- Add a dated **Handoff log** entry at the top of the log in PROGRESS.md: what changed, what's half-done, and the next action.
