package com.davidlang.divecolorcorrector


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


class ColorCorrector(var bitmap: Bitmap) {
    var progressCallback: (Float) -> Unit = { }

    private val progressUpdateEvery = 100000 // pixels
    private val progressInAverageRGB = 0.2f
    private val progressInCreateHistograms = 0.75f // leave a gap so we don't send 1f until actually complete

    fun applyFilter(filter: ColorMatrix): Bitmap {
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(filter.values) }
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return newBitmap
    }

    fun Int.clip(min: Int, max: Int): Int {
        if (this < min)
            return min
        if (this > max)
            return max
        return this
    }

    fun averageRGB(): DoubleColor {
        var r = 0
        var g = 0
        var b = 0
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val progressPerPixel = progressInAverageRGB / pixels.size
        for (i in pixels.indices) {
            if (i % progressUpdateEvery == 0) {
                progressCallback(i * progressPerPixel)
            }
            val color = pixels[i]
            r += Color.red(color)
            g += Color.green(color)
            b += Color.blue(color)
        }
        val total = pixels.size.toDouble()
        val average = DoubleColor(r / total, g / total, b / total)
        progressCallback(progressInAverageRGB)
        return average
    }

    fun createHistograms(hueShift: Int) : Triple<Histogram, Histogram, Histogram> {
        val histR = Histogram(0, 255)
        val histG = Histogram(0, 255)
        val histB = Histogram(0, 255)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val progressPerPixel = progressInCreateHistograms / pixels.size
        for (i in pixels.indices) {
            if (i % progressUpdateEvery == 0) {
                progressCallback(progressInAverageRGB + i * progressPerPixel)
            }

            val color = IntColor(pixels[i])

            var shiftedR = hueShiftRed(color, hueShift).sum() // Use new calculated red value
            if (shiftedR > 255) {
                shiftedR = 255
            } else if (shiftedR < 0) { //TODO is this really possible
                shiftedR = 0
            }

            histR.increment(shiftedR)
            histG.increment(color.g)
            histB.increment(color.b)
        }
        progressCallback(progressInAverageRGB + progressInCreateHistograms)
        return Triple(histR, histG, histB)
    }

    fun underwaterFilter() : ColorMatrix {
        progressCallback(0f)
        // Based on algorithm: https://github.com/nikolajbech/underwater-image-color-correction

        // Magic values:
        val numOfPixels = bitmap.width * bitmap.height
        val thresholdRatio = 2000
        val thresholdLevel = numOfPixels / thresholdRatio
        val minAvgRed: Double = 60.0
        val maxHueShift: Int = 120
        val blueMagicValue: Float = 1.2f

        // Calculate average color:
        val avg = averageRGB()

        // Calculate shift amount:
        var hueShift = 0
        var newAvgRed = avg.r
        while (newAvgRed < minAvgRed) {
            newAvgRed = hueShiftRed(avg, hueShift).sum()
            hueShift++
            if (hueShift > maxHueShift) newAvgRed = 60.0 // Max value
        }

        // Create histogram with new red values:
        val (histR, histG, histB) = createHistograms(hueShift);

        // Normalise values:
        val adjustR = normalizingInterval(histR, thresholdLevel)
        val adjustG = normalizingInterval(histG, thresholdLevel)
        val adjustB = normalizingInterval(histB, thresholdLevel)

        // Make histogram:
        val shifted = hueShiftRed(DoubleColor(1.0,1.0,1.0), hueShift)

        val fullGain: Float = 256f
        val redGain = fullGain / adjustR.diff()
        val greenGain = fullGain / adjustG.diff()
        val blueGain = fullGain / adjustB.diff()

        val redOffset = (-adjustR.low / fullGain) * redGain
        val greenOffset = (-adjustG.low / fullGain) * greenGain
        val blueOffset = (-adjustB.low / fullGain) * blueGain

        val modifiedRed = shifted.r.toFloat() * redGain
        val modifiedRedGreen = shifted.g.toFloat() * redGain
        val modifiedRedBlue = shifted.b.toFloat() * redGain * blueMagicValue

        val matrix = ColorMatrix(floatArrayOf(
            modifiedRed, modifiedRedGreen, modifiedRedBlue, 0f, redOffset,
            0f, greenGain, 0f, 0f, greenOffset,
            0f, 0f, blueGain, 0f, blueOffset,
            0f, 0f, 0f, 1f, 0f
        ));

        progressCallback(1f)
        return matrix
    }

    companion object {
        fun hueShiftRed(rgb: DoubleColor, hueShift: Int): DoubleColor {
            val u = cos(hueShift * PI / 180)
            val w = sin(hueShift * PI / 180)
            return DoubleColor(
                r = (0.299 + 0.701 * u + 0.168 * w) * rgb.r,
                g = (0.587 - 0.587 * u + 0.330 * w) * rgb.g,
                b = (0.114 - 0.114 * u - 0.497 * w) * rgb.b
            )
        }

        fun hueShiftRed(rgb: IntColor, hueShift: Int): IntColor {
            val u = cos(hueShift * PI / 180)
            val w = sin(hueShift * PI / 180)
            return IntColor(
                r = ((0.299 + 0.701 * u + 0.168 * w) * rgb.r).toInt(),
                g = ((0.587 - 0.587 * u + 0.330 * w) * rgb.g).toInt(),
                b = ((0.114 - 0.114 * u - 0.497 * w) * rgb.b).toInt()
            )
        }

        fun normalizingInterval(histogram: Histogram, threshold: Int): Interval {
            val normalize = ArrayList<Int>()

            // Push 0 as start value in normalize array:
            normalize.add(0)

            // Find values under threshold:
            for (i in histogram.from..histogram.to) { // 0-255 inclusive
                if (histogram.get(i) - threshold < 2) normalize.add(i)
            }

            // Push 255 as end value in normalize array:
            normalize.add(255)

            return normalizingInterval(normalize)
        }

        fun normalizingInterval(normArray: ArrayList<Int>): Interval {
            var high: Int = 255
            var low: Int = 0
            var maxDist: Int = 0
            for (i in 1 until normArray.size) {
                val dist = normArray[i] - normArray[i - 1]
                if (dist > maxDist) {
                    maxDist = dist;
                    high = normArray[i]
                    low = normArray[i - 1]
                }
            }
            return Interval(low, high)
        }
    }
}