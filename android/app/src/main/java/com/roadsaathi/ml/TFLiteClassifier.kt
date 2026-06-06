package com.roadsaathi.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import timber.log.Timber
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val allProbabilities: Map<String, Float>
)

@Singleton
class TFLiteClassifier @Inject constructor(
    private val context: Context
) {
    private var interpreter: Interpreter? = null
    private val labels: List<String> = listOf(
        "pothole", "waterlogging", "accident_scene", "signage_missing", "road_collapse"
    )

    companion object {
        private const val MODEL_FILENAME = "hazard_classifier.tflite"
        private const val INPUT_SIZE = 224
        private const val NUM_THREADS = 4
        private const val CONFIDENCE_THRESHOLD = 0.45f
    }

    private var isAvailable: Boolean = false

    init {
        try {
            context.assets.open(MODEL_FILENAME).use { Timber.d("Model asset found") }
            loadModel()
            isAvailable = interpreter != null
        } catch (e: Exception) {
            Timber.w(e, "TFLite model not available, running in stub mode")
            isAvailable = false
            interpreter = null
        }
    }

    fun isModelAvailable(): Boolean = isAvailable

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(NUM_THREADS)
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    try {
                        val gpuDelegate = GpuDelegate(
                            GpuDelegate.Options().apply {
                                inferencePreference = GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED
                            }
                        )
                        addDelegate(gpuDelegate)
                    } catch (e: Exception) {
                        Timber.d(e, "GPU delegate unavailable, falling back to CPU")
                    }
                }
            }
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load TFLite model")
            interpreter = null
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILENAME)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val mappedByteBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
        fileChannel.close()
        inputStream.close()
        assetFileDescriptor.close()
        return mappedByteBuffer
    }

    fun classify(bitmap: Bitmap): ClassificationResult {
        val interp = interpreter
        if (interp == null || !isAvailable) {
            return ClassificationResult("unclassified", 0f, labels.associateWith { 0f })
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
        val outputArray = Array(1) { FloatArray(labels.size) }

        interp.run(inputBuffer, outputArray)

        val probabilities = outputArray[0]
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[maxIndex]

        val allProbMap = labels.mapIndexed { index, label ->
            label to probabilities[index]
        }.toMap()

        val label = if (confidence >= CONFIDENCE_THRESHOLD) labels[maxIndex] else "unclassified"
        return ClassificationResult(
            label = label,
            confidence = confidence,
            allProbabilities = allProbMap
        )
    }

    suspend fun classifyAsync(bitmap: Bitmap): ClassificationResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            classify(bitmap)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 127.5f - 1.0f
            val g = ((pixel shr 8) and 0xFF) / 127.5f - 1.0f
            val b = (pixel and 0xFF) / 127.5f - 1.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }

    fun cleanup() {
        interpreter?.close()
        interpreter = null
    }

    fun close() = cleanup()
}
