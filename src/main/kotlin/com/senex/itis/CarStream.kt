package com.senex.itis

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.color.presets.BROWN
import kotlin.math.roundToInt

/*
    TODO:
        - Точка фиксации потока на выходе
        - Минимальная дистанция между нарушителями: Dнар
        - Скорость образования скопления: Vскоп
        - Оцените расстояние, которое должен проехать автомобиль до полной остановки при фиксированной начальной скорости и постоянном ускорении торможения.
        - Рассчитайте протяженность скопления автомобилей и интенсивность потока через M километров после светофора в зависимости от интенсивности потока, формирующегося в N километрах перед светофором
 */

object CarStreamSettings {

    const val debug: Boolean = true

    object Car {

        const val length: Double = 12.0 // m
        const val width: Double = 6.0 // m
        val startingSpeed: Double = 50.0.kilometersToMeters().hoursToSeconds() // km/h
        val maxSpeed: Double = 180.0.kilometersToMeters().hoursToSeconds() // km/h
        val roadsideMaxSpeed: Double = 60.0.kilometersToMeters().hoursToSeconds() // km/h
        const val acceleration: Double = 10.0 // m/s^2
        const val braking: Double = 20.0 // m/s^2
        const val emergencyBraking: Double = 30.0 // m/s^2
        const val minDistanceToFrontObject: Double = 30.0 // m
        const val canUseRoadside: Boolean = true
        val roadsideSwitchSpeedThreshold: Double = 5.0.kilometersToMeters().hoursToSeconds() // km/h
    }

    object Road {

        val distanceToBridge: Double = 1.0.kilometersToMeters() // km
        val bridgeLength: Double = 0.02.kilometersToMeters() // km (20m forced)
        val distanceFromBridgeToTrafficLight: Double = 0.2.kilometersToMeters() // km
    }

    object TrafficLight {

        const val greenLightDuration: Double = 3.0 // sec
        const val redLightDuration: Double = 7.0 // sec
    }

    object Producer {

        const val intensity: Double = 1.0 // cars/sec
    }
}

fun Double.kilometersToMeters(): Double = this * 1000

fun Double.hoursToSeconds(): Double = this / 60 / 60

class TrafficLight(
    private val greenLightDuration: Double = 3.0,
    private val redLightDuration: Double = 7.0,
) {
    private var lastUpdateTime: Double = -1.0
    private var state: Boolean = true

    fun tryUpdate(seconds: Double): Boolean {
        val duration = if (state) greenLightDuration else redLightDuration
        if (seconds > lastUpdateTime + duration) {
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
        if (cars.isRoadFree()) {
            carStreamProducer.tryProduce(seconds)?.let { cars.add(it) }
        }

        val isTrafficLightGreen = trafficLight.tryUpdate(seconds)

        cars.forEach { car ->
            car.move(
                distanceToFrontCar = checkDistanceToFrontCar(car),
                distanceToTrafficLight = distanceToBridge + bridgeLength + distanceFromBridgeToTrafficLight - car.x - car.length,
                distanceToRoadsideEnd = distanceToBridge - car.x - car.length,
                isTrafficLightGreen = isTrafficLightGreen,
                isRoadsideAvailable = car.x + car.length < distanceToBridge
                        && checkLineAvailable(car = car, lineType = Car.LineType.ROADSIDE),
                isRoadAvailable = checkLineAvailable(car = car, lineType = Car.LineType.ROAD),
                seconds = seconds
            )
        }

        return State(cars = cars, isTrafficLightGreen = isTrafficLightGreen)
    }

    private fun List<Car>.isRoadFree() = none { it.x < it.length + 20.0 }

    private fun checkDistanceToFrontCar(car: Car): Double =
        cars.filter { other -> car.x < other.x && car.currentLine == other.currentLine }
            .minOfOrNull { it.x }
            ?.let { it - car.x - car.length }
            ?: Double.POSITIVE_INFINITY

    private fun checkLineAvailable(car: Car, lineType: Car.LineType): Boolean =
        cars.filter { it.currentLine == lineType }
            .none { other -> other.x in (car.x - car.length - 10.0)..(car.x + car.length + 10.0) }

    data class State(
        val cars: List<Car>,
        val isTrafficLightGreen: Boolean,
    )
}

fun main() = application {
    configure {
        width = 1400
        height = 600
    }

    val carStreamHandler = CarStreamHandler(
        distanceToBridge = CarStreamSettings.Road.distanceToBridge,
        bridgeLength = CarStreamSettings.Road.bridgeLength,
        distanceFromBridgeToTrafficLight = CarStreamSettings.Road.distanceFromBridgeToTrafficLight,
        carStreamProducer = CarStreamProducer(
            startingSpeed = CarStreamSettings.Car.startingSpeed,
            intensity = CarStreamSettings.Producer.intensity
        ),
        trafficLight = TrafficLight(
            greenLightDuration = CarStreamSettings.TrafficLight.greenLightDuration,
            redLightDuration = CarStreamSettings.TrafficLight.redLightDuration,
        ),
    )

    program {
        extend {
            drawer.clear(ColorRGBa.PINK)

            val (cars, isTrafficLightGreen) = carStreamHandler.update(seconds)

            carStreamHandler.drawEnvironment(drawer, isTrafficLightGreen)
            cars.forEach { car ->
                drawer.stroke = ColorRGBa.BLACK
                drawer.strokeWeight = 1.0
                drawer.fill = ColorRGBa.RED
                drawer.rectangle(
                    x = car.x,
                    y = car.currentLine.y,
                    width = CarStreamSettings.Car.length,
                    height = CarStreamSettings.Car.width
                )

                if (CarStreamSettings.debug)
                    drawer.text(car.speed.roundToInt().toString(), car.x, car.currentLine.y)
            }
        }
    }
}

fun CarStreamHandler.drawEnvironment(drawer: Drawer, isTrafficLightGreen: Boolean) {
    drawer.stroke = ColorRGBa.BROWN
    drawer.lineSegment(x0 = 0.0, y0 = 60.0, x1 = distanceToBridge, y1 = 60.0)
    drawer.stroke = ColorRGBa.BLUE
    drawer.lineSegment(
        x0 = distanceToBridge,
        y0 = 60.0,
        x1 = distanceToBridge + bridgeLength,
        y1 = 60.0
    )
    drawer.stroke = if (isTrafficLightGreen) ColorRGBa.GREEN else ColorRGBa.RED
    drawer.strokeWeight = 2.0
    drawer.lineSegment(
        x0 = distanceToBridge + bridgeLength + distanceFromBridgeToTrafficLight,
        y0 = 40.0,
        x1 = distanceToBridge + bridgeLength + distanceFromBridgeToTrafficLight,
        y1 = 60.0
    )
}


