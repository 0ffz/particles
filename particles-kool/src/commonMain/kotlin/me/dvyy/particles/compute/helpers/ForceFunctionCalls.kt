package me.dvyy.particles.compute.helpers

import de.fabmax.kool.modules.ksl.lang.KslComputeStage
import de.fabmax.kool.modules.ksl.lang.KslFunctionFloat1
import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.modules.ksl.lang.KslScopeBuilder
import me.dvyy.particles.compute.forces.ForcesDefinition


// Prepare function calls for all pairwise interactions
context(scope: KslScopeBuilder, stage: KslComputeStage, program: KslProgram)
internal fun KslScopeBuilder.pairwiseFunctionCalls(
    forcesDef: ForcesDefinition,
    dist: KslFloat,
    localCount: KslFloat,
) = forcesDef.pairwiseInteractions.map { (pair, forces) ->
    val invocations = forces.map {
        val kslFunction = stage.functions[it.function.name] as? KslFunctionFloat1
            ?: error("Function ${it.function.name} not registered")
        kslFunction.invoke(
            dist,
            localCount, //TODO generalize to a preprocess step
            *it.getParameters(scope, program, pair)
        )
    }
    invocations to pair.hash
}
