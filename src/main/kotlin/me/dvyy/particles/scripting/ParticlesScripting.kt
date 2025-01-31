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
    val cacheBaseDir = Path("build/particles-cache")

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
    }

    val host = BasicJvmScriptingHost(ScriptingHostConfiguration {
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

    fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
        cacheBaseDir.createDirectories()
        return host.eval(scriptFile.toScriptSource(), compilationConfiguration, null)
    }

    fun <T> evalResult(scriptFile: File): T? {
        val res = evalFile(scriptFile)
        res.reports.forEach {
            if (it.severity > ScriptDiagnostic.Severity.DEBUG) {
                println(" : ${it.message}" + if (it.exception == null) "" else ": ${it.exception}")
            }
        }

        return when (val returned = res.valueOrThrow().returnValue) {
            is ResultValue.Error -> {
                println("Error: ${returned.error}")
                null
            }

            is ResultValue.Value -> {
                returned.value as T
            }

            else -> null
        }
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
