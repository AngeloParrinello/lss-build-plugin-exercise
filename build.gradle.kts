import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    // No magic: calls a method running behind the scenes the same of id("org.jetbrains.kotlin-$jvm")
    // kotlin, a differenza degli altri plugin, si importa così
    `java-gradle-plugin` // Loads the plugin in the test classpath
    signing // this plugin is used to sign the jars and just run ./gradlew signJar
    `maven-publish` // this plugin is used to publish software on Maven Central
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.dokka") version "1.5.31"
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.publishPlug)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.jacoco)
}

// Configuration of software sources
// il modo fine è dire a gradle dove andare a pigliare la dipendenza
repositories {
    mavenCentral() // points to Maven Central, è come scrivere uri("https:indirizzodimavencentral")
}

//// This task creates a file with a classpath descriptor, to be used by the test task
//val createClasspathManifest by tasks.registering { // this delegate uses the variable name as the task name
//    this.doNotTrackState("classpath") // the state does not track the task
//    val outputDir = file("$buildDir/$name") // we will write in this directory
//    inputs.files(sourceSets.main.get().runtimeClasspath) // the input is the runtime classpath of the main source set
//    // Note: due to the line above, this task implicitly requires out plugin to be compiled
//    outputs.dir(outputDir) // the output is the directory where we will write the classpath file
//    doLast { // this is the task we will execute
//        outputDir.mkdirs() // create the directory infrastructure
//        // Write a file with one classpath entry per line
//        file("$outputDir/plugin-classpath.txt")
//            .writeText(sourceSets.main.get().runtimeClasspath.joinToString("\n"))
//    }
//}

dependencies {
    // "implementation" is a configuration created by by the Kotlin plugin
    implementation(gradleApi()) // imports the Gradle API for creating plugins
    // implementation(gradleKotlinDsl()) // imports the Kotlin DSL API for creating plugins since we are using Kotlin and in this way
    // we can use the extension functions of the Kotlin DSL
     // "implementation" is a configuration created by by the Kotlin plugin
    // implementation(kotlin("stdlib-jdk8")) // "kotlin" is an extension method of DependencyHandler
    // The call to "kotlin" passing `module`, returns a String "org.jetbrains.kotlin:kotlin-$module:<KotlinVersion>"
    testImplementation(gradleTestKit())
    testImplementation("io.kotest:kotest-runner-junit5:5.5.1") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core:5.5.1") // for kotest core assertions
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.1") // for kotest core jvm assertions
    // testRuntimeOnly(files(createClasspathManifest)) // the test task will use the classpath file created by the task above
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.1")
}


// Kotest leverages Junit 5 / Jupiter for execution, we need to enable it
tasks.withType<Test> { // The task type is defined in the Java plugin
    // dependsOn(createClasspathManifest) // this task must be executed before the test task, but we can omit it
    // and include the task as dependency of test runtime only
    useJUnitPlatform() // Use JUnit 5 engine
    testLogging {
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
        events(*org.gradle.api.tasks.testing.logging.TestLogEvent.values())
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

project.group = "io.github.angeloparrinello"


// DSL for the Gradle plugin publish-plugin, here you can define all the properties of the plugin
pluginBundle {
    website = ""
    vcsUrl = ""
    tags = listOf("lss2023", "plugin")
}

gitSemVer {
    buildMetadataSeparator.set("-")
}

// I declare which plugins this project contains (we can add more plugin in a single project!)
// autoconfiguration block for the plugin
gradlePlugin {
    plugins {
        create("lss2023-firstplugin") { // the name of the plugin
            id = "${project.group}.${project.name}"
            displayName = "SPE Greeting plugin"
            description = "Example plugin for the SPE course"
            implementationClass = "it.unibo.firstplugin.GreetingPlugin" // this line here create on-the-fly the
            // information defined in the folder META-INF/gradle-plugins
        }
    }
}

/*
In order to publish on the Gradle Plugin Portal (but it is true for any repository) users need to be authenticated
This is most frequently done via authentication tokens, and more rarely by username and password.

It is first required to register (here: https://plugins.gradle.org/user/register), once done, an API Key will be available from the web interface, along with a secret.

These data is required to be able to publish, and can be fed to Gradle in two ways:

By editing the ~/.gradle/gradle.properties file, adding:

gradle.publish.key=YOUR_KEY
gradle.publish.secret=YOUR_SECRET

Via command line, using -P flags:
./gradlew -Pgradle.publish.key=<key> -Pgradle.publish.secret=<secret> publishPlugins

The command line is the safest way to go, as it does not expose the key and secret in the file system.

there is also a third way. if you have set an environment variable, ORG_GRADLE_gradle.publish.key and ORG_GRADLE_gradle.publish.secret,
you can use them in the command line.
 */

tasks.jacocoTestReport {
    reports {
        // xml.isEnabled = true
        html.required.set(true)
    }
}

// Disable JaCoCo on Windows, see: https://issueexplorer.com/issue/koral--/jacoco-gradle-testkit-plugin/9
tasks.jacocoTestCoverageVerification {
    enabled = !Os.isFamily(Os.FAMILY_WINDOWS)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    config = files(File(projectDir, "config/detekt.yml"))
}

// for publishing the plugin on Maven Central you also need to create the sources and javadoc jars
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc.get().outputDirectory) // Automatically makes it depend on dokkaJavadoc
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("source")
    from(tasks.compileKotlin.get().outputs.files)
    from(tasks.processResources.get().source)
}

// once you've created the archives, you need to sign them in order to publish them on Maven Central. So you will need
// to have a signature key.
// Creation: gpg --gen-key
//List: gpg --list-keys
//Distribution: gpg --keyserver keyserver.ubuntu.com --send-keys <KEY-ID>
// Once you have a key, you can use the signing plugin to have Gradle generate signatures
//
//To set a default signatory, add to your ~/.gradle/gradle.properties:
//
//signing.keyId = <your key id>
//signing.password = <redacted>
//signing.secretKeyRingFile = <your user home>/.gnupg/secring.gpg

// on the Gradle side, there is a plugin that can be used to sign the jars
// the best way is to use the plugin through the cli

signing {
    tasks.withType<Jar> {
        sign(this) // ./gradlew signJar signJavaDocJar signSourcesJar
        // after running this command, you will have the jars signed in the build directory (check the .asc files, and you can
        // verify the signature with gpg --verify <file>.asc <file>)
    }
}

/*
publishing {
    maven {
        url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        val mavenCentralPwd: String? by project // Pass the pwd via -PmavenCentralPwd='yourPassword'
        credentials {
            username = "username
            password = mavenCentralPwd
        }
    }
    publications {
        val publicationName by creating(MavenPublication::class) {
            from(components["java"]) // add the jar produced by the java plugin
            // Warning: the gradle plugin-publish plugin already adds them to the java SoftwareComponent
            artifact(javadocJar) // add the javadoc jar to this publication
            artifact(sourceJar) // add the source jar to this publication
            pom {
                name.set("My Library")
                description.set("A concise description of my library")
                url.set("http://www.example.com/library")
                licenses { license { name.set("...") } }
                developers { developer { name.set("...") } }
                scm {
                    url.set("...")
                    url.set("...")
                }
            }
        }
        signing { sign(publicationName) }
    }
}
 */