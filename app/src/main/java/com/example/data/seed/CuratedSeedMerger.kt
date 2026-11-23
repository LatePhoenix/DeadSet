package com.example.data.seed

import com.example.data.local.ShowEntity

/**
 * Resolves curated [SeedShow]s against an existing catalog (R1).
 *
 * - Grateful Dead seeds merge onto a catalog row matched by date + venue (catalog id wins).
 * - Non-GD seeds (e.g. Terrapin Flyer) insert under their seed id when missing.
 * - Unmatched GD seeds are only inserted when [PlanOptions.insertUnmatchedGd] (restore path)
 *   or the database is empty — never on every launch next to a bundled catalog.
 */
object CuratedSeedMerger {

    data class Binding(
        val seed: SeedShow,
        /** Catalog / stored show id that should receive curated enrichment and setlist repair. */
        val targetShowId: String,
        /** True when the target row already existed before this merge. */
        val existed: Boolean,
        /** True when we should insert the seed as a new show. */
        val insertNew: Boolean
    )

    data class PlanOptions(
        /** Fresh / wiped DB: every seed inserts under its seed id. */
        val emptyDatabase: Boolean = false,
        /**
         * When true (restore), insert GD seeds that have no catalog match and are missing.
         * When false (startup repair), leave unmatched GD seeds out so catalog UUIDs stay unique.
         */
        val insertUnmatchedGd: Boolean = false
    )

    fun isGratefulDead(show: ShowEntity): Boolean =
        show.bandName.isBlank() || show.bandName.equals("Grateful Dead", ignoreCase = true)

    fun normalizeVenue(raw: String): String =
        raw.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun venuesMatch(a: String, b: String): Boolean {
        val na = normalizeVenue(a)
        val nb = normalizeVenue(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        if (na == nb) return true
        if (na.contains(nb) || nb.contains(na)) return true
        val ta = na.split(' ').filter { it.length > 2 }.toSet()
        val tb = nb.split(' ').filter { it.length > 2 }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return false
        val overlap = ta.intersect(tb)
        return overlap.size >= 2 || (overlap.isNotEmpty() && (ta.size == 1 || tb.size == 1))
    }

    /**
     * Finds the best existing show for a GD seed: same date, venue match, prefer GD band.
     * If venues disagree but the date has exactly one GD candidate, use that.
     */
    fun findCatalogMatch(seed: SeedShow, existing: List<ShowEntity>): ShowEntity? {
        val sameDate = existing.filter {
            it.date == seed.show.date && isGratefulDead(it)
        }
        sameDate.firstOrNull { venuesMatch(it.venue, seed.show.venue) }?.let { return it }
        return sameDate.singleOrNull()
    }

    /** Catalog enrichment from seed; keeps [base] id and never touches user fields. */
    fun mergeCatalogFields(base: ShowEntity, seed: ShowEntity): ShowEntity = base.copy(
        archiveIdentifier = seed.archiveIdentifier.ifBlank { base.archiveIdentifier },
        historicTrivia = seed.historicTrivia.ifBlank { base.historicTrivia },
        tapeQuality = seed.tapeQuality.ifBlank { base.tapeQuality },
        tourName = seed.tourName.ifBlank { base.tourName },
        eraId = seed.eraId.ifBlank { base.eraId },
        venueAddress = seed.venueAddress.ifBlank { base.venueAddress },
        tags = mergeTags(base.tags, seed.tags)
    )

    fun mergeTags(existing: String, curated: String): String {
        if (curated.isBlank()) return existing
        if (existing.isBlank()) return curated
        return (existing.split(',') + curated.split(','))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .joinToString(",")
    }

    fun plan(
        seeds: List<SeedShow>,
        existing: List<ShowEntity>,
        options: PlanOptions
    ): List<Binding> {
        if (options.emptyDatabase) {
            return seeds.map {
                Binding(seed = it, targetShowId = it.id, existed = false, insertNew = true)
            }
        }
        val byId = existing.associateBy { it.id }
        return seeds.map { seed ->
            if (!isGratefulDead(seed.show)) {
                val existed = byId.containsKey(seed.id)
                Binding(
                    seed = seed,
                    targetShowId = seed.id,
                    existed = existed,
                    insertNew = !existed
                )
            } else {
                val match = findCatalogMatch(seed, existing)
                when {
                    match != null -> Binding(
                        seed = seed,
                        targetShowId = match.id,
                        existed = true,
                        insertNew = false
                    )
                    byId.containsKey(seed.id) -> Binding(
                        seed = seed,
                        targetShowId = seed.id,
                        existed = true,
                        insertNew = false
                    )
                    options.insertUnmatchedGd -> Binding(
                        seed = seed,
                        targetShowId = seed.id,
                        existed = false,
                        insertNew = true
                    )
                    else -> Binding(
                        seed = seed,
                        targetShowId = seed.id,
                        existed = false,
                        insertNew = false
                    )
                }
            }
        }
    }

    /** Seed ids for GD curated rows that should be removed when a catalog match owns the date. */
    fun duplicateSeedIdsToDrop(bindings: List<Binding>, existing: List<ShowEntity>): List<String> {
        val existingIds = existing.map { it.id }.toSet()
        return bindings.mapNotNull { b ->
            if (!isGratefulDead(b.seed.show)) return@mapNotNull null
            if (b.targetShowId == b.seed.id) return@mapNotNull null
            if (b.seed.id in existingIds) b.seed.id else null
        }
    }
}
