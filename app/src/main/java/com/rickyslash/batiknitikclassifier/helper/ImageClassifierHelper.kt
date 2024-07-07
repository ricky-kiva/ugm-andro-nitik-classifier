package com.rickyslash.batiknitikclassifier.helper

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class ImageClassifierHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    val context: Context,
    val imageClassifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    fun clearImageClassifier() {
        imageClassifier = null
    }

    fun classify(image: Bitmap, rotation: Int) {
        if (imageClassifier == null)
            setupImageClassifier()

        var inferenceTime = SystemClock.uptimeMillis()

        val imageProcessor = ImageProcessor.Builder().build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        imageClassifierListener?.onResults(results, inferenceTime)
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            imageClassifier = ImageClassifier
                .createFromFileAndOptions(context, MODEL_NAME, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            imageClassifierListener
                ?.onError("Image classifier failed to initialize. See error logs for details")

            Log.e(TAG, "TFLite failed to load model with error: ${e.message}")
        }
    }

    // receive device rotation from Surface, then return EXIF orientation
    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT // rotated 180 degrees
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM // rotated 90 degrees counterclockwise & flipped horizontally
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT // no rotation (default orientation)
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP // rotated 90 degrees
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(results: List<Classifications>?, inferenceTime: Long)
    }

    companion object {
        private val TAG = ImageClassifierHelper::class.java.simpleName
        private const val MODEL_NAME = "ugm-nitik-60.tflite"
    }
}