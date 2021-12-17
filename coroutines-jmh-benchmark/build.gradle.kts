plugins {
    kotlin("jvm")
    id(Plugins.jmh_gradle) version Vers.jmh_gradle
}

jmh {
    warmupIterations.set(2)
    fork.set(1)
    threads.set(8)
    benchmarkMode.set(listOf("thrpt"))

    includes.set(listOf(".*CreateDetachedTaskPayloadJmh.*"))
}

dependencies {
    //kotlin
    implementation(platform(Libs.kotlin_bom))
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_jdk8)
    implementation(Libs.kotlin_reflect)
    implementation(platform(Libs.kotlinx_coroutines_bom))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_jdk8)
    implementation(Libs.kotlinx_coroutines_reactor)


    //jmh
    implementation(Libs.jmh_core)
    implementation(Libs.jmh_annprocess)
    implementation(Libs.jmh_bytecode)
}
