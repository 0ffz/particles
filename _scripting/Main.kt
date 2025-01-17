import dsl.particles
import org.openrndr.color.ColorRGBa


fun main() = particles {
    val hydrogen = particle(
        color = ColorRGBa.WHITE,
        radius = 1.0,
    )
    val oxygen = particle(
        color = ColorRGBa.RED,
        radius = 1.0,
    )

    val lennardJones = PairwisePotentials.lennardJones()

    (hydrogen to hydrogen) interaction {
        uses(lennardJones)
    }
    (hydrogen to oxygen) interaction {
        uses(lennardJones)
    }
}
