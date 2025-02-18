package me.dvyy.particles.compute

import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.modules.ksl.lang.KslUniformScalar
import me.dvyy.particles.dsl.pairwise.UniformParameter

fun UniformParameter<*>.toKsl(program: KslProgram): KslUniformScalar<*> = program.run {
    when (type) {
        "float" -> uniformFloat1(uniformName)
        else -> error("Unknown parameter")
    }
}
