object Vers {

    //Kotlin dependencies
    const val kotlin = "1.5.30"
    const val kotlinx_coroutines = "1.5.2"

    //jmh
    const val jmh = "1.33"
    const val jmh_gradle = "0.6.6"
}

object Plugins {

    //Gradle plugins
    const val jmh_gradle = "me.champeau.jmh"
}

object Libs {

    //Kotlin dependencies
    const val kotlin_bom = "org.jetbrains.kotlin:kotlin-bom:${Vers.kotlin}"
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib"
    const val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
    const val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect"
    const val kotlinx_coroutines_bom = "org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Vers.kotlinx_coroutines}"
    const val kotlinx_coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core"
    const val kotlinx_coroutines_jdk8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8"
    const val kotlinx_coroutines_reactor = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor"

    const val jmh_core = "org.openjdk.jmh:jmh-core:${Vers.jmh}"
    const val jmh_annprocess = "org.openjdk.jmh:jmh-generator-annprocess:${Vers.jmh}"
    const val jmh_bytecode = "org.openjdk.jmh:jmh-generator-bytecode:${Vers.jmh}"
}


const val ProjectGroup = "com.example.coroutines"
