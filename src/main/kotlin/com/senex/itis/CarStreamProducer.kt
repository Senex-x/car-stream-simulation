package com.senex.itis

class CarStreamProducer(
    private val intensity: Double,
    private val startingSpeed: Double,
    private val carThreshold: Int = Int.MAX_VALUE,
) {
    private var lastProducedTime: Double = -100.0
    private var carsProduced = 0

    fun tryProduce(seconds: Double): Car? =
        if (carThreshold > carsProduced && seconds > lastProducedTime + 1.0 / intensity) {
            lastProducedTime = seconds
            carsProduced++
            Car(speed = startingSpeed, lastTime = seconds)
        } else null
}