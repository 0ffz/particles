//package math
//
//import epsilon
//import org.openrndr.math.Vector2
//import kotlin.math.pow
//
//inline fun lennardJonesPotential(r: Double): Double {
//    return 4 * epsilon * ((sigma / r).pow(12) - (sigma / r).pow(6))
//}
//
///** Derivative of [lennardJonesPotential] */
//fun lennardJonesForce(r: Double): Double {
//    val r6 = (sigma / r).pow(6)
//    val r12 = r6 * r6
//    return (24 * epsilon * ((2 * r12) - r6)) / r
//}
//
//inline fun calculateNetForce(particle: Vector2, vararg nearbyCells: Collection<Vector2>?): Vector2 {
//    var netForce = Vector2.ZERO
//    for (cell in nearbyCells) {
//        for (other in cell ?: continue) {
//            if (other === particle) continue
//            val direction = particle - other
//            val distance = direction.length
//            if (distance > 2.5 * sigma) continue
//            val forceMagnitude = lennardJonesForce(distance)//.coerceAtMost(maxForce)
//            netForce += direction.normalized * forceMagnitude
//        }
//    }
//    return netForce
//}
//
//const val maxSpeedSqr = 1.0
//inline fun newPosition(position: Vector2, prev: Vector2, netForce: Vector2, deltaT: Double): Vector2 {
////    val deltaP = (position - prev)//.mod(dimensions)
////    val appliedForce = (netForce * deltaT.pow(2))
////    val speed = (deltaP + appliedForce)//.let { if(it.squaredLength > maxSpeedSqr) it.normalized * maxSpeedSqr else it }
////    val newPosition = position + speed
////    return newPosition
//    return (position * 2.0) - prev + (netForce * deltaT.pow(2.0))
//}
