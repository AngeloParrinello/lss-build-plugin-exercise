package it.unibo.firstplugin

import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class GreetingTask : DefaultTask() {

    // ideally, all the properties should be lazy, so they are not executed until the task is executed
    // so, all the properties will be not "normal" (i.e. String, but Property<String>) because all the configuration
    // in Gradle should be lazy, especially those that configure the task

    @Input // Property that is used as input for the task, Input is a property of string
    // How to create Property? (Through the map you can create a Provider) you have some
    // constructors that you can use to create a property
    // if you want to modify the metamodel of Gradle, you have to use the named-object,
    // but it is quite advanced
    val greeting: Property<String> = project.objects.property<String>(String::class.java) // Lazy property creation

    @Internal // Read-only property calculated from `greeting`
    // map to greeting plus Gradle string
    val message: Provider<String> = greeting.map { "$it Gradle" }

    @TaskAction // and as a task action...
    fun printMessage() {
        // "logger" is a property of DefaultTask
        // it is all lazy, so it is not executed until the task is executed
        logger.quiet(message.get())
    }
}

// entry point of the dsl, the extension is a class that must be open
// the stuff defined inside the class is our dsl
// this is the part the user will interact with (the section in the build.gradle.kts)
open class GreetingExtension(val project: Project) {
    val defaultGreeting: Property<String> = project.objects.property(String::class.java)
        .apply { convention("Hello from") } // Set a conventional value, default value

    // A DSL would go there
    fun greetWith(greeting: () -> String) = defaultGreeting.set(greeting())

}

// This is the glue between the extension and the task
open class GreetingPlugin : Plugin<Project> {
    // this happens at configuration time
    override fun apply(target: Project) {
        // we can also react at the execution of other plugins
        // target.plugins.withType(JavaPlugin::class.java) {
            // we can do something when the Java plugin is applied
        // }
        // Create the extension
        val extension = target.extensions.create("greetings", GreetingExtension::class.java, target)
        // Create the task
        target.tasks.register("greet", GreetingTask::class.java).get().run {
            // Set the default greeting to be the one configured in the extension
            greeting.set(extension.defaultGreeting)
            // Configuration per-task can still be changed manually by users
        }
    }
}