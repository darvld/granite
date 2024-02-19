import io.github.darvld.buildkit.option

plugins {
  alias(libs.plugins.kotlinx.abiValidator)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.buildconfig)
  id("java-gradle-plugin")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(kotlin("gradle-plugin-api"))
}

gradlePlugin {
  plugins.create("granitePlugin") {
    id = option("gradle-plugin-id")
    implementationClass = "io.github.darvld.granite.gradle.GranitePlugin"
  }
}

buildConfig {
  val pluginProject = projects.packages.granitePluginKotlin
  val pluginId = option("kotlin-plugin-id")

  // these constants are used to register a subplugin artifact that will be used
  // by the Kotlin Gradle Plugin to resolve the compiler plugin dependency
  packageName("$group.gradle")
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"$pluginId\"")
  buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${pluginProject.name}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${pluginProject.group}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${pluginProject.version}\"")
}
