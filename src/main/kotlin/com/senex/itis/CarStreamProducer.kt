package com.senex.itis

class CarStreamProducer(
    private val intensity: Double,
    private val carThreshold: Int = 1,
) {
    private var lastProducedTime: Double = -100.0
    private var carsProduced = 0

    fun tryProduce(seconds: Double): Car? =
        if (carThreshold > carsProduced && seconds > lastProducedTime + 1.0 / intensity) {
            lastProducedTime = seconds
            carsProduced++
            Car(lastTime = seconds)
        } else null
}