package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentBarcodeScanBinding
import com.lasallecollegevancouver.gameinventoryapp.network.PriceChartingRepository
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// Full-screen camera preview that scans for barcodes using ML Kit
// On a successful scan, looks up the UPC on PriceCharting and navigates to the add form
class BarcodeScanFragment : Fragment() {

    private var _binding: FragmentBarcodeScanBinding? = null
    private val binding get() = _binding!!

    private val priceChartingRepository = PriceChartingRepository()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // Prevents multiple scans from firing at the same time
    private var isScanHandled = false

    // Permission launcher — requests camera access at runtime
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to scan barcodes", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarcodeScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }

        // Check for camera permission before starting the preview
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Starts the CameraX camera with a barcode analysis use case
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview — shows what the camera sees in the PreviewView
            val preview = Preview.Builder().build().also { previewUseCase ->
                previewUseCase.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            // ImageAnalysis — passes frames to the ML Kit barcode scanner
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase ->
                    analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageForBarcode(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (exception: Exception) {
                Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Runs ML Kit barcode scanning on a single camera frame
    @androidx.camera.core.ExperimentalGetImage
    private fun processImageForBarcode(imageProxy: androidx.camera.core.ImageProxy) {
        if (isScanHandled) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val upc = barcodes
                    .filter { it.format == Barcode.FORMAT_UPC_A || it.format == Barcode.FORMAT_UPC_E || it.format == Barcode.FORMAT_EAN_13 }
                    .mapNotNull { it.rawValue }
                    .firstOrNull()

                if (upc != null && !isScanHandled) {
                    isScanHandled = true
                    onBarcodeDetected(upc)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    // Called when a valid barcode is detected — looks it up on PriceCharting
    private fun onBarcodeDetected(upc: String) {
        binding.scanStatusText.text = "Found barcode: $upc\nLooking up price..."
        binding.scanStatusText.visibility = View.VISIBLE

        lifecycleScope.launch {
            val product = priceChartingRepository.searchByBarcode(upc)
            if (product != null) {
                // Pass the found product data to the add game form via a Bundle
                val bundle = Bundle().apply {
                    putString("prefillTitle", product.productName)
                    putString("prefillPlatform", product.consoleName ?: "")
                    putInt("prefillPriceChartingId", product.id)
                    putDouble("prefillEstimatedValue", product.bestPrice())
                }
                findNavController().navigate(
                    com.lasallecollegevancouver.gameinventoryapp.R.id.action_barcodeScan_to_addEditGame,
                    bundle
                )
            } else {
                binding.scanStatusText.text = "No results found for barcode: $upc\nTry manual entry."
                // Allow scanning again after a failed lookup
                isScanHandled = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
