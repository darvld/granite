@file: Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "granite"

// build plugin
includeBuild("tools/buildkit")

// list of all subprojects (see below for inclusion logic)
val projects = listOf(
  ":packages:granite-core",
  ":packages:granite-plugin-kotlin",
  ":packages:granite-plugin-gradle",
  ":packages:granite-plugin-intellij",
  // samples
  ":samples:granite-basic",
)

// read local properties not tracked in the VCS (at ~/.gradle/gradle.properties)
val localProperties = File(System.getProperty("user.home")).resolve(".gradle/gradle.properties").let { file ->
  val props = java.util.Properties()
  if (!file.isFile) return@let null

  props.load(file.inputStream())
  return@let props
}

projects.forEach { spec ->
  // some projects use plugins built by other subprojects, which is not supported by Gradle; instead of using
  // nested included builds, we can add a "pre-pass" in which only the plugin-related projects are built and
  // published to MavenLocal, allowing future (full) builds to correctly resolve their artifacts;

  // locate and read project properties
  val props = java.util.Properties()
  rootDir.resolve(spec.replace(":", "/").drop(1)).resolve("gradle.properties").takeIf { it.exists() }?.let {
    props.load(it.inputStream())
  }

  // override with the properties from the local environment
  localProperties?.forEach { key, value -> props[key] = value }

  // allow excluding the project completely (useful for disabling download-heavy projects like the Intellij plugin)
  val disabled = props.getProperty("granite.build.${spec.substringAfterLast(':')}.disable")?.toBoolean() ?: false
  if (disabled) return@forEach

  // opt into the pre-pass by setting the property below, otherwise the project will be excluded by default
  val isPrepass = System.getenv("GRANITE_BUILD_PREPASS").toBoolean()
  val includedInPrepass = props.getProperty("granite.prepass")?.toBoolean() ?: false
  if (isPrepass && !includedInPrepass) return@forEach

  // include the project
  include(spec)
}

// regular boring build config
pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    mavenLocal()
  }
  
  plugins {
    // shared remote cache, see https://docs.less.build/docs/gradle for configuration
    id("build.less") version ("1.0.0-beta6")
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    mavenLocal()
  }
}
