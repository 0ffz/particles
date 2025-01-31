package me.dvyy.particles.scripting

import java.io.File
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

// The KotlinScript annotation marks a class that can serve as a reference to the script definition for
// `createJvmCompilationConfigurationFromTemplate` call as well as for the discovery mechanism
// The marked class also become the base class for defined script type (unless redefined in the configuration)
@KotlinScript(
    // file name extension by which this script type is recognized by mechanisms built into scripting compiler plugin
    // and IDE support, it is recommendend to use double extension with the last one being "kts", so some non-specific
    // scripting support could be used, e.g. in IDE, if the specific support is not installed.
    fileExtension = "particles.kts"
)
// the class is used as the script base class, therefore it should be open or abstract
abstract class SimpleScript

class ParticlesScripting {
    fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {

        val cacheBaseDir =
            Path("build/particles-cache").createDirectories()//createTempDirectory("particles-kts-cache")
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScript> {
            defaultImports(
                "me.dvyy.particles.dsl.*",
                "me.dvyy.particles.potentials.*",
                "org.openrndr.color.*",
            )

            jvm {

                jvmTarget.put("21")
                dependenciesFromCurrentContext(wholeClasspath = true)
//                dependenciesFromCurrentContext(
//                    "script" /* script library jar name (exact or without a version) */
//                )
            }
            hostConfiguration(ScriptingHostConfiguration {
                jvm {
                    compilationCache(
                        CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
                            cacheBaseDir.resolve(
                                compiledScriptUniqueName(script, scriptCompilationConfiguration) + ".jar"
                            ).toFile()
                        }
                    )
                }
            })
        }
        return BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), compilationConfiguration, null)
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun compiledScriptUniqueName(
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
): String {
    val digestWrapper = MessageDigest.getInstance("MD5")
    digestWrapper.update(script.text.toByteArray())
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
    return digestWrapper.digest().toHexString()
}
