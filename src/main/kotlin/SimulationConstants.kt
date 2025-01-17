import kotlinx.serialization.Serializable
import org.openrndr.draw.persistent
import org.openrndr.events.Event
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import kotlin.math.pow
import kotlin.properties.ReadWriteProperty

@Description("Simulation constants")
@Serializable
object SimulationConstants {
    @DoubleParameter("sigma", 0.0, 100.0, precision = 1)
    var sigma = 2.0

    @DoubleParameter("Particle count log", 2.0, 10.0, precision = 2)
    var targetCountLog = 4.0

    val targetCount get() = 10.0.pow(targetCountLog).toInt()


    @DoubleParameter("Test property", 0.0, 10.0, precision = 2)
    var test by object: ReadWriteProperty<Any?, Double> {
        var value: Double = 0.0
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = value
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Double) {
            this.value = value
        }
    }
    @ActionParameter("Save & Restart")
    fun saveAndRestart() {
        restartEvent.trigger(Unit)
    }

    val restartEvent = Event<Unit>("restart-event")
}

