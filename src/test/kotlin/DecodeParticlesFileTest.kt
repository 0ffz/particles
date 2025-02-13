import com.charleskorn.kaml.Yaml
import me.dvyy.particles.dsl.ParticlesConfig
import org.junit.Test
import kotlin.io.path.Path
import kotlin.io.path.readText

class DecodeParticlesFileTest {
    @Test
    fun `should correctly decode particles file`() {
        val config = Yaml.default.decodeFromString(ParticlesConfig.serializer(), Path("particles.yml").readText())
        config
    }
}
