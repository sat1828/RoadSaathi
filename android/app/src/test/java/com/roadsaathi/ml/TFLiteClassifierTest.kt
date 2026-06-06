package com.roadsaathi.ml

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TFLiteClassifierTest {

    @Test
    fun `classifier handles missing model gracefully`() {
        val classifier = TFLiteClassifier(RuntimeEnvironment.getApplication())
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val result = classifier.classify(bitmap)

        assertThat(result.label).isEqualTo("unclassified")
        assertThat(result.confidence).isEqualTo(0f)
        assertThat(classifier.isModelAvailable()).isFalse()
    }

    @Test
    fun `close does not throw when model missing`() {
        val classifier = TFLiteClassifier(RuntimeEnvironment.getApplication())
        classifier.close()
    }
}
