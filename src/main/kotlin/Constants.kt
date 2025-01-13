import org.openrndr.events.Event
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

val targetCount = 10000

//const val sigma = 2.0
//const val maxForce = 100000.0
const val deltaT = 0.01
const val showForceLines = false

@Description("Simulation constants (requires restart)")
object SimulationConstants {
    @DoubleParameter("sigma", 0.0, 100.0, precision = 1)
    var sigma = 2.0

    @ActionParameter("Save & Restart")
    fun saveAndRestart() {
        restartEvent.trigger(Unit)
    }

    val restartEvent = Event<Unit>("restart-event")
}

@Description("Live simulation settings")
class SimulationSettings {
    @DoubleParameter("epsilon", 0.0, 100.0, precision = 1)
    var epsilon = 1.0
}
