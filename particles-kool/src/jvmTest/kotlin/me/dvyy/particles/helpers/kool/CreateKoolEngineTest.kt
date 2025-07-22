package me.dvyy.particles.helpers.kool

import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

@Order(0)
class CreateKoolEngineTest: KoolTest() {
    @Test
    fun `should create engine successfully`() {
        assertNotNull(ctx)
        assertNotNull(scene)
    }
}
