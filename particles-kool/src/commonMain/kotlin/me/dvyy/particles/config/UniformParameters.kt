package me.dvyy.particles.config

//class UniformParameters(
//    val configRepo: ConfigRepository,
//    val overrides: ParameterOverrides,
//    val scope: CoroutineScope,
//    val forces: ForcesDefinition,
//) {
//    val uniformParams = combine(configRepo.config, overrides.overrides) { config, params ->
//        forces.pairwiseInteractions.flatMap { (pair, interactions) ->
//            interactions.flatMap { interaction ->
//                interaction.uniforms.map { (param, config) ->
//                    UniformParameter<Float>(
//                        param.name,
//                        config.path,
//                        param.uniformNameFor(pair),
//                        (params[config.path] ?: config.default).decode(Float.serializer()),
//                        range = config.min..config.max,
//                    )
//                }
//            }
//        }
//    }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())
//}
