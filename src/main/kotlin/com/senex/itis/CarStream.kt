package com.senex.itis

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.color.presets.BROWN

data class Car(
    val debug: Boolean = false,
    var x: Double = 0.0,
    var y: Double = 50.0,
    var onRoadside: Boolean = false,
    private val maxSpeed: Double = 100.0,
    private val acceleration: Double = 50.0,
    private val braking: Double = 80.0,
    private var speed: Double = 0.0,
    private var lastTime: Double = 0.0
) {
    fun move(distanceToFrontCar: Double, isTrafficLightGreen: Boolean, isRoadsideAvailable: Boolean, seconds: Double) {
        val deltaTime = seconds - lastTime
        lastTime = seconds

        val movementType = if (isTrafficLightGreen) MovementType.ACCELERATE else MovementType.BRAKE

        when (movementType) {
            MovementType.ACCELERATE -> accelerate(deltaTime)
            MovementType.BRAKE -> brake(deltaTime)
        }
    }

    private fun moveInternal(seconds: Double, movementType: MovementType) {
        val deltaTime = seconds - lastTime
        lastTime = seconds

        when (movementType) {
            MovementType.ACCELERATE -> accelerate(deltaTime)
            MovementType.BRAKE -> brake(deltaTime)
        }
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
        x += speed * deltaTime
    }

    enum class MovementType {
        ACCELERATE,
        BRAKE,
    }
}

class TrafficLight(
    private val lightChangeDelay: Double = 5.0,
) {
    private var lastUpdateTime: Double = -1.0
    private var state: Boolean = true

    fun tryUpdate(seconds: Double): Boolean {
        if (seconds > lastUpdateTime + lightChangeDelay) {
            lastUpdateTime = seconds
            state = !state
        }
        return state
    }
}

class CarStreamHandler(
    val distanceToBridge: Double = 600.0,
    val bridgeLength: Double = 100.0,
    val distanceFromBridgeToTrafficLight: Double = 100.0,
    private val carStreamProducer: CarStreamProducer,
    private val trafficLight: TrafficLight,
) {
    private val cars = mutableListOf<Car>()

    fun update(seconds: Double): State {
        carStreamProducer.tryProduce(seconds)?.let { cars.add(it) }

        val isTrafficLightGreen = trafficLight.tryUpdate(seconds)

        cars.forEach { car ->
            car.move(
                distanceToFrontCar = checkDistanceToFrontCar(car),
                isTrafficLightGreen = isTrafficLightGreen,
                isRoadsideAvailable = checkRoadsideAvailable(car),
                seconds = seconds
            )
        }

        return State(carCoordinates = cars.map { it.x to it.y }, isTrafficLightGreen = isTrafficLightGreen)
    }

    private fun checkDistanceToFrontCar(car: Car): Double =
        cars.filter { other -> car.x < other.x && car.y == other.y }.minOfOrNull { it.x } ?: Double.MAX_VALUE

    private fun checkRoadsideAvailable(car: Car): Boolean = car.x < distanceToBridge

    data class State(
        val carCoordinates: List<Pair<Double, Double>>,
        val isTrafficLightGreen: Boolean,
    )
}

fun main() = application {
    configure {
        width = 1000
        height = 600
    }

    val carStreamProducer = CarStreamProducer()
    val carStreamHandler = CarStreamHandler(carStreamProducer = carStreamProducer, trafficLight = TrafficLight())

    program {
        extend {
            drawer.clear(ColorRGBa.PINK)

            val (coordinates, isTrafficLightGreen) = carStreamHandler.update(seconds)

            carStreamHandler.drawEnvironment(drawer, isTrafficLightGreen)
            coordinates.forEach { (x, y) ->
                drawer.stroke = ColorRGBa.BLACK
                drawer.fill = ColorRGBa.RED
                drawer.rectangle(x = x, y = y, width = 60.0, height = 30.0)
            }
        }
    }
}

fun CarStreamHandler.drawEnvironment(drawer: Drawer, isTrafficLightGreen: Boolean) {
    drawer.stroke = ColorRGBa.BROWN
    drawer.lineSegment(x0 = 0.0, y0 = 90.0, x1 = distanceToBridge, y1 = 90.0)
    drawer.stroke = ColorRGBa.BLUE
    drawer.lineSegment(
        x0 = distanceToBridge,
        y0 = 90.0,
        x1 = distanceToBridge + bridgeLength,
        y1 = 90.0
    ) // 20 meters in length somehow
    drawer.stroke = if(isTrafficLightGreen) ColorRGBa.GREEN else ColorRGBa.RED
    drawer.lineSegment(
        x0 = distanceToBridge + bridgeLength + distanceFromBridgeToTrafficLight,
        y0 = 40.0,
        x1 = distanceToBridge + bridgeLength + distanceFromBridgeToTrafficLight,
        y1 = 90.0
    )
}


