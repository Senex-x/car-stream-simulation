package com.senex.itis

data class Car(
    val debug: Boolean = false,
    var x: Double = 0.0,
    var currentLine: LineType = LineType.ROAD,
    val length: Double = CarStreamSettings.Car.length,
    private val maxSpeed: Double = CarStreamSettings.Car.maxSpeed,
    private val roadsideMaxSpeed: Double = CarStreamSettings.Car.roadsideMaxSpeed,
    private val acceleration: Double = CarStreamSettings.Car.acceleration,
    private val braking: Double = CarStreamSettings.Car.braking,
    private val emergencyBraking: Double = CarStreamSettings.Car.emergencyBraking,
    private val minDistanceToFrontObject: Double = CarStreamSettings.Car.minDistanceToFrontObject,
    private val lineChangeTimeThreshold: Double = 5.0, // sec
    private val canUseRoadside: Boolean = CarStreamSettings.Car.canUseRoadside,
    private val roadsideSwitchSpeedThreshold: Double = CarStreamSettings.Car.roadsideSwitchSpeedThreshold,
    var speed: Double = 0.0,
    private var lastTime: Double = 0.0,
) {
    private val brakingDistance: Double
        get() = speed * 2.5 / 2 // 2.5 seconds to brake (S = V0 * t / 2)
    private var lastLineChangeTime: Double = -1.0
    private var lastLineChangePosition: Double = -1.0

    fun move(
        distanceToFrontCar: Double,
        distanceToTrafficLight: Double,
        distanceToRoadsideEnd: Double,
        isTrafficLightGreen: Boolean,
        isRoadsideAvailable: Boolean,
        isRoadAvailable: Boolean,
        seconds: Double
    ) {
        val deltaTime = seconds - lastTime
        lastTime = seconds

        val speedThreshold = if (currentLine == LineType.ROAD) maxSpeed else roadsideMaxSpeed
        val hasCarClose = !distanceToFrontCar.isEnoughDistance()
        val hasRoadsideEndClose = currentLine == LineType.ROADSIDE && !distanceToRoadsideEnd.isEnoughDistance()
        val shouldIgnoreTrafficLight = distanceToTrafficLight.isEnoughDistance()
                || distanceToTrafficLight < 0
                || (distanceToTrafficLight < minDistanceToFrontObject && speed > maxSpeed * 0.8)

        val shouldAccelerate = speed < speedThreshold
                && !hasCarClose
                && !hasRoadsideEndClose
                && (shouldIgnoreTrafficLight || isTrafficLightGreen)

        val shouldUseEmergencyBraking = !shouldAccelerate
                && (distanceToFrontCar < minDistanceToFrontObject || distanceToRoadsideEnd < minDistanceToFrontObject)

        if (canUseRoadside) {
            changeLineIfNeeded(
                isRoadsideAvailable = isRoadsideAvailable,
                isRoadAvailable = isRoadAvailable,
                isTrafficLightGreen = isTrafficLightGreen,
                hasCarClose = hasCarClose,
                seconds = seconds,
            )
        }

        if (shouldAccelerate) accelerate(deltaTime) else brake(deltaTime, shouldUseEmergencyBraking)
    }

    private fun Double.isEnoughDistance(): Boolean = this > minDistanceToFrontObject + brakingDistance

    private fun changeLineIfNeeded(
        isRoadsideAvailable: Boolean,
        isRoadAvailable: Boolean,
        isTrafficLightGreen: Boolean,
        hasCarClose: Boolean,
        seconds: Double
    ) {
        val canSwitchToRoadside = hasCarClose
                && currentLine == LineType.ROAD
                && speed < roadsideSwitchSpeedThreshold
                && !isTrafficLightGreen
                && isRoadsideAvailable
        val canSwitchToRoad = isRoadAvailable
                && currentLine == LineType.ROADSIDE

        if (seconds - lastLineChangeTime > lineChangeTimeThreshold
            && lastLineChangePosition != x
            && (canSwitchToRoad || canSwitchToRoadside)
        ) {
            lastLineChangeTime = seconds
            lastLineChangePosition = x
            currentLine = if (currentLine == LineType.ROAD) LineType.ROADSIDE else LineType.ROAD
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

    private fun brake(deltaTime: Double, useEmergencyBraking: Boolean) {
        val brakingValue = if (useEmergencyBraking) emergencyBraking else braking
        val currentBraking = brakingValue * deltaTime
        if (speed > currentBraking) {
            speed -= currentBraking
        } else {
            speed = 0.0
        }
        x += speed * deltaTime
    }

    enum class LineType(val y: Double) {
        ROAD(45.0),
        ROADSIDE(75.0),
    }
}