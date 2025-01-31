package me.dvyy.particles

import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Live simulation settings")
object SimulationSettings {
    @DoubleParameter("Max force", 0.0, 10000.0, precision = 0)
    var maxForce = 10000.0

    @DoubleParameter("Max velocity", 0.0, 100.0, precision = 0)
    var maxVelocity = 100.0

    @DoubleParameter("deltaT", 0.0, 0.1, precision = 4)
    var deltaT = 0.001// SimulationConstants.sigma / sqrt(epsilon)

    @DoubleParameter("Starting velocity", 0.0, 10000.0, precision = 0)
    var startingVelocity = 10.0

    var step = 0
}
