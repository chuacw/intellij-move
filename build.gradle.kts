import org.jetbrains.grammarkit.tasks.GenerateLexer
import org.jetbrains.grammarkit.tasks.GenerateParser

val intellijVersion = prop("intellijVersion", "2021.2")
val kotlinVersion = "1.5.30"

val pluginJarName = "intellij-move-$intellijVersion"
val pluginGroup = "org.move"
val pluginVersion = "1.0.0"
val pluginTomlVersion = "0.2.151.3997-212"

group = pluginGroup
version = pluginVersion

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.2.1"
    id("org.jetbrains.kotlin.jvm") version "1.5.30"
    id("org.jetbrains.grammarkit") version "2021.1.3"
}

dependencies {
    // kotlin stdlib source code
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")

}

allprojects {
    apply {
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij")
    }

    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }

    intellij {
        pluginName.set(pluginJarName)
        version.set(intellijVersion)
        plugins.set(listOf("org.toml.lang:$pluginTomlVersion"))
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        main {
            java.srcDirs("src/main/gen")
//            kotlin.srcDirs("src/main/kotlin")
//            resources.srcDirs("src/$platformVersion/main/resources")
        }
//        test {
//            kotlin.srcDirs("src/$platformVersion/test/kotlin")
//            resources.srcDirs("src/$platformVersion/test/resources")
//        }
    }

    val generateRustLexer = task<GenerateLexer>("generateMoveLexer") {
        source = "src/main/grammars/MoveLexer.flex"
        targetDir = "src/main/gen/org/move/lang"
        targetClass = "_MoveLexer"
        purgeOldFiles = true
    }

    val generateRustParser = task<GenerateParser>("generateMoveParser") {
        source = "src/main/grammars/MoveParser.bnf"
        targetRoot = "src/main/gen"
        pathToParser = "/org/move/lang/MoveParser.java"
        pathToPsiRoot = "/org/move/lang/psi"
        purgeOldFiles = true
    }

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            dependsOn(generateRustLexer, generateRustParser)
            kotlinOptions {
                jvmTarget = "11"
                languageVersion = "1.5"
                apiVersion = "1.5"
                freeCompilerArgs = listOf("-Xjvm-default=compatibility")
            }
        }

        withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask> {
            jbrVersion.set("11_0_9_1b1202.1")
        }

        withType<org.jetbrains.intellij.tasks.RunIdeTask> {
            jbrVersion.set("11_0_9_1b1202.1")
        }
    }
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String, default: String = ""): String {
    val value = extra.properties.getOrDefault(name, default) as String
    if (value.isEmpty()) {
        error("Property `$name` is not defined in gradle.properties")
    }
    return value
}
