package com.senex.itis

class CarStreamProducer(
    private val intensity: Double = 0.5,
) {
    private var lastProducedTime: Double = -1.0

    fun tryProduce(seconds: Double): Car? =
        if (seconds > lastProducedTime + 1.0 / intensity) {
            lastProducedTime = seconds
            Car(lastTime = seconds)
        } else null
}