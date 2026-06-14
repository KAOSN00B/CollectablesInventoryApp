package com.lasallecollegevancouver.gameinventoryapp.network.tcg


// ============================================================
// Raw API response models — internal to TcgRepository only.
// Never expose these to UI; use TcgSearchResult instead.
// ============================================================

// --- Scryfall (Magic: The Gathering) ---

data class ScryfallSearchResponse(
    val data: List<ScryfallCard>,
    val has_more: Boolean,
    val total_cards: Int?
)

data class ScryfallCard(
    val id: String,
    val name: String,
    val set: String,
    val set_name: String,
    val collector_number: String,
    val rarity: String,
    val image_uris: ScryfallImageUris?,  // null for double-faced cards
    val prices: ScryfallPrices,
    val card_faces: List<ScryfallCardFace>?  // double-faced cards store images here
)

data class ScryfallImageUris(
    val normal: String,
    val small: String
)

data class ScryfallPrices(
    val usd: String?,
    val usd_foil: String?
)

data class ScryfallCardFace(
    val image_uris: ScryfallImageUris?
)

// --- Pokémon TCG ---

data class PokemonTcgSearchResponse(
    val data: List<PokemonTcgCard>,
    val totalCount: Int
)

data class PokemonTcgCard(
    val id: String,
    val name: String,
    val set: PokemonTcgSet,
    val number: String,
    val rarity: String?,
    val images: PokemonTcgImages,
    val tcgplayer: PokemonTcgPriceData?
)

data class PokemonTcgSet(
    val id: String,
    val name: String,
    val printedTotal: Int
)

data class PokemonTcgImages(
    val small: String,
    val large: String
)

data class PokemonTcgPriceData(
    // Keys: "normal", "holofoil", "reverseHolofoil", etc.
    val prices: Map<String, PokemonTcgPriceEntry>?
)

data class PokemonTcgPriceEntry(
    val mid: Double?,
    val market: Double?
)

// --- YGOProDeck (Yu-Gi-Oh!) ---

data class YgoProResponse(
    val data: List<YgoProCard>
)

data class YgoProCard(
    val id: Long,
    val name: String,
    val type: String,
    val card_sets: List<YgoProCardSet>?,
    val card_images: List<YgoProCardImage>?,
    val card_prices: List<YgoProCardPrice>?
)

data class YgoProCardSet(
    val set_name: String,
    val set_code: String,
    val set_rarity: String,
    val set_price: String
)

data class YgoProCardImage(
    val id: Long,
    val image_url: String
)

data class YgoProCardPrice(
    val tcgplayer_price: String
)

// ============================================================
// Unified UI model — every TCG fragment and adapter uses this.
// Normalized from all three APIs by TcgRepository mappers.
// ============================================================

data class TcgSearchResult(
    val externalId: String,
    val tcgGame: String,           // "MTG" | "POKEMON" | "YUGIOH"
    val name: String,
    val setName: String,
    val setCode: String,
    val cardNumber: String,
    val rarity: String,
    val imageUrl: String,
    val priceRegular: Double?,     // USD price for non-foil
    val priceFoil: Double?,        // USD price for foil/holo; null if no foil variant exists
    val isFoilVariant: Boolean     // true when this result IS the foil entry (Pokémon emits two)
)
