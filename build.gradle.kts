import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.properties.ReadOnlyProperty


buildscript {

    repositories {
        mavenLocal()
        mavenCentral()
    }

}

plugins {
    base
    kotlin("jvm") version Vers.kotlin apply false
}


/**
 * Project configuration by properties and environment
 */
fun envConfig() = ReadOnlyProperty<Any, String?> { _, property ->
    if (ext.has(property.name)) {
        ext[property.name] as? String
    } else {
        System.getenv(property.name)
    }
}

val repositoryUser by envConfig()
val repositoryPassword by envConfig()
val repositoryUrl by envConfig()


subprojects {

    group = ProjectGroup

    apply {
        plugin("java")
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }


    val sourcesJar by tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        from("src/main/java")
        from("src/main/kotlin")
    }

    tasks {
        withType<JavaCompile> {
            sourceCompatibility = JavaVersion.VERSION_11.toString()
            targetCompatibility = JavaVersion.VERSION_11.toString()
        }
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
        withType<Test> {
            useJUnitPlatform()

            maxParallelForks = 4

            testLogging {
                events(PASSED, FAILED, SKIPPED)
                showStandardStreams = true
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }
}
