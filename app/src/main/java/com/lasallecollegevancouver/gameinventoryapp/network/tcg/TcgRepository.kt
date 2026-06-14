package com.lasallecollegevancouver.gameinventoryapp.network.tcg

import com.lasallecollegevancouver.gameinventoryapp.BuildConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TcgRepository(
    private val scryfallService: ScryfallApiService,
    private val pokemonService: PokemonTcgApiService,
    private val ygoService: YgoProApiService
) {

    // Search only Magic: The Gathering cards via Scryfall.
    // Three strategies in order of precision: exact full name → quoted substring → loose word prefix.
    // Scryfall handles hyphens and spaces equivalently, so no variant generation needed here.
    suspend fun searchMtg(query: String): List<TcgSearchResult> {
        val cleanQuery = query.trim()
        val exactResults        = safeMtgSearch("!\"${escapeScryfallQuery(cleanQuery)}\"")
        val quotedNameResults   = safeMtgSearch("name:\"${escapeScryfallQuery(cleanQuery)}\"")
        val broadNameResults    = safeMtgSearch("name:${escapeScryfallQuery(cleanQuery)}")

        return (exactResults + quotedNameResults + broadNameResults)
            .distinctBy { it.externalId }
    }

    private suspend fun safeMtgSearch(query: String): List<TcgSearchResult> {
        return try {
            val response = scryfallService.searchCards(query)
            if (!response.isSuccessful) emptyList()
            else response.body()?.data?.map { mapScryfallCard(it) } ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun escapeScryfallQuery(query: String): String {
        return query.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    // Search only Pokémon TCG cards
    suspend fun searchPokemon(query: String): List<TcgSearchResult> {
        // Pass key as null when empty so Retrofit omits the header — API works unauthenticated
        val apiKey = BuildConfig.POKEMON_TCG_API_KEY.ifEmpty { null }
        val response = pokemonService.searchCards(
            query = "name:$query*",
            apiKey = apiKey
        )
        if (!response.isSuccessful) return emptyList()
        return response.body()?.data?.flatMap { mapPokemonCard(it) } ?: emptyList()
    }

    // Search only Yu-Gi-Oh! cards via YGOProDeck.
    // YGOProDeck's fname= does substring matching on the raw card name, so "blue eyes"
    // won't find "Blue-Eyes White Dragon" — we generate both spaced and hyphenated variants
    // of every query so either form the user types will work.
    suspend fun searchYugioh(query: String): List<TcgSearchResult> {
        val normalized = normalizeYugiohQuery(query.trim())
        val variants = buildYugiohVariants(query.trim(), normalized)

        // Exact-name lookup on the normalized form only (handles full card name searches)
        val exactResults = safeYgoExactSearch(normalized)

        // Fuzzy search on every variant — merges results so "blue eyes" and "blue-eyes" both work
        val fuzzyResults = variants.flatMap { variant -> safeYgoFuzzySearch(variant) }

        return (exactResults + fuzzyResults).distinctBy { it.externalId }
    }

    // Generates up to 4 variants of a query: original, normalized, and space↔hyphen swaps of each.
    // This ensures "blue eyes", "blue-eyes", "Blue-Eyes", etc. all find the same cards.
    private fun buildYugiohVariants(original: String, normalized: String): Set<String> {
        val variants = mutableSetOf(original, normalized)
        for (base in listOf(original, normalized)) {
            if (base.contains(' '))  variants.add(base.replace(Regex("\\s+"), "-"))
            if (base.contains('-'))  variants.add(base.replace("-", " "))
        }
        return variants
    }

    private suspend fun safeYgoExactSearch(query: String): List<TcgSearchResult> {
        return try {
            val response = ygoService.getCardByExactName(name = query)
            if (!response.isSuccessful) emptyList()
            else response.body()?.data?.flatMap { mapYgoCard(it) } ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private suspend fun safeYgoFuzzySearch(query: String): List<TcgSearchResult> {
        return try {
            val response = ygoService.searchCards(name = query)
            if (!response.isSuccessful) emptyList()
            else response.body()?.data?.flatMap { mapYgoCard(it) } ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun normalizeYugiohQuery(query: String): String {
        return query
            .replace(Regex("\\bblue[\\s-]+eyes\\b", RegexOption.IGNORE_CASE), "Blue-Eyes")
            .replace(Regex("\\bred[\\s-]+eyes\\b", RegexOption.IGNORE_CASE), "Red-Eyes")
            .replace(Regex("\\bevil[\\s-]+hero\\b", RegexOption.IGNORE_CASE), "Evil HERO")
            .replace(Regex("\\belemental[\\s-]+hero\\b", RegexOption.IGNORE_CASE), "Elemental HERO")
            .replace(Regex("\\bdestiny[\\s-]+hero\\b", RegexOption.IGNORE_CASE), "Destiny HERO")
            .replace(Regex("\\bvision[\\s-]+hero\\b", RegexOption.IGNORE_CASE), "Vision HERO")
            .replace(Regex("\\bx[\\s-]+saber\\b", RegexOption.IGNORE_CASE), "X-Saber")
            .replace(Regex("\\bblack[\\s-]+wing", RegexOption.IGNORE_CASE), "Blackwing")
            .replace(Regex("\\bneo[\\s-]+spacian\\b", RegexOption.IGNORE_CASE), "Neo-Spacian")
    }

    // Search all three APIs in parallel and merge results
    suspend fun searchAll(query: String): List<TcgSearchResult> = coroutineScope {
        val mtgDeferred = async {
            try { searchMtg(query) } catch (exception: Exception) { emptyList() }
        }
        val pokemonDeferred = async {
            try { searchPokemon(query) } catch (exception: Exception) { emptyList() }
        }
        val ygoDeferred = async {
            try { searchYugioh(query) } catch (exception: Exception) { emptyList() }
        }
        (mtgDeferred.await() + pokemonDeferred.await() + ygoDeferred.await())
            .sortedBy { it.name }
    }

    // --- Mappers ---

    // One Scryfall card → one TcgSearchResult.
    // Double-faced cards (DFCs) have no top-level image_uris — fall back to card_faces[0].
    private fun mapScryfallCard(card: ScryfallCard): TcgSearchResult {
        val imageUrl = card.image_uris?.normal
            ?: card.card_faces?.firstOrNull()?.image_uris?.normal
            ?: ""
        return TcgSearchResult(
            externalId = card.id,
            tcgGame = "MTG",
            name = card.name,
            setName = card.set_name,
            setCode = card.set.uppercase(),
            cardNumber = card.collector_number,
            rarity = card.rarity.replaceFirstChar { it.uppercase() },
            imageUrl = imageUrl,
            priceRegular = card.prices.usd?.toDoubleOrNull(),
            priceFoil = card.prices.usd_foil?.toDoubleOrNull(),
            isFoilVariant = false
        )
    }

    // One Pokémon card → up to two TcgSearchResults (normal + holofoil when both exist).
    // This lets the user see and add either variant separately.
    private fun mapPokemonCard(card: PokemonTcgCard): List<TcgSearchResult> {
        val results = mutableListOf<TcgSearchResult>()
        val prices = card.tcgplayer?.prices

        val normalPrice = prices?.get("normal")?.market ?: prices?.get("normal")?.mid
        val holofoilPrice = prices?.get("holofoil")?.market ?: prices?.get("holofoil")?.mid
        val reverseHoloPrice = prices?.get("reverseHolofoil")?.market

        // Always emit a normal/base entry
        results.add(
            TcgSearchResult(
                externalId = card.id,
                tcgGame = "POKEMON",
                name = card.name,
                setName = card.set.name,
                setCode = card.set.id.uppercase(),
                cardNumber = "${card.number}/${card.set.printedTotal}",
                rarity = card.rarity ?: "Unknown",
                imageUrl = card.images.large,
                priceRegular = normalPrice ?: holofoilPrice,
                priceFoil = holofoilPrice,
                isFoilVariant = false
            )
        )

        // Emit a separate holofoil entry when a distinct foil price exists
        if (holofoilPrice != null && normalPrice != null) {
            results.add(
                TcgSearchResult(
                    externalId = "${card.id}_holo",
                    tcgGame = "POKEMON",
                    name = card.name,
                    setName = card.set.name,
                    setCode = card.set.id.uppercase(),
                    cardNumber = "${card.number}/${card.set.printedTotal}",
                    rarity = "${card.rarity ?: "Unknown"} Holo",
                    imageUrl = card.images.large,
                    priceRegular = holofoilPrice,
                    priceFoil = reverseHoloPrice,
                    isFoilVariant = true
                )
            )
        }
        return results
    }

    // One YGOPro card → one TcgSearchResult per set printing.
    // A single card (e.g. "Dark Magician") may have dozens of set entries.
    private fun mapYgoCard(card: YgoProCard): List<TcgSearchResult> {
        val imageUrl = card.card_images?.firstOrNull()?.image_url ?: ""
        val basePrice = card.card_prices?.firstOrNull()?.tcgplayer_price?.toDoubleOrNull()

        val sets = card.card_sets
        if (sets.isNullOrEmpty()) {
            // Card exists but has no set data — emit a single generic entry
            return listOf(
                TcgSearchResult(
                    externalId = card.id.toString(),
                    tcgGame = "YUGIOH",
                    name = card.name,
                    setName = "Unknown Set",
                    setCode = "",
                    cardNumber = "",
                    rarity = "",
                    imageUrl = imageUrl,
                    priceRegular = basePrice,
                    priceFoil = null,
                    isFoilVariant = false
                )
            )
        }

        return sets.map { setEntry ->
            val setPrice = setEntry.set_price.toDoubleOrNull() ?: basePrice
            TcgSearchResult(
                externalId = "${card.id}_${setEntry.set_code}",
                tcgGame = "YUGIOH",
                name = card.name,
                setName = setEntry.set_name,
                setCode = setEntry.set_code,
                cardNumber = setEntry.set_code,
                rarity = setEntry.set_rarity,
                imageUrl = imageUrl,
                priceRegular = setPrice,
                priceFoil = null,
                isFoilVariant = false
            )
        }
    }
}
