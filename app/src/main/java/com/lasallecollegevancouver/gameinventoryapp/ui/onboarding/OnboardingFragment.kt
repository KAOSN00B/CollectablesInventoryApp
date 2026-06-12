package com.lasallecollegevancouver.gameinventoryapp.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentOnboardingBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val savedCode = requireContext()
            .getSharedPreferences("collectos_prefs", Context.MODE_PRIVATE)
            .getString("public_code", null)
        if (!savedCode.isNullOrBlank()) {
            navigateToMain()
            return
        }

        binding.buttonCreateCollection.setOnClickListener { createNewCollection() }
        binding.buttonEnterCode.setOnClickListener {
            val enteredCode = binding.editTextCollectionCode.text.toString().trim().uppercase()
            if (enteredCode.length != 5) {
                Toast.makeText(requireContext(), "Enter your 5-character collection code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyAndSaveCode(enteredCode)
        }
    }

    private fun createNewCollection() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val displayName = binding.editTextDisplayName.text.toString().trim().ifEmpty { null }
                val collection = repository.createCollection(displayName)
                savePublicCode(collection.publicCode)
                navigateToMain()
            } catch (exception: Exception) {
                showError("Could not reach the server. Check your connection and try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun verifyAndSaveCode(code: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                repository.getCollection(code)
                savePublicCode(code)
                navigateToMain()
            } catch (exception: Exception) {
                showError("Collection not found. Check the code and try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.buttonCreateCollection.isEnabled = !isLoading
        binding.buttonEnterCode.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun savePublicCode(code: String) {
        requireContext()
            .getSharedPreferences("collectos_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("public_code", code)
            .apply()
    }

    private fun navigateToMain() {
        findNavController().navigate(R.id.action_onboarding_to_dashboard)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
