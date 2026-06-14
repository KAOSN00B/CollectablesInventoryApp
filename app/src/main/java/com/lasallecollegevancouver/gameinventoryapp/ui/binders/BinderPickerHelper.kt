package com.lasallecollegevancouver.gameinventoryapp.ui.binders

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.lasallecollegevancouver.gameinventoryapp.network.Binder
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Reusable helper that shows an AlertDialog for picking or creating a binder.
// Used by every add-item flow (TCG, game, console).
object BinderPickerHelper {

    // Shows the picker dialog. Calls onSelected with the chosen binderId (null = no binder).
    // binders: already-loaded list from the backend. publicCode + repository: used if the
    // user creates a new binder inline.
    fun showPicker(
        context: Context,
        coroutineScope: CoroutineScope,
        binders: List<Binder>,
        publicCode: String,
        repository: CollectOsRepository,
        currentBinderId: Int?,
        onSelected: (binderId: Int?, binderName: String?) -> Unit
    ) {
        // Build the displayed list: existing binder names + "New Binder..." at the bottom
        val displayNames = binders.map { it.name }.toMutableList()
        displayNames.add("+ New Binder...")

        val currentIndex = binders.indexOfFirst { it.id == currentBinderId }.coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle("Add to Binder")
            .setSingleChoiceItems(displayNames.toTypedArray(), currentIndex) { dialog, index ->
                if (index == binders.size) {
                    // User chose "New Binder..." — show the creation dialog
                    dialog.dismiss()
                    showCreateDialog(context, coroutineScope, publicCode, repository, onSelected)
                } else {
                    // User chose an existing binder
                    val chosen = binders[index]
                    dialog.dismiss()
                    onSelected(chosen.id, chosen.name)
                }
            }
            .setNegativeButton("No Binder") { _, _ -> onSelected(null, null) }
            .show()
    }

    // Shows an inline text input dialog to create a new binder, then calls the callback
    private fun showCreateDialog(
        context: Context,
        coroutineScope: CoroutineScope,
        publicCode: String,
        repository: CollectOsRepository,
        onSelected: (binderId: Int?, binderName: String?) -> Unit
    ) {
        val nameInput = EditText(context).apply {
            hint = "Binder name (e.g. Base Set Holos)"
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(context)
            .setTitle("New Binder")
            .setView(nameInput)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    coroutineScope.launch {
                        try {
                            val newBinder = withContext(Dispatchers.IO) {
                                repository.createBinder(publicCode, name)
                            }
                            onSelected(newBinder.id, newBinder.name)
                        } catch (exception: Exception) {
                            // If creation fails, proceed without a binder
                            onSelected(null, null)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
