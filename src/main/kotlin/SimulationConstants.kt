import org.openrndr.events.Event
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import kotlin.math.pow

@Description("Simulation constants")
object SimulationConstants {
    @DoubleParameter("sigma", 0.0, 100.0, precision = 1)
    var sigma = 2.0

    @DoubleParameter("Particle count log", 2.0, 10.0, precision = 2)
    var targetCountLog = 4.0

    val targetCount get() = 10.0.pow(targetCountLog).toInt()


    @ActionParameter("Save & Restart")
    fun saveAndRestart() {
        restartEvent.trigger(Unit)
    }

    val restartEvent = Event<Unit>("restart-event")
}

