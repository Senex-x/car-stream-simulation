package com.senex.itis

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import kotlin.math.roundToInt

class CarTicker(
    val debug: Boolean = false,
    var x: Double = 0.0,
    var y: Double = 50.0,
    private val maxSpeed: Double = 100.0,
    private val acceleration: Double = 50.0,
    private val braking: Double = 80.0,
    private var speed: Double = 0.0,
    private var lastTime: Double = 0.0
) {
    fun move(seconds: Double, movementType: MovementType) {
        //if(debug) println("X $x")

        val deltaTime = seconds - lastTime
        lastTime = seconds

        when (movementType) {
            MovementType.ACCELERATE -> accelerate(deltaTime)
            MovementType.BRAKE -> brake(deltaTime)
        }
        //if(debug) println("Speed 2 $speed")
    }

    private fun accelerate(deltaTime: Double) {
        val currentAcceleration = acceleration * deltaTime

        if (speed < maxSpeed - currentAcceleration) {
            speed += currentAcceleration
        } else {
            speed = maxSpeed
        }
        x += speed * deltaTime
    }

    private fun brake(deltaTime: Double) { // Untested
        if (speed > braking * deltaTime) {
            speed -= braking * deltaTime
        } else {
            speed = 0.0
        }
        x -= speed * deltaTime
    }

    enum class MovementType {
        ACCELERATE,
        BRAKE,
    }
}

fun main() = application {
    configure {
        width = 800
        height = 600
    }

    val cars = mutableListOf(CarTicker())

    val ticker = Ticker(
        update = { seconds, drawer ->
            cars.forEach { car ->
                car.move(seconds, CarTicker.MovementType.ACCELERATE)
                drawer.fill = ColorRGBa.RED
                drawer.rectangle(car.x, car.y, 60.0, 30.0)
            }
        }, { drawer ->
            cars.forEach { car ->
                drawer.fill = ColorRGBa.RED
                drawer.rectangle(car.x, car.y, 60.0, 30.0)
            }
        }
    )

    program {
        extend {
            if (seconds.roundToInt() == 2) cars.add(CarTicker(debug = true, lastTime = seconds))
            //if (seconds.roundToInt() == 4) cars.add(Car(lastTime = seconds))
            //if (seconds.roundToInt() == 6) cars.add(Car(lastTime = seconds))

            ticker.tryUpdate(seconds, drawer)
        }
    }
}

class Ticker(
    private val update: (Double, Drawer) -> Unit,
    private val preserve: (Drawer) -> Unit,
) {
    private var lastSecond: Int = 0

    fun tryUpdate(seconds: Double, drawer: Drawer) {
        if (lastSecond != seconds.roundToInt()) {
            lastSecond = seconds.roundToInt()

            update(lastSecond.toDouble(), drawer)
        } else {
            preserve(drawer)
        }
    }
}
