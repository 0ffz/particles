package me.dvyy.particles.scripting

import java.io.File
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
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

@KotlinScript(fileExtension = "particles.kts")
abstract class SimpleScript

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Repository(vararg val urls: String)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOn(vararg val urls: String)

class ParticlesScripting {
    fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
        val cacheBaseDir = Path("build/particles-cache").createDirectories()
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScript> {
            defaultImports(
                "me.dvyy.particles.dsl.*",
                "me.dvyy.particles.dsl.potentials.*",
                "org.openrndr.color.*",
                "me.dvyy.particles.scripting.Repository",
                "me.dvyy.particles.scripting.DependsOn",
            )

            jvm {
                jvmTarget.put("21")
                dependenciesFromCurrentContext(wholeClasspath = true)
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
