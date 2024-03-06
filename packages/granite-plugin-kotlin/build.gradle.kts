import io.github.darvld.buildkit.option

plugins {
  alias(libs.plugins.kotlinx.abiValidator)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.buildconfig)
}

kotlin {
  compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
  compilerOptions.optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}

dependencies {
  compileOnly(libs.kotlin.compiler)

  testImplementation(projects.packages.graniteCore)
  testImplementation(libs.kotlin.compiler)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(kotlin("test-junit"))
}

buildConfig {
  packageName("$group.plugin")
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${option("kotlin-plugin-id")}\"")
}
