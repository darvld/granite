import io.github.darvld.buildkit.option

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.intellij)
}

intellij {
  pluginName = option("idea-plugin-name")
  version = option("test-idea-version")
  type = "IC"
  
  // require the Kotlin plugin to be installed
  plugins = listOf("Kotlin")
  
  // don't set the sinceBuild/untilBuild field, it's set below
  updateSinceUntilBuild.set(false)
}

tasks.patchPluginXml {
  // compatibility range
  sinceBuild = "231.*"
  untilBuild = "233.*"
  
  // plugin (not IDE) version
  version = option("version")
}

repositories {
  // due to an issue with the Intellij Gradle plugin, we need to re-declare repositories,
  // otherwise only the Intellij repo will be used and dependency resolution will fail
  mavenCentral()
}

dependencies {
  implementation(projects.packages.granitePluginKotlin)
}
