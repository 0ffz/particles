package me.dvyy.particles

import kotlinx.serialization.builtins.serializer
import me.dvyy.particles.dsl.pairwise.UniformParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

//@Description("Live simulation settings")
class SimulationSettings {
    val uniforms = listOf(
        UniformParameter.from(String.serializer(), "float", "dT", "simulation.dT", "0.001", precision = 4, range = 0.0..0.1),
        UniformParameter.from(String.serializer(), "float", "maxForce", "simulation.maxForce", "50000.0", range = 0.0..50000.0),
        UniformParameter.from(String.serializer(), "float", "maxVelocity", "simulation.maxVelocity", "100.0", range = 0.0..100.0),
    )
//    uniform("dT", SimulationSettings.deltaT)
//    uniform("maxForce", SimulationSettings.maxForce)
//    uniform("maxVelocity", SimulationSettings.maxVelocity)
//    @DoubleParameter("Max force", 0.0, 10000.0, precision = 0)
//    var maxForce = 10000.0
//
//    @DoubleParameter("Max velocity", 0.0, 100.0, precision = 0)
//    var maxVelocity = 100.0
//
//    @DoubleParameter("deltaT", 0.0, 0.1, precision = 4)
//    var deltaT = 0.001// SimulationConstants.sigma / sqrt(epsilon)
//
//    @DoubleParameter("Starting velocity", 0.0, 10000.0, precision = 0)
//    var startingVelocity = 10.0
//
//    var step = 0
}
