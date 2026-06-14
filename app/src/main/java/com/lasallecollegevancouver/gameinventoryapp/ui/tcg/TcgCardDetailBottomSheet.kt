package com.lasallecollegevancouver.gameinventoryapp.ui.tcg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.BottomSheetTcgCardDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.AddItemRequest
import com.lasallecollegevancouver.gameinventoryapp.network.Binder
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.TcgSearchResult
import com.lasallecollegevancouver.gameinventoryapp.ui.binders.BinderPickerHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TcgCardDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTcgCardDetailBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()

    // Quantity is tracked as state — the stepper buttons mutate this
    private var currentQuantity = 1

    // Binder selection — null means no binder chosen
    private var selectedBinderId: Int? = null
    private var loadedBinders: List<Binder> = emptyList()

    companion object {
        const val ARG_TCG_RESULT = "tcgSearchResultJson"

        fun newInstance(result: TcgSearchResult): TcgCardDetailBottomSheet {
            return TcgCardDetailBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_TCG_RESULT, Gson().toJson(result)) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetTcgCardDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val card = arguments?.getString(ARG_TCG_RESULT)
            ?.let { Gson().fromJson(it, TcgSearchResult::class.java) }
            ?: run { dismiss(); return }

        populateCardInfo(card)
        setupFoilToggle(card)
        setupQuantityStepper()
        setupBinderPicker()
        setupAddButton(card)
    }

    // Fill in the card image, name, set, rarity, and price row at the top of the sheet
    private fun populateCardInfo(card: TcgSearchResult) {
        Glide.with(this)
            .load(card.imageUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(binding.cardImage)

        binding.cardName.text = card.name
        binding.cardSetInfo.text = buildString {
            append(card.setName)
            if (card.cardNumber.isNotBlank()) append("  •  #${card.cardNumber}")
        }
        binding.cardRarity.text = card.rarity

        // Price row — show both regular and foil if available
        binding.cardPrices.text = buildString {
            val regularPrice = card.priceRegular
            val foilPrice = card.priceFoil
            if (regularPrice != null) append("$%.2f".format(regularPrice))
            if (regularPrice != null && foilPrice != null) append("  |  Foil: ")
            if (foilPrice != null) append("$%.2f".format(foilPrice))
            if (regularPrice == null && foilPrice == null) append("Price unavailable")
        }

        // Pre-fill purchase price with the market price appropriate for this variant
        val marketPrice = if (card.isFoilVariant) card.priceFoil else card.priceRegular
        if (marketPrice != null) {
            binding.purchasePriceInput.setText(String.format("%.2f", marketPrice))
        }
    }

    // Show the foil switch only when a foil price exists (MTG foil, Pokémon holo).
    // If this result IS the foil variant (Pokémon emits two results), start the toggle checked.
    private fun setupFoilToggle(card: TcgSearchResult) {
        if (card.priceFoil != null) {
            binding.foilSwitch.visibility = View.VISIBLE
            binding.foilSwitch.isChecked = card.isFoilVariant
        } else {
            binding.foilSwitch.visibility = View.GONE
        }
    }

    // [−] and [+] buttons update currentQuantity and the display label; minimum is 1
    private fun setupQuantityStepper() {
        updateQuantityDisplay()

        binding.quantityDecrease.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                updateQuantityDisplay()
            }
        }

        binding.quantityIncrease.setOnClickListener {
            currentQuantity++
            updateQuantityDisplay()
        }
    }

    private fun updateQuantityDisplay() {
        binding.quantityDisplay.text = currentQuantity.toString()
    }

    // Load binders in the background when the sheet opens, so they're ready when the user taps
    private fun setupBinderPicker() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return

        lifecycleScope.launch {
            try {
                loadedBinders = repository.getBinders(publicCode)
            } catch (exception: Exception) {
                loadedBinders = emptyList()
            }
        }

        binding.binderPickerRow.setOnClickListener {
            val publicCode2 = PrefsHelper.getPublicCode(requireContext()) ?: return@setOnClickListener
            BinderPickerHelper.showPicker(
                context = requireContext(),
                coroutineScope = lifecycleScope,
                binders = loadedBinders,
                publicCode = publicCode2,
                repository = repository,
                currentBinderId = selectedBinderId
            ) { binderId, binderName ->
                selectedBinderId = binderId
                binding.binderPickerLabel.text = binderName ?: "None"
                // Refresh loaded binders in case user created a new one
                lifecycleScope.launch {
                    try { loadedBinders = repository.getBinders(publicCode2) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun setupAddButton(card: TcgSearchResult) {
        binding.addToCollectionButton.setOnClickListener {
            val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return@setOnClickListener

            val selectedCondition = getSelectedCondition()
            val purchasePrice = binding.purchasePriceInput.text.toString().toDoubleOrNull() ?: 0.0
            val notes = binding.notesInput.text?.toString()?.trim()?.ifEmpty { null }
            val forTrade = binding.forTradeSwitch.isChecked
            val isFoil = binding.foilSwitch.isChecked

            // estimatedValue = whichever market price applies to this copy (foil or regular)
            val estimatedValue = if (isFoil && card.priceFoil != null) {
                card.priceFoil
            } else {
                card.priceRegular ?: 0.0
            }

            binding.addToCollectionButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    repository.addItem(
                        publicCode,
                        AddItemRequest(
                            catalogItemId = null,
                            type = "TCG",
                            title = card.name,
                            platform = card.setName,    // reuse platform field for the set name
                            condition = selectedCondition,
                            purchasePrice = purchasePrice,
                            estimatedValue = estimatedValue,
                            notes = notes,
                            forTrade = forTrade,
                            tcgGame = card.tcgGame,
                            tcgSet = card.setName,
                            tcgSetCode = card.setCode,
                            tcgCardNumber = card.cardNumber,
                            tcgRarity = card.rarity,
                            tcgIsFoil = isFoil,
                            tcgExternalId = card.externalId,
                            quantity = currentQuantity,
                            binderId = selectedBinderId
                        )
                    )
                    dismiss()
                } catch (exception: Exception) {
                    binding.addToCollectionButton.isEnabled = true
                    view?.let {
                        Snackbar.make(it, "Could not save — check your connection", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Read whichever NM/LP/MP/HP/DMG chip is currently checked
    private fun getSelectedCondition(): String {
        return when (binding.conditionChipGroup.checkedChipId) {
            R.id.chip_lp  -> "LP"
            R.id.chip_mp  -> "MP"
            R.id.chip_hp  -> "HP"
            R.id.chip_dmg -> "DMG"
            else          -> "NM"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
