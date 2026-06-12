package com.lasallecollegevancouver.gameinventoryapp.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Game
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentGameDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.PriceChartingRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameDetailFragment : Fragment() {

    private var _binding: FragmentGameDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private val priceChartingRepository = PriceChartingRepository()

    // Holds the loaded game so Edit, Delete, and Refresh actions can reference it
    private var currentGame: Game? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())
        setupMenuItems()
        loadGame()

        // Refresh Market Price — only shown and active when we have a PriceCharting ID
        binding.refreshPriceButton.setOnClickListener { refreshMarketPrice() }
    }

    // Registers Edit and Delete toolbar menu items
    private fun setupMenuItems() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> { navigateToEdit(); true }
                    R.id.action_delete -> { showDeleteConfirmation(); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // Reads the gameId from the Bundle and fetches the matching game from Room
    private fun loadGame() {
        val gameId = arguments?.getInt("gameId", -1) ?: -1
        if (gameId == -1) {
            findNavController().popBackStack()
            return
        }
        lifecycleScope.launch {
            currentGame = database.gameDao().getGameById(gameId)
            currentGame?.let { game -> displayGame(game) }
        }
    }

    // Fills every TextView with the game's stored data
    private fun displayGame(game: Game) {
        binding.detailTitle.text = game.title
        binding.detailPlatform.text = "Platform: ${game.platform}"
        binding.detailGenre.text = "Genre: ${game.genre}"
        binding.detailCondition.text = "Condition: ${game.condition}"
        binding.detailCompletion.text = "Status: ${game.completionStatus}"
        binding.detailPurchasePrice.text = "Purchase Price: $${String.format("%.2f", game.purchasePrice)}"
        binding.detailEstimatedValue.text = "Estimated Value: $${String.format("%.2f", game.estimatedValue)}"
        binding.detailNotes.text = if (game.notes.isNotBlank()) "Notes: ${game.notes}" else ""

        // Convert stored epoch milliseconds to a human-readable date string
        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.detailDateAdded.text = "Added: ${dateFormatter.format(Date(game.dateAdded))}"

        // Show the last price check date if available
        if (game.lastPriceCheck != null) {
            binding.detailLastPriceCheck.text = "Price last updated: ${dateFormatter.format(Date(game.lastPriceCheck))}"
            binding.detailLastPriceCheck.visibility = View.VISIBLE
        } else {
            binding.detailLastPriceCheck.visibility = View.GONE
        }

        // Only show the refresh button if this item has a PriceCharting ID to look up
        binding.refreshPriceButton.visibility =
            if (game.priceChartingId != null) View.VISIBLE else View.GONE
    }

    // Calls PriceCharting, updates estimatedValue in Room, and shows a Snackbar with the change
    private fun refreshMarketPrice() {
        val game = currentGame ?: return
        val priceChartingId = game.priceChartingId ?: return

        binding.refreshPriceButton.isEnabled = false
        binding.refreshPriceButton.text = "Refreshing..."

        lifecycleScope.launch {
            val priceData = priceChartingRepository.refreshPrice(priceChartingId)
            if (priceData == null) {
                Snackbar.make(requireView(), "Price refresh failed — check your connection", Snackbar.LENGTH_SHORT).show()
                binding.refreshPriceButton.isEnabled = true
                binding.refreshPriceButton.text = "Refresh Market Price"
                return@launch
            }

            val oldValue = game.estimatedValue
            val newValue = priceData.bestPrice()

            // Save the updated price and timestamp back to Room
            val updatedGame = game.copy(
                estimatedValue = newValue,
                lastPriceCheck = System.currentTimeMillis()
            )
            database.gameDao().updateGame(updatedGame)
            currentGame = updatedGame
            displayGame(updatedGame)

            val changeText = when {
                newValue > oldValue -> "+$${String.format("%.2f", newValue - oldValue)}"
                newValue < oldValue -> "-$${String.format("%.2f", oldValue - newValue)}"
                else -> "no change"
            }
            Snackbar.make(
                requireView(),
                "Market price updated to $${String.format("%.2f", newValue)} ($changeText)",
                Snackbar.LENGTH_LONG
            ).show()

            binding.refreshPriceButton.isEnabled = true
            binding.refreshPriceButton.text = "Refresh Market Price"
        }
    }

    // Navigates to AddEditGameFragment in edit mode, passing the current game's id
    private fun navigateToEdit() {
        val bundle = Bundle().apply { putInt("gameId", currentGame?.id ?: -1) }
        findNavController().navigate(R.id.action_gameDetail_to_addEditGame, bundle)
    }

    // Asks the user to confirm before permanently deleting the game
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Game")
            .setMessage("Delete \"${currentGame?.title}\" from your collection?")
            .setPositiveButton("Delete") { _, _ -> deleteGame() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGame() {
        val gameToDelete = currentGame ?: return
        lifecycleScope.launch {
            database.gameDao().deleteGame(gameToDelete)
            Snackbar.make(requireView(), "${gameToDelete.title} deleted", Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
