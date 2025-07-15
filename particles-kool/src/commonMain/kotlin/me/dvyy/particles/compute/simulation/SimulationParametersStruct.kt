package me.dvyy.particles.compute.simulation

import de.fabmax.kool.util.MemoryLayout
import de.fabmax.kool.util.Struct

class SimulationParametersStruct : Struct("SimulationParametersStruct", MemoryLayout.Std140) {
    val maxVelocity = float1()
    val maxForce = float1()
}
