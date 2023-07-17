package com.davidlang.divecolorcorrector

data class Histogram(val from: Int, val to: Int) {
    private val counts = IntArray(to-from+1)

    fun get(value: Int): Int {
        return counts[value - from]
    }

    fun increment(value: Int) {
        counts[value - from] += 1
    }
}