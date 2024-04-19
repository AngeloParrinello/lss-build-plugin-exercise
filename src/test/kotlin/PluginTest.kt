import io.kotest.core.spec.style.FreeSpec
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner

class PluginTest : FreeSpec({
    val greetTask = ":greet" // Colon needed!
    fun testSetup(buildFile: () -> String) = projectSetup(buildFile).let { testFolder ->
        GradleRunner.create()
            .withProjectDir(testFolder.root)
            .withPluginClasspath()
            .withArguments(greetTask)
    }
})

fun projectSetup(content: String) = TemporaryFolder().apply{
    create()
    newFile("build.gradle.kts").writeText(content)
}

fun projectSetup(content: () -> String) = projectSetup(content())

//        val manifest: String? = Thread.currentThread().contextClassLoader.getResource("plugin-classpath.txt")?.readText()
//        require (manifest != null) { "Could not load manifest" }
//        // when we need to test sw that creates file, it is a good practice to create a temp directory
//        val tempdir = tempdir()
//        tempdir.mkdirs()
//        val buildGradleKts = with(File(tempdir, "build.gradle.kts")) {
//            writeText(
//                """
//                plugins {
//                    id("it.unibo.lss.greetings")
//                }
//
//                greetings {
//                    greetWith { "Ciao" }
//                }
//
//                """.trimIndent()
//            )
//        }
//        val result = GradleRunner.create()
//            .withPluginClasspath(manifest.lines().map { File(it) }) // we need to pass the classpath to the runner
//            .withProjectDir(tempdir)
//            .withArguments("greet")
//            .build()
//        result.tasks.forEach {
//            it.outcome shouldBe TaskOutcome.SUCCESS
//        }
//        result.tasks.size shouldBe 1
//        result.output shouldContain "Ciao"
//    }
