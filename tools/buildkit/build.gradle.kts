import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  `kotlin-dsl`
}

// use overridable options for publishing information (see gradle.properties)
// in CI for example, the "SNAPSHOT" variant may be added to the version
group = property("buildkit.group").toString()
version = property("buildkit.version").toString()

// resolve the name and full ID for the plugin
val pluginId = "$group.${property("buildkit.plugin-id")}"
val pluginName = property("buildkit.plugin-name").toString()

gradlePlugin {
  plugins.create(pluginName) {
    id = pluginId
    implementationClass = "io.github.darvld.buildkit.BuildkitPlugin"
  }
}

kotlin {
  explicitApi()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile> {
  sourceCompatibility = "11"
  targetCompatibility = "11"
}

tasks.withType<KotlinCompile> {
  compilerOptions.jvmTarget = JvmTarget.JVM_11
}

dependencies {
  implementation(gradleApi())
}
