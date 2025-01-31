package me.dvyy.particles

import org.openrndr.events.Event
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import kotlin.math.pow

@Description("Simulation constants")
object SimulationConstants {
    @DoubleParameter("Min grid size", 0.0, 50.0, precision = 2)
    var minGridSize = 5.0

    @DoubleParameter("Particle count log", 2.0, 10.0, precision = 2)
    var targetCountLog = 4.0

    val targetCount get() = 10.0.pow(targetCountLog).toInt()

    //    @DoubleParameter("Test property", 0.0, 10.0, precision = 2)
//    var test by object: ReadWriteProperty<Any?, Double> {
//        var value: Double = 0.0
//        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = value
//        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Double) {
//            this.value = value
//        }
//    }
    @ActionParameter("Reset positions")
    fun resetPositions() {
        resetPositionsEvent.trigger(Unit)
    }

    @ActionParameter("Recompile & Restart")
    fun recompileAndRestart() {
        restartEvent.trigger(Unit)
    }

    val restartEvent = Event<Unit>("restart-event")
    val resetPositionsEvent = Event<Unit>("reset-positions-event")
}

