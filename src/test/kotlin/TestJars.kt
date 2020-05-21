
import crane.buildBridge
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestJars {

    @Test
    fun test() {
        val original = getResource("testV1.jar")
        val patch = getResource("testV2.jar")
        val dest = patch.parent.resolve("destSrc")
        buildBridge(original, patch, dest,"v2")
//        assert(result.success())
//        debugResultJar(dest)
    }

}

