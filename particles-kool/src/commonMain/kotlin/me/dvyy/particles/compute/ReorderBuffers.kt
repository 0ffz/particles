import de.fabmax.kool.modules.ksl.KslComputeShader
import de.fabmax.kool.modules.ksl.lang.*
import me.dvyy.particles.compute.WORK_GROUP_SIZE

/**
 * Calculates
 */
class ReorderBuffersShader() {
    val shader = KslComputeShader("Reorder buffers") {
        computeStage(WORK_GROUP_SIZE) {
            val indices = storage1d<KslInt1>("indices")
            val positions = storage1d<KslFloat4>("positions")
            val velocities = storage1d<KslFloat4>("velocities")
            val numValues = uniformInt1("numValues")

            main {
                val id = int1Var(inGlobalInvocationId.x.toInt1())
                val swapIndex = int1Var(indices[id])
                `if`((id lt swapIndex) and (id lt numValues)) {
                    val tempPosition = float4Var(positions[id])
                    val tempVelocity = float4Var(velocities[id])
                    positions[id] = positions[swapIndex]
                    positions[swapIndex] = tempPosition
                    velocities[id] = velocities[swapIndex]
                    velocities[swapIndex] = tempVelocity
                }
            }
        }
    }

    var indices by shader.storage1d("indices")
    var positions by shader.storage1d("positions")
    var velocities by shader.storage1d("velocities")
    var numValues by shader.uniform1i("numValues")
}
