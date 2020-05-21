import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the

class SetupTesting : Plugin<Project> {
    override fun apply(project: Project) {
        project.setupTesting()
    }
}

private fun Project.createTestSourceSet(name: String): Pair<Jar, SourceSet> {
    val sourceSet = sourceSets.create(name)
    return tasks.create(name, Jar::class.java) {
        group = "testing"
        from(sourceSet.output)

        destinationDirectory.set(sourceSets["test"].resources.srcDirs.first())
        archiveFileName.set("$name.jar")
    } to sourceSet
}

fun Project.setupTesting() {
    val (v1Jar, v1Set) = createTestSourceSet("testV1")
    val (v2Jar, v2Set) = createTestSourceSet("testV2")
    val (_, mcV1Set) = createTestSourceSet("testMcV1")
    val (_, mcV2Set) = createTestSourceSet("testMcV2")

    v1Set.compileClasspath += mcV1Set.output
    v2Set.compileClasspath += mcV2Set.output

    tasks.named("processTestResources") {
        dependsOn(v1Jar, v2Jar)
    }
}

val Project.sourceSets: SourceSetContainer
    get() = the<JavaPluginConvention>().sourceSets