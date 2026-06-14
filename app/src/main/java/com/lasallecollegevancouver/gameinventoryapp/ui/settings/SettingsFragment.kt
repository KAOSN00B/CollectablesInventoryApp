package com.lasallecollegevancouver.gameinventoryapp.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.AppConfig
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentSettingsBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val publicCode = PrefsHelper.getPublicCode(requireContext())
        binding.publicCodeText.text = publicCode ?: "—"

        binding.copyCodeButton.setOnClickListener { copyCodeToClipboard() }
        binding.shareLinkButton.setOnClickListener { shareCollectionLink() }
        binding.switchCollectionButton.setOnClickListener { switchCollection() }
        binding.signOutButton.setOnClickListener { confirmSignOut() }

        if (publicCode != null) {
            loadDisplayName(publicCode)
        }
    }

    private fun loadDisplayName(publicCode: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val collection = repository.getCollection(publicCode)
                binding.displayNameText.text = collection.displayName ?: "My Collection"
            } catch (exception: Exception) {
                binding.displayNameText.text = "My Collection"
            }
        }
    }

    private fun copyCodeToClipboard() {
        val code = binding.publicCodeText.text.toString().takeIf { it != "—" } ?: return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("CollectOS code", code))
        Toast.makeText(requireContext(), "Code copied!", Toast.LENGTH_SHORT).show()
    }

    private fun shareCollectionLink() {
        val code = PrefsHelper.getPublicCode(requireContext()) ?: return
        val baseUrl = AppConfig.API_BASE_URL.trimEnd('/')
        val shareUrl = "$baseUrl/c/$code"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out my game collection on CollectOS: $shareUrl")
        }
        startActivity(Intent.createChooser(intent, "Share collection via"))
    }

    private fun switchCollection() {
        val friendCode = binding.switchCodeInput.text?.toString()?.trim() ?: return
        if (friendCode.isBlank()) {
            binding.switchCodeInputLayout.error = "Enter a collection code"
            return
        }
        binding.switchCodeInputLayout.error = null
        binding.switchCollectionButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Verify the code exists before navigating — keeps your own code untouched
                repository.getCollection(friendCode)
                val bundle = Bundle().apply { putString("friendCode", friendCode) }
                findNavController().navigate(R.id.action_settings_to_friendCollection, bundle)
                binding.switchCodeInput.text?.clear()
            } catch (exception: Exception) {
                binding.switchCodeInputLayout.error = "Collection not found — check the code"
            } finally {
                binding.switchCollectionButton.isEnabled = true
            }
        }
    }

    private fun confirmSignOut() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("This will clear your saved collection code. Your data is safe on the server.")
            .setPositiveButton("Sign Out") { _, _ -> signOut() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signOut() {
        PrefsHelper.clearPublicCode(requireContext())
        // Pop everything and navigate to onboarding so the user goes through setup again
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        findNavController().navigate(R.id.onboardingFragment, null, navOptions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
