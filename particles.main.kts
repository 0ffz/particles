@file:Repository("https://repo.mineinabyss.com/snapshots/")
@file:DependsOn("me.dvyy:particles-dsl:0.0.3")

import me.dvyy.particles.dsl.particles
import me.dvyy.particles.dsl.potentials.PairwisePotentials
import org.openrndr.color.ColorRGBa

particles {
    application {
        fullscreen = false
    }
    val hydrogen = particle(
        name = "hydrogen",
        color = ColorRGBa.WHITE,
        radius = 5.0,
    )
    val oxygen = particle(
        name = "oxygen",
        color = ColorRGBa.RED,
        radius = 5.0,
        distribution = 2.0,
    )

    val lennardJones = PairwisePotentials.LennardJones

    interactions {
        allPairs {
            lennardJones {
                it.sigma set config("$pairKey.sigma", default = 5.0)
                it.epsilon set config("$pairKey.epsilon", default = 1.0)
            }
        }
//        (hydrogen - hydrogen) {
//            lennardJones {
//                it.sigma fromConfig "$pairKey.sigma"
//                it.epsilon fromConfig "$pairKey.epsilon"
//            }
//        }
//        (hydrogen - oxygen) {
//            lennardJones {
//                it.sigma fromConfig "$pairKey.sigma"
//                it.epsilon fromConfig "$pairKey.epsilon"
//            }
//        }
//        (oxygen - oxygen) {
//            lennardJones {
//                it.sigma set 5.0
//                it.epsilon set 5.0
//            }
//        }
    }
}
