package me.dvyy.particles.helpers.kool

import org.junit.jupiter.api.extension.RegisterExtension

abstract class KoolTest {
    @RegisterExtension
    val extension = KoolTestExtension
    val ctx get() = extension.ctx!!
    val scene get() = extension.scene!!
}
