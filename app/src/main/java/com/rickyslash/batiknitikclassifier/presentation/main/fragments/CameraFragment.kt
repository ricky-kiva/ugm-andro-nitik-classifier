package com.rickyslash.batiknitikclassifier.presentation.main.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.rickyslash.batiknitikclassifier.R
import com.rickyslash.batiknitikclassifier.databinding.FragmentCameraBinding
import com.rickyslash.batiknitikclassifier.ui.ClassificationResultsAdapter
import com.rickyslash.batiknitikclassifier.helper.ImageClassifierHelper
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), ImageClassifierHelper.ClassifierListener {
    private var _binding: FragmentCameraBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var cameraExecutor: ExecutorService

    private val classificationResultsAdapter by lazy {
        ClassificationResultsAdapter().apply {
            updateAdapterSize(imageClassifierHelper.maxResults)
        }
    }

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageClassifierHelper = ImageClassifierHelper(
            context = requireContext(),
            imageClassifierListener = this
        )

        with (binding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = classificationResultsAdapter
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.viewFinder.post {
            setupCamera()
        }

        initBottomSheetControls()
    }

    override fun onResume() {
        super.onResume()

        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()

        cameraExecutor.shutdown()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.viewFinder.display.rotation
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        @Suppress("DEPRECATION")
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        @Suppress("DEPRECATION")
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { img ->
                    if (!::bitmapBuffer.isInitialized) bitmapBuffer = Bitmap
                        .createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)

                    classifyImage(img)
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider
                .bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use care binding failed: $exc")
        }
    }

    private fun classifyImage(image: ImageProxy) {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        imageClassifierHelper.classify(bitmapBuffer, getScreenOrientation())
    }

    @Suppress("DEPRECATION")
    private fun getScreenOrientation(): Int {
        val display: Display?
        val outMetrics = DisplayMetrics()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display = requireActivity().display
            display?.getRealMetrics(outMetrics)
        } else {
            display = requireActivity().windowManager.defaultDisplay
            display.getMetrics(outMetrics)
        }

        return display?.rotation ?: 0
    }

    private fun initBottomSheetControls() {
        binding.bottomSheetLayout.apply {
            thresholdMinus.setOnClickListener {
                if (imageClassifierHelper.threshold >= 0.1) {
                    imageClassifierHelper.threshold -= 0.1f
                    updateControlsUI()
                }
            }

            thresholdPlus.setOnClickListener {
                if (imageClassifierHelper.threshold < 0.9) {
                    imageClassifierHelper.threshold += 0.1f
                    updateControlsUI()
                }
            }

            maxResultsMinus.setOnClickListener {
                if (imageClassifierHelper.maxResults > 1) {
                    imageClassifierHelper.maxResults--
                    updateControlsUI()
                    classificationResultsAdapter
                        .updateAdapterSize(imageClassifierHelper.maxResults)
                }
            }

            maxResultsPlus.setOnClickListener {
                if (imageClassifierHelper.maxResults < 3) {
                    imageClassifierHelper.maxResults++
                    updateControlsUI()
                    classificationResultsAdapter
                        .updateAdapterSize(imageClassifierHelper.maxResults)
                }
            }

            threadsMinus.setOnClickListener {
                if (imageClassifierHelper.numThreads > 1) {
                    imageClassifierHelper.numThreads--
                    updateControlsUI()
                }
            }

            threadsPlus.setOnClickListener {
                if (imageClassifierHelper.numThreads < 4) {
                    imageClassifierHelper.numThreads++
                    updateControlsUI()
                }
            }

            spinnerDelegate.setSelection(0, false)
            spinnerDelegate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    imageClassifierHelper.currentDelegate = position
                    updateControlsUI()
                }
            }

            spinnerModel.setSelection(0, false)
            spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    imageClassifierHelper.currentModel = position
                    updateControlsUI()
                }
            }
        }
    }

    private fun updateControlsUI() {
        binding.bottomSheetLayout.apply {
            maxResultsValue.text = imageClassifierHelper.maxResults.toString()
            thresholdValue.text = imageClassifierHelper.threshold.let { "%.2f".format(it) }
            threadsValue.text = imageClassifierHelper.numThreads.toString()
        }

        imageClassifierHelper.clearImageClassifier()
    }

    companion object {
        private val TAG = CameraFragment::class.java.simpleName
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            classificationResultsAdapter.updateResults(null)
            classificationResultsAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        activity?.runOnUiThread {
            classificationResultsAdapter.updateResults(results)
            classificationResultsAdapter.notifyDataSetChanged()
            binding.bottomSheetLayout.inferenceTimeVal.text = inferenceTime.let { "%d ms".format(it) }
        }
    }
}