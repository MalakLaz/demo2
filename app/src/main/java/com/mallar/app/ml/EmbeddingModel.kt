package com.mallar.app.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

class EmbeddingModel(context: Context) {

    private val interpreter: Interpreter

    init {
        val asset = context.assets.openFd("model.tflite")
        val inputStream = FileInputStream(asset.fileDescriptor)
        val fileChannel = inputStream.channel
        val modelBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            asset.startOffset,
            asset.declaredLength
        )
        interpreter = Interpreter(modelBuffer)
    }

    fun run(bitmap: Bitmap): FloatArray {
        val input = preprocess(bitmap)
        val fullOutput = Array(1) { FloatArray(1280) }
        interpreter.run(input, fullOutput)
        return fullOutput[0]
    }

    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resized.getPixel(x, y)
                // ✅ نفس الـ preprocess_input في Keras: (pixel - 127.5) / 127.5
                input[0][y][x][0] = ((pixel shr 16 and 0xFF) - 127.5f) / 127.5f  // R
                input[0][y][x][1] = ((pixel shr 8  and 0xFF) - 127.5f) / 127.5f  // G
                input[0][y][x][2] = ((pixel         and 0xFF) - 127.5f) / 127.5f  // B
            }
        }
        return input
    }
}