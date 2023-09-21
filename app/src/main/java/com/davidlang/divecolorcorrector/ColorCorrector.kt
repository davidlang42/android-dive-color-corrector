package com.davidlang.divecolorcorrector

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class ColorCorrector(var bitmap: Bitmap) {
    fun applyFilter(filter: ColorMatrix): Bitmap {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val f = filter.values
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val old = bitmap.getPixel(x, y)
                val r = Color.red(old)
                val g = Color.green(old)
                val b = Color.blue(old)
                //val a = Color.alpha(old) // seems to assume alpha is always 255
                val new = Color.argb(
                    255,
                    (r * f[0] + g * f[1] + b * f[2] + f[4] * 255).roundToInt().clip(0, 255),
                    (g * f[6] + f[9] * 255).roundToInt().clip(0, 255),
                    (b * f[12] + f[14] * 255).roundToInt().clip(0, 255)
                )
                newBitmap.setPixel(x, y, new)
            }
        }
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
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val color = bitmap.getPixel(x, y)
                r += Color.red(color)
                g += Color.green(color)
                b += Color.blue(color)
            }
        }
        val total = (bitmap.width * bitmap.height).toDouble()
        return DoubleColor(r/total, g/total, b/total)
    }

    fun underwaterFilter() : ColorMatrix {
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
        val histR = Histogram(0, 255)
        val histG = Histogram(0, 255)
        val histB = Histogram(0, 255)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val color = IntColor(pixel)

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
        }

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

        return ColorMatrix(floatArrayOf(
            modifiedRed, modifiedRedGreen, modifiedRedBlue, 0f, redOffset,
            0f, greenGain, 0f, 0f, greenOffset,
            0f, 0f, blueGain, 0f, blueOffset,
            0f, 0f, 0f, 1f, 0f
        ))
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