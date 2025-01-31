package me.dvyy.particles

import me.dvyy.particles.dsl.ParticlesDSL
import me.dvyy.particles.scripting.ParticlesScripting
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrThrow

fun main() {
    println("Compiling particles script...")
    val res = ParticlesScripting().evalFile(File("particles.kts"))
    res.reports.forEach {
        if (it.severity > ScriptDiagnostic.Severity.DEBUG) {
            println(" : ${it.message}" + if (it.exception == null) "" else ": ${it.exception}")
        }
    }

    when (val returned = res.valueOrThrow().returnValue) {
        is ResultValue.Error -> {
            println("Error: ${returned.error}")
            return
        }

        is ResultValue.Value -> {
            (returned.value as ParticlesDSL).start()
        }

        ResultValue.NotEvaluated -> {}
        is ResultValue.Unit -> {}
    }
}
