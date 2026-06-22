package com.lasallecollegevancouver.gameinventoryapp.ui.common

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.lasallecollegevancouver.gameinventoryapp.R

/**
 * DisplayCaseBinder centralises the small pieces of logic that every Display Case card needs,
 * so individual adapters stay short and consistent:
 *
 *   1. [loadArtwork]  - load an image URL into the card's ImageView via Glide, with a fade-in.
 *   2. [showGradeBadge] / [showCategoryBadge] - drive the floating top-right badge.
 *   3. [hideBadge]     - hide the badge when an item has nothing to flag.
 *
 * Keeping this in one place means the badge colors and image-loading behaviour are identical
 * on every screen that adopts the Display Case card.
 */
object DisplayCaseBinder {

    // --- Badge colors -------------------------------------------------------------------

    // Grading companies get their real brand colors so a collector recognises them instantly.
    private const val COLOR_PSA = 0xFFD32F2F.toInt()   // PSA red
    private const val COLOR_BGS = 0xFF1565C0.toInt()   // Beckett (BGS) blue
    private const val COLOR_CGC = 0xFF2E7D32.toInt()   // CGC green
    private const val COLOR_GRADE_DEFAULT = 0xFF455A64.toInt() // neutral slate for unknown graders

    // Trading-card game colors, matching the existing list-row badges elsewhere in the app.
    private const val COLOR_MTG = 0xFF7B2FBE.toInt()   // Magic purple
    private const val COLOR_POKEMON = 0xFFE3B000.toInt() // Pokémon gold
    private const val COLOR_YUGIOH = 0xFF1565C0.toInt() // Yu-Gi-Oh! blue
    private const val COLOR_CATEGORY_DEFAULT = 0xFF555555.toInt()

    /**
     * Load [imageUrl] into [imageView] using Glide, fading the artwork in once it arrives.
     * While loading (or if the URL is null/blank) the recessed background of the card shows
     * through, so the card never flashes an ugly broken-image icon.
     *
     * @param centerCrop when true the image is cropped to fill the whole frame (best for wide
     *        hero/box artwork like RAWG game art); when false it is fit inside the frame with
     *        no cropping (best for already-portrait trading-card scans).
     */
    fun loadArtwork(imageView: ImageView, imageUrl: String?, centerCrop: Boolean = false) {
        val request = Glide.with(imageView.context)
            .load(imageUrl?.takeIf { it.isNotBlank() })
            .transition(DrawableTransitionOptions.withCrossFade())

        if (centerCrop) request.centerCrop().into(imageView) else request.into(imageView)
    }

    /**
     * Show a grade slab badge such as "PSA 10".
     *
     * @param company the grading company, e.g. "PSA", "BGS", "CGC"
     * @param grade   the numeric grade, e.g. "10", "9.5"
     */
    fun showGradeBadge(badge: TextView, company: String, grade: String) {
        val badgeColor = when (company.uppercase()) {
            "PSA" -> COLOR_PSA
            "BGS", "BECKETT" -> COLOR_BGS
            "CGC" -> COLOR_CGC
            else -> COLOR_GRADE_DEFAULT
        }
        applyBadge(badge, "${company.uppercase()} $grade", badgeColor)
    }

    /**
     * Show a category/game badge such as "MTG", "PKM" or "YGO" in the floating slot.
     * Useful on screens (like raw TCG search results) where items are not graded but we still
     * want a colored tag in the corner of the card.
     */
    fun showCategoryBadge(badge: TextView, tcgGame: String) {
        val (label, color) = when (tcgGame.uppercase()) {
            "MTG" -> "MTG" to COLOR_MTG
            "POKEMON" -> "PKM" to COLOR_POKEMON
            "YUGIOH" -> "YGO" to COLOR_YUGIOH
            else -> tcgGame.uppercase() to COLOR_CATEGORY_DEFAULT
        }
        applyBadge(badge, label, color)
    }

    /**
     * Show an arbitrary badge with caller-chosen [text] and [backgroundColor].
     * Used for one-off tags that are not a grade or a TCG game, e.g. a "TRADE" flag on a game.
     */
    fun showCustomBadge(badge: TextView, text: String, backgroundColor: Int) {
        applyBadge(badge, text, backgroundColor)
    }

    /** Hide the floating badge entirely (item has no grade or category to show). */
    fun hideBadge(badge: TextView) {
        badge.visibility = View.GONE
    }

    // --- Internal -----------------------------------------------------------------------

    /**
     * Set the badge text, recolor its rounded background to [backgroundColor], and reveal it.
     *
     * We mutate() a fresh copy of the shared badge drawable before recoloring so that changing
     * one card's badge color never accidentally changes every other card sharing the drawable.
     */
    private fun applyBadge(badge: TextView, text: String, backgroundColor: Int) {
        val roundedBackground = ContextCompat
            .getDrawable(badge.context, R.drawable.bg_display_case_badge)
            ?.mutate() as? GradientDrawable

        roundedBackground?.setColor(backgroundColor)
        badge.background = roundedBackground
        badge.text = text
        badge.visibility = View.VISIBLE
    }
}
