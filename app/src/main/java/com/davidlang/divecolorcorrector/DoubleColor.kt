package com.davidlang.divecolorcorrector

import kotlin.math.roundToInt

data class DoubleColor(val r: Double, val g: Double, val b: Double) {
    fun sum(): Double {
        return r + g + b
    }

    fun roundToIntColor(): IntColor {
        return IntColor(
            r.roundToInt(),
            g.roundToInt(),
            b.roundToInt()
        )
    }
}